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

import javax.annotation.Nonnull;

import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;

import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitAction;
import com.google.collide.client.code.autocomplete.codegraph.CodeGraphAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.LimitedContextFilePrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.ParsingTask;
import com.google.collide.client.code.autocomplete.codegraph.js.JsAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.js.JsIndexUpdater;
import com.google.collide.client.code.autocomplete.codegraph.py.PyAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.py.PyIndexUpdater;
import com.google.collide.client.code.autocomplete.css.CssAutocompleter;
import com.google.collide.client.code.autocomplete.html.HtmlAutocompleter;
import com.google.collide.client.code.autocomplete.html.XmlCodeAnalyzer;
import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.ScheduledCommandExecutor;
import com.google.collide.client.util.collections.SkipListStringBag;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * Class to implement all the autocompletion support that is not specific to a
 * given language (e.g., css).
 */
public class Autocompleter {

  /**
   * Flag that specifies if proposals are filtered case-insensitively.
   *
   * <p>Once, this constant should become configuration option.
   */
  public static final boolean CASE_INSENSITIVE = true;

  /**
   * Constant which limits number of results returned by
   * {@link LimitedContextFilePrefixIndex}.
   */
  private static final int LOCAL_PREFIX_INDEX_LIMIT = 50;

  private static final XmlCodeAnalyzer XML_CODE_ANALYZER = new XmlCodeAnalyzer();

  private final SkipListStringBag localPrefixIndexStorage;
  private final ParsingTask localPrefixIndexUpdater;
  private final PyIndexUpdater pyIndexUpdater;
  private final JsIndexUpdater jsIndexUpdater;

  private final HtmlAutocompleter htmlAutocompleter;
  private final CssAutocompleter cssAutocompleter;
  private final CodeGraphAutocompleter jsAutocompleter;
  private final CodeGraphAutocompleter pyAutocompleter;

  /**
   * Key that triggered autocomplete box opening.
   */
  private SignalEventEssence boxTrigger;

  /**
   * Proxy that distributes notifications to all code analyzers.
   */
  private final CodeAnalyzer distributingCodeAnalyzer = new CodeAnalyzer() {
    @Override
    public void onBeforeParse() {
      XML_CODE_ANALYZER.onBeforeParse();
      localPrefixIndexUpdater.onBeforeParse();
      pyIndexUpdater.onBeforeParse();
      jsIndexUpdater.onBeforeParse();
    }

    @Override
    public void onParseLine(
        TaggableLine previousLine, TaggableLine line, @Nonnull JsonArray<Token> tokens) {
      LanguageSpecificAutocompleter languageAutocompleter = getLanguageSpecificAutocompleter();
      if (htmlAutocompleter == languageAutocompleter) {
        htmlAutocompleter.updateModeAnchors(line, tokens);
        XML_CODE_ANALYZER.onParseLine(previousLine, line, tokens);
        localPrefixIndexUpdater.onParseLine(previousLine, line, tokens);
        jsIndexUpdater.onParseLine(previousLine, line, tokens);
      } else if (pyAutocompleter == languageAutocompleter) {
        localPrefixIndexUpdater.onParseLine(previousLine, line, tokens);
        pyIndexUpdater.onParseLine(previousLine, line, tokens);
      } else if (jsAutocompleter == languageAutocompleter) {
        localPrefixIndexUpdater.onParseLine(previousLine, line, tokens);
        jsIndexUpdater.onParseLine(previousLine, line, tokens);
      }
    }

    @Override
    public void onAfterParse() {
      XML_CODE_ANALYZER.onAfterParse();
      localPrefixIndexUpdater.onAfterParse();
      pyIndexUpdater.onAfterParse();
      jsIndexUpdater.onAfterParse();
    }

    @Override
    public void onLinesDeleted(JsonArray<TaggableLine> deletedLines) {
      XML_CODE_ANALYZER.onLinesDeleted(deletedLines);
      localPrefixIndexUpdater.onLinesDeleted(deletedLines);
      pyIndexUpdater.onLinesDeleted(deletedLines);
      jsIndexUpdater.onLinesDeleted(deletedLines);
    }
  };

  private class OnSelectCommand extends ScheduledCommandExecutor {

    private ProposalWithContext selectedProposal;

    @Override
    protected void execute() {
      Preconditions.checkNotNull(selectedProposal);
      reallyFinishAutocompletion(selectedProposal);
      selectedProposal = null;
    }

    public void scheduleAutocompletion(ProposalWithContext selectedProposal) {
      Preconditions.checkNotNull(selectedProposal);
      this.selectedProposal = selectedProposal;
      scheduleDeferred();
    }
  }

  private final Editor editor;
  private boolean isAutocompleteInsertion = false;
  private AutocompleteController autocompleteController;
  private final AutocompleteBox popup;

  /**
   * Refreshes autocomplete popup contents (if it is displayed).
   *
   * <p>This method should be called when the code is modified.
   */
  public void refresh() {
    if (autocompleteController == null) {
      return;
    }

    if (isAutocompleteInsertion) {
      return;
    }

    if (popup.isShowing()) {
      scheduleRequestAutocomplete();
    }
  }

  /**
   * Callback passed to {@link AutocompleteController}.
   */
  private final AutocompleterCallback callback = new AutocompleterCallback() {

    @Override
    public void rescheduleCompletionRequest() {
      scheduleRequestAutocomplete();
    }
  };

  private final OnSelectCommand onSelectCommand = new OnSelectCommand();

  public static Autocompleter create(
      Editor editor, CubeClient cubeClient, final AutocompleteBox popup) {
    SkipListStringBag localPrefixIndexStorage = new SkipListStringBag();
    LimitedContextFilePrefixIndex limitedContextFilePrefixIndex = new LimitedContextFilePrefixIndex(
        LOCAL_PREFIX_INDEX_LIMIT, localPrefixIndexStorage);
    CssAutocompleter cssAutocompleter = CssAutocompleter.create();
    CodeGraphAutocompleter jsAutocompleter = JsAutocompleter.create(
        cubeClient, limitedContextFilePrefixIndex);
    HtmlAutocompleter htmlAutocompleter = HtmlAutocompleter.create(
        cssAutocompleter, jsAutocompleter);
    CodeGraphAutocompleter pyAutocompleter = PyAutocompleter.create(
        cubeClient, limitedContextFilePrefixIndex);
    PyIndexUpdater pyIndexUpdater = new PyIndexUpdater();
    JsIndexUpdater jsIndexUpdater = new JsIndexUpdater();
    return new Autocompleter(editor, popup, localPrefixIndexStorage, htmlAutocompleter,
        cssAutocompleter, jsAutocompleter, pyAutocompleter, pyIndexUpdater, jsIndexUpdater);
  }

  @VisibleForTesting
  Autocompleter(Editor editor, final AutocompleteBox popup,
      SkipListStringBag localPrefixIndexStorage, HtmlAutocompleter htmlAutocompleter,
      CssAutocompleter cssAutocompleter, CodeGraphAutocompleter jsAutocompleter,
      CodeGraphAutocompleter pyAutocompleter, PyIndexUpdater pyIndexUpdater,
      JsIndexUpdater jsIndexUpdater) {
    this.editor = editor;
    this.localPrefixIndexStorage = localPrefixIndexStorage;
    this.pyIndexUpdater = pyIndexUpdater;
    this.jsIndexUpdater = jsIndexUpdater;
    this.localPrefixIndexUpdater = new ParsingTask(localPrefixIndexStorage);

    this.cssAutocompleter = cssAutocompleter;
    this.jsAutocompleter = jsAutocompleter;
    this.htmlAutocompleter = htmlAutocompleter;
    this.pyAutocompleter = pyAutocompleter;

    this.popup = popup;
    popup.setDelegate(new AutocompleteBox.Events() {

      @Override
      public void onSelect(ProposalWithContext proposal) {
        if (AutocompleteProposals.NO_OP == proposal) {
          return;
        }
        // This is called on UI click - so surely we want popup to disappear.
        // TODO: It's a quick-fix; uncomment when autocompletions
        //               become completer state free.
        //dismissAutocompleteBox();
        onSelectCommand.scheduleAutocompletion(proposal);
      }

      @Override
      public void onCancel() {
        dismissAutocompleteBox();
      }
    });
  }

  /**
   * Asks popup and language-specific autocompleter to process key press
   * and schedules corresponding autocompletion requests, if required.
   *
   * @return {@code true} if event shouldn't be further processed / bubbled
   */
  public boolean processKeyPress(SignalEventEssence trigger) {
    if (autocompleteController == null) {
      return false;
    }

    if (popup.isShowing() && popup.consumeKeySignal(trigger)) {
      return true;
    }

    if (isCtrlSpace(trigger)) {
      boxTrigger = trigger;
      scheduleRequestAutocomplete();
      return true;
    }

    LanguageSpecificAutocompleter autocompleter = getLanguageSpecificAutocompleter();
    ExplicitAction action =
        autocompleter.getExplicitAction(editor.getSelection(), trigger, popup.isShowing());

    switch (action.getType()) {
      case EXPLICIT_COMPLETE:
        boxTrigger = null;
        performExplicitCompletion(action.getExplicitAutocompletion());
        return true;

      case DEFERRED_COMPLETE:
        boxTrigger = trigger;
        scheduleRequestAutocomplete();
        return false;

      case CLOSE_POPUP:
        dismissAutocompleteBox();
        return false;

      default:
        return false;
    }
  }

  private static boolean isCtrlSpace(SignalEventEssence trigger) {
    return trigger.ctrlKey && (trigger.keyCode == ' ') && (trigger.type == KeySignalType.INPUT);
  }

  /**
   * Hides popup and prevents further activity.
   */
  private void stop() {
    dismissAutocompleteBox();
    if (this.autocompleteController != null) {
      this.autocompleteController.detach();
      this.autocompleteController = null;
    }
    localPrefixIndexStorage.clear();
  }

  /**
   * Setups for the document to be auto-completed.
   */
  public void reset(PathUtil filePath, DocumentParser parser) {
    Preconditions.checkNotNull(filePath);
    Preconditions.checkNotNull(parser);

    stop();

    LanguageSpecificAutocompleter autocompleter = getAutocompleter(parser.getSyntaxType());
    this.autocompleteController = new AutocompleteController(autocompleter, callback);
    autocompleter.attach(parser, autocompleteController, filePath);
  }

  @VisibleForTesting
  protected LanguageSpecificAutocompleter getLanguageSpecificAutocompleter() {
    Preconditions.checkNotNull(autocompleteController);
    return autocompleteController.getLanguageSpecificAutocompleter();
  }

  @VisibleForTesting
  AutocompleteController getController() {
    return autocompleteController;
  }

  @VisibleForTesting
  SyntaxType getMode() {
    return (autocompleteController == null)
        ? SyntaxType.NONE : autocompleteController.getLanguageSpecificAutocompleter().getMode();
  }

  /**
   * Applies textual and UI changes specified with {@link AutocompleteResult}.
   */
  @SuppressWarnings("incomplete-switch")
  private void applyChanges(AutocompleteResult result) {
    switch (result.getPopupAction()) {
      case CLOSE:
        dismissAutocompleteBox();
        break;

      case OPEN:
        scheduleRequestAutocomplete();
        break;
    }

    isAutocompleteInsertion = true;
    try {
      result.apply(editor);
    } finally {
      isAutocompleteInsertion = false;
    }
  }

  /**
   * Fetch changes from controller for selected proposal, hide popup;
   * apply changes.
   *
   * @param proposal proposal item selected by user
   */
  @VisibleForTesting
  void reallyFinishAutocompletion(ProposalWithContext proposal) {
    applyChanges(autocompleteController.finish(proposal));
  }

  /**
   * Dismisses the autocomplete box.
   *
   * <p>This is called when the user hits escape or types until
   * there are no more autocompletions or navigates away
   * from the autocompletion box position.
   */
  public void dismissAutocompleteBox() {
    popup.dismiss();
    boxTrigger = null;
    if (autocompleteController != null) {
      autocompleteController.pause();
    }
  }

  /**
   * Schedules an asynchronous call to compute and display / perform
   * appropriate autocompletion proposals.
   */
  private void scheduleRequestAutocomplete() {
    final SignalEventEssence trigger = boxTrigger;
    final AutocompleteController controller = autocompleteController;
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        requestAutocomplete(controller, trigger);
      }
    });
  }

  private void performExplicitCompletion(AutocompleteResult completion) {
    Preconditions.checkState(!isAutocompleteInsertion);
    applyChanges(completion);
  }

  @VisibleForTesting
  void requestAutocomplete(AutocompleteController controller, SignalEventEssence trigger) {
    if (!controller.isAttached()) {
      return;
    }
    // TODO: If there is only one proposal that gives us nothing
    //               then there are no proposals!
    AutocompleteProposals proposals = controller.start(editor.getSelection(), trigger);
    if (AutocompleteProposals.PARSING == proposals && popup.isShowing()) {
      // Do nothing to avoid flickering.
    } else if (!proposals.isEmpty()) {
      popup.positionAndShow(proposals);
    } else {
      dismissAutocompleteBox();
    }
  }

  @VisibleForTesting
  protected LanguageSpecificAutocompleter getAutocompleter(SyntaxType mode) {
    switch (mode) {
      case HTML:
        return htmlAutocompleter;
      case JS:
        return jsAutocompleter;
      case CSS:
        return cssAutocompleter;
      case PY:
        return pyAutocompleter;
      default:
        return NoneAutocompleter.getInstance();
    }
  }

  public void cleanup() {
    stop();
    jsAutocompleter.cleanup();
    pyAutocompleter.cleanup();
  }

  public CodeAnalyzer getCodeAnalyzer() {
    return distributingCodeAnalyzer;
  }

  /**
   * Refreshes proposals list after cursor has been processed by parser.
   */
  public void onCursorLineParsed() {
    refresh();
  }


  public void onDocumentParsingFinished() {
    refresh();
  }
}
