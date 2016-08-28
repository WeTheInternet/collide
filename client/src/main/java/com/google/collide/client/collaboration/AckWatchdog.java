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

import com.google.collide.client.collaboration.FileConcurrencyController.DocOpListener;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.status.StatusManager;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.util.WindowUnloadingController;
import com.google.collide.client.util.WindowUnloadingController.Message;
import com.google.collide.client.xhrmonitor.XhrWarden;
import com.google.collide.dto.DocOp;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.ot.DocOpUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.user.client.Timer;

/**
 * A service that warns the user if a sent document operation does not receive
 * an acknowledgment in a short amount of itme.
 *
 */

class AckWatchdog implements DocOpListener {

  private static final int ACK_WARNING_TIMEOUT_MS = 10000;
  private static final int ACK_ERROR_TIMEOUT_MS = 60000;

  /*
   * TODO: editor read-only state can be touched by multiple
   * clients. Imagine two that each want to set the editor read-only for 5
   * seconds, (A) sets read-only, a few seconds elapse, (B) sets read-only, (A)
   * wants to set back to write (but B still wants read-only), then (B) wants to
   * set to write. This doesn't handle that well; the editor needs to expose
   * better API for this (give each caller a separate boolean and only set back
   * to write when all callers have readonly=false. Perhaps API will be
   * editor.setReadOnly(getClass(), true) or some string ID instead of class).
   */
  private boolean hasSetEditorReadOnly;
  private boolean isEditorReadOnlyByOthers;
  private Editor editor;
  private final JsonArray<DocOp> unackedDocOps = JsonCollections.createArray();
  private final DocOpRecoveryInitiator docOpRecoveryInitiator;
  private final StatusManager statusManager;
  private StatusMessage warningMessage;
  private StatusMessage errorMessage;
  private final WindowUnloadingController windowUnloadingController;
  private final WindowUnloadingController.Message windowUnloadingMessage;
  private final Timer warningTimer = new Timer() {
    @Override
    public void run() {
      showErrorOrWarningMessage(false);
    }
  };
  private final Timer errorTimer = new Timer() {
    @Override
    public void run() {
      showErrorOrWarningMessage(true);
    }
  };

  AckWatchdog(StatusManager statusManager, WindowUnloadingController windowClosingController,
      DocOpRecoveryInitiator docOpRecoveryInitiator) {
    this.statusManager = statusManager;
    this.windowUnloadingController = windowClosingController;
    this.docOpRecoveryInitiator = docOpRecoveryInitiator;

    // Add a window closing listener to wait for client ops to complete.
    windowUnloadingMessage = new Message() {
      @Override
      public String getMessage() {
        if (unackedDocOps.size() > 0) {
          return
              "You have changes that are still saving and will be lost if you leave this page now.";
        } else {
          return null;
        }
      }
    };
    windowClosingController.addMessage(windowUnloadingMessage);
  }

  void teardown() {
    warningTimer.cancel();
    errorTimer.cancel();
    windowUnloadingController.removeMessage(windowUnloadingMessage);
  }

  public void setEditor(Editor editor) {
    /*
     * TODO: minimizing change in this CL, but a future CL could
     * introudce a document tag for the read-only state
     */
    Editor oldEditor = this.editor;
    if (oldEditor != null && hasSetEditorReadOnly && !isEditorReadOnlyByOthers) {
      // Undo our changes
      oldEditor.setReadOnly(false);
    }
   
    hasSetEditorReadOnly = false;
    
    this.editor = editor;
  }
  
  @Override
  public void onDocOpAckReceived(int documentId, DocOp serverHistoryDocOp, boolean clean) {
    unackedDocOps.remove(0);

    if (unackedDocOps.size() == 0) {
      warningTimer.cancel();
      errorTimer.cancel();
      hideErrorAndWarningMessages();
    }
  }

  @Override
  public void onDocOpSent(int documentId, List<DocOp> docOps) {
    /*
     * Our OT model only allows for one set of outstanding doc ops, so this will
     * not be called again until we have received acks for all of the individual
     * doc ops.
     */

    for (int i = 0, n = docOps.size(); i < n; i++) {
      unackedDocOps.add(docOps.get(i));
    }

    warningTimer.schedule(ACK_WARNING_TIMEOUT_MS);
    errorTimer.schedule(ACK_ERROR_TIMEOUT_MS);
  }

  /**
   * @param error true for error, false for warning
   */
  private void showErrorOrWarningMessage(boolean error) {
    if (error && editor != null) {
      isEditorReadOnlyByOthers = editor.isReadOnly();
      hasSetEditorReadOnly = true;
      editor.setReadOnly(true);
    }

    if (error && errorMessage == null) {
      errorMessage = createErrorMessage();
      errorMessage.fire();
    } else if (!error && warningMessage == null) {
      warningMessage = createWarningMessage();
      warningMessage.fire();
    }
    
    docOpRecoveryInitiator.recover();
  }
  
  private void hideErrorAndWarningMessages() {
    if (hasSetEditorReadOnly && !isEditorReadOnlyByOthers && editor != null) {
      editor.setReadOnly(false);
      hasSetEditorReadOnly = false;
    }

    boolean hadErrorOrWarningMessage = errorMessage != null || warningMessage != null;
    if (errorMessage != null) {
      errorMessage.cancel();
      errorMessage = null;
    }
    
    if (warningMessage != null) {
      warningMessage.cancel();
      warningMessage = null;
    }

    if (hadErrorOrWarningMessage) {
      createReceivedAckMessage().fire();
    }
  }

  private StatusMessage createWarningMessage() {
    StatusMessage msg =
        new StatusMessage(statusManager, MessageType.LOADING,
            "Still saving your latest changes...");
    msg.setDismissable(true);

    XhrWarden.dumpRequestsToConsole();
    return msg;
  }

  private StatusMessage createErrorMessage() {
    StatusMessage msg =
        new StatusMessage(statusManager, MessageType.ERROR,
            "Your latest changes timed out while saving.");
    msg.addAction(StatusMessage.RELOAD_ACTION);
    msg.setDismissable(false);

    XhrWarden.dumpRequestsToConsole();
    return msg;
  }

  private StatusMessage createReceivedAckMessage() {
    StatusMessage msg =
        new StatusMessage(statusManager, MessageType.CONFIRMATION,
            "Saved successfully.");
    msg.setDismissable(true);
    msg.expire(1500);

    return msg;
  }

  private String getUnackedDocOpsString() {
    StringBuilder str = new StringBuilder();

    for (int i = 0, n = unackedDocOps.size(); i < n; i++) {
      str.append(DocOpUtils.toString(unackedDocOps.get(i), true)).append("\n");
    }

    return str.toString();
  }
}
