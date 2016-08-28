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

package collide.client.util;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;

import com.google.gwt.dom.client.Style.Unit;

/**
 * Utility methods for dealing with CSS.
 *
 */
public class CssUtils {
  public static boolean containsClassName(
      com.google.gwt.dom.client.Element element, String className) {
    return (" " + element.getClassName() + " ").contains(" " + className + " ");
  }

  /**
   * Walks the DOM ancestry to find a node with the specified className.
   *
   * @return the element with the specified className, or {@code null} if none
   *         is found.
   */
  public static Element getAncestorOrSelfWithClassName(Element element, String className) {
    while (element != null && !containsClassName(element, className)) {
      element = element.getParentElement();
    }
    return element;
  }

  public static final void addClassName(Element e, String className) {
    assert (className != null) : "Unexpectedly null class name";

    className = className.trim();
    assert (className.length() != 0) : "Unexpectedly empty class name";

    // Get the current style string.
    String oldClassName = e.getClassName();
    int idx = oldClassName.indexOf(className);

    // Calculate matching index.
    while (idx != -1) {
      if (idx == 0 || oldClassName.charAt(idx - 1) == ' ') {
        int last = idx + className.length();
        int lastPos = oldClassName.length();
        if ((last == lastPos)
            || ((last < lastPos) && (oldClassName.charAt(last) == ' '))) {
          break;
        }
      }
      idx = oldClassName.indexOf(className, idx + 1);
    }

    // Only add the style if it's not already present.
    if (idx == -1) {
      if (oldClassName.length() > 0) {
        oldClassName += " ";
      }
      e.setClassName(oldClassName + className);
    }
  }

  public static void removeClassName(Element e, String className) {
    assert (className != null) : "Unexpectedly null class name";

    className = className.trim();
    assert (className.length() != 0) : "Unexpectedly empty class name";

    // Get the current style string.
    String oldStyle = e.getClassName();
    int idx = oldStyle.indexOf(className);

    // Calculate matching index.
    while (idx != -1) {
      if (idx == 0 || oldStyle.charAt(idx - 1) == ' ') {
        int last = idx + className.length();
        int lastPos = oldStyle.length();
        if ((last == lastPos)
            || ((last < lastPos) && (oldStyle.charAt(last) == ' '))) {
          break;
        }
      }
      idx = oldStyle.indexOf(className, idx + 1);
    }

    // Don't try to remove the style if it's not there.
    if (idx != -1) {
      // Get the leading and trailing parts, without the removed name.
      String begin = oldStyle.substring(0, idx).trim();
      String end = oldStyle.substring(idx + className.length()).trim();

      // Some contortions to make sure we don't leave extra spaces.
      String newClassName;
      if (begin.length() == 0) {
        newClassName = end;
      } else if (end.length() == 0) {
        newClassName = begin;
      } else {
        newClassName = begin + " " + end;
      }

      e.setClassName(newClassName);
    }
  }

  public static void replaceClassName(Element e, String oldClassName, String newClassName) {
    removeClassName(e, oldClassName);
    addClassName(e, newClassName);
  }


  public static boolean containsClassName(Element e, String className) {
    assert className != null : "Unexpected null class name";
    String currentClassName = e.getClassName();
    return (currentClassName != null) &&
        (currentClassName.equals(className)
            || currentClassName.startsWith(className + " ")
            || currentClassName.endsWith(" " + className)
            || currentClassName.indexOf(" " + className + " ") != -1);
  }

  public static void setClassNameEnabled(Element element, String className, boolean enable) {
    if (enable) {
      addClassName(element, className);
    } else {
      removeClassName(element, className);
    }
  }

  /**
   * Test if the element his its {@code display} set to something other than
   * {@code none}.
   */
  public static boolean isVisible(Element element) {
    return !element.getStyle().getDisplay().equals(CSSStyleDeclaration.Display.NONE);
  }

  /**
   * Parses the pixels from a value string in pixels, or returns 0 for an empty
   * value.
   */
  public static int parsePixels(String value) {
    assert value.length() == 0 || isPixels(value);
    return value.length() == 0 ? 0 : Integer.parseInt(
        value.substring(0, value.length() - CSSStyleDeclaration.Unit.PX.length()));
  }

  /*
   * TODO: in a separate CL, move all clients of this one over to
   * the second version of this method (have to make sure they adhere by the new
   * contract -- to much work for this CL)
   */
  /**
   * Sets the visibility of an element via the {@code display} CSS property. When visible, the
   * {@code display} will be set to {@code block}.
   */
  @Deprecated
  public static void setDisplayVisibility(Element element, boolean visible) {
    if (visible) {
      element.getStyle().setDisplay(CSSStyleDeclaration.Display.BLOCK);
    } else {
      element.getStyle().setDisplay(CSSStyleDeclaration.Display.NONE);
    }
  }

  /**
   * Sets the visibility of an element via the {@code display} CSS property.
   *
   * @param defaultVisible if true, the element is visible by default (according to its classes)
   * @param displayVisibleValue if {@code defaultVisible} is false, this must be given. This is the
   *        preferred 'display' value to make it visible
   */
  public static void setDisplayVisibility2(Element element, boolean visible,
      boolean defaultVisible, String displayVisibleValue) {
    if (visible) {
      if (defaultVisible) {
        element.getStyle().removeProperty("display");
      } else {
        element.getStyle().setDisplay(displayVisibleValue);
      }
    } else {
      if (defaultVisible) {
        element.getStyle().setDisplay(CSSStyleDeclaration.Display.NONE);
      } else {
        element.getStyle().removeProperty("display");
      }
    }
  }

  /**
   * Sets the visibility of an element via the {@code display} CSS property.
   * This should not be used if the default CSS keeps the element 'display: none'.
   *
   * When hidden, the display property on the element style will be set to none,
   * when visible the display property will be removed.
   */
  public static void setDisplayVisibility2(Element element, boolean visible) {
    setDisplayVisibility2(element, visible, true, null);
  }

  public static void setBoxShadow(Element element, String value) {
    element.getStyle().setProperty("-moz-box-shadow", value);
    element.getStyle().setProperty("-webkit-box-shadow", value);
    element.getStyle().setProperty("box-shadow", value);
  }

  public static void removeBoxShadow(Element element) {
    element.getStyle().removeProperty("-moz-box-shadow");
    element.getStyle().removeProperty("-webkit-box-shadow");
    element.getStyle().removeProperty("box-shadow");
  }

  public static void setUserSelect(Element element, boolean selectable) {
    String value = selectable ? "text" : "none";
    element.getStyle().setProperty("user-select", value);
    element.getStyle().setProperty("-moz-user-select", value);
    element.getStyle().setProperty("-webkit-user-select", value);
  }

  public static boolean isPixels(String value) {
    return value.toLowerCase().endsWith(CSSStyleDeclaration.Unit.PX);
  }

  public static native CSSStyleDeclaration getComputedStyle(Element element) /*-{
    return window.getComputedStyle(element);
  }-*/;

  /**
   * Sets a CSS property on an element as the new value, returning the
   * previously set value.
   */
  public static String setAndSaveProperty(Element element, String propertyName, String value) {
    String savedValue = element.getStyle().getPropertyValue(propertyName);
    element.getStyle().setProperty(propertyName, value);
    return savedValue;
  }

  public static void setTop(Element element, int value, Unit unit) {
    String topValue = Integer.toString(value) + unit;
    element.getStyle().setTop(topValue);
  }
}
