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

import java.util.Comparator;

import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.FileInfo;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.dto.client.DtoClientImpls.DirInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.FileInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.TreeNodeInfoImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.annotations.VisibleForTesting;

/**
 * The Model for the FileTree. This data structure is constructed (without
 * copying) from the DTO based DirInfo workspace file tree.
 *
 *  We visit our workspace file tree that we JSON.parsed into existence, and
 * install back references to their parent nodes. We simply eagerly mutate the
 * same object graph (not copy it), and when needed we can cast each node to a
 * {@link FileTreeNode} to get access to this API.
 *
 */
public class FileTreeNode extends TreeNodeInfoImpl {
  private static final Comparator<DirInfo> dirSortFunction = new Comparator<DirInfo>() {
    @Override
    public int compare(DirInfo a, DirInfo b) {
      return compareNames(a, b);
    }
  };

  private static final Comparator<FileInfo> fileSortFunction =
      new Comparator<FileInfo>() {
        @Override
        public int compare(FileInfo a, FileInfo b) {
          return compareNames(a, b);
        }
      };

  /**
   * Converts the DTO object graph into one suitable to act as the model for our
   * FileTree.
   *
   * @param workspaceFiles {@link DirInfo} that represents the files in a
   *        workspace. This is literally what we JSON.parsed() off the wire.
   * @return (@link FileTreeNode} that is the result of mutating the supplied
   *         {@link DirInfo} tree
   */
  public static FileTreeNode transform(DirInfo workspaceFiles) {
    transformImpl(workspaceFiles);
    return ((DirInfoImpl) workspaceFiles).cast();
  }

  static native FileTreeNode installBackRef(DirInfo parent, TreeNodeInfo child) /*-{
    child.__parentRef = parent;
    return child;
  }-*/;

  private static int compareNames(TreeNodeInfo a, TreeNodeInfo b) {
    return a.getName().compareTo(b.getName());
  }

  private static FileTreeNode getChildNode(FileTreeNode parentNode, String name) {
    if (!parentNode.isDirectory()) {
      return null;
    }

    DirInfoImpl asDirInfo = parentNode.cast();
    JsonArray<DirInfo> childDirs = asDirInfo.getSubDirectories();
    for (int i = 0, n = childDirs.size(); i < n; i++) {
      if (childDirs.get(i).getName().equals(name)) {
        return (FileTreeNode) childDirs.get(i);
      }
    }

    JsonArray<FileInfo> childFiles = asDirInfo.getFiles();
    for (int i = 0, n = childFiles.size(); i < n; i++) {
      if (childFiles.get(i).getName().equals(name)) {
        return (FileTreeNode) childFiles.get(i);
      }
    }

    return null;
  }

  private static void transformImpl(DirInfo node) {
    JsonArray<DirInfo> subDirsArray = node.getSubDirectories();
    for (int i = 0, n = subDirsArray.size(); i < n; i++) {
      DirInfo childDir = subDirsArray.get(i);
      installBackRef(node, childDir);
      transformImpl(childDir);
    }

    JsonArray<FileInfo> files = node.getFiles();
    for (int i = 0, n = files.size(); i < n; i++) {
      FileInfo childFile = files.get(i);
      installBackRef(node, childFile);
    }
  }

  protected FileTreeNode() {
  }

  public final void addChild(FileTreeNode newNode) {
    assert (isDirectory()) : "You are only allowed to add new files and folders to a directory!";

    DirInfoImpl parentDir = this.cast();

    // Install the new node in our model by adding it to the child list of the
    // parent.
    if (newNode.isFile()) {
      parentDir.getFiles().add(newNode.<FileInfoImpl>cast());
    } else if (newNode.isDirectory()) {
      parentDir.getSubDirectories().add(newNode.<DirInfoImpl>cast());
    } else {
      throw new IllegalArgumentException(
          "We somehow made a new node that was neither a directory not a file.");
    }

    // Install a backreference to the parent to enable path resolution.
    installBackRef(parentDir, newNode);
  }

  /**
   * Finds a node with the given path relative to this node.
   *
   * @return the node if it was found, or null if it was not found or if this
   *         node is not a directory
   */
  public final FileTreeNode findChildNode(PathUtil path) {
    FileTreeNode closest = findClosestChildNode(path);
    PathUtil absolutePath = PathUtil.concatenate(getNodePath(), path);
    if (closest == null || !absolutePath.equals(closest.getNodePath())) {
      return null;
    }
    return closest;
  }

  /**
   * Finds a node with the given path relative to this node, or the closest parent node if the
   * branch is incomplete (not loaded).
   *
   * @return the node if it was found, or the closest node in the path if the branch is not
   *         complete, or null if the branch is complete and the node is not found
   */
  public final FileTreeNode findClosestChildNode(PathUtil path) {
    FileTreeNode parentNode = this;
    int dirnameComponents = path.getPathComponentsCount() - 1;
    
    if (dirnameComponents < 0) {
      return parentNode;
    }

    for (int i = 0; i < dirnameComponents; i++) {
      if (!parentNode.isComplete()) {
        return parentNode;
      }

      String childName = path.getPathComponent(i);
      parentNode = getChildNode(parentNode, childName);
      if (parentNode == null) {
        return null;
      }
    }

    // The immediate parent is not complete.
    if (!parentNode.isComplete()) {
      return parentNode;
    }

    // We match the very last path component against parentNode, which should
    // now point to the directory containing the requested resource.
    return getChildNode(parentNode, path.getPathComponent(dirnameComponents));
  }

  /**
   * Returns a node for the child of the given name, or null.
   */
  public final FileTreeNode getChildNode(String name) {
    return getChildNode(this, name);
  }

  /**
   * @return Path from the root node to this node.
   */
  public final PathUtil getNodePath() {
    // TODO: Consider caching this somewhere.
    return new PathUtil.Builder()
        .addPathComponents(getNodePathComponents())
        .build();
  }

  private JsonArray<String> getNodePathComponents() {
    JsonArray<String> pathArray = JsonCollections.createArray();

    // We want to always exclude adding a path entry for the root, since the
    // root is an implicit node. Paths should always be implicitly relative to
    // this workspace root.
    for (FileTreeNode node = this; node.getParent() != null; node = node.getParent()) {
      pathArray.add(node.getName());
    }
    pathArray.reverse();

    return pathArray;
  }

  public native final FileTreeNode getParent() /*-{
    return this.__parentRef;
  }-*/;

  /**
   * @return The associated rendered {@link TreeNodeElement}. If there is no
   *         tree node element rendered yet, then {@code null} is returned.
   */
  public final native TreeNodeElement<FileTreeNode> getRenderedTreeNode() /*-{
    return this.__renderedNode;
  }-*/;

  /**
   * @return the sub directories concatenated with the files list, guaranteed to
   *         be sorted and is only computed once (and subsequently cached).
   */
  @VisibleForTesting
  public final JsoArray<FileTreeNode> getUnifiedChildren() {
    assert isDirectory() : "Only directories have children!";
    enusureUnifiedChildren();
    return getUnifiedChildrenImpl();
  }

  public final boolean isDirectory() {
    return getNodeType() == TreeNodeInfo.DIR_TYPE;
  }

  /**
   * Checks whether or not this directory's children have been loaded.
   *
   * @return true if children have been loaded, false if not
   */
  public final boolean isComplete() {
    assert isDirectory() : "Only directories can be complete";
    DirInfoImpl dirView = this.cast();
    return dirView.isComplete();
  }

  /**
   * Checks whether or not the children of this directory have been requested.
   * 
   * @return true if loading, false if not
   */
  public final native boolean isLoading() /*-{
    return !!this.__isLoading;
  }-*/;

  public final boolean isFile() {
    return getNodeType() == TreeNodeInfo.FILE_TYPE;
  }

  /**
   * Removes a child node.
   */
  public final void removeChild(FileTreeNode child) {
    assert (isDirectory()) : "I am not a directory!";

    if (child.isDirectory()) {
      removeChild(child.getName(), this.<DirInfoImpl>cast().getSubDirectories());
    } else {
      removeChild(child.getName(), this.<DirInfoImpl>cast().getFiles());
    }

    invalidateUnifiedChildrenCache();
  }

  /**
   * Removes a node from the subDirectories list that has the same name as the
   * specified targetName.
   */
  public final void removeChild(String targetName, JsonArray<? extends TreeNodeInfo> children) {
    assert (isDirectory()) : "I am not a directory!";

    for (int i = 0, n = children.size(); i < n; i++) {
      if (targetName.equals(children.get(i).getName())) {
        children.remove(i);
        return;
      }
    }
  }

  /**
   * Patches the tree rooted at the current node with an incoming directory sent
   * from the server.
   *
   *  We actually replace the current node in the tree with the incoming node by
   * walking up one directory and replacing the entry in the subDirectories
   * collection.
   *
   * This method is a no-op if the current node is the workspace root.
   */
  public final void replaceWith(FileTreeNode newNode) {
    FileTreeNode parent = getParent();
    if (parent == null) {
      return;
    }

    installBackRef(parent.<DirInfoImpl>cast(), newNode);

    parent.removeChild(this);
    parent.addChild(newNode);
    parent.invalidateUnifiedChildrenCache();
  }

  /**
   * Specifies whether or not this directory's children have been requested.
   */
  public final native void setLoading(boolean isLoading) /*-{
    this.__isLoading = isLoading;
  }-*/;

  /**
   * Associates this FileTreeNode with the supplied {@link TreeNodeElement} as
   * the rendered node in the tree. This allows us to go from model -> rendered
   * tree element in order to reflect model mutations in the tree.
   */
  public final native void setRenderedTreeNode(TreeNodeElement<FileTreeNode> renderedNode) /*-{
    this.__renderedNode = renderedNode;
  }-*/;

  final native boolean hasUnifiedChildren() /*-{
    return !!this.__childrenCache;
  }-*/;

  final native void invalidateUnifiedChildrenCache() /*-{
    this.__childrenCache = null;
  }-*/;

  private native void cacheUnifiedChildren(JsoArray<FileTreeNode> children) /*-{
    this.__childrenCache = children;
  }-*/;

  /**
   * Sorts the directory and file children arrays (in place), and then caches
   * the concatenation of the two.
   */
  private void enusureUnifiedChildren() {
    assert isDirectory() : "Only directories have children!";

    if (!hasUnifiedChildren()) {
      DirInfoImpl dirView = this.cast();
      JsoArray<DirInfo> dirs = (JsoArray<DirInfo>) dirView.getSubDirectories();
      JsoArray<FileInfo> files = (JsoArray<FileInfo>) dirView.getFiles();
      dirs.sort(dirSortFunction);
      files.sort(fileSortFunction);
      cacheUnifiedChildren(JsoArray.concat(dirs, files).<JsoArray<FileTreeNode>>cast());
    }
  }

  private native JsoArray<FileTreeNode> getUnifiedChildrenImpl() /*-{
    return this.__childrenCache;
  }-*/;
}
