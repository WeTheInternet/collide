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

package com.google.collide.client.filehistory;

import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceConstants;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.WorkspacePlaceNavigationHandler;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

/**
 * A Place representing the file history view in the workspace.
 *
 *
 */
public class FileHistoryPlace extends Place {
  /**
   * The event that gets dispatched in order to arrive at the Workspace.
   *
   * @See {@link WorkspacePlaceNavigationHandler}.
   */
  public class NavigationEvent extends PlaceNavigationEvent<FileHistoryPlace> {
    private static final String PATH_KEY = "path";
    private static final String ROOT_ID_KEY = "revision";
    private final PathUtil path;
    private final String rootId;
    
    private NavigationEvent(PathUtil path, String rootId) {
      super(FileHistoryPlace.this);
      this.path = path;
      this.rootId = rootId;
    }

    @Override
    public JsonStringMap<String> getBookmarkableState() {
      JsoStringMap<String> state = JsoStringMap.create();
      state.put(PATH_KEY, path.getPathString());
      if (rootId != null) {
        state.put(ROOT_ID_KEY, rootId);
      }
      return state;
    }

    public PathUtil getPath() {
      return path;
    }

    public String getRootId() {
      return rootId;
    }
  }

  public static final FileHistoryPlace PLACE = new FileHistoryPlace();

  FileHistoryPlace() {
    super(PlaceConstants.FILEHISTORY_PLACE_NAME);
  }

  @Override
  public PlaceNavigationEvent<FileHistoryPlace> createNavigationEvent(
      JsonStringMap<String> decodedState) {
    return createNavigationEvent(new PathUtil(decodedState.get(NavigationEvent.PATH_KEY)),
        decodedState.get(NavigationEvent.ROOT_ID_KEY));
  }

  public PlaceNavigationEvent<FileHistoryPlace> createNavigationEvent(
      PathUtil path, String rootId) {
    // TODO: considering adding rootId to PathUtil so that the client
    // can somewhat sensibly talk about paths to objects in both space and time.
    return new NavigationEvent(path, rootId);
  }
}
