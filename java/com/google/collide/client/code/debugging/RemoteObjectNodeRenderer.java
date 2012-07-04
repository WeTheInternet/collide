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

package com.google.collide.client.code.debugging;

import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectType;
import com.google.collide.client.ui.tree.NodeRenderer;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.html.Element;
import elemental.html.SpanElement;

/**
 * Renders the {@link RemoteObjectNode} in the {@link RemoteObjectTree}.
 *
 */
public class RemoteObjectNodeRenderer implements NodeRenderer<RemoteObjectNode> {

  private static final RegExp LINE_BREAK_REGEXP = RegExp.compile("\\r?\\n", "g");
  private static final String LINE_BREAK_SUBSTITUTE = "\u21B5";

  public interface Css extends CssResource {
    String root();
    String propertyName();
    String separator();
    String propertyValue();
    String wasThrown();
    String noPropertyValue();
    String notEnumerable();
    String tokenArray();
    String tokenBoolean();
    String tokenDate();
    String tokenFunction();
    String tokenNode();
    String tokenNull();
    String tokenNumber();
    String tokenObject();
    String tokenRegexp();
    String tokenString();
    String tokenUndefined();
    String inPropertyValueMutation();
  }

  interface Resources extends ClientBundle {
    @Source("RemoteObjectNodeRenderer.css")
    Css remoteObjectNodeRendererCss();
  }

  private final Css css;

  RemoteObjectNodeRenderer(Resources resources) {
    css = resources.remoteObjectNodeRendererCss();
  }

  @Override
  public Element getNodeKeyTextContainer(SpanElement treeNodeLabel) {
    return treeNodeLabel.getFirstChildElement();
  }

  @Override
  public SpanElement renderNodeContents(RemoteObjectNode data) {
    SpanElement root = Elements.createSpanElement(css.root());
    if (data.wasThrown()) {
      root.addClassName(css.wasThrown());
    }

    Element propertyNameElement = Elements.createDivElement(css.propertyName());
    if (data.getRemoteObject() == null) {
      propertyNameElement.addClassName(css.noPropertyValue());
    }
    if (!data.isEnumerable()) {
      propertyNameElement.addClassName(css.notEnumerable());
    }
    propertyNameElement.setTextContent(data.getName());
    root.appendChild(propertyNameElement);

    String propertyValue = getPropertyValueAsString(data);
    if (!StringUtils.isNullOrEmpty(propertyValue)) {
      if (!StringUtils.isNullOrEmpty(data.getName())) {
        Element separator = Elements.createDivElement(css.separator());
        separator.setTextContent(":");
        root.appendChild(separator);
      }

      Element propertyValueElement = Elements.createDivElement(css.propertyValue(),
          getTokenClassName(data.getRemoteObject()));
      propertyValueElement.setTextContent(propertyValue);
      root.appendChild(propertyValueElement);
    }

    return root;
  }

  @Override
  public void updateNodeContents(TreeNodeElement<RemoteObjectNode> treeNode) {
    // Not implemented.
  }

  Element getPropertyValueElement(SpanElement treeNodeLabel) {
    return DomUtils.getFirstElementByClassName(treeNodeLabel, css.propertyValue());
  }

  Element getAncestorPropertyNameElement(Element element) {
    return CssUtils.getAncestorOrSelfWithClassName(element, css.propertyName());
  }

  Element getAncestorPropertyValueElement(Element element) {
    return CssUtils.getAncestorOrSelfWithClassName(element, css.propertyValue());
  }

  void enterPropertyValueMutation(SpanElement treeNodeLabel) {
    treeNodeLabel.addClassName(css.inPropertyValueMutation());
  }

  void exitPropertyValueMutation(SpanElement treeNodeLabel, String newLabel) {
    treeNodeLabel.removeClassName(css.inPropertyValueMutation());

    Element propertyValueElement = getPropertyValueElement(treeNodeLabel);
    if (propertyValueElement != null) {
      propertyValueElement.setTextContent(StringUtils.nullToEmpty(newLabel));
    }
  }

  void removeTokenClassName(RemoteObjectNode data, SpanElement treeNodeLabel) {
    Element propertyValueElement = getPropertyValueElement(treeNodeLabel);
    if (propertyValueElement != null) {
      String tokenClassName = getTokenClassName(data.getRemoteObject());
      if (!StringUtils.isNullOrEmpty(tokenClassName)) {
        propertyValueElement.removeClassName(tokenClassName);
      }
    }
  }

  public String getTokenClassName(RemoteObject remoteObject) {
    if (remoteObject == null) {
      return "";
    }
    if (remoteObject.getSubType() != null) {
      switch (remoteObject.getSubType()) {
        case ARRAY:
          return css.tokenArray();
        case DATE:
          return css.tokenDate();
        case NODE:
          return css.tokenNode();
        case NULL:
          return css.tokenNull();
        case REGEXP:
          return css.tokenRegexp();
      }
    }
    if (remoteObject.getType() != null) {
      switch (remoteObject.getType()) {
        case BOOLEAN:
          return css.tokenBoolean();
        case FUNCTION:
          return css.tokenFunction();
        case NUMBER:
          return css.tokenNumber();
        case OBJECT:
          return css.tokenObject();
        case STRING:
          return css.tokenString();
        case UNDEFINED:
          return css.tokenUndefined();
      }
    }
    return "";
  }

  private static String getPropertyValueAsString(RemoteObjectNode node) {
    RemoteObject remoteObject = node.getRemoteObject();
    if (remoteObject == null) {
      return "";
    }
    if (node.wasThrown()) {
      return "[Exception: " + remoteObject.getDescription() + "]";
    }
    if (node.isTransient()) {
      // Just put some UI difference for the transient objects.
      return "";
    }
    if (RemoteObjectType.STRING.equals(remoteObject.getType())) {
      return "\""
          + LINE_BREAK_REGEXP.replace(remoteObject.getDescription(), LINE_BREAK_SUBSTITUTE)
          + "\"";
    }
    return remoteObject.getDescription();
  }
}
