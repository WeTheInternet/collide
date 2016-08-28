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

import com.google.collide.client.code.FileSelectedPlace;
import com.google.collide.client.history.HistoryUtils;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Utility methods for Workspace operations.
 *
 */
public class WorkspaceUtils {

  /**
   * Builds a deep link to a workspace.
   */
  public static String createLinkToWorkspace() {
    JsonArray<PlaceNavigationEvent<?>> history = JsonCollections.createArray();
    history.add(WorkspacePlace.PLACE.createNavigationEvent());
    return "#" + HistoryUtils.createHistoryString(history);
  }

  /**
   * Builds a deep link to a file given a workspace id and file path.
   */
  public static String createDeepLinkToFile(PathUtil filePath) {
    JsonArray<PlaceNavigationEvent<?>> history = JsonCollections.createArray();
    history.add(WorkspacePlace.PLACE.createNavigationEvent(true));
    history.add(FileSelectedPlace.PLACE.createNavigationEvent(filePath));

    return "#" + HistoryUtils.createHistoryString(history);
  }

  private WorkspaceUtils() {
  }
}
