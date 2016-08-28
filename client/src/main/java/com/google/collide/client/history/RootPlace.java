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

package com.google.collide.client.history;

import com.google.collide.client.util.logging.Log;
import com.google.collide.clientlibs.navigation.NavigationToken;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;

/**
 * The Root {@link Place}. We are in the Root place implicitly via onModuleLoad.
 * Thus the RootPlace has no associated {@link PlaceNavigationHandler} or
 * {@link PlaceNavigationEvent}.
 */
public class RootPlace extends Place {

  class NullEvent extends PlaceNavigationEvent<RootPlace> {
    protected NullEvent() {
      super(RootPlace.this);
    }

    @Override
    public JsonStringMap<String> getBookmarkableState() {
      Log.error(getClass(), "The ROOT place should never need bookmarkable state!");
      return null;
    }
  }

  /**
   * The Root Place is implicit. We get here from onModuleLoad.
   */
  static class NullHandler extends PlaceNavigationHandler<NullEvent> {
    @Override
    protected void enterPlace(NullEvent navigationEvent) {
      Log.error(getClass(), "The Navigation handler for the ROOT Place should never fire!");
    }
  }

  public static final RootPlace PLACE = new RootPlace();

  public static final String ROOT_NAME = "Root";

  /**
   * This is an optional default for navigations on the Root that fail. note
   * that this place MUST be able to handle a navigation with an empty key/value
   * state map.
   */
  private Place defaultPlace;

  private RootPlace() {
    super(ROOT_NAME);

    // The Root is always active.
    setIsActive(true, null);
  }

  @Override
  public NullEvent createNavigationEvent(JsonStringMap<String> decodedState) {
    Log.error(getClass(), "The ROOT Place should never need to create a Navigation Event!");
    return null;
  }

  @Override
  public void dispatchHistory(JsonArray<NavigationToken> historyPieces) {
    if (historyPieces.isEmpty()
        || (getRegisteredChild(historyPieces.get(0).getPlaceName()) == null)) {

      // If the history string is empty, or if the first navigation is bogus,
      // then we go to the default child Place if any.
      if (defaultPlace != null) {
        fireChildPlaceNavigation(defaultPlace.createNavigationEvent(JsoStringMap.<String>create()));
      }
      return;
    }

    super.dispatchHistory(historyPieces);
  }

  /**
   * Same as {@link Place#registerChildHandler(Place, PlaceNavigationHandler)}, except this takes an
   * additional parameter to specify whether or not we should treat this child {@link Place} as a
   * default fallback for bogus navigations.
   */
  public <E extends PlaceNavigationEvent<C>, N extends PlaceNavigationHandler<E>,
      C extends Place> void registerChildHandler(C childPlace, N handler, boolean isDefault) {
    if (isDefault) {
      this.defaultPlace = childPlace;
    }

    super.registerChildHandler(childPlace, handler);
  }
}
