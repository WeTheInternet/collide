package com.google.collide.plugin.client.standalone;

import xapi.util.api.SuccessHandler;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.CollideBootstrap;
import com.google.collide.client.history.HistoryUtils;
import com.google.collide.client.history.HistoryUtils.ValueChangeListener;
import com.google.collide.client.history.RootPlace;
import com.google.collide.client.status.StatusPresenter;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.clientlibs.navigation.NavigationToken;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.plugin.client.PluginNavigationHandler;
import com.google.collide.plugin.client.PluginPlace;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class StandalonePlugin implements EntryPoint {

  @Override
  public void onModuleLoad() {

    CollideBootstrap.start(new SuccessHandler<AppContext>() {
      @Override
      public void onSuccess(AppContext appContext) {

//      Elements.replaceContents(AppContext.GWT_ROOT, panel.getView().getElement());
      StandaloneContext ctx = StandaloneContext.create(appContext);

      WorkspacePlace.PLACE.setIsStrict(false);
      StandaloneNavigationHandler handler = new StandaloneNavigationHandler(ctx);
      RootPlace.PLACE.registerChildHandler(WorkspacePlace.PLACE, handler,
        false);
      PluginNavigationHandler api = new PluginNavigationHandler(ctx);
      RootPlace.PLACE.registerChildHandler(PluginPlace.PLACE, api, true);

      // Back/forward buttons or manual manipulation of the hash.
      HistoryUtils.addValueChangeListener(new ValueChangeListener() {
        @Override
        public void onValueChanged(String historyString) {
          replayHistory(HistoryUtils.parseHistoryString(historyString));
        }
      });

      // Status Presenter
      StatusPresenter statusPresenter = StatusPresenter.create(appContext.getResources());
      Elements.getBody().appendChild(statusPresenter.getView().getElement());
      appContext.getStatusManager().setHandler(statusPresenter);

      Scheduler.get().scheduleFinally(new ScheduledCommand() {
        @Override
        public void execute() {
          // Replay History
          replayHistory(HistoryUtils.parseHistoryString());
        }
      });

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
