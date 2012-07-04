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
import com.google.collide.shared.document.TextChange.Type;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;

/**
 * Mutator for the document that provides high-level document mutation API. The
 * {@link Document} delegates to this class to actually perform the mutations.
 */
class DocumentMutatorImpl implements DocumentMutator {

  private class TextDeleter {
    private final JsonArray<Anchor> anchorsInDeletedRangeToRemove = JsonCollections.createArray();
    private final JsonArray<Anchor> anchorsInDeletedRangeToShift = JsonCollections.createArray();
    private final JsonArray<Anchor> anchorsLeftoverFromLastLine = JsonCollections.createArray();
    private int column;
    private int deleteCountForCurLine;
    private final int firstLineColumn;
    private final Line firstLine;
    private final int firstLineNumber;
    private final String firstLineChunk;
    private Line curLine;
    private int curLineNumber;
    private int remainingDeleteCount;

    private TextDeleter(Line line, int lineNumber, int column, String deletedText) {
      firstLine = this.curLine = line;
      firstLineNumber = this.curLineNumber = lineNumber;
      firstLineColumn = this.column = column;
      this.remainingDeleteCount = deletedText.length();

      firstLineChunk = line.getText().substring(0, column);
    }

    void delete() {
      JsonArray<Line> removedLines = JsonCollections.createArray();

      boolean wasNewlineCharDeleted = deleteFromCurLine(true);

      // All deletes on subsequent lines will start at column 0
      if (remainingDeleteCount > 0) {
        column = 0;

        do {
          iterateToNextLine();
          wasNewlineCharDeleted = deleteFromCurLine(false);
          removedLines.add(curLine);
        } while (remainingDeleteCount > 0);
      }

      if (wasNewlineCharDeleted) {
        /*
         * Must join the next line with the current line. Setting
         * deleteCountForLine = 0 will have a nice effect of naturally joining
         * the line.
         */
        iterateToNextLine();
        column = 0;
        deleteCountForCurLine = 0;
        removeLineImpl(curLine);
        removedLines.add(curLine);
      }

      // Move any leftover text on the last line to the first line
      boolean lastLineIsEmpty = curLine.getText().length() == 0;
      boolean lastLineWillHaveLeftoverText =
          deleteCountForCurLine < curLine.getText().length();
      int lastLineFirstUntouchedColumn = column + deleteCountForCurLine;
      if (lastLineWillHaveLeftoverText || lastLineIsEmpty) {
        anchorManager.handleTextDeletionLastLineLeftover(anchorsLeftoverFromLastLine, firstLine,
            curLine, lastLineFirstUntouchedColumn);
      }

      String lastLineChunk = curLine.getText().substring(lastLineFirstUntouchedColumn);
      firstLine.setText(firstLineChunk + lastLineChunk);

      int numberOfDeletedLines = curLineNumber - firstLineNumber;

      anchorManager.handleTextDeletionFinished(anchorsInDeletedRangeToRemove,
          anchorsInDeletedRangeToShift, anchorsLeftoverFromLastLine, firstLine, firstLineNumber,
          firstLineColumn, numberOfDeletedLines, lastLineFirstUntouchedColumn);

      if (numberOfDeletedLines > 0) {
        document.commitLineCountChange(-numberOfDeletedLines);
        document.dispatchLineRemoved(firstLineNumber + 1, removedLines);
      }
    }

    /**
     * Deletes the current line's text to be deleted.
     *
     * @return whether a newline character was deleted
     */
    private boolean deleteFromCurLine(boolean isFirstLine) {
      int maxDeleteCountForCurLine = curLine.getText().length() - column;
      deleteCountForCurLine = Math.min(maxDeleteCountForCurLine, remainingDeleteCount);

      anchorManager.handleTextPredeletionForLine(curLine, column, deleteCountForCurLine,
          anchorsInDeletedRangeToRemove, anchorsInDeletedRangeToShift, isFirstLine);

      /*
       * All lines but the first should be removed from the document (either
       * they have no text remaining, or in the case of a partial selection on
       * the last line, the leftover text will be moved to the first line.)
       */
      if (!isFirstLine) {
        removeLineImpl(curLine);
      }

      remainingDeleteCount -= deleteCountForCurLine;

      int lastCharDeletedIndex = column + deleteCountForCurLine - 1;
      return lastCharDeletedIndex >= 0 ? curLine.getText().charAt(lastCharDeletedIndex) == '\n'
          : false;
    }

    private void iterateToNextLine() {
      curLine = curLine.getNextLine();
      curLineNumber++;
      ensureCurLine();
    }

    private void ensureCurLine() {
      if (curLine == null) {
        throw new IndexOutOfBoundsException(
            "Reached end of document so could not delete the requested remaining "
                + remainingDeleteCount + " characters");
      }
    }
  }

  private AnchorManager anchorManager;
  private final Document document;
  private final JsonArray<TextChange> textChanges;

  DocumentMutatorImpl(Document document) {
    this.document = document;
    this.anchorManager = document.getAnchorManager();

    textChanges = JsonCollections.createArray();
  }

  @Override
  public TextChange deleteText(Line line, int column, int deleteCount) {
    return deleteText(line, document.getLineFinder().findLine(line).number(), column, deleteCount);
  }

  @Override
  public TextChange deleteText(Line line, int lineNumber, int column, int deleteCount) {

    if (deleteCount == 0) {
      // Delete 0 is a NOOP.
      return TextChange.createDeletion(line, lineNumber, column, "");
    }

    if (column >= line.getText().length()) {
      throw new IndexOutOfBoundsException("Attempt to delete text at column " + column
          + " which is greater than line length " + line.getText().length() + "(line text is: "
          + line.getText() + ")");
    }

    String deletedText = document.getText(line, column, deleteCount);

    beginHighLevelModification(TextChange.Type.DELETE, line, lineNumber, column, deletedText);
    TextChange textChange = TextChange.createDeletion(line, lineNumber, column, deletedText);
    textChanges.add(textChange);
    deleteTextImpl(line, lineNumber, column, deletedText);
    endHighLevelModification();

    return textChange;
  }

  @Override
  public TextChange insertText(Line line, int column, String text) {
    return insertText(line, document.getLineFinder().findLine(line).number(), column, text);
  }

  @Override
  public TextChange insertText(Line line, int lineNumber, int column, String text) {

    if (column > LineUtils.getLastCursorColumn(line)) {
      throw new IndexOutOfBoundsException("Attempt to insert text at column " + column
          + " which is greater than line length " + line.getText().length() + "(line text is: "
          + line.getText() + ")");
    }

    beginHighLevelModification(TextChange.Type.INSERT, line, lineNumber, column, text);
    LineInfo lastLineModified = insertTextImpl(line, lineNumber, column, text);
    TextChange textChange =
        TextChange.createInsertion(line, lineNumber, column, lastLineModified.line(),
            lastLineModified.number(), text);
    textChanges.add(textChange);
    endHighLevelModification();

    return textChange;
  }

  @Override
  public TextChange insertText(Line line, int lineNumber, int column, String text,
      boolean canReplaceSelection) {

    // This (lowest-level) document mutator should never replace the selection
    return insertText(line, lineNumber, column, text);
  }

  private void beginHighLevelModification(
      Type type, Line line, int lineNumber, int column, String text) {
    // Clear any change-tracking state
    textChanges.clear();
    // Dispatch the pre-textchange event
    document.dispatchPreTextChange(type, line, lineNumber, column, text);
  }

  private void endHighLevelModification() {
    // Dispatch callbacks
    document.dispatchTextChange(textChanges);
  }

  private LineInfo insertTextImpl(Line line, int lineNumber, int column, String text) {
    if (!text.contains("\n")) {
      insertTextOnOneLineImpl(line, column, text);
      return new LineInfo(line, lineNumber);
    } else {
      return insertMultilineTextImpl(line, lineNumber, column, text);
    }
  }

  private void insertTextOnOneLineImpl(Line line, int column, String text) {
    // Add the text first
    String oldText = line.getText();
    String newText = oldText.substring(0, column) + text + oldText.substring(column);
    line.setText(newText);

    // Update the anchors
    anchorManager.handleSingleLineTextInsertion(line, column, text.length());
  }

  /**
   * @return the line info for the last line modified by this insertion
   */
  private LineInfo insertMultilineTextImpl(Line line, int lineNumber, int column, String text) {
    String lineText = line.getText();
    Preconditions.checkArgument(lineText.endsWith("\n") ? column < lineText.length()
        : column <= lineText.length(), "Given column is out-of-bounds");
    
    JsonArray<Line> linesAdded = JsonCollections.createArray();

    /*
     * The given "line" has two chunks of text: from column 0 to the "column"
     * (exclusive), and from the "column" to the end. The new contents of this
     * line will be: its first chunk + the first line of the inserted text +
     * newline. The second chunk will be used to form a brand new line whose
     * contents are: the last line of the inserted text + the second chunk
     * (which still consists of a newline if it had one originally). In between
     * these two lines will be the inserted text's second line through to the
     * second-to-last line.
     */

    // First, split the line receiving the text
    String firstChunk = lineText.substring(0, column);
    String secondChunk = lineText.substring(column);
    JsonArray<String> insertionLineTexts = StringUtils.split(text, "\n");
    
    line.setText(firstChunk + insertionLineTexts.get(0) + "\n");

    Line prevLine = line;
    int prevLineNumber = lineNumber;
    for (int i = 1, nMinusOne = insertionLineTexts.size() - 1; i < nMinusOne; i++) {
      Line curLine = Line.create(document, insertionLineTexts.get(i) + "\n");
      insertLineImpl(prevLine, curLine);
      linesAdded.add(curLine);

      prevLine = curLine;
      prevLineNumber++;
    }

    /*
     * Remember that if e.g. the insertion text is "a\n", the last item in the
     * array will be the empty string
     */
    String lastInsertionLineText = insertionLineTexts.get(insertionLineTexts.size() - 1);
    String newLineText = lastInsertionLineText + secondChunk;
    int secondChunkColumnInNewLine = lastInsertionLineText.length();
    Line newLine = Line.create(document, newLineText);
    insertLineImpl(prevLine, newLine);
    linesAdded.add(newLine);

    int newLineNumber = prevLineNumber + 1;
    anchorManager.handleMultilineTextInsertion(line, lineNumber, column, newLine, newLineNumber,
        secondChunkColumnInNewLine);

    document.commitLineCountChange(linesAdded.size());
    document.dispatchLineAdded(lineNumber + 1, linesAdded);

    return new LineInfo(newLine, newLineNumber);
  }

  /**
   * Low-level operation that inserts the given line after the previous line.
   *
   * @param previousLine the line after which {@code line} will be inserted
   */
  private void insertLineImpl(Line previousLine, Line line) {
    Line nextLine = previousLine.getNextLine();

    // Update the linked list
    previousLine.setNextLine(line);
    line.setPreviousLine(previousLine);

    if (nextLine != null) {
      nextLine.setPreviousLine(line);
      line.setNextLine(nextLine);
    }

    // Update document state
    if (nextLine == document.getFirstLine()) {
      document.setFirstLine(line);
    }
    if (previousLine == document.getLastLine()) {
      document.setLastLine(line);
    }

    line.setAttached(true);
  }

  private void deleteTextImpl(final Line firstLine, final int firstLineNumber,
      final int firstLineColumn, final String deletedText) {
    Preconditions.checkArgument(firstLineColumn <= LineUtils.getLastCursorColumn(firstLine),
        "The column is out-of-bounds");
    new TextDeleter(firstLine, firstLineNumber, firstLineColumn, deletedText).delete();
  }

  /**
   * A low-level operation that removes the line from the document. This method
   * is meant to only be called by
   * {@link #deleteTextImpl(Line, int, int, String)}.
   */
  private void removeLineImpl(Line line) {
    /*
     * TODO: set detached state, and assert/throw exceptions if any
     * one tries to operate on the detached line
     */

    // Update the linked list and document's first and last lines
    Line previousLine = line.getPreviousLine();
    Line nextLine = line.getNextLine();

    if (previousLine != null) {
      previousLine.setNextLine(nextLine);
    } else {
      assert line == document.getFirstLine() :
          "Line does not have a previous line, but line is not first line in document";
      document.setFirstLine(nextLine);
    }

    if (nextLine != null) {
      nextLine.setPreviousLine(previousLine);
    } else {
      assert line == document.getLastLine() :
          "Line does not have a next line, but line is not last line in document";
      document.setLastLine(previousLine);
    }

    line.setAttached(false);
  }
}
