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

import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.document.DocumentMetadata;
import com.google.collide.client.document.DocumentManager.GetDocumentCallback;
import com.google.collide.client.history.RootPlace;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.FileTreeModel;
import com.google.collide.client.workspace.FileTreeNode;
import com.google.collide.client.workspace.FileTreeUiController;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.client.workspace.FileTreeModel.NodeRequestCallback;
import com.google.collide.dto.FileContents;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.json.client.JsoArray;
import com.google.collide.shared.document.Document;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Handler for file selection that drives file contents and the file tree selection.
 *
 */
public class FileSelectionController implements GetDocumentCallback {

  /**
   * Event that broadcasts that a file's contents have been received over the network and we have
   * determined if it is editable.
   *
   */
  public static class FileOpenedEvent extends GwtEvent<FileOpenedEvent.Handler> {

    public interface Handler extends EventHandler {
      public void onFileOpened(boolean isEditable, PathUtil filePath);
    }

    public static final Type<Handler> TYPE = new Type<Handler>();
    private final boolean isFileEditable;
    private final PathUtil filePath;

    public FileOpenedEvent(boolean isEditable, PathUtil filePath) {
      this.isFileEditable = isEditable;
      this.filePath = filePath;
    }

    @Override
    protected void dispatch(Handler handler) {
      handler.onFileOpened(isFileEditable, filePath);
    }

    public boolean isEditable() {
      return isFileEditable;
    }

    public PathUtil getFilePath() {
      return filePath;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
      return TYPE;
    }
  }

  private FileSelectedPlace.NavigationEvent mostRecentNavigationEvent;
  private boolean isSelectedFileEditable = false;
  private final FileTreeUiController treeUiController;
  private final FileTreeModel fileTreeModel;
  private final EditableContentArea contentArea;
  private final EditorBundle editorBundle;
  private final UneditableDisplay uneditableDisplay;
  private final DocumentManager documentManager;

  public FileSelectionController(DocumentManager documentManager,
      EditorBundle editorBundle,
      UneditableDisplay uneditableDisplay,
      FileTreeModel fileTreeModel,
      FileTreeUiController treeUiController,
      EditableContentArea contentArea) {
    this.documentManager = documentManager;
    this.editorBundle = editorBundle;
    this.uneditableDisplay = uneditableDisplay;
    this.fileTreeModel = fileTreeModel;
    this.treeUiController = treeUiController;
    this.contentArea = contentArea;
  }

  /**
   * Deselects the currently selected file, if one is selected.
   */
  public void deselectFile() {
    mostRecentNavigationEvent = null;
    treeUiController.clearSelectedNodes();
  }

  public void selectFile(FileSelectedPlace.NavigationEvent navigationEvent) {
    // The root is a special case no-op.
    if (!navigationEvent.getPath().equals(PathUtil.WORKSPACE_ROOT)) {
      doSelectTreeNode(navigationEvent);
    }
  }

  public boolean isSelectedFileEditable() {
    return isSelectedFileEditable;
  }

  FileTreeModel getFileTreeModel() {
    return fileTreeModel;
  }

  private void doSelectTreeNode(final FileSelectedPlace.NavigationEvent navigationEvent) {
    mostRecentNavigationEvent = navigationEvent;

    fileTreeModel.requestWorkspaceNode(navigationEvent.getPath(), new NodeRequestCallback() {

      @Override
      public void onNodeAvailable(FileTreeNode node) {

        // If we have since navigated away, exit early and don't select the
        // file.
        if (mostRecentNavigationEvent != navigationEvent
            || !mostRecentNavigationEvent.isActiveLeaf()) {
          return;
        }

        // Expand the tree to reveal the node if it happens to be hidden (in the
        // case of deep linking)
        TreeNodeElement<FileTreeNode> renderedElement = node.getRenderedTreeNode();
        if (renderedElement == null
            || !renderedElement.isActive(treeUiController.getTree().getResources().treeCss())) {

          // Select the node without dispatching the node selection action.
          treeUiController.autoExpandAndSelectNode(node, false);
        }
      }

      @Override
      public void onNodeUnavailable() {
        // This can happen if a history event is dispatched for a path not
        // found in our file tree.

        // TODO: Throw up some UI to show that no such file exists.
        // For now simply ignore the selection.
      }

      @Override
      public void onError(FailureReason reason) {
        // Already logged by the FileTreeModel.
      }
    });

    documentManager.getDocument(navigationEvent.getPath(), this);
  }

  @Override
  public void onDocumentReceived(Document document) {
    PathUtil path = DocumentMetadata.getPath(document);

    if (mostRecentNavigationEvent == null || !mostRecentNavigationEvent.isActiveLeaf()
        || !path.equals(mostRecentNavigationEvent.getPath())) {
      // User selected another file or navigated away since this request, ignore
      return;
    }

    openDocument(document, path);
    WorkspacePlace.PLACE.fireEvent(new FileOpenedEvent(true, path));
  }

  private void openDocument(final Document document, PathUtil path) {
    isSelectedFileEditable = true;

    contentArea.setContent(editorBundle);

    // Note that we dont use the name from the incoming FileContents. They
    // should generally be the same. But in the case of a rename or a move, they
    // might not be. Those changes get propagated separately, so we stick with
    // the client's view of the world wrt to naming.
    editorBundle.setDocument(document, path, DocumentMetadata.getFileEditSessionKey(document));

    if (mostRecentNavigationEvent.getLineNo()
        != FileSelectedPlace.NavigationEvent.IGNORE_LINE_NUMBER) {
      /*
       * TODO: This scheduled deferred is so we set scroll AFTER the selection
       * restorer. After demo, I'll create a better API on editor for components that want to set
       * initial selection/scroll.
       */
      final FileSelectedPlace.NavigationEvent savedNavigationEvent = mostRecentNavigationEvent;
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          if (mostRecentNavigationEvent == savedNavigationEvent) {
            editorBundle.getEditor().scrollTo(mostRecentNavigationEvent.getLineNo(),
                Math.max(0, mostRecentNavigationEvent.getColumn()));
          }
        }
      });
    }

    // Set the tab title to the current open file
    Elements.setCollideTitle(path.getBaseName());

    editorBundle.getEditor().getFocusManager().focus();

    // Save the list of open documents.
    // TODO: Send a list of files when we support tabs.
    JsoArray<String> openFiles = JsoArray.create();
    openFiles.add(path.getPathString());
  }

  @Override
  public void onUneditableFileContentsReceived(FileContents uneditableFile) {
    showDisplayOnly(uneditableFile);
    WorkspacePlace.PLACE.fireEvent(
        new FileOpenedEvent(false, new PathUtil(uneditableFile.getPath())));
  }

  /**
   * Changes the display for an uneditable file.
   */
  private void showDisplayOnly(FileContents uneditableFile) {
    PathUtil filePath = new PathUtil(uneditableFile.getPath());
    if (!filePath.equals(mostRecentNavigationEvent.getPath())) {
      // User selected another file since this request, ignore
      return;
    }

    isSelectedFileEditable = false;

    // Set the tab title to the current open file
    Elements.setCollideTitle(filePath.getBaseName());

    contentArea.setContent(uneditableDisplay);
    uneditableDisplay.displayUneditableFileContents(uneditableFile);
    editorBundle.getBreadcrumbs().setPath(filePath);
  }

  @Override
  public void onFileNotFoundReceived() {
    // TODO: pretty file not found message
    RootPlace.PLACE.fireChildPlaceNavigation(
        WorkspacePlace.PLACE.createNavigationEvent());
  }
}
