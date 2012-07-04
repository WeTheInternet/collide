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

import com.google.collide.client.ui.tree.NodeDataAdapter;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.json.shared.JsonArray;

/**
 * A {@link NodeDataAdapter} for the {@link RemoteObjectNode}.
 */
class RemoteObjectNodeDataAdapter implements NodeDataAdapter<RemoteObjectNode> {

  @Override
  public int compare(RemoteObjectNode a, RemoteObjectNode b) {
    return a.compareTo(b);
  }

  @Override
  public boolean hasChildren(RemoteObjectNode data) {
    return data.hasChildren();
  }

  @Override
  public JsonArray<RemoteObjectNode> getChildren(RemoteObjectNode data) {
    return data.getChildren();
  }

  @Override
  public String getNodeId(RemoteObjectNode data) {
    return data.getNodeId();
  }

  @Override
  public String getNodeName(RemoteObjectNode data) {
    return data.getName();
  }

  @Override
  public RemoteObjectNode getParent(RemoteObjectNode data) {
    return data.getParent();
  }

  @Override
  public TreeNodeElement<RemoteObjectNode> getRenderedTreeNode(RemoteObjectNode data) {
    return data.getRenderedTreeNode();
  }

  @Override
  public void setNodeName(RemoteObjectNode data, String name) {
    data.setName(name);
  }

  @Override
  public void setRenderedTreeNode(RemoteObjectNode data,
      TreeNodeElement<RemoteObjectNode> renderedNode) {
    data.setRenderedTreeNode(renderedNode);
  }

  @Override
  public RemoteObjectNode getDragDropTarget(RemoteObjectNode data) {
    return data;
  }

  @Override
  public JsonArray<String> getNodePath(RemoteObjectNode data) {
    return NodeDataAdapter.PathUtils.getNodePath(this, data);
  }

  @Override
  public RemoteObjectNode getNodeByPath(RemoteObjectNode root, JsonArray<String> relativeNodePath) {
    // TODO: Not yet implemented. Also not used by the debug tree.
    return null;
  }
}
