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

import collide.client.treeview.NodeRenderer;
import collide.client.treeview.TreeNodeElement;
import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.SpanElement;

/**
 * Class used by tree component to produce visual elements.
 */
public class OutlineNodeRenderer implements NodeRenderer<OutlineNode> {

  private static final int LABEL_NODE_INDEX = 1;

  /**
   * Style names associated with elements in the outline node.
   */
  public interface Css extends CssResource {

    String root();

    String icon();

    String clazz();

    String function();

    String cssClazz();

    String jsField();

    String label();

    String disabled();
  }

  /**
   * CSS and images used by the OutlineNodeRenderer.
   */
  public interface Resources extends ClientBundle {

    @Source("clazz.png")
    ImageResource clazz();

    @Source("function.png")
    ImageResource function();

    @Source("css-clazz.png")
    ImageResource cssClazz();

    @Source("js-field.png")
    ImageResource jsField();

    @Source("OutlineNodeRenderer.css")
    Css workspaceNavigationOutlineNodeRendererCss();
  }

  private final Css css;

  public OutlineNodeRenderer(Resources resources) {
    this.css = resources.workspaceNavigationOutlineNodeRendererCss();
  }

  @Override
  public Element getNodeKeyTextContainer(SpanElement treeNodeLabel) {
    return (Element) treeNodeLabel.getChildNodes().item(LABEL_NODE_INDEX);
  }

  @SuppressWarnings("incomplete-switch")
  @Override
  public SpanElement renderNodeContents(OutlineNode data) {
    SpanElement root = Elements.createSpanElement(css.root());

    DivElement icon = Elements.createDivElement(css.icon());
    switch (data.getType()) {
      case CLASS:
        icon.addClassName(css.clazz());
        break;

      case FUNCTION:
        icon.addClassName(css.function());
        break;

      case FIELD:
        icon.addClassName(css.jsField());
        break;

      case CSS_CLASS:
        icon.addClassName(css.cssClazz());
        break;
    }

    SpanElement label = Elements.createSpanElement(css.label());
    label.setTextContent(data.getName());

    root.appendChild(icon);
    root.appendChild(label);

    CssUtils.setClassNameEnabled(label, css.disabled(), !data.isEnabled());

    // TODO: replace with test case
    assert root.getChildNodes().item(LABEL_NODE_INDEX) == label;

    return root;
  }

  @Override
  public void updateNodeContents(TreeNodeElement<OutlineNode> treeNode) {
    // Not implemented.
  }
}
