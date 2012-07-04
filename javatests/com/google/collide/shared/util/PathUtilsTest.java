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

import com.google.collide.shared.util.PathUtils.PathVisitor;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;


/**
 * Tests for {@link PathUtils}.
 *
 */
public class PathUtilsTest extends TestCase {

  /**
   * A mock path visitor that remembers the path that was walked.
   */
  private static class MockPathVisitor implements PathVisitor {

    private final List<String> visitedPaths = new ArrayList<String>();
    private final List<String> visitedNames = new ArrayList<String>();

    public void assertVisitedPaths(String... expected) {
      List<String> expectedList = Lists.newArrayList(expected);
      assertEquals(expectedList, visitedPaths);
    }

    public void assertVisitedNames(String... expected) {
      List<String> expectedList = Lists.newArrayList(expected);
      assertEquals(expectedList, visitedNames);
    }

    @Override
    public void visit(String path, String name) {
      visitedPaths.add(path);
      visitedNames.add(name);
    }
  }

  public void testWalk_rootOnly() {
    MockPathVisitor visitor = walkPath("/", "/");
    visitor.assertVisitedPaths("/");
    visitor.assertVisitedNames("");
  }

  public void testWalk_rootToSubDirectory() {
    MockPathVisitor visitor = walkPath("/a/b/c", "/");
    visitor.assertVisitedPaths("/", "/a", "/a/b", "/a/b/c");
    visitor.assertVisitedNames("", "a", "b", "c");
  }

  public void testWalk_directoryOnly() {
    MockPathVisitor visitor = walkPath("/a/b", "/a/b");
    visitor.assertVisitedPaths("/a/b");
    visitor.assertVisitedNames("b");
  }

  public void testWalk_directoryToSubDirectory() {
    MockPathVisitor visitor = walkPath("/a/b/c/d", "/a/b");
    visitor.assertVisitedPaths("/a/b", "/a/b/c", "/a/b/c/d");
    visitor.assertVisitedNames("b", "c", "d");
  }

  public void testWalk_pathEndsWithSlash() {
    MockPathVisitor visitor = walkPath("/a/b/", "/");
    visitor.assertVisitedPaths("/", "/a", "/a/b");
    visitor.assertVisitedNames("", "a", "b");
  }

  public void testWalk_rootEndsWithSlash() {
    MockPathVisitor visitor = walkPath("/a/b/c", "/a/");
    visitor.assertVisitedPaths("/a", "/a/b", "/a/b/c");
    visitor.assertVisitedNames("a", "b", "c");
  }

  public void testWalk_rootIsNotParent() {
    try {
      walkPath("/a/b/", "/notaparent");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected.
    }
  }

  /**
   * Walks the path from rootPath to path and returns the {@link MockPathVisitor} that visited each
   * path along the way.
   */
  private MockPathVisitor walkPath(String path, String rootPath) {
    MockPathVisitor visitor = new MockPathVisitor();
    PathUtils.walk(path, rootPath, visitor);
    return visitor;
  }

  public void testNormalizePath() {
    assertEquals("/", PathUtils.normalizePath("/"));
    assertEquals("/child", PathUtils.normalizePath("/child"));
    assertEquals("/trailing/slash", PathUtils.normalizePath("/trailing/slash/"));
  }
}
