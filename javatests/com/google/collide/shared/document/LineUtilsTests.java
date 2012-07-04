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

import com.google.collide.shared.document.util.PositionUtils;
import com.google.common.base.Joiner;

import junit.framework.TestCase;

/**
 * Tests for the {@link LineUtils} class.
 *
 */
public class LineUtilsTests extends TestCase {

  /** Tests are dependent on these values, do not change. */
  private static final String[] LINES = {"Hello world\n", "Foo bar\n", "Something else\n"};

  private Document doc;
  private Line line;

  public void testGetPositionBackwards() {
    Position pos;
    line = doc.getLastLine();

    pos = PositionUtils.getPosition(line, -1, 5, -1);
    assertPosition(line, 4, pos);

    pos = PositionUtils.getPosition(line, -1, 5, -3);
    assertPosition(line, 2, pos);

    pos = PositionUtils.getPosition(line, -1, 0, -1);
    assertPosition(line.getPreviousLine(), line.getPreviousLine().getText().length() - 1, pos);

    pos = PositionUtils.getPosition(line, -1, 5, -6);
    assertPosition(line.getPreviousLine(), line.getPreviousLine().getText().length() - 1, pos);

    pos = PositionUtils.getPosition(line, -1, 5, -5 - line.getPreviousLine().getText().length());
    assertPosition(line.getPreviousLine(), 0, pos);

    pos = PositionUtils.getPosition(line, -1, 5, -6 - line.getPreviousLine().getText().length());
    assertPosition(line.getPreviousLine().getPreviousLine(), line.getPreviousLine()
        .getPreviousLine().getText().length() - 1, pos);

    pos =
        PositionUtils.getPosition(line, -1, 5, -6 - line.getPreviousLine().getText().length()
            - line.getPreviousLine().getPreviousLine().getText().length());
    assertPosition(line.getPreviousLine().getPreviousLine().getPreviousLine(), line
        .getPreviousLine().getPreviousLine().getPreviousLine().getText().length() - 1, pos);
  }

  public void testGetPositionForward() {
    Position pos;

    pos = PositionUtils.getPosition(line, -1, 0, 1);
    assertPosition(line, 1, pos);

    pos = PositionUtils.getPosition(line, -1, 0, 5);
    assertPosition(line, 5, pos);

    pos = PositionUtils.getPosition(line, -1, 3, 5);
    assertPosition(line, 8, pos);

    pos = PositionUtils.getPosition(line, -1, 11, 1);
    assertPosition(line.getNextLine(), 0, pos);

    pos = PositionUtils.getPosition(line, -1, 0, line.getText().length());
    assertPosition(line.getNextLine(), 0, pos);

    pos = PositionUtils.getPosition(line, -1, 0, line.getText().length() + 5);
    assertPosition(line.getNextLine(), 5, pos);

    pos = PositionUtils.getPosition(line, -1, 3, line.getText().length());
    assertPosition(line.getNextLine(), 3, pos);

    pos =
        PositionUtils.getPosition(line, -1, 0, line.getText().length()
            + line.getNextLine().getText().length());
    assertPosition(line.getNextLine().getNextLine(), 0, pos);

    pos =
        PositionUtils.getPosition(line, -1, 3, line.getText().length()
            + line.getNextLine().getText().length() + 5);
    assertPosition(line.getNextLine().getNextLine(), 8, pos);

    pos =
        PositionUtils.getPosition(line, -1, 0, line.getText().length()
            + line.getNextLine().getText().length() + 5);
    assertPosition(line.getNextLine().getNextLine(), 5, pos);
  }

  public void testGetPositionOutOfBoundsBeginning() {
    try {
      PositionUtils.getPosition(line, -1, 0, -1);
      fail();
    } catch (PositionOutOfBoundsException e) {
    }
  }

  public void testGetPositionForwardAtDocumentEnd() {
    line = doc.getLastLine();
    Position pos = PositionUtils.getPosition(line, -1, line.getText().length() - 1, 1);
    assertPosition(doc.getLastLine(), doc.getLastLine().getText().length(), pos);
  }

  public void testGetPositionOutOfBoundsEnd() {
    line = doc.getLastLine();
    try {
      PositionUtils.getPosition(line, -1, line.getText().length() - 1, 2);
      fail();
    } catch (PositionOutOfBoundsException e) {
    }
  }

  @Override
  protected void setUp() throws Exception {
    doc =
        Document.createFromString(Joiner.on("").join(LINES));
    line = doc.getFirstLine();
  }

  private void assertPosition(Line expectedLine, int expectedColumn, Position actualPosition) {
    assertEquals(expectedLine, actualPosition.getLine());
    assertEquals(expectedColumn, actualPosition.getColumn());
  }
}
