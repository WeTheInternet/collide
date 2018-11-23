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

import collide.client.editor.DefaultEditorConfiguration;
import collide.client.filetree.AppContextFileTreeController;
import collide.client.filetree.FileTreeController;
import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeModelNetworkController;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.Resources;
import com.google.collide.client.code.CodePanelBundle;
import com.google.collide.client.code.NavigationAreaExpansionEvent;
import com.google.collide.client.code.ParticipantModel;
import com.google.collide.client.collaboration.CollaborationManager;
import com.google.collide.client.collaboration.DocOpsSavedNotifier;
import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.collide.client.search.FileNameSearch;
import com.google.collide.client.search.TreeWalkFileNameSearchImpl;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.dto.GetWorkspaceMetaDataResponse;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls.GetWorkspaceMetaDataImpl;
import com.google.collide.shared.util.ListenerRegistrar.Remover;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;
import com.google.gwt.core.client.Scheduler;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;
import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;

/**
 * Handler for the selection of a Workspace.
 */
// TODO: At some point we should try to make reEnter work on this thing.
public class WorkspacePlaceNavigationHandler extends PlaceNavigationHandler<WorkspacePlace.NavigationEvent> {

  private final AppContext appContext;
  private final FileNameSearch searchIndex;
  private final RemoverManager keyListenerRemoverManager;

  // Presenters and Controllers that require cleanup.
  private Header header;
  private DocumentManager documentManager;
  private CollaborationManager collaborationManager;

  private WorkspacePlace workspacePlace;
  private WorkspaceShell shell;
  private FileTreeModelNetworkController fileNetworkController;
  private KeepAliveTimer keepAliveTimer;
  private ParticipantModel participantModel;
  private CodePanelBundle codePanelBundle;

  public WorkspacePlaceNavigationHandler(AppContext appContext) {
    this.appContext = appContext;
    this.keyListenerRemoverManager = new RemoverManager();
    this.searchIndex = TreeWalkFileNameSearchImpl.create();
  }

  @Override
  protected void cleanup() {
    if (codePanelBundle != null) {
      codePanelBundle.cleanup();
    }

    if (collaborationManager != null) {
      collaborationManager.cleanup();
    }

    if (documentManager != null) {
      documentManager.cleanup();
    }

    if (keyListenerRemoverManager != null) {
      keyListenerRemoverManager.remove();
    }

    if (keepAliveTimer != null) {
      keepAliveTimer.cancel();
    }
  }

  @Override
  protected void enterPlace(final WorkspacePlace.NavigationEvent navigationEvent) {
    // Instantiate the Root View for the Workspace.
    final Resources res = appContext.getResources();
    WorkspaceShell.View workspaceShellView = new WorkspaceShell.View(res, isDetached());
    workspacePlace = navigationEvent.getPlace();

    final FileTreeController<?> fileTreeController = new AppContextFileTreeController(appContext);
    FileTreeModelNetworkController.OutgoingController fileTreeModelOutgoingNetworkController = new FileTreeModelNetworkController.OutgoingController(
      fileTreeController);
    FileTreeModel fileTreeModel = new FileTreeModel(fileTreeModelOutgoingNetworkController);

    documentManager = DocumentManager.create(fileTreeModel, fileTreeController);

    searchIndex.setFileTreeModel(fileTreeModel);

    participantModel = ParticipantModel.create(appContext.getFrontendApi(), appContext.getMessageFilter());

    IncomingDocOpDemultiplexer docOpRecipient = IncomingDocOpDemultiplexer.create(appContext.getMessageFilter());
    collaborationManager = CollaborationManager.create(appContext, documentManager, participantModel,
      docOpRecipient);

    DocOpsSavedNotifier docOpSavedNotifier = new DocOpsSavedNotifier(documentManager, collaborationManager);

    fileNetworkController = FileTreeModelNetworkController.create(fileTreeModel, fileTreeController,
      navigationEvent.getPlace());

    header = Header.create(workspaceShellView.getHeaderView(), workspaceShellView, workspacePlace,
      appContext, searchIndex, fileTreeModel);

    shell = WorkspaceShell.create(workspaceShellView, header);


    // Add a HotKey listener for to auto-focus the AwesomeBox.
    /* The GlobalHotKey stuff utilizes the wave signal event stuff which filters alt+enter as an unimportant
     * event. This prevents us from using the GlobalHotKey manager here. Note: This is capturing since the
     * editor likes to nom-nom keys, in the dart re-write lets think about this sort of stuff ahead of time. */
    final EventRemover eventRemover = Elements.getBody().addEventListener(Event.KEYDOWN, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        KeyboardEvent event = (KeyboardEvent)evt;
        if (event.isAltKey() && event.getKeyCode() == KeyCode.ENTER) {
          appContext.getAwesomeBoxComponentHostModel().revertToDefaultComponent();
          header.getAwesomeBoxComponentHost().show();
        }
      }
    }, true);

    // Track this for removal in cleanup
    keyListenerRemoverManager.track(new Remover() {
      @Override
      public void remove() {
        eventRemover.remove();
      }
    });

    codePanelBundle = createCodePanelBundle(appContext, shell, fileTreeController, fileTreeModel, searchIndex, documentManager,
      participantModel, docOpRecipient, navigationEvent.getPlace());
    codePanelBundle.attach(isDetached(), new DefaultEditorConfiguration());
    codePanelBundle.setMasterPanel(createMasterPanel(res));

    // Attach to the DOM.
    attachComponents(shell, header);

    // Reset the tab title
    Elements.setCollideTitle("");

    if (!navigationEvent.shouldNavExpand()) {
      workspacePlace.fireEvent(new NavigationAreaExpansionEvent(false));
    }

    // Send a message to enter the workspace and initialize the workspace.
    appContext.getFrontendApi().GET_WORKSPACE_META_DATA.send(GetWorkspaceMetaDataImpl.make(),
      new ApiCallback<GetWorkspaceMetaDataResponse>() {

        @Override
        public void onMessageReceived(GetWorkspaceMetaDataResponse message) {
          if (!navigationEvent.getPlace().isActive()) {
            return;
          }

          // Start the keep-alive timer at 10 second intervals.
          keepAliveTimer = new KeepAliveTimer(appContext, 5000);
          keepAliveTimer.start();

          codePanelBundle.enterWorkspace(navigationEvent.isActiveLeaf(), navigationEvent.getPlace(), message);
        }

        @Override
        public void onFail(FailureReason reason) {
          if (FailureReason.UNAUTHORIZED == reason) {
            /* User is not authorized to access this workspace. At this point, the components of the
             * WorkspacePlace already sent multiple requests to the frontend that are bound to fail with the
             * same reason. However, we don't want to gate loading the workspace to handle the rare case that
             * a user accesses a branch that they do not have permission to access. Better to make the
             * workspace load fast and log errors if the user is not authorized. */
            UnauthorizedUser unauthorizedUser = UnauthorizedUser.create(res);
            shell.setPerspective(unauthorizedUser.getView().getElement());
          }
        }
      });
  }

  protected boolean isDetached() {
    return false;
  }

  protected CodePanelBundle createCodePanelBundle(AppContext appContext, WorkspaceShell shell,
    FileTreeController<?> fileTreeController, FileTreeModel fileTreeModel, FileNameSearch searchIndex, DocumentManager documentManager,
    ParticipantModel participantModel, IncomingDocOpDemultiplexer docOpRecipient, WorkspacePlace place) {
    return new CodePanelBundle(appContext, shell, fileTreeController, fileTreeModel, searchIndex, documentManager,
      participantModel, docOpRecipient, place);
  }

  protected void attachComponents(WorkspaceShell shell, Header header) {
    Elements.replaceContents(AppContext.GWT_ROOT, shell.getView().getElement());
  }

  @Override
  protected void reEnterPlace(final WorkspacePlace.NavigationEvent navigationEvent, boolean hasNewState) {
    if (hasNewState || navigationEvent.shouldForceReload()) {
      // Simply do the default action which is to run cleanup/enter.
      super.reEnterPlace(navigationEvent, hasNewState);
    } else {
      // we check to see if we end up being the active leaf and if so show the
      // no file selected panel.
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

  protected void onNoFileSelected() {
    codePanelBundle.showNoFileSelectedPanel();
  }

  protected MultiPanel<?,?> createMasterPanel(Resources resources) {
    return codePanelBundle.contentArea;
  }
}
