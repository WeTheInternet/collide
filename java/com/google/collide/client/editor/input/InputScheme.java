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

package com.google.collide.client.editor.input;

import elemental.js.util.JsMapFromIntTo;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * Controller around a group of {@link InputMode}s to direct text/modifier
 * key input to the active mode. Provides access to the current {@link InputController}
 * to the active mode for performing shortcut actions.
 * 
 * Each scheme can contain additional state that needs to be shared between modes,
 * such as a vi line-mode search "/class", and additional command-mode next match
 * "n" commands
 * 
 * Tied to the lifetime of an editor instance
 * 
 *
 */
public abstract class InputScheme {
  /**
   * Store a reference to the editor's input controller to pass to active mode
   */
  private InputController inputController;
  
  private InputMode activeMode = null;
  
  /**
   * Map from mode number to mode object. Mode numbers should be constants
   * defined for each scheme.
   */
  private JsMapFromIntTo<InputMode> modes;
  
  public InputScheme() {
    this.inputController = null;
    this.modes = JsMapFromIntTo.create();
  }
  
  public InputScheme(InputController input) {
    this.inputController = input;
    this.modes = JsMapFromIntTo.create();
  }
  
  /**
   * Add all Scheme modes and setup the default {@link InputMode} by calling
   * switchMode. Optionally make any scheme-specific document changes 
   * (add status bar, etc)
   */
  public abstract void setup();
  
  /**
   * Called when switching editor modes, this should undo all document 
   * changes made in {@link InputScheme#setup()}
   */
  public void teardown() {
    if (activeMode != null) {
      activeMode.teardown();
    }
  }
  
  /**
   * Add a new mode to this scheme
   */
  public void addMode(int modeNumber, InputMode mode) {
    mode.setScheme(this);
    modes.put(modeNumber, mode);
  }
  
  public InputController getInputController() {
    return inputController;
  }
  
  public InputMode getMode() {
    return activeMode;
  }
  
  /**
   * Switch to the new mode:
   *    call teardown() on active mode (if there is one)
   *    call setup() on new mode
   */
  public void switchMode(int modeNumber) {
    if (modes.hasKey(modeNumber)) {
      if (activeMode != null) {
        activeMode.teardown();
      }
      activeMode = modes.get(modeNumber);
      activeMode.setup();
    }
  }
  
  /**
   * Called from the event handler, dispatch this event to the active mode
   */
  public boolean handleEvent(SignalEvent event, String text) {
    if (activeMode != null) {
      return activeMode.handleEvent(event, text);
    } else {
      return false;
    }
  }
  
  /**
   * This is called after a shortcut has been dispatched.
   */
  protected void handleShortcutCalled() {
  }
}
