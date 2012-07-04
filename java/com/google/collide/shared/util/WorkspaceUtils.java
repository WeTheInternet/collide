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

package com.google.collide.shared.util;

import com.google.collide.dto.WorkspaceInfo;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;

import java.util.Comparator;

/**
 * Utilities for managing workspaces.
 */
public class WorkspaceUtils {

  /**
   * A node in a hierarchical tree of workspaces.
   */
  public static class WorkspaceNode {
    private final WorkspaceInfo workspace;
    private final JsonArray<WorkspaceNode> children;

    public WorkspaceNode(WorkspaceInfo workspace) {
      this.workspace = workspace;
      this.children = JsonCollections.createArray();
    }

    public void addChild(WorkspaceNode child) {
      children.add(child);
    }

    public WorkspaceNode getChild(int index) {
      return children.get(index);
    }

    public int getChildCount() {
      return children.size();
    }

    /**
     * Sort the direct children of this node.
     */
    public void sortChildren(Comparator<? super WorkspaceNode> comparator) {
      sortChildren(comparator, false);
    }

    /**
     * Sort the children of this node and optionally sort the entire branch
     * recursively.
     */
    public void sortChildren(Comparator<? super WorkspaceNode> comparator, boolean recursive) {
      // Sort the direct children.
      children.sort(comparator);

      // Recursively sort the branch children.
      if (recursive) {
        for (int i = 0; i < getChildCount(); i++) {
          getChild(i).sortChildren(comparator, true);
        }
      }
    }

    public WorkspaceInfo getWorkspace() {
      return workspace;
    }
  }

  /**
   * Take a flat list of workspaces and organize them into a hierarchy (or set
   * or hierarchies) based on workspace parent IDs. The children within each
   * node are not sorted. This method is O(n).
   * 
   * @param workspaces the list of workspaces to organize
   * @return a array of root workspace nodes
   */
  public static JsonArray<WorkspaceNode> getWorkspaceHierarchy(
      JsonArray<WorkspaceInfo> workspaces) {
    /*
     * Assume all workspaces are root nodes. Create a map of workspace IDs to
     * their associated tree nodes.
     */
    final JsonStringMap<WorkspaceNode> idToNode = JsonCollections.createMap();
    final JsonArray<WorkspaceNode> rootNodes = JsonCollections.createArray();
    for (int i = 0; i < workspaces.size(); i++) {
      WorkspaceInfo value = workspaces.get(i);
      WorkspaceNode node = new WorkspaceNode(value);
      rootNodes.add(node);
      idToNode.put(value.getId(), node);
    }

    /*
     * Iterate over the list of workspaces and add each workspace as a child of
     * its parent.
     */
    int count = rootNodes.size();
    for (int i = 0; i < count; i++) {
      WorkspaceNode node = rootNodes.get(i);
      WorkspaceInfo workspace = node.getWorkspace();
      WorkspaceNode parentNode = idToNode.get(workspace.getParentId());
      if (parentNode != null) {
        parentNode.addChild(node);

        // This node has a parent, so it is not a root node.
        rootNodes.remove(i);
        i--;
        count--;
      }
    }

    return rootNodes;
  }

  /**
   * Take a flat list of workspaces and organize them into a hierarchy (or set
   * or hierarchies) based on workspace parent IDs, then sort children within
   * each node using the specified comparator.
   * 
   * @param workspaces the list of workspaces to organize
   * @return a array of root workspace nodes
   */
  public static JsonArray<WorkspaceNode> getWorkspaceHierarchy(
      JsonArray<WorkspaceInfo> workspaces, final Comparator<WorkspaceInfo> comparator) {
    JsonArray<WorkspaceNode> rootNodes = getWorkspaceHierarchy(workspaces);

    // Wrap the WorkspaceInfo comparator in a WorkspaceNode comparator.
    Comparator<WorkspaceNode> nodeComparator = new Comparator<WorkspaceUtils.WorkspaceNode>() {
      @Override
      public int compare(WorkspaceNode o1, WorkspaceNode o2) {
        return comparator.compare(o1.getWorkspace(), o2.getWorkspace());
      }
    };

    // Sort each root node.
    for (int i = 0; i < rootNodes.size(); i++) {
      rootNodes.get(i).sortChildren(nodeComparator, true);
    }

    // Sort the list of root nodes.
    rootNodes.sort(nodeComparator);

    return rootNodes;
  }

  /**
   * Returns the name of the trunk workspace given the name of the project.
   */
  public static String createTrunkWorkspaceName(String projectName) {
    return projectName + " source";
  }
}
