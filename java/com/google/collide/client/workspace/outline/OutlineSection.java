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

package com.google.collide.client.workspace.outline;

import collide.client.treeview.Tree;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.WorkspaceNavigationSection;
import com.google.collide.client.workspace.outline.OutlineModel.OutlineModelListener;

/**
 * Navigation panel that allows to quickly navigate through file structure.
 *
 */
public class OutlineSection extends WorkspaceNavigationSection<OutlineSection.View> {

  /**
   * Static factory method for obtaining an instance of the OutlineSection.
   */
  public static OutlineSection create(View view, AppContext appContext, OutlineModel outlineModel,
      OutlineController outlineController) {
    // Create the Tree presenter.
    OutlineNodeRenderer nodeRenderer = new OutlineNodeRenderer(appContext.getResources());
    OutlineNodeDataAdapter nodeDataAdapter = new OutlineNodeDataAdapter();
    Tree<OutlineNode> tree =
        Tree.create(view.treeView, nodeDataAdapter, nodeRenderer, appContext.getResources());
    tree.setTreeEventHandler(outlineController);

    // Instantiate and return the FileTreeSection.
    return new OutlineSection(view, tree, outlineModel);
  }

  private class OutlineModelListenerImpl implements OutlineModelListener {

    @Override
    public void rootChanged(OutlineNode newRoot) {
      tree.replaceSubtree(tree.getModel().getRoot(), newRoot, false);
    }

    @Override
    public void nodeUpdated(final OutlineNode node) {
      // TODO: we should have something like render(node) or even
      //               adapter should be able to "update" rendered node.
      tree.renderTree();
    }

    @Override
    public void rootUpdated() {
      tree.renderTree();
    }
  }

  /**
   * CSS and images used by the OutlineSection.
   */
  public interface Resources extends
      WorkspaceNavigationSection.Resources,
      Tree.Resources,
      OutlineNodeRenderer.Resources {

  }

  /**
   * View for the OutlineSection.
   */
  public static class View extends
      WorkspaceNavigationSection.View<WorkspaceNavigationSection.ViewEvents> {
    final Tree.View<OutlineNode> treeView;

    public View(Resources res) {
      super(res);

      // Instantiate subviews.
      this.treeView = new Tree.View<OutlineNode>(res);

      // Initialize the View.
      setTitle("Code Navigator");
      setStretch(true);
      setBlue(true);
      setContent(treeView.getElement());
      setContentAreaScrollable(true);
    }
  }

  private final Tree<OutlineNode> tree;


  OutlineSection(View view, Tree<OutlineNode> tree, OutlineModel outlineModel) {
    super(view);
    this.tree = tree;
    outlineModel.setListener(new OutlineModelListenerImpl());
  }
}
