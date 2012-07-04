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

import com.google.collide.json.client.Jso;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import javax.annotation.Nullable;

/**
 * Implementation that simulates paste event.
 */
public class TestCutPasteEvent extends Jso implements SignalEvent {

  protected TestCutPasteEvent() {
  }

  public static native TestCutPasteEvent create(@Nullable String data) /*-{
    if (data) {
        return {'clipboardData' : {'getData': function() {return data;}}};
    } else {
        return {};
    }
  }-*/;

  @Override
  public final String getType() {
    return null;
  }

  @Override
  public final Element getTarget() {
    return null;
  }

  @Override
  public final boolean isKeyEvent() {
    return false;
  }

  @Override
  public final boolean isCompositionEvent() {
    return false;
  }

  @Override
  public final boolean isImeKeyEvent() {
    return false;
  }

  @Override
  public final boolean isMouseEvent() {
    return false;
  }

  @Override
  public final boolean isMouseButtonEvent() {
    return false;
  }

  @Override
  public final boolean isMouseButtonlessEvent() {
    return false;
  }

  @Override
  public final boolean isClickEvent() {
    return false;
  }

  @Override
  public final boolean isMutationEvent() {
    return false;
  }

  @Override
  public final boolean isClipboardEvent() {
    return false;
  }

  @Override
  public final boolean isFocusEvent() {
    return false;
  }

  @Override
  public final boolean isPasteEvent() {
    return this.hasOwnProperty("clipboardData");
  }

  @Override
  public final boolean isCutEvent() {
    return !isPasteEvent();
  }

  @Override
  public final boolean isCopyEvent() {
    return false;
  }

  @Override
  public final boolean getCommandKey() {
    return false;
  }

  @Override
  public final boolean getCtrlKey() {
    return false;
  }

  @Override
  public final boolean getMetaKey() {
    return false;
  }

  @Override
  public final boolean getAltKey() {
    return false;
  }

  @Override
  public final boolean getShiftKey() {
    return false;
  }

  @Override
  public final Event asEvent() {
    return (Event) ((Object) this);
  }

  @Override
  public final MoveUnit getMoveUnit() {
    return null;
  }

  @Override
  public final boolean isUndoCombo() {
    return false;
  }

  @Override
  public final boolean isRedoCombo() {
    return false;
  }

  @Override
  public final boolean isCombo(int letter, KeyModifier modifier) {
    return false;
  }

  @Override
  public final boolean isOnly(int letter) {
    return false;
  }

  @Override
  public final int getKeyCode() {
    return 0;
  }

  @Override
  public final int getMouseButton() {
    return 0;
  }

  @Override
  public final KeySignalType getKeySignalType() {
    return null;
  }

  @Override
  public final void stopPropagation() {
  }

  @Override
  public final void preventDefault() {
  }
}
