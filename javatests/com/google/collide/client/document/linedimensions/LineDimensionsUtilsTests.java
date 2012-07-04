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

import com.google.collide.client.document.linedimensions.LineDimensionsCalculatorTests.TestMeasurementProvider;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;

import junit.framework.TestCase;

/**
 * Tests for {@link LineDimensionsUtils}.
 */
public class LineDimensionsUtilsTests extends TestCase {

  public void testInsertionSpaceBeforeTab() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 0, " ");
    assertTrue(LineDimensionsUtils.needsOffset(line));
  }

  public void testInsertionTabBeforeTab() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 0, "\t");
    assertFalse(LineDimensionsUtils.needsOffset(line));
  }

  public void testInsertionSpaceAfter() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 1, " ");
    assertFalse(LineDimensionsUtils.needsOffset(line));
  }

  public void testInsertionTabAfterTab() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 1, "\t");
    assertFalse(LineDimensionsUtils.needsOffset(line));
  }

  public void testInsertionSpaceAtTheEndOfLine() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 4, " ");
    assertFalse(LineDimensionsUtils.needsOffset(line));
  }

  public void testInsertionTabTheEndOfLine() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 4, "\t");
    assertTrue(LineDimensionsUtils.needsOffset(line));
  }

  public void testInsertionSimpleMultiline() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 1, "qwe\n\t");
    assertFalse(LineDimensionsUtils.needsOffset(line));
    assertFalse(LineDimensionsUtils.needsOffset(line.getNextLine()));
  }

  public void testInsertionSpecialMultiline() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 1, "一二三\n\t");
    assertTrue(LineDimensionsUtils.needsOffset(line));
    assertFalse(LineDimensionsUtils.needsOffset(line.getNextLine()));
  }

  public void testInsertionSimpleMultilineAtTheBeginningOfLine() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 0, "qwe\n\t");
    assertFalse(LineDimensionsUtils.needsOffset(line));
    assertFalse(LineDimensionsUtils.needsOffset(line.getNextLine()));
  }

  public void testInsertionSpecialMultilineAtTheBeginningOfLine() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 0, "一二三\n\t");
    assertTrue(LineDimensionsUtils.needsOffset(line));
    assertFalse(LineDimensionsUtils.needsOffset(line.getNextLine()));
  }

  public void testInsertionSpecialMultilineAtTheEndOfLine() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 4, "一二三\n\tText");
    assertTrue(LineDimensionsUtils.needsOffset(line));
    assertFalse(LineDimensionsUtils.needsOffset(line.getNextLine()));
  }

  public void testInsertionThatMakesNextLineSpecial() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 1, "一二三\n \t");
    assertTrue(LineDimensionsUtils.needsOffset(line));
    assertTrue(LineDimensionsUtils.needsOffset(line.getNextLine()));
  }

  public void testInsertionOfLineBreakAtTheEndOfLastLine() {
    Document doc = Document.createFromString("\t123");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 4, "\n");
    assertFalse(LineDimensionsUtils.needsOffset(line));
    assertFalse(LineDimensionsUtils.needsOffset(line.getNextLine()));
  }

  public void testInsertionOfLineBreakAtTheEndOfLine() {
    Document doc = Document.createFromString("\t123\n456");
    LineDimensionsCalculator.createWithCustomProvider(new TestMeasurementProvider(0))
        .handleDocumentChange(doc);
    Line line = doc.getFirstLine();
    assertFalse(LineDimensionsUtils.needsOffset(line));
    doc.insertText(line, 0, 4, "\n");
    assertFalse(LineDimensionsUtils.needsOffset(line));
    assertFalse(LineDimensionsUtils.needsOffset(line.getNextLine()));
  }
}
