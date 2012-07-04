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

import static org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType.NAVIGATION;

import com.google.collide.client.editor.Editor;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.util.input.KeyCodeMap;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test cases for {@link DefaultScheme}.
 */
public class DefaultSchemeTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testAltMovementIsNotCosumed() {
    DefaultScheme scheme = new DefaultScheme(new InputController());

    int[] keys = new int[] {
        KeyCodeMap.ARROW_LEFT, KeyCodeMap.ARROW_RIGHT, KeyCodeMap.ARROW_UP, KeyCodeMap.ARROW_DOWN};
    int[][] modifiers = new int[][] {{ModifierKeys.ALT},
        {ModifierKeys.ALT, ModifierKeys.ACTION},
        {ModifierKeys.ALT, ModifierKeys.SHIFT},
        {ModifierKeys.ALT, ModifierKeys.ACTION, ModifierKeys.SHIFT}};

    for (int i = 0; i < keys.length; i++) {
      for (int j = 0; j < modifiers.length; j++) {
        TestSignalEvent event = new TestSignalEvent(keys[i], NAVIGATION, modifiers[j]);
        assertFalse("i = " + i + "; j = " + j, scheme.defaultMode.onDefaultInput(event, (char) 0));
      }
    }
  }

  public void testCutWithoutSelectionAtLastEmptyLine() {
    String text = "first\nsecond\n";
    checkCut(text, 2, 0, text);
    // Assert: still alive.
  }

  private void checkCut(String text, int line, int column, String expected) {
    Document document = Document.createFromString(text);
    final Editor editor = Editor.create(new MockAppContext());
    editor.setDocument(document);

    LineInfo lineInfo = document.getLineFinder().findLine(line);
    editor.getSelection().setSelection(lineInfo, column, lineInfo, column);
    editor.getInput().processSignalEvent(TestCutPasteEvent.create(null));
    assertEquals(expected, document.asText());
  }
}
