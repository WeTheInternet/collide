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

package com.google.collide.client.search.awesomebox;

import com.google.collide.client.search.awesomebox.shared.MappedShortcutManager;
import com.google.collide.client.search.awesomebox.shared.ShortcutManager;
import com.google.collide.client.search.awesomebox.shared.ShortcutManager.ShortcutPressedCallback;
import com.google.collide.client.util.input.ModifierKeys;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;

/**
 * Tests the context shortcut manager to ensure it calls back correctly.
 */
public class MappedShortcutManagerTest extends TestCase {
  ShortcutManager shortcutManager;

  private KeyboardEvent expectKeyboard(int modifiers, int keyCode, int charCode) {
    KeyboardEvent keyEvent = EasyMock.createMock(KeyboardEvent.class);
    EasyMock.expect(keyEvent.getKeyCode()).andReturn(keyCode).anyTimes();
    EasyMock.expect(keyEvent.getCharCode()).andReturn(charCode).anyTimes();
    EasyMock.expect(keyEvent.isAltKey()).andReturn(
        (modifiers & ModifierKeys.ALT) == ModifierKeys.ALT).anyTimes();
    EasyMock.expect(keyEvent.isCtrlKey()).andReturn(
        (modifiers & ModifierKeys.ACTION) == ModifierKeys.ACTION).anyTimes();
    EasyMock.expect(keyEvent.isMetaKey()).andReturn(
        (modifiers & ModifierKeys.ACTION) == ModifierKeys.ACTION).anyTimes();
    EasyMock.expect(keyEvent.isShiftKey()).andReturn(
        (modifiers & ModifierKeys.SHIFT) == ModifierKeys.SHIFT).anyTimes();
    EasyMock.replay(keyEvent);
    return keyEvent;
  }

  @Override
  public void setUp() {
    shortcutManager = new MappedShortcutManager();
  }

  public void testShortcutCallbackCalled() {
    KeyboardEvent firstKey = expectKeyboard(ModifierKeys.ALT, KeyCode.A, 'a');
    KeyboardEvent secondKey = expectKeyboard(ModifierKeys.ALT | ModifierKeys.SHIFT, KeyCode.B, 'B');
    KeyboardEvent thirdKey =
        expectKeyboard(ModifierKeys.ALT | ModifierKeys.SHIFT | ModifierKeys.ACTION, KeyCode.B, 'B');
    KeyboardEvent fourthKey = expectKeyboard(0, KeyCode.A, 'a');

    ShortcutPressedCallback callback = EasyMock.createMock(ShortcutPressedCallback.class);
    callback.onShortcutPressed(firstKey);
    callback.onShortcutPressed(secondKey);
    callback.onShortcutPressed(thirdKey);
    callback.onShortcutPressed(fourthKey);
    EasyMock.replay(callback);

    shortcutManager.addShortcut(ModifierKeys.ALT, KeyCode.A, callback);
    shortcutManager.addShortcut(ModifierKeys.ALT | ModifierKeys.SHIFT, KeyCode.B, callback);
    shortcutManager.addShortcut(
        ModifierKeys.ALT | ModifierKeys.SHIFT | ModifierKeys.ACTION, KeyCode.B, callback);
    shortcutManager.addShortcut(0, KeyCode.A, callback);

    shortcutManager.onKeyDown(firstKey);
    shortcutManager.onKeyDown(secondKey);
    shortcutManager.onKeyDown(thirdKey);
    shortcutManager.onKeyDown(fourthKey);

    shortcutManager.onKeyDown(expectKeyboard(0, KeyCode.E, 'e'));
    EasyMock.verify(callback);
  }


  public void testClearShortcuts() {
    KeyboardEvent firstKey = expectKeyboard(ModifierKeys.ALT, KeyCode.A, 'a');

    ShortcutPressedCallback callback = EasyMock.createMock(ShortcutPressedCallback.class);
    callback.onShortcutPressed(firstKey);
    EasyMock.replay(callback);

    shortcutManager.addShortcut(ModifierKeys.ALT, KeyCode.A, callback);
    shortcutManager.onKeyDown(firstKey);
    shortcutManager.clearShortcuts();
    shortcutManager.onKeyDown(firstKey);

    EasyMock.verify(callback);
  }

  public void testExistingShortcutAddedCausesLastOneToRun() {
    KeyboardEvent firstKey = expectKeyboard(ModifierKeys.ALT, KeyCode.A, 'a');

    ShortcutPressedCallback callback = EasyMock.createMock(ShortcutPressedCallback.class);
    EasyMock.replay(callback);

    ShortcutPressedCallback secondCallback = EasyMock.createMock(ShortcutPressedCallback.class);
    secondCallback.onShortcutPressed(firstKey);
    EasyMock.replay(secondCallback);

    shortcutManager.addShortcut(ModifierKeys.ALT, KeyCode.A, callback);
    shortcutManager.addShortcut(ModifierKeys.ALT, KeyCode.A, secondCallback);
    shortcutManager.onKeyDown(firstKey);

    EasyMock.verify(callback);
    EasyMock.verify(secondCallback);
  }
}
