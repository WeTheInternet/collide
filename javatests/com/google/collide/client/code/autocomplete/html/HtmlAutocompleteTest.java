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

package com.google.collide.client.code.autocomplete.html;

import static com.google.collide.client.code.autocomplete.TestUtils.CTRL_SPACE;

import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.code.autocomplete.integration.DocumentParserListenerAdapter;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.testutil.TestSchedulerImpl;
import com.google.collide.client.util.IncrementalScheduler;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.collections.SimpleStringBag;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.Stream;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.Pair;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Tests for html autocompletion.
 */
public class HtmlAutocompleteTest extends SynchronousTestCase {

  private MockAutocompleterEnvironment helper;
  private PathUtil path;
  private JsonStringMap<JsonArray<Token>> parsedLines;
  private JsonArray<Token> lineTokens;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    helper = new MockAutocompleterEnvironment();
    path = new PathUtil("/test.html");
    lineTokens = JsonCollections.createArray();
    parsedLines = JsonCollections.createMap();

    helper.specificParser = new TestUtils.MockParser(SyntaxType.HTML) {
      @Override
      public void parseNext(Stream stream, State parserState, JsonArray<Token> tokens) {
        Preconditions.checkState(stream instanceof TestUtils.MockStream);
        JsonArray<Token> lineTokens = parsedLines.get(((TestUtils.MockStream) stream).getText());
        if (lineTokens != null) {
          tokens.addAll(lineTokens);
        }
      }
    };
  }

  private AutocompleteProposals findAutocompletions() {
    prepareAutocompleter();
    return helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
  }

  private void parseOneLine() {
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        JsonArray<IncrementalScheduler.Task> requests = helper.parseScheduler.requests;
        if (!requests.peek().run(1)) {
          requests.pop();
        }
      }
    };
    TestSchedulerImpl.AngryScheduler scheduler = new TestSchedulerImpl.AngryScheduler() {
      @Override
      public void scheduleDeferred(ScheduledCommand scheduledCommand) {
        scheduledCommand.execute();
      }
    };
    TestSchedulerImpl.runWithSpecificScheduler(runnable, scheduler);
  }

  private void prepareAutocompleter() {
    String text = tokensToText(lineTokens);
    parsedLines.put(text, lineTokens);
    helper.setup(path, text, 0, text.length(), false);
    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();
    parseOneLine();
  }

  private String tokensToText(JsonArray<Token> tokens) {
    StringBuilder builder = new StringBuilder();
    for (Token token : tokens.asIterable()) {
      builder.append(token.getValue());
    }
    return builder.toString();
  }

  /**
   * Tests attributes proposals.
   */
  public void testHtmlAttributes() {
    HtmlTagsAndAttributes htmlAttributes = HtmlTagsAndAttributes.getInstance();

    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));
    AutocompleteProposals proposals = findAutocompletions();

    assertEquals(htmlAttributes.searchAttributes("html", new SimpleStringBag(), "").size(),
        proposals.size());
    assertEquals("accesskey", proposals.get(0).getName());
  }

  /**
   * Tests that proposal list is updated when parsing of tag is finished.
   */
  public void testUpdateOnTagFinish() {
    String sampleAttribute = "accesskey";
    HtmlTagsAndAttributes htmlAttributes = HtmlTagsAndAttributes.getInstance();
    SimpleStringBag excluded = new SimpleStringBag();
    assertEquals(1, htmlAttributes.searchAttributes("html", excluded, sampleAttribute).size());
    excluded.add(sampleAttribute);

    JsonArray<Token> tokens1 = JsonCollections.createArray();
    tokens1.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    tokens1.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));
    String line1 = tokensToText(tokens1);
    parsedLines.put(line1, tokens1);

    JsonArray<Token> tokens2 = JsonCollections.createArray();
    tokens2.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, sampleAttribute));
    tokens2.add(new Token(CodeMirror2.HTML, TokenType.TAG, ">"));
    String line2 = tokensToText(tokens2);
    parsedLines.put(line2, tokens2);

    String text = line1 + "\n" + line2 + "\n";
    helper.setup(path, text, 0, line1.length(), false);
    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();

    parseOneLine();
    helper.autocompleter.requestAutocomplete();
    // "...please wait..."
    assertEquals(1, helper.popup.proposals.size());

    parseOneLine();
    assertEquals(htmlAttributes.searchAttributes("html", excluded, "").size(),
        helper.popup.proposals.size());
  }

  /**
   * Tests that {@link HtmlAutocompleter#getModeForColumn} gets the mode from
   * the anchor set prior to the given column.
   */
  public void testGetModeForColumn() {
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));

    prepareAutocompleter();

    Document document = helper.editor.getDocument();
    AnchorManager anchorManager = document.getAnchorManager();
    Line line = document.getFirstLine();
    HtmlAutocompleter htmlAutocompleter = helper.autocompleter.htmlAutocompleter;

    // Delete the anchor that was set by prepareAutocompleter().
    JsonArray<Anchor> anchors =
        AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE);
    assertEquals(0, anchors.size());

    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 0));
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 1));
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 2));
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 3));

    Anchor anchor = anchorManager.createAnchor(HtmlAutocompleter.MODE_ANCHOR_TYPE, line,
        AnchorManager.IGNORE_LINE_NUMBER, 2);
    anchor.setValue("m1");
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 0));
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 1));
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 2));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 3));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 4));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 5));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 6));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 10));

    anchor = anchorManager.createAnchor(HtmlAutocompleter.MODE_ANCHOR_TYPE, line,
        AnchorManager.IGNORE_LINE_NUMBER, 1);
    anchor.setValue("m0");
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 0));
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 1));
    assertEquals("m0", htmlAutocompleter.getModeForColumn(line, 2));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 3));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 4));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 5));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 6));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 10));

    anchor = anchorManager.createAnchor(HtmlAutocompleter.MODE_ANCHOR_TYPE, line,
        AnchorManager.IGNORE_LINE_NUMBER, 5);
    anchor.setValue("m2");
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 0));
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 1));
    assertEquals("m0", htmlAutocompleter.getModeForColumn(line, 2));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 3));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 4));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 5));
    assertEquals("m2", htmlAutocompleter.getModeForColumn(line, 6));
    assertEquals("m2", htmlAutocompleter.getModeForColumn(line, 10));

    anchor = anchorManager.createAnchor(HtmlAutocompleter.MODE_ANCHOR_TYPE, line,
        AnchorManager.IGNORE_LINE_NUMBER, 4);
    anchor.setValue("m3");
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 0));
    assertEquals(CodeMirror2.HTML, htmlAutocompleter.getModeForColumn(line, 1));
    assertEquals("m0", htmlAutocompleter.getModeForColumn(line, 2));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 3));
    assertEquals("m1", htmlAutocompleter.getModeForColumn(line, 4));
    assertEquals("m3", htmlAutocompleter.getModeForColumn(line, 5));
    assertEquals("m2", htmlAutocompleter.getModeForColumn(line, 6));
    assertEquals("m2", htmlAutocompleter.getModeForColumn(line, 10));
  }

  /**
   * Tests {@link HtmlAutocompleter#putModeAnchors}.
   */
  public void testPutModeAnchors() {
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));

    prepareAutocompleter();

    Document document = helper.editor.getDocument();
    Line line = document.getFirstLine();
    HtmlAutocompleter htmlAutocompleter = helper.autocompleter.htmlAutocompleter;

    // Delete the anchor that was set by prepareAutocompleter().
    JsonArray<Anchor> anchors =
        AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE);
    assertEquals(0, anchors.size());

    // Modes are empty, the previous line is null.
    JsonArray<Pair<Integer, String>> modes = JsonCollections.createArray();
    htmlAutocompleter.putModeAnchors(line, modes);
    assertTrue(
        AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE).isEmpty());

    // Modes are empty, the previous line is "null object".
    htmlAutocompleter.putModeAnchors(line, modes);
    assertTrue(
        AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE).isEmpty());

    // Create a line in another document and use it as a not-null previousLine.
    Line previousLine = Document.createFromString("").getFirstLine();
    assertNull(
        AnchorManager.getAnchorsByTypeOrNull(previousLine, HtmlAutocompleter.MODE_ANCHOR_TYPE));

    // Modes are empty, the previous line has no mode anchor.
    htmlAutocompleter.putModeAnchors(line, modes);
    assertTrue(
        AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE).isEmpty());

    // Modes are empty, the previous line has mode anchor.
    Anchor previousLineAnchor = previousLine.getDocument().getAnchorManager().createAnchor(
        HtmlAutocompleter.MODE_ANCHOR_TYPE, previousLine, AnchorManager.IGNORE_LINE_NUMBER, 0);
    previousLineAnchor.setValue("m1");
    htmlAutocompleter.putModeAnchors(line, modes);
    assertTrue(modes.isEmpty());
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE);
    assertEquals(0, anchors.size());

    // Modes are not empty (one mode), the previous line has mode anchor.
    modes.add(new Pair<Integer, String>(0, "m2"));
    htmlAutocompleter.putModeAnchors(line, modes);
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE);
    assertEquals(1, anchors.size());
    assertEquals(0, anchors.get(0).getColumn());
    assertEquals("m2", anchors.get(0).getValue());

    // Modes are not empty (two modes), the previous line has mode anchor.
    modes.add(new Pair<Integer, String>(3, "m3"));
    htmlAutocompleter.putModeAnchors(line, modes);
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE);
    assertEquals(2, anchors.size());
    assertEquals(0, anchors.get(0).getColumn());
    assertEquals("m2", anchors.get(0).getValue());
    assertEquals(3, anchors.get(1).getColumn());
    assertEquals("m3", anchors.get(1).getValue());

    // Modes are not empty (two modes), the previous line is null.
    htmlAutocompleter.putModeAnchors(line, modes);
    anchors = AnchorManager.getAnchorsByTypeOrNull(line, HtmlAutocompleter.MODE_ANCHOR_TYPE);
    assertEquals(2, anchors.size());
    assertEquals(0, anchors.get(0).getColumn());
    assertEquals("m2", anchors.get(0).getValue());
    assertEquals(3, anchors.get(1).getColumn());
    assertEquals("m3", anchors.get(1).getValue());
  }

  /**
   * Tests that proposal list is updated when parsing of document is finished.
   */
  public void testUpdateOnDocFinish() {
    String sampleAttribute = "accesskey";
    HtmlTagsAndAttributes htmlAttributes = HtmlTagsAndAttributes.getInstance();
    SimpleStringBag excluded = new SimpleStringBag();
    assertEquals(1, htmlAttributes.searchAttributes("html", excluded, sampleAttribute).size());
    excluded.add(sampleAttribute);

    JsonArray<Token> tokens1 = JsonCollections.createArray();
    tokens1.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    tokens1.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));
    String line1 = tokensToText(tokens1);
    parsedLines.put(line1, tokens1);

    JsonArray<Token> tokens2 = JsonCollections.createArray();
    tokens2.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, sampleAttribute));
    String line2 = tokensToText(tokens2);
    parsedLines.put(line2, tokens2);

    JsonArray<Token> tokens3 = JsonCollections.createArray();
    tokens3.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));
    String line3 = tokensToText(tokens3);
    parsedLines.put(line3, tokens3);

    String text = line1 + "\n" + line2 + "\n ";
    helper.setup(path, text, 0, line1.length(), false);
    helper.parser.getListenerRegistrar().add(new DocumentParserListenerAdapter(
        helper.autocompleter, helper.editor));
    helper.parser.begin();

    parseOneLine();
    helper.autocompleter.requestAutocomplete();
    // "...please wait..."
    assertEquals(1, helper.popup.proposals.size());

    parseOneLine();
    // Still "...please wait..."
    assertEquals(1, helper.popup.proposals.size());

    parseOneLine();
    assertEquals(htmlAttributes.searchAttributes("html", excluded, "").size(),
        helper.popup.proposals.size());
  }

  /**
   * Tests that find autocompletions do not ruin existing "clean" results.
   */
  public void testFindDoNotRuinResults() {
    String id = "id";
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));

    prepareAutocompleter();
    HtmlTagWithAttributes before = helper.editor.getDocument().getFirstLine().getTag(
        XmlCodeAnalyzer.TAG_END_TAG);
    assertNotNull(before);

    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, id));
    helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    HtmlTagWithAttributes after = helper.editor.getDocument().getFirstLine().getTag(
        XmlCodeAnalyzer.TAG_END_TAG);

    assertTrue("reference equality", before == after);
    assertFalse("tag modifications", after.getAttributes().contains(id));
  }

  /**
   * Tests that used attributes are excluded.
   */
  public void testExcludedHtmlAttributes() {
    String reversed = "reversed";
    HtmlTagsAndAttributes htmlAttributes = HtmlTagsAndAttributes.getInstance();
    SimpleStringBag excluded = new SimpleStringBag();
    assertEquals(1, htmlAttributes.searchAttributes("ol", excluded, reversed).size());
    excluded.add(reversed);

    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<ol"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, reversed));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATOM, "="));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.STRING, "\"\""));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));
    AutocompleteProposals proposals = findAutocompletions();

    assertEquals(htmlAttributes.searchAttributes("ol", excluded, "").size(), proposals.size());
    assertEquals("accesskey", proposals.get(0).getName());
    assertNull(TestUtils.selectProposalByName(proposals, reversed));
  }

  /**
   * Tests the proposals for ELEMENT.
   */
  public void testAutocompleteHtmlElements() {
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<a"));
    AutocompleteProposals proposals = findAutocompletions();

    assertEquals(7, proposals.size());
    assertEquals("abbr", proposals.get(1).getName());

    lineTokens.clear();
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<bod"));
    proposals = findAutocompletions();
    assertEquals(1, proposals.size());
    assertEquals("body", proposals.get(0).getName());

    lineTokens.clear();
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<body"));
    assertEquals(1, findAutocompletions().size());

    lineTokens.clear();
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<body"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, ">"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "</body"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, ">"));
    assertTrue(findAutocompletions().isEmpty());

    lineTokens.clear();
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<"));
    assertEquals(
        HtmlTagsAndAttributes.getInstance().searchTags("").size(), findAutocompletions().size());
  }

  /**
   * Tests the autocompletion of self-closing tag.
   */
  public void testAutocompleteSelfClosingTag() {
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<lin"));
    AutocompleteProposals autocompletions = findAutocompletions();
    assertNotNull(autocompletions);
    ProposalWithContext linkProposal = TestUtils.selectProposalByName(autocompletions, "link");
    assertNotNull(linkProposal);
    AutocompleteResult commonResult =
        helper.autocompleter.htmlAutocompleter.computeAutocompletionResult(linkProposal);
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals(4, result.getJumpLength());
    assertEquals("link />", result.getAutocompletionText());
  }

  /**
   * Tests full autocompletion for ATTRIBUTE.
   */
  public void testJumpLengthAndFullAutocompletionHtmlAttribute() {
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<body"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.WHITESPACE, " "));
    AutocompleteProposals autocompletions = findAutocompletions();
    assertNotNull(autocompletions);
    ProposalWithContext onloadProposal = TestUtils.selectProposalByName(autocompletions, "onload");
    assertNotNull(onloadProposal);
    AutocompleteResult commonResult =
        helper.autocompleter.htmlAutocompleter.computeAutocompletionResult(onloadProposal);
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals(8, result.getJumpLength());

    String fullAutocompletion = "onload=\"\"";
    assertEquals(fullAutocompletion, result.getAutocompletionText());
  }

  /**
   * Tests full autocompletion for ELEMENT.
   */
  public void testJumpLengthAndFullAutocompletionHtmlElement() {
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<bod"));
    AutocompleteProposals autocompletions = findAutocompletions();
    assertNotNull(autocompletions);
    ProposalWithContext bodyProposal = TestUtils.selectProposalByName(autocompletions, "body");
    assertNotNull(bodyProposal);

    AutocompleteResult commonResult =
        helper.autocompleter.htmlAutocompleter.computeAutocompletionResult(bodyProposal);
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals(5, result.getJumpLength());

    String fullAutocompletion = "body></body>";
    assertEquals(fullAutocompletion, result.getAutocompletionText());
  }
}
