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

import com.google.collide.client.AppContext;
import com.google.collide.client.code.FileSelectedPlace.NavigationEvent;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * {@link PlaceNavigationHandler} responsible for handling the selection of files.
 */
public class FileSelectedPlaceNavigationHandler
    extends PlaceNavigationHandler<FileSelectedPlace.NavigationEvent> {

  private final AppContext appContext;
  private final FileSelectionController fileSelectionController;
  private final EditableContentArea contentArea;

  public FileSelectedPlaceNavigationHandler(AppContext appContext,
      CodePerspective codePerspective,
      FileSelectionController fileSelectionController,
      EditableContentArea contentArea) {
    this.appContext = appContext;
    this.fileSelectionController = fileSelectionController;
    this.contentArea = contentArea;
  }

  @Override
  public void cleanup() {
    contentArea.getEditorToolBar().hide();
  }

  @Override
  protected void enterPlace(final NavigationEvent navigationEvent) {
    contentArea.getEditorToolBar().show();
    selectFile(navigationEvent);
  }

  @Override
  protected void reEnterPlace(final NavigationEvent navigationEvent, boolean hasNewState) {
    if (!hasNewState && !navigationEvent.shouldForceReload()) {

      // Nothing to do.
      return;
    }

    selectFile(navigationEvent);
  }

  private void selectFile(final NavigationEvent navigationEvent) {
    /*
     * The navigation event will not be active until all handlers have been called, so wait until
     * the end of the event loop for the navigation event to become active.
     */
    Scheduler.get().scheduleFinally(new ScheduledCommand() {
      @Override
      public void execute() {
        if (navigationEvent.isActiveLeaf()) {
          fileSelectionController.selectFile(navigationEvent);
          contentArea.getEditorToolBar().setCurrentPath(navigationEvent.getPath(),
              fileSelectionController.getFileTreeModel().getLastAppliedTreeMutationRevision());
        }
      }
    });
  }
}
