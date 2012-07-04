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

import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.common.base.Preconditions;

/**
 * Mediator that encapsulates data of the current autocomplete session and
 * provides an interface to populate autocompletion <b>asynchronously</b>.
 */
public class AutocompleteController {

  /**
   * Instance that performs all language specific logic.
   */
  private LanguageSpecificAutocompleter languageAutocompleter;

  /**
   * Callback to {@link Autocompleter} instance methods,
   */
  private AutocompleterCallback autocompleterCallback;

  public AutocompleteController(LanguageSpecificAutocompleter languageAutocompleter,
      AutocompleterCallback callback) {
    Preconditions.checkNotNull(languageAutocompleter);
    Preconditions.checkNotNull(callback);
    this.languageAutocompleter = languageAutocompleter;
    this.autocompleterCallback = callback;
  }

  /**
   * @return the language-specific autocontroller
   */
  public LanguageSpecificAutocompleter getLanguageSpecificAutocompleter() {
    return languageAutocompleter;
  }

  /**
   * Calculates the completion result for the selected proposal.
   *
   * @param proposal proposal item selected by user
   */
  AutocompleteResult finish(ProposalWithContext proposal) {
    AutocompleteResult result = languageAutocompleter.computeAutocompletionResult(proposal);
    pause();
    return result;
  }

  boolean isAttached() {
    return languageAutocompleter != null;
  }

  /**
   * This method execution is scheduled from UI when it needs to
   * show completions.
   *
   * @param trigger event that triggered autocomplete request
   *                (typically Ctrl+Space press)
   */
  AutocompleteProposals start(SelectionModel selection, SignalEventEssence trigger) {
    Preconditions.checkNotNull(languageAutocompleter);

    languageAutocompleter.start();
    return languageAutocompleter.findAutocompletions(selection, trigger);
  }

  /**
   * Stops language specific autocompleter and unbounds it from this controller.
   *
   * <p>{@link #languageAutocompleter} and {@link #autocompleterCallback}
   * are assigned {@code null} to avoid further interaction.
   */
  void detach() {
    languageAutocompleter.detach();
    languageAutocompleter = null;
    autocompleterCallback = null;
  }

  /**
   * Pauses language specific autocompleter.
   *
   * <p>This should be called after the dismission of the autocomplete box.
   */
  void pause() {
    languageAutocompleter.pause();
  }

  /**
   * Transfers request for updating proposals list.
   */
  void scheduleRequestForUpdatedProposals() {
    autocompleterCallback.rescheduleCompletionRequest();
  }
}
