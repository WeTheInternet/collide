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

import com.google.collide.client.code.autocomplete.AutocompleteController;
import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.SyntaxType;
import com.google.common.base.Preconditions;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Implements autocompleter for abstract language statements.
 */
public class CodeGraphAutocompleter extends LanguageSpecificAutocompleter {

  private static final RegExp ID_REGEXP = RegExp.compile("[a-zA-Z\\$_][a-zA-Z\\$_0-9]*$");

  private ScopeTrieBuilder scopeTrieBuilder;

  private final CodeGraphSource codeGraphSource;

  private final ProposalBuilder proposalBuilder;

  private final ExplicitAutocompleter explicitAutocompleter;

  private final LimitedContextFilePrefixIndex contextFilePrefixIndex;

  private final Runnable codeGraphUpdateListener = new Runnable() {
    @Override
    public void run() {
      scopeTrieBuilder.setCodeGraph(codeGraphSource.constructCodeGraph());
      scheduleRequestForUpdatedProposals();
    }
  };

  public CodeGraphAutocompleter(SyntaxType mode, ProposalBuilder proposalBuilder,
      CubeClient cubeClient, LimitedContextFilePrefixIndex contextFilePrefixIndex,
      ExplicitAutocompleter explicitAutocompleter) {
    super(mode);

    this.explicitAutocompleter = explicitAutocompleter;
    this.proposalBuilder = proposalBuilder;
    this.contextFilePrefixIndex = contextFilePrefixIndex;

    this.codeGraphSource = new CodeGraphSource(cubeClient, codeGraphUpdateListener);
  }

  @Override
  public AutocompleteResult computeAutocompletionResult(ProposalWithContext selected) {
    AutocompleteProposals.Context context = selected.getContext();
    String triggeringString = context.getTriggeringString();
    AutocompleteProposal proposal = selected.getItem();

    if (proposal instanceof TemplateProposal) {
      TemplateProposal templateProposal = (TemplateProposal) proposal;
      return templateProposal.buildResult(triggeringString, context.getIndent());
    }

    Preconditions.checkArgument(proposal instanceof CodeGraphProposal);
    CodeGraphProposal selectedProposal = (CodeGraphProposal) proposal;

    return proposalBuilder.computeAutocompletionResult(selectedProposal, triggeringString);
  }

  /**
   * Finds autocompletions for a given completion query.
   *
   * <p>This method is triggered when:<ul>
   *   <li>popup is hidden and user press ctrl-space (event consumed)
   *   <li><b>or</b> popup is hidden and user press "." (dot applied)
   *   <li><b>or</b> popup is shown
   * </ul>
   */
  @Override
  public AutocompleteProposals findAutocompletions(
      SelectionModel selection, SignalEventEssence trigger) {
    Preconditions.checkNotNull(scopeTrieBuilder);

    if (selection.hasSelection()) {
      // Do not autocomplete JS/PY when something is selected.
      return AutocompleteProposals.EMPTY;
    }

    if (trigger == null || trigger.altKey || !trigger.shiftKey) {
      return proposalBuilder.getProposals(getMode(), getParser(), selection, scopeTrieBuilder);
    }
    return contextFilePrefixIndex.search(getMode(),
        calculateTriggeringString(selection));
  }

  private static String calculateTriggeringString(SelectionModel selection) {
    String cursorLine = selection.getCursorLine().getText();
    int cursorColumn = selection.getCursorColumn();
    MatchResult matchResult = ID_REGEXP.exec(cursorLine.substring(0, cursorColumn));
    if (matchResult == null) {
      return "";
    }
    return matchResult.getGroup(0);
  }

  @Override
  protected void pause() {
    super.pause();
    codeGraphSource.setPaused(true);
  }

  @Override
  protected void start() {
    super.start();
    codeGraphSource.setPaused(false);

    Preconditions.checkNotNull(scopeTrieBuilder);
    if (codeGraphSource.hasUpdate()) {
      scopeTrieBuilder.setCodeGraph(codeGraphSource.constructCodeGraph());
    }
  }

  @Override
  public ExplicitAction getExplicitAction(SelectionModel selectionModel,
      SignalEventEssence signal, boolean popupIsShown) {
    return explicitAutocompleter.getExplicitAction(
        selectionModel, signal, popupIsShown, getParser());
  }

  @Override
  public void attach(
      DocumentParser parser, AutocompleteController controller, PathUtil filePath) {
    super.attach(parser, controller, filePath);
    CodeFile contextFile = new CodeFile(filePath);
    scopeTrieBuilder = new ScopeTrieBuilder(contextFile, getMode());
    scopeTrieBuilder.setCodeGraph(codeGraphSource.constructCodeGraph());
  }

  @Override
  public void cleanup() {
    codeGraphSource.cleanup();
  }
}
