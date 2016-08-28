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

package com.google.collide.client.workspace;

import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceConstants;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

/**
 * {@link Place} for arriving at the Workspace.
 */
public class WorkspacePlace extends Place {
  /**
   * The event that gets dispatched in order to arrive at the Workspace.
   *
   * @See {@link WorkspacePlaceNavigationHandler}.
   */
  public class NavigationEvent extends PlaceNavigationEvent<WorkspacePlace> {
    public static final String NAV_EXPAND = "navEx";

    private final boolean forceReload;
    private final boolean shouldNavExpand;

    public NavigationEvent(boolean shouldNavExpand, boolean forceReload) {
      super(WorkspacePlace.this);
      this.forceReload = forceReload;
      this.shouldNavExpand = shouldNavExpand;
    }

    @Override
    public JsonStringMap<String> getBookmarkableState() {
      JsoStringMap<String> state = JsoStringMap.create();
      if (!shouldNavExpand) {
        state.put(NAV_EXPAND, String.valueOf(shouldNavExpand));
      }
      return state;
    }

    public boolean shouldNavExpand() {
      return shouldNavExpand;
    }

    public boolean shouldForceReload() {
      return forceReload;
    }
  }

  public static final WorkspacePlace PLACE = new WorkspacePlace();

  protected WorkspacePlace() {
    super(PlaceConstants.WORKSPACE_PLACE_NAME);
  }

  @Override
  public PlaceNavigationEvent<WorkspacePlace> createNavigationEvent(
      JsonStringMap<String> decodedState) {
    String shouldNavExpandString = decodedState.get(NavigationEvent.NAV_EXPAND);
    boolean shouldNavExpand =
        shouldNavExpandString == null ? true : Boolean.parseBoolean(shouldNavExpandString);

    return createNavigationEvent(shouldNavExpand, false);
  }

  public PlaceNavigationEvent<WorkspacePlace> createNavigationEvent(boolean shouldNavExpand) {
    return createNavigationEvent(shouldNavExpand, false);
  }

  @Override
  public PlaceNavigationEvent<WorkspacePlace> createNavigationEvent() {
    return new NavigationEvent(true, false);
  }


  public PlaceNavigationEvent<WorkspacePlace> createNavigationEvent(
      boolean shouldNavExpand, boolean forceReload) {
    return new NavigationEvent(shouldNavExpand, forceReload);
  }

}
