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

package com.google.collide.plugin.client.standalone;

import collide.client.filetree.FileTreeController;
import collide.client.filetree.FileTreeModel;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.Resources;
import com.google.collide.client.code.CodePanelBundle;
import com.google.collide.client.code.ParticipantModel;
import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.search.FileNameSearch;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.workspace.Header;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.client.workspace.WorkspacePlace.NavigationEvent;
import com.google.collide.client.workspace.WorkspacePlaceNavigationHandler;
import com.google.collide.client.workspace.WorkspaceShell;
import com.google.gwt.core.client.Scheduler;

/**
 * Handler for the selection of a Workspace.
 */
public class StandaloneNavigationHandler extends WorkspacePlaceNavigationHandler {

  private final StandaloneWorkspace panel;
  private boolean once;

  public StandaloneNavigationHandler(StandaloneContext ctx) {
    super(ctx.getAppContext());
    this.panel = ctx.getPanel();
    once = true;
  }

  @Override
  protected void cleanup() {
//    super.cleanup();
  }

  @Override
  protected void attachComponents(WorkspaceShell shell, Header header) {
    Elements.replaceContents(StandaloneConstants.HEADER_PANEL, header.getView().getElement());
    Elements.replaceContents(StandaloneConstants.FILES_PANEL, shell.getView().getElement());
  }

  @Override
  protected MultiPanel<?,?> createMasterPanel(Resources resources){
    return panel;
  }

  @Override
  protected boolean isDetached() {
    return true;
  }

  @Override
  protected CodePanelBundle createCodePanelBundle(AppContext appContext, WorkspaceShell shell,
    FileTreeController<?> fileTreeController, FileTreeModel fileTreeModel, FileNameSearch searchIndex, DocumentManager documentManager,
    ParticipantModel participantModel, IncomingDocOpDemultiplexer docOpRecipient, WorkspacePlace place) {
    return new StandaloneCodeBundle(appContext, shell, fileTreeController,  fileTreeModel, searchIndex, documentManager,
      participantModel, docOpRecipient, place);
  }

  @Override
  protected void enterPlace(final NavigationEvent navigationEvent) {
    if (once) {
      once = false;
      super.enterPlace(navigationEvent);
      Scheduler.get().scheduleFinally(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          if (navigationEvent.isActiveLeaf()) {
            onNoFileSelected();
          }
        }
      });
    }
  }

  @Override
  protected void onNoFileSelected() {
    super.onNoFileSelected();
    panel.closeEditor();
  }

}