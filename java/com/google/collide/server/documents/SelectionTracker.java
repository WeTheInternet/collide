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

package com.google.collide.server.documents;

import com.google.collide.dto.DocumentSelection;
import com.google.collide.dto.server.DtoServerImpls.DocumentSelectionImpl;
import com.google.collide.dto.server.DtoServerImpls.FilePositionImpl;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Helper that tracks the selection (cursor and base positions) for each user.
 * 
 */
public class SelectionTracker {

  private class UserSelection {
    private final String clientId;

    private VersionedDocument document;
    private String resourceId;
    private Anchor cursorAnchor;
    private Anchor baseAnchor;

    private UserSelection(String clientId) {
      this.clientId = clientId;
    }

    private synchronized void teardown() {
      removeAnchors();
    }

    private synchronized void markActive(
        final String resourceId, VersionedDocument document, DocumentSelection documentSelection) {

      if (this.document != document) {
        /*
         * The user switched files or the VersionedDocument instance swapped underneath us (in the
         * latter case, just checking the equality of FileEditSessionKeys would not be enough)
         */
        removeAnchors();

        this.resourceId = resourceId;
        this.document = document;
      }

      if (documentSelection != null) {
        // Cursor/selection has moved via explicit movement action
        moveOrCreateAnchors(document, documentSelection);
      }
    }

    private synchronized void moveOrCreateAnchors(
        VersionedDocument document, DocumentSelection documentSelection) {

      if (cursorAnchor != null) {
        document.moveAnchor(cursorAnchor, documentSelection.getCursorPosition().getLineNumber(),
            documentSelection.getCursorPosition().getColumn());
        document.moveAnchor(baseAnchor, documentSelection.getBasePosition().getLineNumber(),
            documentSelection.getBasePosition().getColumn());
      } else {
        cursorAnchor = document.addAnchor(
            CURSOR_ANCHOR_TYPE, documentSelection.getCursorPosition().getLineNumber(),
            documentSelection.getCursorPosition().getColumn());
        cursorAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);

        baseAnchor = document.addAnchor(
            BASE_ANCHOR_TYPE, documentSelection.getBasePosition().getLineNumber(),
            documentSelection.getBasePosition().getColumn());
        baseAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);
      }
    }

    private synchronized void removeAnchors() {
      if (cursorAnchor != null) {
        document.removeAnchor(cursorAnchor);
        cursorAnchor = null;
      }

      if (baseAnchor != null) {
        document.removeAnchor(baseAnchor);
        baseAnchor = null;
      }
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof UserSelection && clientId.equals(((UserSelection) obj).clientId);
    }

    @Override
    public int hashCode() {
      return clientId.hashCode();
    }
  }

  private static final AnchorType CURSOR_ANCHOR_TYPE =
      AnchorType.create(SelectionTracker.class, "cursor");

  private static final AnchorType BASE_ANCHOR_TYPE =
      AnchorType.create(SelectionTracker.class, "base");

  /**
   * All active selections, keyed by the user's gaia ID.
   */
  private final Map<String, UserSelection> userSelections = Maps.newHashMap();

  @VisibleForTesting
  SelectionTracker() {
    // TODO: Listen for client disconnections to remove selections.
  }

  /**
   * Releases all resources.
   */
  public void close() {
    for (UserSelection selection : userSelections.values()) {
      selection.teardown();
    }
  }

  /**
   * Called when the selection for a user changes.
   * 
   * @param clientId the ID for the user's tab
   * @param resourceId the key for the file edit session that holds the active selection
   * @param documentSelection the position of the selection after any mutations currently being
   *        processed
   */
  public void selectionChanged(String clientId, String resourceId, VersionedDocument document,
      DocumentSelection documentSelection) {

    UserSelection selection = userSelections.get(clientId);
    if (selection == null) {
      selection = new UserSelection(clientId);
      userSelections.put(clientId, selection);
    }

    selection.markActive(resourceId, document, documentSelection);
  }

  public List<DocumentSelection> getDocumentSelections(String resourceId) {

    List<DocumentSelection> selections = Lists.newArrayList();
    for (UserSelection userSelection : userSelections.values()) {
      if (userSelection.resourceId.equals(resourceId)) {
        Anchor cursorAnchor = userSelection.cursorAnchor;
        Anchor baseAnchor = userSelection.baseAnchor;
        if (cursorAnchor != null && baseAnchor != null) {
          FilePositionImpl basePosition = FilePositionImpl.make()
              .setColumn(baseAnchor.getColumn()).setLineNumber(baseAnchor.getLineNumber());
          FilePositionImpl cursorPosition = FilePositionImpl.make().setColumn(
              cursorAnchor.getColumn()).setLineNumber(cursorAnchor.getLineNumber());
          DocumentSelectionImpl selection = DocumentSelectionImpl.make()
              .setUserId(userSelection.clientId).setBasePosition(basePosition)
              .setCursorPosition(cursorPosition);
          selections.add(selection);
        }
      }
    }

    return selections;
  }

  public void removeSelection(String clientId) {
    UserSelection selection = userSelections.remove(clientId);

    if (selection != null) {
      selection.teardown();
    }
  }
}
