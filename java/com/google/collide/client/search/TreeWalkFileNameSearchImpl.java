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

package com.google.collide.client.search;

import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.FileTreeModel;
import com.google.collide.client.workspace.FileTreeNode;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Implements filename search by walking the file tree until enough results are
 * found.
 *
 *
 */
public class TreeWalkFileNameSearchImpl implements FileNameSearch {

  public static TreeWalkFileNameSearchImpl create() {
    return new TreeWalkFileNameSearchImpl();
  }
  
  private FileTreeModel treeModel;

  protected TreeWalkFileNameSearchImpl() {
    treeModel = null; // must set this externally
  }
  
  /**
   * Sets the file tree model to search
   */
  @Override
  public void setFileTreeModel(FileTreeModel model) {
    treeModel = model;
  }

  @Override
  public JsonArray<PathUtil> getMatches(RegExp query, int maxResults) {
    return getMatchesRelativeToPath(PathUtil.WORKSPACE_ROOT, query, maxResults);
  }

  @Override
  public JsonArray<PathUtil> getMatchesRelativeToPath(
      PathUtil searchPath, RegExp query, int maxResults) {

    JsonArray<PathUtil> results = JsonCollections.createArray();
    if (treeModel == null || maxResults < 0) {
      return results;
    }
    
    FileTreeNode root = treeModel.getWorkspaceRoot();
    if (root == null) {
      return results;
    }

    recurseTree(searchPath, root, query, maxResults, results);
    return results;
  }

  /**
   * Recurse a file tree node to find any matching children
   *
   * @param searchPath A relative path to restrict the search to
   * @param node parent tree node
   * @param query search query
   * @param maxResults max results
   * @param results the current results set
   */
  protected void recurseTree(PathUtil searchPath, FileTreeNode node, RegExp query, int maxResults,
      JsonArray<PathUtil> results) {
    
    boolean isChildOfSearchPath = searchPath.containsPath(node.getNodePath());
    
    JsonArray<FileTreeNode> children = node.getUnifiedChildren();
    for (int i = 0;
        i < children.size() && (results.size() < maxResults || maxResults == RETURN_ALL_RESULTS);
        i++) {
      if (children.get(i).isDirectory()) {
        recurseTree(searchPath, children.get(i), query, maxResults, results);
        // early-out, if finished recursing the search directory
        if (searchPath.equals(children.get(i).getNodePath())) {
          return;
        }
      } else if (isChildOfSearchPath && query.test(children.get(i).getName())) {
        results.add(children.get(i).getNodePath());
      }
    }
  }
}
