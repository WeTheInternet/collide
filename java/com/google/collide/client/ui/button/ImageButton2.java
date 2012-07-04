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

package com.google.collide.client.ui.button;

import com.google.collide.client.ui.dropdown.DropdownWidgets.Resources;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.resources.client.ImageResource;

import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.ButtonElement;

/**
 * A button which centers an image inside of a button. This class is special since it uses multiple
 * background images to achieve the desired effect. NOTE: Would be significantly better if we just
 * used a less-style CSS parser for GWT. It is important that you call
 * {@link #initializeAfterAttachedToDom()} after creation.
 */
public class ImageButton2 {
  private final ButtonElement buttonElement;
  private final String ourTopLayer;

  public ImageButton2(Resources res, ImageResource img) {
    this(Elements.createButtonElement(res.baseCss().button()), img);
  }

  public ImageButton2(ButtonElement buttonElement, ImageResource img) {
    this.ourTopLayer = "url(" + img.getSafeUri().asString() + ") no-repeat 50% 50%";
    this.buttonElement = buttonElement;
    attachEvents();
  }

  private void attachEvents() {
    EventListener listener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        refresh();
      }
    };

    // we support over/pressed
    buttonElement.addEventListener(Event.MOUSEDOWN, listener, false);
    buttonElement.addEventListener(Event.MOUSEUP, listener, false);
    buttonElement.addEventListener(Event.MOUSEOVER, listener, false);
    buttonElement.addEventListener(Event.MOUSEOUT, listener, false);
  }

  /**
   * Initializes the button after it's button has been attached to the DOM. This means fully
   * attached to the document body not just appended to a container.
   */
  public void initializeAfterAttachedToDom() {
    refresh();
  }

  /**
   * Refreshes the background image of the image button.
   */
  private void refresh() {
    CSSStyleDeclaration declaration = CssUtils.getComputedStyle(buttonElement);
    if (StringUtils.isNullOrEmpty(declaration.getPropertyValue("background"))) {
      // bail if we're not attached to the dom
      return;
    }

    buttonElement.getStyle().removeProperty("background");
    String currentBackgroundImage = declaration.getPropertyValue("background");
    buttonElement.getStyle().setProperty("background", ourTopLayer + "," + currentBackgroundImage);
  }

  public ButtonElement getButtonElement() {
    return buttonElement;
  }
}
