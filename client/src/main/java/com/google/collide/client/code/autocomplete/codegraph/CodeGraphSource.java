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

package com.google.collide.client.code.autocomplete.codegraph;

import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.codeunderstanding.CubeData;
import com.google.collide.client.codeunderstanding.CubeDataUpdates;
import com.google.collide.client.codeunderstanding.CubeUpdateListener;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.CodeGraph;
import com.google.collide.dto.ImportAssociation;
import com.google.collide.dto.InheritanceAssociation;
import com.google.collide.dto.TypeAssociation;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.common.annotations.VisibleForTesting;

/**
 * Controls codegraph update process. Sends requests to the frontend, processes
 * the response and notifies the client if code graph has been updated.
 *
 */
class CodeGraphSource implements CubeUpdateListener {

  private final CubeClient cubeClient;
  private final Runnable updateListener;
  private boolean isPaused = true;

  /**
   * Flag that shows that instance received updates since last
   * {@link #constructCodeGraph()} invocation.
   */
  private boolean hasUpdate;

  CodeGraphSource(CubeClient cubeClient, Runnable updateListener) {
    this.cubeClient = cubeClient;
    this.updateListener = updateListener;
    cubeClient.addListener(this);
  }

  @VisibleForTesting
  public boolean hasUpdate() {
    return hasUpdate;
  }

  CodeGraph constructCodeGraph() {
    CubeData data = cubeClient.getData();
    hasUpdate = false;

    CodeGraphImpl result = CodeGraphImpl.make();
    result.setCodeBlockMap(JsoStringMap.<CodeBlock>create());
    result.setInheritanceAssociations(JsoArray.<InheritanceAssociation>create());
    result.setTypeAssociations(JsoArray.<TypeAssociation>create());
    result.setImportAssociations(JsoArray.<ImportAssociation>create());

    CodeGraph fullGraph = data.getFullGraph();
    CodeGraph workspaceTree = data.getWorkspaceTree();
    CodeBlock fileTree = data.getFileTree();
    CodeGraph libsSubgraph = data.getLibsSubgraph();

    if (fullGraph != null) {
      mergeCodeGraph(fullGraph, result);
    }
    if (workspaceTree != null) {
      mergeCodeGraph(workspaceTree, result);
    }
    if (fileTree != null) {
      result.getCodeBlockMap().put(fileTree.getId(), fileTree);
    }
    if (libsSubgraph != null) {
      mergeCodeGraph(libsSubgraph, result);
    }
    return result;
  }

  private void mergeCodeGraph(CodeGraph from, CodeGraphImpl to) {
    to.getCodeBlockMap().putAll(from.getCodeBlockMap());
    if (from.getInheritanceAssociations() != null) {
      to.getInheritanceAssociations().addAll(from.getInheritanceAssociations());
    }
    if (from.getTypeAssociations() != null) {
      to.getTypeAssociations().addAll(from.getTypeAssociations());
    }
    if (from.getImportAssociations() != null) {
      to.getImportAssociations().addAll(from.getImportAssociations());
    }
  }

  void setPaused(boolean paused) {
    isPaused = paused;
  }

  void cleanup() {
    cubeClient.removeListener(this);
  }

  @Override
  public void onCubeResponse(CubeData data, CubeDataUpdates updates) {
    hasUpdate = true;
    if (!isPaused) {
      updateListener.run();
    }
  }
}
