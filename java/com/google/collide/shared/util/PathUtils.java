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

import com.google.common.base.Preconditions;

/**
 * Utilities for working with workspace paths.
 */
public class PathUtils {

  /**
   * The root path of workspaces.
   */
  public static final String WORKSPACE_ROOT = "/";

  /**
   * A visitor used to visit a path.
   */
  public static interface PathVisitor {
    /**
     * Visits a path.
     *
     * @param path the path string
     * @param name the name of the directory or file at this path
     */
    public abstract void visit(String path, String name);
  }

  /**
   * Normalizes a path by removing trailing slash, unless the path is the root path.
   *
   * @param path the path to the directory or file
   */
  public static String normalizePath(String path) {
    if (WORKSPACE_ROOT.equals(path)) {
      return path;
    }
    if (path.isEmpty()) {
      return WORKSPACE_ROOT;
    }
    int maybeSlash = path.length() - 1;
    return path.charAt(maybeSlash) == '/' ? path.substring(0, maybeSlash) : path;
  }

  /**
   * Walks every directory a path from the rootPath to the target path.
   *
   * @param path the path to walk
   * @param rootPath the root to start walking, inclusive
   * @param visitor the visitor that will be visited along the way
   */
  public static void walk(String path, String rootPath, PathVisitor visitor) {
    // Ensure that the path ends with a separator.
    if (!path.endsWith("/")) {
      path += "/";
    }
    rootPath = normalizePath(rootPath);

    Preconditions.checkArgument(path.startsWith(rootPath),
        "path \"" + path + "\" must be a descendent of rootPath \"" + rootPath + "\"");

    int nextSlash = rootPath.length();
    while (nextSlash != -1) {
      String curPath = path.substring(0, nextSlash);
      String name = "";
      if (nextSlash > 0) {
        int prevSlash = path.lastIndexOf("/", nextSlash - 1);
        name = curPath.substring(prevSlash + 1);
      }

      // Visit the path.
      visitor.visit(curPath, name);

      // Iterate to the next child directory.
      nextSlash = path.indexOf("/", nextSlash + 1);
    }
  }
}
