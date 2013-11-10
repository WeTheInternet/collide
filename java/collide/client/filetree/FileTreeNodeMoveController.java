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

import collide.client.filetree.FileTreeUiController.DragDropListener;
import collide.client.treeview.SelectionModel;

import com.google.collide.client.AppContext;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.WorkspaceReadOnlyChangedEvent;
import com.google.collide.client.workspace.WorkspaceReadOnlyChangedEvent.Handler;
import com.google.collide.dto.EmptyMessage;
import com.google.collide.dto.Mutation;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.WorkspaceTreeUpdate;
import com.google.collide.dto.client.DtoClientImpls.DirInfoImpl;
import com.google.collide.json.client.JsoArray;
import com.google.common.annotations.VisibleForTesting;

import elemental.js.html.JsDragEvent;

/**
 * A controller to manage the in-tree drag and drop.
 * <p>
 * Dragging started outside file tree is not handled here.
 */
public class FileTreeNodeMoveController implements WorkspaceReadOnlyChangedEvent.Handler {

  // TODO: Change to custom format (such as
  // "application/collide-nodes-move-started") once Chrome supports it.
  private static final String MOVE_START_INDICATOR_FORMAT = "text/plain";
  private static final String MOVE_START_INDICATOR = "NODES_MOVE_STARTED";
  private final FileTreeUiController fileTreeUiController;
  private boolean isReadOnly;
  private final JsoArray<FileTreeNode> nodesToMove = JsoArray.create();
  private final AppContext appContext;
  private final FileTreeModel fileTreeModel;

  public FileTreeNodeMoveController(AppContext appContext,
      FileTreeUiController fileTreeUiController, FileTreeModel fileTreeModel) {
    this.appContext = appContext;
    this.fileTreeUiController = fileTreeUiController;
    this.fileTreeModel = fileTreeModel;
    attachEventHandlers();
  }

  private void attachEventHandlers() {
    if (fileTreeUiController == null) {
      return;
    }
    fileTreeUiController.setFileTreeNodeMoveListener(new DragDropListener() {

      @Override
      public void onDragDrop(FileTreeNode node, JsDragEvent event) {
        event.getDataTransfer().clearData(MOVE_START_INDICATOR_FORMAT);
        if (isReadOnly || !wasDragInTree(event)) {
          return;
        }
        handleMove(node);
        nodesToMove.clear();
      }

      @Override
      public void onDragStart(FileTreeNode node, JsDragEvent event) {
        // Save the selected nodes. Users may drop them in tree.
        saveSelectedNodesOrParam(node);
        // TODO: once Chrome supports dataTransfer.addElement, add
        // nodesToMove to provide move feedback.
        event.getDataTransfer().setData(MOVE_START_INDICATOR_FORMAT, MOVE_START_INDICATOR);
      }
    });
  }

  @Override
  public void onWorkspaceReadOnlyChanged(WorkspaceReadOnlyChangedEvent event) {
    isReadOnly = event.isReadOnly();
  }

  public static boolean wasDragInTree(JsDragEvent event) {
    return MOVE_START_INDICATOR.equals(
        event.getDataTransfer().getData(MOVE_START_INDICATOR_FORMAT));
  }

  private void saveSelectedNodesOrParam(FileTreeNode node) {
    nodesToMove.clear();

    SelectionModel<FileTreeNode> selectionModel =
        fileTreeUiController.getTree().getSelectionModel();
    JsoArray<FileTreeNode> selectedNodes = selectionModel.getSelectedNodes();
    if (selectedNodes.contains(node)) {
      // Drag is starting from one of the selected nodes.
      // We move all selected nodes.
      nodesToMove.addAll(selectedNodes);
    } else {
      // Drag is starting outside all selected nodes.
      // We only move the drag-start-node.
      nodesToMove.add(node);
    }
  }

  /**
   * For test only.
   */
  @VisibleForTesting
  void setNodesToMove(JsoArray<FileTreeNode> nodesToMove) {
    this.nodesToMove.clear();
    this.nodesToMove.addAll(nodesToMove);
  }

  @VisibleForTesting
  boolean isMoveAllowed(FileTreeNode parentDirData) {
    // File should not be moved to its original place, i.e., /a/b/1.js ==> under
    // /a/b
    // Folder should not be moved to its original place and to under itself or
    // under any of its subfolders, i.e., /a/b/c ==> under /a/b, under /a/b/c,
    // under /a/b/c/d.
    for (int i = 0, n = nodesToMove.size(); i < n; i++) {
      FileTreeNode nodeToMove = nodesToMove.get(i);
      if (nodeToMove.isFile()) {
        if (nodeToMove.getParent().getNodePath().equals(parentDirData.getNodePath())) {
          return false;
        }
      } else {
        // is folder.
        // source folder won't be root, so it has parent.
        if (nodeToMove.getParent().getNodePath().equals(parentDirData.getNodePath())) {
          return false;
        }

        if (nodeToMove.getNodePath().containsPath(parentDirData.getNodePath())) {
          return false;
        }
      }
    }
    return true;
  }

  private void handleMove(FileTreeNode parentDirData) {
    if (nodesToMove.isEmpty()) {
      return;
    }

    // Check each selected node to make sure it can be moved.
    if (!isMoveAllowed(parentDirData)) {
      return;
    }

    WorkspaceTreeUpdate msg = fileTreeModel.makeEmptyTreeUpdate();
    for (int i = 0, n = nodesToMove.size(); i < n; i++) {
      FileTreeNode nodeToMove = nodesToMove.get(i);
      PathUtil targetPath = new PathUtil.Builder().addPath(parentDirData.getNodePath())
          .addPathComponent(FileTreeUtils.allocateName(
              parentDirData.<DirInfoImpl>cast(), nodeToMove.getName())).build();
      msg.getMutations().add(FileTreeUtils.makeMutation(Mutation.Type.MOVE,
          nodeToMove.getNodePath(), targetPath, nodeToMove.isDirectory(),
          nodeToMove.getFileEditSessionKey()));
    }

    appContext.getFrontendApi().MUTATE_WORKSPACE_TREE.send(
        msg, new ApiCallback<EmptyMessage>() {

            @Override
          public void onMessageReceived(EmptyMessage message) {
            // Do nothing. We lean on the multicasted MOVE to have the action
            // update our local model.
          }

            @Override
            public void onFail(FailureReason reason) {
              // Do nothing.
            }
        });
  }

  public void cleanup() {
    fileTreeUiController.setFileTreeNodeMoveListener(null);
  }
}
