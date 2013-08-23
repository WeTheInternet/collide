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

package com.google.collide.client;

import xapi.util.api.SuccessHandler;

import com.google.collide.client.history.HistoryUtils;
import com.google.collide.client.history.HistoryUtils.ValueChangeListener;
import com.google.collide.client.history.RootPlace;
import com.google.collide.client.status.StatusPresenter;
import com.google.collide.client.util.Elements;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.client.workspace.WorkspacePlaceNavigationHandler;
import com.google.collide.clientlibs.navigation.NavigationToken;
import com.google.collide.json.shared.JsonArray;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.EntryPoint;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Collide implements EntryPoint {

  /**
   * This is the entry point method.
   */
  @Override
  public void onModuleLoad() {

    CollideBootstrap.start(new SuccessHandler<AppContext>() {
      @Override
      public void onSuccess(AppContext appContext) {
        // Setup Places
        setUpPlaces(appContext);
        
        // Status Presenter
        StatusPresenter statusPresenter = StatusPresenter.create(appContext.getResources());
        Elements.getBody().appendChild(statusPresenter.getView().getElement());
        appContext.getStatusManager().setHandler(statusPresenter);
        
        // Replay History
        replayHistory(HistoryUtils.parseHistoryString());
      }
    });

  }

  @VisibleForTesting
  static void setUpPlaces(AppContext context) {
    RootPlace.PLACE.registerChildHandler(
        WorkspacePlace.PLACE, new WorkspacePlaceNavigationHandler(context), true);

    // Back/forward buttons or manual manipulation of the hash.
    HistoryUtils.addValueChangeListener(new ValueChangeListener() {
      @Override
      public void onValueChanged(String historyString) {
        replayHistory(HistoryUtils.parseHistoryString(historyString));
      }
    });
  }

  private static void replayHistory(JsonArray<NavigationToken> historyPieces) {

    // We don't want to snapshot history as we fire the Place events in the
    // replay.
    RootPlace.PLACE.disableHistorySnapshotting();
    RootPlace.PLACE.dispatchHistory(historyPieces);
    RootPlace.PLACE.enableHistorySnapshotting();
  }
}
