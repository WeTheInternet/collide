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

package com.google.collide.client.ui.dropdown;

import com.google.collide.json.shared.JsonArray;
import com.google.gwt.user.client.Timer;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;
import elemental.html.InputElement;

/**
 * Provides autocomplete functionality for an input using simple list.
 * 
 */
public class AutocompleteController<M> {

  /*
   * hide long enough for the click to be caught by handlers but not long enough
   * to be noticable by the user.
   */
  private static final int HIDE_DELAY = 100;

  public static <M> AutocompleteController<M> create(
      InputElement inputBox, DropdownController<M> controller, AutocompleteHandler<M> callback) {
    return new AutocompleteController<M>(inputBox, controller, callback);
  }

  public interface AutocompleteHandler<M> {
    public JsonArray<M> doCompleteQuery(String query);

    public void onItemSelected(M item);
  }

  /**
   * The amount of time to wait after the user has finished typing before
   * updating autocompletion results.
   */
  private static final int AUTOCOMPLETE_DELAY = 30;

  private final InputElement inputBox;
  private final DropdownController<M> dropdown;
  private final AutocompleteHandler<M> callback;
  private int minimumCharactersBeforeCompletion = 1;

  public AutocompleteController(
      InputElement inputBox, DropdownController<M> dropdown, AutocompleteHandler<M> callback) {
    this.inputBox = inputBox;
    this.dropdown = dropdown;
    this.callback = callback;

    dropdown.preventDefaultOnMouseDown();
    attachHandlers();
  }

  public DropdownController<M> getDropdown() {
    return dropdown;
  }

  /**
   * Specifies the minimum number of characters to be typed in before completion
   * will take place.
   */
  public void setMinimumCharactersBeforeCompletion(int minimum) {
    minimumCharactersBeforeCompletion = minimum;
  }

  private void attachHandlers() {

    inputBox.addEventListener(Event.INPUT, new EventListener() {
      final Timer deferredShow = new Timer() {
        @Override
        public void run() {
          JsonArray<M> items = callback.doCompleteQuery(inputBox.getValue());
          if (items.size() > 0) {
            dropdown.setItems(items);
            dropdown.show();
          } else {
            dropdown.hide();
          }
        }
      };

      @Override
      public void handleEvent(Event evt) {
        KeyboardEvent event = (KeyboardEvent) evt;

        if (inputBox.getValue().length() < minimumCharactersBeforeCompletion) {
          dropdown.hide();
        } else {
          deferredShow.cancel();
          deferredShow.schedule(AUTOCOMPLETE_DELAY);
        }
      }
    }, false);

    inputBox.addEventListener(Event.BLUR, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        dropdown.hide();
      }
    }, false);

    inputBox.addEventListener(Event.KEYUP, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        KeyboardEvent event = (KeyboardEvent) evt;

        if (event.getKeyCode() == KeyCode.ESC) {
          dropdown.hide();
        }
      }
    }, false);

  }
}
