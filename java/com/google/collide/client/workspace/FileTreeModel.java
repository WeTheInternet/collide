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

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.logging.Log;
import com.google.collide.client.workspace.FileTreeModelNetworkController.OutgoingController;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.Mutation;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.WorkspaceTreeUpdate;
import com.google.collide.dto.client.DtoClientImpls.DirInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.WorkspaceTreeUpdateImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * Public API for interacting with the client side workspace file tree model.
 * Also exposes callbacks for mutations that have been applied to the model.
 * 
 * If you want to mutate the workspace file tree, which is a tree of
 * {@link FileTreeNode}'s you need to go through here.
 */
public class FileTreeModel {

  /**
   * Callback interface for requesting the root node, potentially
   * asynchronously.
   */
  public interface RootNodeRequestCallback {
    void onRootNodeAvailable(FileTreeNode root);
  }

  /**
   * Callback interface for requesting a node, potentially asynchronously.
   */
  public interface NodeRequestCallback {
    void onNodeAvailable(FileTreeNode node);

    /**
     * Called if the node does not exist.
     */
    void onNodeUnavailable();

    /**
     * Called if an error occurs while loading the node.
     */
    void onError(FailureReason reason);
  }

  /**
   * Callback interface for getting notified about changes to the workspace tree
   * model that have been applied by the FileTreeController.
   */
  public interface TreeModelChangeListener {

    /**
     * Notification that a node was added.
     */
    void onNodeAdded(PathUtil parentDirPath, FileTreeNode newNode);

    /**
     * Notification that a node was moved/renamed.
     *
     * @param oldPath the old node path
     * @param node the node that was moved, or null if the old path is not loaded. If both the old
     *        path and the new path are loaded, node == newNode and node's parent will be the target
     *        directory of the new path. If the new path is not loaded, node is the node that was in
     *        the old path.
     * @param newPath the new node path
     * @param newNode the new node, or null if the target directory is not loaded
     */
    void onNodeMoved(PathUtil oldPath, FileTreeNode node, PathUtil newPath, FileTreeNode newNode);

    /**
     * Notification that a set of nodes was removed.
     *
     * @param oldNodes a list of nodes that we removed. Every node will still have its parent filled
     */
    void onNodesRemoved(JsonArray<FileTreeNode> oldNodes);

    /**
     * Notification that a node was replaced (can be either a file or directory).
     *
     * @param oldNode the existing node that used to be in the file tree, or null if the workspace
     *        root is being set for the first time
     * @param newNode the node that replaces the {@code oldNode}. This will be the same
     *        {@link FileTreeNode#getNodeType()} as the node it is replacing.
     */
    void onNodeReplaced(@Nullable FileTreeNode oldNode, FileTreeNode newNode);
  }

  /**
   * A {@link TreeModelChangeListener} which does not perform any operations in
   * response to an event. Its only purpose is to allow clients to only override
   * the events matter to them.
   */
  public abstract static class AbstractTreeModelChangeListener implements TreeModelChangeListener {
    @Override
    public void onNodeAdded(PathUtil parentDirPath, FileTreeNode newNode) {
      // intentional no-op, clients should override if needed
    }

    @Override
    public void onNodeMoved(
        PathUtil oldPath, FileTreeNode node, PathUtil newPath, FileTreeNode newNode) {
      // intentional no-op, clients should override if needed
    }

    @Override
    public void onNodesRemoved(JsonArray<FileTreeNode> oldNodes) {
      // intentional no-op, clients should override if needed
    }

    @Override
    public void onNodeReplaced(FileTreeNode oldDir, FileTreeNode newDir) {
      // intentional no-op, clients should override if needed
    }
  }

  /**
   * A {@link TreeModelChangeListener} that performs the exact same action in
   * response to any and all tree mutations.
   */
  public abstract static class BasicTreeModelChangeListener implements TreeModelChangeListener {
    public abstract void onTreeModelChange();

    @Override
    public void onNodeAdded(PathUtil parentDirPath, FileTreeNode newNode) {
      onTreeModelChange();
    }

    @Override
    public void onNodeMoved(
        PathUtil oldPath, FileTreeNode node, PathUtil newPath, FileTreeNode newNode) {
      onTreeModelChange();
    }

    @Override
    public void onNodesRemoved(JsonArray<FileTreeNode> oldNodes) {
      onTreeModelChange();
    }

    @Override
    public void onNodeReplaced(FileTreeNode oldDir, FileTreeNode newDir) {
      onTreeModelChange();
    }
  }

  private interface ChangeDispatcher {
    void dispatch(TreeModelChangeListener changeListener);
  }

  private final JsoArray<TreeModelChangeListener> modelChangeListeners = JsoArray.create();
  private final OutgoingController outgoingNetworkController;
  
  private FileTreeNode workspaceRoot;
  private boolean disableChangeNotifications;

  /**
   * Tree revision that corresponds to the revision of the last
   * successfully applied tree mutation that this client is aware of.
   */
  private String lastAppliedTreeMutationRevision = "0";

  public FileTreeModel(
      FileTreeModelNetworkController.OutgoingController outgoingNetworkController) {
    this.outgoingNetworkController = outgoingNetworkController;
  }

  /**
   * Adds a node to our model by path.
   */
  public void addNode(PathUtil path, final FileTreeNode newNode, String workspaceRootId) {
    if (workspaceRoot == null) {
      // TODO: queue up this add?
      Log.warn(getClass(), "Attempting to add a node before the root is set", path);
      return;
    }

    // Find the parent directory of the node.
    final PathUtil parentDirPath = PathUtil.createExcludingLastN(path, 1);
    FileTreeNode parentDir = getWorkspaceRoot().findChildNode(parentDirPath);

    if (parentDir != null && parentDir.isComplete()) {
      // The parent directory is complete, so add the node.
      addNode(parentDir, newNode, workspaceRootId);
    } else {
      // The parent directory isn't complete, so do not add the node to the model, but update the
      // workspace root id.
      maybeSetLastAppliedTreeMutationRevision(workspaceRootId);
    }
  }

  /**
   * Adds a node to the model under the specified parent node.
   */
  public void addNode(FileTreeNode parentDir, FileTreeNode childNode, String workspaceRootId) {
    addNodeNoDispatch(parentDir, childNode);
    dispatchAddNode(parentDir, childNode, workspaceRootId);
  }

  private void addNodeNoDispatch(final FileTreeNode parentDir, final FileTreeNode childNode) {
    if (parentDir == null) {
      Log.error(getClass(), "Trying to add a child to a null parent!", childNode);
      return;
    }

    Log.debug(getClass(), "Adding ", childNode, " - to - ", parentDir);

    parentDir.addChild(childNode);
  }

  /**
   * Manually dispatch that a node was added.
   */
  void dispatchAddNode(
      final FileTreeNode parentDir, final FileTreeNode childNode, final String workspaceRootId) {
    dispatchModelChange(new ChangeDispatcher() {
      @Override
      public void dispatch(TreeModelChangeListener changeListener) {
        changeListener.onNodeAdded(parentDir.getNodePath(), childNode);
      }
    }, workspaceRootId);
  }

  /**
   * Moves/renames a node in the model.
   */
  public void moveNode(
      final PathUtil oldPath, final PathUtil newPath, final String workspaceRootId) {
    if (workspaceRoot == null) {
      // TODO: queue up this move?
      Log.warn(getClass(), "Attempting to move a node before the root is set", oldPath);
      return;
    }

    // Remove the node from its old path if the old directory is complete.
    final FileTreeNode oldNode = workspaceRoot.findChildNode(oldPath);
    if (oldNode == null) {
      /*
       * No node found at the old path - either it isn't loaded, or we optimistically updated
       * already. Verify that one of those is the case.
       */
      Preconditions.checkState(workspaceRoot.findClosestChildNode(oldPath) != null ||
          workspaceRoot.findChildNode(newPath) != null);
    } else {
      oldNode.setName(newPath.getBaseName());
      oldNode.getParent().removeChild(oldNode);
    }

    // Apply the new root id.
    maybeSetLastAppliedTreeMutationRevision(workspaceRootId);

    // Prepare a callback that will dispatch the onNodeMove event to listeners.    
    NodeRequestCallback callback = new NodeRequestCallback() {
      @Override
      public void onNodeAvailable(FileTreeNode newNode) {
        /*
         * If we had to request the target directory, replace the target node with the oldNode to
         * ensure that all properties (such as the rendered node and the fileEditSessionKey) are
         * copied over correctly.
         */
        if (oldNode != null && newNode != null && newNode != oldNode) {
          newNode.replaceWith(oldNode);
          newNode = oldNode;
        }

        // Dispatch a change event.
        final FileTreeNode finalNewNode = newNode;
        dispatchModelChange(new ChangeDispatcher() {
          @Override
          public void dispatch(TreeModelChangeListener changeListener) {
            changeListener.onNodeMoved(oldPath, oldNode, newPath, finalNewNode);
          }
        }, workspaceRootId);
      }

      @Override
      public void onNodeUnavailable() {
        // The node should be available because we are requesting the node using the root ID
        // immediately after the move.
        Log.error(getClass(),
            "Could not find moved node using the workspace root ID immediately after the move");        
      }

      @Override
      public void onError(FailureReason reason) {
        // Error already logged.
      }
    };

    // Request the target directory. 
    final PathUtil parentDirPath = PathUtil.createExcludingLastN(newPath, 1);
    FileTreeNode parentDir = workspaceRoot.findChildNode(parentDirPath);
    if (parentDir == null || !parentDir.isComplete()) {
      if (oldNode == null) {
        // Early exit if neither the old node nor the target directory is loaded.        
        return;
      } else {
        // If the parent directory was not loaded, don't bother loading it.
        callback.onNodeAvailable(null);
      }
    } else {
      if (oldNode == null) {
        // The old node doesn't exist, so we need to force a refresh of the target directory's
        // children by marking the target directory incomplete.
        DirInfoImpl parentDirView = parentDir.cast();
        parentDirView.setIsComplete(false);
      } else {
        // The old node exists and the target directory is loaded, so add the node to the target.
        parentDir.addChild(oldNode);
      }

      // Request the new node.
      requestWorkspaceNode(newPath, callback);
    }
  }

  /**
   * Removes a node from the model.
   * 
   * @param toDelete the {@link FileTreeNode} we want to remove.
   * @param workspaceRootId the new file tree revision
   * @return the node that was deleted from the model. This will return
   *         {@code null} if the input node is null or if the input node does
   *         not have a parent. Meaning if the input node is the root, this
   *         method will return {@code null}.
   */
  public FileTreeNode removeNode(final FileTreeNode toDelete, String workspaceRootId) {
    // If we found a node at the specified path, then remove it.
    if (deleteNodeNoDispatch(toDelete)) {
      final JsonArray<FileTreeNode> deletedNode = JsonCollections.createArray(toDelete);
      dispatchModelChange(new ChangeDispatcher() {
        @Override
        public void dispatch(TreeModelChangeListener changeListener) {
          changeListener.onNodesRemoved(deletedNode);
        }
      }, workspaceRootId);

      return toDelete;
    }

    return null;
  }

  /**
   * Removes a set of nodes from the model.
   * 
   * @param toDelete the {@link PathUtil}s for the nodes we want to remove.
   * @param workspaceRootId the new file tree revision
   * @return the nodes that were deleted from the model. This will return an
   *         empty list if we try to add a node before we have a root node set,
   *         or if the specified path does not exist..
   */
  public JsonArray<FileTreeNode> removeNodes(
      final JsonArray<PathUtil> toDelete, String workspaceRootId) {
    if (workspaceRoot == null) {
      // TODO: queue up this remove?
      Log.warn(getClass(), "Attempting to remove nodes before the root is set");
      return null;
    }

    final JsonArray<FileTreeNode> deletedNodes = JsonCollections.createArray();
    for (int i = 0; i < toDelete.size(); i++) {
      FileTreeNode node = workspaceRoot.findChildNode(toDelete.get(i));
      if (deleteNodeNoDispatch(node)) {
        deletedNodes.add(node);
      }
    }

    if (deletedNodes.size() == 0) {
      // if none of the nodes created a need to update the UI, just return an
      // empty list.
      return deletedNodes;
    }

    dispatchModelChange(new ChangeDispatcher() {
      @Override
      public void dispatch(TreeModelChangeListener changeListener) {
        changeListener.onNodesRemoved(deletedNodes);
      }
    }, workspaceRootId);

    return deletedNodes;
  }

  /**
   * Deletes a single node (does not update the UI).
   */
  private boolean deleteNodeNoDispatch(FileTreeNode node) {
    if (node == null || node.getParent() == null) {
      return false;
    }
    
    FileTreeNode parent = node.getParent();

    // Guard against someone installing a node of the same name in the parent
    // (meaning we are already gone.
    if (!node.equals(parent.getChildNode(node.getName()))) {

      // This means that the node we are removing from the tree is already
      // effectively removed from where it thinks it is.
      return false;
    }

    node.getParent().removeChild(node);
    return true;
  }

  /**
   * Replaces either the root node for this tree model, or replaces an existing directory node, or
   * replaces an existing file node.
   */
  public void replaceNode(PathUtil path, final FileTreeNode newNode, String workspaceRootId) {
    if (newNode == null) {
      return;
    }

    if (PathUtil.WORKSPACE_ROOT.equals(path)) {
      // Install the workspace root.
      final FileTreeNode oldDir = workspaceRoot;
      workspaceRoot = newNode;

      dispatchModelChange(new ChangeDispatcher() {
        @Override
        public void dispatch(TreeModelChangeListener changeListener) {
          changeListener.onNodeReplaced(oldDir, newNode);
        }
      }, workspaceRootId);
    } else {

      // Patch the model if there is one.
      if (workspaceRoot != null) {
        final FileTreeNode nodeToReplace = workspaceRoot.findChildNode(path);

        // Note. We do not support patching subtrees that don't already
        // exist. This subtree must have already existed, or have been
        // preceded by an ADD or COPY mutation.
        if (nodeToReplace == null) {
          return;
        }

        nodeToReplace.replaceWith(newNode);

        dispatchModelChange(new ChangeDispatcher() {
          @Override
          public void dispatch(TreeModelChangeListener changeListener) {
            changeListener.onNodeReplaced(nodeToReplace, newNode);
          }
        }, workspaceRootId);
      }
    }
  }

  /**
   * @return the current value of the workspaceRoot. Potentially {@code null} if
   *         the model has not yet been populated.
   */
  public FileTreeNode getWorkspaceRoot() {
    return workspaceRoot;
  }

  /**
   * Asks for the root node, potentially asynchronously if the model is not yet
   * populated. If the root node is already available then the callback will be
   * invoked synchronously.
   */
  public void requestWorkspaceRoot(final RootNodeRequestCallback callback) {
    FileTreeNode rootNode = getWorkspaceRoot();
    if (rootNode == null) {

      // Wait for the model to be populated.
      addModelChangeListener(new AbstractTreeModelChangeListener() {
        @Override
        public void onNodeReplaced(FileTreeNode oldNode, FileTreeNode newNode) {
          Preconditions.checkArgument(newNode.getNodePath().equals(PathUtil.WORKSPACE_ROOT),
              "Unexpected non-workspace root subtree replaced before workspace root was replaced: "
              + newNode.toString());
          
          // Should be resilient to concurrent modification!
          removeModelChangeListener(this);
          callback.onRootNodeAvailable(getWorkspaceRoot());
        }
      });

      return;
    }

    callback.onRootNodeAvailable(rootNode);
  }

  /**
   * Adds a {@link TreeModelChangeListener} to be notified of mutations applied
   * by the FileTreeController to the underlying workspace file tree model.
   * 
   * @param modelChangeListener the listener we are adding
   */
  public void addModelChangeListener(TreeModelChangeListener modelChangeListener) {
    modelChangeListeners.add(modelChangeListener);
  }

  /**
   * Removes a {@link TreeModelChangeListener} from the set of listeners
   * subscribed to model changes.
   */
  public void removeModelChangeListener(TreeModelChangeListener modelChangeListener) {
    modelChangeListeners.remove(modelChangeListener);
  }

  public void setDisableChangeNotifications(boolean disable) {
    this.disableChangeNotifications = disable;
  }

  private void dispatchModelChange(ChangeDispatcher dispatcher, String workspaceRootId) {

    // Update the tracked tip ID.
    maybeSetLastAppliedTreeMutationRevision(workspaceRootId);

    if (disableChangeNotifications) {
      return;
    }

    JsoArray<TreeModelChangeListener> copy = modelChangeListeners.slice(
        0, modelChangeListeners.size());
    for (int i = 0, n = copy.size(); i < n; i++) {
      dispatcher.dispatch(copy.get(i));
    }
  }

  /**
   * @return the file tree revision associated with the last seen Tree mutation.
   */
  public String getLastAppliedTreeMutationRevision() {
    return lastAppliedTreeMutationRevision;
  }

  /**
   * Bumps the tracked Root ID for the last applied tree mutation, if the
   * version happens to be larger than the version we are tracking.
   */
  public void maybeSetLastAppliedTreeMutationRevision(String lastAppliedTreeMutationRevision) {
    // TODO: Ensure numeric comparison survives ID obfuscation.
    try {
      long newRevision = StringUtils.toLong(lastAppliedTreeMutationRevision);
      long lastRevision = StringUtils.toLong(this.lastAppliedTreeMutationRevision);
      this.lastAppliedTreeMutationRevision = (newRevision > lastRevision)
          ? lastAppliedTreeMutationRevision : this.lastAppliedTreeMutationRevision;
      // TODO: this should be monotonically increasing; if it's not, we missed an update.
    } catch (NumberFormatException e) {
      Log.error(getClass(), "Root ID is not a numeric long!", lastAppliedTreeMutationRevision);
    }
  }

  /**
   * Folks that want to mutate the file tree should obtain a skeletal {@link WorkspaceTreeUpdate}
   * using this factory method.
   */
  public WorkspaceTreeUpdateImpl makeEmptyTreeUpdate() {
    if (this.lastAppliedTreeMutationRevision == null) {
      throw new IllegalStateException(
          "Attempted to mutate the tree before the workspace file tree was loaded at least once!");
    }

    return WorkspaceTreeUpdateImpl.make()
        .setAuthorClientId(BootstrapSession.getBootstrapSession().getActiveClientId())
        .setMutations(JsoArray.<Mutation>create());
  }

  /**
   * Calculates the list of expanded paths. The list only contains the paths of the deepest expanded
   * directories. Parent directories are assumed to be open as well.
   *
   * @return the list of expanded paths, or null if the workspace root is not loaded
   */
  public JsoArray<String> calculateExpandedPaths() {
    // Early exit if the root isn't loaded yet.
    if (workspaceRoot == null) {
      return null;
    }

    // Walk the tree looking for expanded paths.
    JsoArray<String> expandedPaths = JsoArray.create();
    calculateExpandedPathsRecursive(workspaceRoot, expandedPaths);
    return expandedPaths;
  }

  /**
   * Calculates the list of expanded paths beneath the specified node and adds them to expanded
   * path.  If none of the children 
   *
   * @param node the directory containing the expanded paths
   * @param expandedPaths the running list of expanded paths
   */
  private void calculateExpandedPathsRecursive(FileTreeNode node, JsoArray<String> expandedPaths) {
    assert node.isDirectory() : "node must be a directory";

    // Check if the directory is expanded. The root is always expanded.
    if (node != workspaceRoot) {
      TreeNodeElement<FileTreeNode> dirElem = node.getRenderedTreeNode();
      if (!dirElem.isOpen()) {
        return;
      }
    }

    // Recursively search for expanded subdirectories.
    int expandedPathsCount = expandedPaths.size();
    DirInfoImpl dir = node.cast();
    JsonArray<DirInfo> subDirs = dir.getSubDirectories();
    if (subDirs != null) {
      for (int i = 0; i < subDirs.size(); i++) {
        DirInfo subDir = subDirs.get(i);
        calculateExpandedPathsRecursive((FileTreeNode) subDir, expandedPaths);
      }
    }

    // Add this directory if none of its descendants were added.
    if (expandedPathsCount == expandedPaths.size()) {
      expandedPaths.add(node.getNodePath().getPathString());
    }
  }
  
  /**
   * Asks for the node at the specified path, potentially asynchronously if the model does not yet
   * contain the node. If the node is already available then the callback will be invoked
   * synchronously.
   *
   * @param path the path to the node, which must be a file (not a directory)
   * @param callback the callback to invoke when the node is ready
   */
  public void requestWorkspaceNode(final PathUtil path, final NodeRequestCallback callback) {
    outgoingNetworkController.requestWorkspaceNode(this, path, callback);
  }
  
  /**
   * Asks for the children of the specified node.
   *
   * @param node a directory node
   * @param callback an optional callback that will be notified once the children are fetched. If
   *        null, this method will alert the user if there was an error
   */
  public void requestDirectoryChildren(FileTreeNode node,
      @Nullable final NodeRequestCallback callback) {
    outgoingNetworkController.requestDirectoryChildren(this, node, callback);
  }
}
