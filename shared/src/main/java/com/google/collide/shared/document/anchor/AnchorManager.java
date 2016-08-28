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

package com.google.collide.shared.document.anchor;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.InsertionPlacementStrategy.Placement;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.SortedList;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

// TODO: need to make an interface for the truly public methods
/**
 * Manager for anchors within a document.
 *
 */
public class AnchorManager {

  /**
   * Visitor that is called for each anchor in some collection of anchors.
   */
  public interface AnchorVisitor {
    void visitAnchor(Anchor anchor);
  }

  /*
   * Much logic relies on the fact that in a line's anchor list, these will
   * appear before anchors that care about column numbers.
   */
  /**
   * Constant value for the line number to indicate that the anchor does not
   * care about its line number.
   */
  public static final int IGNORE_LINE_NUMBER = -1;

  /**
   * Constant value for the column to indicate that the anchor does not have a
   * column.
   *
   *  This is useful for anchors that only care about knowing movements of
   * lines, and not specific columns. For example, a viewport may use this for
   * the viewport top and viewport bottom.
   */
  public static final int IGNORE_COLUMN = -1;

  private static final String LINE_TAG_ANCHORS = AnchorManager.class.getName() + ":Anchors";

  private final LineAnchorList lineAnchors;

  // TODO: not really public
  public AnchorManager() {
    lineAnchors = new LineAnchorList();
  }

  /**
   * @param anchorType the type of the anchor
   * @param line the line the anchor should attach to
   * @param lineNumber the line number, or {@link #IGNORE_LINE_NUMBER} if this
   *        anchor is not interested in its line number
   * @param column the column, or {@link #IGNORE_COLUMN} if this anchor is not
   *        positioned on a column
   */
  public Anchor createAnchor(AnchorType anchorType, Line line, int lineNumber, int column) {
    Anchor anchor = new Anchor(anchorType, line, lineNumber, column);

    getAnchors(line).add(anchor);
    if (lineNumber != IGNORE_LINE_NUMBER) {
      lineAnchors.add(anchor);
    }

    return anchor;
  }

  /**
   * Searches the given {@code line} for an anchor with a line number, or returns null.
   */
  public Anchor findAnchorWithLineNumber(Line line) {
    AnchorList anchors = getAnchorsOrNull(line);
    if (anchors == null) {
      return null;
    }

    for (int i = 0, n = anchors.size(); i < n; i++) {
      if (anchors.get(i).hasLineNumber()) {
        return anchors.get(i);
      }
    }

    return null;
  }

  /**
   * Finds the closest anchor with a line number, or null. The anchor may be
   * positioned on another line.
   */
  public Anchor findClosestAnchorWithLineNumber(int lineNumber) {

    if (lineAnchors.size() == 0) {
      return null;
    }

    int insertionIndex = lineAnchors.findInsertionIndex(lineNumber);
    if (insertionIndex == lineAnchors.size()) {
      return lineAnchors.get(insertionIndex - 1);
    } else if (insertionIndex == 0) {
      return lineAnchors.get(0);
    }

    int distanceFromPreviousAnchor =
        lineNumber - lineAnchors.get(insertionIndex - 1).getLineNumber();
    int distanceFromNextAnchor = lineAnchors.get(insertionIndex).getLineNumber() - lineNumber;
    return distanceFromNextAnchor < distanceFromPreviousAnchor
        ? lineAnchors.get(insertionIndex) : lineAnchors.get(insertionIndex - 1);
  }

  /**
   * Returns the list of anchors on the given line. The returned instance is the
   * original list, not a copy.
   * 
   * @see #getAnchorsOrNull(Line)
   */
  @VisibleForTesting
  public static AnchorList getAnchors(Line line) {
    AnchorList columnAnchors = line.getTag(LINE_TAG_ANCHORS);
    if (columnAnchors == null) {
      columnAnchors = new AnchorList();
      line.putTag(LINE_TAG_ANCHORS, columnAnchors);
    }

    return columnAnchors;
  }

  /**
   * Returns the list of anchors on the given line (the returned instance is the
   * original list, not a copy), or null if there are no anchors
   */
  static AnchorList getAnchorsOrNull(Line line) {
    return line.getTag(LINE_TAG_ANCHORS);
  }

  /**
   * Returns anchors of the given type on the given line, or null if there are
   * no anchors of any kind on the given line.
   */
  public static JsonArray<Anchor> getAnchorsByTypeOrNull(Line line, AnchorType type) {
    AnchorList anchorList = getAnchorsOrNull(line);
    if (anchorList == null) {
      return null;
    }
    JsonArray<Anchor> anchors = JsonCollections.createArray();
    for (int i = 0; i < anchorList.size(); i++) {
      Anchor anchor = anchorList.get(i);
      if (type.equals(anchor.getType())) {
        anchors.add(anchor);
      }
    }
    return anchors;
  }

  @VisibleForTesting
  public LineAnchorList getLineAnchors() {
    return lineAnchors;
  }
  
  /**
   * Finds the next anchor relative to the one given or null if there are no
   * subsequent anchors.
   */
  public Anchor getNextAnchor(Anchor anchor) {
    return getAdjacentAnchor(anchor, null, true);
  }

  /**
   * Finds the next anchor of the given type relative to the one given or null
   * if there are no subsequent anchors of that type.
   */
  public Anchor getNextAnchor(Anchor anchor, AnchorType type) {
    return getAdjacentAnchor(anchor, type, true);
  }

  /**
   * Finds the previous anchor relative to the one given or null if there are no
   * preceding anchors.
   */
  public Anchor getPreviousAnchor(Anchor anchor) {
    return getAdjacentAnchor(anchor, null, false);
  }

  /**
   * Finds the previous anchor of the given type relative to the one given or
   * null if there are no preceding anchors of that type.
   */
  public Anchor getPreviousAnchor(Anchor anchor, AnchorType type) {
    return getAdjacentAnchor(anchor, type, false);
  }

  /**
   * Private utility method that performs the search for getNextAnchor/getPreviousAnchor
   *
   *  For now, the performance is O(lines + anchors)
   *
   *  TODO: Instead we should consider maintaining a separate anchor.
   *  TODO: avoid recusrion list. This should let us easily achieve O(anchors)
   *   or better.
   *
   * @param anchor the @{link Anchor} anchor to start the search from
   * @param type the @{link AnchorType} of the anchor, or null to capture any type.
   * @param next true if the search is forwards, false for backwards
   * @return the adjacent anchor, or null if no such anchor is found
   */
  private Anchor getAdjacentAnchor(Anchor anchor, AnchorType type, boolean next) {
    Line currentLine = anchor.getLine();
    AnchorList list = getAnchors(currentLine);

    // Special case for the same line
    int insertionIndex = list.findInsertionIndex(anchor);
    int lowerBound = next ? 0 : 1;
    int upperBound = next ? list.size() - 2 : list.size() - 1;
    if (insertionIndex >= lowerBound && insertionIndex <= upperBound) {
      Anchor anchorInList = list.get(insertionIndex);
      if (anchor == anchorInList) {
        // We found the anchor in the list, and we have a neighbor to return
        Anchor candidateAnchor;
        if (next) {
          candidateAnchor = list.get(insertionIndex + 1);
        } else {
          candidateAnchor = list.get(insertionIndex - 1);
        }
        // Enforce the type
        if (type != null && !candidateAnchor.getType().equals(type)) {
          return getAdjacentAnchor(candidateAnchor, type, next);
        } else {
          return candidateAnchor;
        }
      }
      // Otherwise, the anchor must be on another line
    }

    currentLine = next ? currentLine.getNextLine() : currentLine.getPreviousLine();
    while (currentLine != null) {
      list = getAnchorsOrNull(currentLine);
      if (list != null && list.size() > 0) {
        Anchor candidateAnchor;
        if (next) {
          candidateAnchor = list.get(0);
        } else {
          candidateAnchor = list.get(list.size() - 1);
        }
        // Enforce the type
        if (type != null && !candidateAnchor.getType().equals(type)) {
          return getAdjacentAnchor(candidateAnchor, type, next);
        } else {
          return candidateAnchor;
        }
      }
      currentLine = next ? currentLine.getNextLine() : currentLine.getPreviousLine();
    }

    return null;
  }

  // TODO: not public
  public void handleTextPredeletionForLine(Line line, int column, int deleteCountForLine,
      final JsonArray<Anchor> anchorsInDeletedRangeToRemove,
      final JsonArray<Anchor> anchorsInDeletedRangeToShift, boolean isFirstLine) {

    AnchorList anchors = getAnchorsOrNull(line);
    if (anchors == null) {
      return;
    }

    boolean entireLineDeleted = line.getText().length() == deleteCountForLine;
    assert !entireLineDeleted || column == 0;
    if (entireLineDeleted && !isFirstLine) {
      // If entire line is deleted, shift/remove line anchors too
      for (int i = 0, n = anchors.size(); i < n && anchors.get(i).isLineAnchor(); i++) {
        categorizeAccordingToRemovalStrategy(anchors.get(i), anchorsInDeletedRangeToRemove,
            anchorsInDeletedRangeToShift);
      }
    }

    /*
     * To support the cursor at the end of a line (without a newline), we must
     * extend past the last column of the line
     */
    int lastColumnToDelete =
        entireLineDeleted ? Integer.MAX_VALUE : column + deleteCountForLine - 1;
    for (int i = anchors.findInsertionIndex(column, Anchor.ID_FIRST_IN_COLUMN), n = anchors.size();
        i < n && anchors.get(i).getColumn() <= lastColumnToDelete; i++) {
      categorizeAccordingToRemovalStrategy(anchors.get(i), anchorsInDeletedRangeToRemove,
          anchorsInDeletedRangeToShift);
    }
  }

  // TODO: not public
  public void handleTextDeletionLastLineLeftover(JsonArray<Anchor> anchorsLeftoverFromLastLine,
      Line firstLine, Line lastLine, int lastLineFirstUntouchedColumn) {

    AnchorList anchors = getAnchorsOrNull(lastLine);
    if (anchors == null) {
      return;
    }

    if (firstLine != lastLine) {
      for (int i = 0, n = anchors.size(); i < n && anchors.get(i).isLineAnchor(); i++) {
        anchorsLeftoverFromLastLine.add(anchors.get(i));
      }
    }

    for (int i =
        anchors.findInsertionIndex(lastLineFirstUntouchedColumn, Anchor.ID_FIRST_IN_COLUMN), n =
        anchors.size(); i < n; i++) {
      anchorsLeftoverFromLastLine.add(anchors.get(i));
    }
  }

  // TODO: not public
  /**
   * @param lastLineFirstUntouchedColumn the left-most column that was not part
   *        of the deletion. If all of the characters on the last line were
   *        deleted, this will be the length of the text on the line
   */
  public void handleTextDeletionFinished(JsonArray<Anchor> anchorsInDeletedRangeToRemove,
      JsonArray<Anchor> anchorsInDeletionRangeToShift, JsonArray<Anchor> anchorsLeftoverOnLastLine,
      Line firstLine, int firstLineNumber, int firstLineColumn, int numberOfLinesDeleted,
      int lastLineFirstUntouchedColumn) {

    AnchorDeferredDispatcher dispatcher = new AnchorDeferredDispatcher();
    
    // Remove anchors that did not want to be shifted
    for (int i = 0, n = anchorsInDeletedRangeToRemove.size(); i < n; i++) {
      removeAnchorDeferDispatch(anchorsInDeletedRangeToRemove.get(i), dispatcher);
    }

    // Shift anchors that were part of the deletion range and want to be shifted
    for (int i = 0, n = anchorsInDeletionRangeToShift.size(); i < n; i++) {
      Anchor anchor = anchorsInDeletionRangeToShift.get(i);
      updateAnchorPositionObeyingExistingIgnoresWithoutDispatch(anchor,
          firstLine, firstLineNumber, firstLineColumn);
      dispatcher.deferDispatchShifted(anchor);
    }

    /*
     * Shift anchors that were on the leftover text on the last line (now their
     * text lives on the first line)
     */
    for (int i = 0, n = anchorsLeftoverOnLastLine.size(); i < n; i++) {
      Anchor anchor = anchorsLeftoverOnLastLine.get(i);
      int anchorFirstLineColumn =
          anchor.getColumn() - lastLineFirstUntouchedColumn + firstLineColumn;
      updateAnchorPositionObeyingExistingIgnoresWithoutDispatch(anchor, firstLine, firstLineNumber,
          anchorFirstLineColumn);
      dispatcher.deferDispatchShifted(anchor);
    }

    if (numberOfLinesDeleted > 0) {
      /*
       * Shift the line numbers of anchors past the deleted range (note that the
       * anchors still have their old line numbers, hence the
       * "+ numberOfLinesDeleted")
       */
      shiftLineNumbersDeferDispatch(firstLineNumber + numberOfLinesDeleted + 1,
          -numberOfLinesDeleted, dispatcher);
    }

    dispatcher.dispatch();
  }

  private void categorizeAccordingToRemovalStrategy(Anchor anchor,
      JsonArray<Anchor> anchorsToRemove, JsonArray<Anchor> anchorsToShift) {

    switch (anchor.getRemovalStrategy()) {
      case SHIFT:
        anchorsToShift.add(anchor);
        break;

      case REMOVE:
        anchorsToRemove.add(anchor);
        break;
    }
  }

  // TODO: not public
  public void handleMultilineTextInsertion(Line oldLine, int oldLineNumber, int oldColumn,
      Line newLine, int newLineNumber, int newColumn) {

    AnchorDeferredDispatcher dispatcher = new AnchorDeferredDispatcher();

    /*
     * Shift all of the following line anchors (remember that the anchor data
     * structures do not know about the newly inserted lines yet, so
     * oldLineNumber + 1 is the first line after the insertion point.
     */
    shiftLineNumbersDeferDispatch(oldLineNumber + 1, newLineNumber - oldLineNumber,
        dispatcher);

    /*
     * Now update the anchors on the line receiving the multiline text
     * insertion
     */
    AnchorList anchors = getAnchorsOrNull(oldLine);
    if (anchors != null) {
      // Shift *line* anchors (those without columns) if their strategies allow
      for (int i = 0; i < anchors.size() && anchors.get(i).isLineAnchor();) {
        Anchor anchor = anchors.get(i);

        if (isInsertionPlacementStrategyLater(anchor, oldLine, oldColumn)) {
          updateAnchorPositionWithoutDispatch(anchor, newLine, newLineNumber,
              AnchorManager.IGNORE_COLUMN);
          dispatcher.deferDispatchShifted(anchor);
        } else {
          i++;
        }
      }

      /*
       * Consider moving the anchors that are positioned greater than or equal
       * to the column receiving the newline
       */
      for (int i = anchors.findInsertionIndex(oldColumn, Anchor.ID_FIRST_IN_COLUMN); i < anchors
          .size();) {
        Anchor anchor = anchors.get(i);

        if (anchor.getColumn() == oldColumn
            && !isInsertionPlacementStrategyLater(anchor, oldLine, oldColumn)) {
          /*
           * This anchor is on the same column as the split but does not want to
           * be moved
           */
          i++;
          continue;
        }

        int newAnchorColumn = anchor.getColumn() - oldColumn + newColumn;
        updateAnchorPositionObeyingExistingIgnoresWithoutDispatch(anchor, newLine, newLineNumber,
            newAnchorColumn);
        dispatcher.deferDispatchShifted(anchor);
        // No need to touch i since the size of anchors is one smaller now
      }
    }

    dispatcher.dispatch();
  }

  private boolean isInsertionPlacementStrategyLater(Anchor anchor, Line line, int column) {
    InsertionPlacementStrategy.Placement placement =
        anchor.getInsertionPlacementStrategy().determineForInsertion(anchor, line, column);
    return placement == Placement.LATER;
  }

  // TODO: not public
  public void handleSingleLineTextInsertion(Line line, int column, int length) {
    shiftColumnAnchors(line, column, length, true);
  }

  /**
   * Moves the anchor to a new position.
   */
  public void moveAnchor(final Anchor anchor, Line line, int lineNumber, int column) {
    updateAnchorPositionWithoutDispatch(anchor, line, lineNumber, column);
    anchor.dispatchMoved();
  }

  private void updateAnchorPositionObeyingExistingIgnoresWithoutDispatch(Anchor anchor,
      Line newLine, int newLineNumber, int newColumn) {
    int anchorsNewColumn = anchor.getColumn() == IGNORE_COLUMN ? IGNORE_COLUMN : newColumn;
    int anchorsNewLineNumber =
        anchor.getLineNumber() == IGNORE_LINE_NUMBER ? IGNORE_LINE_NUMBER : newLineNumber;
    updateAnchorPositionWithoutDispatch(anchor, newLine, anchorsNewLineNumber, anchorsNewColumn);
  }

  /**
   * Moves the anchor without dispatching. This will update the anchor lists.
   */
  private void updateAnchorPositionWithoutDispatch(final Anchor anchor, Line line, int lineNumber,
      int column) {

    Preconditions.checkState(anchor.isAttached(), "Cannot move detached anchor");
    
    // Ensure it's different
    if (anchor.getLine() == line && anchor.getLineNumber() == lineNumber
        && anchor.getColumn() == column) {
      return;
    }

    // Remove the anchor
    Line oldLine = anchor.getLine();
    AnchorList oldAnchors = getAnchorsOrNull(oldLine);
    if (oldAnchors == null) {
      throw new IllegalStateException("List of line's anchors should not be null\nLine anchors:\n"
          + dumpAnchors(lineAnchors));
    }
    
    boolean removed = oldAnchors.remove(anchor);
    if (!removed) {
      throw new IllegalStateException(
          "Could not find anchor in list of line's anchors\nAnchors on line:\n"
              + dumpAnchors(oldAnchors) + "\nLine anchors:\n" + dumpAnchors(lineAnchors));
    }

    if (anchor.hasLineNumber()) {
      removed = lineAnchors.remove(anchor);
      if (!removed) {
        throw new IllegalStateException(
            "Could not find anchor in list of anchors that care about line numbers\nLine anchors:\n"
                + dumpAnchors(lineAnchors) + "\nAnchors on line:\n" + dumpAnchors(oldAnchors));
      }
    }

    // Update its position
    anchor.setLineWithoutDispatch(line, lineNumber);
    anchor.setColumnWithoutDispatch(column);
    
    // Add it again so its in the list reflects its new position
    AnchorList anchors = line.equals(oldLine) ? oldAnchors : getAnchors(line);
    anchors.add(anchor);
    if (lineNumber != IGNORE_LINE_NUMBER) {
      lineAnchors.add(anchor);
    }
  }

  public void removeAnchor(Anchor anchor) {
    // Do not add any extra logic here
    AnchorDeferredDispatcher dispatcher = new AnchorDeferredDispatcher();
    removeAnchorDeferDispatch(anchor, dispatcher);
    dispatcher.dispatch();
  }

  /**
   * Clears the line anchors list.  This is not public API.
   */
  public void clearLineAnchors() {
    lineAnchors.clear();
  }

  private void removeAnchorDeferDispatch(Anchor anchor, AnchorDeferredDispatcher dispatcher) {
    if (anchor.hasLineNumber()) {
      lineAnchors.remove(anchor);
    }

    AnchorList anchors = getAnchorsOrNull(anchor.getLine());
    if (anchors != null) {
      anchors.remove(anchor);
    }

    anchor.detach();

    dispatcher.deferDispatchRemoved(anchor);
  }

  /**
   * Shifts the column anchors anchored to the column or later by the given
   * {@code shiftAmount}.
   *
   * @param consultPlacementStrategy true to consult and obey the placement
   *        strategies of anchors that lie exactly on {@code column}
   */
  private void shiftColumnAnchors(Line line, int column, int shiftAmount,
      boolean consultPlacementStrategy) {
    final AnchorList anchors = getAnchorsOrNull(line);
    if (anchors == null) {
      return;
    }

    AnchorDeferredDispatcher dispatcher = new AnchorDeferredDispatcher();

    int insertionIndex = anchors.findInsertionIndex(column, Anchor.ID_FIRST_IN_COLUMN);
    for (int i = insertionIndex; i < anchors.size();) {
      Anchor anchor = anchors.get(i);

      boolean repositionAnchor = true;
      if (consultPlacementStrategy && anchor.getColumn() == column) {
        repositionAnchor = isInsertionPlacementStrategyLater(anchor, line, column);
      }

      if (repositionAnchor) {
        anchors.remove(anchor);
        anchor.setColumnWithoutDispatch(anchor.getColumn() + shiftAmount);
        dispatcher.deferDispatchShifted(anchor);
        // No need to increase i since we removed above
      } else {
        i++;
      }
    }

    // Re-adding in the loop above can cause problems if shiftAmount > 0
    JsonArray<Anchor> shiftedAnchors = dispatcher.getShiftedAnchors();
    if (shiftedAnchors != null) {
      for (int i = 0, n = shiftedAnchors.size(); i < n; i++) {
        anchors.add(shiftedAnchors.get(i));
      }
    }

    // Dispatch after all anchors are in their final, consistent state
    dispatcher.dispatch();
  }

  /**
   * Takes care of shifting the line numbers of interested anchors.
   *
   * @param lineNumber inclusive
   */
  private void shiftLineNumbersDeferDispatch(final int lineNumber, int shiftAmount,
      AnchorDeferredDispatcher dispatcher) {
    int insertionIndex = lineAnchors.findInsertionIndex(lineNumber);
    for (int i = insertionIndex, n = lineAnchors.size(); i < n; i++) {
      Anchor anchor = lineAnchors.get(i);
      anchor.setLineWithoutDispatch(anchor.getLine(), anchor.getLineNumber() + shiftAmount);

      dispatcher.deferDispatchShifted(anchor);
    }
  }

  private static String dumpAnchors(SortedList<Anchor> anchorList) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, n = anchorList.size(); i < n; i++) {
      sb.append(anchorList.get(i).toString()).append("\n");
    }

    return sb.toString();
  }
}
