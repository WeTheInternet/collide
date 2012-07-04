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

package com.google.collide.shared.document.util;

import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.PositionOutOfBoundsException;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.util.TextUtils;
import com.google.common.base.Preconditions;

/**
 * Utility methods for line manipulations.
 */
public final class LineUtils {

  /**
   * Interface for a visitor of lines.
   */
  public interface LineVisitor {
    /**
     * @return true to continue visiting more lines, false to abort the visit
     */
    boolean accept(Line line, int lineNumber, int beginColumn, int endColumn);
  }

  /*
   * TODO: Move to newly created PositionUtils (in a future
   * standalone CL since this has a lot of callers)
   */
  /**
   * Returns a negative number if {@code a} is earlier than {@code b}, a
   * positive number if {@code a} is later than {@code b}, and zero if they are
   * the same.
   */
  public static int comparePositions(int aLineNumber, int aColumn, int bLineNumber, int bColumn) {
    int lineNumberDelta = aLineNumber - bLineNumber;
    return lineNumberDelta != 0 ? lineNumberDelta : aColumn - bColumn;
  }

  /**
   * Returns a cached {@link LineInfo} for this {@link Line} if it exists, or
   * null.
   */
  public static LineInfo getCachedLineInfo(Line line) {
    Anchor anchorWithLineNumber =
        line.getDocument().getAnchorManager().findAnchorWithLineNumber(line);
    return anchorWithLineNumber != null ? anchorWithLineNumber.getLineInfo() : null;
  }

  /**
   * Returns a cached line number for this line if it exists, or -1.
   */
  public static int getCachedLineNumber(Line line) {
    Anchor anchorWithLineNumber =
        line.getDocument().getAnchorManager().findAnchorWithLineNumber(line);
    return anchorWithLineNumber != null ? anchorWithLineNumber.getLineNumber() : -1;
  }

  /**
   * Returns the last column (inclusive) of the line, or -1 if the line is
   * empty.
   */
  public static int getLastColumn(Line line) {
    return line.getText().length() - 1;
  }
  
  /**
   * Returns the first column for the given line where a cursor can be placed.
   * This will skip any strange control characters or zero-width characters that
   * are prefixing a line.
   */
  public static int getFirstCursorColumn(Line line) {
    String lineText = line.getText();
    return TextUtils.findNonMarkNorOtherCharacter(lineText, -1);
  }

  /**
   * Returns the max column for the given line where a cursor can be placed.
   * This can also be thought of as the index after the last character in the
   * line (the newline does not count as a character here.)
   */
  public static int getLastCursorColumn(Line line) {
    // "- 1" because we cannot position after the invisible newline
    return getLastCursorColumn(line.getText());
  }
  
  /**
   * @see #getLastCursorColumn(Line)
   */
  public static int getLastCursorColumn(String lineText) {
    return lineText.endsWith("\n") ? lineText.length() - 1 : lineText.length();
  }

  /**
   * Iterates to and returns the line that is {@code relativePosition} away from
   * the given {@code line}. If {@code relativePosition} is large, consider
   * using {@link LineFinder}.
   */
  public static Line getLine(Line line, int relativePosition) {
    if (relativePosition > 0) {
      for (; line != null && relativePosition > 0; relativePosition--) {
        line = line.getNextLine();
      }
    } else {
      for (; line != null && relativePosition < 0; relativePosition++) {
        line = line.getPreviousLine();
      }
    }

    return line;
  }

  /**
   * Gets the number of characters (including newlines) in the given inclusive
   * range.
   */
  public static int getTextCount(Line beginLine, int beginColumn, Line endLine, int endColumn) {
    Preconditions.checkArgument(beginLine.isAttached(), "beginLine must be attached");
    Preconditions.checkArgument(endLine.isAttached(), "endLine must be attached");

    if (beginLine == endLine) {
      return endColumn - beginColumn + 1;
    }

    int count = beginLine.getText().length() - beginColumn;
    Line line = beginLine.getNextLine();
    while (line != null && line != endLine) {
      count += line.getText().length();
      line = line.getNextLine();
    }
    if (line == null) {
      throw new IndexOutOfBoundsException("can't find endLine");
    }
    count += endColumn + 1;

    return count;
  }

  /**
   * Gets the text from the beginning position to the end position (inclusive).
   */
  public static String getText(Line beginLine, int beginColumn, Line endLine, int endColumn) {

    if (beginLine == endLine) {
      return beginLine.getText().substring(beginColumn, endColumn + 1);
    }

    StringBuilder s = new StringBuilder(beginLine.getText().substring(beginColumn));
    Line line = beginLine.getNextLine();
    while (line != null && line != endLine) {
      s.append(line.getText());
      line = line.getNextLine();
    }
    if (line == null) {
      throw new IndexOutOfBoundsException();
    }
    s.append(endLine.getText().substring(0, endColumn + 1));

    return s.toString();
  }

  /**
   * Returns a line number in the range of the document closest to the
   * {@code targetLineNumber}.
   */
  public static int getValidLineNumber(int targetLineNumber, Document document) {
    int lastLineNumber = document.getLastLineNumber();
    if (targetLineNumber <= 0) {
      return 0;
    } else if (targetLineNumber >= lastLineNumber) {
      return lastLineNumber;
    } else {
      return targetLineNumber;
    }
  }

  /**
   * Returns the target column, or the max column for the line if the target
   * column is too large, or 0 if the target column is negative.
   */
  public static int rubberbandColumn(Line line, int targetColumn) {
    return (int) rubberbandColumn(line, (double) targetColumn);
  }

  public static double rubberbandColumn(Line line, double targetColumn) {
    if (targetColumn < 0) {
      return 0;
    }

    int maxColumnFromLineText = getLastCursorColumn(line);
    return maxColumnFromLineText < targetColumn ? maxColumnFromLineText : targetColumn;
  }

  static Position getPositionBackwards(Line line, int lineNumber, int column,
      int numCharsToTraverse) {

    while (numCharsToTraverse > 0) {
      int remainingCharsOnThisLine = column;
      /*
       * In the case that remainingCharsOnLine == numCharsToTraverse, we want to
       * move to the first column of this line
       */
      if (remainingCharsOnThisLine < numCharsToTraverse) {
        // Skip over this line
        line = line.getPreviousLine();
        if (line == null) {
          throw new PositionOutOfBoundsException(
              "Reached the document beginning, but still wanted to go " + numCharsToTraverse
                  + " characters backwards.");
        }
        lineNumber--;
        column = line.getText().length();
        numCharsToTraverse -= remainingCharsOnThisLine;

      } else {
        // It's within this line
        column -= numCharsToTraverse;
        numCharsToTraverse = 0;
      }
    }

    return new Position(new LineInfo(line, lineNumber), column);
  }

  static Position getPositionForward(Line line, int lineNumber, int column,
      int numCharsToTraverse) {

    while (numCharsToTraverse > 0) {
      int remainingCharsOnThisLine = line.getText().length() - column;
      /*
       * In the case that remainingCharsOnLine == numCharsToTraverse, we want to
       * move to the first column of the next line
       */
      if (remainingCharsOnThisLine <= numCharsToTraverse) {
        if (line.getNextLine() == null) {
          /*
           * For the last line we have no newline character and want to move to
           * the line end
           */
          if (remainingCharsOnThisLine < numCharsToTraverse) {
            throw new PositionOutOfBoundsException(
                "Reached the document end, but still wanted to go "
                    + (numCharsToTraverse - remainingCharsOnThisLine) + " characters forward.");
          } else {
            column += numCharsToTraverse;
          }
        } else {
          // Skip over this line
          line = line.getNextLine();
          lineNumber++;
          column = 0;
        }
        numCharsToTraverse -= remainingCharsOnThisLine;
      } else {
        // It's within this line
        column += numCharsToTraverse;
        numCharsToTraverse = 0;
      }
    }

    return new Position(new LineInfo(line, lineNumber), column);
  }
}
