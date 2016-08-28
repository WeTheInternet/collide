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

import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.CodeGraph;
import com.google.collide.dto.CodeReferences;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;

/**
 * A value object that holds all valuable parts of Cube response.
 */
public class CubeData {

  /**
   * Predefined instance that holds {@code null}'s.
   */
  public static final CubeData EMPTY_DATA = new CubeData(null, null, null, null, null, null);

  private final String filePath;

  private final CodeBlock fileTree;

  private final CodeGraph fullGraph;

  private final CodeGraph libsSubgraph;

  private final CodeGraph workspaceTree;

  private final CodeReferences fileReferences;

  public CubeData(String filePath, CodeBlock fileTree, CodeGraph fullGraph, CodeGraph libsSubgraph,
      CodeGraph workspaceTree, CodeReferences fileReferences) {
    this.filePath = filePath;
    this.fileTree = fileTree;
    this.fullGraph = fullGraph;
    this.libsSubgraph = libsSubgraph;
    this.workspaceTree = workspaceTree;
    this.fileReferences = fileReferences;
  }

  public String getFilePath() {
    return filePath;
  }

  public CodeBlock getFileTree() {
    return fileTree;
  }

  public CodeGraph getFullGraph() {
    return fullGraph;
  }

  public CodeGraph getLibsSubgraph() {
    return libsSubgraph;
  }

  public CodeGraph getWorkspaceTree() {
    return workspaceTree;
  }

  public CodeReferences getFileReferences() {
    return fileReferences;
  }

  @Override
  public String toString() {
    return "CubeData{" +
        "\n  filePath='" + filePath + '\'' +
        ",\n  fileTree=" + objectInfo(fileTree) +
        ",\n  fullGraph=" + objectInfo(fullGraph) +
        ",\n  libsSubgraph=" + objectInfo(libsSubgraph) +
        ",\n  workspaceTree=" + objectInfo(workspaceTree) +
        ",\n  fileReferences=" + objectInfo(fileReferences) +
        "\n}";
  }

  private static String objectInfo(Object object) {
    return object == null ? null : "object";
  }

  private static String objectInfo(CodeReferences object) {
    return (object == null) ? null : "CodeReferences{" +
        "references=" + objectInfo(object.getReferences()) +
        "}";
  }

  private static String objectInfo(CodeGraph object) {
    return (object == null) ? null : "CodeGraph{" +
        "inheritanceAssociations=" + objectInfo(object.getInheritanceAssociations()) +
        ", typeAssociations=" + objectInfo(object.getTypeAssociations()) +
        ", importAssociations=" + objectInfo(object.getImportAssociations()) +
        ", codeBlockMap=" + objectInfo(object.getCodeBlockMap()) +
        "}";
  }

  private static String objectInfo(JsonArray object) {
    return object == null ? null : "JsonArray{size=" + object.size() + "}";
  }

  private static String objectInfo(JsonStringMap object) {
    return object == null ? null : "JsonStringMap{size=" + object.size() + "}";
  }
}
