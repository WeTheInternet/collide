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

package com.google.collide.shared.document;

import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.util.LineUtils;

/*
 * Implementation notes:
 *
 * - This class needs to efficiently resolve line numbers or lines. To do this,
 * it relies heavily on anchors with line numbers in the document. Basically, it
 * finds the closest anchor with line number, and then iterates to the line of
 * interest. Most document edits will originate on a line with an anchor
 * (local/collaborator cursors use anchors), so the common case is fast.
 */
/**
 * Helper to efficiently resolve a line number given the line, or vice versa.
 */
public class LineFinder {

  private final Document document;

  LineFinder(Document document) {
    this.document = document;
  }

  /**
   * Finds the closest {@link LineInfo} for the given line number (must be
   * within the document range). Use {@link #findLine(LineInfo, int)} if you
   * know of a good starting point for the search.
   */
  public LineInfo findLine(int targetLineNumber) {
    if (targetLineNumber >= document.getLineCount()) {
      throw new IndexOutOfBoundsException("Asking for " + targetLineNumber
          + " but document length is " + document.getLineCount());
    }

    int distanceFromFirstLine = targetLineNumber;
    int distanceFromLastLine = document.getLineCount() - targetLineNumber - 1;

    int distanceFromClosestLineAnchor;
    Anchor closestLineAnchor =
        document.getAnchorManager().findClosestAnchorWithLineNumber(targetLineNumber);
    if (closestLineAnchor != null) {
      distanceFromClosestLineAnchor =
          Math.abs(closestLineAnchor.getLineInfo().number() - targetLineNumber);
    } else {
      distanceFromClosestLineAnchor = Integer.MAX_VALUE;
    }

    LineInfo lineInfo;
    if (distanceFromClosestLineAnchor < distanceFromFirstLine
        && distanceFromClosestLineAnchor < distanceFromLastLine) {
      lineInfo = closestLineAnchor.getLineInfo();
    } else if (distanceFromFirstLine < distanceFromLastLine) {
      lineInfo = new LineInfo(document.getFirstLine(), 0);
    } else {
      lineInfo = new LineInfo(document.getLastLine(), document.getLineCount() - 1);
    }

    return findLine(lineInfo, targetLineNumber);
  }

  public LineInfo findLine(Line line) {

    Line forwardIteratingLine = line;
    int forwardLineCount = 0;

    Line backwardIteratingLine = line.getPreviousLine();
    int backwardLineCount = 1;

    while (forwardIteratingLine != null && backwardIteratingLine != null) {
      LineInfo cachedLineInfo = LineUtils.getCachedLineInfo(forwardIteratingLine);
      if (cachedLineInfo != null) {
        return new LineInfo(line, cachedLineInfo.number() - forwardLineCount);
      }

      cachedLineInfo = LineUtils.getCachedLineInfo(backwardIteratingLine);
      if (cachedLineInfo != null) {
        return new LineInfo(line, cachedLineInfo.number() + backwardLineCount);
      }

      backwardIteratingLine = backwardIteratingLine.getPreviousLine();
      backwardLineCount++;

      forwardIteratingLine = forwardIteratingLine.getNextLine();
      forwardLineCount++;
    }

    if (forwardIteratingLine == null) {
      return new LineInfo(line, line.getDocument().getLineCount() - forwardLineCount);
    } else {
      return new LineInfo(line, backwardLineCount - 1);
    }
  }

  /*
   * TODO: really, this should be merged with the other findLine
   * which would then just consider {@code begin} as another known line number
   * to begin the search (along with top, bottom, and closest anchor).
   */
  /**
   * Finds the closest {@link LineInfo} for the given line number. This iterates
   * from the given {@code begin}. Use {@link #findLine(int)} if you DO NOT know
   * of a good starting point for the search.
   */
  public LineInfo findLine(LineInfo begin, int targetLineNumber) {
    if (targetLineNumber >= document.getLineCount()) {
      throw new IndexOutOfBoundsException("Asking for " + targetLineNumber
          + " but document length is " + document.getLineCount());
    }

    if (begin == null) {
      return findLine(targetLineNumber);
    }

    Line line = begin.line();
    int number = begin.number();

    // TODO: see if there's a closer anchor

    if (number < targetLineNumber) {
      while (line.getNextLine() != null && number < targetLineNumber) {
        line = line.getNextLine();
        number++;
      }
    } else if (number > targetLineNumber) {
      while (line.getPreviousLine() != null && number > targetLineNumber) {
        line = line.getPreviousLine();
        number--;
      }
    }

    return new LineInfo(line, number);
  }
}
