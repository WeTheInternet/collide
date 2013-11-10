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
import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeNode;
import com.google.collide.client.workspace.MockOutgoingController;
import com.google.collide.dto.DirInfo;
import com.google.collide.json.shared.JsonArray;
import com.google.common.collect.ImmutableList;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Tests to ensure the search file indexer returns correct results
 */
public class TreeWalkFileNameSearchImplTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return SearchTestUtils.BUILD_MODULE_NAME;
  }

  private RegExp regex(String query) {
    return RegExp.compile(query, "i");
  }

  public void testNoMatches() {
    FileNameSearch indexer = TreeWalkFileNameSearchImpl.create();
    
    // Setup the file tree model with the simple tree (a list of hello files)
    FileTreeModel model = getFileTree(buildSimpleTree());
    indexer.setFileTreeModel(model);

    // Verify no matches
    JsonArray<PathUtil> results = indexer.getMatches(regex("nothello"), 5);
    assertEquals(0, results.size());
  }

  public void testMaxResults() {
    FileNameSearch indexer = TreeWalkFileNameSearchImpl.create();
    
    // Setup the file tree model with the simple tree (a list of hello files)
    FileTreeModel model = getFileTree(buildSimpleTree());
    indexer.setFileTreeModel(model);
    
    // Verify no matches
    JsonArray<PathUtil> results = indexer.getMatches(regex("hello"), 2);
    assertEquals(2, results.size());
    
    results = indexer.getMatches(regex("hello"), 3);
    assertEquals(3, results.size());
    
    
    results = indexer.getMatches(regex("hello"), FileNameSearch.RETURN_ALL_RESULTS);
    assertEquals(4, results.size());
  }

  public void testCorrectMatchesFound() {
    FileNameSearch indexer = TreeWalkFileNameSearchImpl.create();

    // Setup the file tree model with the simple tree (a list of hello files)
    FileTreeModel model = getFileTree(buildComplexTree());
    indexer.setFileTreeModel(model);
    
    // Verify correct matches
    JsonArray<PathUtil> results = indexer.getMatches(regex("world"), 2);
    assertEquals(2, results.size());
    assertContainsPaths(ImmutableList.of("/src/world.js", "/src/world.html"), results);

    results = indexer.getMatches(regex("data"), 4);
    assertEquals(1, results.size());
    assertEquals("data.txt", results.get(0).getBaseName());
  }

  public void testSameFileNames() {
    FileNameSearch indexer = TreeWalkFileNameSearchImpl.create();

    // Setup the file tree model with the simple tree (a list of hello files)
    FileTreeModel model = getFileTree(buildComplexTree());
    indexer.setFileTreeModel(model);

    // Verify that two results are returned from two different directories
    JsonArray<PathUtil> results = indexer.getMatches(regex("hello"), 2);
    assertEquals(2, results.size());
    
    // Returns the proper two files
    assertContainsPaths(ImmutableList.of("/hello.js", "/src/hello.html"), results);
  }
  
  public void testFindFilesRelativeToPath() {
    FileNameSearch indexer = TreeWalkFileNameSearchImpl.create();

    // Setup the file tree model with the simple tree (a list of hello files)
    FileTreeModel model = getFileTree(buildComplexTree());
    indexer.setFileTreeModel(model);

    // Verify that two results are returned from two different directories
    JsonArray<PathUtil> results =
        indexer.getMatchesRelativeToPath(new PathUtil("/src"), regex("hello"), 2);
    assertEquals(1, results.size());

    // Returns the proper two files
    assertContainsPaths(ImmutableList.of("/src/hello.html"), results);
  }

  public void testNoCrashWithInvalidModel() {
    FileNameSearch indexer = TreeWalkFileNameSearchImpl.create();

    // Check null filetree
    indexer.setFileTreeModel(null);
    
    JsonArray<PathUtil> results = indexer.getMatches(regex("haha"), 4);
    assertEquals(0, results.size());
    
    // Crap file tree so we can test no crashing
    FileTreeModel model = new FileTreeModel(new MockOutgoingController());
    indexer.setFileTreeModel(model);
    
    results = indexer.getMatches(regex("haha"), 4);
    assertEquals(0, results.size());
  }
  
  /**
   * Verifies that all values in the {@code actual} array are present in the
   * {@code expected}. Also checks that arrays are the same length
   */
  private void assertContainsPaths(ImmutableList<String> expected, JsonArray<PathUtil> actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < actual.size(); i++) {
      if (!expected.contains(actual.get(i).getPathString())) {
        fail("Actual contains " + actual.get(i).getPathString()
            + " which is not present in expected");
      }
    }
  }
  
  /**
   * Creates a file tree model given a directory structure
   */
  private FileTreeModel getFileTree(DirInfo dir) {
    FileTreeNode root = FileTreeNode.transform(dir);

    FileTreeModel model = new FileTreeModel(new MockOutgoingController());
    model.replaceNode(PathUtil.WORKSPACE_ROOT, root, "1");
    return model;
  }

  private final native DirInfo buildSimpleTree() /*-{
    return {
        // Root node is magic
        nodeType : @com.google.collide.dto.TreeNodeInfo::DIR_TYPE,
        id : "1",
        originId : "1",
        name : "root",
        files : [
            {
                nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                id : "5",
                originId : "5",
                name : "hello.js",
                rootId : "2",
                path : "/hello.js",
                size : "1234"
            },
            {
                nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                id : "6",
                originId : "6",
                name : "hello2.js",
                rootId : "2",
                path : "/hello2.js",
                size : "1234"
            },
            {
                nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                id : "7",
                originId : "7",
                name : "hello3.js",
                rootId : "2",
                path : "/hello3.js",
                size : "1234"
            },
            {
                nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                id : "8",
                originId : "8",
                name : "hello4.js",
                rootId : "2",
                path : "/hello4.js",
                size : "1234"
            }
        ],
        isComplete : true,
        subDirectories : []
    };
  }-*/;
  
  public final native DirInfo buildComplexTree() /*-{
    return {
        // Root node is magic
        nodeType : @com.google.collide.dto.TreeNodeInfo::DIR_TYPE,
        id : "1",
        originId : "1",
        name : "root",
        files : [
          {
              nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
              id : "5",
              originId : "5",
              name : "hello.js",
              rootId : "2",
              path : "/hello.js",
              size : "1234"
          }
        ],
        isComplete : true,
        subDirectories : [
            {
                nodeType : @com.google.collide.dto.TreeNodeInfo::DIR_TYPE,
                id : "2",
                originId : "2",
                name : "src",
                path : "/src",
                files : [
                    {
                        nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                        id : "7",
                        originId : "7",
                        name : "world.js",
                        rootId : "2",
                        path : "/src/world.js",
                        size : "1234"
                    },
                    {
                        nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                        id : "3",
                        originId : "3",
                        name : "hello.html",
                        rootId : "2",
                        path : "/src/hello.html",
                        size : "1234"
                    },
                    {
                        nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                        id : "8",
                        originId : "8",
                        name : "world.html",
                        rootId : "2",
                        path : "/src/world.html",
                        size : "1234"
                    }
                ],
                isComplete : true,
                subDirectories : []
            },
            {
                nodeType : @com.google.collide.dto.TreeNodeInfo::DIR_TYPE,
                id : "4",
                originId : "4",
                name : "res",
                path : "/res",
                files : [
                  {
                      nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                      id : "6",
                      originId : "5",
                      name : "data.txt",
                      rootId : "4",
                      path : "/res/data.txt",
                      size : "1234"
                  }
                ],
                isComplete : true,
                subDirectories : []
            }
        ]
    };
  }-*/;
}
