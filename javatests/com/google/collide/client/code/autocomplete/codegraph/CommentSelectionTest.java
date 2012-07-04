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

package com.google.collide.client.code.autocomplete.codegraph;

import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.lang.LanguageHelperResolver;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.input.TestSignalEvent;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.testutil.TestSchedulerImpl;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.core.client.Scheduler;

import elemental.events.KeyboardEvent;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * Test cases for comment/uncomment selection feature.
 *
 */
public class CommentSelectionTest extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return "com.google.cofllide.client.TestCode";
  }

  public void testSingleLastLineComment() {
    String text = "com<cursor>ment me";
    String expected = "//com<cursor>ment me";
    checkCommentSelection(text, expected, 0, 3, 0, 3, 0, 5, 0, 5);
  }

  public void testSingleLastLineUnComment() {
    String text = "//com<cursor>ment me";
    String expected = "com<cursor>ment me";
    checkCommentSelection(text, expected, 0, 5, 0, 5, 0, 3, 0, 3);
  }

  public void testSingleCommentNextLineIsLong() {
    String text = "com<old cursor>ment me\ncom<new cursor>ment me";
    String expected = "//com<old cursor>ment me\ncom<new cursor>ment me";
    checkCommentSelection(text, expected, 0, 3, 0, 3, 1, 3, 1, 3);
  }

  public void testSingleCommentNextLineIsShort() {
    String text = "blah-blah com*ment me\n..*";
    String expected = "//blah-blah com*ment me\n..*";
    checkCommentSelection(text, expected, 0, 13, 0, 13, 1, 3, 1, 3);
  }

  public void testSingleCommentAtLineStart() {
    String text = "comment me\nblah-blah";
    String expected = "//comment me\nblah-blah";
    checkCommentSelection(text, expected, 0, 0, 0, 0, 1, 0, 1, 0);
  }

  public void testCommentMultiFromMidLineToDocEnd() {
    String text = "first\nsecond";
    String expected = "//first\n//second";
    checkCommentSelection(text, expected, 0, 3, 1, 6, 0, 5, 1, 8);
  }

  public void testCommentMultiFromStartLineToStartLine() {
    String text = "first\nsecond";
    String expected = "//first\nsecond";
    checkCommentSelection(text, expected, 0, 0, 1, 0, 0, 0, 1, 0);
  }

  public void testUnCommentMultiFromMidLineToMidLine() {
    String text = "//first\n//second";
    String expected = "first\nsecond";
    checkCommentSelection(text, expected, 0, 5, 1, 4, 0, 3, 1, 2);
  }

  public void testCommentMultiMixed() {
    String text = "//first\nsecond\n//third\n";
    String expected = "////first\n//second\n////third\n";
    checkCommentSelection(text, expected, 0, 0, 3, 0, 0, 0, 3, 0);
  }

  private void checkCommentSelection(
      String text, String expected, int line1, int column1, int line2, int column2,
      int expectedLine1, int expectedColumn1, int expectedLine2, int expectedColumn2) {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    helper.setup(new PathUtil("test.js"), text, line1, column1, false);
    final Editor editor = helper.editor;
    editor.getInput().getActionExecutor().addDelegate(
        LanguageHelperResolver.getHelper(SyntaxType.JS).getActionExecutor());
    LineFinder lineFinder = editor.getDocument().getLineFinder();
    editor.getSelection().setSelection(
        lineFinder.findLine(line1), column1, lineFinder.findLine(line2), column2);

    final JsonArray<Scheduler.ScheduledCommand> scheduled = JsonCollections.createArray();

    TestSchedulerImpl.AngryScheduler scheduler = new TestSchedulerImpl.AngryScheduler() {
      @Override
      public void scheduleDeferred(ScheduledCommand scheduledCommand) {
        scheduled.add(scheduledCommand);
      }
    };

    final TestSignalEvent ctrlSlashTriger = new TestSignalEvent(
        KeyboardEvent.KeyCode.SLASH, SignalEvent.KeySignalType.INPUT,
        ModifierKeys.ACTION);

    Runnable ctrlShiftSlashClicker = new Runnable() {
      @Override
      public void run() {
        editor.getInput().processSignalEvent(ctrlSlashTriger);
      }
    };

    TestSchedulerImpl.runWithSpecificScheduler(ctrlShiftSlashClicker, scheduler);

    while (!scheduled.isEmpty()) {
      Scheduler.ScheduledCommand command = scheduled.remove(0);
      command.execute();
    }

    String result = editor.getDocument().asText();
    assertEquals("textual result", expected, result);
    Position[] selectionRange = editor.getSelection().getSelectionRange(false);
    assertEquals("selection start line", expectedLine1, selectionRange[0].getLineNumber());
    assertEquals("selection start column", expectedColumn1, selectionRange[0].getColumn());
    assertEquals("selection end line", expectedLine2, selectionRange[1].getLineNumber());
    assertEquals("selection end column", expectedColumn2, selectionRange[1].getColumn());
  }
}
