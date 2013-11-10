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

package collide.client.filetree;

import collide.client.common.CanRunApplication;
import collide.client.filetree.FileTreeContextMenuController.ContextMenuMode;
import collide.client.treeview.Tree;
import collide.client.treeview.TreeNodeElement;
import collide.client.treeview.TreeNodeLabelRenamer;

import com.google.collide.client.code.FileSelectedPlace;
import com.google.collide.client.code.debugging.DebuggingModelController;
import com.google.collide.client.communication.ResourceUriUtils;
import com.google.collide.client.history.Place;
import com.google.collide.client.ui.dropdown.DropdownWidgets;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;

import elemental.html.Location;
import elemental.js.html.JsDragEvent;

/**
 * Listens for changes to the model via callbacks from {@link FileTreeModel} and updates the Tree.
 * Similarly, this controller takes events reported from the {@link Tree} (like clicks and
 * selections) and handles them.
 */
public class FileTreeUiController implements FileTreeModel.TreeModelChangeListener {

  /**
   * Static factory method for obtaining an instance of a FileTreeUiController.
   */
  public static FileTreeUiController create(Place place,
      FileTreeModel fileTreeModel,
      Tree<FileTreeNode> tree,
      FileTreeController<?> controller,
      CanRunApplication applicationRunner) {

    // Set the initial root node for the tree. This will simply be null on first
    // load. But if it isn't we should probably still render what ever was in
    // the FileTreeModel.
    tree.getModel().setRoot(fileTreeModel.getWorkspaceRoot());
    TreeNodeLabelRenamer<FileTreeNode> nodeLabelMutator = new TreeNodeLabelRenamer<FileTreeNode>(
        tree.getModel().getNodeRenderer(), tree.getModel().getDataAdapter(),
        controller.getResources().workspaceNavigationFileTreeNodeRendererCss());
    FileTreeUiController treeUiController = new FileTreeUiController(place,
        controller.getResources(),
        fileTreeModel,
        tree,
        nodeLabelMutator,
        controller,
        applicationRunner);
    fileTreeModel.addModelChangeListener(treeUiController);
    treeUiController.attachEventHandlers();
    return treeUiController;
  }

  /** Listener for drag-and-drop events on nodes in the file tree. */
  public interface DragDropListener {
    void onDragStart(FileTreeNode node, JsDragEvent event);

    void onDragDrop(FileTreeNode node, JsDragEvent event);
  }

  private final FileTreeContextMenuController contextMenuController;
  private DragDropListener uploadDragDropListener;
  private DragDropListener treeNodeMoveListener;
  private final FileTreeModel fileTreeModel;
  private final Tree<FileTreeNode> tree;
  private final Place currentPlace;

  FileTreeUiController(Place place,
      DropdownWidgets.Resources res,
      FileTreeModel fileTreeModel,
      Tree<FileTreeNode> tree,
      TreeNodeLabelRenamer<FileTreeNode> nodeLabelMutator,
      FileTreeController<?> controller,
      CanRunApplication applicationRunner) {
    this.currentPlace = place;
    this.fileTreeModel = fileTreeModel;
    this.tree = tree;
    this.contextMenuController = FileTreeContextMenuController.create(place,
        res,
        this,
        fileTreeModel,
        nodeLabelMutator,
        controller,
        applicationRunner);
  }

  /**
   * Programmatically selects a node in the tree. This will cause any external handlers of the
   * {@link Tree} to have their {@link collide.client.treeview.Tree.Listener#onNodeAction(TreeNodeElement)}
   * method get invoked.
   */
  public void autoExpandAndSelectNode(FileTreeNode nodeToSelect, boolean dispatchNodeAction) {
    tree.autoExpandAndSelectNode(nodeToSelect, dispatchNodeAction);
  }

  public void expandNode(TreeNodeElement<FileTreeNode> parentTreeNode) {
    tree.expandNode(parentTreeNode);
  }

  public void clearSelectedNodes() {
    tree.getSelectionModel().clearSelections();
  }

  public FileTreeContextMenuController getContextMenuController() {
    return contextMenuController;
  }

  public Tree<FileTreeNode> getTree() {
    return tree;
  }

  public void setUploadDragDropListener(DragDropListener listener) {
    uploadDragDropListener = listener;
  }

  public void setFileTreeNodeMoveListener(DragDropListener listener) {
    treeNodeMoveListener = listener;
  }

  public void nodeWillBeAdded() {
  }

  /**
   * Called when a node is added to the model. This will re-render the subtree rooted at this nodes
   * parent iff the parent node is already rendered.
   */
  @Override
  public void onNodeAdded(PathUtil parentDirPath, FileTreeNode newNode) {
    FileTreeNode rootNode = tree.getModel().getRoot();
    if (rootNode == null) {
      return;
    }

    if (PathUtil.WORKSPACE_ROOT.getPathString().equals(parentDirPath.getPathString())) {

      // This means that we are adding to the base of the tree and should reRender.
      reRenderSubTree(rootNode);
      return;
    }

    FileTreeNode parentDir = rootNode.findChildNode(parentDirPath);

    if (parentDir != null && parentDir.isComplete()) {

      // Add the node.
      TreeNodeElement<FileTreeNode> parentDirTreeNode = tree.getNode(parentDir);

      if (parentDirTreeNode != null) {
        reRenderSubTree(parentDir);
      }
    }
  }

  @Override
  public void onNodeMoved(
      PathUtil oldPath, FileTreeNode node, PathUtil newPath, FileTreeNode newNode) {
    FileTreeNode rootNode = tree.getModel().getRoot();
    if (rootNode == null) {
      return;
    }

    if (node != null) {
      FileTreeNode oldParent = rootNode.findChildNode(PathUtil.createExcludingLastN(oldPath, 1));
      onNodeRemoved(oldParent, node.getRenderedTreeNode());
      // do not kill the back reference (as in onNodeRemoved)!
    }

    if (newNode != null) {
      PathUtil parentDirPath = PathUtil.createExcludingLastN(newPath, 1);
      onNodeAdded(parentDirPath, newNode);
    }
  }

  @Override
  public void onNodesRemoved(JsonArray<FileTreeNode> oldNodes) {

    FileTreeNode rootNode = tree.getModel().getRoot();
    if (rootNode == null) {
      return;
    }

    for (int i = 0; i < oldNodes.size(); i++) {
      FileTreeNode oldNode = oldNodes.get(i);
      // If we found a node at the specified path, then remove it.
      if (oldNode != null) {
        onNodeRemoved(oldNode.getParent(), oldNode.getRenderedTreeNode());

        // Kill the back reference so we don't leak.
        oldNode.setRenderedTreeNode(null);
      }
    }
  }

  @Override
  public void onNodeReplaced(FileTreeNode oldNode, FileTreeNode newNode) {

    if (!newNode.isDirectory()) {
      // We don't need to do anything with files being replaced
      return;
    }

    if (PathUtil.WORKSPACE_ROOT.getPathString().equals(newNode.getNodePath().getPathString())) {

      // Install the workspace root for the tree and render it. Expansion state
      // should be restored by this method.
      tree.replaceSubtree(tree.getModel().getRoot(), newNode, false);
    } else if (oldNode != null) {
      TreeNodeElement<FileTreeNode> oldRenderedElement = oldNode.getRenderedTreeNode();

      // If the node that we just replaced had a rendered tree node, we need
      // to re-render.
      if (oldRenderedElement != null) {
        // If the node was loading, animate it open now that the children are available.
        tree.replaceSubtree(oldNode, newNode, oldNode.isLoading());

        // Kill the back reference so we don't leak.
        oldNode.setRenderedTreeNode(null);
      }
    }
  }

  /**
   * Re-renders the subtree rooted at the specified {@link FileTreeNode}, ensuring that its direct
   * children are sorted.
   */
  /* TODO : restore selection state */
  public void reRenderSubTree(FileTreeNode parentDir) {
    parentDir.invalidateUnifiedChildrenCache();
    tree.replaceSubtree(parentDir, parentDir, false);
  }

  private void attachEventHandlers() {
    tree.setTreeEventHandler(new Tree.Listener<FileTreeNode>() {

      @Override
      public void onNodeAction(TreeNodeElement<FileTreeNode> node) {
        if (node.getData().isFile()
            && getContextMenuController().getMode() == ContextMenuMode.READY) {
          currentPlace.fireChildPlaceNavigation(
              FileSelectedPlace.PLACE.createNavigationEvent(node.getData().getNodePath()));
        }
      }

      @Override
      public void onNodeClosed(TreeNodeElement<FileTreeNode> node) {}

      @Override
      public void onNodeContextMenu(int mouseX, int mouseY, TreeNodeElement<FileTreeNode> node) {
        getContextMenuController().show(mouseX, mouseY, node);
      }

      @Override
      public void onNodeDragDrop(TreeNodeElement<FileTreeNode> node, JsDragEvent event) {
        if (treeNodeMoveListener != null) {
          treeNodeMoveListener.onDragDrop(node.getData(), event);
        }

        if (uploadDragDropListener != null) {
          uploadDragDropListener.onDragDrop(node.getData(), event);
        }
      }

      @Override
      public void onRootDragDrop(JsDragEvent event) {
        if (treeNodeMoveListener != null) {
          treeNodeMoveListener.onDragDrop(fileTreeModel.getWorkspaceRoot(), event);
        }

        if (uploadDragDropListener != null) {
          uploadDragDropListener.onDragDrop(fileTreeModel.getWorkspaceRoot(), event);
        }
      }

      @Override
      public void onNodeDragStart(TreeNodeElement<FileTreeNode> node, JsDragEvent event) {
        // When drag starts in tree, we do not know if users want to drop it
        // outside tree or in tree. So, we prepare for both cases.
        prepareForDraggingToOutside(node, event);
        if (treeNodeMoveListener != null) {
          treeNodeMoveListener.onDragStart(node.getData(), event);
        }

        if (uploadDragDropListener != null) {
          uploadDragDropListener.onDragStart(node.getData(), event);
        }
      }

      @Override
      public void onNodeExpanded(TreeNodeElement<FileTreeNode> node) {
        if (!node.getData().isComplete() && !node.getData().isLoading()) {
          // Mark the node as loading.
          node.getData().setLoading(true);
          tree.getModel().getNodeRenderer().updateNodeContents(node);

          // Load the children of the directory.
          fileTreeModel.requestDirectoryChildren(node.getData(), null);
        }
      }

      @Override
      public void onRootContextMenu(int mouseX, int mouseY) {
        getContextMenuController().show(mouseX, mouseY, null);
      }

      private void prepareForDraggingToOutside(
          TreeNodeElement<FileTreeNode> node, JsDragEvent event) {
        FileTreeNode fileTreeNode = node.getData();
        PathUtil nodePath = fileTreeNode.getNodePath();

        String downloadFileName = fileTreeNode.isDirectory() ? nodePath.getBaseName() + ".zip"
            : nodePath.getBaseName();

        Location location = elemental.client.Browser.getWindow().getLocation();
        String urlHttpHostPort = location.getProtocol() + "//" + location.getHost();

        String downloadUrl = "application/octet-stream:" + downloadFileName + ":"
            + ResourceUriUtils.getAbsoluteResourceUri(nodePath)
            + (fileTreeNode.isDirectory() ? "?rt=zip" : "");
        event.getDataTransfer().setData("DownloadURL", downloadUrl);
      }
    });
  }

  private void onNodeRemoved(FileTreeNode oldParent, TreeNodeElement<FileTreeNode> oldNode) {
    tree.removeNode(oldNode);

    // Make sure to remove expansion controls from any directory that might
    // now be empty
    if (oldParent != null && oldParent.getRenderedTreeNode() != null) {
      if (oldParent.getUnifiedChildren().isEmpty()) {
        oldParent.getRenderedTreeNode().makeLeafNode(tree.getResources().treeCss());
      }
    }
  }
}
