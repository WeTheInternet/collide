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

import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeNode;

import com.google.collide.client.history.Place;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * A controller whose sole responsibility it is to listen for changes on the
 * file tree model and reload the editor contents when a relevant change occurs.
 *
 */
public class EditorReloadingFileTreeListener implements FileTreeModel.TreeModelChangeListener {

  public static EditorReloadingFileTreeListener create(
      Place currentPlace, EditorBundle editorBundle, FileTreeModel fileTreeModel) {

    EditorReloadingFileTreeListener listener =
        new EditorReloadingFileTreeListener(currentPlace, editorBundle, fileTreeModel);
    fileTreeModel.addModelChangeListener(listener);

    return listener;
  }

  private final Place currentPlace;
  private final EditorBundle editorBundle;
  private final FileTreeModel fileTreeModel;

  private EditorReloadingFileTreeListener(
      Place currentPlace, EditorBundle editorBundle, FileTreeModel fileTreeModel) {
    this.currentPlace = currentPlace;
    this.editorBundle = editorBundle;
    this.fileTreeModel = fileTreeModel;
  }

  public void cleanup() {
    fileTreeModel.removeModelChangeListener(this);
  }

  @Override
  public void onNodeAdded(PathUtil parentDirPath, FileTreeNode newNode) {
  }

  @Override
  public void onNodeMoved(
      PathUtil oldPath, FileTreeNode node, PathUtil newPath, FileTreeNode newNode) {
    // if the moved node is currently open in the editor, or is a dir that is
    // somewhere in the path of whatever is open in the editor, then fix the
    // breadcrumbs
    PathUtil editorPath = editorBundle.getBreadcrumbs().getPath();
    if (editorPath == null) {
      return;
    }

    if (oldPath.containsPath(editorPath)) {
      // replace the start of the editor's path with the node's new path
      final PathUtil newEditorPath = PathUtil.concatenate(newPath,
          PathUtil.createExcludingFirstN(editorPath, oldPath.getPathComponentsCount()));

      editorBundle.getBreadcrumbs().setPath(newEditorPath);

      // Wait until DocumentManagerFileTreeModelListener updates the path in the document.
      Scheduler.get().scheduleFinally(new ScheduledCommand() {
        @Override
        public void execute() {
          currentPlace.fireChildPlaceNavigation(
              FileSelectedPlace.PLACE.createNavigationEvent(newEditorPath));
        }
      });
    }
  }

  @Override
  public void onNodesRemoved(JsonArray<FileTreeNode> oldNodes) {
    // do nothing
  }

  @Override
  public void onNodeReplaced(FileTreeNode oldNode, FileTreeNode newNode) {

    if (oldNode == null || !isRelevantPath(oldNode.getNodePath())) {
      return;
    }

    /*
     * TODO: If we synchronously fire the place
     * navigation, eventually we get into a state where the FileTree attempts to
     * autoselect the node, but fails with an assertion error (rushing, so no
     * time to investigate right now)
     */
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        // Reload the file
        currentPlace.fireChildPlaceNavigation(
            FileSelectedPlace.PLACE.createNavigationEvent(editorBundle.getPath(),
                FileSelectedPlace.NavigationEvent.IGNORE_LINE_NUMBER,
                FileSelectedPlace.NavigationEvent.IGNORE_COLUMN, true));
      }
    });
  }

  private boolean isRelevantPath(PathUtil changePath) {
    PathUtil editorPath = editorBundle.getPath();
    return editorPath != null && changePath.containsPath(editorPath);
  }
}
