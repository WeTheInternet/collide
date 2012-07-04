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

import static com.google.collide.shared.document.AnchorTestUtils.assertAnchorColumns;
import static com.google.collide.shared.document.AnchorTestUtils.assertAnchorLineNumbers;
import static com.google.collide.shared.document.AnchorTestUtils.assertAnchorPositions;
import static com.google.collide.shared.document.anchor.AnchorManager.IGNORE_COLUMN;
import static com.google.collide.shared.document.anchor.AnchorManager.IGNORE_LINE_NUMBER;
import static com.google.collide.shared.document.anchor.InsertionPlacementStrategy.EARLIER;
import static com.google.collide.shared.document.anchor.InsertionPlacementStrategy.LATER;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.document.anchor.AnchorList;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.InsertionPlacementStrategy;
import com.google.common.base.Joiner;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 */
public class AnchorTests extends TestCase {

  // 100 cols FTW
  private static final String[] LINES = {"About Google\n",
      "\n",
      "The Beginning\n",
      "\n",
      "Beginning in 1996, Stanford University graduate students Larry Page and "
          + "Sergey Brin built a\n",
      "search engine called 'BackRub' that used links to determine the importance of "
          + "individual\n",
      "web pages. By 1998 they had formalized their work, creating the company you know "
          + "today as Google."};

  private Document doc;
  private AnchorManager anchorManager;

  private static final AnchorType ANCHOR_TYPE_1 = AnchorType.create(AnchorTests.class, "1");
  private static final AnchorType ANCHOR_TYPE_2 = AnchorType.create(AnchorTests.class, "2");

  private Anchor docStart;
  private Anchor docStartCol1;
  private Anchor firstEmptyLine;
  private Anchor theBeginning;
  private Anchor year1996a;
  private Anchor year1996b;
  private Anchor year1996c;
  private Anchor individual;
  private Anchor year1998;
  private Anchor docEnd;

  private ArrayList<Anchor> anchors;

  @Override
  protected void setUp() {
    doc =
        Document.createFromString(Joiner.on("").join(LINES));
    anchorManager = doc.getAnchorManager();
    Line line = doc.getFirstLine();

    docStart = anchorManager.createAnchor(ANCHOR_TYPE_1, line, IGNORE_LINE_NUMBER, 0);
    docStartCol1 = anchorManager.createAnchor(ANCHOR_TYPE_1, line, 0, 1);
    line = line.getNextLine();

    firstEmptyLine = anchorManager.createAnchor(ANCHOR_TYPE_1, line, IGNORE_LINE_NUMBER, 0);

    line = line.getNextLine();
    theBeginning = anchorManager.createAnchor(ANCHOR_TYPE_2, line, IGNORE_LINE_NUMBER, 0);

    line = line.getNextLine().getNextLine();
    year1996a = anchorManager.createAnchor(ANCHOR_TYPE_1, line, IGNORE_LINE_NUMBER, 13);
    year1996b = anchorManager.createAnchor(ANCHOR_TYPE_2, line, IGNORE_LINE_NUMBER, 13);
    year1996c = anchorManager.createAnchor(ANCHOR_TYPE_2, line, IGNORE_LINE_NUMBER, 13);

    line = line.getNextLine();
    individual = anchorManager.createAnchor(ANCHOR_TYPE_1, line, IGNORE_LINE_NUMBER, 79);

    line = line.getNextLine();
    year1998 = anchorManager.createAnchor(ANCHOR_TYPE_2, line, IGNORE_LINE_NUMBER, 19);
    docEnd = anchorManager.createAnchor(ANCHOR_TYPE_1, line, IGNORE_LINE_NUMBER, 99);

    anchors = new ArrayList<Anchor>();
    anchors.add(docStart);
    anchors.add(docStartCol1);
    anchors.add(firstEmptyLine);
    anchors.add(theBeginning);
    anchors.add(year1996a);
    anchors.add(year1996b);
    anchors.add(year1996c);
    anchors.add(individual);
    anchors.add(year1998);
    anchors.add(docEnd);
  }

  /**
   * Currently the AnchorType.create will create a new instance of an anchor for
   * the same type. So, anchor types cannot rely on reference equality. If we
   * ever decide to change that, this test will let us know to update
   * expectations elsewhere.
   */
  public void testAnchorTypeEquality() {
    AnchorType type1dup = AnchorType.create(AnchorTests.class, "1");
    assertEquals(ANCHOR_TYPE_1, type1dup);
    assertEquals(ANCHOR_TYPE_1.toString(), type1dup.toString());

    // Reminder to update AnchorType comparisons to identity rather than .equals
    assertNotSame(ANCHOR_TYPE_1, type1dup);
  }

  /**
   * Verify we can walk the anchors in order.
   */
  public void testSimpleTraversal() {
    AnchorList anchorList = anchorManager.getAnchors(doc.getFirstLine());
    assertEquals(2, anchorList.size());
    Anchor anchor = anchorList.get(0);

    for (Anchor a : anchors) {
      assertSame(anchor, a);
      anchor = anchorManager.getNextAnchor(anchor);
    }
  }

  /**
   * Verify we can walk the anchors in reverse order
   */
  public void testBackwardsTraversal() {
    AnchorList anchorList = anchorManager.getAnchors(doc.getLastLine());
    assertEquals(2, anchorList.size());

    Anchor anchor = docEnd;
    for (int i = anchors.size() - 1; i >= 0; i--) {
      assertSame(anchors.get(i), anchor);
      assertEquals(anchors.get(i).getType(), anchor.getType());
      anchor = anchorManager.getPreviousAnchor(anchor);
    }
  }

  /**
   * Test {@link AnchorManager#getAnchorsByTypeOrNull(Line, AnchorType)}.
   */
  public void testGetAnchorsByTypeOrNull() {
    Line line = doc.getFirstLine();
    JsonArray<Anchor> anchors =
        AnchorManager.getAnchorsByTypeOrNull(line, ANCHOR_TYPE_1);
    assertEquals(2, anchors.size());
    assertSame(docStart, anchors.get(0));
    assertSame(docStartCol1, anchors.get(1));
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, ANCHOR_TYPE_2);
    assertEquals(0, anchors.size());

    line = line.getNextLine();
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, ANCHOR_TYPE_1);
    assertEquals(1, anchors.size());
    assertSame(firstEmptyLine, anchors.get(0));
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, ANCHOR_TYPE_2);
    assertEquals(0, anchors.size());

    line = line.getNextLine();
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, ANCHOR_TYPE_1);
    assertEquals(0, anchors.size());
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, ANCHOR_TYPE_2);
    assertEquals(1, anchors.size());
    assertSame(theBeginning, anchors.get(0));

    line = line.getNextLine();
    assertNotNull(line);
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, ANCHOR_TYPE_1);
    assertNull(anchors);
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, ANCHOR_TYPE_2);
    assertNull(anchors);
  }

  /**
   * Currently, we rely on being able to push anchors past the end of the line.
   */
  public void testAnchorPastEnd() {
    Line line = doc.getFirstLine().getNextLine();
    Anchor anchor = anchorManager.createAnchor(ANCHOR_TYPE_1, line, IGNORE_LINE_NUMBER, 1);
    assertSame(anchorManager.getPreviousAnchor(anchor), firstEmptyLine);

    line = line.getNextLine();
    anchorManager.moveAnchor(anchor, line, IGNORE_LINE_NUMBER, 100);
    assertSame(anchorManager.getPreviousAnchor(anchor), theBeginning);
  }

  /**
   * Verify that the type filter is working as expected.
   */
  public void testType1Traversal() {
    ArrayList<Anchor> type1Anchors = new ArrayList<Anchor>();
    for (Anchor a : anchors) {
      if (a.getType().equals(ANCHOR_TYPE_1)) {
        type1Anchors.add(a);
      }
    }

    Anchor anchor = docStart;
    assertEquals(ANCHOR_TYPE_1, anchor.getType());
    for (Anchor a : type1Anchors) {
      assertSame(a, anchor);
      assertEquals(ANCHOR_TYPE_1, anchor.getType());
      anchor = anchorManager.getNextAnchor(anchor, ANCHOR_TYPE_1);
    }
    assertNull(anchor);
  }

  public void testInsertionPlacementStrategyForLineAnchors() {
    Anchor a1 =
        createAnchorForPlacementStrategy(doc.getFirstLine(), true, IGNORE_COLUMN, EARLIER);
    Anchor a2 =
        createAnchorForPlacementStrategy(doc.getFirstLine(), true, IGNORE_COLUMN, LATER);
    assertAnchorLineNumbers(a1, 0, a2, 0);

    doc.insertText(doc.getFirstLine(), 0, "Newline\n");
    assertAnchorLineNumbers(a1, 0, a2, 1);

    doc.deleteText(doc.getFirstLine(), 0, doc.getFirstLine().getText().length());
    assertAnchorLineNumbers(a1, 0, a2, 0);

    doc.insertText(doc.getFirstLine(), 0, "Many\nnew\nlines\n!\n");
    assertAnchorLineNumbers(a1, 0, a2, 4);
  }

  public void testInsertionPlacementStrategyForColumnAnchors() {
    Anchor a1 = createAnchorForPlacementStrategy(doc.getFirstLine(), false, 0, EARLIER);
    Anchor a2 = createAnchorForPlacementStrategy(doc.getFirstLine(), false, 0, LATER);
    assertAnchorColumns(a1, 0, a2, 0);

    doc.insertText(doc.getFirstLine(), 0, "a");
    assertAnchorColumns(a1, 0, a2, 1);

    doc.deleteText(doc.getFirstLine(), 0, doc.getFirstLine().getText().length());
    assertAnchorColumns(a1, 0, a2, 0);

    doc.insertText(doc.getFirstLine(), 0, "more than a trivial insertion");
    assertAnchorColumns(a1, 0, a2, "more than a trivial insertion".length());

    doc.insertText(doc.getFirstLine(), 0, "\n");
    assertEquals(doc.getFirstLine(), a1.getLine());
    assertEquals(doc.getFirstLine().getNextLine(), a2.getLine());
    assertAnchorColumns(a1, 0, a2, "more than a trivial insertion".length());
  }

  public void testInsertionPlacementStrategyForLineNumberAndColumnAnchors() {
    Anchor a1 = createAnchorForPlacementStrategy(doc.getFirstLine(), true, 1, EARLIER);
    Anchor a2 = createAnchorForPlacementStrategy(doc.getFirstLine(), true, 1, LATER);
    assertAnchorPositions(a1, 0, 1, a2, 0, 1);

    doc.insertText(doc.getFirstLine(), 0, "a");
    assertAnchorPositions(a1, 0, 2, a2, 0, 2);

    doc.insertText(doc.getFirstLine(), 2, "a");
    assertAnchorPositions(a1, 0, 2, a2, 0, 3);

    doc.insertText(doc.getFirstLine(), 0, "\n");
    assertAnchorPositions(a1, 1, 2, a2, 1, 3);

    Line secondLine = doc.getFirstLine().getNextLine();
    doc.deleteText(secondLine, 0, secondLine.getText().length());
    assertAnchorPositions(a1, 1, 0, a2, 1, 0);

    doc.insertText(secondLine, 0, "more than a trivial insertion");
    assertAnchorPositions(a1, 1, 0, a2, 1, "more than a trivial insertion".length());

    doc.insertText(secondLine, 0, "many\nnew\nlines\n!\n");
    assertAnchorPositions(a1, 1, 0, a2, 5, "more than a trivial insertion".length());
  }

  private Anchor createAnchorForPlacementStrategy(Line line, boolean storeLineNumber, int column,
      InsertionPlacementStrategy insertionPlacementStrategy) {
    Anchor a =
        doc.getAnchorManager().createAnchor(ANCHOR_TYPE_1, doc.getFirstLine(),
            storeLineNumber ? doc.getLineFinder().findLine(line).number() : IGNORE_LINE_NUMBER,
            column);
    a.setInsertionPlacementStrategy(insertionPlacementStrategy);
    a.setRemovalStrategy(RemovalStrategy.SHIFT);
    return a;
  }
}
