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

import com.google.collide.client.editor.renderer.LineRenderer;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * Search match highlighting.
 *
 */
public class SearchMatchRenderer implements LineRenderer {

  public interface Css extends CssResource {
    String match();
  }

  public interface Resources extends ClientBundle {
    @Source("SearchMatchRenderer.css")
    Css searchMatchRendererCss();
  }

  private final SearchModel model;
  private boolean inMatch = false;
  private final JsonArray<Integer> edges;
  private final Css css;

  public SearchMatchRenderer(Resources resource, SearchModel model) {
    this.model = model;
    edges = JsonCollections.createArray();
    css = resource.searchMatchRendererCss();
  }

  @Override
  public void renderNextChunk(Target target) {
    int end = edges.get(1);
    int start = edges.remove(0);
    
    /*
     * TODO: This is caused by back to back matches (which we want to
     * render separately since we can highlight a single match), maybe a better
     * way to fix this in the future?
     */
    if (start == end) {
      end = edges.get(1);
      start = edges.remove(0);
      inMatch = !inMatch;
    }
    
    target.render(end - start, inMatch ? css.match() : null);
    inMatch = !inMatch;
  }

  @Override
  public boolean resetToBeginningOfLine(Line line, int lineNumber) {
    assert model.getQuery() != null;
    assert model.getSearchPattern() != null;

    edges.clear();
    String text = line.getText();
    RegExp regex = model.getSearchPattern();
    /*
     * We must not forget to clear the lastIndex since it is a global regex, if
     * we don't it can lead to a false negative for matches.
     */
    regex.setLastIndex(0);
    MatchResult match = regex.exec(text);
    if (match == null || match.getGroup(0).isEmpty()) {
      return false; 
    }
    
    do {
      int start = regex.getLastIndex() - match.getGroup(0).length();
      edges.add(start);
      edges.add(regex.getLastIndex());
      match = regex.exec(text);
    } while (match != null && !match.getGroup(0).isEmpty());

    // Handles the edge cases of matching at beginning or end of a line
    inMatch = true;
    if (edges.get(0) != 0) {
      inMatch = false;
      edges.splice(0, 0, 0);
    }
    if (edges.peek() != text.length()) {
      edges.add(text.length());
    }
    
    return true;
  }

  @Override
  public boolean shouldLastChunkFillToRight() {
    return false;
  }
}
