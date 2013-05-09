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

package com.google.collide.client.code.gotodefinition;

import com.google.collide.client.code.FileSelectedPlace;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.history.Place;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.logging.Log;
import com.google.gwt.core.client.Scheduler;

import elemental.client.Browser;

/**
 * Helper class that performs navigation for various references.
 *
 */
class ReferenceNavigator {

  private final Editor editor;
  private final Place currentPlace;
  private String currentFilePath;

  public ReferenceNavigator(Place currentPlace, Editor editor) {
    this.currentPlace = currentPlace;
    this.editor = editor;
  }

  public void goToFile(String path, final int lineNumber, final int column) {
    if (path.equals(currentFilePath)) {
      // Defer over other mouse click and mouse move listeners.
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          editor.scrollTo(lineNumber, column);
        }
      });
    } else {
      currentPlace.fireChildPlaceNavigation(FileSelectedPlace.PLACE.createNavigationEvent(
          new PathUtil(path), lineNumber, column, false));
    }
  }

  public void goToUrl(String url) {
    Log.debug(getClass(), "Navigating to URL \"" + url + "\"");
    Browser.getWindow().open(url,url);
  }

  public void setCurrentFilePath(String path) {
    this.currentFilePath = path;
  }
}
