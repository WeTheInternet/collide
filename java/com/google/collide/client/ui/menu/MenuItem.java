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

package com.google.collide.client.ui.menu;

import com.google.collide.client.util.Elements;

import elemental.events.EventListener;
import elemental.html.Element;

/**
 * Simple tuple of a label and an action.
 *
 */
public class MenuItem<T> {
  /**
   * Behavior invoked for a MenuItem.
   */
  public interface MenuAction<T> {
    /**
     * @param context the context, or null if the action is being performed on the root
     */
    public void doAction(T context);
  }

  /**
   * Allows for custom rendering of a MenuItem.
   */
  public interface MenuItemRenderer<T> {
    /**
     * Called when the menu item is to be created. Any returned element will
     * automatically have the menuItem css style added.
     *
     * @return The menu item element.
     */
    public Element render(MenuItem<T> item);

    /**
     * Called as the menu item is being shown so that it can make conditional
     * modifications to the rendered menu item.
     */
    public void onShowing(MenuItem<T> item, T context);
  }

  protected final MenuAction<T> action;
  private final String label;
  private boolean enabled = true;
  private Element element;
  private MenuItemRenderer<T> renderer;

  public MenuItem(String label, MenuAction<T> action) {
    this.action = action;
    this.label = label;
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }
  
  public void setMenuItemRenderer(MenuItemRenderer<T> renderer) {
    this.renderer = renderer;
  }

  /**
   * Dispatches the menu action if the MenuItem is enabled and we have a valid
   * MenuAction to dispatch.
   *
   * A <code>null</code> context implies that this action refers to the Tree's
   * root.
   */
  public void dispatchAction(T context) {
    if (isEnabled() && action != null) {
      action.doAction(context);
    }
  }
  
  public void dispatchShowing(T context) {
    if (isEnabled() && renderer != null) {
      renderer.onShowing(this, context);
    }
  }

  /**
   * @param enabled the enabled to set
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    setEnabledStyle();
  }

  /**
   * @return the enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Creates the DOM structure of a given element, using the given event handler
   * as the "appropriate" callback.
   *
   * @return the element of the new menu item
   */
  public Element createDom(
      String menuItemCss, EventListener clickListener, EventListener mouseDownListener) {
    if (getElement() == null) {
      if (renderer != null) {
        element = renderer.render(this);
        element.addClassName(menuItemCss);
      } else {
        element = Elements.createDivElement(menuItemCss);
        element.setTextContent(getLabel());
      }
      if (clickListener != null) {
        element.setOnClick(clickListener);
      }
      if (mouseDownListener != null) {
        element.setOnMouseDown(mouseDownListener);
      }
      setEnabledStyle();
    }
    return element;
  }

  /**
   * Gets the DOM element, or null if not initialized
   */
  public Element getElement() {
    return element;
  }

  private void setEnabledStyle() {
    if (getElement() != null) {
      element.setAttribute("enabled", enabled ? "true" : "false");
    }
  }
}
