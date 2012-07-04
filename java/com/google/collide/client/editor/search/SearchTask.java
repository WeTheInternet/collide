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

import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.search.SearchModel.SearchProgressListener;
import com.google.collide.client.util.IncrementalScheduler;
import com.google.collide.client.util.IncrementalScheduler.Task;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.common.base.Preconditions;

/**
 * A class which can be used to iterate through the lines of a document. It will
 * synchronously callback for each line in the viewport then asynchronously
 * callback for the remaining lines in the document. The direction and start
 * line within the viewport are configurable.
 */
public class SearchTask {

  public interface SearchTaskExecutor {
    /**
     * Called for each line in the document as it is searched.
     *
     * @param line the current line
     * @param number the current line number
     * @param shouldRenderLine if this line is in the viewport and a render must
     *        be performed if any visible changes are made.
     * @return false to stop the search.
     */
    public boolean onSearchLine(Line line, int number, boolean shouldRenderLine);
  }

  /**
   * The direction of the document search.
   */
  public enum SearchDirection {
    UP, DOWN
  }

  /**
   * An object which simplifies searches by hiding the logic which depends on
   * the search direction.
   */
  private static class SearchDirectionHelper {
    private final ViewportModel viewport;
    private final Document document;

    private SearchDirection direction;

    public SearchDirectionHelper(ViewportModel viewport, Document document) {
      this.viewport = viewport;
      this.document = document;
    }

    /**
     * Sets the direction of the search so the helper can return valid line
     * information.
     */
    public void setDirection(SearchDirection direction) {
      this.direction = direction;
    }

    /**
     * Gets the starting line of the viewport, bottom if
     * {@link SearchDirection#DOWN}, top if {@link SearchDirection#UP}.
     */
    public Line getViewportEndLine() {
      return isGoingDown() ? viewport.getBottomLine() : viewport.getTopLine();
    }

    /**
     * Gets the starting line of the viewport, bottom if
     * {@link SearchDirection#DOWN}, top if {@link SearchDirection#UP}.
     */
    public LineInfo getViewportEndLineInfo() {
      return isGoingDown() ? viewport.getBottomLineInfo() : viewport.getTopLineInfo();
    }

    /**
     * Gets the starting line of the viewport, top if
     * {@link SearchDirection#DOWN}, bottom if {@link SearchDirection#UP}.
     */
    public LineInfo getViewportStartLineInfo() {
      return isGoingDown() ? viewport.getTopLineInfo() : viewport.getBottomLineInfo();
    }

    /**
     * Returns the line necessary to wrap around the document. i.e. if you are
     * searching down it will return the top, if you are searching up it will
     * return the bottom.
     */
    public LineInfo getWrapDocumentLine() {
      return isGoingDown() ? document.getFirstLineInfo() : document.getLastLineInfo();
    }

    /**
     * Returns if the search is going down.
     */
    public boolean isGoingDown() {
      return direction == SearchDirection.DOWN;
    }

    /**
     * Returns true if the line to be wrapped to is not at the corresponding
     * edge of the document. i.e. Don't wrap to the top of the document if the
     * viewport is at the top already (which we've already scanned).
     */
    public boolean canWrapDocument() {
      boolean atEdge = isGoingDown() ? viewport.getTopLine() == document.getFirstLine() :
          viewport.getBottomLine() == document.getLastLine();
      return !atEdge;
    }
  }

  /**
   * Indicates that the search task should start a search starting at either the
   * top or bottom of the viewport depending on the selected direction.
   */
  public static final LineInfo DEFAULT_START_LINE = new LineInfo(null, -1);

  private static final AnchorType SEARCH_TASK_ANCHOR =
      AnchorType.create(SearchTask.class, "SearchAnchor");

  private final ViewportModel viewport;
  private final IncrementalScheduler scheduler;
  private final Document document;
  private final Task asyncSearchTask;
  private final SearchDirectionHelper searchDirectionHelper;

  private Anchor stopLineAnchor;
  private Anchor searchTaskAnchor;
  private SearchProgressListener progressListener;
  private SearchTaskExecutor executor;
  private boolean shouldWrapDocument = true;

  public SearchTask(Document document, ViewportModel viewport, IncrementalScheduler scheduler) {
    this.document = document;
    this.viewport = viewport;
    this.scheduler = scheduler;
    this.searchDirectionHelper = new SearchDirectionHelper(viewport, document);

    asyncSearchTask = new AsyncSearchTask();
  }

  public void teardown() {
    scheduler.teardown();
    removeSearchTaskAnchors();
  }

  /**
   * Starts searching the document in the down direction starting at the
   * default line.
   */
  public void searchDocument(
      SearchTaskExecutor executor, SearchProgressListener progressListener) {
    searchDocumentStartingAtLine(
        executor, progressListener, SearchDirection.DOWN, DEFAULT_START_LINE);
  }

  /**
   * Starts searching the document in the down direction starting at the given
   * line.
   */
  public void searchDocument(
      SearchTaskExecutor executor, SearchProgressListener progressListener, LineInfo startLine) {
    searchDocumentStartingAtLine(
        executor, progressListener, SearchDirection.DOWN, startLine);
  }

  /**
   * Starts searching the document in the given direction starting at the
   * default start line.
   */
  public void searchDocument(SearchTaskExecutor executor, SearchProgressListener progressListener,
      SearchDirection direction) {
    searchDocumentStartingAtLine(executor, progressListener, direction, DEFAULT_START_LINE);
  }

  /**
   * Starts searching the document at the given line in the viewport and in the
   * specified direction. If the startLine is not within the viewport then
   * behavior is undefined and terrible things will likely happen.
   */
  public void searchDocumentStartingAtLine(SearchTaskExecutor executor,
      SearchProgressListener progressListener, SearchDirection direction, LineInfo startLine) {
    scheduler.cancel();
    this.progressListener = progressListener;
    this.executor = executor;

    searchDirectionHelper.setDirection(direction);
    if (startLine == DEFAULT_START_LINE) {
      startLine = searchDirectionHelper.getViewportStartLineInfo();
    }

    dispatchSearchBegin();
    boolean doAsyncSearch = true;
    if (startLine.number() >= viewport.getTopLineNumber() &&
        startLine.number() <= viewport.getBottomLineNumber()) {
      doAsyncSearch = scanViewportStartingAtLine(startLine);
    }

    if (doAsyncSearch) {
      setupSearchTaskAnchors(startLine);
      scheduler.schedule(asyncSearchTask);
    } else {
      dispatchSearchDone();
    }
  }

  /**
   * Returns if the search will wrap around the document when it gets to the
   * bottom or top.
   */
  public boolean isShouldWrapDocument() {
    return shouldWrapDocument;
  }

  /**
   * Determines if the search should wrap around the document either from the
   * top to the bottom or vice versa.
   */
  public void setShouldWrapDocument(boolean shouldWrapDocument) {
    this.shouldWrapDocument = shouldWrapDocument;
  }

  /**
   * Cancels any currently running search task.
   */
  public void cancelTask() {
    scheduler.cancel();
  }

  /**
   * Starts a scan of the viewport at the given line. If the given lineInfo is
   * not a line within the viewport then behavior is undefined (and likely not
   * going to end well).
   */
  private boolean scanViewportStartingAtLine(LineInfo startLineInfo) {
    Preconditions.checkArgument(
        startLineInfo.number() >= viewport.getTopLineNumber() &&
        startLineInfo.number() <= viewport.getBottomLineNumber(),
        "Editor: Search start line number not within viewport.");

    LineInfo lineInfo = startLineInfo.copy();
    do {
      if (!executor.onSearchLine(lineInfo.line(), lineInfo.number(), true)) {
        return false;
      }
    } while (lineInfo.line() != searchDirectionHelper.getViewportEndLine()
        && lineInfo.moveTo(searchDirectionHelper.isGoingDown()));

    /*
     * If we stopped because lineInfo == endline then we need to continue async
     * scanning, otherwise this moveTo call will fail and we won't bother. We
     * also have to check for the case where the viewport was already scrolled
     * to the very bottom or top of the document.
     */
    return lineInfo.moveTo(searchDirectionHelper.isGoingDown())
        || (searchDirectionHelper.canWrapDocument() && shouldWrapDocument);
  }

  /**
   * Removes the search task anchors after the task has completed.
   */
  private void removeSearchTaskAnchors() {
    if (stopLineAnchor != null) {
      document.getAnchorManager().removeAnchor(stopLineAnchor);
      stopLineAnchor = null;
    }
    if (searchTaskAnchor != null) {
      document.getAnchorManager().removeAnchor(searchTaskAnchor);
      searchTaskAnchor = null;
    }
  }

  /**
   * Sets an anchor at the top of the current viewport and one line below the
   * end of the viewport so we can scan the rest of the document.
   */
  private void setupSearchTaskAnchors(LineInfo stopLine) {
    if (stopLineAnchor == null) {
      stopLineAnchor = document.getAnchorManager().createAnchor(
          SEARCH_TASK_ANCHOR, stopLine.line(), stopLine.number(), AnchorManager.IGNORE_COLUMN);
      stopLineAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);
    } else {
      document.getAnchorManager().moveAnchor(
          stopLineAnchor, stopLine.line(), stopLine.number(), AnchorManager.IGNORE_COLUMN);
    }

    // Try to set the line at the top or bottom of viewport (depending on
    // direction), if we fail then wrap around the document. We don't have to
    // check shoudl wrap here since the viewport scan would have returned false.
    LineInfo startAnchorLine = searchDirectionHelper.getViewportEndLineInfo();
    if (!startAnchorLine.moveTo(searchDirectionHelper.isGoingDown())) {
      startAnchorLine = searchDirectionHelper.getWrapDocumentLine();
    }

    if (searchTaskAnchor == null) {
      searchTaskAnchor =
          document.getAnchorManager().createAnchor(SEARCH_TASK_ANCHOR, startAnchorLine.line(),
              startAnchorLine.number(), AnchorManager.IGNORE_COLUMN);
      searchTaskAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);
    } else {
      document.getAnchorManager().moveAnchor(searchTaskAnchor, startAnchorLine.line(),
          startAnchorLine.number(), AnchorManager.IGNORE_COLUMN);
    }
  }

  private void dispatchSearchBegin() {
    if (progressListener != null) {
      progressListener.onSearchBegin();
    }
  }

  private void dispatchSearchProgress() {
    if (progressListener != null) {
      progressListener.onSearchProgress();
    }
  }

  private void dispatchSearchDone() {
    removeSearchTaskAnchors();
    if (progressListener != null) {
      progressListener.onSearchDone();
    }
  }

  private class AsyncSearchTask implements IncrementalScheduler.Task {
    @Override
    public boolean run(int workAmount) {
      LineInfo lineInfo = searchTaskAnchor.getLineInfo();

      for (; lineInfo.line() != stopLineAnchor.getLine() && workAmount > 0; workAmount--) {
        if (!executor.onSearchLine(lineInfo.line(), lineInfo.number(), false)) {
          dispatchSearchDone();
          return false;
        }

        if (!lineInfo.moveTo(searchDirectionHelper.isGoingDown())) {
          if (shouldWrapDocument) {
            lineInfo = searchDirectionHelper.getWrapDocumentLine();
          } else {
            dispatchSearchDone();
            return false;
          }
        }
      }

      if (lineInfo.line() == stopLineAnchor.getLine()) {
        dispatchSearchDone();
        return false;
      }

      document.getAnchorManager().moveAnchor(
          searchTaskAnchor, lineInfo.line(), lineInfo.number(), AnchorManager.IGNORE_COLUMN);
      dispatchSearchProgress();
      return true;
    }
  }
}
