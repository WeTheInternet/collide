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

package com.google.collide.client.workspace.outline;

import com.google.collide.dto.CodeBlock;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Abstract implementation that holds shared code.
 */
public abstract class AbstractOutlineBuilder implements OutlineBuilder {

  /**
   * Array of generated nodes, sorted by line number.
   */
  protected JsonArray<OutlineNode> allNodes;

  @Override
  public OutlineNode build(CodeBlock source, Document target) {
    Preconditions.checkNotNull(source);
    Preconditions.checkNotNull(target);
    Preconditions.checkArgument(source.getBlockType() == CodeBlock.Type.VALUE_FILE);

    allNodes = JsonCollections.createArray();

    try {
      OutlineNode result = buildTree(source);
      bindNodes(target);
      return result;
    } finally {
      this.allNodes = null;
    }
  }

  protected abstract OutlineNode buildTree(CodeBlock source);

  /**
   * Bind allNodes to document lines.
   */
  protected void bindNodes(Document target) {
    // At least root node is listed
    Preconditions.checkState(!allNodes.isEmpty());

    AnchorManager anchorManager = target.getAnchorManager();

    LineFinder lineFinder = target.getLineFinder();
    LineInfo cursor = lineFinder.findLine(0);
    int lastLineNumber = target.getLastLineNumber();

    for (int i = 0, size = allNodes.size(); i < size; ++i) {
      OutlineNode node = allNodes.get(i);
      int lineNumber = node.getLineNumber();
      if (lineNumber > lastLineNumber) {
        break;
      }

      // TODO: we should create method that reuses cursor object!
      cursor = lineFinder.findLine(cursor, lineNumber);

      maybeBindNode(node, cursor.line(), anchorManager);
    }
  }

  /**
   * Check that line corresponds to node, and, if so, bind.
   */
  protected abstract void maybeBindNode(OutlineNode node, Line line, AnchorManager anchorManager);
}
