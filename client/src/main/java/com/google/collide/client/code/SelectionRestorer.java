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

package com.google.collide.client.code;

import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.PathUtil;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.util.LineUtils;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import elemental.js.util.JsMapFromStringTo;

/**
 * A simple class for saving/restoring the user's selection as he moves between
 * files. This only persists the saved selections in the client's memory, not to
 * the server. This will try to restore the relative position of the cursor on
 * the screen too (e.g. if the cursor was 30px from the top of the viewport, it
 * will stay 30px from the top of the viewport when restored).
 *
 * The client must call {@link #onBeforeDocumentChanged()} and
 * {@link #onDocumentChanged(PathUtil)}.
 */
class SelectionRestorer {

  private static class Selection {
    final int baseColumn;
    final int baseLineNumber;
    final int cursorColumn;
    final int cursorLineNumber;
    /**
     * Tracks the gap between the top of the viewport and the top of the
     * cursor's line
     */
    final int cursorScrollTopOffset;

    Selection(int baseLineNumber, int baseColumn, int cursorLineNumber, int cursorColumn,
        int cursorScrollTopOffset) {
      this.baseColumn = baseColumn;
      this.baseLineNumber = baseLineNumber;
      this.cursorColumn = cursorColumn;
      this.cursorLineNumber = cursorLineNumber;
      this.cursorScrollTopOffset = cursorScrollTopOffset;
    }
  }

  private final Editor editor;
  private String fileEditSessionKey;
  /** Map from file edit session key to {@link Selection} */
  private final JsMapFromStringTo<Selection> selections = JsMapFromStringTo.create();

  SelectionRestorer(Editor editor) {
    this.editor = editor;
  }

  void onBeforeDocumentChanged() {
    saveSelection();
  }

  private void saveSelection() {
    if (fileEditSessionKey == null) {
      return;
    }

    SelectionModel selectionModel = editor.getSelection();
    Buffer buffer = editor.getBuffer();

    int cursorLineNumber = selectionModel.getCursorLineNumber();
    int cursorScrollTopOffset = buffer.calculateLineTop(cursorLineNumber) - buffer.getScrollTop();

    selections.put(fileEditSessionKey, new Selection(selectionModel.getBaseLineNumber(),
        selectionModel.getBaseColumn(), cursorLineNumber, selectionModel.getCursorColumn(),
        cursorScrollTopOffset));
  }

  void onDocumentChanged(String fileEditSessionKey) {
    this.fileEditSessionKey = fileEditSessionKey;
    restoreSelection();
  }

  private void restoreSelection() {
    if (fileEditSessionKey == null) {
      return;
    }

    final Selection selection = selections.get(fileEditSessionKey);
    if (selection == null) {
      return;
    }

    Document document = editor.getDocument();
    LineFinder lineFinder = document.getLineFinder();
    
    LineInfo baseLineInfo =
        lineFinder.findLine(Math.min(selection.baseLineNumber, document.getLastLineNumber()));
    int baseColumn = LineUtils.rubberbandColumn(baseLineInfo.line(), selection.baseColumn);

    final LineInfo cursorLineInfo =
      lineFinder.findLine(Math.min(selection.cursorLineNumber, document.getLastLineNumber()));
    int cursorColumn = LineUtils.rubberbandColumn(cursorLineInfo.line(), selection.cursorColumn);
    
    editor.getSelection().setSelection(baseLineInfo, baseColumn, cursorLineInfo, cursorColumn);    

    // Defer to match editor's initially deferred scrolling
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        Buffer buffer = editor.getBuffer();
        int targetScrollTop = buffer.calculateLineTop(cursorLineInfo.number())
            - selection.cursorScrollTopOffset;
        buffer.setScrollTop(Math.max(0, targetScrollTop));
      }
    });
  }
}
