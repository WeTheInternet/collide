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

package com.google.collide.client.document.linedimensions;

import com.google.collide.client.document.linedimensions.LineDimensionsCalculator.RoundingStrategy;
import com.google.collide.client.editor.search.SearchTestsUtil;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

/**
 * Tests for {@link LineDimensionsCalculator}.
 */
public class LineDimensionsCalculatorTests extends TestCase {
  /**
   * An object which provides measurement information to a
   * {@link LineDimensionsCalculator}.
   */
  public static class TestMeasurementProvider implements MeasurementProvider {

    private final int characterWidth;

    public TestMeasurementProvider(int characterWidth) {
      this.characterWidth = characterWidth;
    }

    @Override
    public double getCharacterWidth() {
      return characterWidth;
    }

    @Override
    public double measureStringWidth(String text) {
      int length = 0;
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        int utype = Character.getType(c);
        if (utype == Character.COMBINING_SPACING_MARK || utype == Character.NON_SPACING_MARK
            || utype == Character.ENCLOSING_MARK) {
          // These characters are 0, width (we do this so its clear)
          length += 0;
        } else if (c == '\t') {
          length += TAB_SIZE;
        } else if (c == '\r') {
          length += 0;
        } else if (c < 255) {
          length += 1;
        } else {
          /*
           * if its not a combining mark, and its not standard Latin, make it
           * just like a tab. This is just for testing purposes, and isn't
           * usually the case.
           */
          length += TAB_SIZE;
        }
      }
      return length * getCharacterWidth();
    }
  }

  /** Tab size in columns. */
  private static final int TAB_SIZE = 2;
  /** Width of a character in pixels. */
  private static final int CHARACTER_SIZE = 8;

  private LineDimensionsCalculator calculator;
  private MeasurementProvider measurementProvider;
  private Document basicDocument;
  private Document indentAndCarriageReturnDocument;
  private Document fullUnicodeDocument;

  @Override
  public void setUp() {
    measurementProvider = new TestMeasurementProvider(CHARACTER_SIZE);
    calculator = LineDimensionsCalculator.createWithCustomProvider(measurementProvider);
    LineDimensionsUtils.setTabSpaceEquivalence(TAB_SIZE);

    basicDocument = Document.createFromString(Joiner.on('\n').join(BASIC_NO_SPECIAL_DOCUMENT));
    indentAndCarriageReturnDocument =
        Document.createFromString(Joiner.on('\n').join(TAB_AND_CARRIAGE_RETURN_DOCUMENT));
    fullUnicodeDocument = Document.createFromString(Joiner.on('\n').join(FULL_UNICODE_DOCUMENT));
  }

  public void testSettingDocumentDoesNothingToDocument() {
    calculator.handleDocumentChange(basicDocument);

    LineInfo lineInfo = basicDocument.getFirstLineInfo();
    do {
      assertTag(null, lineInfo.line());
    } while (lineInfo.moveToNext());
  }

  public void testLinesAreLazilyTagged() {
    calculator.handleDocumentChange(basicDocument);

    LineInfo lineTwo = SearchTestsUtil.gotoLineInfo(basicDocument, 2);
    LineInfo lineThree = SearchTestsUtil.gotoLineInfo(basicDocument, 3);
    calculator.convertColumnToX(lineTwo.line(), 3);
    calculator.convertColumnToX(lineThree.line(), 3);

    LineInfo lineInfo = basicDocument.getFirstLineInfo();
    do {
      if (lineInfo.number() == lineTwo.number() || lineInfo.number() == lineThree.number()) {
        assertTag(false, lineInfo.line());
      } else {
        assertTag(null, lineInfo.line());
      }
    } while (lineInfo.moveToNext());
  }

  public void testCalculatedCorrectlyForSimpleCase() {
    calculator.handleDocumentChange(basicDocument);

    LineInfo lineInfo = SearchTestsUtil.gotoLineInfo(basicDocument, 2);
    double x = assertReversibleAndReturnX(lineInfo.line(), 3);
    assertEquals(naiveColumnToX(3), x);

    x = assertReversibleAndReturnX(lineInfo.line(), 0);
    assertEquals(0.0, x);

    x = assertReversibleAndReturnX(lineInfo.line(), lineInfo.line().length() - 1);
    assertEquals(naiveColumnToX(lineInfo.line().length() - 1), x);
  }

  public void testIndentationAndCarriageReturnDoesNotAddOffsetCache() {
    calculator.handleDocumentChange(indentAndCarriageReturnDocument);

    LineInfo lineInfo = indentAndCarriageReturnDocument.getFirstLineInfo();
    do {
      calculator.convertColumnToX(lineInfo.line(), 3);
      assertTag(false, lineInfo.line());
    } while (lineInfo.moveToNext());
  }

  public void testIndentationHandled() {
    calculator.handleDocumentChange(indentAndCarriageReturnDocument);

    LineInfo lineInfo = indentAndCarriageReturnDocument.getFirstLineInfo();
    double x = assertReversibleAndReturnX(lineInfo.line(), 0);
    assertEquals(0.0, x);

    x = assertReversibleAndReturnX(lineInfo.line(), 1);
    assertWideChars(1, 1, x);

    x = assertReversibleAndReturnX(lineInfo.line(), 2);
    assertWideChars(2, 2, x);

    x = assertReversibleAndReturnX(lineInfo.line(), 3);
    assertWideChars(3, 3, x);

    x = assertReversibleAndReturnX(lineInfo.line(), 4);
    assertWideChars(4, 3, x);

    x = assertReversibleAndReturnX(lineInfo.line(), 4);
    assertWideChars(4, 3, x);

    int lastColumn = lineInfo.line().length() - 1;
    x = assertReversibleAndReturnX(lineInfo.line(), lastColumn);
    assertWideChars(lastColumn, 3, x);
  }

  public void testCarriageReturnHandled() {
    calculator.handleDocumentChange(indentAndCarriageReturnDocument);

    LineInfo lineInfo = SearchTestsUtil.gotoLineInfo(indentAndCarriageReturnDocument, 1);
    double x = assertReversibleAndReturnX(lineInfo.line(), 0);
    assertEquals(0.0, x);

    x = assertReversibleAndReturnX(lineInfo.line(), 5);
    assertEquals(naiveColumnToX(5), x);

    x = assertReversibleAndReturnX(lineInfo.line(), 15);
    assertEquals(naiveColumnToX(15), x);

    // Test offset due to carriage return is correct
    int length = lineInfo.line().length();
    x = assertReversibleAndReturnX(lineInfo.line(), length - 3);
    assertWideCharsAndZeroWidthChars(length - 3, 0, 0, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineInfo.line(), length - 2, 1);
    assertWideCharsAndZeroWidthChars(length - 2, 0, 0, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineInfo.line(), length - 1, 0);
    assertWideCharsAndZeroWidthChars(length - 1, 0, 1, x);
  }

  public void testBothTabAndCarriageReturn() {
    calculator.handleDocumentChange(indentAndCarriageReturnDocument);

    LineInfo lineTwo = SearchTestsUtil.gotoLineInfo(indentAndCarriageReturnDocument, 2);
    double x = assertReversibleAndReturnX(lineTwo.line(), 1);
    assertWideChars(1, 1, x);

    x = assertReversibleAndReturnX(lineTwo.line(), 2);
    assertWideChars(2, 1, x);

    x = assertReversibleAndReturnX(lineTwo.line(), 10);
    assertWideChars(10, 1, x);

    // Test offset due to carriage return is correct
    int length = lineTwo.line().length();
    x = assertReversibleAndReturnX(lineTwo.line(), length - 3);
    assertWideCharsAndZeroWidthChars(length - 3, 1, 0, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineTwo.line(), length - 2, 1);
    assertWideCharsAndZeroWidthChars(length - 2, 1, 0, x);

    x = assertReversibleAndReturnX(lineTwo.line(), length - 1);
    assertWideCharsAndZeroWidthChars(length - 1, 1, 1, x);
  }

  public void testLineWithAllTabsAndCarriageReturn() {
    calculator.handleDocumentChange(indentAndCarriageReturnDocument);

    LineInfo lineThree = SearchTestsUtil.gotoLineInfo(indentAndCarriageReturnDocument, 3);
    double x = assertReversibleAndReturnX(lineThree.line(), 1);
    assertWideChars(1, 1, x);

    x = assertReversibleAndReturnX(lineThree.line(), 2);
    assertWideChars(2, 2, x);

    x = assertReversibleAndReturnX(lineThree.line(), 3);
    assertWideChars(3, 3, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineThree.line(), 4, 1);
    assertWideChars(4, 4, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineThree.line(), 5, 0);
    assertWideCharsAndZeroWidthChars(5, 4, 1, x);
  }

  public void testLineWithAllTabsAndCarriageReturnWithTabSizeOfThree() {
    LineDimensionsUtils.setTabSpaceEquivalence(3);
    testLineWithAllTabsAndCarriageReturn();
    testBothTabAndCarriageReturn();
  }

  public void testAssertAllLinesWithSpecialCharsHaveATag() {
    LineInfo lineInfo = fullUnicodeDocument.getFirstLineInfo();
    do {
      calculator.convertColumnToX(lineInfo.line(), 3);
      assertTag(true, lineInfo.line());
    } while (lineInfo.moveToNext());
  }

  public void testAssertLineOneOfUnicodeDocIsRight() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    LineInfo lineOne = fullUnicodeDocument.getFirstLineInfo();
    double x = assertReversibleAndReturnX(lineOne.line(), 1);
    assertWideChars(1, 1, x);

    x = assertReversibleAndReturnX(lineOne.line(), 7);
    assertWideChars(7, 1, x);

    x = assertReversibleAndReturnX(lineOne.line(), 8);
    assertWideChars(8, 2, x);

    // Test Carriage Return
    int length = lineOne.line().length();
    x = assertReversibleAndReturnX(lineOne.line(), length - 4);
    assertWideCharsAndZeroWidthChars(length - 4, 2, 0, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineOne.line(), length - 3, 2);
    assertWideCharsAndZeroWidthChars(length - 3, 2, 0, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineOne.line(), length - 2, 1);
    assertWideCharsAndZeroWidthChars(length - 2, 2, 1, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineOne.line(), length - 1, 0);
    assertWideCharsAndZeroWidthChars(length - 1, 2, 2, x);
  }

  public void testAssertLineTwoOfUnicodeDocIsRight() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    LineInfo lineTwo = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 1);
    for (int i = 0; i < lineTwo.line().length(); i++) {
      double x = assertReversibleAndReturnX(lineTwo.line(), i);
      assertWideChars(i, i, x);
    }
  }

  public void testAssertLineThreeOfUNicodeDocIsRight() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    LineInfo lineThree = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 2);
    double x = assertReversibleAndReturnX(lineThree.line(), 0);
    assertEquals(0.0, x);

    for (int i = 1, j = 2; i < lineThree.line().length() - 2; i += 2, j += 2) {
      x = assertReversibleAndReturnX(lineThree.line(), i);
      assertWideChars(i, i - 1, x);

      x = assertReversibleAndReturnX(lineThree.line(), j);
      assertWideChars(j, j - 1, x);
    }

    // not dealing with \n btw
    int lastCharIndex = lineThree.line().length() - 1;
    x = assertReversibleAndReturnX(lineThree.line(), lastCharIndex);
    /*
     * so this looks funny so I'll comment it but its just convenient. it's
     * saying that given lastCharIndex column, it has 2 less widechars then its
     * column index. This makes sense because if every column before it was a
     * wide char then we'd have myIndex - 1 wide chars.
     */
    assertWideChars(lastCharIndex, lastCharIndex - 2, x);
  }

  public void testAssertLineFourOfUnicodeDocIsRight() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    LineInfo lineFour = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 3);
    double x = assertReversibleAndReturnX(lineFour.line(), 0);
    assertEquals(0.0, x);

    // The first character is an a + a ` combining mark
    x = assertReversibleAndReturnXAccountingForZeroWidth(lineFour.line(), 1, 1);
    assertWideCharsAndZeroWidthChars(1, 0, 0, x);
    x = assertReversibleAndReturnXAccountingForZeroWidth(lineFour.line(), 2, 0);
    assertWideCharsAndZeroWidthChars(2, 0, 1, x);

    // Test remaining characters
    x = assertReversibleAndReturnX(lineFour.line(), 3);
    assertWideCharsAndZeroWidthChars(3, 0, 1, x);
    x = assertReversibleAndReturnX(lineFour.line(), 4);
    assertWideCharsAndZeroWidthChars(4, 0, 1, x);
  }

  public void testAssertLineFiveOfUnicodeDocIsRight() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    /*
     * This line looks like LLccLLccLL
     * NOTE: These characters all appear double wide since the test measurer
     * just blatently makes any character > 255 double wide. In realty arabic
     * characters aren't like that and present other challenges related to size.
     */
    LineInfo lineFive = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 4);
    double x = assertReversibleAndReturnX(lineFive.line(), 0);
    assertEquals(0.0, x);

    x = assertReversibleAndReturnX(lineFive.line(), 1);
    assertWideCharsAndZeroWidthChars(1, 1, 0, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineFive.line(), 2, 2);
    assertWideCharsAndZeroWidthChars(2, 2, 0, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineFive.line(), 3, 1);
    assertWideCharsAndZeroWidthChars(3, 2, 1, x);

    x = assertReversibleAndReturnX(lineFive.line(), 4);
    assertWideCharsAndZeroWidthChars(4, 2, 2, x);

    x = assertReversibleAndReturnX(lineFive.line(), 5);
    assertWideCharsAndZeroWidthChars(5, 3, 2, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineFive.line(), 6, 2);
    assertWideCharsAndZeroWidthChars(6, 4, 2, x);

    x = assertReversibleAndReturnXAccountingForZeroWidth(lineFive.line(), 7, 1);
    assertWideCharsAndZeroWidthChars(7, 4, 3, x);

    x = assertReversibleAndReturnX(lineFive.line(), 8);
    assertWideCharsAndZeroWidthChars(8, 4, 4, x);

    x = assertReversibleAndReturnX(lineFive.line(), 9);
    assertWideCharsAndZeroWidthChars(9, 5, 4, x);

    x = assertReversibleAndReturnX(lineFive.line(), 10);
    assertWideCharsAndZeroWidthChars(10, 6, 4, x);
  }

  public void testAssertLineSixWithMultipleCombiningMarksWorks() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    // This string is a````=à
    LineInfo lineSix = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 5);
    double x = assertReversibleAndReturnX(lineSix.line(), 0);
    assertEquals(0.0, x);

    // a`
    x = assertReversibleAndReturnXAccountingForZeroWidth(lineSix.line(), 1, 4);
    assertWideCharsAndZeroWidthChars(1, 0, 0, x);

    // a``
    x = assertReversibleAndReturnXAccountingForZeroWidth(lineSix.line(), 2, 3);
    assertWideCharsAndZeroWidthChars(2, 0, 1, x);

    // a```
    x = assertReversibleAndReturnXAccountingForZeroWidth(lineSix.line(), 3, 2);
    assertWideCharsAndZeroWidthChars(3, 0, 2, x);

    // a```, if you do this I hate you
    x = assertReversibleAndReturnXAccountingForZeroWidth(lineSix.line(), 4, 1);
    assertWideCharsAndZeroWidthChars(4, 0, 3, x);

    // a````=
    x = assertReversibleAndReturnX(lineSix.line(), 5);
    assertWideCharsAndZeroWidthChars(5, 0, 4, x);

    // a````=à
    x = assertReversibleAndReturnX(lineSix.line(), 6);
    assertWideCharsAndZeroWidthChars(6, 0, 4, x);
  }

  public void testTextMutationsMarkCacheDirtyWithoutCombiningMarks() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    // The second line is all katakana characters so no combining marks
    LineInfo lineTwo = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 1);

    // we want to build the cache so 'll just ask for a column at the end's x
    calculator.convertColumnToX(lineTwo.line(), lineTwo.line().length() - 1);

    // Lets perform a delete and ensure all is still working :)
    fullUnicodeDocument.deleteText(lineTwo.line(), 5, 1);
    double x = assertReversibleAndReturnX(lineTwo.line(), 4);
    assertWideChars(4, 4, x);
    x = assertReversibleAndReturnX(lineTwo.line(), 5);
    assertWideChars(5, 5, x);
    x = assertReversibleAndReturnX(lineTwo.line(), 6);
    assertWideChars(6, 6, x);

    // Lets perform a non-special insertion.
    fullUnicodeDocument.insertText(lineTwo.line(), 5, "alex");
    x = assertReversibleAndReturnX(lineTwo.line(), 4);
    assertWideChars(4, 4, x);
    x = assertReversibleAndReturnX(lineTwo.line(), 5);
    assertWideChars(5, 5, x);
    x = assertReversibleAndReturnX(lineTwo.line(), 6);
    assertWideChars(6, 5, x);
    x = assertReversibleAndReturnX(lineTwo.line(), 7);
    assertWideChars(7, 5, x);
  }

  public void testMutationsMakesNewLine() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    // The second line is all katakana characters so no combining marks
    LineInfo lineInfo = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 1);

    // we want to build the cache so 'll just ask for a column at the end's x
    calculator.convertColumnToX(lineInfo.line(), lineInfo.line().length() - 1);

    // Lets perform a non-special insertion.
    fullUnicodeDocument.insertText(lineInfo.line(), 5, "al\nex");
    double x = assertReversibleAndReturnX(lineInfo.line(), 4);
    assertWideChars(4, 4, x);
    x = assertReversibleAndReturnX(lineInfo.line(), 5);
    assertWideChars(5, 5, x);
    x = assertReversibleAndReturnX(lineInfo.line(), 6);
    assertWideChars(6, 5, x);
    x = assertReversibleAndReturnX(lineInfo.line(), 7);
    assertWideChars(7, 5, x);

    // Check the new line that was created works right
    lineInfo.moveToNext();
    x = assertReversibleAndReturnX(lineInfo.line(), 1);
    assertWideChars(1, 0, x);
    x = assertReversibleAndReturnX(lineInfo.line(), 2);
    assertWideChars(2, 0, x);
    x = assertReversibleAndReturnX(lineInfo.line(), 3);
    assertWideChars(3, 1, x);
    x = assertReversibleAndReturnX(lineInfo.line(), 4);
    assertWideChars(4, 2, x);
  }

  public void testCorrectWhenMutationsAroundZeroWidthCharacters() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    // We use the accented a from line three for these tests
    LineInfo lineFour = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 3);

    // we want to build the cache so I'll just ask for a column at the end's x
    calculator.convertColumnToX(lineFour.line(), lineFour.line().length() - 1);

    // delete the grave accent combining mark. cache should remove the entry
    // from a as well.
    fullUnicodeDocument.deleteText(lineFour.line(), 1, 1);

    double x = assertReversibleAndReturnX(lineFour.line(), 1);
    assertWideChars(1, 0, x);

    // We do some inserting of zero-width grave accents so we can test the
    // multi-combining mark case (the closest I can get to Arabic craziness).
    fullUnicodeDocument.insertText(lineFour.line(), 1, "\u0300\u0300\u0300");
    // rebuild cache again
    calculator.convertColumnToX(lineFour.line(), lineFour.line().length() - 1);
    // delete the last mark
    fullUnicodeDocument.deleteText(lineFour.line(), 3, 1);

    // Assert all is well, and we measure correctly
    x = assertReversibleAndReturnXAccountingForZeroWidth(lineFour.line(), 2, 1);
    assertWideCharsAndZeroWidthChars(2, 0, 1, x);
  }

  public void testConvertingXToColumn() {
    calculator.handleDocumentChange(fullUnicodeDocument);

    // All characters in this line are double-wide.
    LineInfo lineTwo = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 1);

    // we loop through skipping the \n
    for (int i = 0; i < lineTwo.line().length() - 1; i++) {
      assertXToColumn(lineTwo.line(), i, CHARACTER_SIZE * 2 * i, CHARACTER_SIZE * 2 * (i + 1));
    }

    LineInfo lineThree = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 3);
    assertXToColumn(lineThree.line(), 0, 0, CHARACTER_SIZE); // a
    // =, we bypass the ` automagically since it can't be clicked on
    assertXToColumn(lineThree.line(), 2, CHARACTER_SIZE, CHARACTER_SIZE * 2);
    // à
    assertXToColumn(lineThree.line(), 3, CHARACTER_SIZE * 2, CHARACTER_SIZE * 3);

    LineInfo lineFive = SearchTestsUtil.gotoLineInfo(fullUnicodeDocument, 5);
    assertXToColumn(lineFive.line(), 0, 0, CHARACTER_SIZE); // a
    // =, skip ````
    assertXToColumn(lineFive.line(), 5, CHARACTER_SIZE, CHARACTER_SIZE * 2);
  }

  /**
   * Asserts that a range of x's maps to its corresponding column correctly.
   */
  private void assertXToColumn(Line line, int column, double leftEdgeX, double rightEdgeX) {
    double characterWidth = rightEdgeX - leftEdgeX;
    for (double x = leftEdgeX; x < rightEdgeX; x++) {
      for (RoundingStrategy roundingStrategy : RoundingStrategy.values()) {
        int expectedColumn = roundingStrategy.apply(column + (x - leftEdgeX) / characterWidth);
        int resultColumn = calculator.convertXToColumn(line, x, roundingStrategy);
        assertEquals(expectedColumn, resultColumn);
      }
    }
  }

  /**
   * Converts a column to it's x position. Columns are 0-based.
   */
  private double naiveColumnToX(int column) {
    return measurementProvider.getCharacterWidth() * column;
  }

  /**
   * Converts a column to its x coordinate then converts the x coordinate back
   * to a column ensuring it matches.
   *
   * @return the x coordinate of the column so it can be tested further.
   */
  private double assertReversibleAndReturnX(Line line, int column) {
    return assertReversibleAndReturnXAccountingForZeroWidth(line, column, 0);
  }

  /**
   * Converts a column to its x coordinate then converts the x coordinate back
   * to a column ensuring it matches. This method accounts for any zero width
   * characters so that the assertion when converting x back to column will be
   * correct.
   *
   * @param contiguousZeroWidthChars number of contiguous zero width characters
   *        to the right of the current column (inclusive).
   *
   * @return the x coordinate of the column so it can be tested further.
   */
  private double assertReversibleAndReturnXAccountingForZeroWidth(
      Line line, int column, int contiguousZeroWidthChars) {
    double x = calculator.convertColumnToX(line, column);
    assertEquals(column + contiguousZeroWidthChars,
        calculator.convertXToColumn(line, x, RoundingStrategy.FLOOR));
    return x;
  }

  /**
   * @see #assertWideCharsAndZeroWidthChars(int, int, int, double)
   */
  private void assertWideChars(int column, int wideChars, double x) {
    assertWideCharsAndZeroWidthChars(column, wideChars, 0, x);
  }

  /**
   * @param wideChars Number of tabs
   * @param zeroWidthCharsToLeft Number of zero-width characters to the left of
   *        the column (exclusive).
   */
  private void assertWideCharsAndZeroWidthChars(
      int column, int wideChars, int zeroWidthCharsToLeft, double x) {
    assertEquals(naiveColumnToX(LineDimensionsUtils.getTabWidth() * wideChars)
        + naiveColumnToX(column - wideChars - zeroWidthCharsToLeft), x);
  }

  private static void assertTag(Boolean expected, Line line) {
    Boolean tag = line.getTag(LineDimensionsUtils.NEEDS_OFFSET_LINE_TAG);
    assertEquals("Line " + line.getText(), expected, tag);
  }

  public static final ImmutableList<String> BASIC_NO_SPECIAL_DOCUMENT =
      ImmutableList.of("Listen my children, and you shall hear",
          "Of the midnight ride of Paul Reveare", "On the eighteenth of April in seventy-five",
          "Hardly a man was now alive", "Who remembers that fateful day and year.");

  public static final ImmutableList<String> TAB_AND_CARRIAGE_RETURN_DOCUMENT =
      ImmutableList.of("\t\t\tSome people see a problem and think,",
          "'I know I'll use regular expressions!'.\r", "\tNow they have two problems\r",
          // Whoever types in this line is a real sob....
          "\t\t\t\t\r",
          "This line intentionally left blank so previous line gets a \\n w/o me typing it :)");

  public static final ImmutableList<String> FULL_UNICODE_DOCUMENT =
      ImmutableList.of("\tsimple\tline\r\r", // middle tab forces offset cache.
          "烏烏龍茶烏茶龍龍茶烏龍茶",
          "8\t♜\t♞\t♝\t♛\t♚\t♝\t♞\t♜8",
          // first a is a ` combining mark, second is a single char
          "à=à",
          // ahhh!!!!! Goes LLccLLccLL
          "لضَّالِّين",
          // this is an a + ` x 4
          "à̀̀̀=à");
}
