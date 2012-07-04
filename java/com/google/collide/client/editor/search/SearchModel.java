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

import com.google.collide.client.AppContext;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.BasicIncrementalScheduler;
import com.google.collide.client.util.ClientStringUtils;
import com.google.collide.client.util.IncrementalScheduler;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.RegExpUtils;
import com.google.gwt.regexp.shared.RegExp;

/**
 * External handle to search functions in the editor.
 *
 * TODO: Handle number of matches changing due to document mutations.
 *
 */
public class SearchModel {

  public static SearchModel create(AppContext context,
      Document document,
      Renderer renderer,
      ViewportModel viewport,
      SelectionModel selectionModel,
      DocumentMutator editorDocumentMutator) {
    /*
     * This is a pretty fast operation so by default we guess about 5000 lines
     * in 100 ms.
     */
    IncrementalScheduler scheduler =
        new BasicIncrementalScheduler(context.getUserActivityManager(), 100, 5000);
    SearchTask searchTask = new SearchTask(document, viewport, scheduler);

    IncrementalScheduler matchScheduler =
        new BasicIncrementalScheduler(context.getUserActivityManager(), 100, 5000);
    SearchTask matchTask = new SearchTask(document, viewport, matchScheduler);
    return new SearchModel(context,
        document,
        renderer,
        viewport,
        new SearchMatchManager(document, selectionModel, editorDocumentMutator, matchTask),
        searchTask,
        selectionModel);
  }

  public static SearchModel createWithManagerAndScheduler(AppContext context,
      Document document,
      Renderer renderer,
      ViewportModel viewport,
      SearchMatchManager matchManager,
      IncrementalScheduler scheduler,
      SelectionModel selectionModel) {
    return new SearchModel(context,
        document,
        renderer,
        viewport,
        matchManager,
        new SearchTask(document, viewport, scheduler),
        selectionModel);
  }
  
  public interface SearchProgressListener {
    public void onSearchBegin();

    public void onSearchProgress();

    public void onSearchDone();
  }
  
  public interface MatchCountListener {
    public void onMatchCountChanged(int total);
  }
  
  private class SearchTaskHandler implements SearchTask.SearchTaskExecutor {

    private RegExp oldSearchPattern;

    public void setOldSearchPattern(RegExp oldSearchPattern) {
      this.oldSearchPattern = oldSearchPattern;
    }

    @Override
    public boolean onSearchLine(Line line, int number, boolean shouldRenderLine) {
      int matches = RegExpUtils.resetAndGetNumberOfMatches(searchPattern, line.getText());
      matchManager.addMatches(new LineInfo(line, number), matches);
      if (shouldRenderLine) {
        handleViewportLine(line, matches);
      }

      return true;
    }

    private void handleViewportLine(Line line, int matches) {
      if (matches > 0) {
        renderer.requestRenderLine(line);
      } else if (oldSearchPattern != null
          && RegExpUtils.resetAndTest(oldSearchPattern, line.getText())) {
        renderer.requestRenderLine(line);
      }
    }
  }

  private final SearchMatchRenderer lineRenderer;
  private String query;
  private final Renderer renderer;
  private RegExp searchPattern;
  private final SearchMatchManager matchManager;
  private final ViewportModel viewport;
  private final SearchTask searchTask;
  private final SelectionModel selectionModel;
  private final SearchTaskHandler searchTaskHandler;

  protected SearchModel(AppContext context,
      Document document,
      Renderer renderer,
      ViewportModel viewport,
      SearchMatchManager matchManager,
      SearchTask searchTask,
      SelectionModel selectionModel) {

    this.lineRenderer = new SearchMatchRenderer(context.getResources(), this);
    this.matchManager = matchManager;
    this.query = "";
    this.renderer = renderer;
    this.viewport = viewport;
    this.searchTask = searchTask;
    this.selectionModel = selectionModel;
    
    searchTaskHandler = new SearchTaskHandler();
  }

  /**
   * @return currently active query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @return currently active search pattern
   */
  public RegExp getSearchPattern() {
    return searchPattern;
  }

  /**
   * Matches a wildcard type search query in the editor
   */
  public void setQuery(String query) {
    setQuery(query, null);
  }

  /**
   * Matches a wildcard type search query in the editor
   *
   * @param progressListener optional search progress listener.
   */
  public void setQuery(final String query, SearchProgressListener progressListener) {
    if (query == null) {
      throw new IllegalArgumentException("Query cannot be null");
    }
    
    this.query = query;
    if (query.isEmpty()) {
      if (searchPattern != null) {
        matchManager.clearMatches();
        cleanupAfterQuery();
      }
      return;
    }

    if (searchPattern == null) {
      // moving from no query to an active query; add the line renderer
      renderer.addLineRenderer(lineRenderer);
    }

    /*
     * Heuristic for case sensitivity: If the string is all lower-case we match
     * case-insensitively; otherwise the pattern is case sensitive.
     */
    String regExpOptions = ClientStringUtils.containsUppercase(query) ? "g" : "gi";

    // Create the new search pattern
    searchTaskHandler.setOldSearchPattern(searchPattern);
    searchPattern = RegExpUtils.createRegExpForWildcardPattern(query, regExpOptions);
    
    // setSearchPattern automatically clears any match data
    matchManager.setSearchPattern(searchPattern);
    Line line = selectionModel.getCursorLine();
    int lineNumber = selectionModel.getCursorLineNumber();
    searchTask.searchDocument(searchTaskHandler, progressListener, new LineInfo(line, lineNumber));
  }

  public SearchMatchManager getMatchManager() {
    return matchManager;
  }
  
  public ListenerRegistrar<MatchCountListener> getMatchCountChangedListenerRegistrar() {
    return matchManager.getMatchCountChangedListenerRegistrar();
  }

  public void teardown() {
    searchTask.teardown();
  }

  /**
   * Cleans up the viewport when we no longer have a query. Rerenders lines that
   * the last searchPattern has highlighted.
   */
  private void cleanupAfterQuery() {
    renderer.removeLineRenderer(lineRenderer);

    LineInfo lineInfo = viewport.getTopLineInfo();
    do {
      if (RegExpUtils.resetAndTest(searchPattern, lineInfo.line().getText())) {
        renderer.requestRenderLine(lineInfo.line());
      }
    } while (lineInfo.line() != viewport.getBottomLine() && lineInfo.moveToNext());

    searchPattern = null;
  }

}
