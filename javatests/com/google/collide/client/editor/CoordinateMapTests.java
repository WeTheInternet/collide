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

package com.google.collide.client.editor;

import com.google.collide.client.editor.CoordinateMap.DocumentSizeProvider;
import com.google.collide.shared.document.Document;
import com.google.common.base.Joiner;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

/**
 * Tests for {@link CoordinateMap}.
 * 
 */
public class CoordinateMapTests extends TestCase {

  private static final int LINE_HEIGHT = 10;

  // 100 cols FTW
  private static final String[] LINES = {
      "About Google\n",
      "\n",
      "The Beginning\n",
      "\n",
      "Beginning in 1996, Stanford University graduate students Larry Page and "
          + "Sergey Brin built a\n",
      "search engine called 'BackRub' that used links to determine the importance of "
          + "individual\n",
      "web pages. By 1998 they had formalized their work, creating the company you know "
          + "today as Google."};

  private CoordinateMap coordMap;
  private Document doc;

  private final DocumentSizeProvider documentSizeProvider = new DocumentSizeProvider() {
    @Override
    public void handleSpacerHeightChanged(Spacer s, int i) {
    }

    @Override
    public int getEditorLineHeight() {
      return LINE_HEIGHT;
    }

    @Override
    public float getEditorCharacterWidth() {
      return (float) 7.0;
    }
  };

  private Buffer mockBuffer;

  @Override
  protected void setUp() throws Exception {
    doc =
        Document.createFromString(Joiner.on("").join(LINES));

    IMocksControl control = EasyMock.createControl();
    control.resetToNice();
    mockBuffer = control.createMock(Buffer.class);
    control.replay();

    coordMap = new CoordinateMap(documentSizeProvider);
    coordMap.handleDocumentChange(doc);
  }

  public void testNoSpacers() {
    assertNoSpacers();
  }

  private void assertNoSpacers() {
    for (int i = 0; i < doc.getLineCount(); i++) {
      assertEquals(i * LINE_HEIGHT, coordMap.convertLineNumberToY(i));
      if (i > 0) {
        assertNotSame(i, coordMap.convertYToLineNumber(i * LINE_HEIGHT - 1));
      }
      assertEquals(i, coordMap.convertYToLineNumber(i * LINE_HEIGHT));
      assertEquals(i, coordMap.convertYToLineNumber(i * LINE_HEIGHT + 1));
      assertEquals(i, coordMap.convertYToLineNumber(i * LINE_HEIGHT + LINE_HEIGHT / 2));
      assertEquals(i, coordMap.convertYToLineNumber((i + 1) * LINE_HEIGHT - 1));
      assertNotSame(i, coordMap.convertYToLineNumber((i + 1) * LINE_HEIGHT));
    }
  }

  public void testLine0SpacerCreation() {
    coordMap.createSpacer(doc.getLineFinder().findLine(0), 30, mockBuffer, "");
    assertEquals(30, coordMap.convertLineNumberToY(0));
    assertEquals(0, coordMap.convertYToLineNumber(0));
    assertEquals(0, coordMap.convertYToLineNumber(15));
    assertEquals(0, coordMap.convertYToLineNumber(30));
    assertEquals(0, coordMap.convertYToLineNumber(30 + LINE_HEIGHT - 1));
    assertNotSame(0, coordMap.convertYToLineNumber(30 + LINE_HEIGHT));
  }

  public void testMultipleSpacers() {
    Spacer spacerOnLine0 =
        coordMap.createSpacer(doc.getLineFinder().findLine(0), 30, mockBuffer, "");
    assertEquals(30, coordMap.convertLineNumberToY(0));
    assertEquals(40, coordMap.convertLineNumberToY(1));
    assertEquals(0, coordMap.convertYToLineNumber(0));
    assertEquals(0, coordMap.convertYToLineNumber(30));
    assertEquals(0, coordMap.convertYToLineNumber(39));
    assertEquals(1, coordMap.convertYToLineNumber(40));
    assertEquals(1, coordMap.convertYToLineNumber(49));

    Spacer spacerOnLine2 =
        coordMap.createSpacer(doc.getLineFinder().findLine(2), 40, mockBuffer, "");
    assertEquals(90, coordMap.convertLineNumberToY(2));
    assertEquals(2, coordMap.convertYToLineNumber(50));
    assertEquals(2, coordMap.convertYToLineNumber(51));
    assertEquals(2, coordMap.convertYToLineNumber(89));
    assertEquals(2, coordMap.convertYToLineNumber(90));
    assertNotSame(2, coordMap.convertYToLineNumber(100));

    Spacer spacerOnLine3 =
        coordMap.createSpacer(doc.getLineFinder().findLine(3), 10, mockBuffer, "");
    assertEquals(110, coordMap.convertLineNumberToY(3));
    assertEquals(120, coordMap.convertLineNumberToY(4));
    assertEquals(3, coordMap.convertYToLineNumber(100));
    assertEquals(3, coordMap.convertYToLineNumber(119));
    assertEquals(4, coordMap.convertYToLineNumber(120));
    assertEquals(4, coordMap.convertYToLineNumber(129));
    assertNotSame(4, coordMap.convertYToLineNumber(130));

    Spacer spacerOnLine5 =
        coordMap.createSpacer(doc.getLineFinder().findLine(5), 60, mockBuffer, "");
    assertEquals(190, coordMap.convertLineNumberToY(5));
    assertEquals(5, coordMap.convertYToLineNumber(130));
    assertEquals(5, coordMap.convertYToLineNumber(131));
    assertEquals(5, coordMap.convertYToLineNumber(190));
    assertEquals(5, coordMap.convertYToLineNumber(199));
    assertNotSame(5, coordMap.convertYToLineNumber(200));

    // Test invalidation
    Spacer spacerOnLine1 =
        coordMap.createSpacer(doc.getLineFinder().findLine(1), 10, mockBuffer, "");
    assertEquals(130, coordMap.convertLineNumberToY(4));
    assertEquals(200, coordMap.convertLineNumberToY(5));
    assertEquals(0, coordMap.convertYToLineNumber(0));
    assertEquals(0, coordMap.convertYToLineNumber(30));
    assertEquals(0, coordMap.convertYToLineNumber(39));
    assertEquals(1, coordMap.convertYToLineNumber(50));
    assertEquals(1, coordMap.convertYToLineNumber(59));
    assertEquals(2, coordMap.convertYToLineNumber(60));
    assertEquals(2, coordMap.convertYToLineNumber(61));
    assertEquals(2, coordMap.convertYToLineNumber(99));
    assertEquals(2, coordMap.convertYToLineNumber(100));
    assertEquals(3, coordMap.convertYToLineNumber(110));
    assertEquals(3, coordMap.convertYToLineNumber(129));
    assertEquals(4, coordMap.convertYToLineNumber(130));
    assertEquals(4, coordMap.convertYToLineNumber(139));
    assertEquals(5, coordMap.convertYToLineNumber(140));
    assertEquals(5, coordMap.convertYToLineNumber(141));
    assertEquals(5, coordMap.convertYToLineNumber(200));
    assertEquals(5, coordMap.convertYToLineNumber(209));
    assertNotSame(5, coordMap.convertYToLineNumber(210));

    // Test deletion
    coordMap.removeSpacer(spacerOnLine1);
    assertEquals(30, coordMap.convertLineNumberToY(0));
    assertEquals(40, coordMap.convertLineNumberToY(1));
    assertEquals(90, coordMap.convertLineNumberToY(2));
    assertEquals(110, coordMap.convertLineNumberToY(3));
    assertEquals(120, coordMap.convertLineNumberToY(4));
    assertEquals(190, coordMap.convertLineNumberToY(5));
    assertEquals(0, coordMap.convertYToLineNumber(0));
    assertEquals(0, coordMap.convertYToLineNumber(30));
    assertEquals(0, coordMap.convertYToLineNumber(39));
    assertEquals(1, coordMap.convertYToLineNumber(40));
    assertEquals(1, coordMap.convertYToLineNumber(49));
    assertEquals(2, coordMap.convertYToLineNumber(50));
    assertEquals(2, coordMap.convertYToLineNumber(51));
    assertEquals(2, coordMap.convertYToLineNumber(89));
    assertEquals(2, coordMap.convertYToLineNumber(90));
    assertEquals(3, coordMap.convertYToLineNumber(100));
    assertEquals(3, coordMap.convertYToLineNumber(119));
    assertEquals(4, coordMap.convertYToLineNumber(120));
    assertEquals(4, coordMap.convertYToLineNumber(129));
    assertEquals(5, coordMap.convertYToLineNumber(130));
    assertEquals(5, coordMap.convertYToLineNumber(131));
    assertEquals(5, coordMap.convertYToLineNumber(190));
    assertEquals(5, coordMap.convertYToLineNumber(199));
    assertNotSame(5, coordMap.convertYToLineNumber(200));

    coordMap.removeSpacer(spacerOnLine5);
    coordMap.removeSpacer(spacerOnLine0);
    coordMap.removeSpacer(spacerOnLine3);
    coordMap.removeSpacer(spacerOnLine2);

    assertNoSpacers();
  }
}
