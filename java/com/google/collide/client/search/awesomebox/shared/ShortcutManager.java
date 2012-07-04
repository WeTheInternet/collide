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

package com.google.collide.client.search.awesomebox.shared;

import elemental.events.KeyboardEvent;


// TODO: Refactor when we figure out some sort of reusable shortcut
// component that is a permenant solution.
public interface ShortcutManager {

  public interface ShortcutPressedCallback {
    public void onShortcutPressed(KeyboardEvent event);
  }

  /**
   * Adds a shortcut to the shortcut manager. If the shortcut already exists it
   * will be overwritten.
   *
   * @param modifiers Logical OR of modifier keys
   * @param keycode KeyCode
   */
  public void addShortcut(int modifiers, int keycode, ShortcutPressedCallback callback);

  /**
   * Clears all shortcuts in the manager.
   */
  public void clearShortcuts();

  /**
   * On key down we check for shortcuts.
   */
  public void onKeyDown(KeyboardEvent event);
}
