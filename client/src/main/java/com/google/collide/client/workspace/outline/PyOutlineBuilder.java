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

import static com.google.collide.client.workspace.outline.OutlineNode.OUTLINE_NODE_ANCHOR_TYPE;
import static com.google.collide.client.workspace.outline.OutlineNode.OutlineNodeType.CLASS;
import static com.google.collide.client.workspace.outline.OutlineNode.OutlineNodeType.FUNCTION;
import static com.google.collide.client.workspace.outline.OutlineNode.OutlineNodeType.ROOT;
import static com.google.collide.shared.document.anchor.AnchorManager.IGNORE_LINE_NUMBER;

import com.google.collide.dto.CodeBlock;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;

/**
 * Builder object that build node tree from given code block, and associates
 * nodes with lines in document.
 *
 * TODO: should we use autocompletion data?
 */
public class PyOutlineBuilder extends AbstractOutlineBuilder {

  @Override
  protected OutlineNode buildTree(CodeBlock source) {
    return buildTree(source, null);
  }

  @Override
  protected void maybeBindNode(OutlineNode node, Line line, AnchorManager anchorManager) {
    String text = line.getText();
    String itemName = node.getName();
    int nameColumn = text.indexOf(itemName, node.getColumn());
    if (nameColumn >= 0) {
      node.setEnabled(true);
      Anchor anchor = anchorManager.createAnchor(
          OUTLINE_NODE_ANCHOR_TYPE, line, IGNORE_LINE_NUMBER, nameColumn);
      node.setAnchor(anchor);
    }
  }

  /**
   * Recursively builds node tree.
   *
   * <p>We assume that tree has the following structure: FILE that contains
   * FUNCTIONs and CLASSes that consist of FUNCTIONs. That way we always cut
   * the tree on FUNCTION nodes.
   */
  private OutlineNode buildTree(CodeBlock block, OutlineNode parent) {
    OutlineNode.OutlineNodeType type;
    boolean isLeafNode = false;
    switch (block.getBlockType()) {
      case CodeBlock.Type.VALUE_FILE:
        type = ROOT;
        break;

      case CodeBlock.Type.VALUE_CLASS:
        type = CLASS;
        break;

      case CodeBlock.Type.VALUE_FUNCTION:
        isLeafNode = true;
        type = FUNCTION;
        break;

      default:
        return null;
    }

    OutlineNode result = new OutlineNode(block.getName(), type, parent, block.getStartLineNumber(),
        block.getStartColumn());
    allNodes.add(result);

    // Can't or should not go deeper.
    if (isLeafNode || block.getChildren() == null) {
      return result;
    }

    JsonArray<OutlineNode> resultChildren = result.getChildren();
    JsonArray<CodeBlock> blockChildren = block.getChildren();
    final int size = blockChildren.size();
    for (int i = 0; i < size; i++) {
      CodeBlock item = blockChildren.get(i);
      OutlineNode node = buildTree(item, result);
      if (node == null) {
        continue;
      }
      resultChildren.add(node);
    }

    return result;
  }
}
