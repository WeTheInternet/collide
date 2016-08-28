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

import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;

import elemental.events.KeyboardEvent;

/**
 * The simplest shortcut manager I could conceive.
 */
// TODO: HA this will need rewriting
public class MappedShortcutManager implements ShortcutManager {

  public JsonStringMap<ShortcutPressedCallback> shortcutMap;

  public MappedShortcutManager() {
    shortcutMap = JsonCollections.createMap();
  }

  @Override
  public void addShortcut(int modifiers, int keyCode, ShortcutPressedCallback callback) {
    shortcutMap.put(getKeyForShortcut(modifiers, keyCode), callback);
  }

  /**
   * Returns the string key in the map for the given modifier keys and key code.
   */
  private String getKeyForShortcut(int modifiers, int keyCode) {
    return String.valueOf(modifiers) + "-" + String.valueOf(keyCode);
  }

  @Override
  public void onKeyDown(KeyboardEvent event) {
    int modifiers = ModifierKeys.computeExactModifiers(event);
    ShortcutPressedCallback callback =
        shortcutMap.get(getKeyForShortcut(modifiers, event.getKeyCode()));
    if (callback != null) {
      callback.onShortcutPressed(event);
    }
  }

  @Override
  public void clearShortcuts() {
    // TODO: Better way to clear this
    shortcutMap = JsonCollections.createMap();
  }

}
