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

import collide.client.filetree.FileTreeModel.NodeRequestCallback;
import collide.client.filetree.FileTreeModel.RootNodeRequestCallback;

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.communication.MessageFilter.MessageRecipient;
import com.google.collide.client.history.Place;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.GetDirectoryResponse;
import com.google.collide.dto.Mutation;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.dto.WorkspaceTreeUpdateBroadcast;
import com.google.collide.dto.client.DtoClientImpls.GetDirectoryImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.FrontendConstants;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;


/**
 * Controller responsible for receiving DTOs that come off the {@link MessageFilter} that were sent
 * from the frontend, and updating the {@link FileTreeModel}.
 *
 */
public class FileTreeModelNetworkController implements FileTreeInvalidatedEvent.Handler {

  /**
   * Static factory method to obtain an instance of FileTreeModelNetworkController.
   */
  public static FileTreeModelNetworkController create(FileTreeModel fileTreeModel,
      FileTreeController<?> fileTreeController, Place currentPlace) {
    FileTreeModelNetworkController networkController =
        new FileTreeModelNetworkController(fileTreeModel, fileTreeController);
    currentPlace.registerSimpleEventHandler(FileTreeInvalidatedEvent.TYPE, networkController);
    networkController.registerForInvalidations(fileTreeController.getMessageFilter());

    // Load the tree.
    networkController.reloadDirectory(PathUtil.WORKSPACE_ROOT);
    return networkController;
  }

  /**
   * A controller for the outgoing network requests for fetching nodes. This logic is used mainly
   * for lazy tree loading.
   *
   * <p>
   * This class is a static class to prevent circular instance dependencies between the
   * {@link FileTreeModel} and the {@link FileTreeModelNetworkController}.
   */
  public static class OutgoingController {

    private final FileTreeController<?> fileTreeController;

    public OutgoingController(FileTreeController<?> fileTreeController) {
      this.fileTreeController = fileTreeController;
    }

    /**
     * @see FileTreeModel#requestWorkspaceNode
     */
    void requestWorkspaceNode(final FileTreeModel fileTreeModel, final PathUtil path,
        final NodeRequestCallback callback) {
      // Wait until the root node has been loaded.
      fileTreeModel.requestWorkspaceRoot(new RootNodeRequestCallback() {
        @Override
        public void onRootNodeAvailable(FileTreeNode root) {
          // Find the closest node in the tree, which might be the node we want.
          final FileTreeNode closest = root.findClosestChildNode(path);
          if (closest == null) {
            // The node does not exist in the tree.
            callback.onNodeUnavailable();
            return;
          } else if (path.equals(closest.getNodePath())) {
            // The node is already in the tree.
            callback.onNodeAvailable(closest);
            return;
          }

          // Get the directory that contains the path.
          final PathUtil dirPath = PathUtil.createExcludingLastN(path, 1);

          // Request the node and its parents, starting from the closest node.
          /*
           * TODO: We should revisit deep linking in the file tree and possible only show
           * the directory that is deep linked. Otherwise, we may have to load a lot of parent
           * directories when deep linking to a very deep directory.
           */
          GetDirectoryImpl getDirectoryAndPath = GetDirectoryImpl.make()
              .setPath(dirPath.getPathString())
              .setDepth(FrontendConstants.DEFAULT_FILE_TREE_DEPTH)
              .setRootId(fileTreeModel.getLastAppliedTreeMutationRevision());
              // Include the root path so we load parent directories leading up to the file.
              //.setRootPath(closest.getNodePath().getPathString());
          fileTreeController.getDirectory(
              getDirectoryAndPath, new ApiCallback<GetDirectoryResponse>() {

                @Override
                public void onMessageReceived(GetDirectoryResponse response) {
                  DirInfo baseDir = response.getBaseDirectory();
                  if (baseDir == null) {
                    /*
                     * The folder was most probably deleted before the server received our request.
                     * We should receive a tango notification to update the client.
                     */
                    return;
                  }
                  FileTreeNode incomingSubtree = FileTreeNode.transform(baseDir);
                  fileTreeModel.replaceNode(
                      new PathUtil(response.getPath()), incomingSubtree, response.getRootId());

                  // Check if the node now exists.
                  FileTreeNode child = fileTreeModel.getWorkspaceRoot().findChildNode(path);
                  if (child == null) {
                    callback.onNodeUnavailable();
                  } else {
                    callback.onNodeAvailable(child);
                  }
                }

                @Override
                public void onFail(FailureReason reason) {
                  Log.error(getClass(), "Failed to retrieve directory path "
                      + dirPath.getPathString());

                  // Update the callback.
                  callback.onError(reason);
                }
              });
        }
      });
    }

    /**
     * @see FileTreeModel#requestDirectoryChildren
     */
    void requestDirectoryChildren(
        final FileTreeModel fileTreeModel, FileTreeNode node, final NodeRequestCallback callback) {
      Preconditions.checkArgument(node.isDirectory(), "Cannot request children of a file");
      final PathUtil path = node.getNodePath();
      GetDirectoryImpl getDirectory = GetDirectoryImpl.make()
          .setPath(path.getPathString())
          .setDepth(FrontendConstants.DEFAULT_FILE_TREE_DEPTH)
          .setRootId(fileTreeModel.getLastAppliedTreeMutationRevision());
      fileTreeController.getDirectory(getDirectory,
          new ApiCallback<GetDirectoryResponse>() {

            @Override
            public void onMessageReceived(GetDirectoryResponse response) {
              DirInfo baseDir = response.getBaseDirectory();
              if (baseDir == null) {
                /*
                 * The folder was most probably deleted before the server received our request. We
                 * should receive a tango notification to update the client.
                 */
                if (callback != null) {
                  callback.onNodeUnavailable();
                }
                return;
              }

              FileTreeNode incomingSubtree = FileTreeNode.transform(baseDir);
              fileTreeModel.replaceNode(
                  new PathUtil(response.getPath()), incomingSubtree, response.getRootId());

              if (callback != null) {
                callback.onNodeAvailable(incomingSubtree);
              }
            }

            @Override
            public void onFail(FailureReason reason) {
              Log.error(getClass(), "Failed to retrieve children for directory "
                  + path.getPathString());

              if (callback != null) {
                callback.onError(reason);
              } else {
                StatusMessage fatal = new StatusMessage(
                    fileTreeController.getStatusManager(), MessageType.FATAL,
                    "Could not retrieve children of directory.  Please try again.");
                fatal.setDismissable(true);
                fatal.fire();
              }
            }
          });
    }

  }

  private final FileTreeController<?> fileTreeController;
  private final FileTreeModel fileTreeModel;

  FileTreeModelNetworkController(FileTreeModel fileTreeModel, FileTreeController<?> fileTreeController) {
    this.fileTreeModel = fileTreeModel;
    this.fileTreeController = fileTreeController;
  }

  /**
   * Adds a node to our model from a broadcasted workspace tree mutation.
   */
  private void handleExternalAdd(String newPath, TreeNodeInfo newNode, String newTipId) {

    String ourClientId = BootstrapSession.getBootstrapSession().getActiveClientId();
    PathUtil path = new PathUtil(newPath);
    FileTreeNode rootNode = fileTreeModel.getWorkspaceRoot();

    // If the root is null... then we are receiving mutations before we even got
    // the workspace in the first place. So we should ignore.
    // TODO: This is a potential race. We need to schedule a pending
    // referesh for the tree!!
    if (rootNode == null) {
      Log.warn(getClass(), "Receiving ADD tree mutations before the root node is set for node: "
          + path.getPathString());
      return;
    }

    FileTreeNode existingNode = rootNode.findChildNode(path);
    if (existingNode == null) {
      fileTreeModel.addNode(path, (FileTreeNode) newNode, newTipId);
    } else {
      // If it's already there, it's probably a placeholder node we created.
      existingNode.setFileEditSessionKey(newNode.getFileEditSessionKey());
      // Because adding new node via context menu disable change notifications,
      // we need explicitly notify listeners.
      fileTreeModel.dispatchAddNode(existingNode.getParent(), existingNode, newTipId);
    }
  }

  private void handleExternalCopy(String newpath, TreeNodeInfo newNode, String newTipId) {
    PathUtil path = new PathUtil(newpath);
    FileTreeNode rootNode = fileTreeModel.getWorkspaceRoot();

    // If the root is null... then we are receiving mutations before we even got
    // the workspace in the first place. So we should ignore.
    // TODO: This is a potential race. We need to schedule a pending
    // referesh for the tree!!
    if (rootNode == null) {
      Log.warn(getClass(), "Receiving COPY tree mutations before the root node is set for node: "
          + path.getPathString());
      return;
    }

    FileTreeNode installedNode = (FileTreeNode) newNode;
    fileTreeModel.addNode(path, installedNode, newTipId);
  }

  /**
   * Removes a node from the model and from the rendered Tree.
   */
  private void handleExternalDelete(JsonArray<Mutation> deletedNodes, String newTipId) {

    // Note that for deletes we do NOT currently optimistically update the UI.
    // So we need to remove the node, even if we triggered said delete.
    JsonArray<PathUtil> pathsToDelete = JsonCollections.createArray();
    for (int i = 0; i < deletedNodes.size(); i++) {
      pathsToDelete.add(new PathUtil(deletedNodes.get(i).getOldPath()));
    }
    fileTreeModel.removeNodes(pathsToDelete, newTipId);
  }

  private void handleExternalMove(String oldPathStr, String newPathStr, String newTipId) {
    PathUtil oldPath = new PathUtil(oldPathStr);
    PathUtil newPath = new PathUtil(newPathStr);
    FileTreeNode rootNode = fileTreeModel.getWorkspaceRoot();

    // If the root is null... then we are receiving mutations before we even got
    // the workspace in the first place. So we should ignore.
    // TODO: This is a potential race. We need to schedule a pending
    // referesh for the tree!!
    if (rootNode == null) {
      Log.warn(getClass(), "Receiving MOVE tree mutations before the root node is set for node: "
          + oldPath.getPathString());
      return;
    }

    fileTreeModel.moveNode(oldPath, newPath, newTipId);
  }

  /**
   * Handles ADD, RENAME, DELETE, or COPY messages.
   */
  private void handleFileTreeMutation(WorkspaceTreeUpdateBroadcast treeUpdate) {
    JsonArray<Mutation> mutations = treeUpdate.getMutations();
    for (int i = 0, n = mutations.size(); i < n; i++) {
      Mutation mutation = mutations.get(i);
      switch (mutation.getMutationType()) {
        case ADD:
          handleExternalAdd(
              mutation.getNewPath(), mutation.getNewNodeInfo(), treeUpdate.getNewTreeVersion());
          break;
        case DELETE:
          handleExternalDelete(treeUpdate.getMutations(), treeUpdate.getNewTreeVersion());
          break;
        case MOVE:
          handleExternalMove(mutation.getOldPath(), mutation.getNewPath(), treeUpdate.getNewTreeVersion());
          break;
        case COPY:
          handleExternalCopy(mutation.getNewPath(), mutation.getNewNodeInfo(), treeUpdate.getNewTreeVersion());
          break;
        default:
          assert (false) : "We got some kind of malformed workspace tree mutation!";
          break;
      }

      // Bump the tracked tip.
      fileTreeModel.maybeSetLastAppliedTreeMutationRevision(treeUpdate.getNewTreeVersion());
    }
  }

  public void handleSubtreeReplaced(GetDirectoryResponse response) {
    FileTreeNode incomingSubtree = FileTreeNode.transform(response.getBaseDirectory());
    fileTreeModel.replaceNode(
        new PathUtil(response.getPath()), incomingSubtree, response.getRootId());
    if (PathUtil.WORKSPACE_ROOT.equals(new PathUtil(response.getPath()))) {
      fileTreeModel.maybeSetLastAppliedTreeMutationRevision(response.getRootId());
    }
  }

  @Override
  public void onFileTreeInvalidated(PathUtil invalidatedPath) {

    // Check if the invalidated path points to a file
    if (!invalidatedPath.equals(PathUtil.WORKSPACE_ROOT)) {
      if (fileTreeModel.getWorkspaceRoot() != null) {
        FileTreeNode invalidatedNode =
            fileTreeModel.getWorkspaceRoot().findChildNode(invalidatedPath);
        if (invalidatedNode == null) {
          // Our lazy tree does not contain the node, we don't have to do anything
          return;
        }

        if (invalidatedNode.isFile()) {
          reloadDirectory(PathUtil.createExcludingLastN(invalidatedPath, 1));
          return;
        }

      } else {
        /*
         * We don't have enough information yet to invalidate the specific node, so we just
         * invalidate the workspace root
         */
        invalidatedPath = PathUtil.WORKSPACE_ROOT;
      }
    }

    reloadDirectory(invalidatedPath);
  }

  private void reloadDirectory(final PathUtil invalidatedPath) {
    GetDirectoryImpl request = GetDirectoryImpl.make().setRootId(
        fileTreeModel.getLastAppliedTreeMutationRevision())
        .setDepth(FrontendConstants.DEFAULT_FILE_TREE_DEPTH);

    // Fetch the parent directory of the file
    request.setPath(invalidatedPath.toString());

    fileTreeController.getDirectory(request,
        new ApiCallback<GetDirectoryResponse>() {

          @Override
          public void onMessageReceived(GetDirectoryResponse response) {
            handleSubtreeReplaced(response);
          }

          @Override
          public void onFail(FailureReason reason) {
            Log.error(getClass(), "Failed to retrieve file metadata for workspace.");
            StatusMessage fatal = new StatusMessage(
                fileTreeController.getStatusManager(), MessageType.FATAL,
                "There was a problem refreshing changes within the file tree :(.");
            fatal.addAction(StatusMessage.RELOAD_ACTION);
            fatal.setDismissable(true);
            fatal.fire();
          }
        });
  }

  public void registerForInvalidations(MessageFilter messageFilter) {
    messageFilter.registerMessageRecipient(RoutingTypes.WORKSPACETREEUPDATEBROADCAST,
        new MessageRecipient<WorkspaceTreeUpdateBroadcast>() {

      @Override
      public void onMessageReceived(WorkspaceTreeUpdateBroadcast update) {
        if (update != null) {
          handleFileTreeMutation(update);
        } else {
          // Either the invalidation was not the next sequential one or we
          // didn't get the payload. Reload the entire tree.
          onFileTreeInvalidated(PathUtil.WORKSPACE_ROOT);
        }
      }
    });
  }
}
