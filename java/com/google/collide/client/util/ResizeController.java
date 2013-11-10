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

package com.google.collide.client.util;


import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.util.dom.eventcapture.MouseCaptureListener;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.animation.client.AnimationScheduler.AnimationCallback;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.MouseEvent;
import elemental.util.ArrayOf;
import elemental.util.Collections;

/**
 * Controller that adds resizing capabilities to elements.
 */
public class ResizeController {

  /**
   * CSS used by the resize controller.
   */
  public static interface Css extends CssResource {
    String horizontalCursor();

    String verticalCursor();

    String northeastCursor();

    String northwestCursor();

    String southeastCursor();

    String southwestCursor();

    String elementResizing();

    String hSplitter();

    String vSplitter();

    String moveCursor();

    String neSplitter();

    String nwSplitter();

    String seSplitter();

    String swSplitter();

  }

  /**
   * POJO that encapsulates an element and the CSS property that should be updated as the user drags the
   * splitter.
   */
  public static class ElementInfo {
    private final Element element;
    /**
     * Stores the CSS property's value. This is faster than reading the CSS property value from
     * {@link #applyDelta(int)}. This is also required to avoid out-of-sync with the cursor and
     * width/height-resizing elements. (For example, an element's width is to be adjusted, and the mouse moves
     * to the left of the element's left. The width should be negative, but the CSS property won't store a
     * negative value. When the mouse moves back to the right, it will grow the element's width, but the mouse
     * pointer will not be exactly over the element anymore.)
     */
    protected int propertyValue;
    private int propertyMinValue = Integer.MIN_VALUE;
    private int propertyMaxValue = Integer.MAX_VALUE;
    private final ResizeProperty resizeProperty;
    private final String resizePropertyName;

    public ElementInfo(Element element, ResizeProperty resizeProperty) {
      this.element = element;
      this.resizeProperty = resizeProperty;
      this.resizePropertyName = resizeProperty.propertyName();
    }

    /**
     * Constructs a new {@link ElementInfo} and sets the default value of the property.
     */
    public ElementInfo(Element element, ResizeProperty resizeProperty, String defaultValue) {
      this(element, resizeProperty);
      element.getStyle().setProperty(resizePropertyName, defaultValue);
    }

    public Element getElement() {
      return element;
    }

    public int getPropertyMinValue() {
      return propertyMinValue;
    }

    public int getPropertyMaxValue() {
      return propertyMaxValue;
    }

    public ResizeProperty getResizeProperty() {
      return resizeProperty;
    }

    public ElementInfo setPropertyMinValue(int value) {
      propertyMinValue = value;
      return this;
    }

    public ElementInfo setPropertyMaxValue(int value) {
      propertyMaxValue = value;
      return this;
    }

    private void applyDelta(int deltaW, int deltaH) {
      if (resizeProperty.isVertical()) deltaW = deltaH;
      if (resizeProperty.isNegative()) {
        propertyValue -= deltaW;
      } else {
        propertyValue += deltaW;
      }
      element.getStyle().setProperty(resizePropertyName, propertyValue + CSSStyleDeclaration.Unit.PX);
    }

    public void refresh() {
      resetPropertyValue();
      int delta = computeApplicableDelta(0);
      applyDelta(delta, delta);
    }

    protected int computeApplicableDelta(int delta) {
      int nextValue;
      if (getResizeProperty().isNegative()) {
        nextValue = propertyValue - delta;
        nextValue = Math.min(nextValue, propertyMaxValue);
        nextValue = Math.max(nextValue, propertyMinValue);
        return propertyValue-nextValue;
      }else {
        nextValue = propertyValue + delta;
        nextValue = Math.min(nextValue, propertyMaxValue);
        nextValue = Math.max(nextValue, propertyMinValue);
        return nextValue - propertyValue;

      }
    }

    protected void resetPropertyValue() {
      // Use the value of a CSS property if it has been explicitly set.
      String value = getElement().getStyle().getPropertyValue(resizeProperty.toString());
      //read in from the methods once per reset; our bounds will be fresh once per drag event.
      propertyMaxValue = getPropertyMaxValue();
      propertyMinValue = getPropertyMinValue();

      if (propertyMaxValue <= 0)
        propertyMaxValue = Integer.MAX_VALUE;
      if (propertyMinValue <= 0)
        propertyMinValue = Integer.MIN_VALUE;

      if (!StringUtils.isNullOrEmpty(value) && CssUtils.isPixels(value)) {
        int index = value.indexOf('.');
        if (index!=-1) {
          propertyValue = Integer.parseInt(value.substring(0, index));
        }else {
          propertyValue = CssUtils.parsePixels(value);
        }
        return;
      }

      switch (resizeProperty) {
      case WIDTH:
      case NEG_WIDTH:
        propertyValue = getElement().getClientWidth();
        break;

      case HEIGHT:
      case NEG_HEIGHT:
        propertyValue = getElement().getClientHeight();
        break;

      case LEFT:
      case NEG_LEFT:
        propertyValue = getElement().getOffsetLeft();
        break;

      case TOP:
      case NEG_TOP:
        propertyValue = getElement().getOffsetTop();
        break;

      case RIGHT:
      case NEG_RIGHT:
        propertyValue = getElement().getOffsetWidth() + getElement().getOffsetLeft();
        break;

      case BOTTOM:
      case NEG_BOTTOM:
        propertyValue = getElement().getOffsetHeight() + getElement().getOffsetTop();
        break;
      }
    }
  }

  public static class ResizeEventHandler {
    public void whileDragging(float deltaW, float deltaH, float deltaX, float deltaY) {

    }
    public void startDragging(ElementInfo ... elementInfos) {

    }

    public void doneDragging(float deltaX, float deltaY, float origX, float origY) {

    }
  }

  /**
   * Enumeration that describes which CSS property should be affected by the resize.
   */
  public enum ResizeProperty {
    BOTTOM(false, true), HEIGHT(false, true), LEFT(true, false), 
    NEG_BOTTOM(false, true, true), NEG_HEIGHT(false, true, true),
    NEG_LEFT(true, false, true), NEG_RIGHT(true, false, true),
    NEG_TOP(false, true, true), NEG_WIDTH(true, false, true), 
    RIGHT(true, false), TOP(false, true), WIDTH(true, false);

    private final String propName;
    private final boolean negative, horizontal, vertical;

    private ResizeProperty(boolean horizontal, boolean vertical) {
      this(horizontal, vertical, false);
    }

    public String propertyName() {
      return propName;
    }

    private ResizeProperty(boolean horizontal, boolean vertical, boolean negative) {
      this.negative = negative;
      this.propName = (negative ? toString().substring(4) : toString()).toLowerCase();
      this.horizontal = horizontal;
      this.vertical = vertical;
    }

    public boolean isHorizontal() {
      return horizontal;
    }

    public boolean isNegative() {
      return negative;
    }

    public boolean isVertical() {
      return vertical;
    }
  }

  /**
   * ClientBundle for the resize controller.
   */
  public interface Resources extends ClientBundle {
    @Source("ResizeController.css")
    Css resizeControllerCss();
  }

  private final Css css;

  private final ElementInfo[] elementInfos;

  private final boolean horizontal, vertical;

  private final MouseCaptureListener mouseCaptureListener = new MouseCaptureListener() {
    @Override
    protected boolean onMouseDown(MouseEvent evt) {
      return canStartResizing();
    }

    @Override
    protected void onMouseMove(MouseEvent evt) {
      if (!resizing) {
        resizeStarted();
        if (!resizing)//let subclasses cancel in resizeStarted()
          return;
      }
      if (horizontal) {
        int delta = getDeltaX();
        resizeDraggedW(negativeW ? -delta : delta);
      }
      if (vertical) {
        int delta = getDeltaY();
        resizeDraggedH(negativeH ? -delta : delta);
      }
    }

    @Override
    protected void onMouseUp(MouseEvent evt) {
      if (resizing) {
        resizeEnded();
      }
    }
  };

  private boolean resizing;

  private AnimationCallback animationCallback;

  private final Element splitter;

  private boolean negativeW, negativeH;

  private int unappliedW, unappliedH;
  private int totalW, totalH;

  private String hoverClass;

  private final String enabledClass;

  private final ArrayOf<ResizeEventHandler> handlers;

  public void addEventHandler(ResizeEventHandler handler) {
//    assert !handlers.contains(handler) : "You are adding the same resize event handler more than once; " +
//    		"at "+DebugUtil.getCaller(10);
    handlers.push(handler);
  }
  public void removeEventHandler(ResizeEventHandler handler) {
    handlers.remove(handler);
    assert !handlers.contains(handler) : "You somehow got more than one copy of the resize event handler, " +
    	handler+", into the resize controller "+this;
  }

  /**
   * @param resources the css resources used to style dom
   * @param splitter the element that will act as the splitter
   * @param elementInfos element(s) that will be resized as the user drags the splitter
   */
  public ResizeController(Resources resources, Element splitter, ElementInfo ... elementInfos) {
    this.css = resources.resizeControllerCss();
    this.splitter = splitter;
    this.elementInfos = elementInfos;
    handlers = Collections.arrayOf();
    ElementInfo horizEl = null, vertEl = null;
    for (ElementInfo info : elementInfos) {
      if (info.resizeProperty.isHorizontal()) {
        horizEl = info;
        if (vertEl != null) break;
      }
      if (info.resizeProperty.isVertical()) {
        vertEl = info;
        if (horizEl != null) break;
      }
    }
    this.horizontal = horizEl != null;
    this.vertical = vertEl != null;
    enabledClass = initCss(horizEl, vertEl);
  }

  protected String initCss(ElementInfo horizEl, ElementInfo vertEl) {
    if (horizontal) {
      if (vertical) {
        // need to use the correct north/south east/west class name
        switch (horizEl.resizeProperty) {
        case LEFT:
        case NEG_LEFT:
        case NEG_WIDTH:
          // west
          switch (vertEl.resizeProperty) {
          case TOP:
          case NEG_TOP:
          case NEG_HEIGHT:
            splitter.addClassName(css.nwSplitter());
            return css.northwestCursor();
          case HEIGHT:
          case BOTTOM:
          case NEG_BOTTOM:
            splitter.addClassName(css.swSplitter());
            return css.southwestCursor();
          default:
            throw new RuntimeException("Configuration error; " + horizEl.resizeProperty +
              " was vertical but " + "was not handled correctly by switch statement.");
          }
        case WIDTH:
        case RIGHT:
        case NEG_RIGHT:
          // east
          switch (vertEl.resizeProperty) {
          case TOP:
          case NEG_BOTTOM:
          case NEG_HEIGHT:
            splitter.addClassName(css.neSplitter());
            return css.northeastCursor();
          case HEIGHT:
          case BOTTOM:
          case NEG_TOP:
            splitter.addClassName(css.seSplitter());
            return css.southeastCursor();
          default:
            throw new RuntimeException("Configuration error; " + horizEl.resizeProperty +
              " was vertical but " + "was not handled correctly by switch statement.");
          }
        default:
          throw new RuntimeException("Configuration error; " + horizEl.resizeProperty +
            " was horizontal but " + "was not handled correctly by switch statement.");
        }
      } else {
        splitter.addClassName(css.hSplitter());
        return css.horizontalCursor();
      }
    } else {
      splitter.addClassName(css.vSplitter());
      return css.verticalCursor();
    }
  }

  public void setNegativeDeltaW(boolean negativeDelta) {
    this.negativeW = negativeDelta;
  }

  public void setNegativeDeltaH(boolean negativeDelta) {
    this.negativeH = negativeDelta;
  }

  public ElementInfo[] getElementInfos() {
    return elementInfos;
  }

  public Element getSplitter() {
    return splitter;
  }

  public void start() {
    getSplitter().addEventListener(Event.MOUSEDOWN, mouseCaptureListener, false);
  }

  public void stop() {
    getSplitter().removeEventListener(Event.MOUSEDOWN, mouseCaptureListener, false);
    mouseCaptureListener.release();

    if (resizing) {
      resizeEnded();
    }
  }

  private void maybeSchedule() {
    /* Give the browser a chance to redraw before applying the next delta. Otherwise, we'll end up locking the
     * browser if the user moves the mouse too quickly. */
    if (animationCallback == null) {
      animationCallback = new AnimationCallback() {
        @Override
        public void execute(double arg0) {
          if (this != animationCallback) {
            // The resize event was already ended.
            return;
          }
          animationCallback = null;
          applyUnappliedDelta();
        }
      };
      AnimationScheduler.get().requestAnimationFrame(animationCallback);
    }
  }

  private void resizeDraggedW(int delta) {
    unappliedW += delta;
    maybeSchedule();
  }

  private void resizeDraggedH(int delta) {
    unappliedH += delta;
    maybeSchedule();
  }

  private void applyUnappliedDelta() {
    int deltaW = unappliedW, deltaH = unappliedH;
    for (ElementInfo elementInfo : getElementInfos()) {
      // deltaToApply ends up being the minimum delta that any element can
      // accept.
      if (elementInfo.resizeProperty.horizontal) {
        deltaW = elementInfo.computeApplicableDelta(deltaW);
      }
      if (elementInfo.resizeProperty.vertical) {
        deltaH = elementInfo.computeApplicableDelta(deltaH);
      }
    }
    totalW += deltaW;
    totalH += deltaH;
    unappliedW -= deltaW;
    unappliedH -= deltaH;
    applyDelta(deltaW, deltaH);
  }

  protected void applyDelta(int deltaW, int deltaH) {
    //apply deltas to the elements we are resizing
    float x, y, w, h;
    x = y = w = h = 0;
    for (ElementInfo elementInfo : getElementInfos()) {
      elementInfo.applyDelta(deltaW, deltaH);
      switch (elementInfo.resizeProperty) {
        case LEFT:
        case NEG_LEFT:
          x += deltaW;
        case RIGHT:
        case NEG_RIGHT:
        case WIDTH:
        case NEG_WIDTH:
          w += deltaH;
          break;
        case TOP:
        case NEG_TOP:
          y += deltaH;
        case BOTTOM:
        case NEG_BOTTOM:
        case HEIGHT:
        case NEG_HEIGHT:
          h += deltaH;
          break;
      }
    }
    //also apply deltas to any added listener
    for (int i = 0; i < handlers.length(); i++) {
      handlers.get(i).whileDragging(w, h, x, y);
    }
  }

  protected boolean canStartResizing() {
    return true;
  }

  protected void resizeStarted() {
    resizing = true;

    if (hoverClass != null) {
      splitter.addClassName(hoverClass);
    }

    for (ElementInfo elementInfo : getElementInfos()) {
      // Disables transitions while we resize, or it will appear laggy.
      elementInfo.getElement().addClassName(css.elementResizing());
      elementInfo.resetPropertyValue();
//      elementInfo.propertyValue;
    }

    totalH = totalW = 0;
    //also apply deltas to any added listener
    for (int i = 0; i < handlers.length(); i++) {
      handlers.get(i).startDragging(getElementInfos());
    }

    setResizeCursorEnabled(true);
  }

  protected Css getCss() {
    return css;
  }

  protected void resizeEnded() {
    // Force a final resize if there is some unapplied delta.
    if (unappliedW != 0 || unappliedW != 0) {
      applyUnappliedDelta();
    }

    //notify handlers of the detach
    for (int i = 0; i < handlers.length(); i++) {
      handlers.get(i).doneDragging(totalW, totalH,0 ,0);
    }

    for (ElementInfo elementInfo : getElementInfos()) {
      elementInfo.getElement().removeClassName(css.elementResizing());
    }

    if (hoverClass != null) {
      splitter.removeClassName(hoverClass);
    }

    setResizeCursorEnabled(false);

    resizing = false;
    animationCallback = null;
    unappliedW = 0;
    unappliedH = 0;
  }

  /**
   * Setting this property allows to avoid control "blinking" during resizing. Set the class to be applied
   * when control is being dragged. The specified style-class should have at least the same sense as
   * {@code :hover} pseudo-class applied to control.
   *
   * @param hoverClass style-class to be saved ad applied appropriately
   */
  public void setHoverClass(String hoverClass) {
    this.hoverClass = hoverClass;
  }

  protected String getResizeCursor() {
    return enabledClass;
  }

  /**
   * Forces all elements on the page to use the resize cursor while resizing.
   */
  private void setResizeCursorEnabled(boolean enabled) {
    CssUtils.setClassNameEnabled(Elements.getBody(), getResizeCursor(), enabled);
  }
  public boolean isNegativeWidth() {
    for (ElementInfo el : elementInfos) {
      switch (el.resizeProperty) {
      case NEG_WIDTH:
      case LEFT:
        return true;
      default:
      }
    }
    return false;
  }
  public boolean isNegativeHeight() {
    for (ElementInfo el : elementInfos) {
      switch (el.resizeProperty) {
      case NEG_HEIGHT:
      case TOP:
        return true;
      default:
      }
    }
    return false;
  }
}
