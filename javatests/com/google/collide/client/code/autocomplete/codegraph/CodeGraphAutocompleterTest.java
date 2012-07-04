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

import static com.google.collide.client.code.autocomplete.TestUtils.CTRL_SPACE;
import static com.google.collide.codemirror2.TokenType.NULL;

import com.google.collide.client.code.autocomplete.AbstractTrie;
import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals.Context;
import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.collections.SkipListStringBag;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;

/**
 * Tests for JavaScript autocompletion.
 *
 */
public class CodeGraphAutocompleterTest extends SynchronousTestCase {

  /**
   * The simplest completion context implementation.
   */
  static class MockProposalBuilder extends ProposalBuilder<State> {

    @Override
    public CompletionContext<State> buildContext(
        SelectionModel selection, DocumentParser parser) {
      JsonArray<Token> tokens = JsonCollections.createArray();
      State state = TestUtils.createMockState();
      tokens.add(new Token(null, NULL, ""));
      ParseResult<State> parseResult = new ParseResult<State>(tokens, state) {};
      return buildContext(
          new ParseUtils.ExtendedParseResult<State>(parseResult, ParseUtils.Context.IN_CODE));
    }

    public MockProposalBuilder() {
      super(State.class);
    }

    @Override
    protected void addShortcutsTo(CompletionContext<State> context, JsonStringSet prefixes) {
    }

    @Override
    protected JsonArray<String> getLocalVariables(ParseResult<State> stateParseResult) {
      return JsonCollections.createArray();
    }

    @Override
    protected PrefixIndex<TemplateProposal> getTemplatesIndex() {
      return new AbstractTrie<TemplateProposal>();
    }

    @Override
    protected boolean checkIsThisPrefix(String prefix) {
      return "zis.".equals(prefix);
    }
  }

  private PathUtil path;
  private MockAutocompleterEnvironment helper;
  private CodeGraphAutocompleter autocompleter;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    path = new PathUtil("/test.none");
    helper = new MockAutocompleterEnvironment();

    SkipListStringBag localPrefixIndexStorage = new SkipListStringBag();
    LimitedContextFilePrefixIndex contextFilePrefixIndex = new LimitedContextFilePrefixIndex(
        10, localPrefixIndexStorage);
    autocompleter = new CodeGraphAutocompleter(SyntaxType.JS, new MockProposalBuilder(),
        helper.cubeClient, contextFilePrefixIndex, new ExplicitAutocompleter());

    helper.specificAutocompleter = autocompleter;
  }

  public void testFullFunctionCompletion() {
    helper.setup(path, "get", 0, 3, false);
    autocompleter.findAutocompletions(helper.editor.getSelection(), CTRL_SPACE);
    AutocompleteProposal functionProposal = new CodeGraphProposal("getFoo",
        path, true);
    AutocompleteResult commonResult = autocompleter.computeAutocompletionResult(
        new ProposalWithContext(SyntaxType.NONE, functionProposal, new Context("get")));
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals("jump length", 7, result.getJumpLength());
    assertEquals("autocompletion text", "getFoo()", result.getAutocompletionText());
  }

  /*
   * TODO: Write test that tests proposals update when updater
   *               fires notification
   */
  public void testFullPropertyCompletion() {
    helper.setup(path, "g", 0, 1, false);
    autocompleter.findAutocompletions(helper.editor.getSelection(), CTRL_SPACE);
    AutocompleteProposal propertyProposal = new CodeGraphProposal("gender",
        path, false);
    AutocompleteResult commonResult = autocompleter.computeAutocompletionResult(
        new ProposalWithContext(SyntaxType.NONE, propertyProposal, new Context("get")));
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals("jump length", 6, result.getJumpLength());
    assertEquals("autocompletion text", "gender", result.getAutocompletionText());
  }

  public void testTemplateProcessing() {
    helper.setup(path, "", 0, 0, false);
    autocompleter.findAutocompletions(helper.editor.getSelection(), CTRL_SPACE);
    AutocompleteProposal proposal = new TemplateProposal("simple", "simple (%c) <%i%n>");
    AutocompleteResult commonResult = autocompleter.computeAutocompletionResult(
        new ProposalWithContext(SyntaxType.NONE, proposal, new Context("")));
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals("autocompletion text", "simple () <\n  \n>", result.getAutocompletionText());
    assertEquals("jump length", 8, result.getJumpLength());
    assertEquals("backspace count", 0, result.getBackspaceCount());
  }
}
