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

package com.google.collide.client.code.debugging;

import com.google.collide.client.ui.dropdown.DropdownController;
import com.google.collide.client.ui.dropdown.DropdownController.DropdownPositionerBuilder;
import com.google.collide.client.ui.dropdown.DropdownWidgets;
import com.google.collide.client.ui.list.SimpleList.ListItemRenderer;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.client.ui.tree.TreeNodeLabelRenamer;
import com.google.collide.client.ui.tree.TreeNodeMutator;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;

import elemental.html.Element;

/**
 * Handles {@link RemoteObjectTree} tree context menu actions.
 *
 */
class RemoteObjectTreeContextMenuController {

  /**
   * Callbacks on the context menu actions.
   */
  interface Listener {
    void onAddNewChild(TreeNodeElement<RemoteObjectNode> node);
    void onNodeEdited(TreeNodeElement<RemoteObjectNode> node, String newLabel);
    void onNodeDeleted(TreeNodeElement<RemoteObjectNode> node);
    void onNodeRenamed(TreeNodeElement<RemoteObjectNode> node, String oldLabel);
  }

  /**
   * A menu item in the context menu.
   */
  private interface TreeNodeMenuItem {
    public void onClicked(TreeNodeElement<RemoteObjectNode> node);

    @Override
    public String toString();
  }

  /**
   * Renderer for an item in the context menu.
   */
  class TreeItemRenderer extends ListItemRenderer<TreeNodeMenuItem> {
    @Override
    public void render(Element listItemBase, TreeNodeMenuItem item) {
      listItemBase.setTextContent(item.toString());
    }
  }

  static RemoteObjectTreeContextMenuController create(DropdownWidgets.Resources resources,
      DebuggerState debuggerState, RemoteObjectNodeRenderer nodeRenderer,
      TreeNodeLabelRenamer<RemoteObjectNode> nodeLabelMutator) {

    RemoteObjectTreeContextMenuController controller =
        new RemoteObjectTreeContextMenuController(debuggerState, nodeRenderer, nodeLabelMutator);

    controller.installContextMenuController(resources);

    return controller;
  }

  private final DebuggerState debuggerState;
  private DropdownController<TreeNodeMenuItem> contextDropdownController;
  private final RemoteObjectNodeRenderer nodeRenderer;
  private final TreeNodeLabelRenamer<RemoteObjectNode> nodeLabelMutator;
  private Listener listener;

  private TreeNodeElement<RemoteObjectNode> selectedNode;
  private TreeItemRenderer renderer;

  private TreeNodeMenuItem menuRename;
  private TreeNodeMenuItem menuDelete;
  private TreeNodeMenuItem menuAdd;
  private TreeNodeMenuItem menuEdit;

  private RemoteObjectTreeContextMenuController(DebuggerState debuggerState,
      RemoteObjectNodeRenderer nodeRenderer,
      TreeNodeLabelRenamer<RemoteObjectNode> nodeLabelMutator) {
    this.debuggerState = debuggerState;
    this.nodeRenderer = nodeRenderer;
    this.nodeLabelMutator = nodeLabelMutator;

    this.renderer = new TreeItemRenderer();
    createMenuItems();
  }

  void setListener(Listener listener) {
    this.listener = listener;
  }

  private void installContextMenuController(DropdownWidgets.Resources res) {
    DropdownController.Listener<TreeNodeMenuItem> listener =
        new DropdownController.BaseListener<TreeNodeMenuItem>() {
          @Override
          public void onItemClicked(TreeNodeMenuItem item) {
            item.onClicked(selectedNode);
          }
        };

    Positioner mousePositioner = new DropdownPositionerBuilder().setHorizontalAlign(
        HorizontalAlign.RIGHT).buildMousePositioner();
    contextDropdownController = new DropdownController.Builder<TreeNodeMenuItem>(
        mousePositioner, null, res, listener, renderer).setShouldAutoFocusOnOpen(true).build();
  }

  void show(int mouseX, int mouseY, TreeNodeElement<RemoteObjectNode> nodeElement) {
    selectedNode = nodeElement;
    if (nodeElement == null) {
      return;
    }

    RemoteObjectNode node = nodeElement.getData();
    JsonArray<TreeNodeMenuItem> menuItems = JsonCollections.createArray();

    if (canAdd(node)) {
      menuItems.add(menuAdd);
    }
    if (canEdit(node)) {
      menuItems.add(menuEdit);
    }
    if (canRenameAndDelete(node)) {
      menuItems.add(menuRename);
      menuItems.add(menuDelete);
    }

    if (!menuItems.isEmpty()) {
      contextDropdownController.setItems(menuItems);
      contextDropdownController.showAtPosition(mouseX, mouseY);
    } else {
      contextDropdownController.hide();
    }
  }

  void hide() {
    contextDropdownController.hide();
  }

  Element getContextMenuElement() {
    return contextDropdownController.getElement();
  }

  private boolean canAdd(RemoteObjectNode node) {
    return node.canAddRemoteObjectProperty();
  }

  private boolean canEdit(RemoteObjectNode node) {
    return node.isWritable()
        && debuggerState.isActive()
        && (node.isRootChild() || isRealRemoteObject(node.getParent()));
  }

  private boolean canRenameAndDelete(RemoteObjectNode node) {
    return node.isDeletable()
        && (node.isRootChild() || isRealRemoteObject(node.getParent()));
  }

  private static boolean isRealRemoteObject(RemoteObjectNode node) {
    return node != null && node.getRemoteObject() != null && !node.isTransient();
  }

  private void enterAddProperty(TreeNodeElement<RemoteObjectNode> node) {
    if (listener != null) {
      listener.onAddNewChild(node);
    }
  }

  boolean enterEditPropertyValue(TreeNodeElement<RemoteObjectNode> nodeToEdit) {
    if (!canEdit(nodeToEdit.getData())) {
      return false;
    }
    nodeLabelMutator.cancel();
    nodeLabelMutator.getTreeNodeMutator().enterMutation(
        nodeToEdit, new TreeNodeMutator.MutationAction<RemoteObjectNode>() {
          @Override
          public Element getElementForMutation(TreeNodeElement<RemoteObjectNode> node) {
            return nodeRenderer.getPropertyValueElement(node.getNodeLabel());
          }

          @Override
          public void onBeforeMutation(TreeNodeElement<RemoteObjectNode> node) {
            nodeRenderer.enterPropertyValueMutation(node.getNodeLabel());
          }

          @Override
          public void onMutationCommit(
              TreeNodeElement<RemoteObjectNode> node, String oldLabel, String newLabel) {
            String trimmedLabel = StringUtils.trimNullToEmpty(newLabel);
            nodeRenderer.exitPropertyValueMutation(node.getNodeLabel(), trimmedLabel);

            if (listener != null && !StringUtils.equalStringsOrEmpty(oldLabel, trimmedLabel)) {
              listener.onNodeEdited(node, trimmedLabel);
            }
          }

          @Override
          public boolean passValidation(TreeNodeElement<RemoteObjectNode> node, String newLabel) {
            return !StringUtils.isNullOrWhitespace(newLabel);
          }
        });
    return true;
  }

  boolean enterRenameProperty(TreeNodeElement<RemoteObjectNode> nodeToRename) {
    if (!canRenameAndDelete(nodeToRename.getData())) {
      return false;
    }
    nodeLabelMutator.cancel();
    nodeLabelMutator.enterMutation(
        nodeToRename, new TreeNodeLabelRenamer.LabelRenamerCallback<RemoteObjectNode>() {
          @Override
          public void onCommit(String oldLabel, TreeNodeElement<RemoteObjectNode> node) {
            if (listener != null && !oldLabel.equals(node.getData().getName())) {
              listener.onNodeRenamed(node, oldLabel);
            }
          }

          @Override
          public boolean passValidation(TreeNodeElement<RemoteObjectNode> node, String newLabel) {
            return !StringUtils.isNullOrWhitespace(newLabel);
          }
        });
    return true;
  }

  private void enterDeleteProperty(TreeNodeElement<RemoteObjectNode> node) {
    if (listener != null) {
      listener.onNodeDeleted(node);
    }
  }

  private void createMenuItems() {
    menuRename = new TreeNodeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<RemoteObjectNode> node) {
        enterRenameProperty(node);
      }

      @Override
      public String toString() {
        return "Rename";
      }
    };

    menuDelete = new TreeNodeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<RemoteObjectNode> node) {
        enterDeleteProperty(node);
      }

      @Override
      public String toString() {
        return "Delete";
      }
    };

    menuAdd = new TreeNodeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<RemoteObjectNode> node) {
        enterAddProperty(node);
      }

      @Override
      public String toString() {
        return "Add";
      }
    };

    menuEdit = new TreeNodeMenuItem() {
      @Override
      public void onClicked(TreeNodeElement<RemoteObjectNode> node) {
        enterEditPropertyValue(node);
      }

      @Override
      public String toString() {
        return "Edit";
      }
    };
  }
}
