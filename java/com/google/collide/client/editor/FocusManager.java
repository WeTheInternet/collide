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

package com.google.collide.client.editor;

import com.google.collide.client.util.Elements;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerManager.Dispatcher;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;

/**
 * Tracks the focus state of the editor.
 *
 */
public class FocusManager {

  /**
   * A listener that is called when the editor receives or loses focus.
   */
  public interface FocusListener {
    void onFocusChange(boolean hasFocus);
  }

  private final ListenerManager<FocusManager.FocusListener> focusListenerManager = ListenerManager
      .create();
  private boolean hasFocus;
  private final Element inputElement;


  FocusManager(Buffer buffer, Element inputElement) {
    this.inputElement = inputElement;

    attachEventHandlers(buffer);
    hasFocus = inputElement.equals(Elements.getActiveElement());
  }

  private void attachEventHandlers(Buffer buffer) {
    inputElement.addEventListener(Event.FOCUS, new EventListener() {
      private final Dispatcher<FocusManager.FocusListener> dispatcher =
          new Dispatcher<FocusManager.FocusListener>() {
            @Override
            public void dispatch(FocusListener listener) {
              listener.onFocusChange(true);
            }
          };

      @Override
      public void handleEvent(Event evt) {
        hasFocus = true;
        focusListenerManager.dispatch(dispatcher);
      }
    }, false);

    inputElement.addEventListener(Event.BLUR, new EventListener() {
      private final Dispatcher<FocusManager.FocusListener> dispatcher =
          new Dispatcher<FocusManager.FocusListener>() {
            @Override
            public void dispatch(FocusListener listener) {
              listener.onFocusChange(false);
            }
          };

      @Override
      public void handleEvent(Event evt) {
        hasFocus = false;
        focusListenerManager.dispatch(dispatcher);
      }
    }, false);

    buffer.getMouseClickListenerRegistrar().add(new Buffer.MouseClickListener() {
      @Override
      public void onMouseClick(int x, int y) {
        focus();
      }
    });
  }

  public ListenerRegistrar<FocusManager.FocusListener> getFocusListenerRegistrar() {
    return focusListenerManager;
  }

  public boolean hasFocus() {
    return hasFocus;
  }

  public void focus() {
    inputElement.focus();
  }
}
