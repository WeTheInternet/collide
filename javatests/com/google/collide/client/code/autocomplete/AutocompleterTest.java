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

package com.google.collide.client.code.autocomplete;

import static com.google.collide.client.code.autocomplete.TestUtils.createDocumentParser;

import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.code.autocomplete.AutocompleteResult.PopupAction;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment.MockAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.CodeGraphAutocompleter;
import com.google.collide.client.code.autocomplete.css.CssAutocompleter;
import com.google.collide.client.code.autocomplete.html.HtmlAutocompleter;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.JsonCollections;

/**
 * Test for some aspects of autocompletion life cycle.
 *
 */
public class AutocompleterTest extends SynchronousTestCase {

  private static class StubAutocompleter extends NoneAutocompleter {

    public StubAutocompleter() {
      super(SyntaxType.HTML);
    }

    @Override
    public AutocompleteProposals findAutocompletions(SelectionModel selection,
        SignalEventEssence trigger) {
      return new AutocompleteProposals(SyntaxType.NONE, "",
          JsonCollections.createArray(new AutocompleteProposal("ab")));
    }

    @Override
    public AutocompleteResult computeAutocompletionResult(
        ProposalWithContext proposal) {
      return new DefaultAutocompleteResult("ab", 2, 0, 0, 0, PopupAction.CLOSE, "a");
    }
  }

  private MockAutocompleterEnvironment helper;
  private PathUtil path;

  @Override
  public String getModuleName() {
    return
        "com.google.collide.client.TestCode";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    helper = new MockAutocompleterEnvironment();
    path = new PathUtil("/test.none");
  }

  private static void changeAutocompleterPath(Autocompleter autocompleter, PathUtil path) {
    autocompleter.reset(path, createDocumentParser(path));
  }

  public void testAutocompleteControllerLifecycleImplicitOnDocEvent() {
    helper.specificAutocompleter = new StubAutocompleter();
    Autocompleter autocompleter = helper.setup(path, "<a", 0, 2, false);
    assertNotNull(autocompleter.getController());
    autocompleter.requestAutocomplete(autocompleter.getController(), null);
    assertNotNull(autocompleter.getController());
    autocompleter.dismissAutocompleteBox();
    assertNotNull(autocompleter.getController());
  }

  public void testDismissAutocompleteBox() {
    helper.specificAutocompleter = new StubAutocompleter();
    MockAutocompleter autocompleter = helper.setup(path, "a", 0, 1, false);
    autocompleter.requestAutocomplete(autocompleter.getController(), null);
    assertTrue("expected: popup appeared", helper.popup.isShowing());
    autocompleter.dismissAutocompleteBox();
    assertFalse("expected: popup disappeared", helper.popup.isShowing());
  }

  public void testDoAutocomplete() {
    helper.specificAutocompleter = new StubAutocompleter();
    MockAutocompleter autocompleter = helper.setup(path, "a", 0, 1, false);
    autocompleter.requestAutocomplete(autocompleter.getController(), null);
    assertTrue("expected: popup appeared", helper.popup.isShowing());
    assertEquals("expected: 1 proposal found", 1, helper.popup.proposals.size());
  }

  public void testEditorContentsReplaced() {
    Autocompleter autocompleter = helper.setup(path, "", 0, 0, false);
    changeAutocompleterPath(autocompleter, new PathUtil("/test.html"));
    assertEquals(SyntaxType.HTML, autocompleter.getMode());

    changeAutocompleterPath(autocompleter, new PathUtil("/test.js"));
    assertEquals(SyntaxType.JS, autocompleter.getMode());

    changeAutocompleterPath(autocompleter, new PathUtil("/test.py"));
    assertEquals(SyntaxType.PY, autocompleter.getMode());

    changeAutocompleterPath(autocompleter, new PathUtil("/test.css"));
    assertEquals(SyntaxType.CSS, autocompleter.getMode());

    changeAutocompleterPath(autocompleter, new PathUtil("/test.foo"));
    assertEquals(SyntaxType.NONE, autocompleter.getMode());

    changeAutocompleterPath(autocompleter, new PathUtil(""));
    assertEquals(SyntaxType.NONE, autocompleter.getMode());
  }

  public void testFinishAutocompletion() {
    helper.specificAutocompleter = new StubAutocompleter();
    MockAutocompleter autocompleter = helper.setup(path, "a", 0, 1, false);
    AutocompleteController controller = autocompleter.getController();
    assertNotNull(controller);
    autocompleter.requestAutocomplete(controller, null);
    autocompleter.reallyFinishAutocompletion(helper.popup.proposals.select(0));

    assertEquals("ab", helper.editor.getDocument().asText());
    // check that the caret is in the right place
    assertEquals(2, helper.editor.getSelection().getCursorColumn());
    assertNotNull(autocompleter.getController());
  }

  public void testProposalsUpdate() {
    helper.specificAutocompleter = new StubAutocompleter();
    Autocompleter autocompleter = helper.setup(path, "a", 0, 1, false);

    AutocompleteController controller = autocompleter.getController();
    autocompleter.requestAutocomplete(controller, null);

    // Requesting completions again
    controller.start(helper.editor.getSelection(), null);

    assertEquals(1, helper.popup.proposals.size());
  }

  public void testProposalsSorting() {
    JsonArray<AutocompleteProposal> unsortedCompletions = JsonCollections.createArray();
    unsortedCompletions.add(new AutocompleteProposal("ur"));
    unsortedCompletions.add(new AutocompleteProposal("go"));
    unsortedCompletions.add(new AutocompleteProposal("Gb"));
    unsortedCompletions.add(new AutocompleteProposal("ga"));
    unsortedCompletions.add(new AutocompleteProposal("ab"));
    unsortedCompletions.add(new AutocompleteProposal("gA"));
    unsortedCompletions.add(new AutocompleteProposal("go"));

    AutocompleteProposals proposals =
        new AutocompleteProposals(SyntaxType.NONE, "", unsortedCompletions);

    assertEquals("input/output size", unsortedCompletions.size(), proposals.size());
    String previous = proposals.get(0).getLabel();
    for (int i = 1, l = proposals.size(); i < l; i++) {
      String current = proposals.get(i).getLabel();
      assertTrue("order", current.compareToIgnoreCase(previous) >= 0);
      previous = current;
    }
  }

  public void testAutocompletionMode() {
    Autocompleter autocompleter = helper.setup(path, "", 0, 0, false);

    SyntaxType mode = SyntaxType.NONE;
    assertTrue(autocompleter.getAutocompleter(mode) instanceof  NoneAutocompleter);

    mode = SyntaxType.YAML;
    assertTrue(autocompleter.getAutocompleter(mode) instanceof  NoneAutocompleter);

    mode = SyntaxType.SVG;
    assertTrue(autocompleter.getAutocompleter(mode) instanceof  NoneAutocompleter);

    mode = SyntaxType.XML;
    assertTrue(autocompleter.getAutocompleter(mode) instanceof NoneAutocompleter);

    mode = SyntaxType.CSS;
    assertTrue(autocompleter.getAutocompleter(mode) instanceof CssAutocompleter);

    mode = SyntaxType.HTML;
    assertTrue(autocompleter.getAutocompleter(mode) instanceof HtmlAutocompleter);

    mode = SyntaxType.JS;
    LanguageSpecificAutocompleter jsAutocompleter = autocompleter.getAutocompleter(mode);
    assertTrue(jsAutocompleter instanceof CodeGraphAutocompleter);
    assertEquals(mode, jsAutocompleter.getMode());
  }

  public void testAutocompletionDoNotBreakAppOnSelectedText() {
    helper.specificAutocompleter = new LanguageSpecificAutocompleter(SyntaxType.HTML) {

      @Override
      public AutocompleteResult computeAutocompletionResult(ProposalWithContext proposal) {
        return new DefaultAutocompleteResult("[]", 1, 0, 0, 0, PopupAction.CLOSE, "");
      }

      @Override
      public AutocompleteProposals findAutocompletions(
          SelectionModel selection, SignalEventEssence trigger) {
        return AutocompleteProposals.EMPTY;
      }

      @Override
      public void cleanup() {}
    };

    // Forward selection
    Autocompleter autocompleter = helper.setup(path, "go (veryLongSelection).q!", 0, 0, false);
    Editor editor = helper.editor;
    LineInfo lineInfo = editor.getDocument().getLineFinder().findLine(0);
    editor.getSelection().setSelection(lineInfo, 4, lineInfo, 21);
    ProposalWithContext proposal = new ProposalWithContext(SyntaxType.NONE,
        new AutocompleteProposal(""), new AutocompleteProposals.Context(""));
    autocompleter.reallyFinishAutocompletion(proposal);
    assertEquals("go ([]).q!", editor.getDocument().getFirstLine().getText());
    assertFalse(editor.getSelection().hasSelection());
    assertEquals(5, editor.getSelection().getCursorPosition().getColumn());

    // Reverse selection
    autocompleter = helper.setup(path, "go (veryLongSelection).q!", 0, 0, false);
    editor = helper.editor;
    lineInfo = editor.getDocument().getLineFinder().findLine(0);
    editor.getSelection().setSelection(lineInfo, 21, lineInfo, 4);
    autocompleter.reallyFinishAutocompletion(proposal);
    assertEquals("go ([]).q!", editor.getDocument().getFirstLine().getText());
    assertFalse(editor.getSelection().hasSelection());
    assertEquals(5, editor.getSelection().getCursorPosition().getColumn());
  }
}
