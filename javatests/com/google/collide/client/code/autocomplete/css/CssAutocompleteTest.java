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

package com.google.collide.client.code.autocomplete.css;

import static com.google.collide.client.code.autocomplete.TestUtils.CTRL_SPACE;
import static com.google.collide.client.code.autocomplete.css.CssTrie.findAndFilterAutocompletions;

import com.google.collide.client.code.autocomplete.AbstractTrie;
import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Joiner;

/**
 * Tests for css autocompletion.
 *
 */
public class CssAutocompleteTest extends SynchronousTestCase {

  private CssAutocompleter cssAutocompleter;
  private MockAutocompleterEnvironment helper;

  @Override
  public String getModuleName() {
    return "com.google.cofllide.client.TestCode";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    cssAutocompleter = CssAutocompleter.create();
    helper = new MockAutocompleterEnvironment();
  }

  public void testFindAndFilterAutocompletions() {
    AbstractTrie<AutocompleteProposal> cssTrie = CssTrie.createTrie();
    JsonArray<String> completedProps = JsonCollections.createArray();
    JsonArray<AutocompleteProposal> proposals;

    proposals = findAndFilterAutocompletions(cssTrie, "clea", completedProps);
    assertEquals(1, proposals.size());
    assertEquals("clear", proposals.get(0).getName());

    proposals = findAndFilterAutocompletions(cssTrie, "clear", completedProps);
    assertEquals(1, proposals.size());
    assertEquals("clear", proposals.get(0).getName());

    proposals = findAndFilterAutocompletions(cssTrie, "hiybbprqag", completedProps);
    assertEquals(0, proposals.size());

    proposals = findAndFilterAutocompletions(cssTrie, "", completedProps);
    assertEquals(115, proposals.size());

    completedProps.add("clear");
    proposals = findAndFilterAutocompletions(cssTrie, "", completedProps);
    assertEquals(114, proposals.size());
  }

  /**
   * Tests getting the context.
   */
  public void testAttributeNameFullAutocompletion() {
    String text = Joiner.on("").join(new String[]{
        ".something {\n",
        "cur\n",
        "color: black;\n",
        "fake: ;\n",
        "}\n"
    });
    helper.setup(new PathUtil("test.css"), text, 1, 3, false);
    SelectionModel selection = helper.editor.getSelection();

    AutocompleteProposals completions = cssAutocompleter.findAutocompletions(selection, CTRL_SPACE);
    assertEquals(1, completions.size());
    AutocompleteResult commonResult = cssAutocompleter.computeAutocompletionResult(
        completions.select(0));
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals(8, result.getJumpLength());
    assertEquals("cursor: ;", result.getAutocompletionText());

    CssCompletionQuery query = cssAutocompleter.updateOrCreateQuery(
        null, selection.getCursorPosition());
    JsoArray<String> completedProperties = query.getCompletedProperties();
    assertEquals(2, completedProperties.size());
    assertEquals("color", completedProperties.get(0));
    assertEquals("fake", completedProperties.get(1));
  }

  /**
   * Tests getting the context.
   */
  public void testAttributeValueFullAutocompletion() {
    String text = Joiner.on("").join(new String[]{
        ".something {\n",
        "azimuth: \n",
    });
    helper.setup(new PathUtil("/some.css"), text, 1, 9, false);
    AutocompleteProposals completions = cssAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SPACE);
    assertEquals(14, completions.size());
    ProposalWithContext leftSideProposal = TestUtils.selectProposalByName(completions, "left-side");
    assertNotNull(leftSideProposal);
    AutocompleteResult commonResult = cssAutocompleter.computeAutocompletionResult(
        leftSideProposal);
    assertTrue("result type", commonResult instanceof DefaultAutocompleteResult);
    DefaultAutocompleteResult result = (DefaultAutocompleteResult) commonResult;
    assertEquals(9, result.getJumpLength());
  }

  /**
   * Tests filtering out properties that appear after.
   */
  public void testFilterPropertiesAfter() {
    CssCompletionQuery cssCompletionQuery =
        new CssCompletionQuery("back", "color: blue;\nfake: ;\nbackground-color: blue;");
    JsonArray<AutocompleteProposal> proposals =
        findAndFilterAutocompletions(CssTrie.createTrie(), cssCompletionQuery.getTriggeringString(),
            cssCompletionQuery.getCompletedProperties());
    // Notably, the list does not contain background-color.
    assertEquals(5, proposals.size());
    assertEquals("background", proposals.get(0).getName());
    assertEquals("background-attachment", proposals.get(1).getName());
    assertEquals("background-image", proposals.get(2).getName());
    assertEquals("background-position", proposals.get(3).getName());
    assertEquals("background-repeat", proposals.get(4).getName());
  }

  /**
   * Tests filtering out properties that appear after.
   */
  public void testFilterExistingValuesAfter() {
    CssCompletionQuery cssCompletionQuery = new CssCompletionQuery(
        "background: ", "black;\ncolor: blue;\nfake: ;\nbackground-color: blue;");

    JsoArray<AutocompleteProposal> proposals =
        CssPartialParser.getInstance().getAutocompletions(cssCompletionQuery.getProperty(),
            cssCompletionQuery.getValuesBefore(), cssCompletionQuery.getTriggeringString(),
            cssCompletionQuery.getValuesAfter());

    // Notably, the list does not contain colors.
    assertEquals(2, proposals.size());
    assertEquals("transparent", proposals.get(0).getName());
    assertEquals("inherit", proposals.get(1).getName());
  }

  /**
   * Tests filtering out properties that appear before.
   */
  public void testFilterExistingValuesBefore() {
    CssCompletionQuery cssCompletionQuery = new CssCompletionQuery(
        "background: black ", "\ncolor: blue;\nfake: ;\nbackground-color: blue;");
    JsoArray<AutocompleteProposal> proposals =
        CssPartialParser.getInstance().getAutocompletions(cssCompletionQuery.getProperty(),
            cssCompletionQuery.getValuesBefore(), cssCompletionQuery.getTriggeringString(),
            cssCompletionQuery.getValuesAfter());

    // Notably, the list does not contain colors.
    assertEquals(3, proposals.size());
    assertEquals("<uri>", proposals.get(0).getName());
    assertEquals("none", proposals.get(1).getName());
    assertEquals("inherit", proposals.get(2).getName());
  }

  /**
   * Tests that for some properties values can be repeated, so they are proposed
   * again.
   */
  public void testRepeatingProperties() {
    CssCompletionQuery cssCompletionQuery = new CssCompletionQuery(
        "padding: 19px ", "\ncolor: blue;\nfake: ;\nbackground-color: blue;");
    JsoArray<AutocompleteProposal> proposals =
        CssPartialParser.getInstance().getAutocompletions(cssCompletionQuery.getProperty(),
            cssCompletionQuery.getValuesBefore(), cssCompletionQuery.getTriggeringString(),
            cssCompletionQuery.getValuesAfter());

    // Notably, the list does not contain colors.
    assertEquals(10, proposals.size());
    assertEquals("<number>em", proposals.get(0).getName());
    assertEquals("<number>ex", proposals.get(1).getName());
    assertEquals("<number>in", proposals.get(2).getName());
    assertEquals("<number>cm", proposals.get(3).getName());
    assertEquals("<number>mm", proposals.get(4).getName());
    assertEquals("<number>pt", proposals.get(5).getName());
    assertEquals("<number>pc", proposals.get(6).getName());
    assertEquals("<number>px", proposals.get(7).getName());
    assertEquals("<number>%", proposals.get(8).getName());
    assertEquals("inherit", proposals.get(9).getName());
  }

  /**
   * Tests that number proposals come up first for the property 'border'.
   */
  public void testBorder() {
    CssCompletionQuery cssCompletionQuery =
        new CssCompletionQuery("border: ", "\ncolor: blue;\nfake: ;\nbackground-color: blue;");
    JsoArray<AutocompleteProposal> proposals =
        CssPartialParser.getInstance().getAutocompletions(cssCompletionQuery.getProperty(),
            cssCompletionQuery.getValuesBefore(), cssCompletionQuery.getTriggeringString(),
            cssCompletionQuery.getValuesAfter());

    // Notably, number proposals are first.
    assertEquals(43, proposals.size());
    assertEquals("<number>em", proposals.get(0).getName());
  }

  /**
   * Tests filtering out properties that appear after 'border'.
   */
  public void testBorderWithExistingValue() {
    CssCompletionQuery cssCompletionQuery =
        new CssCompletionQuery("border: 1px ", "color: blue;\nfake: ;\nbackground-color: blue;");
    JsoArray<AutocompleteProposal> proposals =
        CssPartialParser.getInstance().getAutocompletions(cssCompletionQuery.getProperty(),
            cssCompletionQuery.getValuesBefore(), cssCompletionQuery.getTriggeringString(),
            cssCompletionQuery.getValuesAfter());

    // Notably, the list does not contain background-color.
    assertEquals(15, proposals.size());
  }

  public void testQueryType() {
    CssCompletionQuery query = new CssCompletionQuery("clea", "");
    assertEquals(CompletionType.PROPERTY, query.getCompletionType());
  }
}
