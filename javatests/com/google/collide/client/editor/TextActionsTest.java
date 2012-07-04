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

package com.google.collide.client.editor;

import static com.google.collide.client.code.autocomplete.TestUtils.createDocumentParser;

import com.google.collide.client.autoindenter.Autoindenter;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.input.TestSignalEvent;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.testutil.TestSchedulerImpl;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.core.client.Scheduler;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import elemental.events.KeyboardEvent;

/**
 * Tests for {@link TextActions} and their bindings in
 * {@link com.google.collide.client.editor.input.DefaultScheme}.
 */
public class TextActionsTest extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testSplitLine() {
    final TestSignalEvent ctrlEnter = new TestSignalEvent(KeyboardEvent.KeyCode.ENTER,
        SignalEvent.KeySignalType.INPUT, ModifierKeys.ACTION);

    String text = "  color: black;";
    String expected = "  color: \n  black;";
    checkAction(ctrlEnter, text, expected, 0, 9, 0, 9, 0, 9);
  }

  public void testSplitLineWithSelection() {
    final TestSignalEvent ctrlEnter = new TestSignalEvent(KeyboardEvent.KeyCode.ENTER,
        SignalEvent.KeySignalType.INPUT, ModifierKeys.ACTION);

    String text = "  color: black;";
    String expected = "  color\n  black;";
    checkAction(ctrlEnter, text, expected, 0, 7, 0, 9, 0, 7);
  }

  public void testSplitLineWithReverseSelection() {
    final TestSignalEvent ctrlEnter = new TestSignalEvent(KeyboardEvent.KeyCode.ENTER,
        SignalEvent.KeySignalType.INPUT, ModifierKeys.ACTION);

    String text = "  color: black;";
    String expected = "  color\n  black;";
    checkAction(ctrlEnter, text, expected, 0, 9, 0, 7, 0, 7);
  }

  public void testStartNewLine() {
    final TestSignalEvent shiftEnter = new TestSignalEvent(KeyboardEvent.KeyCode.ENTER,
        SignalEvent.KeySignalType.INPUT, ModifierKeys.SHIFT);

    String text = "  color: black;";
    String expected = "  color: black;\n  ";
    checkAction(shiftEnter, text, expected, 0, 8, 0, 8, 1, 2);
  }

  public void testStartNewLineWithSelection() {
    final TestSignalEvent shiftEnter = new TestSignalEvent(KeyboardEvent.KeyCode.ENTER,
        SignalEvent.KeySignalType.INPUT, ModifierKeys.SHIFT);

    String text = "  color: black;";
    String expected = "  color: black;\n  ";
    checkAction(shiftEnter, text, expected, 0, 0, 0, 8, 1, 2);
  }

  private void checkAction(final SignalEvent trigger, String text, String expected,
      int line1, int column1, int line2, int column2,
      int expectedLine, int expectedColumn) {
    Document document = Document.createFromString(text);
    final Editor editor = Editor.create(new MockAppContext());
    editor.setDocument(document);

    editor.getInput().getActionExecutor().addDelegate(TextActions.INSTANCE);

    PathUtil path = new PathUtil("test.css");
    DocumentParser documentParser = createDocumentParser(path);
    Autoindenter autoindenter = Autoindenter.create(documentParser, editor);

    LineFinder lineFinder = editor.getDocument().getLineFinder();
    editor.getSelection().setSelection(
        lineFinder.findLine(line1), column1, lineFinder.findLine(line2), column2);

    final JsonArray<Scheduler.ScheduledCommand> scheduled = JsonCollections.createArray();

    TestSchedulerImpl.AngryScheduler scheduler = new TestSchedulerImpl.AngryScheduler() {
      @Override
      public void scheduleDeferred(ScheduledCommand scheduledCommand) {
        // Do nothing
      }

      @Override
      public void scheduleFinally(ScheduledCommand scheduledCommand) {
        scheduled.add(scheduledCommand);
      }
    };

    Runnable triggerClicker = new Runnable() {
      @Override
      public void run() {
        editor.getInput().processSignalEvent(trigger);
      }
    };

    try {
      TestSchedulerImpl.runWithSpecificScheduler(triggerClicker, scheduler);
    } finally {
      autoindenter.teardown();
    }

    if (scheduled.size() != 1) {
      fail("exactly 1 scheduled command expected");
    }
    scheduled.get(0).execute();

    String result = editor.getDocument().asText();
    assertEquals("textual result", expected, result);
    Position[] selectionRange = editor.getSelection().getSelectionRange(false);
    assertFalse("no selection", editor.getSelection().hasSelection());
    assertEquals("cursor line", expectedLine, selectionRange[0].getLineNumber());
    assertEquals("cursor column", expectedColumn, selectionRange[0].getColumn());
  }
}
