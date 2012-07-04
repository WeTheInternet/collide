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

package com.google.collide.shared.document.anchor;

import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.document.util.PositionUtils;
import com.google.common.base.Preconditions;

/**
 * Utility methods relating to anchors.
 */
public final class AnchorUtils {

  /**
   * Returns a negative number if {@code a} is earlier than {@code b}, a
   * positive number if the inverse, and zero if they are positioned the same.
   *
   * Comparing an anchor that ignores either a line number or column to another
   * anchor that does not ignore that same property is invalid and will have
   * strange results.
   *
   * @param a an anchor with both a line number and a column
   * @param b an anchor with both a line number and a column
   */
  public static int compare(Anchor a, Anchor b) {
    assert (a.hasLineNumber() == b.hasLineNumber());
    assert ((a.getColumn() == AnchorManager.IGNORE_COLUMN) == (b.getColumn() == 
        AnchorManager.IGNORE_COLUMN));

    return LineUtils.comparePositions(a.getLineNumber(), a.getColumn(), b.getLineNumber(),
        b.getColumn());
  }

  /**
   * @param a an anchor with both a line number and a column
   * @see #compare(Anchor, Anchor)
   */
  public static int compare(Anchor a, int bLineNumber, int bColumn) {
    assert a.hasLineNumber();
    assert (a.getColumn() != AnchorManager.IGNORE_COLUMN);

    return LineUtils.comparePositions(a.getLineNumber(), a.getColumn(), bLineNumber, bColumn);
  }

  /**
   * Replace all text between line anchors {@code begin} and {@code end}
   * with {@code text}.
   */
  public static void setTextBetweenAnchors(String text, Anchor begin, Anchor end,
      DocumentMutator documentMutator) {
    Preconditions.checkArgument(begin.isAttached(), "begin must be attached");
    Preconditions.checkArgument(begin.isLineAnchor(), "begin must be line anchor");
    Preconditions.checkArgument(end.isLineAnchor(), "end must be line anchor");
    Preconditions.checkArgument(end.isAttached(), "end must be attached");
    Preconditions.checkArgument(
        begin.getLineNumber() <= end.getLineNumber(), "begin line below end line");

    // TODO: Fix same-line text replacement.
    LineInfo topLineInfo = begin.getLineInfo();
    Line topLine = topLineInfo.line();
    Line bottomLine = end.getLine();

    /*
     * At the very end of the document, the text being inserted will have a
     * trailing "\n" that needs to be deleted to avoid an empty line at the
     * end.
     */
    boolean deleteEndingNewline = !bottomLine.getText().endsWith("\n");
    if (!text.endsWith("\n")) {
      text = text + "\n";
    }
    // Delete all of the existing text, minus the last newline.
    int deleteCount =
        LineUtils.getTextCount(topLine, 0, bottomLine, bottomLine.getText().length() - 
          (deleteEndingNewline ? 0 : 1));

    documentMutator.insertText(topLine, topLineInfo.number(), 0, text, false);
    Position endOfInsertion =
        PositionUtils.getPosition(topLine, topLineInfo.number(), 0, text.length() - 1);
    documentMutator.deleteText(endOfInsertion.getLine(), endOfInsertion.getColumn(), deleteCount);
  }

  private AnchorUtils() {
  }

  public static void visitAnchorsOnLine(Line line, AnchorManager.AnchorVisitor visitor) {
    AnchorList anchors = AnchorManager.getAnchorsOrNull(line);
    if (anchors == null) {
      return;
    }

    for (int i = 0; i < anchors.size(); i++) {
      visitor.visitAnchor(anchors.get(i));
    }
  }
}
