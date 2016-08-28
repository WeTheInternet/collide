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
import com.google.collide.client.collaboration.cc.RevisionProvider;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.FrontendApi.RequestResponseApi;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.RecoverFromMissedDocOps;
import com.google.collide.dto.RecoverFromMissedDocOpsResponse;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.ServerToClientDocOp;
import com.google.collide.dto.client.DtoClientImpls.ClientToServerDocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.RecoverFromMissedDocOpsImpl;
import com.google.collide.dto.client.DtoClientImpls.ServerToClientDocOpImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.ErrorCallback;

import elemental.util.Timer;

/**
 * A class that performs the XHR to recover missed doc ops and funnels the results into the right
 * components.
 */
class DocOpRecoverer {    
  private static final int RECOVERY_MAX_RETRIES = 5;
  private static final int RECOVERY_RETRY_DELAY_MS = 5000;

  private final String fileEditSessionKey;
  private final RequestResponseApi<RecoverFromMissedDocOps, RecoverFromMissedDocOpsResponse>
      recoverFrontendApi;
  private final DocOpReceiver docOpReceiver;
  private final LastClientToServerDocOpProvider lastSentDocOpProvider;
  private final RevisionProvider revisionProvider;
  
  private boolean isRecovering;
  
  DocOpRecoverer(String fileEditSessionKey, RequestResponseApi<
      RecoverFromMissedDocOps, RecoverFromMissedDocOpsResponse> recoverFrontendApi,
      DocOpReceiver docOpReceiver, LastClientToServerDocOpProvider lastSentDocOpProvider,
      RevisionProvider revisionProvider) {
    this.fileEditSessionKey = fileEditSessionKey;
    this.recoverFrontendApi = recoverFrontendApi;
    this.docOpReceiver = docOpReceiver;
    this.lastSentDocOpProvider = lastSentDocOpProvider;
    this.revisionProvider = revisionProvider;
  }

  /**
   * Attempts to recover after missed doc ops.
   */
  void recover(ErrorCallback errorCallback) {
    recover(errorCallback, 0);
  }
  
  private void recover(final ErrorCallback errorCallback, final int retryCount) {
    
    if (isRecovering) {
      return;
    }
    
    isRecovering = true;
    
    Log.info(getClass(), "Recovering from disconnection");
    
    // 1) Gather potentially unacked doc ops
    final ClientToServerDocOpImpl lastSentMsg =
        lastSentDocOpProvider.getLastClientToServerDocOpMsg();
    
    /*
     * 2) Pause processing of incoming doc ops and queue them instead. This allows us, in the
     * future, to apply the queued doc ops after recovery (the recovery response may not have
     * contained some of the queued doc ops depending on the order the XHR and doc ops being
     * processed by the server.)
     */
    docOpReceiver.pause();
    
    // 3) Perform recovery XHR
    /*
     * If we had unacked doc ops, we must use their intended version since that
     * is the version of the document to which the unacked doc ops apply
     * cleanly. If there aren't any unacked doc ops, we can use the latest
     * version of the document that we have. (These can differ if we received
     * doc ops while still waiting for our ack.)
     * 
     * The unacked doc ops' intended version will always be less than or equal
     * to the latest version we have received. When applying the returned doc
     * ops from the document history, we will skip those that have already been
     * applied.
     */
    int revision = lastSentMsg != null ? lastSentMsg.getCcRevision() : revisionProvider.revision();
    
    RecoverFromMissedDocOpsImpl recoveryDto =
        RecoverFromMissedDocOpsImpl.make()
            .setClientId(BootstrapSession.getBootstrapSession().getActiveClientId())
            .setCurrentCcRevision(revision)
            .setFileEditSessionKey(fileEditSessionKey);
    
    if (lastSentMsg != null) {
      recoveryDto.setDocOps2((JsoArray<String>) lastSentMsg.getDocOps2());
    }
    
    recoverFrontendApi.send(recoveryDto,
        new ApiCallback<RecoverFromMissedDocOpsResponse>() {
          @Override
          public void onMessageReceived(RecoverFromMissedDocOpsResponse message) {
            
            // 4) Process the doc ops while I was disconnected (which will include our ack)
            JsonArray<ServerToClientDocOp> recoveredServerDocOps = message.getDocOps();
            for (int i = 0; i < recoveredServerDocOps.size(); i++) {
              ServerToClientDocOp serverDocOp = recoveredServerDocOps.get(i);
              if (serverDocOp.getAppliedCcRevision() > revisionProvider.revision()) {
                docOpReceiver.simulateOrderedDocOpReceived((ServerToClientDocOpImpl) serverDocOp,
                    true);
              }
            }
            
            // 5) Process queued doc ops while I was recovering
            JsonArray<ServerToClientDocOp> queuedServerDocOps =
                docOpReceiver.getOrderedQueuedServerToClientDocOps();
            for (int i = 0; i < queuedServerDocOps.size(); i++) {
              ServerToClientDocOp serverDocOp = queuedServerDocOps.get(i);
              if (serverDocOp.getAppliedCcRevision() > revisionProvider.revision()) {
                docOpReceiver.simulateOrderedDocOpReceived((ServerToClientDocOpImpl) serverDocOp,
                    true);
              }
            }

            /*
             * 6) Back to normal! At this point, any unacked doc ops will have
             * been acked. Any queued doc ops are scheduled to be sent. We clear
             * the last client-to-server-doc-op. We can also resume the doc op
             * receiver now since our document is at the version that they will
             * be targetting.
             */
            lastSentDocOpProvider.clearLastClientToServerDocOpMsg(lastSentMsg);
            docOpReceiver.resume(revisionProvider.revision() + 1);
            
            Log.info(getClass(), "Recovered successfully");
            
            handleRecoverFinished();
          }

          @Override
          public void onFail(FailureReason reason) {
            if (retryCount < RECOVERY_MAX_RETRIES) {
              new Timer() {
                @Override
                public void run() {
                  recover(errorCallback, retryCount + 1);
                }
              }.schedule(RECOVERY_RETRY_DELAY_MS);
            } else {
              Log.info(getClass(), "Could not recover");
              errorCallback.onError();
              
              handleRecoverFinished();
            }
          }
        });
  }

  private void handleRecoverFinished() {
    isRecovering = false;
  }
}
