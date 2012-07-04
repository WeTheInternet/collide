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

package com.google.collide.client.util.dom;

import com.google.collide.client.util.BrowserUtils;
import com.google.collide.client.util.Elements;
import com.google.gwt.user.client.DOM;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MouseEvent;
import elemental.html.ClientRect;
import elemental.html.DivElement;
import elemental.html.Element;

/**
 * Utility methods for DOM manipulation.
 *
 */
public final class DomUtils {

  public static class Offset {
    public int top = 0;
    public int left = 0;

    private Offset() {
    }

    private Offset(int top, int left) {
      this.top = top;
      this.left = left;
    }
  }

  private static final EventListener STOP_PROPAGATION_EVENT_LISTENER = new EventListener() {
    @Override
    public void handleEvent(Event evt) {
      evt.stopPropagation();
      evt.preventDefault();
    }
  };

  /**
   * Returns the client offset to the top-left of the given element.
   */
  @Deprecated
  public static Offset calculateElementClientOffset(Element element) {
    return calculateElementOffset(element, null, false);
  }

  /**
   * Returns an offset to the top-left of a child element relative to the
   * top-left of an ancestor element, optionally including any scroll top or
   * left in elements from the ancestor (inclusive) to the child (exclusive).
   *
   * @param ancestorElement optional, if null the offset from the top-left of
   *        the page will be given. Should not be the childElement.
   */
  @Deprecated
  public static Offset calculateElementOffset(
      Element childElement, Element ancestorElement, boolean includeScroll) {

    Offset offset = new Offset();
    Element element = childElement;
    for (; element.getOffsetParent() != null && element != ancestorElement; element =
        element.getOffsetParent()) {
      offset.top += element.getOffsetTop();
      offset.left += element.getOffsetLeft();

      if (!includeScroll) {
        offset.top -= element.getOffsetParent().getScrollTop();
        offset.left -= element.getOffsetParent().getScrollLeft();
      }
    }

    return offset;
  }

  /**
   * Wrapper for getting the offsetX from a mouse event that provides a fallback
   * implementation for Firefox. (See
   * https://bugzilla.mozilla.org/show_bug.cgi?id=122665#c3 )
   */
  public static int getOffsetX(MouseEvent event) {
    if (BrowserUtils.isFirefox()) {
      return event.getClientX()
          - calculateElementClientOffset((Element) event.getTarget()).left;
    } else {
      return event.getOffsetX();
    }
  }

  /**
   * @see #getOffsetX(MouseEvent)
   */
  public static int getOffsetY(MouseEvent event) {
    if (BrowserUtils.isFirefox()) {
      return event.getClientY()
          - calculateElementClientOffset((Element) event.getTarget()).top;
    } else {
      return event.getOffsetY();
    }
  }

  public static Element getNthChild(Element element, int index) {
    Element child = element.getFirstChildElement();
    while (child != null && index > 0) {
      --index;
      child = child.getNextSiblingElement();
    }
    return child;
  }

  public static Element getNthChildWithClassName(Element element, int index, String className) {
    Element child = element.getFirstChildElement();
    while (child != null) {
      if (child.hasClassName(className)) {
        --index;
        if (index < 0) {
          break;
        }
      }
      child = child.getNextSiblingElement();
    }
    return child;
  }

  /**
   * @return number of previous sibling elements that have the given class
   */
  public static int getSiblingIndexWithClassName(Element element, String className) {
    int index = 0;
    while (element != null) {
      element = (Element) element.getPreviousSibling();
      if (element != null && element.hasClassName(className)) {
        ++index;
      }
    }
    return index;
  }

  public static Element getFirstElementByClassName(Element element, String className) {
    return (Element) element.getElementsByClassName(className).item(0);
  }

  public static DivElement appendDivWithTextContent(Element root, String className, String text) {
    DivElement element = Elements.createDivElement(className);
    element.setTextContent(text);
    root.appendChild(element);
    return element;
  }

  /**
   * Ensures that the {@code scrollable} element is scrolled such that
   * {@code target} is visible.
   * 
   * Note: This can trigger a synchronous layout.
   */
  public static boolean ensureScrolledTo(Element scrollable, Element target) {
    ClientRect targetBounds = target.getBoundingClientRect();
    ClientRect scrollableBounds = scrollable.getBoundingClientRect();

    int deltaBottoms = (int) (targetBounds.getBottom() - scrollableBounds.getBottom());  
    int deltaTops = (int) (targetBounds.getTop() - scrollableBounds.getTop());  
    
    if (deltaTops >= 0 && deltaBottoms <= 0) {
      // In bounds
      return false;
    }
    
    if (targetBounds.getHeight() > scrollableBounds.getHeight() || deltaTops < 0) {
      /*
       * Selected is taller than viewport height or selected is scrolled above
       * viewport, so set to top
       */
      scrollable.setScrollTop(scrollable.getScrollTop() + deltaTops);
    } else {
      // Selected is scrolled below viewport
      scrollable.setScrollTop(scrollable.getScrollTop() + deltaBottoms);
    }
    
    return true;
  }

  /**
   * Checks whether the given {@code target} element is fully visible in
   * {@code scrollable}'s scrolled viewport.
   * 
   * Note: This can trigger a synchronous layout.
   */
  public static boolean isFullyInScrollViewport(Element scrollable, Element target) {
    ClientRect targetBounds = target.getBoundingClientRect();
    ClientRect scrollableBounds = scrollable.getBoundingClientRect();

    return targetBounds.getTop() >= scrollableBounds.getTop()
        && targetBounds.getBottom() <= scrollableBounds.getBottom();
  }

  /**
   * Stops propagation for the common mouse events (down, move, up, click,
   * dblclick).
   */
  public static void stopMousePropagation(Element element) {
    element.addEventListener(Event.MOUSEDOWN, STOP_PROPAGATION_EVENT_LISTENER, false);
    element.addEventListener(Event.MOUSEMOVE, STOP_PROPAGATION_EVENT_LISTENER, false);
    element.addEventListener(Event.MOUSEUP, STOP_PROPAGATION_EVENT_LISTENER, false);
    element.addEventListener(Event.CLICK, STOP_PROPAGATION_EVENT_LISTENER, false);
    element.addEventListener(Event.DBLCLICK, STOP_PROPAGATION_EVENT_LISTENER, false);
  }

  /**
   * Prevent propagation of scrolling to parent containers on mouse wheeling,
   * when target container can not be scrolled anymore.
   */
  public static void preventExcessiveScrollingPropagation(final Element container) {
    container.addEventListener(Event.MOUSEWHEEL, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        int deltaY = DOM.eventGetMouseWheelVelocityY((com.google.gwt.user.client.Event) evt);
        int scrollTop = container.getScrollTop();
        if (deltaY < 0) {
          if (scrollTop == 0) {
            evt.preventDefault();
          }
        } else {
          int scrollBottom = scrollTop + (int) container.getBoundingClientRect().getHeight();
          if (scrollBottom == container.getScrollHeight()) {
            evt.preventDefault();
          }
        }
        evt.stopPropagation();
      }
    }, false);
  }

  /**
   * Doing Elements.asJsElement(button).setDisabled(true); doesn't work for buttons, possibly
   * because they're actually AnchorElements
   */
  public static void setDisabled(Element element, boolean disabled) {
    if (disabled) {
      element.setAttribute("disabled", "disabled");
    } else {
      element.removeAttribute("disabled");
    }
  }

  public static boolean getDisabled(Element element) {
    return element.hasAttribute("disabled");
  }
 
  /**
   * @return true if the provided element or one of its children have focus.
   */
  public static boolean isElementOrChildFocused(Element element) {
    Element active = element.getOwnerDocument().getActiveElement();
    return element.contains(active);
  }

  private DomUtils() {} // COV_NF_LINE
}
