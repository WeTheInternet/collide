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
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.dto.client.DtoClientImpls.ClientToServerDocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.DocumentSelectionImpl;
import com.google.collide.dto.client.DtoClientImpls.FilePositionImpl;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.ListenerRegistrar.Remover;

/**
 * A class that tracks local cursor changes and eventually will aid in
 * broadcasting them to the collaborators.
 *
 */
class LocalCursorTracker
    implements SelectionModel.CursorListener, ClientToServerDocOpCreationParticipant {

  private final DocumentCollaborationController collaborationController;
  private boolean hasExplicitCursorChange;
  private final SelectionModel selectionModel;
  private final Remover cursorListenerRemover;

  LocalCursorTracker(
      DocumentCollaborationController collaborationController, SelectionModel selectionModel) {
    this.collaborationController = collaborationController;
    this.selectionModel = selectionModel;

    cursorListenerRemover = selectionModel.getCursorListenerRegistrar().add(this);
  }

  @Override
  public void onCreateClientToServerDocOp(ClientToServerDocOpImpl message) {

    if (!hasExplicitCursorChange) {
      return;
    }

    FilePositionImpl basePosition =
        FilePositionImpl.make().setColumn(selectionModel.getBaseColumn()).setLineNumber(
            selectionModel.getBaseLineNumber());
    FilePositionImpl cursorPosition =
        FilePositionImpl.make().setColumn(selectionModel.getCursorColumn()).setLineNumber(
            selectionModel.getCursorLineNumber());

    DocumentSelectionImpl selection =
        DocumentSelectionImpl.make().setBasePosition(basePosition).setCursorPosition(
            cursorPosition).setUserId(BootstrapSession.getBootstrapSession().getUserId());

    message.setSelection(selection);

    // Reset
    hasExplicitCursorChange = false;
  }

  @Override
  public void onCursorChange(LineInfo lineInfo, int column, boolean isExplicitChange) {
    if (isExplicitChange) {
      hasExplicitCursorChange = true;
      collaborationController.ensureQueuedDocOp();
    }
  }

  /**
   * Forces the next client to server doc op to have our selection included.
   */
  void forceSendingSelection() {
    hasExplicitCursorChange = true;
  }
  
  void teardown() {
    cursorListenerRemover.remove();
  }
}
