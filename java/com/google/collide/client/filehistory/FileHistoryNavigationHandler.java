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

import com.google.collide.client.AppContext;
import com.google.collide.client.code.EditableContentArea;
import com.google.collide.client.code.EditableContentArea.Content;
import com.google.collide.client.diff.EditorDiffContainer;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;

/**
 * Navigation handler for the FileHistoryPlace.
 *
 *
 */
public class FileHistoryNavigationHandler
    extends PlaceNavigationHandler<FileHistoryPlace.NavigationEvent> {

  private final EditorDiffContainer editorDiffContainer;
  private final Timeline timeline;
  private final FileHistory fileHistory;
  private final EditableContentArea contentArea;
  private final FileHistoryApi api;
  private final AppContext appContext;
  private Content oldContent;

  public FileHistoryNavigationHandler(Place currentPlace,
      AppContext appContext,
      EditableContentArea contentArea,
      DocumentManager documentManager) {
    this.appContext = appContext;
    this.contentArea = contentArea;
    this.editorDiffContainer = EditorDiffContainer.create(appContext);
    this.fileHistory = FileHistory.create(
        currentPlace, appContext, new FileHistory.View(appContext.getResources()));
    this.timeline = Timeline.create(fileHistory, appContext);
    this.api =
        new FileHistoryApi(appContext,editorDiffContainer, timeline, documentManager);
    this.fileHistory.setApi(api);
    this.timeline.setApi(api);
  }

  @Override
  public void cleanup() {
    fileHistory.teardown();
    contentArea.getEditorToolBar().show();
    if (oldContent != null) {
      contentArea.setContent(oldContent);
    }
  }

  @Override
  protected void enterPlace(FileHistoryPlace.NavigationEvent navigationEvent) {
    Elements.asJsElement(fileHistory.getView().diff)
        .appendChild(editorDiffContainer.getView().getElement());

    oldContent = contentArea.getCurrentContent();
    contentArea.setContent(fileHistory);
    contentArea.setLocationBreadcrumbsVisibility(false);
    fileHistory.setup(contentArea.getView().getHeaderElement());
    contentArea.getEditorToolBar().hide();

    /* Get file contents and diff */
    PathUtil filePath = navigationEvent.getPath();
    fileHistory.setPath(filePath);
    timeline.setPath(filePath);
    timeline.setLoading();
    api.getFileRevisions(filePath, navigationEvent.getRootId());
  }
}
