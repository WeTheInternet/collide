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

import collide.client.treeview.NodeDataAdapter;
import collide.client.treeview.TreeNodeElement;

import com.google.collide.json.shared.JsonArray;

/**
 * Adapter used by tree component to communicate with data class.
 */
public class OutlineNodeDataAdapter implements NodeDataAdapter<OutlineNode> {

  @Override
  public int compare(OutlineNode a, OutlineNode b) {
    // TODO: implement
    return 0;
  }

  @Override
  public boolean hasChildren(OutlineNode data) {
    JsonArray jsonArray = data.getChildren();
    return jsonArray != null && !jsonArray.isEmpty();
  }

  @Override
  public JsonArray<OutlineNode> getChildren(OutlineNode data) {
    return data.getChildren();
  }

  @Override
  public String getNodeId(OutlineNode data) {
    return data.getName();
  }

  @Override
  public String getNodeName(OutlineNode data) {
    return data.getName();
  }

  @Override
  public OutlineNode getParent(OutlineNode data) {
    return data.getParent();
  }

  @Override
  public TreeNodeElement<OutlineNode> getRenderedTreeNode(OutlineNode data) {
    return data.getRenderedTreeNode();
  }

  @Override
  public void setNodeName(OutlineNode data, String name) {
    data.setName(name);
  }

  @Override
  public void setRenderedTreeNode(OutlineNode data, TreeNodeElement<OutlineNode> renderedNode) {
    data.setRenderedTreeNode(renderedNode);
  }

  @Override
  public OutlineNode getDragDropTarget(OutlineNode data) {
    return data;
  }

  @Override
  public JsonArray<String> getNodePath(OutlineNode data) {
    return NodeDataAdapter.PathUtils.getNodePath(this, data);
  }

  @Override
  public OutlineNode getNodeByPath(OutlineNode root, JsonArray<String> relativeNodePath) {
    // TODO: Not yet implemented. Also not used by the outline tree.
    return null;
  }
}
