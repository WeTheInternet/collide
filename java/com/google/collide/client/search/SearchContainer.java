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

package com.google.collide.client.search;

import com.google.collide.client.code.FileContent;
import com.google.collide.client.code.FileSelectedPlace;
import com.google.collide.client.history.Place;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.SearchResponse;
import com.google.collide.dto.SearchResult;
import com.google.collide.dto.Snippet;
import com.google.collide.json.client.JsoArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.SpanElement;

/**
 * Container for search results, at least for those searches that have a results
 * page. (Local search and search-and-replace do not...)
 *
 */
public class SearchContainer extends UiComponent<SearchContainer.View> implements FileContent {

  public interface Css extends CssResource {
    String container();

    String next();

    String otherpage();

    String pager();

    String previous();

    String second();

    String snippet();

    String thispage();

    String title();
  }

  public interface Resources extends ClientBundle {
    @Source("next_page.png")
    ImageResource nextPage();

    @Source("previous_page.png")
    ImageResource prevPage();

    @Source("SearchContainer.css")
    Css searchContainerCss();
  }

  public static class View extends CompositeView<Void> {
    Css css;
    DivElement pager;
    DivElement results;

    public View(Css css) {
      super(Elements.createDivElement(css.container()));
      this.css = css;
      createDom();
    }

    private void createDom() {
      Element top = getElement();
      results = Elements.createDivElement();
      top.appendChild(results);
      pager = Elements.createDivElement(css.pager());
      top.appendChild(pager);
    }

    public void clear() {
      results.removeFromParent();
      pager.removeFromParent();
      createDom();
    }
  }

  private final String query;
  private final Place currentPlace;

  public SearchContainer(Place currentPlace, View view, final String query) {
    super(view);
    this.currentPlace = currentPlace;
    this.query = query;
  }

  /**
   * Updates with new results.
   *
   * @param message the message containing the new results.
   */
  public void showResults(SearchResponse message) {
    getView().clear();
    showResultsImpl(
        message.getPage(), message.getPageCount(), (JsoArray<SearchResult>) message.getResults());
  }
  
  @Override
  public PathUtil filePath() {
    return null;
  }

  @Override
  public Element getContentElement() {
    return getView().getElement();
  }

  @Override
  public void onContentDisplayed() {
  }

  // TODO: Clean up the code below. It does not properly follow the
  // View/Presenter contracts used by other UiComponents. It also makes many
  // static references to Places on dispatch.
  /**
   * Updates the view to displays results and appropriate pager widgetry.
   *
   * @param page the page of "this" result page, one-based
   * @param pageCount the total number of pages
   * @param items the {@link SearchResult} items on this page.
   */
  private void showResultsImpl(final int page, int pageCount, JsoArray<SearchResult> items) {
    Css css = getView().css;
    buildPager(page, pageCount, css);

    for (int i = 0; i < items.size(); i++) {
      SearchResult item = items.get(i);
      DivElement outer = Elements.createDivElement();
      if (i > 0) {
        outer.setClassName(css.second());
      }
      final PathUtil path = new PathUtil(item.getTitle());
      AnchorElement title = Elements.createAnchorElement(css.title());
      title.setTextContent(item.getTitle());
      if (item.getUrl() != null) {
        // this is unusual, but allows search results to point outside of this
        // workspace, e.g. to language API docs.
        title.setHref(item.getUrl());
      } else {
        // this is the common case; the title will be a path in this workspace
        // and clicking on the link should take us to its editor.
        title.setOnclick(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            currentPlace.fireChildPlaceNavigation(
                FileSelectedPlace.PLACE.createNavigationEvent(path));
          }
        });
      }
      outer.appendChild(title);
      JsoArray<Snippet> snippets = (JsoArray<Snippet>) item.getSnippets();
      for (int j = 0; j < snippets.size(); j++) {
        DivElement snippetDiv = Elements.createDivElement(css.snippet());
        final int lineNo = snippets.get(j).getLineNumber();
        snippetDiv.setTextContent(lineNo + ": " + snippets.get(j).getSnippetText());
        snippetDiv.setOnclick(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            // lineNo is 1-based, whereas the editor expects 0-based
            int documentLineNo = lineNo - 1;
            currentPlace.fireChildPlaceNavigation(
                FileSelectedPlace.PLACE.createNavigationEvent(path, documentLineNo));
          }
        });
        outer.appendChild(snippetDiv);
      }
      getView().results.appendChild(outer);
    }
  }

  private void buildPager(final int page, final int pageCount, Css css) {
    if (pageCount > 1) {
      if (page > 1) {
        DivElement previous = Elements.createDivElement(css.previous());
        getView().pager.appendChild(previous);
        previous.setOnclick(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            currentPlace.fireChildPlaceNavigation(
                SearchPlace.PLACE.createNavigationEvent(query, page - 1));
          }
        });
      }
      if (page > 7) {
        SpanElement elipsis = Elements.createSpanElement(css.thispage());
        elipsis.setTextContent("...");
        getView().pager.appendChild(elipsis);
      }
      // page numbers are one-based (i.e. human-oriented)
      for (int i = page > 6 ? page - 6 : 1; i < pageCount + 1 && i < page + 6; i++) {
        SpanElement counter =
            Elements.createSpanElement(i == page ? css.thispage() : css.otherpage());
        counter.setTextContent(Integer.toString(i));
        getView().pager.appendChild(counter);
        final int pageNumber = i;
        counter.setOnclick(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            currentPlace.fireChildPlaceNavigation(
                SearchPlace.PLACE.createNavigationEvent(query, pageNumber));
          }
        });
        if (page + 7 < pageCount + 1) {
          SpanElement elipsis = Elements.createSpanElement(css.thispage());
          elipsis.setTextContent("...");
          getView().pager.appendChild(elipsis);
        }
      }
      if (page < pageCount) {
        DivElement next = Elements.createDivElement(css.next());
        getView().pager.appendChild(next);
        next.setOnclick(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            currentPlace.fireChildPlaceNavigation(
                SearchPlace.PLACE.createNavigationEvent(query, page + 1));
          }
        });
      }
    }
  }

  @Override
  public void onContentDestroyed() {

  }
}
