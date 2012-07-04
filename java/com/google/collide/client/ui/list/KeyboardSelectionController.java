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

package com.google.collide.client.ui.list;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;
import elemental.html.Element;

/**
 * A controller which attaches to an input element and proxies keyboard
 * navigation events to an object which {@link HasSelection}.
 *
 */
public class KeyboardSelectionController {

  private final Element inputElement;
  private final HasSelection list;
  private boolean handlerEnabled = false;

  /**
   * Creates a new KeyboardSelectionController which proxies keyboard events
   * from inputElement to the supplied list.
   */
  public KeyboardSelectionController(Element inputElement, HasSelection list) {
    this.inputElement = inputElement;
    this.list = list;

    attachHandlers();
  }

  /**
   * Disables/Enables the keyboard navigation. When the handler is disabled the
   * controller will not proxy any keyboard input. This is useful if the list is
   * hidden or disabled.
   */
  public void setHandlerEnabled(boolean enabled) {
    handlerEnabled = enabled;
    // reset the selection to the first item
    list.setSelectedItem(0);
  }

  private void attachHandlers() {
    inputElement.addEventListener(Event.KEYDOWN, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (!handlerEnabled) {
          return;
        }

        KeyboardEvent event = (KeyboardEvent) evt;
        if (event.getKeyCode() == KeyCode.DOWN) {
          list.selectNext();
          evt.preventDefault();
        } else if (event.getKeyCode() == KeyCode.UP) {
          list.selectPrevious();
          evt.preventDefault();
        } else if (event.getKeyCode() == KeyCode.ENTER) {
          list.handleClick();
        }
      }
    }, false);
  }
}
