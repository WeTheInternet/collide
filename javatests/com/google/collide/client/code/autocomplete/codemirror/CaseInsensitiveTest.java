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
import com.google.collide.client.code.autocomplete.Autocompleter;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.testutil.CodeMirrorTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.shared.document.Line;

/**
 * Test cases that check case-insensitiveness of autocompleter.
 */
public class CaseInsensitiveTest extends CodeMirrorTestCase {

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    assertTrue(Autocompleter.CASE_INSENSITIVE);
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testTemplate() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.js"), "WhI", 0, 3, true);
    AutocompleteProposals proposals = helper.autocompleter.jsAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    AutocompleteProposals.ProposalWithContext proposal = TestUtils.selectProposalByName(
        proposals, "while");
    assertNotNull(proposal);
    helper.autocompleter.reallyFinishAutocompletion(proposal);
    assertEquals("while () {\n", helper.editor.getDocument().getFirstLine().getText());
  }

  public void testLocalVariable() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String text = "function foo() {\n"
        + "  var barBeQue;\n"
        + "  BArbE\n" // Cursor here.
        + "}";

    helper.setup(new PathUtil("foo.js"), text, 2, 7, true);
    helper.parser.begin();
    helper.parseScheduler.requests.get(0).run(10);
    AutocompleteProposals proposals = helper.autocompleter.jsAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    AutocompleteProposals.ProposalWithContext proposal = TestUtils.selectProposalByName(
        proposals, "barBeQue");
    assertNotNull(proposal);
    helper.autocompleter.reallyFinishAutocompletion(proposal);
    Line thirdLine = helper.editor.getDocument().getLineFinder().findLine(2).line();
    assertEquals("  barBeQue\n", thirdLine.getText());
  }

  public void testCssProperty() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String prefix = "td {bORDEr: black; ";
    helper.setup(new PathUtil("foo.css"), prefix + "boR", 0, prefix.length() + 3, true);
    AutocompleteProposals proposals = helper.autocompleter.cssAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    assertNull(TestUtils.selectProposalByName(proposals, "border"));

    AutocompleteProposals.ProposalWithContext proposal = TestUtils.selectProposalByName(
        proposals, "border-color");
    assertNotNull(proposal);
    helper.autocompleter.reallyFinishAutocompletion(proposal);
    String text = helper.editor.getDocument().getFirstLine().getText();
    assertTrue(text.startsWith(prefix + "border-color"));
  }

  public void testCssValue() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String prefix = "td {color: ";
    helper.setup(new PathUtil("foo.css"), prefix + "BLA", 0, prefix.length() + 3, true);
    AutocompleteProposals proposals = helper.autocompleter.cssAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    AutocompleteProposals.ProposalWithContext proposal = TestUtils.selectProposalByName(
        proposals, "black");
    assertNotNull(proposal);
    helper.autocompleter.reallyFinishAutocompletion(proposal);
    String text = helper.editor.getDocument().getFirstLine().getText();
    assertTrue(text.startsWith(prefix + "black"));
  }

  public void testHtmlAttributes() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String prefix = "<html iD='' ";
    helper.setup(new PathUtil("foo.html"), prefix + "I", 0, prefix.length() + 1, true);
    helper.parser.begin();
    helper.parseScheduler.requests.get(0).run(10);
    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    assertNull(TestUtils.selectProposalByName(proposals, "id"));
    AutocompleteProposals.ProposalWithContext proposal = TestUtils.selectProposalByName(
        proposals, "itemid");
    assertNotNull(proposal);
    helper.autocompleter.reallyFinishAutocompletion(proposal);
    String text = helper.editor.getDocument().getFirstLine().getText();
    assertTrue(text.startsWith(prefix + "itemid"));
  }

  public void testHtmlPreviousLineAttributes() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    String prefix = "<html iD='' \n";
    helper.setup(new PathUtil("foo.html"), prefix + "I", 1, 1, true);
    helper.parser.begin();
    helper.parseScheduler.requests.get(0).run(10);
    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    assertNull(TestUtils.selectProposalByName(proposals, "id"));
  }

  public void testHtmlTag() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();

    helper.setup(new PathUtil("foo.html"), "<HT", 0, 3, true);
    helper.parser.begin();
    helper.parseScheduler.requests.get(0).run(10);
    AutocompleteProposals proposals = helper.autocompleter.htmlAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    AutocompleteProposals.ProposalWithContext proposal = TestUtils.selectProposalByName(
        proposals, "html");
    assertNotNull(proposal);
    helper.autocompleter.reallyFinishAutocompletion(proposal);
    String text = helper.editor.getDocument().getFirstLine().getText();
    assertTrue(text.startsWith("<html"));
  }
}
