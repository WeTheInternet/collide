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

import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.util.input.KeyCodeMap;
import com.google.collide.client.util.input.ModifierKeys;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * An input scheme to support the editor's read-only mode.
 */
public class ReadOnlyScheme extends InputScheme {

  private class ReadOnlyInputMode extends InputMode {
    @Override
    public void setup() {
      addShortcut(new EventShortcut(ModifierKeys.ACTION, 'a') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          getInputController().getSelection().selectAll();
          return true;
        }
      });
    }

    @Override
    public void teardown() {
    }

    @Override
    public boolean onDefaultInput(SignalEvent event, char character) {
      int key = KeyCodeMap.getKeyFromEvent(event);
      ViewportModel viewport = getInputController().getViewportModel();

      switch (key) {
        case KeyCodeMap.ARROW_LEFT:
        case KeyCodeMap.ARROW_RIGHT:
          viewport.shiftHorizontally(key == KeyCodeMap.ARROW_RIGHT);
          return true;

        case KeyCodeMap.ARROW_UP:
        case KeyCodeMap.ARROW_DOWN:
          viewport.shiftVertically(key == KeyCodeMap.ARROW_DOWN, false);
          return true;

        case KeyCodeMap.PAGE_UP:
        case KeyCodeMap.PAGE_DOWN:
          viewport.shiftVertically(key == KeyCodeMap.PAGE_DOWN, true);
          return true;

        case KeyCodeMap.HOME:
        case KeyCodeMap.END:
          viewport.jumpTo(key == KeyCodeMap.END);
          return true;
      }

      return false;
    }

    @Override
    public boolean onDefaultPaste(SignalEvent signal, String text) {
      return false;
    }
  }


  public ReadOnlyScheme(InputController input) {
    super(input);

    addMode(1, new ReadOnlyInputMode());
  }

  @Override
  public void setup() {
    switchMode(1);
  }
}
