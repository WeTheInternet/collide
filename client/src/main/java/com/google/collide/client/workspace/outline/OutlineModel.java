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

import javax.annotation.Nullable;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Model object that holds essential navigation structure data data and sends
 * notifications when data is changed.
 */
public class OutlineModel {

  /**
   * OutlineModel notifications listener interface.
   */
  public interface OutlineModelListener {

    public void rootChanged(OutlineNode newRoot);

    void nodeUpdated(OutlineNode node);

    void rootUpdated();
  }

  private Document document;

  private OutlineModelListener listener;

  private OutlineNode root;

  public OutlineNode getRoot() {
    return root;
  }

  /**
   * Marks outdated nodes.
   */
  public void setDisabled(final OutlineNode node) {
    if (node.isEnabled()) {
      node.setEnabled(false);
      listener.nodeUpdated(node);
    }
  }

  public void setListener(@Nullable OutlineModelListener listener) {
    this.listener = listener;
  }

  public void setRootChildren(JsonArray<OutlineNode> nodes) {
    JsonArray<OutlineNode> rootChildren = root.getChildren();
    rootChildren.clear();
    rootChildren.addAll(nodes);
    if (listener != null) {
      listener.rootUpdated();
    }
  }

  public void updateRoot(OutlineNode root, Document document) {
    Preconditions.checkNotNull(root);
    Preconditions.checkNotNull(document);

    cleanup();

    this.root = root;
    this.document = document;
    if (listener != null) {
      listener.rootChanged(root);
    }
  }

  public void cleanup() {
    if (root == null || document == null) {
      return;
    }

    JsonArray<Anchor> anchors = JsonCollections.createArray();
    collectAttachedAnchors(anchors, root);

    AnchorManager anchorManager = document.getAnchorManager();
    for (int i = 0, l = anchors.size(); i < l; i++) {
      anchorManager.removeAnchor(anchors.get(i));
    }

    root = null;
    document = null;
  }

  private static void collectAttachedAnchors(JsonArray<Anchor> anchors, OutlineNode node) {
    Anchor anchor = node.getAnchor();
    if (anchor != null && anchor.isAttached()) {
      anchors.add(anchor);
    }
    JsonArray<OutlineNode> children = node.getChildren();
    if (children == null) {
      return;
    }
    for (int i = 0, l = children.size(); i < l; ++i) {
      collectAttachedAnchors(anchors, children.get(i));
    }
  }
}
