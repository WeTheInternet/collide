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

import com.google.collide.client.util.input.ModifierKeys;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import java.util.Set;

/**
 * Test wrapper for keyboard inputs.
 *
 */
public class TestSignalEvent implements SignalEvent {

  private final Set<Integer> modifiers = Sets.newHashSet();
  private final int key;
  private final KeySignalType signalType;

  public TestSignalEvent(int key) {
    this(key, new int[]{});
  }

  public TestSignalEvent(int key, KeySignalType signalType, int... modifiers) {
    for (int mod : modifiers) {
      this.modifiers.add(mod);
    }
    this.key = key;
    this.signalType = signalType;
  }

  public TestSignalEvent(int key, int... modifiers) {
    this(key, null, modifiers);
  }

  @Override
  public Event asEvent() {
    return null;
  }

  @Override
  public boolean getAltKey() {
    return modifiers.contains(ModifierKeys.ALT);
  }

  @Override
  public boolean getCommandKey() {
    return modifiers.contains(ModifierKeys.ACTION);
  }

  @Override
  public boolean getCtrlKey() {
    return modifiers.contains(ModifierKeys.ACTION);
  }

  @Override
  public int getKeyCode() {
    return key;
  }

  @Override
  public KeySignalType getKeySignalType() {
    return signalType;
  }

  @Override
  public boolean getMetaKey() {
    return false;
  }

  @Override
  public int getMouseButton() {
    return 0;
  }

  @Override
  public MoveUnit getMoveUnit() {
    return null;
  }

  @Override
  public boolean getShiftKey() {
    return modifiers.contains(ModifierKeys.SHIFT);
  }

  @Override
  public Element getTarget() {
    return null;
  }

  @Override
  public String getType() {
    return "keydown";
  }

  @Override
  public boolean isClickEvent() {
    return false;
  }

  @Override
  public boolean isClipboardEvent() {
    return false;
  }

  @Override
  public boolean isCombo(int letter, KeyModifier modifier) {
    return false;
  }

  @Override
  public boolean isCompositionEvent() {
    return false;
  }

  @Override
  public boolean isCopyEvent() {
    return false;
  }

  @Override
  public boolean isCutEvent() {
    return false;
  }

  @Override
  public boolean isFocusEvent() {
   return false;
  }

  @Override
  public boolean isImeKeyEvent() {
    return false;
  }

  @Override
  public boolean isKeyEvent() {
    return (key != 0);
  }

  @Override
  public boolean isMouseButtonEvent() {
    return false;
  }

  @Override
  public boolean isMouseButtonlessEvent() {
    return false;
  }

  @Override
  public boolean isMouseEvent() {
    return false;
  }

  @Override
  public boolean isMutationEvent() {
    return false;
  }

  @Override
  public boolean isOnly(int letter) {
    return (letter == key);
  }

  @Override
  public boolean isPasteEvent() {
    return false;
  }

  @Override
  public boolean isRedoCombo() {
    return false;
  }

  @Override
  public boolean isUndoCombo() {
    return false;
  }

  @Override
  public void preventDefault() {
  }

  @Override
  public void stopPropagation() {
  }
}
