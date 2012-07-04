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
import com.google.collide.client.code.autocomplete.TestUtils.MockIncrementalScheduler;
import com.google.collide.client.code.autocomplete.codegraph.CodeGraphAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.LimitedContextFilePrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.js.JsAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.js.JsIndexUpdater;
import com.google.collide.client.code.autocomplete.codegraph.py.PyAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.py.PyIndexUpdater;
import com.google.collide.client.code.autocomplete.css.CssAutocompleter;
import com.google.collide.client.code.autocomplete.html.HtmlAutocompleter;
import com.google.collide.client.codeunderstanding.CodeGraphTestUtils.MockCubeClient;
import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.collections.SkipListStringBag;
import com.google.collide.codemirror2.Parser;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;

/**
 * Autocompleter and editor setup code.
 *
 * <p>This code was moved from TestSetupHelper.
 */
public class MockAutocompleterEnvironment {

  /**
   * Simplest implementation that remembers passed data.
   */
  public static class MockAutocompleterPopup implements AutocompleteBox {

    boolean isShown;

    public AutocompleteProposals proposals = AutocompleteProposals.EMPTY;

    public Events delegate;

    @Override
    public boolean isShowing() {
      return isShown;
    }

    @Override
    public boolean consumeKeySignal(SignalEventEssence signal) {
      return false;
    }

    @Override
    public void setDelegate(Events delegate) {
      this.delegate = delegate;
    }

    @Override
    public void dismiss() {
      isShown = false;
    }

    @Override
    public void positionAndShow(AutocompleteProposals items) {
      isShown = true;
      this.proposals = items;
    }
  }

  /**
   * {@link Autocompleter} implementation that allows to substitute given
   * {@link LanguageSpecificAutocompleter}.
   */
  public static class MockAutocompleter extends Autocompleter {

    private LanguageSpecificAutocompleter specificAutocompleter;
    public final SkipListStringBag localPrefixIndexStorage;
    public final HtmlAutocompleter htmlAutocompleter;
    public final CssAutocompleter cssAutocompleter;
    public final CodeGraphAutocompleter jsAutocompleter;
    public final CodeGraphAutocompleter pyAutocompleter;

    public static MockAutocompleter create(
        Editor editor, CubeClient cubeClient, AutocompleteBox popup) {
      SkipListStringBag localPrefixIndexStorage = new SkipListStringBag();
      LimitedContextFilePrefixIndex contextFilePrefixIndex = new LimitedContextFilePrefixIndex(
          10, localPrefixIndexStorage);

      CssAutocompleter cssAutocompleter = CssAutocompleter.create();
      CodeGraphAutocompleter jsAutocompleter = JsAutocompleter.create(
          cubeClient, contextFilePrefixIndex);
      HtmlAutocompleter htmlAutocompleter = HtmlAutocompleter.create(
          cssAutocompleter, jsAutocompleter);
      CodeGraphAutocompleter pyAutocompleter = PyAutocompleter.create(
          cubeClient, contextFilePrefixIndex);
      return new MockAutocompleter(editor, popup, localPrefixIndexStorage, htmlAutocompleter,
          cssAutocompleter, jsAutocompleter, pyAutocompleter);
    }

    MockAutocompleter(Editor editor, final AutocompleteBox popup,
        SkipListStringBag localPrefixIndexStorage, HtmlAutocompleter htmlAutocompleter,
        CssAutocompleter cssAutocompleter, CodeGraphAutocompleter jsAutocompleter,
        CodeGraphAutocompleter pyAutocompleter) {
      super(editor, popup, localPrefixIndexStorage, htmlAutocompleter, cssAutocompleter,
          jsAutocompleter, pyAutocompleter, new PyIndexUpdater(), new JsIndexUpdater());
      this.localPrefixIndexStorage = localPrefixIndexStorage;
      this.htmlAutocompleter = htmlAutocompleter;
      this.cssAutocompleter = cssAutocompleter;
      this.jsAutocompleter = jsAutocompleter;
      this.pyAutocompleter = pyAutocompleter;
    }

    @Override
    protected LanguageSpecificAutocompleter getAutocompleter(SyntaxType mode) {
      if (specificAutocompleter != null && SyntaxType.NONE == mode) {
        return specificAutocompleter;
      }
      return super.getAutocompleter(mode);
    }

    @Override
    public void reallyFinishAutocompletion(ProposalWithContext proposal) {
      super.reallyFinishAutocompletion(proposal);
    }

    public boolean pressKey(SignalEventEssence key) {
      return processKeyPress(key);
    }

    public void requestAutocomplete() {
      super.requestAutocomplete(getController(), null);
    }
  }

  public MockAutocompleter autocompleter;
  public Editor editor;
  public final MockCubeClient cubeClient = MockCubeClient.create();
  public LanguageSpecificAutocompleter specificAutocompleter;
  public Parser specificParser;
  public MockAutocompleterPopup popup;
  public DocumentParser parser;
  public MockIncrementalScheduler parseScheduler = new MockIncrementalScheduler();

  public MockAutocompleter setup(PathUtil path, String text, int lineNumber, int column,
      boolean setupRealParser) {
    return setup(path, Document.createFromString(text), lineNumber, column, setupRealParser);
  }

  public MockAutocompleter setup(PathUtil path, Document document, int lineNumber, int column,
      boolean setupRealParser) {
    editor = Editor.create(new MockAppContext());
    editor.setDocument(document);
    popup = new MockAutocompleterPopup();
    autocompleter = MockAutocompleter.create(editor, cubeClient, popup);
    autocompleter.specificAutocompleter = specificAutocompleter;
    if (specificParser == null) {
      parser = createDocumentParser(path, setupRealParser, parseScheduler, document);
    } else {
      parser = DocumentParser.create(document, specificParser, parseScheduler);
    }
    autocompleter.reset(path, parser);
    LineInfo lineInfo = editor.getDocument().getLineFinder().findLine(lineNumber);
    editor.getSelection().setSelection(lineInfo, column, lineInfo, column);
    return autocompleter;
  }
}
