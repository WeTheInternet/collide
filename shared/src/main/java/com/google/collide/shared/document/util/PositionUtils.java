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

import static com.google.collide.shared.document.util.LineUtils.getPositionBackwards;
import static com.google.collide.shared.document.util.LineUtils.getPositionForward;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.util.LineUtils.LineVisitor;
import com.google.collide.shared.util.JsonCollections;

/**
 * Utility methods relating to {@link Position}.
 */
public final class PositionUtils {

  public static int compare(Position a, Position b) {
    return LineUtils.comparePositions(a.getLineNumber(), a.getColumn(), b.getLineNumber(),
        b.getColumn());
  }

  public static Position[] getIntersection(Position[] a, Position[] b) {
    // Ensure that A starts before B
    if (compare(a[0], b[0]) > 0) {
      return getIntersection(b, a);
    }

    if (compare(b[0], a[1]) > 0) {
      // No intersection
      return null;
    }

    Position earlierEnd = compare(a[1], b[1]) < 0 ? a[1] : b[1];
    return new Position[] {b[0], earlierEnd};
  }

  public static JsonArray<Position[]> getDifference(Position[] a, Position[] b) {
    // Ensure that A starts before B
    int abStartComparison = compare(a[0], b[0]);
    if (abStartComparison > 0) {
      return getDifference(b, a);
    }

    if (compare(b[0], a[1]) > 0) {
      // No intersection, so return both
      return JsonCollections.createArray(a, b);
    }

    JsonArray<Position[]> difference = JsonCollections.createArray();
    if (abStartComparison != 0) {
      // Range from the start of A (inclusive) to the start of B (exclusive)
      difference.add(new Position[] {a[0], getPosition(b[0], -1)});
    }

    int abEndComparison = compare(a[1], b[1]);
    if (abEndComparison != 0) {
      Position earlierEnd = abEndComparison < 0 ? a[1] : b[1];
      Position laterEnd = abEndComparison < 0 ? b[1] : a[1];
      difference.add(new Position[] {getPosition(earlierEnd, 1), laterEnd});
    }

    return difference;
  }

  /**
   * Returns the position (line and column) which is {@code relativeOffset}
   * characters (including newlines) away from the given starting position (
   * {@code line} and {@code column}).
   *
   */
  public static Position getPosition(Line line, int lineNumber, int column, int relativeOffset) {
    return relativeOffset < 0 ?
        getPositionBackwards(line, lineNumber, column, -relativeOffset)
        : getPositionForward(line, lineNumber, column, relativeOffset);
  }

  /**
   * @see #getPosition(com.google.collide.shared.document.Line, int, int, int)
   */
  public static Position getPosition(Position position, int relativeOffset) {
    return relativeOffset < 0 ? getPositionBackwards(position.getLine(), position.getLineNumber(),
        position.getColumn(), -relativeOffset) : getPositionForward(position.getLine(),
        position.getLineNumber(), position.getColumn(), relativeOffset);
  }

/*  public static Position getPosition(Position start, int relativeOffset) {
    return getPosition(start.getLine(), start.getLineNumber(), start.getColumn(),
        relativeOffset);
  }*/

  /**
   * Visit each line in order from start to end, calling lineVisitor.accept()
   * for each line. If lineVisitor.accept returns false, stop execution, else
   * continue until end is reached.
   *
   * This method can search backwards if start is later than end. In this case,
   * ensure that start's column is exclusive.
   *
   * @param lineVisitor the {@code endColumn} given to the visitor is exclusive
   * @param start if searching backwards, the column should be exclusive.
   * @param end if searching forward, the column should be exclusive. If
   *        searching backwards, the column should be inclusive
   */
  public static void visit(LineVisitor lineVisitor, Position start, Position end) {

    if (start.getLine().equals(end.getLine())) {
      lineVisitor
          .accept(start.getLine(), start.getLineNumber(), start.getColumn(), end.getColumn());
      return;
    }

    boolean iterateForward = compare(start, end) < 0;
    if (iterateForward) {
      if (!lineVisitor.accept(start.getLine(), start.getLineNumber(), start.getColumn(), start
          .getLine().length())) {
        return;
      }
    } else {
      if (!lineVisitor.accept(start.getLine(), start.getLineNumber(), 0, start.getColumn())) {
        return;
      }
    }

    LineInfo curLine = start.getLineInfo();
    curLine.moveTo(iterateForward);

    while (curLine.line() != end.getLine()) {
      if (!lineVisitor.accept(curLine.line(), curLine.number(), 0, curLine.line().length())) {
        return;
      }

      if (!curLine.moveTo(iterateForward)) {
        throw new IllegalStateException(
            "Could not find the requested ending line while visiting position range");
      }
    }

    if (iterateForward) {
      lineVisitor.accept(end.getLine(), end.getLineNumber(), 0, end.getColumn());
    } else {
      lineVisitor.accept(end.getLine(), end.getLineNumber(), end.getColumn(),
 end.getLine()
          .length());
    }
  }

  private PositionUtils() {
  }
}
