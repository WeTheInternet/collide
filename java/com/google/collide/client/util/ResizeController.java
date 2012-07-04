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

import com.google.collide.client.util.dom.eventcapture.MouseCaptureListener;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.animation.client.AnimationScheduler.AnimationCallback;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;
import elemental.events.MouseEvent;
import elemental.html.Element;

/**
 * Controller that adds resizing capabilities to elements.
 */
public class ResizeController {
  /**
   * CSS used by the resize controller.
   *
   */
  public interface Css extends CssResource {
    String horizontalCursor();

    String verticalCursor();

    String elementResizing();

    String hSplitter();

    String vSplitter();
  }

  /**
   * POJO that encapsulates an element and the CSS property that should be
   * updated as the user drags the splitter.
   *
   */
  public static class ElementInfo {
    private final Element element;
    /**
     * Stores the CSS property's value. This is faster than reading the CSS
     * property value from {@link #applyDelta(int)}. This is also required to
     * avoid out-of-sync with the cursor and width/height-resizing elements.
     * (For example, an element's width is to be adjusted, and the mouse moves
     * to the left of the element's left. The width should be negative, but the
     * CSS property won't store a negative value. When the mouse moves back to
     * the right, it will grow the element's width, but the mouse pointer will
     * not be exactly over the element anymore.)
     */
    private int propertyValue;
    private int propertyMinValue = Integer.MIN_VALUE;
    private int propertyMaxValue = Integer.MAX_VALUE;
    private final ResizeProperty resizeProperty;
    private final String resizePropertyName;

    public ElementInfo(Element element, ResizeProperty resizeProperty) {
      this.element = element;
      this.resizeProperty = resizeProperty;
      this.resizePropertyName = resizeProperty.toString().toLowerCase();
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

    public ElementInfo setPropertyMinValue(int value) {
      propertyMinValue = value;
      return this;
    }

    public ElementInfo setPropertyMaxValue(int value) {
      propertyMaxValue = value;
      return this;
    }

    private void applyDelta(int delta) {
      propertyValue += delta;
      element.getStyle().setProperty(
          resizePropertyName, propertyValue + CSSStyleDeclaration.Unit.PX);
    }

    private int computeApplicableDelta(int delta) {
      int nextValue = propertyValue + delta;
      nextValue = Math.min(nextValue, propertyMaxValue);
      nextValue = Math.max(nextValue, propertyMinValue);
      return nextValue - propertyValue;
    }

    private void resetPropertyValue() {
      // Use the value of a CSS property if it has been explicitly set.
      String value = getElement().getStyle().getPropertyValue(resizeProperty.toString());
      if (!StringUtils.isNullOrEmpty(value) && CssUtils.isPixels(value)) {
        propertyValue = CssUtils.parsePixels(value);
        return;
      }

      switch (resizeProperty) {
        case WIDTH:
          propertyValue = getElement().getClientWidth();
          break;

        case HEIGHT:
          propertyValue = getElement().getClientHeight();
          break;

        case LEFT:
          propertyValue = getElement().getOffsetLeft();
          break;

        case TOP:
          propertyValue = getElement().getOffsetTop();
          break;

        case RIGHT:
          propertyValue =
              getElement().getOffsetParent().getClientWidth() - getElement().getOffsetLeft()
                  - getElement().getOffsetWidth();
          break;

        case BOTTOM:
          propertyValue =
              getElement().getOffsetParent().getClientHeight() - getElement().getOffsetTop()
                  - getElement().getOffsetHeight();
          break;
      }
    }
  }

  /**
   * Enumeration that describes which CSS property should be affected by the
   * resize.
   *
   */
  public enum ResizeProperty {
    BOTTOM, HEIGHT, LEFT, RIGHT, TOP, WIDTH
  }

  /**
   * ClientBundle for the resize controller.
   *
   */
  public interface Resources extends ClientBundle {
    @Source("ResizeController.css")
    Css resizeControllerCss();
  }

  private static boolean isHorizontal(ResizeProperty resizeProperty) {
    return resizeProperty == ResizeProperty.WIDTH || resizeProperty == ResizeProperty.LEFT
        || resizeProperty == ResizeProperty.RIGHT;
  }

  private final Css css;

  private final ElementInfo[] elementInfos;

  private final boolean horizontal;

  private final MouseCaptureListener mouseCaptureListener = new MouseCaptureListener() {
    @Override
    protected boolean onMouseDown(MouseEvent evt) {
      return canStartResizing();
    }

    @Override
    protected void onMouseMove(MouseEvent evt) {
      if (!resizing) {
        resizeStarted();
      }
      int delta = horizontal ? getDeltaX() : getDeltaY();
      resizeDragged(negativeDelta ? -delta : delta);
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

  private boolean negativeDelta;

  private int unappliedDelta;

  private String hoverClass;

  /**
   * @param splitter the element that will act as the splitter
   * @param elementInfos element(s) that will be resized as the user drags the
   *        splitter
   */
  public ResizeController(Resources resources, Element splitter, ElementInfo... elementInfos) {
    this.css = resources.resizeControllerCss();
    this.splitter = splitter;
    this.elementInfos = elementInfos;
    this.horizontal = isHorizontal(elementInfos[0].resizeProperty);

    if (horizontal) {
      splitter.addClassName(css.hSplitter());
    } else {
      splitter.addClassName(css.vSplitter());
    }
  }

  public void setNegativeDelta(boolean negativeDelta) {
    this.negativeDelta = negativeDelta;
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

  private void resizeDragged(int delta) {
    unappliedDelta += delta;

    /*
     * Give the browser a chance to redraw before applying the next delta.
     * Otherwise, we'll end up locking the browser if the user moves the mouse
     * too quickly.
     */
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

  private void applyUnappliedDelta() {
    int deltaToApply = unappliedDelta;
    for (ElementInfo elementInfo : getElementInfos()) {
      // deltaToApply ends up being the minimum delta that any element can
      // accept.
      deltaToApply = elementInfo.computeApplicableDelta(deltaToApply);
    }
    unappliedDelta -= deltaToApply;
    applyDelta(deltaToApply);
  }

  protected void applyDelta(int delta) {
    for (ElementInfo elementInfo : getElementInfos()) {
      elementInfo.applyDelta(delta);
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
    }

    setResizeCursorEnabled(true);
  }

  protected Css getCss() {
    return css;
  }

  protected void resizeEnded() {
    // Force a final resize if there is some unapplied delta.
    if (unappliedDelta > 0) {
      applyUnappliedDelta();
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
    unappliedDelta = 0;
  }

  /**
   * Setting this property allows to avoid control "blinking" during resizing.
   *
   * Set the class to be applied when control is being dragged.
   *
   * The specified style-class should have at least the same sense as
   * {@code :hover} pseudo-class applied to control.
   *
   * @param hoverClass style-class to be saved ad applied appropriately
   */
  public void setHoverClass(String hoverClass) {
    this.hoverClass = hoverClass;
  }

  /**
   * Forces all elements on the page to use the resize cursor while resizing.
   */
  private void setResizeCursorEnabled(boolean enabled) {
    String className = horizontal ? css.horizontalCursor() : css.verticalCursor();
    CssUtils.setClassNameEnabled(Elements.getBody(), className, enabled);
  }
}
