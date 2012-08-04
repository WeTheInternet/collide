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

import javax.annotation.Nullable;

import com.google.collide.client.AppContext;
import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.code.Participant;
import com.google.collide.client.code.ParticipantModel;
import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.selection.CursorView;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.dto.client.DtoClientImpls.DocumentSelectionImpl;
import com.google.collide.dto.client.DtoClientImpls.FilePositionImpl;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.util.JsonCollections;

import elemental.util.Timer;


/**
 * A controller for the collaborators' cursors.
 */
class CollaboratorCursorController {

  private class CollaboratorState {
    private Anchor anchor;
    private CursorView cursorView;
    private Timer inactiveTimer = new Timer() {
      @Override
      public void run() {
        cursorView.setVisibility(false);
      }
    };
    
    CollaboratorState(Anchor anchor, CursorView cursorView) {
      this.anchor = anchor;
      this.cursorView = cursorView;
    }

    void markAsActive() {
      inactiveTimer.schedule(INACTIVE_DELAY_MS);
      
      if (!cursorView.isVisible()) {
        cursorView.setVisibility(true);
      }
    }
  }
  
  private static final AnchorType COLLABORATOR_CURSOR_ANCHOR_TYPE = AnchorType.create(
      CollaboratorCursorController.class, "collaboratorCursor");

  private static final int INACTIVE_DELAY_MS = 10 * 1000;
  
  private final AppContext appContext;
  private final Buffer buffer;
  private final JsonStringMap<CollaboratorState> collaboratorStates = JsonCollections.createMap();
  private final Document document;
  private final ParticipantModel participantModel;
  private final ParticipantModel.Listener participantModelListener =
      new ParticipantModel.Listener() {
        @Override
        public void participantRemoved(Participant participant) {
          CollaboratorState collaboratorState = collaboratorStates.get(participant.getUserId());
          if (collaboratorState == null) {
            return;
          }

          document.getAnchorManager().removeAnchor(collaboratorState.anchor);
        }

        @Override
        public void participantAdded(Participant participant) {
          CollaboratorState collaboratorState = collaboratorStates.get(participant.getUserId());
          if (collaboratorState == null) {
            return;
          }
          
          collaboratorState.cursorView.setColor(participant.getColor());
        }
      };

  CollaboratorCursorController(AppContext appContext, Document document, Buffer buffer,
      ParticipantModel participantModel, JsonStringMap<DocumentSelection> collaboratorSelections) {
    this.appContext = appContext;
    this.buffer = buffer;
    this.document = document;
    this.participantModel = participantModel;

    participantModel.addListener(participantModelListener);
    
    collaboratorSelections.iterate(new IterationCallback<DocumentSelection>() {
      @Override
      public void onIteration(String userId, DocumentSelection selection) {
        try {
          handleSelectionChangeWithUserId(userId, selection);
        } catch (Throwable t) {
          /*
           * TODO: There's a known bug that if a document is not open in the editor,
           * our cached collaborator selections won't get transformed with incoming doc ops. This
           * means that it's possible the selection is out of the actual document's range (either
           * line number or column is too big.)
           */
        }
      }
    });
  }

  void handleSelectionChange(String clientId, @Nullable DocumentSelection selection) {
    handleSelectionChangeWithUserId(participantModel.getUserId(clientId), selection);
  }
  
  private void handleSelectionChangeWithUserId(String userId,
      @Nullable DocumentSelection selection) {
    
    if (BootstrapSession.getBootstrapSession().getUserId().equals(userId)) {
      // Do not draw a collaborator cursor for our user
      return;
    }
    
    // If a user is typing, a selection will likely be null (since it isn't an explicit move)
    if (selection != null) {
      LineInfo lineInfo =
          document.getLineFinder().findLine(selection.getCursorPosition().getLineNumber());
      int cursorColumn = selection.getCursorPosition().getColumn();
  
      if (!collaboratorStates.containsKey(selection.getUserId())) {
        createCursor(selection.getUserId(), lineInfo.line(), lineInfo.number(), cursorColumn);
      } else {
        document.getAnchorManager().moveAnchor(collaboratorStates.get(selection.getUserId()).anchor,
            lineInfo.line(), lineInfo.number(), cursorColumn);
      }
    }
    
    if (collaboratorStates.containsKey(userId)) {
      collaboratorStates.get(userId).markAsActive();
    }   
  }

  void teardown() {
    participantModel.removeListener(participantModelListener);
    
    collaboratorStates.iterate(new IterationCallback<CollaboratorState>() {
      @Override
      public void onIteration(String key, CollaboratorState collaboratorState) {
        document.getAnchorManager().removeAnchor(collaboratorState.anchor);
      }
    });
  }

  private void createCursor(final String userId, Line line, int lineNumber, int column) {
    final CursorView cursorView = CursorView.create(appContext, false);
    cursorView.setVisibility(true);

    Participant participant = participantModel.getParticipantByUserId(userId);
    if (participant != null) {
      /*
       * If the participant exists already, set his color (otherwise the
       * participant model listener will set the color)
       */
      cursorView.setColor(participant.getColor());
    }

    Anchor anchor =
        document.getAnchorManager().createAnchor(COLLABORATOR_CURSOR_ANCHOR_TYPE, line, lineNumber,
            column);
    anchor.setRemovalStrategy(RemovalStrategy.SHIFT);
    buffer.addAnchoredElement(anchor, cursorView.getElement());

    collaboratorStates.put(userId, new CollaboratorState(anchor, cursorView));
  }

  public JsonStringMap<DocumentSelection> getSelectionsMap() {
    final JsonStringMap<DocumentSelection> map = JsonCollections.createMap();
    
    collaboratorStates.iterate(
        new IterationCallback<CollaboratorCursorController.CollaboratorState>() {
          @Override
          public void onIteration(String userId, CollaboratorState state) {
            FilePositionImpl basePosition = FilePositionImpl.make().setColumn(
                state.anchor.getColumn()).setLineNumber(state.anchor.getLineNumber());
            FilePositionImpl cursorPosition = FilePositionImpl.make().setColumn(
                state.anchor.getColumn()).setLineNumber(state.anchor.getLineNumber());
            DocumentSelectionImpl selection = DocumentSelectionImpl.make()
                .setBasePosition(basePosition).setCursorPosition(cursorPosition).setUserId(userId);

            map.put(userId, selection);
          }
        });
    
    return map;
  }
}
