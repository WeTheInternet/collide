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

import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.codegraph.CompletionContext;
import com.google.collide.client.code.autocomplete.codegraph.py.PyProposalBuilder;
import com.google.collide.client.testutil.CodeMirrorTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.PyState;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;

/**
 * Test for PY autocompletion cases, when codemirror parser is used.
 *
 */
public class PyCodemirrorTest extends CodeMirrorTestCase {
  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testNoProposalsInCommentsAndStrings() {
                // 0         1         2
                // 01234567890123456789012345
    String text = "a = 'Hello Kitty' # Funny?";

    checkHasProposals(text, 0, true, "global");
    checkHasProposals(text, 4, true, "before string");
    checkHasProposals(text, 5, false, "string began");
    checkHasProposals(text, 11, false, "in string after space");
    checkHasProposals(text, 15, false, "in string");
    checkHasProposals(text, 17, true, "after string");
    checkHasProposals(text, 18, true, "before comment");
    checkHasProposals(text, 20, false, "in comment after space");
    checkHasProposals(text, 23, false, "in comment");
  }

  private void checkHasProposals(String text, int column,
      boolean expectHasProposals, String message) {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.py"), text, 0, column, true);
    AutocompleteProposals proposals = helper.autocompleter.pyAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    assertEquals(message, expectHasProposals, proposals.size() > 0);
  }

  public void testContextBuilding() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String text = "a .bc.de .f";
    helper.setup(new PathUtil("foo.py"), text, 0, text.length(), true);
    PyProposalBuilder proposalBuilder = new PyProposalBuilder();
    CompletionContext<PyState> completionContext = proposalBuilder
        .buildContext(helper.editor.getSelection(), helper.parser);
    assertEquals("previous context", "a.bc.de.", completionContext.getPreviousContext());
    assertEquals("triggering string", "f", completionContext.getTriggeringString());
  }

  public void testTemplateProposalsInGlobal() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.py"), "con", 0, 3, true);
    AutocompleteProposals autocompletions =
        helper.autocompleter.pyAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SPACE);
    AutocompleteProposals.ProposalWithContext proposal = autocompletions.select(0);
    assertEquals("proposal name", "continue", proposal.getItem().getName());
    helper.autocompleter.reallyFinishAutocompletion(proposal);

    String text = helper.editor.getDocument().getFirstLine().getText();
    assertEquals("resulting text", "continue\n", text);
  }

  public void testOperatorKeywords() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String text = "if a is not None:";
    helper.setup(new PathUtil("foo.py"), text, 0, text.length(), true);
    JsonArray<Token> tokens = helper.parser
        .parseLineSync(helper.editor.getDocument().getFirstLine());
    assertEquals("4-th token == 'is'", "is", tokens.get(4).getValue());
    assertEquals("4-th token is keyword", TokenType.KEYWORD, tokens.get(4).getType());
    assertEquals("6-th token == 'not'", "not", tokens.get(6).getValue());
    assertEquals("6-th token is keyword", TokenType.KEYWORD, tokens.get(6).getType());
  }
}
