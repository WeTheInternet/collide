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

import javax.annotation.Nullable;

import com.google.collide.client.code.debugging.DebuggerApiTypes.OnEvaluateExpressionResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertiesResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertyChanged;
import com.google.collide.client.code.debugging.DebuggerApiTypes.PropertyDescriptor;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.ui.dropdown.DropdownWidgets;
import com.google.collide.client.ui.tree.Tree;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.client.ui.tree.TreeNodeLabelRenamer;
import com.google.collide.client.ui.tree.TreeNodeMutator;
import com.google.collide.client.util.AnimationUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Timer;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.js.html.JsDragEvent;

/**
 * Renders a {@link RemoteObject} in a tree-like UI.
 *
 */
public class RemoteObjectTree extends UiComponent<RemoteObjectTree.View> {

  public interface Css extends CssResource, TreeNodeMutator.Css {
    String root();

    @Override
    String nodeNameInput();
  }

  interface Resources
      extends
      DropdownWidgets.Resources,
      ClientBundle,
      Tree.Resources,
      RemoteObjectNodeRenderer.Resources {
    @Source("RemoteObjectTree.css")
    Css remoteObjectTreeCss();
  }

  /**
   * The view for the remote object tree.
   */
  static class View extends CompositeView<ViewEvents> {
    private final Css css;
    private final Tree.View<RemoteObjectNode> treeView;

    private final EventListener dblClickListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        Element target = (Element) evt.getTarget();
        if (getDelegate().onDblClick(target)) {
          evt.stopPropagation();
        }
      }
    };

    private final EventListener scrollListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        // Ensure scrollLeft is always zero (we do not support horizontal scrolling).
        getElement().setScrollLeft(0);
      }
    };

    View(Resources resources) {
      css = resources.remoteObjectTreeCss();
      treeView = new Tree.View<RemoteObjectNode>(resources);

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(treeView.getElement());
      rootElement.addEventListener(Event.DBLCLICK, dblClickListener, false);
      rootElement.addEventListener(Event.SCROLL, scrollListener, false);
      setElement(rootElement);
    }
  }

  /**
   * User actions on the debugger.
   */
  public interface Listener {
    void onRootChildrenChanged();
  }

  /**
   * The view events.
   */
  private interface ViewEvents {
    boolean onDblClick(Element target);
  }

  static RemoteObjectTree create(View view, Resources resources, DebuggerState debuggerState) {
    RemoteObjectNodeDataAdapter nodeDataAdapter = new RemoteObjectNodeDataAdapter();
    RemoteObjectNodeRenderer nodeRenderer = new RemoteObjectNodeRenderer(resources);
    Tree<RemoteObjectNode> tree = Tree.create(view.treeView, nodeDataAdapter, nodeRenderer,
        resources);
    TreeNodeLabelRenamer<RemoteObjectNode> nodeLabelMutator =
        new TreeNodeLabelRenamer<RemoteObjectNode>(
            nodeRenderer, nodeDataAdapter, resources.remoteObjectTreeCss());
    RemoteObjectTreeContextMenuController contextMenuController =
        RemoteObjectTreeContextMenuController.create(
            resources, debuggerState, nodeRenderer, nodeLabelMutator);
    return new RemoteObjectTree(
        view, tree, nodeRenderer, nodeLabelMutator, debuggerState, contextMenuController);
  }

  private static final int MAX_NUMBER_OF_CACHED_PATHS = 50;

  private final Tree<RemoteObjectNode> tree;
  private final TreeNodeLabelRenamer<RemoteObjectNode> nodeLabelMutator;
  private final RemoteObjectNodeRenderer nodeRenderer;
  private final DebuggerState debuggerState;
  private final RemoteObjectTreeContextMenuController contextMenuController;
  private Listener listener;
  private RemoteObjectNodeCache remoteObjectNodes = new RemoteObjectNodeCache();
  private JsonArray<JsonArray<String>> pathsToExpand = JsonCollections.createArray();
  private JsonStringMap<String> deferredEvaluations = JsonCollections.createMap();

  private RemoteObjectNode recentEditedNode;

  private final Tree.Listener<RemoteObjectNode> treeListener =
      new Tree.Listener<RemoteObjectNode>() {
        @Override
        public void onNodeAction(TreeNodeElement<RemoteObjectNode> node) {
        }

        @Override
        public void onNodeClosed(TreeNodeElement<RemoteObjectNode> node) {
        }

        @Override
        public void onNodeContextMenu(int mouseX, int mouseY,
            TreeNodeElement<RemoteObjectNode> node) {
          contextMenuController.show(mouseX, mouseY, node);
        }

        @Override
        public void onNodeDragDrop(TreeNodeElement<RemoteObjectNode> node, JsDragEvent event) {
        }

        @Override
        public void onRootDragDrop(JsDragEvent event) {
        }

        @Override
        public void onNodeExpanded(TreeNodeElement<RemoteObjectNode> node) {
          RemoteObjectNode remoteObjectNode = node.getData();
          if (remoteObjectNode.shouldRequestChildren()) {
            debuggerState.requestRemoteObjectProperties(
                remoteObjectNode.getRemoteObject().getObjectId());
          }
        }

        @Override
        public void onRootContextMenu(int mouseX, int mouseY) {
        }

        @Override
        public void onNodeDragStart(TreeNodeElement<RemoteObjectNode> node, JsDragEvent event) {
        }
      };

  private final TreeNodeLabelRenamer.LabelRenamerCallback<RemoteObjectNode> addNewNodeCallback =
      new TreeNodeLabelRenamer.LabelRenamerCallback<RemoteObjectNode>() {
        @Override
        public void onCommit(String oldLabel, TreeNodeElement<RemoteObjectNode> node) {
          handleOnAddNewNode(node);
        }

        @Override
        public boolean passValidation(TreeNodeElement<RemoteObjectNode> node, String newLabel) {
          return true;
        }
      };

  private class DebuggerListenerImpl implements DebuggerState.RemoteObjectListener,
      DebuggerState.DebuggerStateListener,
      DebuggerState.EvaluateExpressionListener {

    @Override
    public void onDebuggerStateChange() {
      if (!debuggerState.isActive()) {
        // Prevent memory leaks.
        pathsToExpand.clear();
        deferredEvaluations = JsonCollections.createMap();
      }
    }

    @Override
    public void onRemoteObjectPropertiesResponse(OnRemoteObjectPropertiesResponse response) {
      handleOnRemoteObjectPropertiesResponse(response);
    }

    @Override
    public void onRemoteObjectPropertyChanged(OnRemoteObjectPropertyChanged response) {
      handleOnRemoteObjectPropertyChanged(response);
    }

    @Override
    public void onEvaluateExpressionResponse(OnEvaluateExpressionResponse response) {
      handleOnEvaluateExpressionResponse(response);
    }

    @Override
    public void onGlobalObjectChanged() {
      reevaluateRootChildren();
    }
  }

  private final DebuggerListenerImpl debuggerListener = new DebuggerListenerImpl();

  private final Timer treeHeightRestorer = new Timer() {
    @Override
    public void run() {
      AnimationUtils.animatePropertySet(getView().getElement(), "min-height", "0px",
          AnimationUtils.SHORT_TRANSITION_DURATION);
    }
  };
  private final ListenerRegistrar.RemoverManager removerManager =
      new ListenerRegistrar.RemoverManager();

  private RemoteObjectTree(View view, Tree<RemoteObjectNode> tree,
      RemoteObjectNodeRenderer nodeRenderer,
      TreeNodeLabelRenamer<RemoteObjectNode> nodeLabelMutator, DebuggerState debuggerState,
      RemoteObjectTreeContextMenuController contextMenuController) {
    super(view);

    this.tree = tree;
    this.nodeRenderer = nodeRenderer;
    this.nodeLabelMutator = nodeLabelMutator;
    this.debuggerState = debuggerState;
    this.contextMenuController = contextMenuController;

    removerManager
        .track(debuggerState.getRemoteObjectListenerRegistrar().add(debuggerListener))
        .track(debuggerState.getDebuggerStateListenerRegistrar().add(debuggerListener))
        .track(debuggerState.getEvaluateExpressionListenerRegistrar().add(debuggerListener));

    tree.setTreeEventHandler(treeListener);
    setContextMenuEventListener();

    view.setDelegate(new ViewEvents() {
      @Override
      public boolean onDblClick(Element target) {
        return handleOnDblClick(target);
      }
    });
  }

  private void setContextMenuEventListener() {
    contextMenuController.setListener(new RemoteObjectTreeContextMenuController.Listener() {
      @Override
      public void onAddNewChild(TreeNodeElement<RemoteObjectNode> nodeElement) {
        // Expand the node if collapsed.
        tree.expandNode(nodeElement);
        treeListener.onNodeExpanded(nodeElement);

        addMutableChild(nodeElement.getData());
      }

      @Override
      public void onNodeEdited(TreeNodeElement<RemoteObjectNode> nodeElement, String newLabel) {
        // Collapse the node if expanded.
        tree.closeNode(nodeElement);

        RemoteObjectNode node = nodeElement.getData();
        RemoteObjectNode parent = node.getParent();

        if (node.isRootChild()) {
          String expression = "(" + node.getName() + ") = (" + newLabel + ")";
          // After this is executed we need to re-evaluate the original watch expression.
          deferredEvaluations.put(expression, node.getName());
          debuggerState.evaluateExpression(expression);
        } else if (parent != null && parent.getRemoteObject() != null) {
          // We do not know the new token type, so just remove the old one for the
          // time being (i.e. until we get the answer from the debugger).
          nodeRenderer.removeTokenClassName(node, nodeElement.getNodeLabel());

          recentEditedNode = node;
          debuggerState.setRemoteObjectProperty(
              parent.getRemoteObject().getObjectId(), node.getName(), newLabel);
        }
      }

      @Override
      public void onNodeDeleted(TreeNodeElement<RemoteObjectNode> nodeElement) {
        RemoteObjectNode node = nodeElement.getData();
        RemoteObjectNode parent = node.getParent();
        JsonArray<RemoteObjectNode> selectedNodes = tree.getSelectionModel().getSelectedNodes();

        if (node.isRootChild()) {
          for (int i = 0, n = selectedNodes.size(); i < n; ++i) {
            RemoteObjectNode selectedNode = selectedNodes.get(i);
            if (selectedNode.isRootChild() && selectedNode.getParent() == parent) {
              parent.removeChild(selectedNode);
            }
          }
          // Repaint the sub-tree.
          replaceRemoteObjectNode(parent, parent);
        } else if (parent != null && parent.getRemoteObject() != null) {
          for (int i = 0, n = selectedNodes.size(); i < n; ++i) {
            RemoteObjectNode selectedNode = selectedNodes.get(i);
            if (selectedNode.getParent() == parent) {
              recentEditedNode = null;
              debuggerState.removeRemoteObjectProperty(
                  parent.getRemoteObject().getObjectId(), selectedNode.getName());
            }
          }
        }
      }

      @Override
      public void onNodeRenamed(TreeNodeElement<RemoteObjectNode> nodeElement, String oldLabel) {
        RemoteObjectNode node = nodeElement.getData();
        RemoteObjectNode parent = node.getParent();

        if (node.isRootChild()) {
          debuggerState.evaluateExpression(node.getName());
        } else if (parent != null && parent.getRemoteObject() != null) {
          String newLabel = node.getName();

          // Undo renaming until we get the answer from the debugger.
          nodeLabelMutator.mutateNodeKey(nodeElement, oldLabel);

          recentEditedNode = node;
          debuggerState.renameRemoteObjectProperty(
              parent.getRemoteObject().getObjectId(), oldLabel, newLabel);
        }
      }
    });
  }

  void teardown() {
    removerManager.remove();
    tree.setTreeEventHandler(null);
    contextMenuController.setListener(null);
    setRoot(null); // Also clears the RemoteObjectNodeCache.
    setListener(null);
    pathsToExpand.clear();
    deferredEvaluations = JsonCollections.createMap();
    recentEditedNode = null;
  }

  void setListener(Listener listener) {
    this.listener = listener;
  }

  /**
   * Returns the root of the tree. Also commits all active mutations, so that
   * the tree should not be in an inconsistent state while exposing it's root
   * outside.
   *
   * @return tree root
   */
  RemoteObjectNode getRoot() {
    nodeLabelMutator.forceCommit();
    return tree.getModel().getRoot();
  }

  int getRootChildrenCount() {
    RemoteObjectNode root = tree.getModel().getRoot();
    if (root == null) {
      return 0;
    }
    return root.getChildren().size();
  }

  void setRoot(@Nullable RemoteObjectNode newRoot) {
    RemoteObjectNode root = tree.getModel().getRoot();
    if (root != newRoot) {
      recentEditedNode = null;
    }
    replaceRemoteObjectNode(root, newRoot);
  }

  Element getContextMenuElement() {
    return contextMenuController.getContextMenuElement();
  }

  /**
   * Replaces an existing {@link RemoteObjectNode} with a new one, and refreshes
   * the tree UI. This will also save the changes to the {@link RemoteObjectNode}
   * model.
   *
   * @param oldNode old node to be removed
   * @param newNode new node to replace the old one
   */
  void replaceRemoteObjectNode(RemoteObjectNode oldNode, RemoteObjectNode newNode) {
    contextMenuController.hide();

    // Save the changes to the model.
    if (oldNode != newNode && oldNode != null) {
      RemoteObjectNode parent = oldNode.getParent();
      if (parent != null) {
        parent.removeChild(oldNode);
        if (newNode != null) {
          parent.addChild(newNode);
        }
      }
    }

    saveTreeMinHeight();
    // NOTE: Update the UI before tearing down the cache.
    pathsToExpand.addAll(tree.replaceSubtree(oldNode, newNode, false));
    replaceRemoteObjectNodesCache(collectAllChildren());
    expandCachedPaths();

    if (listener != null && newNode == tree.getModel().getRoot()) {
      listener.onRootChildrenChanged();
    }
  }

  private void expandCachedPaths() {
    pathsToExpand = tree.expandPaths(pathsToExpand, true);

    // Ensure maximum size of the cache.
    if (pathsToExpand.size() > MAX_NUMBER_OF_CACHED_PATHS) {
      pathsToExpand.splice(0, pathsToExpand.size() - MAX_NUMBER_OF_CACHED_PATHS);
    }
  }

  private void removeRemoteObjectNode(RemoteObjectNode node) {
    contextMenuController.hide();

    // Save the changes to the model.
    RemoteObjectNode parent = node.getParent();
    if (parent != null) {
      parent.removeChild(node);
    }

    // Update the tree UI.
    tree.removeNode(node.getRenderedTreeNode());
    remoteObjectNodes.remove(node);
    node.teardown();

    if (listener != null && parent == tree.getModel().getRoot()) {
      listener.onRootChildrenChanged();
    }
  }

  private void saveTreeMinHeight() {
    Element element = getView().getElement();
    AnimationUtils.removeTransitions(element.getStyle());
    element.getStyle().setProperty("min-height",
        element.getOffsetHeight() + CSSStyleDeclaration.Unit.PX);
    treeHeightRestorer.schedule(100);
  }

  void collapseRootChildren() {
    RemoteObjectNode root = tree.getModel().getRoot();
    if (root == null) {
      return;
    }

    JsonArray<RemoteObjectNode> children = root.getChildren();
    for (int i = 0, n = children.size(); i < n; ++i) {
      TreeNodeElement<RemoteObjectNode> nodeElement =
          tree.getModel().getDataAdapter().getRenderedTreeNode(children.get(i));
      if (nodeElement != null) {
        tree.closeNode(nodeElement);
      }
    }
  }

  void addMutableRootChild() {
    if (nodeLabelMutator.isMutating()) {
      return;
    }

    RemoteObjectNode root = tree.getModel().getRoot();
    if (root == null) {
      root = RemoteObjectNode.createRoot();
      setRoot(root);
    }

    addMutableChild(root);
  }

  private void addMutableChild(RemoteObjectNode parent) {
    RemoteObjectNode child = RemoteObjectNode.createBeingEdited();

    if (hasNoRealRemoteObjectChildren(parent)) {
      // Handle the case when there is only a dummy "No Properties" node.
      replaceRemoteObjectNode(parent.getChildren().get(0), child);
    } else {
      parent.addChild(child);
      replaceRemoteObjectNode(parent, parent);
    }

    nodeLabelMutator.cancel();
    nodeLabelMutator.enterMutation(tree.getModel().getDataAdapter().getRenderedTreeNode(child),
        addNewNodeCallback);
  }

  private void handleOnAddNewNode(TreeNodeElement<RemoteObjectNode> nodeElement) {
    RemoteObjectNode node = nodeElement.getData();
    RemoteObjectNode parent = node.getParent();

    String name = node.getName();
    boolean isRootChild = node.isRootChild();

    recentEditedNode = parent;
    removeRemoteObjectNode(node);

    if (isRootChild) {
      appendRootChild(name);
    } else if (parent != null && parent.getRemoteObject() != null
        && !StringUtils.isNullOrWhitespace(name)) {
      RemoteObjectNode newChild = parent.getFirstChildByName(name);
      if (newChild == null) {
        // Tell the debugger to add the new property to the remote object and wait for response.
        debuggerState.setRemoteObjectProperty(parent.getRemoteObject().getObjectId(),
            name, DebuggerApiTypes.UNDEFINED_REMOTE_OBJECT.getDescription());
      } else if (!nodeLabelMutator.isMutating() && newChild.getRenderedTreeNode() != null) {
        contextMenuController.enterEditPropertyValue(newChild.getRenderedTreeNode());
      }
    } else if (hasNoRealRemoteObjectChildren(parent)) {
      // We should probably render the dummy "No Properties" element again.
      replaceRemoteObjectNode(parent, parent);
    }
  }

  /**
   * @return true if the given node has only the dummy "No Properties" child
   */
  private static boolean hasNoRealRemoteObjectChildren(RemoteObjectNode node) {
    JsonArray<RemoteObjectNode> children = node.getChildren();
    return (children.size() == 1 && children.get(0).getRemoteObject() == null);
  }

  private void handleOnRemoteObjectPropertiesResponse(OnRemoteObjectPropertiesResponse response) {
    JsonArray<RemoteObjectNode> parentNodes = remoteObjectNodes.get(response.getObjectId());
    if (parentNodes == null) {
      return;
    }
    for (int i = 0, n = parentNodes.size(); i < n; ++i) {
      handleOnRemoteObjectPropertiesResponse(response, parentNodes.get(i));
    }
    expandCachedPaths();
    // Re-schedule the treeHeightRestorer timer.
    saveTreeMinHeight();

    if (recentEditedNode != null) {
      // A user may have been in the process of adding a new property, but was unable to do so
      // until we fetch all properties of the remote object from the debugger.
      for (int i = 0, n = parentNodes.size(); i < n; ++i) {
        RemoteObjectNode parentNode = parentNodes.get(i);
        if (parentNode == recentEditedNode) {
          recentEditedNode = null;
          if (!nodeLabelMutator.isMutating()) {
            addMutableChild(parentNode);
          }
          break;
        }
      }
    }
  }

  private void handleOnRemoteObjectPropertiesResponse(OnRemoteObjectPropertiesResponse response,
      RemoteObjectNode parentNode) {
    if (!parentNode.shouldRequestChildren()) {
      // We already processed a request for this node.
      return;
    }

    JsonArray<PropertyDescriptor> properties = response.getProperties();
    for (int i = 0, n = properties.size(); i < n; ++i) {
      PropertyDescriptor property = properties.get(i);
      boolean isGetterOrSetter =
          (property.getGetterFunction() != null || property.getSetterFunction() != null);
      if (isGetterOrSetter) {
        if (property.getGetterFunction() != null) {
          RemoteObjectNode child = RemoteObjectNode.createGetterProperty(
              property.getName(), property.getGetterFunction());
          appendNewNode(parentNode, child);
        }
        if (property.getSetterFunction() != null) {
          RemoteObjectNode child = RemoteObjectNode.createSetterProperty(
              property.getName(), property.getSetterFunction());
          appendNewNode(parentNode, child);
        }
      } else if (property.getValue() != null) {
        RemoteObjectNode child =
            new RemoteObjectNode.Builder(property.getName(), property.getValue())
                .setWasThrown(property.wasThrown())
                .setDeletable(property.isConfigurable())
                .setWritable(property.isWritable())
                .setEnumerable(property.isEnumerable())
                .build();
        appendNewNode(parentNode, child);
      }
    }

    parentNode.setAllChildrenRequested();

    // Repaint the node.
    pathsToExpand.addAll(tree.replaceSubtree(parentNode, parentNode, false));
  }

  private void appendNewNode(RemoteObjectNode parent, RemoteObjectNode child) {
    remoteObjectNodes.put(child);
    parent.addChild(child);
  }

  private void handleOnRemoteObjectPropertyChanged(OnRemoteObjectPropertyChanged response) {
    JsonArray<RemoteObjectNode> parentNodes = remoteObjectNodes.get(response.getObjectId());
    if (parentNodes == null) {
      return;
    }

    boolean shouldRefreshProperties =
        (response.wasThrown() || (response.isValueChanged() && response.getValue() == null));

    for (int i = 0, n = parentNodes.size(); i < n; ++i) {
      RemoteObjectNode parent = parentNodes.get(i);
      if (shouldRefreshProperties) {
        // Just refresh all properties of this object.
        if (parent == recentEditedNode) {
          recentEditedNode = null;
        }
        RemoteObjectNode newParent =
            new RemoteObjectNode.Builder(parent.getName(), parent.getRemoteObject(), parent)
                .build();
        replaceRemoteObjectNode(parent, newParent);
        continue;
      }

      RemoteObjectNode child = parent.getFirstChildByName(response.getOldName());
      if (child == null) {
        // A new property was added.
        if (response.getNewName() != null) {
          RemoteObjectNode newChild =
              new RemoteObjectNode.Builder(response.getNewName(), response.getValue()).build();
          parent.addChild(newChild);
          replaceRemoteObjectNode(parent, parent);

          // Continue adding a new property with editing it's value.
          if (parent == recentEditedNode) {
            recentEditedNode = null;
            if (!nodeLabelMutator.isMutating() && newChild.getRenderedTreeNode() != null) {
              contextMenuController.enterEditPropertyValue(newChild.getRenderedTreeNode());
            }
          }
        }
      } else if (response.getNewName() == null) {
        // The property was removed.
        removeRemoteObjectNode(child);
      } else {
        RemoteObject newValue =
            response.isValueChanged() ? response.getValue() : child.getRemoteObject();
        RemoteObjectNode newChild =
            new RemoteObjectNode.Builder(response.getNewName(), newValue, child).build();

        // We could be replacing onto an existing child. If so, delete it first.
        if (!StringUtils.equalNonEmptyStrings(response.getOldName(), response.getNewName())) {
          RemoteObjectNode oldNewChild = parent.getFirstChildByName(response.getNewName());
          if (oldNewChild != null) {
            removeRemoteObjectNode(oldNewChild);
          }
        }

        replaceRemoteObjectNode(child, newChild);

        // Collapse the selection to the recently edited node.
        if (child == recentEditedNode) {
          recentEditedNode = null;          
          tree.getSelectionModel().selectSingleNode(newChild);
        }
      }
    }
  }

  private void replaceRemoteObjectNodesCache(final RemoteObjectNodeCache newCache) {
    // Tear down objects from the old cache first.
    remoteObjectNodes.iterate(new RemoteObjectNodeCache.IterationCallback() {
      @Override
      public void onIteration(String key, JsonArray<RemoteObjectNode> values) {
        for (int i = 0, n = values.size(); i < n; ++i) {
          RemoteObjectNode node = values.get(i);
          if (!newCache.contains(node)) {
            node.teardown();
          }
        }
      }
    });
    remoteObjectNodes = newCache;
  }

  private RemoteObjectNodeCache collectAllChildren() {
    final RemoteObjectNodeCache result = new RemoteObjectNodeCache();

    RemoteObjectNode root = tree.getModel().getRoot();
    if (root != null) {
      Tree.iterateDfs(root, tree.getModel().getDataAdapter(), new Tree.Visitor<RemoteObjectNode>() {

        @Override
        public boolean shouldVisit(RemoteObjectNode node) {
          return true; // Visit all nodes.
        }

        @Override
        public void visit(RemoteObjectNode node, boolean willVisitChildren) {
          result.put(node);
        }
      });
    }

    return result;
  }

  private boolean handleOnDblClick(Element target) {
    TreeNodeElement<RemoteObjectNode> nodeElement = tree.getNodeFromElement(target);
    if (nodeElement == null) {
      return false;
    }

    RemoteObjectNode node = nodeElement.getData();
    if (node.hasChildren()) {
      // The default DblClick will expand/collapse the node.
      return false;
    }

    if (nodeRenderer.getAncestorPropertyNameElement(target) != null) {
      return contextMenuController.enterRenameProperty(nodeElement);
    } else if (nodeRenderer.getAncestorPropertyValueElement(target) != null) {
      return contextMenuController.enterEditPropertyValue(nodeElement);
    }

    return false;
  }

  private void handleOnEvaluateExpressionResponse(OnEvaluateExpressionResponse response) {
    RemoteObjectNode root = tree.getModel().getRoot();
    if (root == null) {
      return;
    }

    String expression = response.getExpression();
    RemoteObject result = response.getResult();

    JsonArray<RemoteObjectNode> children = root.getChildren();
    for (int i = 0, n = children.size(); i < n; ++i) {
      RemoteObjectNode child = children.get(i);
      if (!child.isTransient() && child.getName().equals(expression)) {
        RemoteObjectNode newChild = new RemoteObjectNode.Builder(expression, result, child)
            .setWasThrown(response.wasThrown())
            .build();
        // Repaint the subtree.
        replaceRemoteObjectNode(child, newChild);
      }
    }

    String deferredExpression = deferredEvaluations.remove(expression);
    if (!StringUtils.isNullOrEmpty(deferredExpression)) {
      debuggerState.evaluateExpression(deferredExpression);
    }
  }

  void reevaluateRootChildren() {
    // Clear cached data (we are about to refresh everything anyway).
    deferredEvaluations = JsonCollections.createMap();

    RemoteObjectNode root = tree.getModel().getRoot();
    if (root == null) {
      // Nothing to do.
      return;
    }

    JsonArray<RemoteObjectNode> children = root.getChildren();

    if (debuggerState.isActive()) {
      for (int i = 0, n = children.size(); i < n; ++i) {
        RemoteObjectNode child = children.get(i);
        if (!child.isTransient()) {
          debuggerState.evaluateExpression(child.getName());
        }
      }
    } else {
      // Set all expressions' values undefined.
      RemoteObjectNode newRoot = RemoteObjectNode.createRoot();

      for (int i = 0, n = children.size(); i < n; ++i) {
        RemoteObjectNode oldChild = children.get(i);
        RemoteObjectNode newChild = new RemoteObjectNode.Builder(
            oldChild.getName(), DebuggerApiTypes.UNDEFINED_REMOTE_OBJECT, oldChild).build();
        newRoot.addChild(newChild);
      }

      setRoot(newRoot);
    }
  }


  private void appendRootChild(String expression) {
    String trimmedExpression = StringUtils.trimNullToEmpty(expression);
    if (StringUtils.isNullOrEmpty(trimmedExpression)) {
      return;
    }

    RemoteObjectNode root = tree.getModel().getRoot();
    if (root == null) {
      root = RemoteObjectNode.createRoot();
    }

    RemoteObjectNode lastChild = root.getLastChild();
    RemoteObjectNode newChild =
        new RemoteObjectNode.Builder(trimmedExpression, DebuggerApiTypes.UNDEFINED_REMOTE_OBJECT)
            .setOrderIndex(lastChild == null ? 0 : lastChild.getOrderIndex() + 1)
            .build();
    root.addChild(newChild);

    // Repaint the root.
    setRoot(root);

    debuggerState.evaluateExpression(trimmedExpression);
  }
}
