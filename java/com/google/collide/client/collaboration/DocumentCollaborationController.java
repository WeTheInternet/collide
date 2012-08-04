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

import com.google.collide.client.AppContext;
import com.google.collide.client.code.ParticipantModel;
import com.google.collide.client.collaboration.FileConcurrencyController.CollaboratorDocOpSink;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.dto.client.ClientDocOpFactory;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Document.TextListener;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.ot.Composer;
import com.google.collide.shared.ot.Composer.ComposeException;
import com.google.collide.shared.ot.DocOpApplier;
import com.google.collide.shared.ot.DocOpBuilder;
import com.google.collide.shared.ot.DocOpUtils;
import com.google.collide.shared.util.ErrorCallback;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;

/**
 * Controller that adds real-time collaboration at the document level.
 *
 *  This controller attaches to the document to broadcast any local changes to other collaborators.
 * Conversely, it receives other collaborators' changes and applies them to the local document.
 *
 * Clients must call {@link #initialize}.
 */
public class DocumentCollaborationController implements DocOpRecoveryInitiator {

  private final AppContext appContext;
  private final ParticipantModel participantModel;
  private final Document document;
  private final RemoverManager removerManager = new RemoverManager();

  private AckWatchdog ackWatchdog;
  private FileConcurrencyController fileConcurrencyController;

  private Editor editor;
  private LocalCursorTracker localCursorTracker;

  /** Saves the collaborators' selections to display when we attach to an editor */
  private JsonStringMap<DocumentSelection> collaboratorSelections = JsonCollections.createMap();
  private CollaboratorCursorController collaboratorCursorController;
  private final IncomingDocOpDemultiplexer docOpDemux;

  /**
   * Used to prevent remote doc ops from being considered as local user edits inside the document
   * callback
   */
  private boolean isConsumingRemoteDocOp;
  private final CollaboratorDocOpSink remoteOpSink = new CollaboratorDocOpSink() {
    @Override
    public void consume(DocOp docOp, String clientId, DocumentSelection selection) {
      isConsumingRemoteDocOp = true;
      try {
        DocOpApplier.apply(docOp, document);

        if (editor == null) {
          if (selection != null) {
            collaboratorSelections.put(selection.getUserId(), selection);
          }
        } else {
          collaboratorCursorController.handleSelectionChange(clientId, selection);
        }

      } finally {
        isConsumingRemoteDocOp = false;
      }
    }
  };

  private final Document.TextListener localTextListener = new TextListener() {
    @Override
    public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
      if (isConsumingRemoteDocOp) {
        /*
         * These text changes are being caused by the consumption of the remote doc ops. We don't
         * want to rebroadcast these.
         */
        return;
      }

      DocOp op = null;

      for (int i = 0, n = textChanges.size(); i < n; i++) {
        TextChange textChange = textChanges.get(i);
        DocOp curOp = DocOpUtils.createFromTextChange(ClientDocOpFactory.INSTANCE, textChange);
        try {
          op = op != null ? Composer.compose(ClientDocOpFactory.INSTANCE, op, curOp) : curOp;
        } catch (ComposeException e) {
          if (editor != null) {
            editor.setReadOnly(true);
          }
          new StatusMessage(appContext.getStatusManager(), MessageType.FATAL,
              "Problem processing the text changes, please reload.").fire();
          return;
        }
      }

      fileConcurrencyController.consumeLocalDocOp(op);
    }
  };

  /**
   * Creates an instance of the {@link DocumentCollaborationController}.
   */
  public DocumentCollaborationController(AppContext appContext, ParticipantModel participantModel,
      IncomingDocOpDemultiplexer docOpDemux, Document document,
      JsonArray<DocumentSelection> selections) {
    this.appContext = appContext;
    this.participantModel = participantModel;
    this.docOpDemux = docOpDemux;
    this.document = document;

    for (int i = 0, n = selections.size(); i < n; i++) {
      DocumentSelection selection = selections.get(i);
      collaboratorSelections.put(selection.getUserId(), selection);
    }
  }

  public void initialize(String fileEditSessionKey, int ccRevision) {
    ackWatchdog = new AckWatchdog(
        appContext.getStatusManager(), appContext.getWindowUnloadingController(), this);

    fileConcurrencyController = FileConcurrencyController.create(appContext,
        fileEditSessionKey,
        document.getId(),
        docOpDemux,
        remoteOpSink,
        ackWatchdog,
        this);
    fileConcurrencyController.start(ccRevision);

    removerManager.track(document.getTextListenerRegistrar().add(localTextListener));
  }

  @Override
  public void teardown() {
    detachFromEditor();

    removerManager.remove();

    /*
     * Replace the concurrency controller instance to ensure there isn't any internal state leftover
     * from the previous file. (At the time of this writing, the concurrency control library has
     * internal state that cannot be reset completely via its public API.)
     */
    fileConcurrencyController.stop();
    fileConcurrencyController = null;

    ackWatchdog.teardown();
    ackWatchdog = null;
  }

  public void attachToEditor(Editor editor) {
    this.editor = editor;
    ackWatchdog.setEditor(editor);

    /*
     * TODO: when supporting multiple editors, we'll need to encapsulate these in a
     * POJO keyed off editor ID. For now, assume only a single editor.
     */
    localCursorTracker = new LocalCursorTracker(this, editor.getSelection());
    localCursorTracker.forceSendingSelection();

    collaboratorCursorController = new CollaboratorCursorController(
        appContext, document, editor.getBuffer(), participantModel, collaboratorSelections);

    fileConcurrencyController.setDocOpCreationParticipant(localCursorTracker);

    // Send our document selection
    ensureQueuedDocOp();
  }

  public void detachFromEditor() {
    /*
     * The "!= null" checks are for when detachFromEditor is called from teardown because we can be
     * torndown before the document is detached from the editor.
     */

fileConcurrencyController.setDocOpCreationParticipant(null);

    if (collaboratorCursorController != null) {
      collaboratorSelections = collaboratorCursorController.getSelectionsMap();
      collaboratorCursorController.teardown();
      collaboratorCursorController = null;
    }

    if (localCursorTracker != null) {
      localCursorTracker.teardown();
      localCursorTracker = null;
    }

    ackWatchdog.setEditor(null);
    this.editor = null;
  }

  FileConcurrencyController getFileConcurrencyController() {
    return fileConcurrencyController;
  }

  void ensureQueuedDocOp() {
    if (fileConcurrencyController.getQueuedClientOpCount() == 0) {
      // There aren't any queued doc ops, create and send a noop doc op
      DocOp noopDocOp = new DocOpBuilder(ClientDocOpFactory.INSTANCE, false).retainLine(
          document.getLineCount()).build();
      fileConcurrencyController.consumeLocalDocOp(noopDocOp);
    }
  }

  @Override
  public void recover() {
    fileConcurrencyController.recover(new ErrorCallback() {
      @Override
      public void onError() {
        StatusMessage fatal = new StatusMessage(appContext.getStatusManager(), MessageType.FATAL,
            "There was a problem synchronizing with the server.");
        fatal.addAction(StatusMessage.RELOAD_ACTION);
        fatal.setDismissable(false);
        fatal.fire();
      }
    });
  }

  void handleTransportReconnectedSuccessfully() {
    recover();
  }
}
