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

package com.google.collide.client.code;

import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceConstants;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.logging.Log;
import com.google.collide.client.workspace.WorkspacePlaceNavigationHandler;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;
import com.google.common.base.Strings;

/**
 * Place representing the selection of a file when already in the "Code"
 * perspective.
 *
 */
public class FileSelectedPlace extends Place {
  /**
   * The event that gets dispatched in order to arrive at the Workspace.
   *
   * @See {@link WorkspacePlaceNavigationHandler}.
   */
  public class NavigationEvent extends PlaceNavigationEvent<FileSelectedPlace> {
    public static final int IGNORE_LINE_NUMBER = -1;
    public static final int IGNORE_COLUMN = -1;
    private static final String PATH_KEY = "path";
    private static final String LINE_KEY = "line";
    private static final String COLUMN_KEY = "column";
    private static final String FORCE_RELOAD_KEY = "forceReload";
    private final boolean forceReload;
    private final int lineNumber;
    private final int column;
    private final PathUtil path;

    private NavigationEvent(PathUtil path, int lineNumber, int column, boolean forceReload) {
      super(FileSelectedPlace.this);
      this.path = path;
      this.lineNumber = lineNumber;
      this.column = column;
      this.forceReload = forceReload;
    }

    @Override
    public JsonStringMap<String> getBookmarkableState() {
      JsoStringMap<String> state = JsoStringMap.create();
      state.put(PATH_KEY, path.getPathString());

      /*
       * Only put things that deviate from the default (so we don't pollute the
       * URL)
       */
      if (lineNumber != IGNORE_LINE_NUMBER) {
        state.put(LINE_KEY, Integer.toString(lineNumber));
      }

      if (column != IGNORE_COLUMN) {
        state.put(COLUMN_KEY, Integer.toString(column));
      }

      if (forceReload) {
        state.put(FORCE_RELOAD_KEY, Boolean.toString(forceReload));
      }

      return state;
    }

    public PathUtil getPath() {
      return path;
    }

    /**
     * Returns the line number to pre-scroll to, or {@link #IGNORE_LINE_NUMBER}
     * if the dispatcher does not have a preference for the line number.
     */
    public int getLineNo() {
      return lineNumber;
    }

    public int getColumn() {
      return column;
    }

    public boolean shouldForceReload() {
      return forceReload;
    }
  }

  public static final FileSelectedPlace PLACE = new FileSelectedPlace();

  private FileSelectedPlace() {
    super(PlaceConstants.FILE_SELECTED_PLACE_NAME);
  }

  @Override
  public PlaceNavigationEvent<FileSelectedPlace> createNavigationEvent(
      JsonStringMap<String> decodedState) {
    String lineNumberStr = decodedState.get(NavigationEvent.LINE_KEY);
    int lineNumber = NavigationEvent.IGNORE_LINE_NUMBER;
    if (!Strings.isNullOrEmpty(lineNumberStr)) {
      try {
        lineNumber = Integer.parseInt(lineNumberStr);
        if (lineNumber < 0 && lineNumber != NavigationEvent.IGNORE_LINE_NUMBER) {
          Log.warn(getClass(), "Line number can not to be negative!");
          lineNumber = NavigationEvent.IGNORE_LINE_NUMBER;
        }
      } catch (NumberFormatException e) {
        Log.warn(getClass(), "Illegal line number format!");
      }
    }

    String columnStr = decodedState.get(NavigationEvent.COLUMN_KEY);
    int column = NavigationEvent.IGNORE_COLUMN;
    if (!Strings.isNullOrEmpty(columnStr)) {
      try {
        column = Integer.parseInt(columnStr);
        if (column < 0 && column != NavigationEvent.IGNORE_COLUMN) {
          Log.warn(getClass(), "Column can not to be negative!");
          column = NavigationEvent.IGNORE_COLUMN;
        }
      } catch (NumberFormatException e) {
        Log.warn(getClass(), "Illegal column format!");
      }
    }

    // parseBoolean maps null and non-"true" (ignoring case) to false
    boolean forceReload = Boolean.parseBoolean(decodedState.get(NavigationEvent.FORCE_RELOAD_KEY));

    return createNavigationEvent(new PathUtil(decodedState.get(NavigationEvent.PATH_KEY)),
        lineNumber, column, forceReload);
  }

  public PlaceNavigationEvent<FileSelectedPlace> createNavigationEvent(PathUtil path) {
    return createNavigationEvent(path, NavigationEvent.IGNORE_LINE_NUMBER);
  }

  public PlaceNavigationEvent<FileSelectedPlace> createNavigationEvent(PathUtil path, int lineNo) {
    return createNavigationEvent(path, lineNo, NavigationEvent.IGNORE_COLUMN, false);
  }

  public PlaceNavigationEvent<FileSelectedPlace> createNavigationEvent(PathUtil path, int lineNo,
      int column, boolean forceReload) {
    checkNullPath(path);
    return new NavigationEvent(path, lineNo, column, forceReload);
  }

  private void checkNullPath(PathUtil path) {
    if (path == null) {
      Log.warn(getClass(), "Trying to select a null file!");
    }
  }
}
