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
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.SyntaxType;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Base class for language-specific autocompleters.
 *
 * <p>State transitions:<ol>
 * <li> {@link #LanguageSpecificAutocompleter} -> (2)
 * <li> {@link #attach} -> (3) or (8)
 * <li> {@link #getExplicitAction} -> (4) or (7) or (8)
 * <li> {@link #start} -> (5) or (8)
 * <li> {@link #findAutocompletions} -> (5) or (6) or (7) or (8)
 * <li> {@link #computeAutocompletionResult} -> (5) or (6) or (7) or (8)
 * <li> {@link #pause} -> (3) or (8)
 * <li> {@link #detach()}
 * </ol>
 */
public abstract class LanguageSpecificAutocompleter {

  /**
   * Enumeration of types of actions preformed on keypress.
   */
  public enum ExplicitActionType {
    /**
     * Default behaviour (just append char).
     */
    DEFAULT,

    /**
     * Append char and open autocompletion box.
     */
    DEFERRED_COMPLETE,

    /**
     * Do not append char, use explicit autocompletion.
     */
    EXPLICIT_COMPLETE,

    /**
     * Append char and close autocompletion box.
     */
    CLOSE_POPUP
  }

  /**
   * Bean that holds explicit action type, and optional explicit autocompletion.
   */
  public static class ExplicitAction {

    public static final ExplicitAction DEFAULT = new ExplicitAction(
        ExplicitActionType.DEFAULT, null);

    public static final ExplicitAction DEFERRED_COMPLETE = new ExplicitAction(
        ExplicitActionType.DEFERRED_COMPLETE, null);

    public static final ExplicitAction CLOSE_POPUP = new ExplicitAction(
        ExplicitActionType.CLOSE_POPUP, null);

    private final ExplicitActionType type;
    private final AutocompleteResult explicitAutocompletion;

    public ExplicitAction(AutocompleteResult explicitAutocompletion) {
      this(ExplicitActionType.EXPLICIT_COMPLETE, explicitAutocompletion);
    }

    public ExplicitAction(ExplicitActionType type, AutocompleteResult explicitAutocompletion) {
      this.type = type;
      this.explicitAutocompletion = explicitAutocompletion;
    }

    public ExplicitActionType getType() {
      return type;
    }

    public AutocompleteResult getExplicitAutocompletion() {
      return explicitAutocompletion;
    }
  }

  private AutocompleteController controller;
  private boolean isPaused;
  private final SyntaxType mode;
  private DocumentParser documentParser;

  protected LanguageSpecificAutocompleter(SyntaxType mode) {
    Preconditions.checkNotNull(mode);
    this.mode = mode;
  }

  /**
   * Computes the full autocompletion for a selected proposal.
   *
   * <p>A full autocompletion may include such things as closing tags or braces.
   * In complex substitution case the number of indexes the caret should jump
   * after the autocompletion is complete is also computed.
   *
   * @param proposal proposal selected by user
   * @return value object with information for applying changes
   */
  public abstract AutocompleteResult computeAutocompletionResult(ProposalWithContext proposal);

  /**
   * Finds autocompletions for a given completion query.
   *
   * @param selection used to obtain current cursor position and selection
   * @param trigger used to request different lists of proposals
   * @return POJO holding array of autocompletion proposals.
   */
  public abstract AutocompleteProposals findAutocompletions(
      SelectionModel selection, SignalEventEssence trigger);

  /**
   * Cleanup before instance is dismissed.
   */
  public abstract void cleanup();

  /**
   * Prepare instance for the new autocompletion life cycle.
   *
   * <p>No completions are requested yet, but the completer can prepare
   * itself (e.g. pre-fetch data).
   */
  protected void attach(
      DocumentParser parser, AutocompleteController controller, PathUtil filePath) {
    Preconditions.checkNotNull(parser);
    documentParser = parser;
    this.controller = controller;
    isPaused = true;
  }

  /**
   * Specifies the behavior of this controller on signal events (not text
   * changes).
   *
   * <p>Ctrl-space event is processed directly and not passed to this
   * method.
   *
   * <p>Key press in not applied to document yet, so be careful when analyse
   * text around cursor.
   */
  protected ExplicitAction getExplicitAction(SelectionModel selectionModel,
      SignalEventEssence signal, boolean popupIsShown) {
    return ExplicitAction.DEFAULT;
  }

  protected void start() {
    isPaused = false;
  }

  protected void pause() {
    isPaused = true;
  }

  /**
   * Indicates the end of this autocompleter lifecycle.
   *
   * <p>User switched to other file and is not willing to see any proposals
   * from this completer.
   */
  protected void detach() {
    pause();
    this.controller = null;
  }

  /**
   * Invoked by implementations to provide asynchronously obtained proposals.
   */
  protected final void scheduleRequestForUpdatedProposals() {
    if (!isPaused) {
      controller.scheduleRequestForUpdatedProposals();
    }
  }

  protected SyntaxType getMode() {
    return mode;
  }

  @Nonnull
  protected DocumentParser getParser() {
    return Preconditions.checkNotNull(documentParser);
  }
}
