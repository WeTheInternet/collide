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

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.util.PositionUtils;
import com.google.common.base.Joiner;

import junit.framework.TestCase;

import org.junit.Assert;

/**
 * Tests for {@link PositionUtils}.
 */
public class PositionUtilsTests extends TestCase {

  /** Tests are dependent on these values, do not change. */
  private static final String[] LINES = {"Hello world\n", "Foo bar\n", "Something else\n"};

  private Document doc;
  private Line line;
  private Position startOfLine1;
  private Position endOfLine1;
  private Position startOfLine2;
  private Position endOfLine2;
  private Position startOfLine3;
  private Position endOfLine3;

  public void testPositionEquality() {
    assertEquals(startOfLine1, new Position(new LineInfo(line, 0), 0));
    assertNotSame(startOfLine1, startOfLine2);
  }

  public void testIntersection() {
    // Equal ranges
    assertIntersectionEquals(startOfLine1, endOfLine1, startOfLine1, endOfLine1, startOfLine1,
        endOfLine1);

    // Ranges start at same place
    assertIntersectionEquals(startOfLine1, endOfLine1, startOfLine1, endOfLine1, startOfLine1,
        endOfLine2);
    assertIntersectionEquals(startOfLine1, endOfLine1, startOfLine1, endOfLine2, startOfLine1,
        endOfLine1);

    // One range inside another
    assertIntersectionEquals(startOfLine2, endOfLine2, startOfLine1, endOfLine3, startOfLine2,
        endOfLine2);

    // Ranges end at same place
    assertIntersectionEquals(startOfLine2, endOfLine2, startOfLine1, endOfLine2, startOfLine2,
        endOfLine2);

    // Typical intersection
    assertIntersectionEquals(startOfLine2, endOfLine2, startOfLine1, endOfLine2, startOfLine2,
        endOfLine3);

    // No intersection
    assertIntersectionEquals(null, null, startOfLine1, endOfLine1, startOfLine2, endOfLine2);
  }

  public void testDifference() {
    // No difference
    assertDifferenceEquals(startOfLine1, endOfLine1, startOfLine1, endOfLine1);

    // Ranges start at same place
    assertDifferenceEquals(startOfLine1, endOfLine1, startOfLine1, endOfLine2, startOfLine2,
        endOfLine2);
    assertDifferenceEquals(startOfLine1, endOfLine2, startOfLine1, endOfLine1, startOfLine2,
        endOfLine2);

    // One range inside another
    assertDifferenceEquals(startOfLine1, endOfLine3, startOfLine2, endOfLine2, startOfLine1,
        endOfLine1, startOfLine3, endOfLine3);

    // Ranges end at same place
    assertDifferenceEquals(startOfLine1, endOfLine2, startOfLine2, endOfLine2, startOfLine1,
        endOfLine1);

    // Typical intersection
    assertDifferenceEquals(startOfLine1, endOfLine2, startOfLine2, endOfLine3, startOfLine1,
        endOfLine1, startOfLine3, endOfLine3);

    // No intersection
    assertDifferenceEquals(startOfLine1, endOfLine1, startOfLine2, endOfLine2, startOfLine1,
        endOfLine1, startOfLine2, endOfLine2);
  }

  @Override
  protected void setUp() throws Exception {
    doc =
        Document.createFromString(Joiner.on("").join(LINES));
    line = doc.getFirstLine();

    Line curLine = line;
    startOfLine1 = new Position(new LineInfo(curLine, 0), 0);
    endOfLine1 = new Position(new LineInfo(curLine, 0), LINES[0].length() - 1);

    curLine = curLine.getNextLine();
    startOfLine2 = new Position(new LineInfo(curLine, 1), 0);
    endOfLine2 = new Position(new LineInfo(curLine, 1), LINES[1].length() - 1);

    curLine = curLine.getNextLine();
    startOfLine3 = new Position(new LineInfo(curLine, 2), 0);
    endOfLine3 = new Position(new LineInfo(curLine, 2), LINES[2].length() - 1);
  }

  private void assertIntersectionEquals(Position expectedStart, Position expectedEnd,
      Position aStart, Position aEnd, Position bStart, Position bEnd) {
    Position[] intersection =
        PositionUtils.getIntersection(new Position[] {aStart, aEnd}, new Position[] {bStart, bEnd});
    if (expectedStart == null) {
      assertEquals(null, intersection);
    } else {
      Assert.assertArrayEquals((new Position[] {expectedStart, expectedEnd}), intersection);
    }
  }

  private void assertDifferenceEquals(Position aStart, Position aEnd, Position bStart,
      Position bEnd, Position... expected) {
    JsonArray<Position[]> difference =
        PositionUtils.getDifference(new Position[] {aStart, aEnd}, new Position[] {bStart, bEnd});
    assertEquals(expected.length / 2, difference.size());
    int expectedPos = 0;
    for (int diffPos = 0; diffPos < difference.size(); diffPos++) {
      Assert.assertArrayEquals(new Position[] {expected[expectedPos++], expected[expectedPos++]},
          difference.get(diffPos));
    }
  }
}
