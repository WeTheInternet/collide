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

import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceConstants;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

/**
 * @{link Place} for the Workspace search results page.
 */
public class SearchPlace extends Place {

  public class NavigationEvent extends PlaceNavigationEvent<SearchPlace> {
    public static final String QUERY_KEY = "q";
    public static final String PAGE_KEY = "p";
    
    private final String query;
    private final int page;
    
    private NavigationEvent(String query) {
      super(SearchPlace.this);
      this.query = query;
      this.page = 1;
    }

    private NavigationEvent(String query, int page) {
      super(SearchPlace.this);
      this.query = query;
      this.page = page;
    }

    @Override
    public JsonStringMap<String> getBookmarkableState() {
      JsoStringMap<String> map = JsoStringMap.create();
      map.put(QUERY_KEY, query);
      map.put(PAGE_KEY, Integer.toString(page));
      return map;      
    }

    public String getQuery() {
      return query;
    }

    public int getPage() {
      return page;
    }
  }

  public static final SearchPlace PLACE = new SearchPlace();

  private SearchPlace() {
    super(PlaceConstants.WORKSPACE_SEARCH_PLACE_NAME);
  }

  @Override
  public PlaceNavigationEvent<SearchPlace> createNavigationEvent(
      JsonStringMap<String> decodedState) {
    int page = 1;
    String pageString = decodedState.get(NavigationEvent.PAGE_KEY); 
    if (pageString != null) {
      page = Integer.parseInt(pageString);
    }
    return new NavigationEvent(decodedState.get(NavigationEvent.QUERY_KEY), page);
  }

  /**
   * @param query the query expression to search on
   * @return a new navigation event
   */
  public PlaceNavigationEvent<SearchPlace> createNavigationEvent(String query) {
    return new NavigationEvent(query);
  }

  /**
   * @param query the query expression to search on
   * @return a new navigation event
   */
  public PlaceNavigationEvent<SearchPlace> createNavigationEvent(String query, int page) {
    return new NavigationEvent(query, page);
  }

}
