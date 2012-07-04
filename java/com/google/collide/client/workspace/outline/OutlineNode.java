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

import com.google.collide.client.documentparser.AsyncParser;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Data class containing information about file structure node and some renderer-specific
 * information.
 *
 */
public class OutlineNode implements AsyncParser.LineAware {

  public static final AnchorType OUTLINE_NODE_ANCHOR_TYPE = AnchorType.create(
      OutlineNode.class, "outlineNode");

  /**
   * Types of structure nodes.
   */
  public enum OutlineNodeType {

    ROOT, CLASS, FUNCTION, FIELD, CSS_CLASS
  }

  private final OutlineNodeType type;
  private final JsonArray<OutlineNode> children;
  private final OutlineNode parent;
  private final int lineNumber;
  private final int column;
  private boolean enabled;
  private Anchor anchor;

  private String name;
  private TreeNodeElement<OutlineNode> renderedNode;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return The associated rendered {@link TreeNodeElement}. If there is no
   *         tree node element rendered yet, then {@code null} is returned.
   */
  public TreeNodeElement<OutlineNode> getRenderedTreeNode() {
    return renderedNode;
  }

  public OutlineNodeType getType() {
    return type;
  }

  public OutlineNode(String name, OutlineNodeType type, OutlineNode parent, int lineNumber,
      int column) {
    Preconditions.checkNotNull(type);
    this.parent = parent;
    this.lineNumber = lineNumber;
    this.column = column;
    this.name = name;
    this.type = type;
    this.children = JsonCollections.createArray();
  }

  /**
   * Associates this FileTreeNode with the supplied {@link TreeNodeElement} as
   * the rendered node in the tree. This allows us to go from model -> rendered
   * tree element in order to reflect model mutations in the tree.
   */
  public void setRenderedTreeNode(TreeNodeElement<OutlineNode> renderedNode) {
    this.renderedNode = renderedNode;
  }

  public JsonArray<OutlineNode> getChildren() {
    return children;
  }

  public OutlineNode getParent() {
    return parent;
  }

  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  public int getColumn() {
    return column;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Anchor getAnchor() {
    return anchor;
  }

  public void setAnchor(Anchor anchor) {
    this.anchor = anchor;
  }
}
