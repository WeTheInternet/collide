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

import static com.google.collide.client.code.autocomplete.TestUtils.CTRL_SHIFT_SPACE;
import static com.google.collide.client.code.autocomplete.TestUtils.CTRL_SPACE;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createCodeGraph;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createFreshness;
import static org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;

import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitAction;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitActionType;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.code.autocomplete.codegraph.ParsingTask;
import com.google.collide.client.code.autocomplete.integration.DocumentParserListenerAdapter;
import com.google.collide.client.code.autocomplete.integration.TaggableLineUtil;
import com.google.collide.client.testutil.CodeMirrorTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.Token;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.client.DtoClientImpls;
import com.google.collide.dto.client.DtoClientImpls.CodeBlockImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphResponseImpl;
import com.google.collide.dto.client.DtoClientImpls.MockCodeBlockImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Line;

/**
 * Test for various auto-completion cases, when CodeMirror parser is used.
 */
public class HtmlCodemirrorTest extends CodeMirrorTestCase {

  private static void setupHelper(MockAutocompleterEnvironment helper, String text) {
    helper.setup(new PathUtil("foo.html"), text, 0, text.length(), true);
    helper.parser.begin();
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testExplicit() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    setupHelper(helper, "<html><body><");
    AutocompleteResult commonResult = helper.autocompleter.htmlAutocompleter
        .getExplicitAction(helper.editor.getSelection(), new SignalEventEssence('/'), false)
        .getExplicitAutocompletion();
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals("/body>", result.getAutocompletionText());
  }

  public void testCssFindAutocompletions() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    setupHelper(helper, "<html><head><style>p {color:bl");
    Line line = helper.editor.getDocument().getLastLine();
    JsonArray<Token> tokens = helper.parser.parseLineSync(line);
    assertEquals("html", tokens.get(0).getMode());
    assertEquals("css", tokens.get(tokens.size() - 1).getMode());

    AutocompleteProposals proposals =
        helper.autocompleter.htmlAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SPACE);

    assertEquals(2, proposals.size());
    assertEquals("blue", proposals.get(1).getName());
  }

  public void testCssFindAutocompletionsAfterEmptyLine() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String line0 = "<html><head><style>p {color:";
    String line1 = "";
    String line2 = "bl";
    String text = line0 + "\n" + line1 + "\n" + line2;
    helper.setup(new PathUtil("foo.html"), text, 2, line2.length(), true);
    helper.parser.begin();
    Line line = helper.editor.getDocument().getFirstLine();
    JsonArray<Token> tokens = helper.parser.parseLineSync(line);
    assertEquals("html", tokens.get(0).getMode());
    assertEquals("css", tokens.get(tokens.size() - 2).getMode());
    assertEquals("", tokens.get(tokens.size() - 1).getMode());
    line = helper.editor.getDocument().getLineFinder().findLine(1).line();
    tokens = helper.parser.parseLineSync(line);
    assertEquals(1, tokens.size());
    assertEquals("", tokens.get(0).getMode());
    line = helper.editor.getDocument().getLastLine();
    tokens = helper.parser.parseLineSync(line);
    assertEquals(1, tokens.size());
    assertEquals("css", tokens.get(0).getMode());

    AutocompleteProposals proposals =
        helper.autocompleter.htmlAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SPACE);

    assertEquals(2, proposals.size());
    assertEquals("blue", proposals.get(1).getName());
  }

  public void testCssGetExplicitAutocompletion() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    setupHelper(helper, "<html><head><style>p ");
    Line line = helper.editor.getDocument().getLastLine();
    JsonArray<Token> tokens = helper.parser.parseLineSync(line);
    assertEquals("html", tokens.get(0).getMode());
    assertEquals("css", tokens.get(tokens.size() - 1).getMode());
    helper.autocompleter.htmlAutocompleter.updateModeAnchors(line, tokens);

    SignalEventEssence trigger = new SignalEventEssence('{');
    AutocompleteResult result = helper.autocompleter.htmlAutocompleter
        .getExplicitAction(helper.editor.getSelection(), trigger, false)
        .getExplicitAutocompletion();
    assertTrue("result type", result instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult defaultResult = (DefaultAutocompleteResult) result;
    assertEquals("{\n  \n}", defaultResult.getAutocompletionText());
  }

  public void testAfterCssMultiplexing() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    setupHelper(helper, "<html><head><style>p {color:blue;}</style><a");

    AutocompleteProposals proposals =
        helper.autocompleter.htmlAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SPACE);

    assertEquals(7, proposals.size());
    assertEquals("abbr", proposals.get(1).getName());
  }

  public void testJavascriptFindAutocompletions() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    setupHelper(helper,
        "<html><body><script type=\"text/javascript\">function a() { var abba, apple, arrow; a");
    Line line = helper.editor.getDocument().getLastLine();
    JsonArray<Token> tokens = helper.parser.parseLineSync(line);
    assertEquals("html", tokens.get(0).getMode());
    assertEquals("javascript", tokens.get(tokens.size() - 1).getMode());
    TaggableLine previousLine = TaggableLineUtil.getPreviousLine(line);
    helper.autocompleter.htmlAutocompleter.updateModeAnchors(line, tokens);
    new ParsingTask(helper.autocompleter.localPrefixIndexStorage).onParseLine(
        previousLine, line, tokens);

    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SHIFT_SPACE);

    assertEquals(3, proposals.size());
    assertEquals("apple", proposals.get(1).getName());

    proposals = helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);

    assertEquals(4, proposals.size());
    assertEquals("arguments", proposals.get(2).getName());
  }

  public void testJavascriptFindAutocompletionsOnEmptyLine() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.html"),
        "<html><head><script>\n\n</script></head></html>", 1, 0, true);

    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();
    helper.parseScheduler.requests.pop().run(10);

    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter
        .findAutocompletions(helper.editor.getSelection(), CTRL_SPACE);
    assertTrue(TestUtils.findProposalByName(proposals.getItems(), "break") != null);
  }

  public void testJavascriptApplyAutocompletions() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String prologue = "<html>\n<script type=\"text/javascript\">\n";
    String epilogue = "</script>\n</html>\n";
    String text = prologue + "d\n" + epilogue;
    helper.setup(new PathUtil("foo.html"), text, 2, 1, true);
    helper.parser.begin();
    helper.parseScheduler.requests.get(0).run(10);

    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);

    assertEquals(3, proposals.size());
    assertEquals("delete", proposals.get(1).getName());
    helper.autocompleter.reallyFinishAutocompletion(proposals.select(1));
    assertEquals(prologue + "delete \n" + epilogue, helper.editor.getDocument().asText());
  }

  public void testJavascriptFindAutocompletionsSeesFunctionInCodegraph() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    // Something like function aFoo() {}
    CodeBlockImpl aFoo = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("aFoo")
        .setChildren(JsoArray.<CodeBlock>create())
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(0)
        .setEndColumn(19);
    CodeBlockImpl fileCodeBlock = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FILE)
        .setName("/foo.js")
        .setChildren(JsoArray.<CodeBlock>from(aFoo))
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(0)
        .setEndColumn(19);
    CodeGraphImpl codeGraph = createCodeGraph(fileCodeBlock);
    CodeGraphResponseImpl response = DtoClientImpls.MockCodeGraphResponseImpl.make();
    response.setFreshness(createFreshness("0", "1", "0"));
    response.setFullGraphJson(codeGraph.serialize());

    // This will immediately fire api call
    helper.cubeClient.setPath("/foo.js");
    assertEquals("one api call after setDocument", 1,
        helper.cubeClient.api.collectedCallbacks.size());
    helper.cubeClient.api.collectedCallbacks.get(0).onMessageReceived(response);

    setupHelper(helper,
        "<html><body><script type=\"text/javascript\">function a() { var abba, apple, arrow; a");

    Line line = helper.editor.getDocument().getLastLine();
    JsonArray<Token> tokens = helper.parser.parseLineSync(line);
    assertEquals("html", tokens.get(0).getMode());
    assertEquals("javascript", tokens.get(tokens.size() - 1).getMode());
    TaggableLine previousLine = TaggableLineUtil.getPreviousLine(line);
    helper.autocompleter.htmlAutocompleter.updateModeAnchors(line, tokens);
    new ParsingTask(helper.autocompleter.localPrefixIndexStorage).onParseLine(
        previousLine, line, tokens);

    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SHIFT_SPACE);

    assertEquals(3, proposals.size());
    assertEquals("apple", proposals.get(1).getName());

    proposals = helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);

    assertEquals(5, proposals.size());
    assertEquals("aFoo", proposals.get(1).getName());
  }

  public void testJavascriptGetExplicitAutocompletion() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    setupHelper(helper, "<html><body><script type=\"text/javascript\">foo");
    Line line = helper.editor.getDocument().getLastLine();
    JsonArray<Token> tokens = helper.parser.parseLineSync(line);
    assertEquals("html", tokens.get(0).getMode());
    assertEquals("javascript", tokens.get(tokens.size() - 1).getMode());
    TaggableLine previousLine = TaggableLineUtil.getPreviousLine(line);
    helper.autocompleter.htmlAutocompleter.updateModeAnchors(line, tokens);
    new ParsingTask(helper.autocompleter.localPrefixIndexStorage).onParseLine(
        previousLine, line, tokens);

    SignalEventEssence trigger = new SignalEventEssence('[');
    AutocompleteResult result = helper.autocompleter.htmlAutocompleter
        .getExplicitAction(helper.editor.getSelection(), trigger, false)
        .getExplicitAutocompletion();
    assertTrue("result type", result instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult defaultResult = (DefaultAutocompleteResult) result;
    assertEquals("[]", defaultResult.getAutocompletionText());

    trigger = new SignalEventEssence('(');
    result = helper.autocompleter.htmlAutocompleter
        .getExplicitAction(helper.editor.getSelection(), trigger, false)
        .getExplicitAutocompletion();
    assertTrue("result type", result instanceof DefaultAutocompleteResult);
    defaultResult = (DefaultAutocompleteResult) result;
    assertEquals("()", defaultResult.getAutocompletionText());
  }

  public void testPopupDoNotAnnoyUsers() {
    final MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.html"),
        "<html>\n <body>\n  <script>\n\n  </script>\n </body>\n</html>", 3, 0, true);
    ExplicitActionType action = helper.autocompleter.htmlAutocompleter.getExplicitAction(
        helper.editor.getSelection(), new SignalEventEssence(' '), false).getType();
    assertTrue("no popup before mode is determined", action == ExplicitActionType.DEFAULT);

    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();
    helper.parseScheduler.requests.pop().run(10);

    action = helper.autocompleter.htmlAutocompleter.getExplicitAction(
        helper.editor.getSelection(), new SignalEventEssence(' '), false).getType();
    assertTrue("no popup in JS mode", action == ExplicitActionType.DEFAULT);
  }

  public void testNoPopupAfterClosingTag() {
    final MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.html"), "<html>\n <script\n<body style=''>\n", 2, 8, true);

    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();
    helper.parseScheduler.requests.pop().run(10);

    SignalEventEssence signalGt = new SignalEventEssence(
        '>', false, false, true, false, KeySignalType.INPUT);
    ExplicitActionType action = helper.autocompleter.htmlAutocompleter.getExplicitAction(
        helper.editor.getSelection(), signalGt, false).getType();
    assertTrue("no popup after closing tag", action == ExplicitActionType.DEFAULT);
  }

  /**
   * Tests a sharp edge: when html parser changes it's mode.
   *
   * <p>Parser changes it's mode when it meets "&gt;" that finishes opening
   * "script" tag.
   *
   * <p>As a consequence, xml-analyzer may interpret the following
   * content "inside-out".
   */
  public void testScriptTagDoNotConfuseXmlProcessing() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.html"),
        "<html>\n <script></script>\n <body>\n", 2, 7, true);

    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();
    helper.parseScheduler.requests.pop().run(10);

    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter
        .findAutocompletions(helper.editor.getSelection(), CTRL_SPACE);
    assertEquals(0, proposals.size());
  }

  /**
   * Tests a sharp edge: when html parser changes it's mode.
   *
   * <p>Parser changes it's mode when it meets "&gt;" that finishes opening
   * "script" tag.
   *
   * <p>This situation could confuse autocompleter and cause unwanted popup
   * when user press space just after "&gt;".
   */
  public void testNoJsPopupOnAfterSpace() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.html"),
        "<html>\n <script>\n", 1, 9, true);

    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();
    helper.parseScheduler.requests.pop().run(10);

    ExplicitAction action = helper.autocompleter.htmlAutocompleter
        .getExplicitAction(helper.editor.getSelection(), new SignalEventEssence(' '), false);
    assertFalse(action.getType() == ExplicitActionType.DEFERRED_COMPLETE);
  }

  /**
   * Tests a sharp edge: when html parser is asked for proposals on line that
   * is not parsed yet.
   *
   * <p>Previously we got NPE in this case.
   */
  public void testNoProposalsOnGrayLine() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.html"),
        "<html>\n <body>\n  <di", 2, 5, true);

    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();
    helper.parseScheduler.requests.pop().run(1);

    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter
        .findAutocompletions(helper.editor.getSelection(), CTRL_SPACE);
    assertEquals(0, proposals.size());
  }
}
