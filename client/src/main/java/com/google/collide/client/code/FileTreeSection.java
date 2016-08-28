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

import collide.client.common.CanRunApplication;
import collide.client.filetree.FileTreeController;
import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeNode;
import collide.client.filetree.FileTreeNodeDataAdapter;
import collide.client.filetree.FileTreeNodeRenderer;
import collide.client.filetree.FileTreeUiController;
import collide.client.treeview.Tree;
import collide.client.util.Elements;

import com.google.collide.client.history.Place;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.ProjectInfo;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;

/**
 * {@link WorkspaceNavigationSection} that is the presenter for the File Tree.
 *
 * This class owns the instances of the {@link FileTreeUiController}, and the {@link Tree} widget.
 *
 */
public class FileTreeSection extends WorkspaceNavigationSection<FileTreeSection.View> {

  /**
   * Static factory method for obtaining an instance of the FileTreeSection.
   */
  public static FileTreeSection create(Place place, FileTreeController<?> controller,
      FileTreeModel fileTreeModel,
      CanRunApplication applicationRunner) {

    // create the view
    FileTreeSection.View view = new FileTreeSection.View(controller.getResources());

    // Create the Tree presenter.
    FileTreeNodeRenderer nodeRenderer =
        FileTreeNodeRenderer.create(controller.getResources());
    FileTreeNodeDataAdapter nodeDataAdapter = new FileTreeNodeDataAdapter();
    Tree<FileTreeNode> tree = Tree.create(
        view.treeView, nodeDataAdapter, nodeRenderer, controller.getResources());

    // Create the UI controller.
    FileTreeUiController treeUiController = FileTreeUiController.create(place,
        fileTreeModel,
        tree,
        controller,
        applicationRunner);

    // attach a file tree menu to the button
    treeUiController.getContextMenuController()
        .createMenuDropdown(Elements.asJsElement(view.menuButton));

    // Instantiate and return the FileTreeSection.
    FileTreeSection fileTreeSection =
        new FileTreeSection(view, tree, treeUiController, fileTreeModel);
    return fileTreeSection;
  }

  public interface Css extends CssResource {
    String root();
  }

  /**
   * CSS and images used by the FileTreeSection.
   */
  public interface Resources
      extends
      WorkspaceNavigationSection.Resources,
      Tooltip.Resources,
      FileTreeNodeRenderer.Resources {

    @Source("FileTreeSection.css")
    Css workspaceNavigationFileTreeSectionCss();
  }

  /**
   * View for the FileTreeSection.
   */
  public static class View extends WorkspaceNavigationSection.View<WorkspaceNavigationSection.ViewEvents> {
    final Element root;
    Tree.View<FileTreeNode> treeView;

    View(Resources res) {
      super(res);

      // Instantiate subviews.
      this.treeView = new Tree.View<FileTreeNode>(res);

      root = Elements.createDivElement(res.workspaceNavigationFileTreeSectionCss().root());
      root.appendChild(treeView.getElement());

      // Initialize the View.
      setTitle("Project Files");
      setStretch(true);
      setShowMenuButton(true);
      setContent(root);
      setContentAreaScrollable(true);
      setUnderlineHeader(true);

      title.addClassName(css.headerLink());
    }
  }

  private class ViewEventsImpl extends WorkspaceNavigationSection.AbstractViewEventsImpl {
    @Override
    public void onTitleClicked() {
      if (projectInfo != null) {
        // was goto landing
      }
    }
  }

  private final Tree<FileTreeNode> tree;
  private final FileTreeUiController fileTreeUiController;
  private ProjectInfo projectInfo;
  private final FileTreeModel fileTreeModel;
  private final boolean isReadOnly = false;
  private final FileTreeModel.TreeModelChangeListener fileTreeModelChangeListener =
      new FileTreeModel.BasicTreeModelChangeListener() {
        @Override
        public void onTreeModelChange() {
          updateProjectTemplatePickerVisibility();
        }
      };

  FileTreeSection(View view, Tree<FileTreeNode> tree, FileTreeUiController fileTreeUiController,
      FileTreeModel fileTreeModel) {
    super(view);
    view.setDelegate(new ViewEventsImpl());

    this.tree = tree;
    this.fileTreeUiController = fileTreeUiController;
    this.fileTreeModel = fileTreeModel;

    fileTreeModel.addModelChangeListener(fileTreeModelChangeListener);

    getView().setTitle("Collide Source");
    updateProjectTemplatePickerVisibility();
  }

  public void cleanup() {
    fileTreeModel.removeModelChangeListener(fileTreeModelChangeListener);
  }

  public Tree<FileTreeNode> getTree() {
    return tree;
  }

  public FileTreeUiController getFileTreeUiController() {
    return fileTreeUiController;
  }

  private void updateProjectTemplatePickerVisibility() {
    if (isReadOnly) {
      return;
    }
    DirInfo root = (DirInfo) fileTreeModel.getWorkspaceRoot();

    /*
     * If it is null, the file tree hasn't been loaded yet so we can't be sure whether or not there
     * are files, don't show it yet
     */
    boolean showPicker =
        root != null && root.getFiles().isEmpty() && root.getSubDirectories().isEmpty();
  }
}
