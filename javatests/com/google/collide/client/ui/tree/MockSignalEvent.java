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

package com.google.collide.client.ui.tree;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * Simple stub for testing selection model responses to CTRL and SHIFT clicks.
 * 
 */
class MockSignalEvent implements SignalEvent {
  private final boolean ctrl;
  private final boolean shift;

  MockSignalEvent(boolean ctrl, boolean shift) {
    this.ctrl = ctrl;
    this.shift = shift;
  }
  
  @Override
  public Event asEvent() {
    return null;
  }

  @Override
  public boolean getAltKey() {
    return false;
  }

  @Override
  public boolean getCommandKey() {
    return ctrl;
  }

  @Override
  public boolean getCtrlKey() {
    return ctrl;
  }

  @Override
  public int getKeyCode() {
    return 0;
  }

  @Override
  public KeySignalType getKeySignalType() {
    return null;
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
    return shift;
  }

  @Override
  public Element getTarget() {
    return null;
  }

  @Override
  public String getType() {
    return null;
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
    return false;
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
    return false;
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
