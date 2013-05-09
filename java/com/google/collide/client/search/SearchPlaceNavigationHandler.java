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

import com.google.collide.client.AppContext;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.workspace.FileTreeUiController;
import com.google.collide.dto.SearchResponse;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls.SearchImpl;

/**
 * Navigation handler for the {@link SearchPlace}.
 *
 */
public class SearchPlaceNavigationHandler extends PlaceNavigationHandler<
    SearchPlace.NavigationEvent> {

  private final AppContext context;
  private final MultiPanel<?,?> contentArea;
  private final FileTreeUiController fileTreeUiController;
  private final Place currentPlace;
  private SearchContainer searchContainer;

  public SearchPlaceNavigationHandler(AppContext context,
      MultiPanel<?,?> contentArea, FileTreeUiController fileTreeUiController,
      Place currentPlace) {
    this.context = context;
    this.contentArea = contentArea;
    this.fileTreeUiController = fileTreeUiController;
    this.currentPlace = currentPlace;
    searchContainer = null;
  }

  @Override
  public void cleanup() {
    contentArea.getToolBar().show();
  }

  @Override
  protected void enterPlace(SearchPlace.NavigationEvent navigationEvent) {
    fileTreeUiController.clearSelectedNodes();
    if (searchContainer == null) {
      // first entrance (later queries already from a search place don't go
      // here)
      searchContainer =
          new SearchContainer(currentPlace,
              new SearchContainer.View(context.getResources().searchContainerCss()),
              navigationEvent.getQuery());
    }

    contentArea.setContent(searchContainer);
    contentArea.getToolBar().hide();
    StatusMessage message = new StatusMessage(
        context.getStatusManager(), StatusMessage.MessageType.LOADING, "Searching...");
    message.fireDelayed(200);
    context.getFrontendApi().SEARCH.send(SearchImpl
        .make()
        .setQuery(navigationEvent.getQuery())
        .setPage(navigationEvent.getPage()),
        new ApiCallback<SearchResponse>() {

          @Override
          public void onFail(FailureReason reason) {
            new StatusMessage(context.getStatusManager(), StatusMessage.MessageType.ERROR,
                "Search failed in 3 attempts.  Try again later.").fire();
          }

          @Override
          public void onMessageReceived(SearchResponse message) {
            searchContainer.showResults(message);
          }
        });
  }
}
