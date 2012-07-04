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
package com.google.collide.client.autoindenter;

import static com.google.collide.client.code.autocomplete.TestUtils.createDocumentParser;

import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.input.TestCutPasteEvent;
import com.google.collide.client.editor.input.TestSignalEvent;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.testutil.TestSchedulerImpl;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.input.KeyCodeMap;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.core.client.Scheduler;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * Tests for {@link Autoindenter}.
 */
public class AutoindenterTest extends SynchronousTestCase {

  static final SignalEvent TRIGGER_ENTER = new TestSignalEvent(
            KeyCodeMap.ENTER, SignalEvent.KeySignalType.INPUT, 0);

  @Override
  public String getModuleName() {
    return "com.google.collide.client.autoindenter.TestModule";
  }

  public void testAtStart() {
    String text = "    A\n    B";
    String expected = "\n    A\n    B";
    checkAutoindenter(text, 0, 0, 0, 0, TRIGGER_ENTER, expected, false);
  }

  public void testAfterSpace() {
    String text = "    A\n    B";
    String expected = " \n    A\n    B";
    checkAutoindenter(text, 0, 1, 0, 1, TRIGGER_ENTER, expected, true);
  }

  public void testAfterText() {
    String text = "    A.B";
    String expected = "    A.\n    B";
    checkAutoindenter(text, 0, 6, 0, 6, TRIGGER_ENTER, expected, true);
  }

  public void testAtEol() {
    String text = "    A;";
    String expected = "    A;\n    ";
    checkAutoindenter(text, 0, 6, 0, 6, TRIGGER_ENTER, expected, true);
  }

  public void testSourceLine() {
    String text = "  A;\n    B;\n      C;\n";
    String expected = "  A;\n    B;\n    \n      C;\n";
    checkAutoindenter(text, 1, 6, 1, 6, TRIGGER_ENTER, expected, true);
  }

  public void testEnterOnSelection() {
    String text = "  hello\n\n  world\n";
    checkAutoindenter(text, 1, 0, 2, 0, TRIGGER_ENTER, text, false);
  }

  public void testInsertLine() {
    String text = "  ThreadUtils.runInParallel(\n    function() {\n    },\n  );\n";
    SignalEvent trigger = TestCutPasteEvent.create("  driverF.login(LOGIN_F, PASS);\n");
    String expected = "  ThreadUtils.runInParallel(\n    function() {\n"
        + "  driverF.login(LOGIN_F, PASS);\n    },\n  );\n";
    checkAutoindenter(text, 2, 0, 2, 0, trigger, expected, false);
  }

  private static void checkAutoindenter(String text, int line1, int column1, int line2, int column2,
      final SignalEvent trigger, String expected, boolean allowScheduling) {
    PathUtil path = new PathUtil("test.js");
    DocumentParser documentParser = createDocumentParser(path);
    Document document = Document.createFromString(text);
    Editor editor = Editor.create(new MockAppContext());
    editor.setDocument(document);
    checkAutoindenter(line1, column1, line2, column2, trigger, expected, allowScheduling,
        documentParser, document, editor);
  }

  static void checkAutoindenter(int line1, int column1, int line2, int column2,
      final SignalEvent trigger, String expected, boolean allowScheduling,
      DocumentParser documentParser, Document document, final Editor editor) {
    Autoindenter autoindenter = Autoindenter.create(documentParser, editor);

    LineFinder lineFinder = document.getLineFinder();
    editor.getSelection().setSelection(
        lineFinder.findLine(line1), column1,
        lineFinder.findLine(line2), column2);

    Runnable triggerClicker = new Runnable() {
      @Override
      public void run() {
        editor.getInput().processSignalEvent(trigger);
      }
    };

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

    try {
      TestSchedulerImpl.runWithSpecificScheduler(triggerClicker, scheduler);
    } finally {
      autoindenter.teardown();
    }

    if (!allowScheduling) {
      if (scheduled.size() > 0) {
        fail("unexpected scheduling");
      }
    } else {
      if (scheduled.size() != 1) {
        fail("exactly 1 scheduled command expected, but " + scheduled.size() + " were scheduled");
      }
      scheduled.get(0).execute();
    }

    assertEquals(expected, document.asText());
  }
}
