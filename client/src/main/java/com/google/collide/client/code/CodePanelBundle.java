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

import collide.client.filetree.FileTreeController;
import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeNodeMoveController;

import com.google.collide.client.AppContext;
import com.google.collide.client.CollideSettings;
import com.google.collide.client.code.CodePerspective.Resources;
import com.google.collide.client.code.CodePerspective.View;
import com.google.collide.client.code.FileSelectionController.FileOpenedEvent;
import com.google.collide.client.code.errorrenderer.EditorErrorListener;
import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.editor.Editor.DocumentListener;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.plugin.ClientPluginService;
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
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.dom.eventcapture.GlobalHotKey;
import com.google.collide.client.util.logging.Log;
import com.google.collide.client.workspace.WorkspaceShell;
import com.google.collide.client.workspace.outline.OutlineModel;
import com.google.collide.client.workspace.outline.OutlineSection;
import com.google.collide.dto.GetWorkspaceMetaDataResponse;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;

import elemental.dom.Element;

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
  public EditableContentArea contentArea;
  private CodePerspective codePerspective;
  private final FileNameNavigationSection fileNavSection;
  private final PrimaryWorkspaceActionSection primaryWorkspaceActionsSection;
  private final GotoActionSection gotoActionSection;
  private final OutlineViewAwesomeBoxSection outlineViewAwesomeBoxSection;
  private final Place currentPlace;


  // AwesomeBox Components
  private final FindReplaceComponent findReplaceComponent;

  // Presenters and Controllers that require cleanup.
  private EditorBundle editorBundle;
  private EditorReloadingFileTreeListener editorReloadingFileTreeListener;
  private NoFileSelectedPanel welcomePanel;
  private FileSelectionController fileSelectionController;
  private FileTreeNodeMoveController treeNodeMoveController;
  private FileTreeSection fileTreeSection;
  private ClientPluginService plugins;
  private MultiPanel<?,?> masterPanel;
  private final FileTreeController<?> fileTreeController;

  public CodePanelBundle(AppContext appContext,
      WorkspaceShell shell,
      FileTreeController<?> fileTreeController,
      FileTreeModel fileTreeModel,
      FileNameSearch searchIndex,
      DocumentManager documentManager,
      ParticipantModel participantModel,
      IncomingDocOpDemultiplexer docOpReceiver,
      Place place) {
    this.appContext = appContext;
    this.shell = shell;
    this.fileTreeController = fileTreeController;
    this.fileTreeModel = fileTreeModel;
    this.searchIndex = searchIndex;
    this.documentManager = documentManager;
    this.participantModel = participantModel;
    this.docOpReceiver = docOpReceiver;
    this.currentPlace = place;

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
    plugins.cleanup();
    if (masterPanel != null)
      masterPanel.destroy();
    GlobalHotKey.unregister(appContext.getKeyBindings().localFind());
    GlobalHotKey.unregister(appContext.getKeyBindings().localReplace());
    GlobalHotKey.unregister(appContext.getKeyBindings().gotoLine());
    GlobalHotKey.unregister(appContext.getKeyBindings().snapshot());
  }

  public void attach(boolean detached) {
    // Construct the Root View for the CodePerspective.
    CodePerspective.Resources res = appContext.getResources();
    CodePerspective.View codePerspectiveView = initCodePerspective(res, detached);

    // Then create all the Presenters.
    OutlineModel outlineModel = new OutlineModel();
    editorBundle = EditorBundle.create(appContext,
        currentPlace,
        documentManager,
        participantModel,
        outlineModel,
        fileTreeModel,
        EditorErrorListener.NOOP_ERROR_RECEIVER);


    // AwesomeBox Section Setup
    appContext.getAwesomeBoxModel().changeContext(awesomeBoxCodeContext);
    outlineViewAwesomeBoxSection.setOutlineModelAndEditor(outlineModel, editorBundle.getEditor());
    registerAwesomeBoxEvents();

    editorReloadingFileTreeListener =
        EditorReloadingFileTreeListener.create(currentPlace, editorBundle, fileTreeModel);

    fileTreeSection = FileTreeSection.create(
        currentPlace, fileTreeController, fileTreeModel, editorBundle.getDebuggingModelController());
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

    contentArea = initContentArea(codePerspectiveView, appContext, editorBundle,fileTreeSection);

    codePerspective = CodePerspective.create(
        codePerspectiveView, currentPlace, workspaceNavigation, contentArea, appContext, detached);

    // Connect views to the DOM.
    Element rightSidebarContentContainer = codePerspectiveView.getSidebarElement();
    rightSidebarContentContainer.appendChild(
        editorBundle.getDebuggingModelController().getDebuggingSidebarElement());

    attachShellToDom(shell, codePerspectiveView);

  }


  protected View initCodePerspective(Resources res, boolean detached) {
    return new CodePerspective.View(res, detached);
  }

  protected EditableContentArea initContentArea(View codePerspectiveView,
      AppContext appContext, EditorBundle editorBundle,
      FileTreeSection fileTreeSection) {
    return EditableContentArea.create(codePerspectiveView.getContentView(), appContext, editorBundle,
        fileTreeSection.getFileTreeUiController());
  }

  protected void attachShellToDom(WorkspaceShell shell, View codePerspectiveView) {
    shell.setPerspective(codePerspectiveView.getElement());
  }

  public void enterWorkspace(boolean isActiveLeaf, final Place place,
      final GetWorkspaceMetaDataResponse metadata) {
    if (!isActiveLeaf) {
      return;
    }

    // If there are no previously open files, show the welcome panel.
    JsonArray<String> open = metadata.getLastOpenFiles();
    String openFile = CollideSettings.get().getOpenFile();
    Log.info(getClass(), "Opening ",open);
    if (openFile != null){
      open = JsoArray.create();
      open.add(openFile);
    }
    if (open.size() == 0) {
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
        place.fireChildPlaceNavigation(event);
      }
    });
  }

  public void showNoFileSelectedPanel() {
    editorBundle.getBreadcrumbs().clearPath();
    contentArea.setContent(welcomePanel, contentArea.newBuilder().setHistoryIcon(false).build());
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
    gotoActionSection.attachEditorAndPlace(currentPlace, editorBundle.getEditor());

    // We register the file opened events.
    fileNavSection.registerOnFileOpenedHandler(currentPlace);
    primaryWorkspaceActionsSection.registerOnFileOpenedHandler(currentPlace);
    currentPlace.registerSimpleEventHandler(FileOpenedEvent.TYPE, new FileOpenedEvent.Handler() {
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

  public void setMasterPanel(MultiPanel<?,?> masterPanel) {
    this.masterPanel = masterPanel;


    UneditableDisplay uneditableDisplay = UneditableDisplay.create(new UneditableDisplay.View(appContext.getResources()));

    fileSelectionController = new FileSelectionController(documentManager,
        editorBundle,
        uneditableDisplay,
        fileTreeModel,
        fileTreeSection.getFileTreeUiController(),
        masterPanel);

    // Creates the welcome panel
    welcomePanel = NoFileSelectedPanel.create(currentPlace, appContext.getResources());

    // Register navigation handler for FileSelection.
    FileSelectedPlaceNavigationHandler fileSelectionHandler =
        new FileSelectedPlaceNavigationHandler(appContext,
            codePerspective,
            fileSelectionController,
            contentArea);
    currentPlace.registerChildHandler(FileSelectedPlace.PLACE, fileSelectionHandler);

    // Register navigation handler for searching within files.
    SearchPlaceNavigationHandler searchHandler = new SearchPlaceNavigationHandler(appContext,
        masterPanel, fileTreeSection.getFileTreeUiController(), currentPlace);
    currentPlace.registerChildHandler(SearchPlace.PLACE, searchHandler);

    plugins = initPlugins(masterPanel);
  }

  protected ClientPluginService initPlugins(MultiPanel<?, ?> masterPanel) {
    return ClientPluginService.initialize(appContext, masterPanel, currentPlace);
  }
}
