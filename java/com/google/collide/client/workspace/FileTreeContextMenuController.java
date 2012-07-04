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

import com.google.collide.client.AppContext;
import com.google.collide.client.Resources;
import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.code.debugging.DebuggingModelController;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.ResourceUriUtils;
import com.google.collide.client.history.Place;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.testing.DebugAttributeSetter;
import com.google.collide.client.ui.dropdown.DropdownController;
import com.google.collide.client.ui.dropdown.DropdownController.DropdownPositionerBuilder;
import com.google.collide.client.ui.list.SimpleList.ListItemRenderer;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.ui.tree.SelectionModel;
import com.google.collide.client.ui.tree.Tree;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.client.ui.tree.TreeNodeLabelRenamer;
import com.google.collide.client.ui.tree.TreeNodeLabelRenamer.LabelRenamerCallback;
import com.google.collide.client.util.BrowserUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.FileTreeModel.NodeRequestCallback;
import com.google.collide.client.workspace.UploadClickedEvent.UploadType;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.EmptyMessage;
import com.google.collide.dto.FileInfo;
import com.google.collide.dto.Mutation;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.dto.WorkspaceTreeUpdate;
import com.google.collide.dto.client.DtoClientImpls.DirInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.FileInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.TreeNodeInfoImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.FrontendConstants;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.user.client.Window;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;
import elemental.html.IFrameElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles File tree context menu actions.
 */
public class FileTreeContextMenuController {

  /**
   * The data for a menu item in the context menu.
   */
  abstract class FileTreeMenuItem {
    abstract void onClicked(TreeNodeElement<FileTreeNode> node);

    boolean isDisabled() {
      return false;
    }

    @Override
    public abstract String toString();
  }

  class FileTreeItemRenderer extends ListItemRenderer<FileTreeMenuItem> {
    final String DISABLED_COLOR = "#ccc";

    @Override
    public void render(Element listItemBase, FileTreeMenuItem item) {
      if (item.isDisabled()) {
        listItemBase.getStyle().setColor(DISABLED_COLOR);
      }
      new DebugAttributeSetter().add("disabled", Boolean.toString(item.isDisabled()))
          .on(listItemBase);
      listItemBase.setTextContent(item.toString());
    }
  }

  /**
   * Specifies the current mode of the context menu. Some modes may indicate that the context menu
   * retains control of the cursor, for example in RENAME mode. When the mode is READY, the context
   * menu will not capture the cursor.
   */
  public enum ContextMenuMode {
    /** the cursor will not be captured by the context menu */
    READY,

    /** the cursor will be captured and used for placing the caret in the rename text field */
    RENAME
  }

  private static final String NEW_FILE_NAME = "untitled";

  /**
   * The parameter used to enable file tree cut.
   */
  private static final String FILE_TREE_CUT_URL_PARAM = "fileTreeCutEnabled";

  static final String DOWNLOAD_FRAME_ID = "download";

  /**
   * Static factory method for obtaining an instance of FileTreeContextMenuController.
   */
  public static FileTreeContextMenuController create(Place place,
      Resources res,
      FileTreeUiController fileTreeUiController,
      FileTreeModel fileTreeModel,
      TreeNodeLabelRenamer<FileTreeNode> nodeLabelMutator,
      AppContext appContext,
      DebuggingModelController debuggingModelController) {

    FileTreeContextMenuController ctxMenuController = new FileTreeContextMenuController(place,
        fileTreeUiController,
        fileTreeModel,
        nodeLabelMutator,
        appContext,
        debuggingModelController);
    ctxMenuController.installContextMenu(res);
    return ctxMenuController;
  }

  private final AppContext appContext;

  private DropdownController<FileTreeMenuItem> contextDropdownController;

  private DropdownController<FileTreeMenuItem> buttonDropdownController;

  private final JsoArray<FileTreeNode> copiedNodes = JsoArray.create();
  private boolean copiedNodesAreCut = false;

  private final JsonArray<FileTreeMenuItem> rootMenuItems = JsonCollections.createArray();
  private final JsonArray<FileTreeMenuItem> dirMenuItems = JsonCollections.createArray();
  private final JsonArray<FileTreeMenuItem> fileMenuItems = JsonCollections.createArray();
  private final JsonArray<FileTreeMenuItem> readonlyRootMenuItems = JsonCollections.createArray();
  private final JsonArray<FileTreeMenuItem> readonlyDirMenuItems = JsonCollections.createArray();
  private final JsonArray<FileTreeMenuItem> readonlyFileMenuItems = JsonCollections.createArray();
  
  private final boolean isReadOnly = false;
  private final FileTreeItemRenderer renderer;
  private final List<FileTreeMenuItem> allMenuItems = new ArrayList<FileTreeMenuItem>();
  private final FileTreeModel fileTreeModel;
  private final FileTreeUiController fileTreeUiController;
  private final TreeNodeLabelRenamer<FileTreeNode> nodeLabelMutator;
  private final Place place;
  private final DebuggingModelController debuggingModelController;

  private Tooltip invalidNameTooltip;

  private ContextMenuMode mode = ContextMenuMode.READY;

  private TreeNodeElement<FileTreeNode> selectedNode;

  FileTreeContextMenuController(Place place,
      FileTreeUiController fileTreeUiController,
      FileTreeModel fileTreeModel,
      TreeNodeLabelRenamer<FileTreeNode> nodeLabelMutator,
      AppContext appContext,
      DebuggingModelController debuggingModelController) {
    this.place = place;
    this.fileTreeUiController = fileTreeUiController;
    this.fileTreeModel = fileTreeModel;
    this.appContext = appContext;
    this.nodeLabelMutator = nodeLabelMutator;
    this.debuggingModelController = debuggingModelController;

    createMenuItems();

    renderer = new FileTreeItemRenderer();
  }

  /**
   * Creates an additional dropdown menu attached to a button instead of a right click.
   *
   * @param anchorElement the element to attach the button to
   */
  public void createMenuDropdown(Element anchorElement) {
    // Create the dropdown controller for the file tree menu button.
    DropdownController.Listener<FileTreeMenuItem> listener =
        new DropdownController.BaseListener<FileTreeMenuItem>() {
          @Override
          public void onItemClicked(FileTreeMenuItem item) {
            if (item.isDisabled()) {
              return;
            }
            item.onClicked(null);
          }
        };
    Positioner positioner = new DropdownPositionerBuilder().setHorizontalAlign(
        HorizontalAlign.RIGHT).buildAnchorPositioner(anchorElement);
    buttonDropdownController = new DropdownController.Builder<FileTreeMenuItem>(positioner,
        anchorElement, appContext.getResources(), listener, renderer).setShouldAutoFocusOnOpen(true)
        .build();
    buttonDropdownController.setItems(rootMenuItems);
  }

  /**
   * Simply handles the copy command from the context menu. Doesn't actually do anything other than
   * stash a reference to the copied node until a paste is issued.
   *
   * @param node the copied node
   * @param isCut true if the node is cut
   */
  private void handleCopy(TreeNodeElement<FileTreeNode> node, boolean isCut) {
    copiedNodesAreCut = isCut;
    copiedNodes.clear();

    SelectionModel<FileTreeNode> selectionModel =
        fileTreeUiController.getTree().getSelectionModel();
    copiedNodes.addAll(selectionModel.getSelectedNodes());

    // If there is no active selection, simply make the clicked on node the
    // copiedNode.
    if (copiedNodes.isEmpty()) {
      copiedNodes.add(node.getData());
    }
  }

  public void handleDelete(final TreeNodeElement<FileTreeNode> nodeToDelete) {
    WorkspaceTreeUpdate msg = fileTreeModel.makeEmptyTreeUpdate();
    JsoArray<FileTreeNode> selectedNodes =
        fileTreeUiController.getTree().getSelectionModel().getSelectedNodes();

    for (int i = 0, n = selectedNodes.size(); i < n; i++) {
      FileTreeNode node = selectedNodes.get(i);
      copiedNodes.remove(node);
      msg.getMutations().add(FileTreeUtils.makeMutation(
          Mutation.Type.DELETE, node.getNodePath(), null, node.isDirectory(),
          node.getFileEditSessionKey()));
    }

    appContext.getFrontendApi().MUTATE_WORKSPACE_TREE.send(
        msg, new ApiCallback<EmptyMessage>() {

          @Override
          public void onMessageReceived(EmptyMessage message) {

            // We lean on the Tango broadcast to mutate the
            // tree.
          }

          @Override
          public void onFail(FailureReason reason) {
            // Do nothing.
          }
        });
  }

  public void handleNewFile(TreeNodeElement<FileTreeNode> parentTreeNode) {

    handleNodeWillBeAdded();

    FileTreeNode parentData = getDirData(parentTreeNode);
    String newFileName = FileTreeUtils.allocateName(parentData.<DirInfoImpl>cast(), NEW_FILE_NAME);

    FileInfoImpl newFile = FileInfoImpl.make().setSize("0");
    newFile.<TreeNodeInfoImpl>cast().setNodeType(TreeNodeInfo.FILE_TYPE);
    newFile.setName(newFileName);

    handleNewNode(parentTreeNode, parentData, newFile.<FileTreeNode>cast());
  }

  public void handleNewFolder(TreeNodeElement<FileTreeNode> parentTreeNode) {

    handleNodeWillBeAdded();

    FileTreeNode parentData = getDirData(parentTreeNode);
    String newDirName =
        FileTreeUtils.allocateName(parentData.<DirInfoImpl>cast(), NEW_FILE_NAME + "Folder");

    DirInfoImpl newDir = DirInfoImpl.make()
        .setFiles(JsoArray.<FileInfo>create()).setSubDirectories(JsoArray.<DirInfo>create())
        .setIsComplete(false);
    newDir.<TreeNodeInfoImpl>cast().setNodeType(TreeNodeInfo.DIR_TYPE);
    newDir.setName(newDirName);

    handleNewNode(parentTreeNode, parentData, newDir.<FileTreeNode>cast());
  }

  /**
   * Notify that a file is going to be added so that we can hide the template picker. This is
   * workspace/user specific, so we don't need to broadcast the message. The actual add will be
   * broadcast.
   */
  private void handleNodeWillBeAdded() {
    fileTreeUiController.nodeWillBeAdded();
  }

  public void handleDownload(TreeNodeElement<FileTreeNode> parentTreeNode, final boolean asZip) {
    FileTreeNode parentData = getDirData(parentTreeNode);
    final String path = parentData == null ? "/" : parentData.getNodePath().getPathString();

    if (parentTreeNode != null) {
      handleDownloadImpl(asZip, path, path.substring(path.lastIndexOf('/') + 1));
    } else {
// TODO: Re-enable workspace downloading.

//      workspaceManager.getWorkspace(new QueryCallback<Workspace>() {
//        @Override
//        public void onFail(FailureReason reason) {
//          handleDownloadImpl(asZip, "/", "workspace-" + );
//        }
//
//        @Override
//        public void onQuerySuccess(Workspace result) {
//          // Lose special characters that would confuse OS'es, shells, or
//          // people. We replace spaces and tabs; slash, colon and backslash;
//          // semicolon and quotes with underbar. To avoid confusing HTTP
//          // agents, we then URL-encode the result.
//          String name = result.getWorkspaceInfo().getName().replaceAll("[\\s/:\\\\;'\"]", "_");
//          name = URL.encodeQueryString(name);
//          handleDownloadImpl(asZip, "/", name);
//        }
//      });
    }
  }

  private void handleDownloadImpl(boolean asZip, String path, String fileSource) {
    String relativeUri =
        ResourceUriUtils.getAbsoluteResourceUri(fileSource);
    // actual .zip resources we download raw; anything else we do as a zip
    String source = relativeUri + (asZip ? ".zip?rt=zip" : "?rt=download") + "&cl="
        + BootstrapSession.getBootstrapSession().getActiveClientId();
    source = source + "&" + FrontendConstants.FILE_PARAM_NAME + "=" + path;

    // we're going to download the zip into a hidden iframe, which because
    // it's a zip the browser should offer to save on disk.
    final IFrameElement iframe = Elements.createIFrameElement();
    iframe.setId(DOWNLOAD_FRAME_ID);
    iframe.getStyle().setDisplay("none");
    iframe.setOnLoad(new EventListener() {
      @Override
      public void handleEvent(Event event) {
        iframe.removeFromParent();
      }
    });

    iframe.setSrc(source);
    Elements.getBody().appendChild(iframe);
  }

  /**
   * We do not do an optimistic UI update here since we potentially will need to re-fetch an entire
   * subtree of data, and doing an eager deep clone on the client seems like it would short circuit
   * a lot of our DTO->model data transformation.
   *
   *  Note that we do not support CUT for nodes in the tree. Only COPY and MOVE. Therefore PASTE
   * only has to consider the COPY case.
   *
   * @param parentDirNode the parent dir node (which may be incomplete), or null for the root
   */
  public void handlePaste(TreeNodeElement<FileTreeNode> parentDirNode) {
    if (copiedNodes.isEmpty()) {
      return;
    }

    // Figure out where it is being pasted to.
    FileTreeNode parentDirData = getDirData(parentDirNode);

    if (!parentDirData.isComplete()) {
      // Ensure we have its children so our duplicate check works
      fileTreeModel.requestDirectoryChildren(parentDirData, new NodeRequestCallback() {
        @Override
        public void onNodeAvailable(FileTreeNode node) {
          handlePasteForCompleteParent(node);
        }

        @Override
        public void onNodeUnavailable() {
          /*
           * This should be very rare, if you paste into an incomplete directory at the same time a
           * collaborator deletes it (your XHR response comes faster than the tree mutation push
           * message)
           */
          new StatusMessage(appContext.getStatusManager(), MessageType.ERROR,
              "The destination folder for the paste no longer exists.").fire();
        }

        @Override
        public void onError(FailureReason reason) {
          new StatusMessage(appContext.getStatusManager(), MessageType.ERROR,
              "The paste had a problem, please try again.").fire();
        }
      });
    } else {
      handlePasteForCompleteParent(parentDirData);
    }
  }

  private void handlePasteForCompleteParent(FileTreeNode parentDirData) {
    // TODO: Figure out if we are pasting on top of files that already
    // exist with the same name. If we do, we need to handle that via a prompted
    // replace.

    Mutation.Type mutationType = copiedNodesAreCut ? Mutation.Type.MOVE : Mutation.Type.COPY;
    WorkspaceTreeUpdate msg = fileTreeModel.makeEmptyTreeUpdate();
    for (int i = 0, n = copiedNodes.size(); i < n; i++) {
      FileTreeNode copiedNode = copiedNodes.get(i);
      PathUtil targetPath = new PathUtil.Builder().addPath(parentDirData.getNodePath())
          .addPathComponent(FileTreeUtils.allocateName(
              parentDirData.<DirInfoImpl>cast(), copiedNode.getName())).build();
      msg.getMutations().add(FileTreeUtils.makeMutation(
          mutationType, copiedNode.getNodePath(), targetPath, copiedNode.isDirectory(),
          copiedNode.getFileEditSessionKey()));
    }

    // Cut nodes can only be pasted once.
    if (copiedNodesAreCut) {
      copiedNodes.clear();
    }

    appContext.getFrontendApi().MUTATE_WORKSPACE_TREE.send(
        msg, new ApiCallback<EmptyMessage>() {

          @Override
          public void onMessageReceived(EmptyMessage message) {

            // Do nothing. We lean on the multicasted COPY to have the action
            // update our local model.
          }

          @Override
          public void onFail(FailureReason reason) {
            // Do nothing.
          }
        });
  }

  /**
   * Renames the specified node via an inline edit UI.
   *
   *  In the event of a failure on the FE, the appropriate action is to restore the previous name of
   * the node.
   */
  public void handleRename(TreeNodeElement<FileTreeNode> renamedNode) {

    // We hang on to the old name in case we need to roll back the rename.
    FileTreeNode data = renamedNode.getData();
    final String oldName = data.getName();
    final PathUtil oldPath = data.getNodePath();

    // Go into "rename node" mode.
    setMode(ContextMenuMode.RENAME);
    nodeLabelMutator.enterMutation(renamedNode, new LabelRenamerCallback<FileTreeNode>() {
      @Override
      public void onCommit(String oldLabel, final TreeNodeElement<FileTreeNode> node) {
        if (invalidNameTooltip != null) {
          // if we were showing a tooltip related to the rename, hide it now
          invalidNameTooltip.destroy();
          invalidNameTooltip = null;
        }

        // If the name didn't change. Do nothing.
        if (oldLabel.equals(node.getData().getName())) {
          setMode(ContextMenuMode.READY);
          return;
        }

        // The node should have been renamed in the UI. This is where we
        // send a message to the frontend.
        WorkspaceTreeUpdate msg = fileTreeModel.makeEmptyTreeUpdate();
        msg.getMutations().add(FileTreeUtils.makeMutation(
            Mutation.Type.MOVE, oldPath, node.getData().getNodePath(), node.getData().isDirectory(),
            node.getData().getFileEditSessionKey()));

        appContext.getFrontendApi().MUTATE_WORKSPACE_TREE.send(
            msg, new ApiCallback<EmptyMessage>() {

              @Override
              public void onFail(FailureReason reason) {

                // TODO: Differentiate between a server mutation problem
                // and some other failure. If the mutation succeeded,
                // this revert might be overzealous since the change
                // could have been applied, but timeout or something
                // afterwards. This is a rare corner case though.

                // Roll back!
                nodeLabelMutator.mutateNodeKey(node, oldName);
              }

              @Override
              public void onMessageReceived(EmptyMessage message) {
                // Notification of tree mutation will come via Tango.
                // If this file was open, EditorReloadingFileTreeListener will
                // ensure that the editor now points to the new path.

              }
            });

        setMode(ContextMenuMode.READY);
      }

      @Override
      public boolean passValidation(TreeNodeElement<FileTreeNode> node, String newLabel) {
        if (newLabel.equals(node.getData().getName())) {
          return true;
        }

        return notifyIfNameNotValid(node, newLabel);
      }
    });
  }

  private void handleViewFile(TreeNodeElement<FileTreeNode> node) {
    debuggingModelController.runApplication(node.getData().getNodePath());
  }

  /**
   * Shows the context menu at the specified X and Y coordinates, for a given {@link FileTreeNode}.
   */
  public void show(int mouseX, int mouseY, TreeNodeElement<FileTreeNode> node) {
    if (fileTreeModel.getWorkspaceRoot() == null) {
      return;
    }
    selectedNode = node;

    if (isReadOnly) {
      if (node == null) {
        contextDropdownController.setItems(readonlyRootMenuItems);
      } else if (node.getData().isDirectory()) {
        contextDropdownController.setItems(readonlyDirMenuItems);
      } else {
        contextDropdownController.setItems(readonlyFileMenuItems);
      }
    } else {
      if (node == null) {
        contextDropdownController.setItems(rootMenuItems);
      } else if (node.getData().isDirectory()) {
        contextDropdownController.setItems(dirMenuItems);
      } else {
        contextDropdownController.setItems(fileMenuItems);
      }
    }
    contextDropdownController.showAtPosition(mouseX, mouseY);
  }

  /**
   * Sends a {@link WorkspaceTreeUpdate} message for an ADD mutation to the frontend to be
   * broadcasted to all clients.
   *
   *  Additions create a placeholder node in order to obtain a name for the new node via inline
   * editing of the node. In the event of a failure, we simply need to pop the added node out of the
   * tree.
   */
  private void broadcastAdd(final FileTreeNode installedNode) {

    TreeNodeElement<FileTreeNode> placeholderNode = installedNode.getRenderedTreeNode();

    assert (placeholderNode != null) : "Placeholder node was not allocated for newly added node: "
        + installedNode.getName();

    // Go into "rename node" mode.
    nodeLabelMutator.enterMutation(placeholderNode, new LabelRenamerCallback<FileTreeNode>() {
      @Override
      public void onCommit(String oldLabel, final TreeNodeElement<FileTreeNode> node) {
        if (invalidNameTooltip != null) {
          // if we were showing a tooltip related to the rename, hide it now
          invalidNameTooltip.destroy();
          invalidNameTooltip = null;
        }

        WorkspaceTreeUpdate msg = fileTreeModel.makeEmptyTreeUpdate();
        msg.getMutations().add(FileTreeUtils.makeMutation(Mutation.Type.ADD, null,
            installedNode.getNodePath(), installedNode.isDirectory(), null));

        appContext.getFrontendApi().MUTATE_WORKSPACE_TREE.send(
            msg, new ApiCallback<EmptyMessage>() {

              @Override
              public void onFail(FailureReason reason) {

                // TODO: Differentiate between a server mutation problem
                // and some other failure. If the mutation succeeded,
                // this revert might be overzealous since the change
                // could have been applied, but timeout or something
                // afterwards. This is a rare corner case though.

                // Roll back! Pop the node out of the tree since the add failed
                // on the FE. Note that the node that was added was "optimistic"
                // and would not have been able to bump the tracked tip ID, so
                // just pass in the existing one.
                fileTreeModel.removeNode(
                    installedNode, fileTreeModel.getLastAppliedTreeMutationRevision());
              }

              @Override
              public void onMessageReceived(EmptyMessage message) {

                // Notification will come via Tango and ignored. Rerender the
                // parent directory so that we can get sort order.
                fileTreeUiController.reRenderSubTree(node.getData().getParent());

                fileTreeUiController.autoExpandAndSelectNode(node.getData(), true);     
              }
            });
      }

      @Override
      public boolean passValidation(TreeNodeElement<FileTreeNode> node, String newLabel) {
        return notifyIfNameNotValid(node, newLabel);
      }
    });
  }

  private FileTreeNode getDirData(TreeNodeElement<FileTreeNode> parentDirNode) {
    return (parentDirNode == null) ? fileTreeModel.getWorkspaceRoot() : parentDirNode.getData();
  }

  private void handleNewNode(TreeNodeElement<FileTreeNode> parentTreeNode, FileTreeNode parentData,
      FileTreeNode newNodeData) {
    // Add a node that we will then open a label renamer for. We don't want the
    // model to synchronously update any model listeners since we want to wait
    // until the label rename action succeeds.
    fileTreeModel.setDisableChangeNotifications(true);
    try {

      // This is an optimistic addition. We cannot bump tracked tip, so just
      // pass in the existing one.
      // TODO: Add some affordance to FileTreeNode so that we can
      // know when a node has not yet been committed.
      fileTreeModel.addNode(
          parentData, newNodeData, fileTreeModel.getLastAppliedTreeMutationRevision());
    } finally {
      fileTreeModel.setDisableChangeNotifications(false);
    }

    // If we are adding to the root node, then we need to simply append nodes to
    // the tree's root container.
    if (parentTreeNode == null) {
      Tree<FileTreeNode> tree = fileTreeUiController.getTree();
      TreeNodeElement<FileTreeNode> newRenderedNode = tree.createNode(newNodeData);
      tree.getView().getElement().appendChild(newRenderedNode);
    } else {

      // Open the parent node which should create the rendered placeholder for
      // the new node.
      parentData.invalidateUnifiedChildrenCache();
      fileTreeUiController.expandNode(parentTreeNode);
    }

    broadcastAdd(newNodeData);
  }

  private void createMenuItems() {
    FileTreeMenuItem newFile = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleNewFile(node);
      }

      @Override
      public String toString() {
        return "New File";
      }
    };

    FileTreeMenuItem newFolder = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleNewFolder(node);
      }

      @Override
      public String toString() {
        return "New Folder";
      }
    };

    // Check if the cut menu option is enabled.
    FileTreeMenuItem cut = null;
    if (BrowserUtils.hasUrlParameter(FILE_TREE_CUT_URL_PARAM, "t")) {
      cut = new FileTreeMenuItem() {
        @Override
        public void onClicked(TreeNodeElement<FileTreeNode> node) {
          handleCopy(node, true);
        }

        @Override
        public String toString() {
          return "Cut";
        }
      };
    }

    FileTreeMenuItem copy = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleCopy(node, false);
      }

      @Override
      public String toString() {
        return "Copy";
      }
    };

    FileTreeMenuItem rename = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleRename(node);
      }

      @Override
      boolean isDisabled() {
        return fileTreeUiController.getTree().getSelectionModel().getSelectedNodes().size() > 1;
      }

      @Override
      public String toString() {
        return "Rename";
      }
    };

    FileTreeMenuItem delete = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleDelete(node);
      }

      @Override
      public String toString() {
        return "Delete";
      }
    };

    FileTreeMenuItem viewFile = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleViewFile(node);
      }

      @Override
      public String toString() {
        return "View in Browser";
      }
    };

    FileTreeMenuItem paste = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handlePaste(node);
      }

      @Override
      boolean isDisabled() {
        return copiedNodes.isEmpty();
      }

      @Override
      public String toString() {
        return "Paste";
      }
    };

    FileTreeMenuItem folderAsZip = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleDownload(node, true);
      }

      @Override
      public String toString() {
        return "Download Folder as a Zip";
      }
    };

    FileTreeMenuItem branchAsZip = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleDownload(node, true);
      }

      @Override
      public String toString() {
        return "Download Branch as a Zip";
      }
    };

    FileTreeMenuItem download = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        handleDownload(node, false);
      }

      @Override
      public String toString() {
        return "Download";
      }
    };

    final PathUtil rootPath = PathUtil.EMPTY_PATH;
    FileTreeMenuItem uploadFile = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        place.fireEvent(new UploadClickedEvent(UploadType.FILE, node == null ? rootPath
            : node.getData().getNodePath()));
      }

      @Override
      public String toString() {
        return "Upload File";
      }
    };

    FileTreeMenuItem uploadZip = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        place.fireEvent(new UploadClickedEvent(UploadType.ZIP, node == null ? rootPath
            : node.getData().getNodePath()));
      }

      @Override
      public String toString() {
        return "Upload and Extract Zip";
      }
    };

    FileTreeMenuItem uploadFolder = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        place.fireEvent(new UploadClickedEvent(UploadType.DIRECTORY, node == null ? rootPath
            : node.getData().getNodePath()));
      }

      @Override
      public String toString() {
        return "Upload Folder";
      }
    };

    FileTreeMenuItem newTab = new FileTreeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<FileTreeNode> node) {
        String link = WorkspaceUtils.createDeepLinkToFile(node.getData().getNodePath());
        Window.open(link, node.getData().getName(), null);
      }

      @Override
      public String toString() {
        return "Open in New Tab";
      }
    };

    rootMenuItems.add(newFile);
    rootMenuItems.add(newFolder);
    rootMenuItems.add(paste);
    rootMenuItems.add(uploadFile);
    rootMenuItems.add(uploadFolder);
    rootMenuItems.add(uploadZip);
    rootMenuItems.add(branchAsZip);

    dirMenuItems.add(newFile);
    dirMenuItems.add(newFolder);
    if (cut != null) {
      dirMenuItems.add(cut);
    }
    dirMenuItems.add(copy);
    dirMenuItems.add(paste);
    dirMenuItems.add(rename);
    dirMenuItems.add(delete);
    dirMenuItems.add(uploadFile);
    dirMenuItems.add(uploadFolder);
    dirMenuItems.add(uploadZip);
    dirMenuItems.add(folderAsZip);

    if (cut != null) {
      fileMenuItems.add(cut);
    }
    fileMenuItems.add(copy);
    fileMenuItems.add(viewFile);
    fileMenuItems.add(newTab);
    fileMenuItems.add(rename);
    fileMenuItems.add(delete);
    fileMenuItems.add(download);

    // Read Only Variety
    readonlyRootMenuItems.add(branchAsZip);

    readonlyDirMenuItems.add(folderAsZip);

    readonlyFileMenuItems.add(viewFile);
    readonlyFileMenuItems.add(newTab);
    readonlyFileMenuItems.add(download);


    allMenuItems.add(newFile);
    allMenuItems.add(newFolder);
    if (cut != null) {
      allMenuItems.add(cut);
    }
    allMenuItems.add(copy);
    allMenuItems.add(paste);
    allMenuItems.add(rename);
    allMenuItems.add(delete);
    allMenuItems.add(uploadFile);
    allMenuItems.add(uploadFolder);
    allMenuItems.add(uploadZip);
    allMenuItems.add(folderAsZip);
    allMenuItems.add(branchAsZip);
    allMenuItems.add(download);
  }

  /**
   * Create the context menu.
   */
  private void installContextMenu(Resources res) {

    DropdownController.Listener<FileTreeMenuItem> listener =
        new DropdownController.BaseListener<FileTreeMenuItem>() {
          @Override
          public void onItemClicked(FileTreeMenuItem item) {
            if (item.isDisabled()) {
              return;
            }
            item.onClicked(selectedNode);
          }
        };

    Positioner positioner = new DropdownPositionerBuilder().setHorizontalAlign(
        HorizontalAlign.RIGHT).buildMousePositioner();
    contextDropdownController = new DropdownController.Builder<FileTreeMenuItem>(
        positioner, null, res, listener, renderer).setShouldAutoFocusOnOpen(true).build();
    contextDropdownController.setItems(rootMenuItems);
  }

  /**
   * Test if node can be renamed to specified name. If not, show the user an error message.
   *
   * <p>
   * Two conditions are checked:
   * <ul>
   * <li>there is sibling with same name
   * <li>name do not contain special symbols
   * </ul>
   *
   * <p>
   * If any of the conditions fail, then appropriate warning is shown.
   */
  private boolean notifyIfNameNotValid(TreeNodeElement<FileTreeNode> node, String newLabel) {
    String message = null;
    if (!FileTreeUtils.hasNoPeerWithName(node.getData(), newLabel)) {
      message = "A file named '" + newLabel + "' already exists in this folder.";
    }
    // TODO: We need more sophisticated check.
    if (newLabel.indexOf('\\') >= 0 || newLabel.indexOf('/') >= 0 || newLabel.indexOf(',') >= 0
        || newLabel.indexOf('?') >= 0 || newLabel.indexOf('"') >= 0 || newLabel.indexOf('\'') >= 0
        || newLabel.indexOf('*') >= 0) {
      message = "A filename cannot contain any of the following characters: \\ / , ? \" ' *";
    }
    if (message != null) {
      showInvalidNameTooltip(node, message);
      return false;
    }
    return true;
  }

  /**
   * Show a tooltip next to the file notifying the user that they've entered an invalid name.
   *
   * @param node the node by which to show the tooltip
   * @param message the message to show the user
   */
  private void showInvalidNameTooltip(TreeNodeElement<FileTreeNode> node, String message) {
    if (invalidNameTooltip != null) {
      invalidNameTooltip.destroy();
    }
    invalidNameTooltip = Tooltip.create(
        appContext.getResources(), node, PositionController.VerticalAlign.MIDDLE,
        PositionController.HorizontalAlign.RIGHT, message);
    invalidNameTooltip.setDelay(0);
    invalidNameTooltip.show();
  }

  public ContextMenuMode getMode() {
    return mode;
  }

  public void setMode(ContextMenuMode mode) {
    this.mode = mode;
  }
}
