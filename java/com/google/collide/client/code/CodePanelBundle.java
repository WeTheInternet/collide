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

import org.waveprotocol.wave.client.common.util.SignalEvent;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.FileSelectionController.FileOpenedEvent;
import com.google.collide.client.code.errorrenderer.EditorErrorListener;
import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.editor.Editor.DocumentListener;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.search.FileNameSearch;
import com.google.collide.client.search.SearchPlace;
import com.google.collide.client.search.SearchPlaceNavigationHandler;
import com.google.collide.client.search.awesomebox.AwesomeBoxContext;
import com.google.collide.client.search.awesomebox.FileNameNavigationSection;
import com.google.collide.client.search.awesomebox.GotoActionSection;
import com.google.collide.client.search.awesomebox.OutlineViewAwesomeBoxSection;
import com.google.collide.client.search.awesomebox.PrimaryWorkspaceActionSection;
import com.google.collide.client.search.awesomebox.PrimaryWorkspaceActionSection.FindActionSelectedCallback;
import com.google.collide.client.search.awesomebox.components.FindReplaceComponent;
import com.google.collide.client.search.awesomebox.components.FindReplaceComponent.FindMode;
import com.google.collide.client.search.awesomebox.host.AwesomeBoxComponentHost.AwesomeBoxComponentHiddenListener;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.dom.eventcapture.GlobalHotKey;
import com.google.collide.client.workspace.FileTreeModel;
import com.google.collide.client.workspace.FileTreeNodeMoveController;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.client.workspace.WorkspaceShell;
import com.google.collide.client.workspace.outline.OutlineModel;
import com.google.collide.client.workspace.outline.OutlineSection;
import com.google.collide.dto.GetWorkspaceMetaDataResponse;
import com.google.collide.shared.document.Document;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;

import elemental.html.Element;

/**
 * Dependencies and Presenter setup code for everything under the workspace header involved with
 * code editing.
 */
/*
 * TODO: Some cleanup in here now that the this class is created and invoked synchronously.
 * Particularly lets extract the AwesomeBox cruft and see if anything else makes sense to bundle
 * together into a separate class.
 */
public class CodePanelBundle {

  // Dependencies passed in at Handler construction time.
  private final AppContext appContext;
  private final WorkspaceShell shell;
  private final FileTreeModel fileTreeModel;
  private final DocumentManager documentManager;
  private final ParticipantModel participantModel;
  private final IncomingDocOpDemultiplexer docOpReceiver;

  // AwesomeBox Related
  public final FileNameSearch searchIndex;
  public final AwesomeBoxContext awesomeBoxCodeContext;
  private final FileNameNavigationSection fileNavSection;
  private final PrimaryWorkspaceActionSection primaryWorkspaceActionsSection;
  private final GotoActionSection gotoActionSection;
  private final OutlineViewAwesomeBoxSection outlineViewAwesomeBoxSection;
  private final WorkspacePlace workspacePlace;


  // AwesomeBox Components
  private final FindReplaceComponent findReplaceComponent;

  // Presenters and Controllers that require cleanup.
  private EditorBundle editorBundle;
  private EditorReloadingFileTreeListener editorReloadingFileTreeListener;
  private NoFileSelectedPanel welcomePanel;
  private FileSelectionController fileSelectionController;
  private FileTreeNodeMoveController treeNodeMoveController;
  private FileTreeSection fileTreeSection;
  private EditableContentArea contentArea;

  public CodePanelBundle(AppContext appContext,
      WorkspaceShell shell,
      FileTreeModel fileTreeModel,
      FileNameSearch searchIndex,
      DocumentManager documentManager,
      ParticipantModel participantModel,
      IncomingDocOpDemultiplexer docOpReceiver,
      WorkspacePlace workspacePlace) {
    this.appContext = appContext;
    this.shell = shell;
    this.fileTreeModel = fileTreeModel;
    this.searchIndex = searchIndex;
    this.documentManager = documentManager;
    this.participantModel = participantModel;
    this.docOpReceiver = docOpReceiver;
    this.workspacePlace = workspacePlace;

    awesomeBoxCodeContext = new AwesomeBoxContext(new AwesomeBoxContext.Builder().setWaterMarkText(
        "Type to find files and use actions").setPlaceholderText("Actions"));
    fileNavSection = new FileNameNavigationSection(appContext.getResources(), searchIndex);
    primaryWorkspaceActionsSection = new PrimaryWorkspaceActionSection(appContext.getResources());
    gotoActionSection = new GotoActionSection(appContext.getResources());
    outlineViewAwesomeBoxSection = new OutlineViewAwesomeBoxSection(appContext.getResources());

    // Special Components
    findReplaceComponent =
        new FindReplaceComponent(new FindReplaceComponent.View(appContext.getResources()));

    initializeAwesomeBoxContext();
  }

  private void initializeAwesomeBoxContext() {
    primaryWorkspaceActionsSection.getFindActionSelectionListener()
        .add(new FindActionSelectedCallback() {
          @Override
          public void onSelected(FindMode mode) {
            findReplaceComponent.setFindMode(mode);
            appContext.getAwesomeBoxComponentHostModel().setActiveComponent(findReplaceComponent);
            shell.getHeader().getAwesomeBoxComponentHost().show();
          }
        });

    awesomeBoxCodeContext.addSection(outlineViewAwesomeBoxSection);
    awesomeBoxCodeContext.addSection(fileNavSection);
    awesomeBoxCodeContext.addSection(gotoActionSection);
    awesomeBoxCodeContext.addSection(primaryWorkspaceActionsSection);
  }

  public void cleanup() {
    editorReloadingFileTreeListener.cleanup();
    editorBundle.cleanup();
    if (welcomePanel != null) {
      welcomePanel.detach();
    }
    treeNodeMoveController.cleanup();
    fileTreeSection.cleanup();

    GlobalHotKey.unregister(appContext.getKeyBindings().localFind());
    GlobalHotKey.unregister(appContext.getKeyBindings().localReplace());
    GlobalHotKey.unregister(appContext.getKeyBindings().gotoLine());
    GlobalHotKey.unregister(appContext.getKeyBindings().snapshot());
  }

  public void attach() {
    // Construct the Root View for the CodePerspective.
    CodePerspective.Resources res = appContext.getResources();
    CodePerspective.View codePerspectiveView = new CodePerspective.View(res);

    // Then create all the Presenters.
    OutlineModel outlineModel = new OutlineModel();
    editorBundle = EditorBundle.create(appContext,
        workspacePlace,
        documentManager,
        participantModel,
        outlineModel,
        fileTreeModel,
        EditorErrorListener.NOOP_ERROR_RECEIVER);

    UneditableDisplay uneditableDisplay = UneditableDisplay.create(new UneditableDisplay.View(res));

    // AwesomeBox Section Setup
    appContext.getAwesomeBoxModel().changeContext(awesomeBoxCodeContext);
    outlineViewAwesomeBoxSection.setOutlineModelAndEditor(outlineModel, editorBundle.getEditor());
    registerAwesomeBoxEvents();

    editorReloadingFileTreeListener =
        EditorReloadingFileTreeListener.create(workspacePlace, editorBundle, fileTreeModel);

    fileTreeSection = FileTreeSection.create(
        workspacePlace, appContext, fileTreeModel, editorBundle.getDebuggingModelController());
    fileTreeSection.getTree().renderTree(0);

    // TODO: The term "Section" is overloaded here. It
    // conflates the NavigationSection (the File Tree and the Conflict List)
    // with panels in the NavigationToolBar. The Collab and Outline Sections
    // here are actually panels on the NavigationToolBar. Clean this up.

    CollaborationSection collabSection = CollaborationSection.create(
        new CollaborationSection.View(res), participantModel, appContext);

    treeNodeMoveController = new FileTreeNodeMoveController(
        appContext, fileTreeSection.getFileTreeUiController(), fileTreeModel);

    OutlineSection outlineSection = OutlineSection.create(
        new OutlineSection.View(res), appContext, outlineModel,
        editorBundle.getOutlineController());

    final WorkspaceNavigationToolBar navigationToolBar = WorkspaceNavigationToolBar.create(
        codePerspectiveView.getNavigationView().getNavigationToolBarView(), collabSection,
        outlineSection);

    WorkspaceNavigation workspaceNavigation = WorkspaceNavigation.create(
        codePerspectiveView.getNavigationView(),
        new WorkspaceNavigationSection[] {fileTreeSection},
        new WorkspaceNavigationSection[] {collabSection, outlineSection}, navigationToolBar);
    navigationToolBar.setWorkspaceNavigation(workspaceNavigation);

    contentArea =
        EditableContentArea.create(codePerspectiveView.getContentView(), appContext, editorBundle);

    CodePerspective codePerspective = CodePerspective.create(
        codePerspectiveView, workspacePlace, workspaceNavigation, contentArea, appContext);

    // Connect views to the DOM.
    Element rightSidebarContentContainer = codePerspectiveView.getSidebarElement();
    rightSidebarContentContainer.appendChild(
        editorBundle.getDebuggingModelController().getDebuggingSidebarElement());
    shell.setPerspective(codePerspectiveView.getElement());

    fileSelectionController = new FileSelectionController(documentManager,
        editorBundle,
        uneditableDisplay,
        fileTreeModel,
        fileTreeSection.getFileTreeUiController(),
        contentArea);

    // Creates the welcome panel
    welcomePanel = NoFileSelectedPanel.create(workspacePlace, appContext.getResources());

    // Register navigation handler for FileSelection.
    FileSelectedPlaceNavigationHandler fileSelectionHandler =
        new FileSelectedPlaceNavigationHandler(appContext,
            codePerspective,
            fileSelectionController,
            contentArea);
    workspacePlace.registerChildHandler(FileSelectedPlace.PLACE, fileSelectionHandler);

    // Register navigation handler for searching within files.
    SearchPlaceNavigationHandler searchHandler = new SearchPlaceNavigationHandler(appContext,
        contentArea, fileTreeSection.getFileTreeUiController(), workspacePlace);
    workspacePlace.registerChildHandler(SearchPlace.PLACE, searchHandler);
  }

  public void enterWorkspace(final PlaceNavigationEvent<? extends Place> navigationEvent,
      final GetWorkspaceMetaDataResponse metadata) {
    if (!navigationEvent.isActiveLeaf()) {
      return;
    }

    // If there are no previously open files, show the welcome
    // panel.
    if (metadata.getLastOpenFiles().size() == 0) {
      showNoFileSelectedPanel();
      return;
    }

    Scheduler.get().scheduleFinally(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        // we check the very first file since that's all we support right now
        String file = metadata.getLastOpenFiles().get(0);
        Preconditions.checkNotNull(file, "Somehow the file to navigate to was null");

        PlaceNavigationEvent<FileSelectedPlace> event =
            FileSelectedPlace.PLACE.createNavigationEvent(new PathUtil(file));
        navigationEvent.getPlace().fireChildPlaceNavigation(event);
      }
    });
  }

  public void showNoFileSelectedPanel() {
    editorBundle.getBreadcrumbs().clearPath();
    contentArea.setContent(welcomePanel, false);
  }

  /**
   * Register's events that the AwesomeBox context uses.
   */
  private void registerAwesomeBoxEvents() {
    // If these fail someone called the registerAwesomeBoxEvents method
    // to early in enterPlace. Lets notify them clearly.
    Preconditions.checkArgument(
        editorBundle != null, "Editor must be created before registering awesome box events.");

    // Register Hiding Listener
    shell.getHeader().getAwesomeBoxComponentHost().getComponentHiddenListener()
        .add(new AwesomeBoxComponentHiddenListener() {
          @Override
          public void onHidden(Reason reason) {
            if (reason != Reason.OTHER) {
              editorBundle.getEditor().getFocusManager().focus();
            }
          }
        });

    // Attach the editor to the editorActionSection
    gotoActionSection.attachEditorAndPlace(workspacePlace, editorBundle.getEditor());

    // We register the file opened events.
    fileNavSection.registerOnFileOpenedHandler(workspacePlace);
    primaryWorkspaceActionsSection.registerOnFileOpenedHandler(workspacePlace);
    workspacePlace.registerSimpleEventHandler(FileOpenedEvent.TYPE, new FileOpenedEvent.Handler() {
      @Override
      public void onFileOpened(boolean isEditable, PathUtil filePath) {
        shell.getHeader().getAwesomeBoxComponentHost().hide();
      }
    });

    // Hook find/replace upto the editor.
    editorBundle.getEditor().getDocumentListenerRegistrar().add(new DocumentListener() {
      @Override
      public void onDocumentChanged(Document oldDocument, Document newDocument) {
        findReplaceComponent.attachEditor(editorBundle.getEditor());
      }
    });

    // Register any hotkeys
    GlobalHotKey.Handler findAndReplaceHandler = new GlobalHotKey.Handler() {
      @Override
      public void onKeyDown(SignalEvent event) {
        if (fileSelectionController.isSelectedFileEditable()) {
          findReplaceComponent.setFindMode(event.getShiftKey() ? FindMode.REPLACE : FindMode.FIND);

          String selectedText = editorBundle.getEditor().getSelection().getSelectedText();
          boolean isFindActive = findReplaceComponent.isActive();
          boolean shouldReplaceIfInFindMode = isFindActive && !selectedText.isEmpty();

          // TODO: Check the AB query once it exists
          // && !abQuery.isEmpty();
          boolean shouldReplaceIfNotInFindMode = !isFindActive && !selectedText.isEmpty();

          if (shouldReplaceIfInFindMode || shouldReplaceIfNotInFindMode) {
            findReplaceComponent.setQuery(selectedText);
          }
          appContext.getAwesomeBoxComponentHostModel().setActiveComponent(findReplaceComponent);
        }
        shell.getHeader().getAwesomeBoxComponentHost().show();
      }
    };
    GlobalHotKey.register(
        appContext.getKeyBindings().localFind(), findAndReplaceHandler, "local find");
    GlobalHotKey.register(
        appContext.getKeyBindings().localReplace(), findAndReplaceHandler, "local replace");
    GlobalHotKey.register(appContext.getKeyBindings().gotoLine(), new GlobalHotKey.Handler() {
      @Override
      public void onKeyDown(SignalEvent event) {
        // TODO: Make a component which handles goto only.
        shell.getHeader().getAwesomeBoxComponentHost().show();
      }
    }, "goto line");
  }
}
