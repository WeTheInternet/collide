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

package com.google.collide.client.editor.selection;

import static com.google.collide.shared.document.util.LineUtils.getLastCursorColumn;
import static com.google.collide.shared.document.util.LineUtils.rubberbandColumn;

import com.google.collide.client.document.linedimensions.LineDimensionsCalculator.RoundingStrategy;
import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.AnchorUtils;
import com.google.collide.shared.document.anchor.InsertionPlacementStrategy;
import com.google.collide.shared.document.anchor.ReadOnlyAnchor;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.document.util.PositionUtils;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.StringUtils;
import com.google.collide.shared.util.TextUtils;
import com.google.collide.shared.util.UnicodeUtils;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.regexp.shared.RegExp;

import org.waveprotocol.wave.client.common.util.UserAgent;

// TODO: this class is getting huge, time to split responsibilities
/**
 * A class that models the user's selection. In addition to storing the
 * selection and cursor positions, this class listens for mouse drags and other
 * actions that affect the selection.
 *
 * The lifecycle of this class is tied to the current document. When the
 * document is replaced, a new instance of this class is created for the new
 * document.
 */
public class SelectionModel implements Buffer.MouseDragListener {

  /**
   * Enumeration of movement actions.
   */
  public enum MoveAction {
    LEFT,
    RIGHT,
    WORD_LEFT,
    WORD_RIGHT,
    UP,
    DOWN,
    PAGE_UP,
    PAGE_DOWN,
    LINE_START,
    LINE_END,
    TEXT_START,
    TEXT_END
  }

  private static final AnchorType SELECTION_ANCHOR_TYPE = AnchorType.create(SelectionModel.class,
      "selection");

  /**
   * Listener that is called when the user's cursor changes position.
   */
  public interface CursorListener {
    /**
     * @param isExplicitChange true if this change was a result of either the
     *        user moving his cursor or through programatic setting, or false if
     *        it was caused by text mutations in the document
     */
    void onCursorChange(LineInfo lineInfo, int column, boolean isExplicitChange);
  }

  /**
   * Listener that is called when the user changes his selection. This will not
   * be called if the selection's position in the document shifts
   * because of edits elsewhere in the document.
   *
   * Note: The selection is different from the cursor. This will not be called
   * if the user does not have a selection and his cursor moves.
   */
  public interface SelectionListener {
    /**
     * @param oldSelectionRange the selection range before this selection, or
     *        null if there was not a selection
     * @param newSelectionRange the new selection range, or null if there is not
     *        a selection
     */
    void onSelectionChange(Position[] oldSelectionRange, Position[] newSelectionRange);
  }

  private class AnchorListener implements Anchor.ShiftListener {
    @Override
    public void onAnchorShifted(Anchor anchor) {
      if (anchor == cursorAnchor) {
        preferredCursorColumn = anchor.getColumn();
      }

      dispatchCursorChange(false);
    }
  }

  /**
   * A repeating command that continues a user's drag-based selection when the
   * user's mouse pointer moves outside of the editor.
   */
  // TODO: split out MouseDragRepeater into a smaller class
  private class MouseDragRepeater implements RepeatingCommand {
    private static final int REPEAT_PERIOD_MS = 100;

    private int deltaX;
    private int deltaY;

    @Override
    public boolean execute() {
      // check for movement this frame
      if (deltaY == 0 && deltaX == 0) {
        return false;
      }

      LineInfo cursorLineInfo = cursorAnchor.getLineInfo();
      int cursorColumn = cursorAnchor.getColumn();
      int newScrollTop = buffer.getScrollTop() + deltaY;

      if (deltaY != 0) {
        int targetCursorY = deltaY < 0 ? newScrollTop : newScrollTop + buffer.getHeight();
        int cursorLineNumber = buffer.convertYToLineNumber(targetCursorY, true);
        int actualCursorTop = buffer.convertLineNumberToY(cursorLineNumber);
        if (deltaY < 0 && actualCursorTop < newScrollTop && cursorLineNumber > 0) {
          /*
           * The current line is partially visible, increment so we get a fully
           * visible line
           */
          cursorLineNumber++;
        } else if (deltaY > 0 && cursorLineNumber < document.getLastLineNumber()) {
          // See above
          cursorLineNumber--;
        }

        cursorLineInfo = document.getLineFinder().findLine(cursorLineNumber);
      }

      if (deltaX != 0) {
        int targetCursorX =
            buffer.calculateColumnLeft(cursorLineInfo.line(), cursorAnchor.getColumn()) + deltaX;
        cursorColumn = buffer.convertXToRoundedVisibleColumn(targetCursorX, cursorLineInfo.line());
      }

      buffer.setScrollTop(newScrollTop);
      if (viewport.isLineNumberFullyVisibleInViewport(cursorLineInfo.number())) {
        // Only move cursor if the target line is visible inside of viewport
        moveCursorUsingSelectionGranularity(
            cursorLineInfo, buffer.convertColumnToX(cursorLineInfo.line(), cursorColumn), false);
      }

      return true;
    }

    private void schedule(int deltaX, int deltaY) {
      if (this.deltaX == 0 && this.deltaY == 0) {
        // The repeated command is not scheduled, so schedule it
        Scheduler.get().scheduleFixedPeriod(this, REPEAT_PERIOD_MS);
      }

      this.deltaX = deltaX;
      this.deltaY = deltaY;
    }

    private void cancel() {
      deltaX = 0;
      deltaY = 0;
    }
  }

  private enum SelectionGranularity {
    CHARACTER, WORD, LINE;

    private static SelectionGranularity forClickCount(int clickCount) {
      switch (clickCount) {
        case 1:
          return CHARACTER;
        case 2:
          return WORD;
        case 3:
          return LINE;
        default:
          return CHARACTER;
      }
    }
  }

  public static SelectionModel create(Document document, Buffer buffer) {
    ListenerRegistrar.RemoverManager removalManager = new ListenerRegistrar.RemoverManager();
    SelectionModel selection = new SelectionModel(document, buffer, removalManager);
    removalManager.track(buffer.getMouseDragListenerRegistrar().add(selection));

    return selection;
  }

  private Anchor createSelectionAnchor(Line line, int lineNumber, int column, Document document,
      AnchorListener anchorListener) {
    Anchor anchor =
        document.getAnchorManager().createAnchor(SELECTION_ANCHOR_TYPE, line, lineNumber, column);
    anchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    anchor.getShiftListenerRegistrar().add(anchorListener);
    return anchor;
  }

  private final AnchorListener anchorListener;

  /**
   * The anchor of the selection ("anchor" defined as "where the selection
   * began", not "anchor" defined in terms of document anchors).
   */
  private final Anchor baseAnchor;
  private final Buffer buffer;

  /** The cursor of the selection */
  private final Anchor cursorAnchor;
  private final ListenerManager<CursorListener> cursorListenerManager;
  private final Document document;
  /**
   * While the user is dragging, this defines the lower bound for the minimum
   * selection that must be selected regardless of where the user's mouse
   * pointer is. This should be null outside of a drag.
   *
   * For example, if the user is in word-selection mode (by double-clicking to
   * start the selection), the minimum selection will be the initial word that
   * was double-clicked.
   */
  private Anchor minimumDragSelectionLowerBound;
  /** Like {@link #minimumDragSelectionLowerBound}, this defines the upper bound */
  private Anchor minimumDragSelectionUpperBound;
  private final MouseDragRepeater mouseDragRepeater = new MouseDragRepeater();

  /**
   * Tracks the column that the user explicitly moved to. For example, the user
   * moves to line 2, column 80 and then presses the up arrow. Line 1 only has
   * 30 columns, so it will move to column 30, but this will still be column 80
   * so if the user presses the down arrow, it will take him back to column 80.
   */
  private int preferredCursorColumn;
  private SelectionGranularity selectionGranularity = SelectionGranularity.CHARACTER;
  private final ListenerManager<SelectionListener> selectionListenerManager;
  private final ListenerRegistrar.RemoverManager removerManager;

  private ViewportModel viewport;

  private SelectionModel(
      Document document, Buffer buffer, ListenerRegistrar.RemoverManager removerManager) {
    this.document = document;
    this.buffer = buffer;
    this.removerManager = removerManager;
    anchorListener = new AnchorListener();
    cursorAnchor = createSelectionAnchor(document.getFirstLine(), 0, 0, document, anchorListener);
    baseAnchor = createSelectionAnchor(document.getFirstLine(), 0, 0, document, anchorListener);
    cursorListenerManager = ListenerManager.create();
    selectionListenerManager = ListenerManager.create();
  }

  public void deleteSelection(DocumentMutator documentMutator) {
    Preconditions.checkState(hasSelection(), "can't delete selection when there is no selection");
    Position[] selectionRange = getSelectionRange(true);
    /*
     * TODO: optimize. It's currently O(n) where n is the number of
     * lines, but can be O(1) with an additional delete API
     */
    int deleteCount =
        LineUtils.getTextCount(selectionRange[0].getLine(), selectionRange[0].getColumn(),
            selectionRange[1].getLine(), selectionRange[1].getColumn());
    documentMutator.deleteText(selectionRange[0].getLine(), selectionRange[0].getLineNumber(),
        selectionRange[0].getColumn(), deleteCount);
  }

  public void deselect() {
    if (!hasSelection()) {
      return;
    }

    Position[] oldSelectionRange = getSelectionRangeForCallback();
    moveAnchor(baseAnchor, cursorAnchor.getLineInfo(), cursorAnchor.getColumn(), false);
    dispatchSelectionChange(oldSelectionRange);
  }

  public int getBaseColumn() {
    return baseAnchor.getColumn();
  }

  public Line getBaseLine() {
    return baseAnchor.getLine();
  }

  public int getBaseLineNumber() {
    return baseAnchor.getLineNumber();
  }

  public int getCursorColumn() {
    return cursorAnchor.getColumn();
  }

  public Line getCursorLine() {
    return cursorAnchor.getLine();
  }

  public int getCursorLineNumber() {
    return cursorAnchor.getLineNumber();
  }

  public ListenerRegistrar<CursorListener> getCursorListenerRegistrar() {
    return cursorListenerManager;
  }

  public ListenerRegistrar<SelectionListener> getSelectionListenerRegistrar() {
    return selectionListenerManager;
  }

  // TODO: I think we should introduce SelectionRange bean.
  /**
   * Returns the selection range where position[0] is always the logical start
   * of selection and position[1] is always the logical end.
   *
   * @param inclusiveEnd true for the returned position[1] to be the last
   *        character in the selection, false for position[1] to be the
   *        character after the last character in the selection. If true there
   *        must currently be a selection.
   */
  public Position[] getSelectionRange(boolean inclusiveEnd) {
    Preconditions.checkArgument(
        hasSelection() || !inclusiveEnd, "There must be a selection if inclusiveEnd is requested.");
    Position[] selection = new Position[2];

    Anchor beginAnchor = getEarlierSelectionAnchor();
    Anchor endAnchor = getLaterSelectionAnchor();

    selection[0] = new Position(beginAnchor.getLineInfo(), beginAnchor.getColumn());

    if (inclusiveEnd) {
      Preconditions.checkState(hasSelection(),
          "Can't get selection range inclusive end when nothing is selected");
      selection[1] =
          PositionUtils.getPosition(endAnchor.getLine(), endAnchor.getLineNumber(),
              endAnchor.getColumn(), -1);
    } else {
      selection[1] = new Position(endAnchor.getLineInfo(), endAnchor.getColumn());
    }

    return selection;
  }

  public int getSelectionBeginLineNumber() {
    return isCursorAtEndOfSelection() ? baseAnchor.getLineNumber() : cursorAnchor.getLineNumber();
  }

  public int getSelectionEndLineNumber() {
    return isCursorAtEndOfSelection() ? cursorAnchor.getLineNumber()
        : baseAnchor.getLineNumber();
  }

  public boolean hasSelection() {
    return AnchorUtils.compare(cursorAnchor, baseAnchor) != 0;
  }

  public String getSelectedText() {
    if (!hasSelection()) {
      return "";
    }

    Position[] selectionRange = getSelectionRange(true);
    return LineUtils.getText(selectionRange[0].getLine(), selectionRange[0].getColumn(),
        selectionRange[1].getLine(), selectionRange[1].getColumn());
  }

  /**
   * Returns true if the selection spans a newline character.
   */
  public boolean hasMultilineSelection() {
    return cursorAnchor.getLine() != baseAnchor.getLine();
  }

  public boolean isCursorAtEndOfSelection() {
    return AnchorUtils.compare(cursorAnchor, baseAnchor) >= 0;
  }

  /**
   * Performs specified movement action.
   */
  public void move(MoveAction action, boolean isShiftHeld) {
    boolean shouldUpdatePreferredColumn = true;
    int column = cursorAnchor.getColumn();
    LineInfo lineInfo = cursorAnchor.getLineInfo();
    String lineText = lineInfo.line().getText();

    switch (action) {
      case LEFT:
        column = TextUtils.findPreviousNonMarkNorOtherCharacter(lineText, column);
        break;

      case RIGHT:
        column = TextUtils.findNonMarkNorOtherCharacter(lineText, column);
        break;

      case WORD_LEFT:
        column = TextUtils.findPreviousWord(lineText, column, false);
        /**
         * {@link TextUtils#findNextWord} can return line length indicating it's
         * at the end of a word on the line. If this line ends in a* {@code \n}
         * that will cause us to move to the next line when we check
         * {@link LineUtils#getLastCursorColumn} which isn't what we want. So
         * fix it now in case the lines ends in {@code \n}.
         */
        if (column == lineInfo.line().length()) {
          column = rubberbandColumn(lineInfo.line(), column);
        }
        break;

      case WORD_RIGHT:
        column = TextUtils.findNextWord(lineText, column, true);
        /**
         * {@link TextUtils#findNextWord} can return line length indicating it's
         * at the end of a word on the line. If this line ends in a* {@code \n}
         * that will cause us to move to the next line when we check
         * {@link LineUtils#getLastCursorColumn} which isn't what we want. So
         * fix it now in case the lines ends in {@code \n}.
         */
        if (column == lineInfo.line().length()) {
          column = rubberbandColumn(lineInfo.line(), column);
        }
        break;

      case UP:
        column = preferredCursorColumn;
        if (lineInfo.line() == document.getFirstLine() && (isShiftHeld || UserAgent.isMac())) {
          /* 
           * Pressing up on the first line should:
           * - On Mac, always go to first column, or
           * - On all platforms, shift+up should select to first column
           */
          column = 0;
        } else {
          lineInfo.moveToPrevious();
        }

        column = rubberbandColumn(lineInfo.line(), column);
        shouldUpdatePreferredColumn = false;
        break;

      case DOWN:
        column = preferredCursorColumn;
        if (lineInfo.line() == document.getLastLine() && (isShiftHeld || UserAgent.isMac())) {
          // Consistent with up-arrowing on first line
          column = LineUtils.getLastCursorColumn(lineInfo.line());
        } else {
          lineInfo.moveToNext();
        }

        column = rubberbandColumn(lineInfo.line(), column);
        shouldUpdatePreferredColumn = false;
        break;

      case PAGE_UP:
        for (int i = buffer.getFlooredHeightInLines(); i > 0; i--) {
          lineInfo.moveToPrevious();
        }
        column = rubberbandColumn(lineInfo.line(), preferredCursorColumn);
        shouldUpdatePreferredColumn = false;
        break;

      case PAGE_DOWN:
        for (int i = buffer.getFlooredHeightInLines(); i > 0; i--) {
          lineInfo.moveToNext();
        }
        column = rubberbandColumn(lineInfo.line(), preferredCursorColumn);
        shouldUpdatePreferredColumn = false;
        break;

      case LINE_START:
        int firstNonWhitespaceColumn = TextUtils.countWhitespacesAtTheBeginningOfLine(
            lineInfo.line().getText());
        column = (column != firstNonWhitespaceColumn) ? firstNonWhitespaceColumn : 0;
        break;

      case LINE_END:
        column = LineUtils.getLastCursorColumn(lineInfo.line());
        break;

      case TEXT_START:
        lineInfo = new LineInfo(document.getFirstLine(), 0);
        column = 0;
        break;

      case TEXT_END:
        lineInfo = new LineInfo(document.getLastLine(), document.getLineCount() - 1);
        column = LineUtils.getLastCursorColumn(lineInfo.line());
        break;
    }

    if (column < 0) {
      if (lineInfo.moveToPrevious()) {
        column = getLastCursorColumn(lineInfo.line());
      } else {
        column = 0;
      }
    } else if (column > getLastCursorColumn(lineInfo.line())) {
      if (lineInfo.moveToNext()) {
        column = LineUtils.getFirstCursorColumn(lineInfo.line());
      } else {
        column = rubberbandColumn(lineInfo.line(), column);
      }
    }

    moveCursor(lineInfo, column, shouldUpdatePreferredColumn, isShiftHeld,
        getSelectionRangeForCallback());
  }

  @Override
  public void onMouseClick(Buffer buffer, int clickCount, int x, int y, boolean isShiftHeld) {
    int lineNumber = buffer.convertYToLineNumber(y, true);
    LineInfo newLineInfo =
        buffer.getDocument().getLineFinder().findLine(cursorAnchor.getLineInfo(), lineNumber);
    int newColumn = buffer.convertXToRoundedVisibleColumn(x, newLineInfo.line());
    // Allow the user to keep clicking to iterate through selection modes
    clickCount = (clickCount - 1) % 3 + 1;

    selectionGranularity = SelectionGranularity.forClickCount(clickCount);

    if (clickCount == 1) {
      moveCursor(newLineInfo, newColumn, true, isShiftHeld, getSelectionRangeForCallback());
    } else {
      setInitialSelectionForGranularity(newLineInfo, newColumn, x);
    }
  }

  private void setInitialSelectionForGranularity(LineInfo lineInfo, int column, int x) {

    /*
     * If the given column is more the line's length (for example, when appending to the last line
     * of the doc), then just assume no initial selection (since most of that calculation code
     * relies on getting the out-of-bounds character).
     */
    int lineTextLength = lineInfo.line().getText().length();
    if (column >= lineTextLength) {
      moveCursor(lineInfo, lineTextLength, true, false, getSelectionRangeForCallback());
    } else if (selectionGranularity == SelectionGranularity.WORD) {
      Line line = lineInfo.line();
      String text = line.getText();
      if (UnicodeUtils.isWhitespace(text.charAt(column))) {
        moveCursor(lineInfo, column, true, false, getSelectionRangeForCallback());
      } else {
        // Start seeking from the next column so the character under cursor
        // will belong to the "previous word".
        int nextColumn = column + 1;
        int wordStartColumn = TextUtils.findPreviousWord(text, nextColumn, false);
        wordStartColumn = LineUtils.rubberbandColumn(line, wordStartColumn);
        moveAnchor(baseAnchor, lineInfo, wordStartColumn, false);
        moveCursorUsingSelectionGranularity(lineInfo, x, false);
      }
    } else if (selectionGranularity == SelectionGranularity.LINE) {
      moveAnchor(baseAnchor, lineInfo, 0, false);
      moveCursorUsingSelectionGranularity(lineInfo, x, false);
    }
  }

  @Override
  public void onMouseDrag(Buffer buffer, int x, int y) {
    /*
     * The click callback sets up the initial selection, this will become the
     * minimum selection
     */
    ensureMinimumDragSelectionFromCurrentSelection();

    int lineNumber = buffer.convertYToLineNumber(y, true);
    LineInfo newLineInfo =
        document.getLineFinder().findLine(cursorAnchor.getLineInfo(), lineNumber);

    // Only move the cursor
    if (viewport.isLineNumberFullyVisibleInViewport(newLineInfo.number())) {
      moveCursorUsingSelectionGranularity(newLineInfo, x, false);
    }
    manageRepeaterForDrag(x, y);
  }

  private void ensureMinimumDragSelectionFromCurrentSelection() {
    if (minimumDragSelectionLowerBound != null) {
      return;
    }

    Position[] selectionRange = getSelectionRange(false);
    minimumDragSelectionLowerBound = createAnchorFromPosition(selectionRange[0]);
    minimumDragSelectionUpperBound = createAnchorFromPosition(selectionRange[1]);
  }

  private Anchor createAnchorFromPosition(Position position) {
    return document.getAnchorManager().createAnchor(SELECTION_ANCHOR_TYPE, position.getLine(),
        position.getLineInfo().number(), position.getColumn());
  }

  private void removeMinimumDragSelection() {
    if (minimumDragSelectionLowerBound == null) {
      return;
    }

    document.getAnchorManager().removeAnchor(minimumDragSelectionLowerBound);
    document.getAnchorManager().removeAnchor(minimumDragSelectionUpperBound);
    minimumDragSelectionLowerBound = minimumDragSelectionUpperBound = null;
  }

  /**
   * Moves the cursor in the general direction of the {@code targetColumn}, but
   * since this takes into account the {@link #selectionGranularity}, the actual
   * column may be different.
   *
   * @param targetLineInfo the cursor will (mostly) stay within this line. Most
   *        callers will give the line underneath the mouse pointer as this
   *        parameter. (The cursor may move to the next line if the selection
   *        granularity is line.)
   */
  private void moveCursorUsingSelectionGranularity(LineInfo targetLineInfo, int x,
      boolean updatePreferredColumn) {

    Line targetLine = targetLineInfo.line();
    int roundedTargetColumn = buffer.convertXToRoundedVisibleColumn(x, targetLine);
    // Forward if the cursor anchor will be ahead of the base anchor
    boolean growForward =
        AnchorUtils.compare(baseAnchor, targetLineInfo.number(), roundedTargetColumn) <= 0;

    LineInfo newLineInfo = targetLineInfo;
    int newColumn = roundedTargetColumn;

    switch (selectionGranularity) {
      case WORD:
        if (growForward) {
          /*
           * Floor the column so the last pixel of the last character of the
           * current word does not trigger a finding of the next word
           */
          newColumn =
              TextUtils.findNextWord(
                  targetLine.getText(),
                  buffer.convertXToColumn(x, targetLine, RoundingStrategy.FLOOR), false);
        } else {
          // See note above about flooring, but we ceil here instead
          newColumn =
              TextUtils.findPreviousWord(
                  targetLine.getText(),
                  buffer.convertXToColumn(x, targetLine, RoundingStrategy.CEIL), false);
        }
        break;

      case LINE:
        // The cursor is on column 0 regardless
        newColumn = 0;
        if (growForward) {
          // If growing forward, move to the next line, if possible
          newLineInfo = targetLineInfo.copy();
          if (!newLineInfo.moveToNext()) {
            /*
             * There isn't a next line, so just move the cursor to the end of
             * line
             */
            newColumn = LineUtils.getLastCursorColumn(newLineInfo.line());
          }
        }
        break;
    }

    Position[] oldSelectionRange = getSelectionRangeForCallback();

    newColumn = LineUtils.rubberbandColumn(newLineInfo.line(), newColumn);
    ensureNewSelectionObeysMinimumDragSelection(newLineInfo, newColumn);

    moveCursor(newLineInfo, newColumn, updatePreferredColumn, true, oldSelectionRange);
  }

  private void ensureNewSelectionObeysMinimumDragSelection(LineInfo newCursorLineInfo,
      int newCursorColumn) {

    if (minimumDragSelectionLowerBound == null
        || AnchorUtils.compare(minimumDragSelectionLowerBound,
            minimumDragSelectionUpperBound) == 0) {
      // There isn't a minimum drag selection set
      return;
    }

    // Is the new selection growing forward?
    boolean newGrowForward =
        AnchorUtils.compare(baseAnchor, newCursorLineInfo.number(), newCursorColumn) <= 0;

    boolean newSelectionIsAheadOfMinimum =
        newGrowForward && AnchorUtils.compare(baseAnchor, minimumDragSelectionUpperBound) >= 0;
    boolean newSelectionIsBehindMinimum =
        !newGrowForward && AnchorUtils.compare(baseAnchor, minimumDragSelectionLowerBound) <= 0;

    // Move base anchor to correct minimum selection bound
    Anchor newBaseAnchorPosition = null;
    if (newSelectionIsBehindMinimum) {
      newBaseAnchorPosition = minimumDragSelectionUpperBound;
    } else if (newSelectionIsAheadOfMinimum) {
      newBaseAnchorPosition = minimumDragSelectionLowerBound;
    }

    if (newBaseAnchorPosition != null) {
      moveAnchor(baseAnchor, newBaseAnchorPosition.getLineInfo(),
          newBaseAnchorPosition.getColumn(), false);
    }
  }

  private void manageRepeaterForDrag(int x, int y) {
    int bufferScrollLeft = buffer.getScrollLeft();
    int bufferScrollTop = buffer.getScrollTop();
    int bufferHeight = buffer.getHeight();
    int bufferWidth = buffer.getWidth();

    int deltaX = 0;
    int deltaY = 0;

    if (y - bufferScrollTop < 0) {
      deltaY = y - bufferScrollTop;
    } else if (y >= bufferScrollTop + bufferHeight) {
      deltaY = y - (bufferScrollTop + bufferHeight);
    }

    if (x - bufferScrollLeft < 0) {
      deltaX = x - bufferScrollLeft;
    } else if (x >= bufferScrollLeft + bufferWidth) {
      deltaX = x - (bufferScrollLeft + bufferWidth);
    }

    if (deltaX == 0 && deltaY == 0) {
      mouseDragRepeater.cancel();
    } else {
      mouseDragRepeater.schedule(deltaX, deltaY);
    }
  }

  @Override
  public void onMouseDragRelease(Buffer buffer, int x, int y) {
    mouseDragRepeater.cancel();
    removeMinimumDragSelection();
  }

  public void setSelection(LineInfo baseLineInfo, int baseColumn, LineInfo cursorLineInfo,
      int cursorColumn) {

    Preconditions.checkArgument(baseColumn <= LineUtils.getLastCursorColumn(baseLineInfo.line()),
        "The base column is out-of-bounds");
    int lastCursorColumn = LineUtils.getLastCursorColumn(cursorLineInfo.line());
    Preconditions.checkArgument(cursorColumn <= lastCursorColumn,
        "The cursor column is out-of-bounds. Expected <= " + lastCursorColumn
            + ", got " + cursorColumn + ", line " + cursorLineInfo.number());

    baseColumn = LineUtils.rubberbandColumn(baseLineInfo.line(), baseColumn);
    cursorColumn = LineUtils.rubberbandColumn(cursorLineInfo.line(), cursorColumn);

    Position[] oldSelectionRange = getSelectionRangeForCallback();

    moveAnchor(baseAnchor, baseLineInfo, baseColumn, false);
    boolean hasSelection =
        LineUtils.comparePositions(cursorLineInfo.number(), cursorColumn, baseLineInfo.number(),
            baseColumn) != 0;
    moveCursor(cursorLineInfo, cursorColumn, true, hasSelection, oldSelectionRange);
  }

  public void setCursorPosition(LineInfo lineInfo, int column) {
    int lastCursorColumn = LineUtils.getLastCursorColumn(lineInfo.line());
    Preconditions.checkArgument(column <= lastCursorColumn,
        "The cursor column is out-of-bounds. Expected <= " + lastCursorColumn
            + ", got " + column + ", line " + lineInfo.number());
    moveCursor(lineInfo, column, true, hasSelection(), getSelectionRangeForCallback());
  }

  public void selectAll() {
    Position[] oldSelectionRange = getSelectionRangeForCallback();

    moveAnchor(baseAnchor, new LineInfo(document.getFirstLine(), 0), 0, false);
    moveCursor(new LineInfo(document.getLastLine(), document.getLastLineNumber()),
        LineUtils.getLastCursorColumn(document.getLastLine()), true, true, oldSelectionRange);
  }

  public void teardown() {
    removerManager.remove();
    if (baseAnchor != cursorAnchor) {
      document.getAnchorManager().removeAnchor(baseAnchor);
    }
  }

  public ReadOnlyAnchor getCursorAnchor() {
    return cursorAnchor;
  }

  public Position getCursorPosition() {
    return new Position(cursorAnchor.getLineInfo(), cursorAnchor.getColumn());
  }

  private void dispatchCursorChange(final boolean isExplicitChange) {
    cursorListenerManager.dispatch(new Dispatcher<SelectionModel.CursorListener>() {
      @Override
      public void dispatch(CursorListener listener) {
        listener.onCursorChange(cursorAnchor.getLineInfo(), cursorAnchor.getColumn(),
            isExplicitChange);
      }
    });
  }

  private void dispatchSelectionChange(final Position[] oldSelectionRange) {
    selectionListenerManager.dispatch(new Dispatcher<SelectionModel.SelectionListener>() {
      @Override
      public void dispatch(SelectionListener listener) {
        listener.onSelectionChange(oldSelectionRange, getSelectionRangeForCallback());
      }
    });
  }

  private Position[] getSelectionRangeForCallback() {
    return hasSelection() ? getSelectionRange(true) : null;
  }

  /**
   * Moves the cursor and potentially the base. This method will dispatch the
   * appropriate callbacks.
   *
   * @param lineInfo the line where the cursor will be positioned
   * @param column the column (on the given line) where the cursor will be
   *        positioned
   * @param updatePreferredColumn see {@link #preferredCursorColumn}
   * @param isSelecting false to ensure there is not a selection after the
   *        movement
   * @param oldSelectionRange the selection range (via
   *        {@link #getSelectionRangeForCallback()}) before the caller modified
   *        the selection. This will be passed to the selection callback as the
   *        old selection range.
   */
  private void moveCursor(LineInfo lineInfo, int column, boolean updatePreferredColumn,
      boolean isSelecting, Position[] oldSelectionRange) {

    boolean hadSelection = hasSelection();

    // Check if base anchor should move
    if (!isSelecting) {
      moveAnchor(baseAnchor, lineInfo, column, false);
    }

    // Move cursor anchor
    moveAnchor(cursorAnchor, lineInfo, column, updatePreferredColumn);

    boolean willHaveSelection = hasSelection();

    dispatchCursorChange(true);
    if (isSelecting || willHaveSelection != hadSelection) {
      dispatchSelectionChange(oldSelectionRange);
    }
  }

  private void moveAnchor(Anchor anchor, LineInfo lineInfo, int column,
      boolean updatePreferredColumn) {

    if (anchor.getLine().equals(lineInfo.line()) && anchor.getColumn() == column) {
      return;
    }

    if (updatePreferredColumn) {
      preferredCursorColumn = column;
    }

    document.getAnchorManager().moveAnchor(anchor, lineInfo.line(), lineInfo.number(), column);
  }

  public void initialize(ViewportModel viewport) {
    this.viewport = viewport;
  }

  /**
   * Sets specified strategy to earlier selection anchor and runs routine;
   * initial strategy is restored before return.
   */
  private void runWithEarlierAnchorPlacementStrategy(InsertionPlacementStrategy strategy,
      Runnable runnable) {
    Anchor earlierSelectionAnchor = getEarlierSelectionAnchor();
    InsertionPlacementStrategy existingInsertionPlacementStrategy =
        earlierSelectionAnchor.getInsertionPlacementStrategy();
    earlierSelectionAnchor.setInsertionPlacementStrategy(strategy);

    try {
      runnable.run();
    } finally {
      earlierSelectionAnchor.setInsertionPlacementStrategy(existingInsertionPlacementStrategy);
    }
  }

  public void toggleComments(final DocumentMutator documentMutator,
      final RegExp commentChecker, final String commentHead) {
    if (hasSelection()) {
      runWithEarlierAnchorPlacementStrategy(
          InsertionPlacementStrategy.EARLIER, new Runnable() {
        @Override
        public void run() {
          toggleCommentsAssumingEarlierSelectionAnchorWontShift(
              documentMutator, commentChecker, commentHead);
        }
      });
    } else {
      toggleCommentsAssumingEarlierSelectionAnchorWontShift(
          documentMutator, commentChecker, commentHead);
    }
  }

  private void toggleCommentsAssumingEarlierSelectionAnchorWontShift(
      DocumentMutator documentMutator, RegExp commentChecker, String commentHead) {
    new ToggleCommentsController(commentChecker, commentHead).processLines(documentMutator, this);
  }

  /**
   * Adjusts the indentation (either indents or dedents) of the line(s) in the
   * selection.
   */
  public void adjustSelectionIndentation(
      final DocumentMutator documentMutator, final String tabString, final boolean indent) {
    runWithEarlierAnchorPlacementStrategy(InsertionPlacementStrategy.EARLIER, new Runnable() {
      @Override
      public void run() {
        adjustSelectionIndentationAssumingEarlierSelectionAnchorWontShift(
            documentMutator, tabString, indent);
      }
    });
  }

  private void adjustSelectionIndentationAssumingEarlierSelectionAnchorWontShift(
      final DocumentMutator documentMutator, final String tabString, final boolean indent) {
    Position[] selectionRange = getSelectionRange(false);
    Line terminator = selectionRange[1].getLine();
    if (selectionRange[1].getColumn() != 0 || !hasSelection()) {
      terminator = terminator.getNextLine();
    }

    int lineNumber = selectionRange[0].getLineNumber();
    Line line = selectionRange[0].getLine();

    while (line != terminator) {
      if (indent) {
        documentMutator.insertText(line, lineNumber, 0, tabString, false);
      } else {
        int toDelete = StringUtils.findCommonPrefixLength(tabString, line.getText());
        if (toDelete > 0) {
          documentMutator.deleteText(line, 0, toDelete);
        }
      }
      lineNumber++;
      line = line.getNextLine();
    }
  }

  private Anchor getEarlierSelectionAnchor() {
    return isCursorAtEndOfSelection() ? baseAnchor : cursorAnchor;
  }

  private Anchor getLaterSelectionAnchor() {
    return isCursorAtEndOfSelection() ? cursorAnchor : baseAnchor;
  }
}
