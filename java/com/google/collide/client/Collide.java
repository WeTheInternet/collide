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

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.history.HistoryUtils;
import com.google.collide.client.history.HistoryUtils.ValueChangeListener;
import com.google.collide.client.history.RootPlace;
import com.google.collide.client.status.StatusPresenter;
import com.google.collide.client.util.ClientImplementationsInjector;
import com.google.collide.client.util.Elements;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.client.workspace.WorkspacePlaceNavigationHandler;
import com.google.collide.client.xhrmonitor.XhrWarden;
import com.google.collide.clientlibs.navigation.NavigationToken;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.json.shared.JsonArray;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.Window;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Collide implements EntryPoint {

  /**
   * This is the entry point method.
   */
  @Override
  public void onModuleLoad() {

    // If we do not have a valid Client ID we need to redirect back to the login page.
    if (BootstrapSession.getBootstrapSession().getActiveClientId() == null) {
      Window.Location.assign("/static/login.html");
      return;
    }

    ClientImplementationsInjector.inject();

    final AppContext appContext = AppContext.create();

    GWT.setUncaughtExceptionHandler(appContext.getUncaughtExceptionHandler());
    XhrWarden.watch();

    Resources resources = appContext.getResources();

    // TODO: Figure out why when we use the + operator to concat,
    // these Strings don't at compile time converge to a single String literal.
    // In theory they should. For now we use a StringBuilder.

    // Make sure you call getText() on your CssResource!
    StringBuilder styleBuilder = new StringBuilder();
    styleBuilder.append(resources.appCss().getText());
    styleBuilder.append(resources.baseCss().getText());
    styleBuilder.append(resources.workspaceHeaderCss().getText());
    styleBuilder.append(resources.editorToolBarCss().getText());
    styleBuilder.append(resources.defaultSimpleListCss().getText());
    styleBuilder.append(resources.workspaceShellCss().getText());
    styleBuilder.append(resources.workspaceEditorCss().getText());
    styleBuilder.append(resources.workspaceEditorBufferCss().getText());
    styleBuilder.append(resources.workspaceEditorCursorCss().getText());
    styleBuilder.append(resources.workspaceEditorConsoleViewCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingModelCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarBreakpointsPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarCallStackPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarControlsPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarHeaderCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarNoApiPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarScopeVariablesPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDomInspectorCss().getText());
    styleBuilder.append(
        resources.workspaceEditorDebuggingSidebarWatchExpressionsPaneCss().getText());
    styleBuilder.append(resources.remoteObjectTreeCss().getText());
    styleBuilder.append(resources.remoteObjectNodeRendererCss().getText());
    styleBuilder.append(resources.editorDiffContainerCss().getText());
    styleBuilder.append(resources.evaluationPopupControllerCss().getText());
    styleBuilder.append(resources.goToDefinitionCss().getText());
    styleBuilder.append(resources.treeCss().getText());
    styleBuilder.append(resources.workspaceNavigationCss().getText());
    styleBuilder.append(resources.workspaceNavigationFileTreeSectionCss().getText());
    styleBuilder.append(resources.workspaceNavigationShareWorkspacePaneCss().getText());
    styleBuilder.append(resources.workspaceNavigationToolBarCss().getText());
    styleBuilder.append(resources.workspaceNavigationFileTreeNodeRendererCss().getText());
    styleBuilder.append(resources.workspaceNavigationOutlineNodeRendererCss().getText());
    styleBuilder.append(resources.workspaceNavigationParticipantListCss().getText());
    styleBuilder.append(resources.searchContainerCss().getText());
    styleBuilder.append(resources.statusPresenterCss().getText());
    styleBuilder.append(resources.noFileSelectedPanelCss().getText());
    styleBuilder.append(resources.diffRendererCss().getText());
    styleBuilder.append(resources.deltaInfoBarCss().getText());
    styleBuilder.append(resources.codePerspectiveCss().getText());
    styleBuilder.append(resources.unauthorizedUserCss().getText());
    styleBuilder.append(resources.syntaxHighlighterRendererCss().getText());
    styleBuilder.append(resources.lineNumberRendererCss().getText());
    styleBuilder.append(resources.uneditableDisplayCss().getText());
    styleBuilder.append(resources.editorSelectionLineRendererCss().getText());
    styleBuilder.append(resources.fileHistoryCss().getText());
    styleBuilder.append(resources.timelineCss().getText());
    styleBuilder.append(resources.timelineNodeCss().getText());
    styleBuilder.append(resources.popupCss().getText());
    styleBuilder.append(resources.tooltipCss().getText());
    styleBuilder.append(resources.sliderCss().getText());
    styleBuilder.append(resources.editableContentAreaCss().getText());
    styleBuilder.append(resources.workspaceLocationBreadcrumbsCss().getText());
    styleBuilder.append(resources.awesomeBoxCss().getText());
    styleBuilder.append(resources.awesomeBoxSectionCss().getText());
    styleBuilder.append(resources.centerPanelCss().getText());
    styleBuilder.append(resources.autocompleteComponentCss().getText());
    styleBuilder.append(resources.runButtonTargetPopupCss().getText());
    styleBuilder.append(resources.popupBlockedInstructionalPopupCss().getText());
    styleBuilder.append(resources.dropdownWidgetsCss().getText());
    styleBuilder.append(resources.parenMatchHighlighterCss().getText());
    styleBuilder.append(resources.awesomeBoxHostCss().getText());
    styleBuilder.append(resources.awesomeBoxComponentCss().getText());
    styleBuilder.append(resources.coachmarkCss().getText());
    styleBuilder.append(resources.sidebarListCss().getText());

    /*
     * workspaceNavigationSectionCss, animationController, and
     * resizeControllerCss must come last because they overwrite the CSS
     * properties from previous CSS rules.
     */
    styleBuilder.append(resources.workspaceNavigationSectionCss().getText());
    styleBuilder.append(resources.resizeControllerCss().getText());

    StyleInjector.inject(styleBuilder.toString());
    Elements.injectJs(CodeMirror2.getJs(resources));

    // Setup Places
    setUpPlaces(appContext);

    // Status Presenter
    StatusPresenter statusPresenter = StatusPresenter.create(appContext.getResources());
    Elements.getBody().appendChild(statusPresenter.getView().getElement());
    appContext.getStatusManager().setHandler(statusPresenter);

    // Replay History
    replayHistory(HistoryUtils.parseHistoryString());
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
