package com.google.collide.client.terminal;

import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

public class TerminalPlace extends Place{


  public class NavigationEvent extends PlaceNavigationEvent<TerminalPlace> {
    public static final String QUERY_KEY = "q";
    public static final String PAGE_KEY = "p";
    
    private final String query;
    private final int page;
    
    private NavigationEvent(String query) {
      super(TerminalPlace.this);
      this.query = query;
      this.page = 1;
    }

    private NavigationEvent(String query, int page) {
      super(TerminalPlace.this);
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

  
  protected TerminalPlace(String placeName) {
    super(placeName);
  }

  @Override
  public PlaceNavigationEvent<? extends Place> createNavigationEvent(
      JsonStringMap<String> decodedState) {
    return null;
  }

}
