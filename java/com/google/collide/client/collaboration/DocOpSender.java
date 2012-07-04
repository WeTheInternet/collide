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

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.collaboration.FileConcurrencyController.DocOpListener;
import com.google.collide.client.collaboration.cc.GenericOperationChannel.SendOpService;
import com.google.collide.client.communication.FrontendApi;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.ServerToClientDocOps;
import com.google.collide.dto.client.DtoClientImpls.ClientToServerDocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.DocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.ServerToClientDocOpImpl;
import com.google.collide.json.client.Jso;
import com.google.collide.json.client.JsoArray;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import java.util.List;

/**
 * Helper to take outgoing doc ops from the local concurrency control library
 * and send them to the server.
 *
 */
class DocOpSender implements SendOpService<DocOp>, LastClientToServerDocOpProvider {

  private final ListenerManager<DocOpListener> docOpListenerManager;  
  private final int documentId;
  private final String fileEditSessionKey;
  private final FrontendApi frontendApi;
  private final IncomingDocOpDemultiplexer docOpDemux;
  
  private ClientToServerDocOpCreationParticipant clientToServerDocOpCreationParticipant;
  private ClientToServerDocOpImpl lastClientToServerDocOpMsg;
  private final DocOpRecoveryInitiator docOpRecoveryInitiator;

  public DocOpSender(FrontendApi frontendApi,
      IncomingDocOpDemultiplexer docOpDemux,
      String fileEditSessionKey,
      int documentId,
      ListenerManager<DocOpListener> docOpListenerManager,
      DocOpRecoveryInitiator docOpRecoveryInitiator) {
    this.frontendApi = frontendApi;
    this.docOpDemux = docOpDemux;
    this.fileEditSessionKey = fileEditSessionKey;
    this.documentId = documentId;
    this.docOpListenerManager = docOpListenerManager;
    this.docOpRecoveryInitiator = docOpRecoveryInitiator;
  }

  @Override
  public void callbackNotNeeded(SendOpService.Callback callback) {
  }

  @Override
  public void requestRevision(SendOpService.Callback callback) {
    /*
     * TODO: get revision from server, but for now this is never
     * called since we are not handling connection errors fully
     */
    assert false;
  }

  @Override
  public void submitOperations(
      int revision, final List<DocOp> operations, final SendOpService.Callback callback) {
    try {
      /*
       * Copy the operations into the list.
       * TODO: Consider making the client code maintain this list as a native collection.
       */
      JsoArray<String> docOps = JsoArray.create();
      for (int i = 0, n = operations.size(); i < n; i++) {
        docOps.add(Jso.serialize((DocOpImpl) operations.get(i)));
      }
      ClientToServerDocOpImpl message = ClientToServerDocOpImpl
          .make()
          .setFileEditSessionKey(fileEditSessionKey)
          .setCcRevision(revision)
          .setClientId(BootstrapSession.getBootstrapSession().getActiveClientId())
          .setDocOps2(docOps);

      if (clientToServerDocOpCreationParticipant != null) {
        clientToServerDocOpCreationParticipant.onCreateClientToServerDocOp(message);
      }

      frontendApi.MUTATE_FILE.send(message, new ApiCallback<ServerToClientDocOps>() {
        @Override
        public void onFail(FailureReason reason) {
          if (reason == FailureReason.MISSING_WORKSPACE_SESSION) {
            docOpRecoveryInitiator.teardown();
          } else {
            docOpRecoveryInitiator.recover();
          }
        }

        @Override
        public void onMessageReceived(ServerToClientDocOps message) {
          for (int i = 0; i < message.getDocOps().size(); i++) {
            docOpDemux.handleServerToClientDocOpMsg(
                (ServerToClientDocOpImpl) message.getDocOps().get(i));
          }
        }        
      });
      
      lastClientToServerDocOpMsg = message;

      docOpListenerManager.dispatch(new Dispatcher<DocOpListener>() {
        @Override
        public void dispatch(DocOpListener listener) {
          listener.onDocOpSent(documentId, operations);
        }
      });

      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          /*
           * Need to defer this since the client is not expecting a success
           * reply from within the same call stack
           */
          /*
           * TODO: Need the applied revision. We can't easily get
           * it since unlike HTTP, our push channel does not have a mechanism
           * for an inline response (could create a separate response message,
           * which is this TODO). We'll be okay for now without it because the
           * cc lib uses it for recovery, but we never report failures so it
           * doesn't have exercise the recovery logic right now. It also uses
           * this for optimizations if the given applied revision matches what
           * it expects, but since we give an unexpected value, it does nothing
           * with it.
           */
          callback.onSuccess(Integer.MIN_VALUE);
        }
      });

    } catch (Throwable t) {
      callback.onFatalError(t);
    }
  }

  void setDocOpCreationParticipant(ClientToServerDocOpCreationParticipant participant) {
    clientToServerDocOpCreationParticipant = participant;
  }

  @Override
  public ClientToServerDocOpImpl getLastClientToServerDocOpMsg() {
    return lastClientToServerDocOpMsg;
  }

  /**
   * Clears the message that would be returned by
   * {@link #getLastClientToServerDocOpMsg()}.
   * 
   * @param clientToServerDocOpMsgToDelete if provided, the current message must
   *        match the given message for it to be cleared
   */
  @Override
  public void clearLastClientToServerDocOpMsg(
      ClientToServerDocOpImpl clientToServerDocOpMsgToDelete) {
    if (clientToServerDocOpMsgToDelete == null
        || clientToServerDocOpMsgToDelete == lastClientToServerDocOpMsg) {
      lastClientToServerDocOpMsg = null;
    }
  }
}
