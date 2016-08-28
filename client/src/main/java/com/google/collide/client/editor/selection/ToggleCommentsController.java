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

package com.google.collide.client.editor.selection;

import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.common.base.Preconditions;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Utility that comments / uncomments selected lines.
 *
 */
public class ToggleCommentsController {

  private final RegExp commentChecker;

  private final String commentHead;

  ToggleCommentsController(RegExp commentChecker, String commentHead) {
    this.commentChecker = commentChecker;
    this.commentHead = commentHead;
  }

  void processLines(DocumentMutator documentMutator, SelectionModel selection) {
    boolean moveDown = !selection.hasSelection();
    Position[] selectionRange = selection.getSelectionRange(false);
    int initialColumn = selectionRange[0].getColumn();
    Line terminator = selectionRange[1].getLine();
    if (selectionRange[1].getColumn() != 0 || !selection.hasSelection()) {
      terminator = terminator.getNextLine();
    }

    int lineNumber = selectionRange[0].getLineNumber();
    Line current = selectionRange[0].getLine();

    if (canUncommentAll(current, terminator)) {
      int headLength = commentHead.length();
      while (current != terminator) {
        int pos = current.getText().indexOf(commentHead);
        documentMutator.deleteText(current, lineNumber, pos, headLength);
        lineNumber++;
        current = current.getNextLine();
      }
    } else {
      while (current != terminator) {
        documentMutator.insertText(current, lineNumber, 0, commentHead, false);
        lineNumber++;
        current = current.getNextLine();
      }
    }

    if (moveDown) {
      moveCursorDown(selection, initialColumn);
    }
  }

  /**
   * Check that all lines between begin (inclusive) and end (exclusive) are
   * commented.
   *
   * @param end {@code null} to check to document end
   */
  private boolean canUncommentAll(Line begin, Line end) {
    Line current = begin;
    while (current != end) {
      Preconditions.checkNotNull(current, "hasn't met terminator before document end");
      if (!commentChecker.test(current.getText())) {
        return false;
      }

      current = current.getNextLine();
    }
    return true;
  }

  private void moveCursorDown(SelectionModel selection, int initialColumn) {
    Line line = selection.getCursorLine().getNextLine();
    if (line == null) {
      return;
    }
    int lineNumber = selection.getCursorLineNumber() + 1;

    String text = line.getText();
    int lineLength = text.length();
    if (text.endsWith("\n")) {
      lineLength--;
    }
    int column = Math.min(initialColumn, lineLength);
    selection.setCursorPosition(new LineInfo(line, lineNumber), column);
  }
}
