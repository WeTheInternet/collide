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

package com.google.collide.client.code.autocomplete.codemirror;

import static com.google.collide.client.code.autocomplete.TestUtils.CTRL_SPACE;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_BACKSPACE;
import static org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType.DELETE;

import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitAction;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitActionType;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment.MockAutocompleter;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.input.TestSignalEvent;
import com.google.collide.client.testutil.CodeMirrorTestCase;
import com.google.collide.client.testutil.TestSchedulerImpl;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;

import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;

import javax.annotation.Nullable;

/**
 * Test for JS autocompletion cases, when CodeMirror parser is used.
 *
 */
public class JsCodemirrorTest extends CodeMirrorTestCase {

  static final SignalEventEssence DELETE_KEY = new SignalEventEssence(
      KEY_BACKSPACE, false, false, false, false, DELETE);
  private static final SignalEventEssence ENTER = new SignalEventEssence(
      KeyCodes.KEY_ENTER, false, false, false, false, KeySignalType.INPUT);

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testNoProposalsInCommentsAndStrings() {
                 // 0         1         2
                 // 012345678901234567890123456789
    String text1 = "var a = 'Hello Kitty'; // Funny?";
    String text2 = "var b = /* Aha =) */ 0;";

    checkHasProposals(text1, 0, true, "global");
    checkHasProposals(text1, 8, true, "before string");
    checkHasProposals(text1, 9, false, "string began");
    checkHasProposals(text1, 10, false, "in string");
    checkHasProposals(text1, 15, false, "in string after space");
    checkHasProposals(text1, 21, true, "after string");
    checkHasProposals(text1, 22, true, "after string");
    checkHasProposals(text1, 23, true, "before comment");
    checkHasProposals(text1, 26, false, "in comment after space");
    checkHasProposals(text1, 30, false, "in comment");

    checkHasProposals(text2, 0, true, "after comment");
    checkHasProposals(text2, 8, true, "before multiline");
    checkHasProposals(text2, 13, false, "in multiline");
    checkHasProposals(text2, 15, false, "in multiline after space");
    checkHasProposals(text2, 21, true, "after multiline");
  }

  private void checkHasProposals(String text, int column,
      boolean expectHasProposals, String message) {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.js"), text, 0, column, true);
    AutocompleteProposals proposals = helper.autocompleter.jsAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    assertEquals(message, expectHasProposals, !proposals.isEmpty());
  }

  public void testNoTemplateProposalsAfterThis() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.js"), "this.", 0, 5, true);
    AutocompleteProposals proposals = helper.autocompleter.jsAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    assertTrue("has no proposals", proposals.isEmpty());
  }

  public void testTemplateProposalsInGlobal() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.js"), "", 0, 0, true);
    AutocompleteProposals autocompletions =
        helper.autocompleter.jsAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SPACE);
    assertFalse("has proposals", autocompletions.isEmpty());
    ProposalWithContext whileProposal = TestUtils.selectProposalByName(autocompletions, "while");
    assertNotNull("has 'while'", whileProposal);

    helper.autocompleter.reallyFinishAutocompletion(whileProposal);
    assertEquals("while () {\n  \n}", helper.editor.getDocument().asText());
  }

  public void testAutocompletionAfterKeyword() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.js"), "for", 0, 3, true);
    AutocompleteProposals autocompletions =
        helper.autocompleter.jsAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SPACE);
    ProposalWithContext proposal = autocompletions.select(0);
    assertEquals("proposal name", "for", proposal.getItem().getName());
    helper.autocompleter.reallyFinishAutocompletion(proposal);

    String text = helper.editor.getDocument().getFirstLine().getText();
    assertEquals("resulting text", "for (;;) {\n", text);
  }

  public void testDoNotDieOnLongLines() {
    String longLine = " ";
    for (int i = 0; i < 12; i++) {
      longLine = longLine + longLine;
    }
    // longLine length is 4096

    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    String text = "function foo() {\n"
        + "  var bar1;\n"
        + "  var bar2 ='" + longLine + "';\n"
        + "  var bar3;\n"
        + "  " // Cursor here.
        + "}";
    helper.setup(new PathUtil("foo.js"), text, 4, 2, true);
    helper.parser.begin();
    helper.parseScheduler.requests.get(0).run(10);
    AutocompleteProposals autocompletions =
        helper.autocompleter.jsAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SPACE);
    JsonStringSet proposals = JsonCollections.createStringSet();
    for (int i = 0, l = autocompletions.size(); i < l; i++) {
      proposals.add(autocompletions.get(i).getName());
    }
    assertTrue("contains var defined before long line", proposals.contains("bar1"));
    assertTrue("contains var defined after long line", proposals.contains("bar3"));
  }

  public void testDoNotDieOnRegExp() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    String text = "function foo() {\n"
        + "  var bar1 = /regexp/;\n"
        + "  var bar2;\n"
        + "  " // Cursor here.
        + "}";
    helper.setup(new PathUtil("foo.js"), text, 3, 2, true);
    helper.parser.begin();
    helper.parseScheduler.requests.get(0).run(10);
    AutocompleteProposals autocompletions =
        helper.autocompleter.jsAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SPACE);
    JsonStringSet proposals = JsonCollections.createStringSet();
    for (int i = 0, l = autocompletions.size(); i < l; i++) {
      proposals.add(autocompletions.get(i).getName());
    }
    assertTrue("contains var defined in line with regexp", proposals.contains("bar1"));
    assertTrue("contains var defined after line with regexp", proposals.contains("bar2"));
  }

  public void testDeleteAtBeginningOfDocument() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String text = "<cursorAtTheBeginingOfFirstLine>";
    helper.setup(new PathUtil("foo.js"), text, 0, 0, true);
    ExplicitAction action = helper.autocompleter.jsAutocompleter.getExplicitAction(
        helper.editor.getSelection(), DELETE_KEY, false);
    assertEquals(LanguageSpecificAutocompleter.ExplicitActionType.DEFAULT, action.getType());
  }

  public void testClosePopupOnSpace() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    // TODO: vars in the global scope are not registered by CM.
    String text = "function a() { var abba, apple, arrow; a";
    helper.setup(new PathUtil("foo.js"), text, 0, text.length(), true);
    final MockAutocompleter autocompleter = helper.autocompleter;

    assertFalse("initially popup is not shown", helper.popup.isShowing());

    final JsonArray<Scheduler.ScheduledCommand> scheduled = JsonCollections.createArray();

    // We want to click ctrl-space.
    Runnable ctrlSpaceClicker = new Runnable() {
      @Override
      public void run() {
        autocompleter.pressKey(CTRL_SPACE);
      }
    };

    // Collect deferred tasks in array.
    TestSchedulerImpl.AngryScheduler scheduler = new TestSchedulerImpl.AngryScheduler() {
      @Override
      public void scheduleDeferred(ScheduledCommand scheduledCommand) {
        scheduled.add(scheduledCommand);
      }
    };

    // Now, if we hit ctrl-space - popup will appear with 3 variables.
    TestSchedulerImpl.runWithSpecificScheduler(ctrlSpaceClicker, scheduler);

    assertEquals("actual autocompletion is deferred", 1, scheduled.size());

    // Now autocompletion acts.
    scheduled.get(0).execute();

    assertTrue("popup appeared", helper.popup.isShowing());
    assertEquals("variables are proposed", 4, helper.popup.proposals.size());

    // Now, if we type " " autocompletion popup should disappear.
    autocompleter.pressKey(new SignalEventEssence(' '));
    assertFalse("popup disappeared", helper.popup.isShowing());
  }

  public void testRawExplicitInContext() {
    SignalEventEssence quoteKey = new SignalEventEssence('"');

    checkExplicit("line-comment", "// var a =", 0, quoteKey, null);
    checkExplicit("in-block-comment", "/* var a = */", 3, quoteKey, null);
    checkExplicit("block-comment-eol", "/* var a =\nsecond line*/", 0, quoteKey, null);
    checkExplicit("in-string", "var a =''", 1, quoteKey, null);

    checkExplicit("bs-in-string", "var a ='\"\"'", 2, DELETE_KEY, null);
    checkExplicit("bs-in-comment", "// var a =''", 1, DELETE_KEY, null);
    checkExplicit("bs-between-string", "var a ='''", 1, DELETE_KEY, null);
  }

  public void testBracesPairing() {
    checkExplicit("braces-pairing", "foo", 0, new SignalEventEssence('['),
        new DefaultAutocompleteResult("[]", "", 1));
  }

  public void testBracesNotPairing() {
    checkExplicit("braces-not-pairing", "foo", 3, new SignalEventEssence('['), null);
  }

  public void testQuotesPairing() {
    checkExplicit("quotes-pairing", "", 0, new SignalEventEssence('"'),
        new DefaultAutocompleteResult("\"\"", "", 1));
  }

  public void testQuotesNotPairing() {
    checkExplicit("quotes-not-pairing", "\"\"", 0, new SignalEventEssence('"'), null);
  }

  public void testBracesPassing() {
    checkExplicit(
        "braces-passing", "foo[]", 1, new SignalEventEssence(']'),
        DefaultAutocompleteResult.PASS_CHAR);
  }

  public void testBracesNotPassing() {
    checkExplicit("braces-not-passing", "[(()]", 2, new SignalEventEssence(')'), null);
  }

  public void testSymmetricDeletion() {
    DefaultAutocompleteResult bsDelete = new DefaultAutocompleteResult(
        "", 0, 1, 0, 1, null, "");
    checkExplicit("bs-after-comment", "/* Q */ var a =''", 1, DELETE_KEY, bsDelete);
    checkExplicit("bs-braces", "foo[]", 1, DELETE_KEY, bsDelete);
  }

  public void testEnterBetweenCurlyBraces() {
    checkExplicit("enter-between-curly-braces", "  {}", 1, ENTER,
        new DefaultAutocompleteResult("\n    \n  ", "", 5));
  }

  private void checkExplicit(String message, String text, int tailOffset,
      SignalEventEssence trigger, @Nullable DefaultAutocompleteResult expected) {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    Document document = Document.createFromString(text);
    int column = LineUtils.getLastCursorColumn(document.getFirstLine()) - tailOffset;
    helper.setup(new PathUtil("foo.js"), document, 0, column, true);

    ExplicitAction action = helper.autocompleter.jsAutocompleter.getExplicitAction(
        helper.editor.getSelection(), trigger, false);
    AutocompleteResult commonResult = action.getExplicitAutocompletion();
    if (expected == null) {
      assertNull("result", commonResult);
      assertFalse("action", ExplicitActionType.EXPLICIT_COMPLETE == action.getType());
      return;
    } else {
      assertTrue("action", ExplicitActionType.EXPLICIT_COMPLETE == action.getType());
    }
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);

    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertNotNull(message + ":result", result);
    assertEquals(message + ":text",
        expected.getAutocompletionText(), result.getAutocompletionText());
    assertEquals(message + ":delete", expected.getDeleteCount(), result.getDeleteCount());
    assertEquals(message + ":bspace", expected.getBackspaceCount(), result.getBackspaceCount());
    assertEquals(message + ":jump", expected.getJumpLength(), result.getJumpLength());
  }

  /**
   * Integration test: check that
   * {@link com.google.collide.client.code.autocomplete.codegraph.CodeGraphAutocompleter}
   * allows
   * {@link com.google.collide.client.editor.input.DefaultScheme} to
   * perform specific textual changes.
   */
  public void testDoNotPreventCtrlBs() {
    String text = "#!@abc   ";
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    helper.setup(new PathUtil("test.js"), text, 0, text.length(), true);

    final Editor editor = helper.editor;

    final JsonArray<Scheduler.ScheduledCommand> scheduled = JsonCollections.createArray();

    TestSchedulerImpl.AngryScheduler scheduler = new TestSchedulerImpl.AngryScheduler() {
      @Override
      public void scheduleDeferred(ScheduledCommand scheduledCommand) {
        scheduled.add(scheduledCommand);
      }
    };

    Runnable ctrlBsClicker = new Runnable() {
      @Override
      public void run() {
        TestSignalEvent bsTrigger = new TestSignalEvent(
            KeyCodes.KEY_BACKSPACE, KeySignalType.DELETE, ModifierKeys.ACTION);
        editor.getInput().processSignalEvent(bsTrigger);
      }
    };

    TestSchedulerImpl.runWithSpecificScheduler(ctrlBsClicker, scheduler);

    while (!scheduled.isEmpty()) {
      Scheduler.ScheduledCommand command = scheduled.remove(0);
      command.execute();
    }

    String result2 = editor.getDocument().getFirstLine().getText();
    assertEquals("after ctrl-BS", "#!@", result2);
  }
}
