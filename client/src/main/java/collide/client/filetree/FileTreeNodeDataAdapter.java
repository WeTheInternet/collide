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

import collide.client.treeview.NodeDataAdapter;
import collide.client.treeview.TreeNodeElement;

import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 */
public class FileTreeNodeDataAdapter implements NodeDataAdapter<FileTreeNode> {

  @Override
  public int compare(FileTreeNode a, FileTreeNode b) {

    // Directories are listed before files. This is also implicitly codified in
    // the constants.
    int comparison = a.getNodeType() - b.getNodeType();

    // If they are nodes of different types, we can return.
    if (comparison != 0) {
      return comparison;
    }

    return a.getName().compareTo(b.getName());
  }

  @Override
  public boolean hasChildren(FileTreeNode data) {
    return data.isDirectory() && (!data.isComplete() || data.getUnifiedChildren().size() > 0);
  }

  @Override
  public JsonArray<FileTreeNode> getChildren(FileTreeNode data) {
    return data.isDirectory() ? data.getUnifiedChildren() :
        JsonCollections.<FileTreeNode>createArray();
  }

  @Override
  public String getNodeId(FileTreeNode data) {
    return data.getName();
  }

  @Override
  public String getNodeName(FileTreeNode data) {
    return data.getName();
  }

  @Override
  public FileTreeNode getParent(FileTreeNode data) {
    return data.getParent();
  }

  @Override
  public TreeNodeElement<FileTreeNode> getRenderedTreeNode(FileTreeNode data) {
    return data.getRenderedTreeNode();
  }

  @Override
  public void setNodeName(FileTreeNode data, String key) {
    data.setName(key);
  }

  @Override
  public void setRenderedTreeNode(FileTreeNode data, TreeNodeElement<FileTreeNode> renderedNode) {
    data.setRenderedTreeNode(renderedNode);
  }

  @Override
  public FileTreeNode getDragDropTarget(FileTreeNode data) {
    return data.isDirectory() ? data : data.getParent();
  }

  @Override
  public JsonArray<String> getNodePath(FileTreeNode data) {
    return NodeDataAdapter.PathUtils.getNodePath(this, data);
  }

  @Override
  public FileTreeNode getNodeByPath(FileTreeNode root, JsonArray<String> relativeNodePath) {
    return root.findChildNode(PathUtil.createFromPathComponents(relativeNodePath));
  }
}
