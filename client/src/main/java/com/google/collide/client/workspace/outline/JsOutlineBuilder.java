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
import static com.google.collide.client.workspace.outline.OutlineNode.OutlineNodeType.FIELD;
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
 *
 */
public class JsOutlineBuilder extends AbstractOutlineBuilder {

  static final String PROTOTYPE_NODE_NAME = "prototype";

  @Override
  protected OutlineNode buildTree(CodeBlock source) {
    return buildTree(source, null, false);
  }

  @Override
  protected void maybeBindNode(OutlineNode node, Line line, AnchorManager anchorManager) {
    // TODO: Syntax allows line breaks -> examine several lines.
    String text = line.getText();
    String itemName = node.getName();
    int column = node.getColumn();
    int nameColumn = text.indexOf(itemName, column);
    if (nameColumn < 0) {
      nameColumn = text.lastIndexOf(itemName);
    }
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
   * <p>There are 3 types of input blocks: FILE, FIELD and FUNCTION.
   *
   * <p>We classify this as different types of nodes:<ul>
   * <li> FILE is always a ROOT node
   * <li> FUNCTION is either CLASS node (if it contain other functions)
   * <li> or FUNCTION node (otherwise)
   * <li> FIELD block is classified as FIELD node only if they are
   * inside ROOT or CLASS node.
   * </ul>
   */
  private OutlineNode buildTree(CodeBlock block, OutlineNode parent, boolean processFields) {
    OutlineNode.OutlineNodeType type;
    switch (block.getBlockType()) {
      case CodeBlock.Type.VALUE_FILE:
        type = ROOT;
        break;

      case CodeBlock.Type.VALUE_FIELD:
        if (!processFields) {
          return null;
        }
        type = FIELD;
        break;

      case CodeBlock.Type.VALUE_FUNCTION:
        type = FUNCTION;
        break;

      default:
        return null;
    }

    // Test if this block is  CLASS.
    JsonArray<CodeBlock> blockChildren = block.getChildren();
    if (blockChildren != null) {
      for (CodeBlock codeBlock : blockChildren.asIterable()) {
        if (codeBlock.getBlockType() == CodeBlock.Type.VALUE_FUNCTION) {
          type = CLASS;
          break;
        } else if (PROTOTYPE_NODE_NAME.equals(codeBlock.getName())) {
          type = CLASS;
          break;
        }
      }
    }

    OutlineNode result = new OutlineNode(block.getName(), type, parent, block.getStartLineNumber(),
        block.getStartColumn());
    allNodes.add(result);

    addNodes(blockChildren, result, (type == ROOT) || (type == CLASS));

    return result;
  }

  private void addNodes(JsonArray<CodeBlock> source, OutlineNode dest, boolean processFields) {
    if (source == null) {
      return;
    }

    JsonArray<OutlineNode> children = dest.getChildren();
    for (CodeBlock item : source.asIterable()) {
      if (PROTOTYPE_NODE_NAME.equals(item.getName())) {
        addNodes(item.getChildren(), dest, processFields);
      } else {
        OutlineNode node = buildTree(item, dest, processFields);
        if (node == null) {
          continue;
        }
        children.add(node);
      }
    }
  }
}
