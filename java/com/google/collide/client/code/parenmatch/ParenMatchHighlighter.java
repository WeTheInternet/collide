
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

package com.google.collide.client.code.parenmatch;

import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.renderer.LineRenderer;
import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.client.editor.renderer.SingleChunkLineRenderer;
import com.google.collide.client.editor.search.SearchTask;
import com.google.collide.client.editor.search.SearchTask.SearchDirection;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.editor.selection.SelectionModel.CursorListener;
import com.google.collide.client.util.BasicIncrementalScheduler;
import com.google.collide.client.util.IncrementalScheduler;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.RegExpUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.client.Timer;

/*
 * TODO : Make this language specific and utilize code
 * understanding.
 */
/**
 * Highlights matching character for (), {}, [], and <> when the cursor is next
 * to one of them.
 */
public class ParenMatchHighlighter {

  /** Opening paren characters. */
  public static final String OPEN_PARENS = "(<[{";
  /**
   * Closing paren characters. {@link #CLOSE_PARENS}[i] must be the closing
   * match for {@link #OPEN_PARENS}[i].
   */
  public static final String CLOSE_PARENS = ")>]}";

  static final AnchorType MATCH_ANCHOR_TYPE =
      AnchorType.create(ParenMatchHighlighter.class, "matchAnchor");

  /**
   * Paren match highlighting CSS.
   */
  public interface Css extends Editor.EditorSharedCss {
    String match();
  }

  /**
   * Paren match highlighting resources.
   */
  public interface Resources extends ClientBundle {
    @Source({"ParenMatchHighlighter.css", "com/google/collide/client/common/constants.css"})
    Css parenMatchHighlighterCss();
  }

  /**
   * Handler for processing each line during the search.
   */
  private class SearchTaskHandler implements SearchTask.SearchTaskExecutor {

    private Line startLine;
    private int cursorColumn;
    private SearchDirection direction;
    private char searchChar;
    private char cancelChar;
    private int matchCount;
    private RegExp regExp;

    /**
     * Initialize the search parameters.
     *
     * @param startLine the line the search will start at.
     * @param cursorColumn the column where the paren character was found.
     * @param direction the direction to search, depending on whether we're
     *        looking for the closing or opening paren.
     * @param searchChar the char we are looking for that opens or closes the
     *        found paren
     * @param cancelChar the char we found. If found again, we must find it's
     *        match before we find the original paren's match.
     */
    public void initialize(Line startLine, int cursorColumn, SearchDirection direction,
        char searchChar, char cancelChar) {
      this.startLine = startLine;
      this.cursorColumn = cursorColumn;
      this.direction = direction;
      this.searchChar = searchChar;
      this.cancelChar = cancelChar;
      if (searchChar == '[' || searchChar == ']') {
        this.regExp = RegExp.compile("\\[|\\]", "g");
      } else {
        // searching ( or ) -> [(]|[)]
        this.regExp = RegExp.compile("[" + this.searchChar + "]|[" + this.cancelChar + "]", "g");
      }
      // we start at 1 and try to get down to 0 by finding the actual match.
      this.matchCount = 1;
    }

    @Override
    public boolean onSearchLine(Line line, int number, boolean shouldRenderLine) {
      String lineText= line.getText();
      /*
       * Set match to -1 since we call getNextMatch with match + 1 to exclude a
       * found match from the next round.
       */
      int match = -1;
      if (direction == SearchDirection.DOWN) {
        if (line == startLine) {
          // the - 1 is to make sure we include the character at the cursor.
          match = cursorColumn - 1;
        }
        MatchResult result;
        while ((result = RegExpUtils.findMatchAfterIndex(regExp, lineText, match)) != null) {
          match = result.getIndex();
          if (checkForMatch(line, number, match, shouldRenderLine)) {
            return false;
          }
        }
      } else {
        int endColumn = line.length() - 1;
        if (line == startLine) {
          endColumn = cursorColumn - 2;
        }
        // first get all matches
        JsonArray<Integer> matches = JsonCollections.createArray();
        MatchResult result;
        while ((result = RegExpUtils.findMatchAfterIndex(regExp, lineText, match)) != null) {
          match = result.getIndex();
          if (match <= endColumn) {
            matches.add(match);
          } else {
            break;
          }
        }
        // then iterate backwards through them
        /**
         * TODO : look for a faster way to do this such that we
         * don't have to iterate back through them
         */
        for (int i = matches.size() - 1; i >= 0; i--) {
          if (checkForMatch(line, number, matches.get(i), shouldRenderLine)) {
            return false;
          }
        }
      }
      return true;
    }

    /**
     * Check if this character is the match we are looking for.
     */
    private boolean checkForMatch(Line line, int number, int column, boolean shouldRenderLine) {
      char nextChar = line.getText().charAt(column);
      if (nextChar == searchChar) {
        matchCount--;
      } else if (nextChar == cancelChar) {
        matchCount++;
      }

      if (matchCount == 0) {
        matchAnchor = anchorManager.createAnchor(MATCH_ANCHOR_TYPE, line, number, column);
        // when testing, css is null
        matchRenderer = SingleChunkLineRenderer.create(matchAnchor, matchAnchor, css.match());
        renderer.addLineRenderer(matchRenderer);
        if (shouldRenderLine) {
          renderer.requestRenderLine(line);
        }
        return true;
      }
      return false;
    }
  }

  /**
   * A helper class to handle client events and listeners. This allows all client and GWT
   * functionality to be mocked out in the tests by hiding the implementation details of the
   * {@link Timer}.
   */
  static class ParenMatchHelper implements ListenerRegistrar<CursorListener> {

    CursorListener cursorListener;
    Remover remover;
    final SelectionModel selectionModel;
    
    final Timer timer = new Timer() {
      @Override
      public void run() {
        if (cursorListener == null) {
          return;
        }
        LineInfo cursorLine =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        int cursorColumn = selectionModel.getCursorColumn();
        cursorListener.onCursorChange(cursorLine, cursorColumn, true);
      }
    };

    public ParenMatchHelper(SelectionModel model) {
      this.selectionModel = model;
    }
    
    void register() {
      remover = selectionModel.getCursorListenerRegistrar().add(new CursorListener() {
        @Override
        public void onCursorChange(LineInfo lineInfo, int column, boolean isExplicitChange) {
          timer.schedule(50);
        }
      });
    }

    void cancelTimer() {
      timer.cancel();
    }

    @Override
    public ListenerRegistrar.Remover add(CursorListener listener) {
      Preconditions.checkArgument(this.cursorListener == null, "Can't register two listeners");
      this.cursorListener = listener;
      register();

      return new Remover() {

        @Override
        public void remove() {
          remover.remove();
          cursorListener = null;
        }

      };
    }

    @Override
    public void remove(CursorListener listener) {
      throw new UnsupportedOperationException("The remover must be used to remove the listener");
    }
  }

  public static ParenMatchHighlighter create(
      Document document,
      ViewportModel viewportModel,
      AnchorManager anchorManager,
      Resources res,
      Renderer renderer,
      final SelectionModel selection) {
    
    final IncrementalScheduler scheduler = new BasicIncrementalScheduler(100, 5000);
    
    ParenMatchHelper helper = new ParenMatchHelper(selection);

    return new ParenMatchHighlighter(
        document, viewportModel, anchorManager, res, renderer, scheduler, helper);
  }

  private final AnchorManager anchorManager;
  private final Renderer renderer;
  private final IncrementalScheduler scheduler;
  private final SearchTask searchTask;
  private final SearchTaskHandler searchTaskHandler;
  private final Css css;
  private final ListenerRegistrar<CursorListener> listenerRegistrar;
  private final CursorListener cursorListener;
  
  private ListenerRegistrar.Remover cursorListenerRemover;
  private Anchor matchAnchor;
  private LineRenderer matchRenderer;

  @VisibleForTesting
  ParenMatchHighlighter(Document document, ViewportModel viewportModel,
      AnchorManager anchorManager, Resources res, Renderer renderer,
      IncrementalScheduler scheduler, ListenerRegistrar<CursorListener> listenerRegistrar) {
    this.anchorManager = anchorManager;
    this.renderer = renderer;
    this.scheduler = scheduler;
    this.listenerRegistrar = listenerRegistrar;
    searchTask = new SearchTask(document, viewportModel, scheduler);
    searchTaskHandler = new SearchTaskHandler();
    css = res.parenMatchHighlighterCss();

    cursorListener = new CursorListener() {
      @Override
      public void onCursorChange(LineInfo lineInfo, int column, boolean isExplicitChange) {
        cancel();
        maybeSearch(lineInfo, column);
      }
    };

    cursorListenerRemover = this.listenerRegistrar.add(cursorListener);
  }

  /**
   * Enable or disable the match highlighter. By default it's enabled.
   */
  public void setEnabled(boolean enabled) {
    Preconditions.checkNotNull(
        cursorListenerRemover, "can't enable when cursorListenerRemover is null");
    if (enabled) {
      cursorListenerRemover = listenerRegistrar.add(cursorListener);
    } else {
      cancel();
      cursorListenerRemover.remove();
    }
  }

  /**
   * Cancel the current matching - both the search and any displayed matches.
   */
  public void cancel() {
    scheduler.cancel();
    searchTask.cancelTask();
    if (matchRenderer != null) {
      renderer.removeLineRenderer(matchRenderer);
      renderer.requestRenderLine(matchAnchor.getLine());
      anchorManager.removeAnchor(matchAnchor);
      matchRenderer = null;
    }
  }

  public void teardown() {
    cancel();
    cursorListenerRemover.remove();
  }

  /**
   * Checks if there is a paren to match at the cursor and starts a search if
   * so.
   * 
   * @param cursorLine
   * @param cursorColumn
   */
  private void maybeSearch(LineInfo cursorLine, int cursorColumn) {
    if (cursorColumn > 0) {
      char cancelChar = cursorLine.line().getText().charAt(cursorColumn - 1);

      int openIndex = OPEN_PARENS.indexOf(cancelChar);
      if (openIndex >= 0) {
        search(SearchDirection.DOWN, CLOSE_PARENS.charAt(openIndex), cancelChar, cursorLine,
            cursorColumn);
        return;
      }
      int closeIndex = CLOSE_PARENS.indexOf(cancelChar);
      if (closeIndex >= 0) {
        search(SearchDirection.UP, OPEN_PARENS.charAt(closeIndex), cancelChar, cursorLine,
            cursorColumn);
        return;
      }
    }
  }

  /**
   * Starts the search for the matching paren.
   * 
   * @param direction the direction to search in
   * @param searchChar the character we want to match
   * @param cancelChar the character we found, which if we find again we need to
   *        find its match first.
   * @param cursorLine the line where the to-be-matched character was found
   * @param column the column to the right of the to-be-matched character
   */
  @VisibleForTesting
  protected void search(final SearchDirection direction, final char searchChar,
      final char cancelChar, final LineInfo cursorLine, final int column) {
    searchTaskHandler.initialize(cursorLine.line(), column, direction, searchChar, cancelChar);
    searchTask.searchDocumentStartingAtLine(searchTaskHandler, null, direction, cursorLine);
  }
}
