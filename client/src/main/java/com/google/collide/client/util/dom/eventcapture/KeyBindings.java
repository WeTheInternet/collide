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

package com.google.collide.client.util.dom.eventcapture;

import com.google.collide.client.util.input.CharCodeWithModifiers;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.json.client.JsoStringMap;

/**
 * Standard key bindings for Collide.
 */
public class KeyBindings {

  private static final String LOCAL_REPLACE = "local_replace";
  private static final String LOCAL_FIND = "local_find";
  private static final String GOTO = "goto";
  private static final String SNAPSHOT = "snapshot";

  private JsoStringMap<CharCodeWithModifiers> map = JsoStringMap.create();

  public KeyBindings() {
    map.put(LOCAL_FIND, new CharCodeWithModifiers(ModifierKeys.ACTION, 'f'));
    map.put(LOCAL_REPLACE, new CharCodeWithModifiers(ModifierKeys.ACTION, 'F'));
    map.put(GOTO, new CharCodeWithModifiers(ModifierKeys.ACTION, 'g'));
    map.put(SNAPSHOT, new CharCodeWithModifiers(ModifierKeys.ACTION, 's'));

    // TODO: Add custom key bindings.
  }
  
  /**
   * @return key for local find
   */
  public CharCodeWithModifiers localFind() {
    return map.get(LOCAL_FIND);
  }

  /**
   * @return key for local replace
   */
  public CharCodeWithModifiers localReplace() {
    return map.get(LOCAL_REPLACE);
  }

  /**
   * @return keycode for goto line/etc.
   */
  public CharCodeWithModifiers gotoLine() {
    return map.get(GOTO);
  }
  
  /**
   * @return keycode for making a snapshot.
   */
  public CharCodeWithModifiers snapshot() {
    return map.get(SNAPSHOT);
  }
}
