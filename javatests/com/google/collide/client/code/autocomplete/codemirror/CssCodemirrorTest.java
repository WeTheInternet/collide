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

import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitAction;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitActionType;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.testutil.TestSchedulerImpl;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.outline.CssOutlineParser;
import com.google.collide.client.workspace.outline.OutlineConsumer;
import com.google.collide.client.workspace.outline.OutlineNode;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerManager;
import com.google.gwt.core.client.Scheduler;

import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;

import javax.annotation.Nullable;

/**
 * Test for explicit autocompletion cases, when codemirror parser is used.
 *
 */
public class CssCodemirrorTest extends
    com.google.collide.client.testutil.CodeMirrorTestCase {

  private static final String EXPLICIT_BRACES = "{\n  \n}";

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  private void checkExplicit(@Nullable String expected, String prefix) {
    SignalEventEssence trigger = new SignalEventEssence('{');

    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    helper.setup(new PathUtil("foo.css"), prefix, 0, prefix.length(), true);
    ExplicitAction action = helper.autocompleter.cssAutocompleter.getExplicitAction(
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
    assertEquals(expected, result.getAutocompletionText());
  }

  public void testExplicit() {
    checkExplicit(EXPLICIT_BRACES, "");
    checkExplicit(EXPLICIT_BRACES, "div.root");
    checkExplicit(EXPLICIT_BRACES, "div.root ");
    checkExplicit(EXPLICIT_BRACES, "div.root /*foo*/");
    checkExplicit(EXPLICIT_BRACES, "div.root /*foo*/ ");
    checkExplicit(EXPLICIT_BRACES, ".root ");
    checkExplicit(EXPLICIT_BRACES, "@media all");
    checkExplicit(EXPLICIT_BRACES, "@media all ");
    checkExplicit(EXPLICIT_BRACES, "@media all { td");
    checkExplicit(EXPLICIT_BRACES, "@media all /*foo*/");
    checkExplicit(EXPLICIT_BRACES, "@media all /*foo*/ ");

    checkExplicit(null, "{");
    checkExplicit(null, "{ ");
    checkExplicit(null, "{ {");
    checkExplicit(null, "{ /*");
    checkExplicit(null, "{ /* ");
    checkExplicit(null, "{ /*foo*/");
    checkExplicit(null, "{ /*foo*/ ");
    checkExplicit(null, "/*");
    checkExplicit(null, "/* ");
    checkExplicit(null, ".root /*");
    checkExplicit(null, ".root {/*foo*/");
    checkExplicit(null, "@media all { td {");
  }

  public void testOutlineWithMedia() {
    JsonStringSet tags = JsonCollections.createStringSet("td");
    checkOutlineParser("@media screen { td{}}", tags);
    checkOutlineParser("@media screen {td {}}", tags);
  }

  public void testOutline() {
    String text = ""
        + " td,\tp.dark-green,  a:hover, [title~=hello]"
        //  ^^   ^^^^^^^^^^^^   ^^^^^^^  ^^^^^^^^^^^^^^
        + " {\tborder: 3px solid #55AAEE;   font: message-box; background-color: #EEEEEE;}"
        //
        + " a.bad:visited {background-image:url('gradient2.png');}"
        //  ^^^^^^^^^^^^^
        + "b/*comment*/.sparse, #para, div#main { -custom:'img {';}"
        // ^-----------^^^^^^^  ^^^^^  ^^^^^^^^
        + " .marked p, p  >  i:first-child {}"
        //  ^^^^^^^^^  ^^-^-^^^^^^^^^^^^^^
        + " q:lang(no) {quotes: \"~\" \"~\";}\n"
        //  ^^^^^^^^^^
        ;

    JsonStringSet tags = JsonCollections.createStringSet("td", "p.dark-green", "a:hover",
        "[title~=hello]", "a.bad:visited", "b.sparse", "#para", "div#main", ".marked p",
        "p > i:first-child", "q:lang(no)");

    checkOutlineParser(text, tags);
  }

  private void checkOutlineParser(String text, JsonStringSet expectedNodes) {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    helper.setup(new PathUtil("foo.css"), text, 0, 0, true);

    ListenerManager<DocumentParser.Listener> registrar = ListenerManager.create();

    final JsonArray<OutlineNode> output = JsonCollections.createArray();

    OutlineConsumer consumer = new OutlineConsumer() {
      @Override
      public void onOutlineParsed(JsonArray<OutlineNode> nodes) {
        output.clear();
        output.addAll(nodes);
      }
    };

    Line line = helper.editor.getDocument().getFirstLine();
    JsonArray<Token> tokens = helper.parser
        .parseLineSync(line);

    CssOutlineParser cssOutlineParser = new CssOutlineParser(registrar, consumer);

    cssOutlineParser.onIterationStart(0);
    cssOutlineParser.onDocumentLineParsed(line, 0, tokens);
    cssOutlineParser.onIterationFinish();

    final int outputSize = output.size();
    assertEquals("number of nodes", expectedNodes.getKeys().size(), outputSize);
    for (int i = 0; i < outputSize; i++) {
      OutlineNode node = output.get(i);
      String nodeName = node.getName();
      assertTrue("unexpected item: [" + nodeName + "]", expectedNodes.contains(nodeName));
    }

    cssOutlineParser.cleanup();
  }

  public void testWorkflow() {
    final MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    // TODO: vars in the global scope are not registered by CM.
    String text = "td { cur";
    helper.setup(new PathUtil("foo.css"), text, 0, text.length(), true);
    final MockAutocompleterEnvironment.MockAutocompleter autocompleter = helper.autocompleter;

    final JsonArray<Scheduler.ScheduledCommand> scheduled = JsonCollections.createArray();

    Runnable ctrlSpaceClicker = new Runnable() {
      @Override
      public void run() {
        autocompleter.pressKey(
            new SignalEventEssence(' ', true, false, false, false, KeySignalType.INPUT));
      }
    };

    TestSchedulerImpl.AngryScheduler scheduler = new TestSchedulerImpl.AngryScheduler() {
      @Override
      public void scheduleDeferred(ScheduledCommand scheduledCommand) {
        scheduled.add(scheduledCommand);
      }
    };

    TestSchedulerImpl.runWithSpecificScheduler(ctrlSpaceClicker, scheduler);
    assertEquals("actual autocompletion is deferred", 1, scheduled.size());
    scheduled.get(0).execute();
    assertFalse("expect nonempty popup", helper.popup.proposals.isEmpty());

    scheduled.clear();

    Runnable enterClicker = new Runnable() {
      @Override
      public void run() {
        helper.popup.delegate.onSelect(helper.popup.proposals.select(0));
      }
    };

    TestSchedulerImpl.runWithSpecificScheduler(enterClicker, scheduler);
    assertEquals("apply proposal is deferred", 1, scheduled.size());

    // We expect not to explode at this moment.
    scheduled.get(0).execute();
  }
}
