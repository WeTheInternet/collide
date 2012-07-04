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

package com.google.collide.client.editor.search;

import com.google.collide.client.editor.search.SearchModel.MatchCountListener;
import com.google.collide.client.editor.search.SearchTask.SearchTaskExecutor;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.RegExpUtils;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Manages search matches and can be queried to determine the current match and
 * select a new match.
 */
/*
 * TODO: Consider making searching for matches asynchrounous if it
 * proves to be a bottleneck. Particularly revisit code that touches
 * totalMatches since it will no longer be valid and could lead to races.
 */
public class SearchMatchManager {
  
  private final Document document;
  private RegExp searchPattern;

  int totalMatches;
  private final SelectionModel selection;
  private final DocumentMutator editorDocumentMutator;
  private final SearchTask searchTask;
  private final ListenerManager<MatchCountListener> totalMatchesListenerManager =
      ListenerManager.create();

  public SearchMatchManager(Document document, SelectionModel selection,
      DocumentMutator editorDocumentMutator, SearchTask searchTask) {
    this.document = document;
    this.selection = selection;
    this.editorDocumentMutator = editorDocumentMutator;
    this.searchTask = searchTask;
  }

  /**
   * Moves to the next match starting from the current cursor position.
   *
   * @returns Position of match or null if no matches are found.
   */
  public Position selectNextMatch() {
    Position[] position = selection.getSelectionRange(false);
    return selectNextMatchFromPosition(position[1].getLineInfo(), position[1].getColumn());
  }

  /**
   * Moves to the next match after the given position (inclusive).
   *
   * @returns Position of match or null if no matches are found.
   */
  public Position selectNextMatchFromPosition(LineInfo lineInfo, int startColumn) {
    if (totalMatches == 0 || searchPattern == null || lineInfo == null) {
      return null;
    }

    /*
     * Basic Strategy: loop through lines until we find another match, if we hit
     * the end start at the top. Until we hit our own line then just select the
     * first match from index 0 (shouldn't be us).
     */
    Line beginLine = lineInfo.line();
    int column = startColumn;
    do {
      if (selectNextMatchOnLine(lineInfo, column, lineInfo.line().length())) {
        return new Position(lineInfo, selection.getCursorColumn());
      }
      if (!lineInfo.moveToNext()) {
        lineInfo = document.getFirstLineInfo();
      }
      // after first attempt, we always look at start of line
      column = 0;
    } while (lineInfo.line() != beginLine);

    // We check to ensure there wasn't another match to wrap to on our own line
    if (selectNextMatchOnLine(lineInfo, 0, startColumn)) {
      return new Position(lineInfo, selection.getCursorColumn());
    }
    return null;
  }

  /**
   * Moves to the previous match starting at the current cursor position.
   *
   * @returns Position of match or null if no matches are found.
   */
  public Position selectPreviousMatch() {
    Position[] position = selection.getSelectionRange(false);
    return selectPreviousMatchFromPosition(position[0].getLineInfo(), position[0].getColumn());
  }

  /**
   * Moves to the previous match from the given position (inclusive).
   *
   * @returns Position of match or null if no matches are found.
   */
  public Position selectPreviousMatchFromPosition(LineInfo lineInfo, int startColumn) {
    if (totalMatches == 0 || searchPattern == null || lineInfo == null) {
      return null;
    }

    /*
     * Basic Strategy: loop through lines going up, we have to go right to left
     * though so we use the line keys to determine how many matches should be in
     * a line and back out from that.
     */
    Line beginLine = lineInfo.line();
    int column = startColumn;
    do {
      if (selectPreviousMatchOnLine(lineInfo, 0, column)) {
        return new Position(lineInfo, selection.getCursorColumn());
      }

      if (!lineInfo.moveToPrevious()) {
        lineInfo = document.getLastLineInfo();
      }
      // after first attempt we want the last match in a line always
      column = lineInfo.line().getText().length();
    } while (lineInfo.line() != beginLine);

    // We check to ensure there wasn't another match to wrap to on our own line
    if (selectPreviousMatchOnLine(lineInfo, startColumn, beginLine.length())) {
      return new Position(lineInfo, selection.getCursorColumn());
    }
    return null;
  }

  /**
   * Increments the current total. If no match is currently selected this will
   * select the first match that is added automatically.
   */
  public void addMatches(LineInfo lineInfo, int matches) {
    assert searchPattern != null;

    if (totalMatches == 0 && matches > 0) {
      selectNextMatchOnLine(lineInfo, 0, lineInfo.line().length());
    }

    totalMatches += matches;
    dispatchTotalMatchesChanged();
  }

  public int getTotalMatches() {
    return totalMatches;
  }
  
  ListenerRegistrar<MatchCountListener> getMatchCountChangedListenerRegistrar() {
    return totalMatchesListenerManager;
  }

  public void clearMatches() {
    totalMatches = 0;
    dispatchTotalMatchesChanged();
  }

  /**
   * @return true if current selection is a match to the searchPattern.
   */
  private boolean isSelectionRangeAMatch() {
    Position[] selectionRange = selection.getSelectionRange(false);
    if (searchPattern != null && totalMatches > 0
        && selectionRange[0].getLine() == selectionRange[1].getLine()) {
      String text =
          document.getText(selectionRange[0].getLine(), selectionRange[0].getColumn(),
              selectionRange[1].getColumn() - selectionRange[0].getColumn());

      return !text.isEmpty() && RegExpUtils.resetAndTest(searchPattern, text);
    }
    return false;
  }

  /**
   * Sets the search pattern used when finding matches. Also clears any existing
   * match count.
   */
  public void setSearchPattern(RegExp searchPattern) {
    clearMatches();
    this.searchPattern = searchPattern;
  }
  
  private void dispatchTotalMatchesChanged() {
    totalMatchesListenerManager.dispatch(new Dispatcher<MatchCountListener>() {
      @Override
      public void dispatch(MatchCountListener listener) {
        listener.onMatchCountChanged(totalMatches);
      }
    });
  }

  /**
   * Selects the next match using the search pattern given line and startIndex.
   *
   * @param startIndex The boundary to find the next match after.
   * @param endIndex The boundary to find the next match before.
   *
   * @returns true if match is found
   */
  private boolean selectNextMatchOnLine(LineInfo line, int startIndex, int endIndex) {
    searchPattern.setLastIndex(startIndex);
    MatchResult result = searchPattern.exec(line.line().getText());

    if (result == null || result.getIndex() >= endIndex) {
      return false;
    }

    moveAndSelectMatch(line, result.getIndex(), result.getGroup(0).length());
    return true;
  }

  /**
   * Selects the previous match using the search pattern given line and
   * startIndex.
   *
   * @param startIndex The boundary to find a previous match after.
   * @param endIndex The boundary to find a previous match before.
   *
   * @returns true if a match is found
   */
  private boolean selectPreviousMatchOnLine(LineInfo line, int startIndex, int endIndex) {
    searchPattern.setLastIndex(0);

    // Find the last match without going over our startIndex
    MatchResult lastMatch = null;
    for (MatchResult result = searchPattern.exec(line.line().getText());
        result != null && result.getIndex() < endIndex && result.getIndex() >= startIndex;
        result = searchPattern.exec(line.line().getText())) {
      lastMatch = result;
    }

    if (lastMatch == null) {
      return false;
    }

    moveAndSelectMatch(line, lastMatch.getIndex(), lastMatch.getGroup(0).length());
    return true;
  }

  /**
   * Moves the editor selection to the specified line and column and selects
   * length characters.
   */
  private void moveAndSelectMatch(LineInfo line, int column, int length) {
    selection.setSelection(line, column + length, line, column);
  }

  public void replaceAllMatches(final String replacement) {
    // TODO: There's an issue relying on the same SearchTask as
    // SearchModel, since they share the same scheduler the searchModel can
    // preempt a replaceAll before it is finish!
    searchTask.searchDocument(new SearchTaskExecutor() {
      @Override
      public boolean onSearchLine(Line line, int number, boolean shouldRenderLine) {
        searchPattern.setLastIndex(0);
        for (MatchResult result = searchPattern.exec(line.getText());
            result != null && result.getGroup(0).length() != 0;
            result = searchPattern.exec(line.getText())) {

          int start = searchPattern.getLastIndex() - result.getGroup(0).length();
          editorDocumentMutator.deleteText(line, number, start, result.getGroup(0).length());
          editorDocumentMutator.insertText(line, number, start, replacement);

          int newIndex = result.getIndex() + replacement.length();
          searchPattern.setLastIndex(newIndex);
        }
        return true;
      }
    }, null);
  }

  public boolean replaceMatch(String replacement) {
    if (!isSelectionRangeAMatch() && selectNextMatch() == null) {
      return false;
    }

    editorDocumentMutator.insertText(selection.getCursorLine(), selection.getCursorLineNumber(),
        selection.getCursorColumn(), replacement, true);
    selectNextMatch();
    return true;
  }
}
