// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.collide.client.collaboration;

import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer.Receiver;
import com.google.collide.client.collaboration.cc.GenericOperationChannel.ReceiveOpChannel;
import com.google.collide.client.collaboration.cc.RevisionProvider;
import com.google.collide.client.util.ClientTimer;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.dto.ServerToClientDocOp;
import com.google.collide.dto.client.DtoClientImpls.ServerToClientDocOpImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.Pair;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.Reorderer;
import com.google.collide.shared.util.Reorderer.ItemSink;
import com.google.collide.shared.util.Reorderer.TimeoutCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Helper to receive messages from the transport and pass it onto the local
 * concurrency control library.
 *
 */
class DocOpReceiver implements ReceiveOpChannel<DocOp> {

  private ReceiveOpChannel.Listener<DocOp> listener;
  
  @VisibleForTesting
  final Receiver unorderedDocOpReceiver = new Receiver() {
    @Override
    public void onDocOpReceived(ServerToClientDocOpImpl message, DocOp docOp) {
      // We just received this doc op from the wire, pass it to the reorderer
      docOpReorderer.acceptItem(Pair.of(message, docOp), message.getAppliedCcRevision());
    }
  };

  private final ItemSink<Pair<ServerToClientDocOpImpl, DocOp>> orderedDocOpSink =
      new ItemSink<Pair<ServerToClientDocOpImpl, DocOp>>() {
        @Override
        public void onItem(Pair<ServerToClientDocOpImpl, DocOp> docOpPair, int version) {
          onReceivedOrderedDocOp(docOpPair.first, docOpPair.second, false);
        }
      };

  private Reorderer<Pair<ServerToClientDocOpImpl, DocOp>> docOpReorderer;
  private final TimeoutCallback outOfOrderTimeoutCallback;
  private final int outOfOrderTimeoutMs;
  
  private final String fileEditSessionKey;
  /** Valid only for a partial scope of messageReceiver */
  private String currentMessageClientId;
  /** Valid only for a partial scope of messageReceiver */
  private DocumentSelection currentMessageSelection;
  private final IncomingDocOpDemultiplexer docOpDemux;
  
  private boolean isPaused;
  private final JsonArray<ServerToClientDocOp> queuedOrderedServerToClientDocOps = JsonCollections
      .createArray();
  
  private RevisionProvider revisionProvider;

  DocOpReceiver(IncomingDocOpDemultiplexer docOpDemux, String fileEditSessionKey,
      Reorderer.TimeoutCallback outOfOrderTimeoutCallback, int outOfOrderTimeoutMs) {
    this.docOpDemux = docOpDemux;
    this.fileEditSessionKey = fileEditSessionKey;
    this.outOfOrderTimeoutCallback = outOfOrderTimeoutCallback;
    this.outOfOrderTimeoutMs = outOfOrderTimeoutMs;
  }

  void setRevisionProvider(RevisionProvider revisionProvider) {
    this.revisionProvider = revisionProvider;
  }
  
  @Override
  public void connect(int revision, ReceiveOpChannel.Listener<DocOp> listener) {
    Preconditions.checkState(revisionProvider != null, "Must have set revisionProvider by now");
    
    this.listener = listener;
    
    int nextExpectedVersion = revision + 1;
    this.docOpReorderer = Reorderer.create(
        nextExpectedVersion, orderedDocOpSink, outOfOrderTimeoutMs, outOfOrderTimeoutCallback,
        ClientTimer.FACTORY);
    docOpDemux.setReceiver(fileEditSessionKey, unorderedDocOpReceiver);
  }

  @Override
  public void disconnect() {
    docOpDemux.setReceiver(fileEditSessionKey, null);
  }

  /**
   * Pauses the processing (calling back to listener) of received doc ops. While paused, any
   * received doc ops will be stored in a queue which can be retrieved via
   * {@link #getOrderedQueuedServerToClientDocOps()}. This queue will only contain doc ops from this
   * point forward.
   *
   * <p>
   * This method can be called multiple times without calling {@link #resume(int)}.
   */
  void pause() {
    if (isPaused) {
      return;
    }
    
    isPaused = true;
    docOpReorderer.setTimeoutEnabled(false);
    
    // Clear queue so it will contain only doc ops after this pause
    queuedOrderedServerToClientDocOps.clear();
  }
  
  JsonArray<ServerToClientDocOp> getOrderedQueuedServerToClientDocOps() {
    return queuedOrderedServerToClientDocOps;
  }

  /**
   * Resumes the processing of received doc ops.
   * 
   * <p>
   * While this was paused, doc ops were accumulated in the queue
   * {@link #getOrderedQueuedServerToClientDocOps()}. Those will not be processed
   * automatically.
   * 
   * <p>
   * This method cannot be called when already resumed.
   */
  void resume(int nextExpectedVersion) {
    Preconditions.checkState(isPaused, "Cannot resume if already resumed");
    
    isPaused = false;
    docOpReorderer.setTimeoutEnabled(true);
    docOpReorderer.skipToVersion(nextExpectedVersion);
  }
  
  String getClientId() {
    return currentMessageClientId;
  }

  DocumentSelection getSelection() {
    return currentMessageSelection;
  }
  
  /**
   * @param bypassPaused whether to process the doc op immediately even if
   *        {@link #pause()} has been called
   */
  void simulateOrderedDocOpReceived(ServerToClientDocOpImpl message, boolean bypassPaused) {
    DocOp docOp = message.getDocOp2();    
    onReceivedOrderedDocOp(message, docOp, bypassPaused);
  }
  
  private void onReceivedOrderedDocOp(
      ServerToClientDocOpImpl message, DocOp docOp, boolean bypassPaused) {
    if (isPaused && !bypassPaused) {
      // Just queue the doc op messages instead
      queuedOrderedServerToClientDocOps.add(message);
      return;
    }
    
    if (revisionProvider.revision() >= message.getAppliedCcRevision()) {
      // Already seen this
      return;
    }
            
    /*
     * Later in the stack, we need this valuable information for rendering the
     * collaborator's cursor. But, since we funnel through the concurrency
     * control library, this information is lost. The workaround is to stash
     * the data here. We will fetch this from the callback given by the
     * concurrency control library.
     * 
     * TODO: This is really a workaround until I have time to
     * think about a clean API for sending/receiving positions like the
     * collaborative selection/cursor stuff requires. Once I do that, I'll
     * also remove this workaround and make position transformation a
     * first-class feature inside the forked CC library.
     */
    currentMessageClientId = message.getClientId();
    currentMessageSelection = message.getSelection();

    try {
      listener.onMessage(message.getAppliedCcRevision(), message.getClientId(), docOp);
    } catch (Throwable t) {
      Log.error(getClass(), "Could not handle received doc op", t);
    }

    currentMessageClientId = null;
    currentMessageSelection = null;
  }
}
