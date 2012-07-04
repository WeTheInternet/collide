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

import static com.google.collide.shared.document.AnchorTestUtils.assertAnchorPosition;
import static com.google.collide.shared.document.DocumentTestUtils.deleteAndAssertEquals;
import static com.google.collide.shared.document.anchor.AnchorManager.IGNORE_COLUMN;
import static com.google.collide.shared.document.anchor.AnchorManager.IGNORE_LINE_NUMBER;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document.LineCountListener;
import com.google.collide.shared.document.Document.LineListener;
import com.google.collide.shared.document.Document.TextListener;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.document.anchor.Anchor.RemoveListener;
import com.google.collide.shared.document.anchor.Anchor.ShiftListener;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.util.LineUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;
import java.util.Random;

/**
 * Tests mutations on the document.
 *
 * Notes:
 * <ul>
 * <li>lineAnchor* members are anchors that only care about the line number
 * <li>columnAnchor* members are anchors that only care about the column (not a
 * line number)
 * <li>anchor* members are anchors that care about both the line number and
 * column
 * </ul>
 *
 */
public class DocumentTests extends TestCase {

  private static final AnchorType DOCUMENT_TEST_ANCHOR_TYPE =
      AnchorType.create(DocumentTests.class, "test");

  /** Tests are dependent on these values, do not change. */
  private static final String[] LINES = {"Hello world\n", "Foo bar\n", "Something else\n"};

  /** Tests are dependent on these values, do not change. */
  private static final String[] LINES_TO_INSERT =
      {"Insert number one\n", "Number two\n", "three\n"};

  private Anchor columnAnchorOnLine0H;
  private Anchor anchorOnLine0Space;
  private Anchor columnAnchorOnLine0W;
  private Anchor anchorOnLine0D;
  private Anchor columnAnchorOnLine0Newline;
  private Anchor anchorOnLine1F;
  private Anchor columnAnchorOnLine1SecondO;
  private Anchor anchorOnLine1Space;
  private Anchor columnAnchorOnLine1R;
  private Anchor anchorOnLine1Newline;
  private Anchor columnAnchorOnLine2S;
  private Anchor columnAnchorOnLine2Space;
  private Anchor anchorOnLine2LastE;
  private Anchor columnAnchorOnLine2Newline;

  private Anchor lineAnchorOnLine0;
  private Anchor lineAnchorOnLine1;
  private Anchor lineAnchorOnLine2;

  private Document doc;
  private LineFinder lf;

  private AnchorManager anchorManager;

  /** Starts at first line of the document */
  private Line line;

  private final Random random = new Random();

  public void testCreateFromString() {
    Document doc =
        Document.createFromString(Joiner.on("").join(LINES));
    Line line = doc.getFirstLine();
    for (String expectedLineText : LINES) {
      assertEquals(expectedLineText, line.getText());
      line = line.getNextLine();
    }
  }

  public void testDeleteEntireDocumentContents() {
    Line originalFirstLine = line;

    int docLength = 0;
    while (line != null) {
      docLength += line.getText().length();
      line = line.getNextLine();
    }

    // Shift a few anchors
    columnAnchorOnLine0H.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    anchorOnLine1Space.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    columnAnchorOnLine2Newline.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);

    doc.deleteText(doc.getFirstLine(), 0, docLength);

    assertEquals(originalFirstLine, doc.getFirstLine());
    assertEquals(originalFirstLine, doc.getLastLine());
    assertEquals("", originalFirstLine.getText());
    // The three shifted anchors above, and the line anchor on line 0
    assertEquals(4, doc.getAnchorManager().getAnchors(originalFirstLine).size());
    assertAnchorPosition(columnAnchorOnLine0H, 0, true, 0);
    assertAnchorPosition(anchorOnLine1Space, 0, false, 0);
    assertAnchorPosition(columnAnchorOnLine2Newline, 0, true, 0);
    assertFalse(anchorOnLine0D.isAttached());
    assertFalse(anchorOnLine1Newline.isAttached());
    assertFalse(anchorOnLine2LastE.isAttached());
  }

  public void testDeleteText() {
    // Delete single letter in front, middle, end
    deleteAndAssertEquals(line, 0, 1, "ello world\n");
    assertFalse(columnAnchorOnLine0H.isAttached());
    assertEquals(4, anchorOnLine0Space.getColumn());
    assertEquals(5, columnAnchorOnLine0W.getColumn());
    assertEquals(10, columnAnchorOnLine0Newline.getColumn());

    deleteAndAssertEquals(line, 4, 1, "elloworld\n");
    assertFalse(anchorOnLine0Space.isAttached());
    assertEquals(4, columnAnchorOnLine0W.getColumn());
    assertEquals(9, columnAnchorOnLine0Newline.getColumn());

    deleteAndAssertEquals(line, 8, 1, "elloworl\n");
    assertFalse(anchorOnLine0D.isAttached());
    assertEquals(4, columnAnchorOnLine0W.getColumn());
    assertEquals(8, columnAnchorOnLine0Newline.getColumn());

    assertEquals(0, lineAnchorOnLine0.getLineInfo().number());
    assertEquals(AnchorManager.IGNORE_COLUMN, lineAnchorOnLine0.getColumn());

    // Delete multiple letters from the front, middle, end
    line = line.getNextLine();

    deleteAndAssertEquals(line, 0, 2, "o bar\n");
    assertFalse(anchorOnLine1F.isAttached());
    assertEquals(0, columnAnchorOnLine1SecondO.getColumn());

    deleteAndAssertEquals(line, 1, 2, "oar\n");
    assertFalse(anchorOnLine1Space.isAttached());
    assertEquals(0, columnAnchorOnLine1SecondO.getColumn());
    assertEquals(2, columnAnchorOnLine1R.getColumn());
    assertEquals(3, anchorOnLine1Newline.getColumn());

    deleteAndAssertEquals(line, 1, 2, "o\n");
    assertFalse(columnAnchorOnLine1R.isAttached());
    assertEquals(0, columnAnchorOnLine1SecondO.getColumn());
    assertEquals(1, anchorOnLine1Newline.getColumn());

    assertEquals(1, lineAnchorOnLine1.getLineInfo().number());
    assertEquals(AnchorManager.IGNORE_COLUMN, lineAnchorOnLine1.getColumn());

    // Delete entire contents of line, minus the newline
    line = line.getNextLine();

    deleteAndAssertEquals(line, 0, LINES[2].length() - 1, "\n");
    assertFalse(columnAnchorOnLine2S.isAttached());
    assertFalse(columnAnchorOnLine2Space.isAttached());
    assertFalse(anchorOnLine2LastE.isAttached());
    assertEquals(0, columnAnchorOnLine2Newline.getColumn());

    assertEquals(2, lineAnchorOnLine2.getLineInfo().number());
    assertEquals(AnchorManager.IGNORE_COLUMN, lineAnchorOnLine2.getColumn());
  }

  public void testDeleteMultilineText() {
    deleteAndAssertEquals(columnAnchorOnLine0Newline.getLine(),
        columnAnchorOnLine0Newline.getColumn(), 1, "Hello worldFoo bar\n");
  }

  public void testViewportBottomShiftingOnSmallDocument() {
    Document doc = Document.createFromString("abc\n");
    Anchor bottomAnchor =
        doc.getAnchorManager().createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, doc.getLastLine(), 1,
        IGNORE_COLUMN);
    deleteAndAssertEquals(doc.getFirstLine(), 3, 1, "abc");
    assertAnchorPosition(bottomAnchor, 0, false, IGNORE_COLUMN);
  }

  public void testCursorAtEndOfOnlyLine() {
    Document doc = Document.createFromString("abc");
    // Column is the non-existent character after 'c'
    Anchor cursorAnchor =
        doc.getAnchorManager().createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, doc.getFirstLine(), 0, 3);
    cursorAnchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    deleteAndAssertEquals(doc.getFirstLine(), 0, 3, "");
    assertAnchorPosition(cursorAnchor, 0, false, 0);
  }

  public void testInsertText() {

    // Insert at front
    insertTextAndAssertEquals(line, 0, "This is ", "This is Hello world\n");
    assertEquals(8, columnAnchorOnLine0H.getColumn());
    assertEquals(13, anchorOnLine0Space.getColumn());
    assertEquals(19, columnAnchorOnLine0Newline.getColumn());

    // Insert in middle
    insertTextAndAssertEquals(line, 7, " a test of", "This is a test of Hello world\n");
    assertEquals(18, columnAnchorOnLine0H.getColumn());
    assertEquals(23, anchorOnLine0Space.getColumn());
    assertEquals(29, columnAnchorOnLine0Newline.getColumn());

    // Insert at end
    insertTextAndAssertEquals(
        line, line.getText().length() - 1, "!!", "This is a test of Hello world!!\n");
    assertEquals(18, columnAnchorOnLine0H.getColumn());
    assertEquals(23, anchorOnLine0Space.getColumn());
    assertEquals(31, columnAnchorOnLine0Newline.getColumn());
  }

  public void testInsertTextMultilineAtEnd() {
    String insertText = "\n" + getStringOfLinesToInsert();
    // The last line is "", so get the line before it for "Something else"
    line = doc.getLastLine().getPreviousLine();
    String lastLineText = line.getText();

    /*
     * Insert before the last line's newline (insertText starts with a newline,
     * so in the end, its text should be unchanged)
     */
    doc.insertText(line, line.getText().length() - 1, insertText);

    /*
     * The last line's text should be unchanged
     */
    assertEquals(lastLineText, line.getText());

    line = line.getNextLine();
    for (int i = 0; i < LINES_TO_INSERT.length; i++) {
      assertEquals(LINES_TO_INSERT[i], line.getText());
      line = line.getNextLine();
    }

    // Empty newline since insertText started with newline
    assertEquals("\n", line.getText());

    assertAnchorPosition(columnAnchorOnLine0H, 0, true, 0);
    assertAnchorPosition(anchorOnLine0Space, 0, false, 5);
    assertAnchorPosition(columnAnchorOnLine0W, 0, true, 6);
    assertAnchorPosition(anchorOnLine0D, 0, false, 10);
    assertAnchorPosition(columnAnchorOnLine0Newline, 0, true, 11);
    assertAnchorPosition(anchorOnLine1F, 1, false, 0);
    assertAnchorPosition(columnAnchorOnLine1SecondO, 1, true, 2);
    assertAnchorPosition(anchorOnLine1Space, 1, false, 3);
    assertAnchorPosition(columnAnchorOnLine1R, 1, true, 6);
    assertAnchorPosition(anchorOnLine1Newline, 1, false, 7);
    assertAnchorPosition(columnAnchorOnLine2S, 2, true, 0);
    assertAnchorPosition(columnAnchorOnLine2Space, 2, true, 9);
    assertAnchorPosition(anchorOnLine2LastE, 2, false, 13);
    // The only anchor that moved
    assertAnchorPosition(columnAnchorOnLine2Newline, 6, true, 0);
    assertAnchorPosition(lineAnchorOnLine0, 0, false, IGNORE_COLUMN);
    assertAnchorPosition(lineAnchorOnLine1, 1, false, IGNORE_COLUMN);
    assertAnchorPosition(lineAnchorOnLine2, 2, false, IGNORE_COLUMN);
  }

  public void testInsertTextMultilineAtFrontWithTrailingNewline() {
    String s = getStringOfLinesToInsert();

    doc.insertText(line, 0, s);

    line = doc.getFirstLine();
    for (int i = 0; i < LINES_TO_INSERT.length; i++) {
      assertEquals(LINES_TO_INSERT[i], line.getText());
      line = line.getNextLine();
    }

    for (int i = 0; i < LINES.length; i++) {
      assertEquals(LINES[i], line.getText());
      line = line.getNextLine();
    }

    assertAnchorPosition(columnAnchorOnLine0H, 3, true, 0);
    assertAnchorPosition(anchorOnLine0Space, 3, false, 5);
    assertAnchorPosition(columnAnchorOnLine0W, 3, true, 6);
    assertAnchorPosition(anchorOnLine0D, 3, false, 10);
    assertAnchorPosition(columnAnchorOnLine0Newline, 3, true, 11);
    assertAnchorPosition(anchorOnLine1F, 4, false, 0);
    assertAnchorPosition(columnAnchorOnLine1SecondO, 4, true, 2);
    assertAnchorPosition(anchorOnLine1Space, 4, false, 3);
    assertAnchorPosition(columnAnchorOnLine1R, 4, true, 6);
    assertAnchorPosition(anchorOnLine1Newline, 4, false, 7);
    assertAnchorPosition(columnAnchorOnLine2S, 5, true, 0);
    assertAnchorPosition(columnAnchorOnLine2Space, 5, true, 9);
    assertAnchorPosition(anchorOnLine2LastE, 5, false, 13);
    assertAnchorPosition(columnAnchorOnLine2Newline, 5, true, 14);
    assertAnchorPosition(lineAnchorOnLine0, 0, false, IGNORE_COLUMN);
    assertAnchorPosition(lineAnchorOnLine1, 4, false, IGNORE_COLUMN);
    assertAnchorPosition(lineAnchorOnLine2, 5, false, IGNORE_COLUMN);
  }

  public void testInsertTextMultilineAtMiddleOfLineWithoutTrailingNewline() {
    String s = getStringOfLinesToInsert();
    s += "No trailing newline here!";

    doc.insertText(line, 5, s);

    line = doc.getFirstLine();
    assertEquals(LINES[0].substring(0, 5) + LINES_TO_INSERT[0], line.getText());

    line = line.getNextLine();
    for (int i = 1; i < LINES_TO_INSERT.length; i++) {
      assertEquals(LINES_TO_INSERT[i], line.getText());
      line = line.getNextLine();
    }

    assertEquals("No trailing newline here!" + LINES[0].substring(5), line.getText());
    line = line.getNextLine();

    for (int i = 1; i < LINES.length; i++) {
      assertEquals(LINES[i], line.getText());
      line = line.getNextLine();
    }

    assertAnchorPosition(columnAnchorOnLine0H, 0, true, 0);
    assertAnchorPosition(anchorOnLine0Space, 3, false, 25);
    assertAnchorPosition(columnAnchorOnLine0W, 3, true, 26);
    assertAnchorPosition(anchorOnLine0D, 3, false, 30);
    assertAnchorPosition(columnAnchorOnLine0Newline, 3, true, 31);
    assertAnchorPosition(anchorOnLine1F, 4, false, 0);
    assertAnchorPosition(columnAnchorOnLine1SecondO, 4, true, 2);
    assertAnchorPosition(anchorOnLine1Space, 4, false, 3);
    assertAnchorPosition(columnAnchorOnLine1R, 4, true, 6);
    assertAnchorPosition(anchorOnLine1Newline, 4, false, 7);
    assertAnchorPosition(columnAnchorOnLine2S, 5, true, 0);
    assertAnchorPosition(columnAnchorOnLine2Space, 5, true, 9);
    assertAnchorPosition(anchorOnLine2LastE, 5, false, 13);
    assertAnchorPosition(columnAnchorOnLine2Newline, 5, true, 14);
    assertAnchorPosition(lineAnchorOnLine0, 0, false, IGNORE_COLUMN);
    assertAnchorPosition(lineAnchorOnLine1, 4, false, IGNORE_COLUMN);
    assertAnchorPosition(lineAnchorOnLine2, 5, false, IGNORE_COLUMN);
  }

  public void testRemoveRemovalStrategy() {
    columnAnchorOnLine0H.setRemovalStrategy(Anchor.RemovalStrategy.REMOVE);
    doc.deleteText(line, 0, 2);
    assertFalse(columnAnchorOnLine0H.isAttached());

    anchorOnLine0Space.setRemovalStrategy(Anchor.RemovalStrategy.REMOVE);
    int spaceColumn = anchorOnLine0Space.getColumn();
    doc.deleteText(line, spaceColumn, 2);
    assertFalse(anchorOnLine0Space.isAttached());

    anchorOnLine0D.setRemovalStrategy(Anchor.RemovalStrategy.REMOVE);
    int dColumn = anchorOnLine0D.getColumn();
    doc.deleteText(line, dColumn, 1);
    assertFalse(anchorOnLine0D.isAttached());

    // Deleting the newline will join lines
    columnAnchorOnLine0Newline.setRemovalStrategy(Anchor.RemovalStrategy.REMOVE);
    int newlineColumn = columnAnchorOnLine0Newline.getColumn();
    doc.deleteText(line, newlineColumn, 1);
    assertFalse(columnAnchorOnLine0Newline.isAttached());
  }

  public void testShiftRemovalStrategy() {
    columnAnchorOnLine0H.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    doc.deleteText(line, 0, 2);
    assertTrue(columnAnchorOnLine0H.isAttached());
    assertEquals(line, columnAnchorOnLine0H.getLine());
    assertEquals(0, columnAnchorOnLine0H.getColumn());

    anchorOnLine0Space.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    int spaceColumn = anchorOnLine0Space.getColumn();
    doc.deleteText(line, spaceColumn, 2);
    assertTrue(anchorOnLine0Space.isAttached());
    assertEquals(line, anchorOnLine0Space.getLine());
    assertEquals(spaceColumn, anchorOnLine0Space.getColumn());

    anchorOnLine0D.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    int dColumn = anchorOnLine0D.getColumn();
    doc.deleteText(line, dColumn, 1);
    assertTrue(anchorOnLine0D.isAttached());
    assertEquals(line, anchorOnLine0D.getLine());
    assertEquals(dColumn, anchorOnLine0D.getColumn());

    // Deleting the newline will join lines
    columnAnchorOnLine0Newline.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    lineAnchorOnLine1.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    int newlineColumn = columnAnchorOnLine0Newline.getColumn();
    doc.deleteText(line, newlineColumn, 1);
    assertTrue(columnAnchorOnLine0Newline.isAttached());
    assertEquals(line, columnAnchorOnLine0Newline.getLine());
    assertEquals(AnchorManager.IGNORE_LINE_NUMBER, columnAnchorOnLine0Newline.getLineNumber());
    assertEquals(newlineColumn, columnAnchorOnLine0Newline.getColumn());
    assertTrue(lineAnchorOnLine1.isAttached());
    assertEquals(line, lineAnchorOnLine1.getLine());
    assertEquals(0, lineAnchorOnLine1.getLineNumber());
    assertEquals(AnchorManager.IGNORE_COLUMN, lineAnchorOnLine1.getColumn());

    columnAnchorOnLine2Newline.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    line = columnAnchorOnLine2Newline.getLine();
    newlineColumn = columnAnchorOnLine2Newline.getColumn();
    doc.deleteText(line, newlineColumn, 1);
    assertTrue(columnAnchorOnLine2Newline.isAttached());
    assertEquals(line, columnAnchorOnLine2Newline.getLine());
    assertEquals(LineUtils.getLastCursorColumn(line), columnAnchorOnLine2Newline.getColumn());
  }

  public void testLineCountDuringTextChangeDispatch() {
    final int origLineCount = doc.getLineCount();
    
    doc.getTextListenerRegistrar().add(new TextListener() {
      @Override
      public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
        assertEquals(origLineCount + 1, doc.getLineCount());
      }
    });
    
    doc.insertText(doc.getFirstLine(), 0, "\n");
  }

  @SuppressWarnings("unchecked")
  public void testListenerCallbackOrderingOnInsertLine() {
    /*
     * The callback ordering is considered API and cannot be changed unless all
     * clients are updated (once public, it cannot be changed at all.) This must
     * match the ordering mentioned in the Document class's javadoc.
     */
    doc = Document.createFromString("one\ntwo\nthree");
    List<Class<?>> listenerOrdering = setupForListenerCallbackOrdering(doc);
    doc.insertText(doc.getFirstLine(), 0, "\n");
    assertEquals(Lists.newArrayList(ShiftListener.class, LineCountListener.class,
        LineListener.class, TextListener.class), listenerOrdering);
  }

  @SuppressWarnings("unchecked")
  public void testListenerCallbackOrderingOnRemoveLine() {
    doc = Document.createFromString("one\ntwo\nthree");
    List<Class<?>> listenerOrdering;
    
    listenerOrdering = setupForListenerCallbackOrdering(doc);
    doc.deleteText(doc.getFirstLine(), 0, doc.getFirstLine().length());
    assertEquals(Lists.newArrayList(ShiftListener.class, LineCountListener.class,
        LineListener.class, TextListener.class), listenerOrdering);

    doc = Document.createFromString("one\ntwo\nthree");
    listenerOrdering = setupForListenerCallbackOrdering(doc);
    doc.deleteText(doc.getFirstLine().getNextLine(), 0, doc.getFirstLine().getNextLine().length());
    assertEquals(Lists.newArrayList(ShiftListener.class, RemoveListener.class,
        LineCountListener.class, LineListener.class, TextListener.class), listenerOrdering);
  }

  public void testErrorWhenMutatingPastEndOfLine() {
    try {
      doc.insertText(line, line.length(), "Something");
      fail("Error should have been thrown");
    } catch (Exception e) {
    }

    /*
     * This is OK since the last line does not have a newline, this is the only
     * way to append a char to the last line
     */
    doc.insertText(doc.getLastLine(), doc.getLastLine().length(), "Something");

    try {
      doc.insertText(doc.getLastLine(), doc.getLastLine().length()+1, "Something");
      fail("Error should have been thrown");
    } catch (Exception e) {
    }
  }
  
  /**
   * For anchor listener testing, the anchor will be attached to the
   * "number of lines / 2".
   */
  private static List<Class<?>> setupForListenerCallbackOrdering(Document doc) {
    final List<Class<?>> callbackOrdering = Lists.newArrayList();
    
    Anchor shiftAnchor =
        doc.getAnchorManager().createAnchor(DOCUMENT_TEST_ANCHOR_TYPE,
            doc.getLineFinder().findLine(doc.getLineCount() / 2).line(), doc.getLineCount() / 2, 0);
    shiftAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);
    shiftAnchor.getShiftListenerRegistrar().add(new ShiftListener() {
      @Override
      public void onAnchorShifted(Anchor anchor) {
        callbackOrdering.add(ShiftListener.class);
      }
    });

    Anchor removeAnchor =
      doc.getAnchorManager().createAnchor(DOCUMENT_TEST_ANCHOR_TYPE,
          doc.getLineFinder().findLine(doc.getLineCount() / 2).line(), doc.getLineCount() / 2, 0);
    removeAnchor.getRemoveListenerRegistrar().add(new RemoveListener() {
      @Override
      public void onAnchorRemoved(Anchor anchor) {
        callbackOrdering.add(RemoveListener.class);
      }
    });
    
    doc.getLineCountListenerRegistrar().add(new LineCountListener() {
      @Override
      public void onLineCountChanged(Document document, int lineCount) {
        callbackOrdering.add(LineCountListener.class);
      }
    });
    
    doc.getLineListenerRegistrar().add(new LineListener() {
      @Override
      public void onLineRemoved(Document document, int lineNumber, JsonArray<Line> removedLines) {
        callbackOrdering.add(LineListener.class);
      }
      
      @Override
      public void onLineAdded(Document document, int lineNumber, JsonArray<Line> addedLines) {
        callbackOrdering.add(LineListener.class);
      }
    });
    
    doc.getTextListenerRegistrar().add(new TextListener() {
      @Override
      public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
        callbackOrdering.add(TextListener.class);
      }
    });
    
    return callbackOrdering;
  }
  
  @Override
  protected void setUp() throws Exception {
    doc = Document.createFromString(Joiner.on("").join(LINES));
    lf = doc.getLineFinder();
    anchorManager = doc.getAnchorManager();
    line = doc.getFirstLine();

    Line curLine = line;
    LineInfo curLineInfo = new LineInfo(line, 0);
    columnAnchorOnLine0H =
        anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, IGNORE_LINE_NUMBER, 0);
    anchorOnLine0Space = anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, 0, 5);
    columnAnchorOnLine0W =
        anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, IGNORE_LINE_NUMBER, 6);
    anchorOnLine0D =
        anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, 0, curLine.getText()
            .length() - 2);
    columnAnchorOnLine0Newline = anchorManager.createAnchor(
        DOCUMENT_TEST_ANCHOR_TYPE, curLine, IGNORE_LINE_NUMBER, curLine.getText().length() - 1);
    lineAnchorOnLine0 = anchorManager.createAnchor(
        DOCUMENT_TEST_ANCHOR_TYPE, curLineInfo.line(), curLineInfo.number(), IGNORE_COLUMN);

    curLine = curLine.getNextLine();
    curLineInfo.moveToNext();
    anchorOnLine1F = anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, 1, 0);
    columnAnchorOnLine1SecondO =
        anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, IGNORE_LINE_NUMBER, 2);
    anchorOnLine1Space = anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, 1, 3);
    columnAnchorOnLine1R = anchorManager.createAnchor(
        DOCUMENT_TEST_ANCHOR_TYPE, curLine, IGNORE_LINE_NUMBER, curLine.getText().length() - 2);
    anchorOnLine1Newline =
        anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, 1, curLine.getText()
            .length() - 1);
    lineAnchorOnLine1 = anchorManager.createAnchor(
        DOCUMENT_TEST_ANCHOR_TYPE, curLineInfo.line(), curLineInfo.number(), IGNORE_COLUMN);

    curLine = curLine.getNextLine();
    curLineInfo.moveToNext();
    columnAnchorOnLine2S =
        anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, IGNORE_LINE_NUMBER, 0);
    columnAnchorOnLine2Space =
        anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, IGNORE_LINE_NUMBER, 9);
    anchorOnLine2LastE =
        anchorManager.createAnchor(DOCUMENT_TEST_ANCHOR_TYPE, curLine, 2, curLine.getText()
            .length() - 2);
    columnAnchorOnLine2Newline = anchorManager.createAnchor(
        DOCUMENT_TEST_ANCHOR_TYPE, curLine, IGNORE_LINE_NUMBER, curLine.getText().length() - 1);
    lineAnchorOnLine2 = anchorManager.createAnchor(
        DOCUMENT_TEST_ANCHOR_TYPE, curLineInfo.line(), curLineInfo.number(), IGNORE_COLUMN);
  }

  private String getStringOfLinesToInsert() {
    String s = "";
    for (int i = 0; i < LINES_TO_INSERT.length; i++) {
      s += LINES_TO_INSERT[i];
    }
    return s;
  }

  private void insertTextAndAssertEquals(Line line, int column, String text, String expectedText) {
    doc.insertText(line, column, text);
    assertEquals(expectedText, line.getText());
  }
}
