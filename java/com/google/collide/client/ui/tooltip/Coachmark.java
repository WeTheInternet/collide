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

package com.google.collide.client.ui.tooltip;

import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.tooltip.Coachmark.View;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.CssResource;

import elemental.css.CSSStyleDeclaration;
import elemental.html.DivElement;
import elemental.html.Element;

/**
 * An object which represents a coach mark (a tooltip like notification which is often dismissable).
 */
// TODO: Generalize the class, it currently isn't so general.
public class Coachmark extends UiComponent<View> {

  public static Coachmark create(
      Tooltip.Resources res, Renderer renderer, PositionController.Positioner positioner) {
    View view = new View(res.coachmarkCss());
    return new Coachmark(view, renderer, new PositionController(positioner, view.getElement()));
  }

  public interface Css extends CssResource {
    public String base();

    public String container();

    public String arrow();

    public String arrowInner();

    public String alert();

    public String disclaimer();

    public int arrowSize();
  }

  /**
   * A renderer which renders the contents of a {@link Coachmark}.
   */
  public interface Renderer {
    /**
     * @param container the container to which any child elements should be attached.
     */
    public void render(Element container, Coachmark coachmark);
  }

  public static class BasicRenderer implements Renderer {
    private final String text;

    public BasicRenderer(String text, Coachmark coachmark) {
      this.text = text;
    }

    @Override
    public void render(Element container, Coachmark coachmark) {
      container.setTextContent(text);
    }
  }

  public static class View extends CompositeView<Void> {

    private final DivElement container;
    private final DivElement arrow;
    private final DivElement arrowInner;
    private final Css css;

    public View(Css css) {
      super(Elements.createDivElement(css.base()));
      this.css = css;
      CssUtils.setDisplayVisibility2(getElement(), false);

      this.container = Elements.createDivElement(css.container());
      this.arrow = Elements.createDivElement(css.arrow());
      this.arrowInner = Elements.createDivElement(css.arrowInner());

      arrow.appendChild(arrowInner);
      getElement().appendChild(arrow);
      getElement().appendChild(container);
    }

    public Element getArrow() {
      return arrow;
    }

    public Element getContainer() {
      return container;
    }
  }

  private final Renderer renderer;
  private final PositionController positionController;
  private boolean hasRendered = false;

  private Coachmark(View view, Renderer renderer, PositionController positionController) {
    super(view);
    this.renderer = renderer;
    this.positionController = positionController;
  }

  public void show() {
    Element element = getView().getElement();
    if (!hasRendered) {
      renderer.render(getView().getContainer(), this);
      hasRendered = true;
    }

    updatePosition();
    CssUtils.setDisplayVisibility2(element, true);
  }

  public void hide() {
    Element element = getView().getElement();
    CssUtils.setDisplayVisibility2(element, false);
  }

  private void updatePosition() {
    PositionController.Positioner positioner = positionController.getPositioner();
    int x = 0;
    int y = getYOffset(positioner.getVerticalAlignment());

    // TODO: This is megahacked together, if you try to do a coach mark get here and find
    // you need better, we should refactor this so it is more general.
    updateArrow(getXOffset(positioner.getHorizontalAlignment()), y);
    positionController.updateElementPosition(x, y);
  }

  private void updateArrow(int x, int y) {
    Element arrow = getView().getArrow();
    CSSStyleDeclaration style = arrow.getStyle();

    HorizontalAlign alignment = positionController.getPositioner().getHorizontalAlignment();
    style.setLeft("auto");
    style.setRight("auto");
    if (alignment == HorizontalAlign.LEFT) {
      style.setLeft(-(x * 2), CSSStyleDeclaration.Unit.PX);
    } else {
      style.setRight((x * 2), CSSStyleDeclaration.Unit.PX);
    }
    style.setTop(-(y * 2), CSSStyleDeclaration.Unit.PX);
  }

  private int getYOffset(VerticalAlign alignment) {
    int arrowSize = getView().css.arrowSize();
    switch (alignment) {
      case TOP:
        return -arrowSize;
      case BOTTOM:
        return arrowSize;
      default:
        return 0;
    }
  }

  private int getXOffset(HorizontalAlign alignment) {
    int arrowSize = getView().css.arrowSize();
    switch (alignment) {
      case LEFT:
        return -arrowSize;
      case RIGHT:
        return arrowSize;
      default:
        return 0;
    }
  }
}
