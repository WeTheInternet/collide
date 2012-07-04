package com.google.collide.client.ui.tree;

import elemental.html.Element;
import elemental.html.SpanElement;

/**
 * Flyweight renderer whose job it is to take a NodeData and construct the
 * appropriate DOM structure for the tree node contents.
 *
 * @param <D> The type of data we want to render.
 */
public interface NodeRenderer<D> {

  /**
   * Takes in a {@link SpanElement} constructed via a call to
   * {@link #renderNodeContents} and returns an element whose contract is that
   * it contains only text corresponding to the key for the node's underlying
   * data.
   *
   * This ofcourse depends on the structure that was generated via the call to
   * {@link #renderNodeContents}.
   */
  Element getNodeKeyTextContainer(SpanElement treeNodeLabel);

  /**
   * Constructs the label portion of a {@link TreeNodeElement}. Labels can have
   * arbitrary DOM structure, with one constraint. At least one element MUST
   * contain only text that corresponds to the String key for the underlying
   * node's data.
   */
  SpanElement renderNodeContents(D data);

  /**
   * Updates the node's contents to reflect the current state of the node.
   * 
   * @param treeNode the tree node that contains the rendered node contents
   */
  void updateNodeContents(TreeNodeElement<D> treeNode);
}
