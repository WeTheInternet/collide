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

import java.util.List;

import org.waveprotocol.wave.client.scheduler.SchedulerInstance;

import com.google.collide.client.AppContext;
import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.collaboration.cc.GenericOperationChannel;
import com.google.collide.client.collaboration.cc.TransformQueue;
import com.google.collide.client.status.StatusManager;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.ClientToServerDocOp;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.dto.client.ClientDocOpFactory;
import com.google.collide.dto.client.DtoClientImpls.DocumentSelectionImpl;
import com.google.collide.dto.client.DtoClientImpls.FilePositionImpl;
import com.google.collide.shared.ot.OperationPair;
import com.google.collide.shared.ot.PositionTransformer;
import com.google.collide.shared.ot.Transformer;
import com.google.collide.shared.util.ErrorCallback;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.Reorderer.TimeoutCallback;

/**
 * Controller that handles the real-time collaboration and concurrency control
 * for a file. An instance is per file, and is meant to be replaced by a new
 * instance when switching files.
 *
 */
class FileConcurrencyController {

  private static final int OUT_OF_ORDER_DOC_OP_TIMEOUT_MS = 5000;

  interface CollaboratorDocOpSink {
    /**
     * @param selection as described in
     *        {@link ClientToServerDocOp#getSelection()}, with the exception
     *        that this has been transformed with outstanding client ops so that
     *        it is ready to be applied to the local document
     */
    void consume(DocOp docOp, String clientId, DocumentSelection selection);
  }

  interface DocOpListener {
    void onDocOpAckReceived(int documentId, DocOp serverHistoryDocOp, boolean clean);
    void onDocOpSent(int documentId, List<DocOp> docOps);
  }

  private static class ChannelListener implements GenericOperationChannel.Listener<DocOp> {
    private FileConcurrencyController controller;
    private final DocOpSender sender;
    private final ListenerManager<DocOpListener> docOpListenerManager;

    private ChannelListener(
        ListenerManager<DocOpListener> docOpListenerManager, DocOpSender sender) {
      this.docOpListenerManager = docOpListenerManager;
      this.sender = sender;
    }

    @Override
    public void onAck(final DocOp serverHistoryOp, final boolean clean) {
      sender.clearLastClientToServerDocOpMsg(null);
      docOpListenerManager.dispatch(new Dispatcher<DocOpListener>() {
        @Override
        public void dispatch(DocOpListener listener) {
          listener.onDocOpAckReceived(controller.getDocumentId(), serverHistoryOp, clean);
        }
      });
    }

    @Override
    public void onError(Throwable e) {
      Log.error(getClass(), "Error from concurrency control", e);
    }

    @Override
    public void onRemoteOp(DocOp serverHistoryOp, List<DocOp> pretransformedUnackedClientOps,
        List<DocOp> pretransformedQueuedClientOps) {
      /*
       * Do not pass the given server history doc op because it hasn't been
       * transformed for local consumption. Instead, the client calls
       * GenericOperationChannel.receive().
       */
      controller.onRemoteOp(pretransformedUnackedClientOps, pretransformedQueuedClientOps);
    }
  }

  private static class OutOfOrderDocOpTimeoutRecoveringCallback implements TimeoutCallback {
    private final StatusManager statusManager;    
    private DocOpRecoverer recoverer;
    
    private final ErrorCallback errorCallback = new ErrorCallback() {
      @Override
      public void onError() {
        StatusMessage fatal = new StatusMessage(statusManager, MessageType.FATAL,
            "There was a problem syncing with the server.");
        fatal.addAction(StatusMessage.RELOAD_ACTION);
        fatal.setDismissable(false);
        fatal.fire();
      }
    }; 
    
    OutOfOrderDocOpTimeoutRecoveringCallback(StatusManager statusManager) {
      this.statusManager = statusManager;
    }

    @Override
    public void onTimeout(int lastVersionDispatched) {
      recoverer.recover(errorCallback);
    }
  } 
  
  private static final TransformQueue.Transformer<DocOp> transformer =
      new TransformQueue.Transformer<DocOp>() {
        @Override
        public List<DocOp> compact(List<DocOp> clientOps) {
          // TODO: implement for efficiency
          return clientOps;
        }

        @Override
        public org.waveprotocol.wave.model.operation.OperationPair<DocOp> transform(DocOp clientOp,
            DocOp serverOp) {
          try {
            OperationPair operationPair =
                Transformer.transform(ClientDocOpFactory.INSTANCE, clientOp, serverOp);
            return new org.waveprotocol.wave.model.operation.OperationPair<DocOp>(
                operationPair.clientOp(), operationPair.serverOp());
          } catch (Exception e) {
            // TODO: stop using RuntimeException and make a custom
            // exception type
            Log.error(getClass(), "Error from DocOp transformer", e);
            throw new RuntimeException(e);
          }
        }
      };

  public static FileConcurrencyController create(AppContext appContext,
      String fileEditSessionKey,
      int documentId,
      IncomingDocOpDemultiplexer docOpDemux,
      CollaboratorDocOpSink remoteOpSink,
      DocOpListener docOpListener,
      DocOpRecoveryInitiator docOpRecoveryInitiator) {

    ListenerManager<DocOpListener> docOpListenerManager = ListenerManager.create();
    docOpListenerManager.add(docOpListener);
    
    OutOfOrderDocOpTimeoutRecoveringCallback timeoutCallback =
        new OutOfOrderDocOpTimeoutRecoveringCallback(appContext.getStatusManager());
    DocOpReceiver receiver = new DocOpReceiver(
        docOpDemux, fileEditSessionKey, timeoutCallback, OUT_OF_ORDER_DOC_OP_TIMEOUT_MS);
    DocOpSender sender = new DocOpSender(appContext.getFrontendApi(),
        docOpDemux,
        fileEditSessionKey,
        documentId,
        docOpListenerManager,
        docOpRecoveryInitiator);
    ChannelListener listener = new ChannelListener(docOpListenerManager, sender);
    
    // TODO: implement the Logger interface using our logging utils
    GenericOperationChannel<DocOp> channel = new GenericOperationChannel<DocOp>(
        SchedulerInstance.getMediumPriorityTimer(), transformer, receiver, sender, listener);
    receiver.setRevisionProvider(channel);
    
    DocOpRecoverer recoverer = new DocOpRecoverer(fileEditSessionKey,
        appContext.getFrontendApi().RECOVER_FROM_MISSED_DOC_OPS,
        receiver,
        sender,
        channel);
    timeoutCallback.recoverer = recoverer;
    
    FileConcurrencyController fileConcurrencyController = new FileConcurrencyController(channel,
        receiver,
        sender,
        remoteOpSink,
        recoverer,
        docOpListenerManager,
        documentId);
    listener.controller = fileConcurrencyController;

    return fileConcurrencyController;
  }

  private final GenericOperationChannel<DocOp> ccChannel;
  private final DocOpReceiver receiver;
  private final CollaboratorDocOpSink sink;
  private final DocOpSender sender;
  private final DocOpRecoverer recoverer;
  private final ListenerManager<DocOpListener> docOpListenerManager;
  private final int documentId;

  private FileConcurrencyController(GenericOperationChannel<DocOp> ccChannel,
      DocOpReceiver receiver,
      DocOpSender sender,
      CollaboratorDocOpSink sink,
      DocOpRecoverer recoverer,
      ListenerManager<DocOpListener> docOpListenerManager,
      int documentId) {
    this.ccChannel = ccChannel;
    this.receiver = receiver;
    this.sender = sender;
    this.sink = sink;
    this.recoverer = recoverer;
    this.docOpListenerManager = docOpListenerManager;
    this.documentId = documentId;
  }

  int getDocumentId() {
    return documentId;
  }

  void consumeLocalDocOp(DocOp docOp) {
    ccChannel.send(docOp);
  }

  ListenerRegistrar<DocOpListener> getDocOpListenerRegistrar() {
    return docOpListenerManager;
  }
  
  int getQueuedClientOpCount() {
    return ccChannel.getQueuedClientOpCount();
  }

  int getUnackedClientOpCount() {
    return ccChannel.getUnacknowledgedClientOpCount();
  }
  
  void start(int ccRevision) {
    ccChannel.connect(ccRevision, BootstrapSession.getBootstrapSession().getActiveClientId());
  }

  void stop() {
    ccChannel.disconnect();
  }

  void setDocOpCreationParticipant(ClientToServerDocOpCreationParticipant participant) {
    sender.setDocOpCreationParticipant(participant);
  }

  void recover(ErrorCallback errorCallback) {
    recoverer.recover(errorCallback);
  }
  
  private void onRemoteOp(List<DocOp> pretransformedUnackedClientOps,
      List<DocOp> pretransformedQueuedClientOps) {

    DocumentSelection selection = receiver.getSelection();
    if (selection != null) {
      // Transform the remote position with our unacked and queued doc ops
      selection =
          transformSelection(selection, pretransformedUnackedClientOps,
              pretransformedQueuedClientOps);
    }

    sink.consume(ccChannel.receive(), receiver.getClientId(), selection);
  }

  private DocumentSelection transformSelection(DocumentSelection selection,
      List<DocOp> pretransformedUnackedClientOps,
      List<DocOp> pretransformedQueuedClientOps) {

    PositionTransformer basePositionTransformer =
        new PositionTransformer(selection.getBasePosition().getLineNumber(), selection
            .getBasePosition().getColumn());
    PositionTransformer cursorPositionTransformer =
        new PositionTransformer(selection.getCursorPosition().getLineNumber(), selection
            .getCursorPosition().getColumn());

    for (DocOp op : pretransformedUnackedClientOps) {
      basePositionTransformer.transform(op);
      cursorPositionTransformer.transform(op);
    }

    for (DocOp op : pretransformedQueuedClientOps) {
      basePositionTransformer.transform(op);
      cursorPositionTransformer.transform(op);
    }

    return DocumentSelectionImpl.make().setBasePosition(makeFilePosition(basePositionTransformer))
        .setCursorPosition(makeFilePosition(cursorPositionTransformer))
        .setUserId(selection.getUserId());
  }

  private FilePositionImpl makeFilePosition(PositionTransformer transformer) {
    return
        FilePositionImpl.make().setLineNumber(transformer.getLineNumber())
            .setColumn(transformer.getColumn());
  }
}
