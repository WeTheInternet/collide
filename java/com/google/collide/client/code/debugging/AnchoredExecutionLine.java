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

package com.google.collide.client.code.debugging;

import com.google.collide.client.editor.Editor;
import com.google.collide.client.testing.DebugAttributeSetter;
import com.google.collide.client.util.Elements;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;

import elemental.dom.Element;

/**
 * Handles an <i>execution line</i> anchored into a document.
 *
 * <p>Execution line is a line in a script where debugger stopped. Each call
 * frame in the call stack has exactly one execution line, and execution line
 * of the topmost call frame is where the debugger stopped last.
 *
 */
class AnchoredExecutionLine {

  private static final AnchorType EXECUTION_LINE_ANCHOR_TYPE = AnchorType.create(
      AnchoredExecutionLine.class, "executionLine");

  static AnchoredExecutionLine create(Editor editor, int lineNumber, String bufferLineClassName,
      String gutterLineClassName) {
    return new AnchoredExecutionLine(editor, lineNumber, bufferLineClassName, gutterLineClassName);
  }

  private final Editor editor;
  private final Document document;
  private final Anchor lineExecutionAnchor;
  private final Element bufferExecutionLine;
  private final Element gutterExecutionLine;

  private AnchoredExecutionLine(Editor editor, int lineNumber, String bufferLineClassName,
      String gutterLineClassName) {
    this.editor = editor;
    document = editor.getDocument();
    lineExecutionAnchor = createExecutionLineAnchor(document, lineNumber);

    bufferExecutionLine = Elements.createDivElement(bufferLineClassName);
    new DebugAttributeSetter().add("linenumber", String.valueOf(lineNumber + 1)).on(
        bufferExecutionLine);
    editor.getBuffer().addAnchoredElement(lineExecutionAnchor, bufferExecutionLine);

    gutterExecutionLine = Elements.createDivElement(gutterLineClassName);
    new DebugAttributeSetter().add("linenumber", String.valueOf(lineNumber + 1)).on(
        gutterExecutionLine);
    editor.getLeftGutter().addAnchoredElement(lineExecutionAnchor, gutterExecutionLine);
  }

  void teardown() {
    editor.getBuffer().removeAnchoredElement(lineExecutionAnchor, bufferExecutionLine);
    editor.getLeftGutter().removeAnchoredElement(lineExecutionAnchor, gutterExecutionLine);
    document.getAnchorManager().removeAnchor(lineExecutionAnchor);
  }

  private static Anchor createExecutionLineAnchor(Document document, int lineNumber) {
    LineInfo lineInfo = document.getLineFinder().findLine(lineNumber);
    Anchor anchor = document.getAnchorManager().createAnchor(EXECUTION_LINE_ANCHOR_TYPE,
        lineInfo.line(), lineInfo.number(), AnchorManager.IGNORE_COLUMN);
    anchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    return anchor;
  }
}
