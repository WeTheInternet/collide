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
package com.google.collide.client.codeunderstanding;

/**
 * Immutable value object holding information about what fields of
 * {@link CubeData} has been updated.
 */
public class CubeDataUpdates {

  public static final CubeDataUpdates NO_UPDATES =
      new CubeDataUpdates(false, false, false, false, false);

  private final boolean fileTree;

  private final boolean fullGraph;

  private final boolean libsSubgraph;

  private final boolean workspaceTree;

  private final boolean fileReferences;

  public CubeDataUpdates(boolean fileTree, boolean fullGraph, boolean libsSubgraph,
      boolean workspaceTree, boolean fileReferences) {
    this.fileTree = fileTree;
    this.fullGraph = fullGraph;
    this.libsSubgraph = libsSubgraph;
    this.workspaceTree = workspaceTree;
    this.fileReferences = fileReferences;
  }

  public boolean isFileTree() {
    return fileTree;
  }

  public boolean isFullGraph() {
    return fullGraph;
  }

  public boolean isLibsSubgraph() {
    return libsSubgraph;
  }

  public boolean isWorkspaceTree() {
    return workspaceTree;
  }

  public boolean isFileReferences() {
    return fileReferences;
  }

  @Override
  public String toString() {
    return "CubeDataUpdates{" +
        "\n  fileTree=" + fileTree +
        ",\n  fullGraph=" + fullGraph +
        ",\n  libsSubgraph=" + libsSubgraph +
        ",\n  workspaceTree=" + workspaceTree +
        ",\n  fileReferences=" + fileReferences +
        "\n}";
  }

  public static boolean hasUpdates(CubeDataUpdates updates) {
    return updates.fileTree || updates.fullGraph || updates.libsSubgraph || updates.workspaceTree
        || updates.fileReferences;
  }
}
