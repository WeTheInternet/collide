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

import collide.client.filetree.FileTreeModel;

import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Listens to the file model changes and handles building and searching the file
 * index
 *
 *
 */
public interface FileNameSearch {
  
  /**
   * When passed in for maxResults this will return all results in the tree.
   */
  public static final int RETURN_ALL_RESULTS = 0;
  
  /**
   * Retrieves matches from the index given a query
   *
   * @param query The user query
   * @param maxResults The maximum number of results to return
   *
   * @return An array of file paths which match the query
   */
  JsonArray<PathUtil> getMatches(RegExp query, int maxResults);

  /**
   * Retrieves matches from the index that are at or below the supplied path
   *
   * @param searchPath The path to restrict the search to
   *
   * @return An array of file paths which match the query
   */
  JsonArray<PathUtil> getMatchesRelativeToPath(PathUtil searchPath, RegExp query, int maxResults);

  /**
   * Sets the file tree model to search
   */
  void setFileTreeModel(FileTreeModel model);
  
}
