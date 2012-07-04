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

import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Object that holds a set of proposals produced by
 * {@link LanguageSpecificAutocompleter}.
 *
 * <p>In this object proposals are kept together with object that allows
 * construction of {@link AutocompleteResult} (some kind of context).
 *
 */
public class AutocompleteProposals {

  /**
   * Class that holds common information for the set of proposals.
   */
  public static class Context {

    private final String triggeringString;
    private final int indent;

    public Context(String triggeringString) {
      this(triggeringString, 0);
    }

    public Context(String triggeringString, int indent) {
      this.triggeringString = triggeringString;
      this.indent = indent;
    }

    public String getTriggeringString() {
      return triggeringString;
    }

    public int getIndent() {
      return indent;
    }
  }

  /**
   * Immutable bean that holds both selected item and context object for
   * computing autocompletion.
   */
  public static class ProposalWithContext {

    private final SyntaxType syntaxType;
    private final AutocompleteProposal proposal;
    private final Context context;

    public ProposalWithContext(SyntaxType syntaxType,
        AutocompleteProposal proposal, Context context) {
      this.syntaxType = syntaxType;
      this.proposal = proposal;
      this.context = context;
    }

    public SyntaxType getSyntaxType() {
      return syntaxType;
    }

    public AutocompleteProposal getItem() {
      return proposal;
    }

    public Context getContext() {
      return context;
    }
  }

  public static final AutocompleteProposals EMPTY = new AutocompleteProposals("");
  public static final AutocompleteProposals PARSING;
  public static final ProposalWithContext NO_OP =
      // Using SyntaxType.NONE because for no-op the mode does not matter.
      new ProposalWithContext(SyntaxType.NONE, null, null);

  static {
    JsonArray<AutocompleteProposal> pleaseWaitItems = JsonCollections.createArray();
    pleaseWaitItems.add(new AutocompleteProposal("...parsing..."));
    // Using SyntaxType.NONE because for parsing proposals the mode does not
    // matter.
    PARSING = new AutocompleteProposals(SyntaxType.NONE, "", pleaseWaitItems) {
      @Override
      public ProposalWithContext select(AutocompleteProposal proposal) {
        return NO_OP;
      }
    };
  }

  private SyntaxType syntaxType;

  protected final Context context;

  private final String hint;

  protected final JsonArray<AutocompleteProposal> items;

  private AutocompleteProposals(String context) {
    // Using SyntaxType.NONE because for empty proposals the mode does not
    // matter.
    this(SyntaxType.NONE, context, JsonCollections.<AutocompleteProposal>createArray());
  }

  public AutocompleteProposals(SyntaxType syntaxType, String triggeringString,
      JsonArray<AutocompleteProposal> items) {
    this(syntaxType, triggeringString, items, null);
  }

  public AutocompleteProposals(SyntaxType syntaxType, String triggeringString,
      JsonArray<AutocompleteProposal> items, String hint) {
    this(syntaxType, new Context(triggeringString), items, hint);
  }

  /**
   * Constructs proposals object.
   *
   * <p>Side effect: items in the given array are reordered.
   */
  public AutocompleteProposals(SyntaxType syntaxType, Context context,
      JsonArray<AutocompleteProposal> items, String hint) {
    Preconditions.checkNotNull(context);
    Preconditions.checkNotNull(items);
    this.syntaxType = syntaxType;
    this.context = context;
    this.items = items;
    this.hint = hint;
    items.sort(AutocompleteProposal.LEXICOGRAPHICAL_COMPARATOR);
  }

  public AutocompleteProposal get(int index) {
    return items.get(index);
  }

  public int size() {
    return items.size();
  }

  /**
   * Returns the autocomplete proposals as a JsonArray.
   *
   * Note: The returned array should not be modified; it is the same instance as
   * the internal array.
   */
  public JsonArray<AutocompleteProposal> getItems() {
    return items;
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public final ProposalWithContext select(int index) {
    return select(items.get(index));
  }

  public ProposalWithContext select(AutocompleteProposal proposal) {
    Preconditions.checkState(items.contains(proposal));
    return new ProposalWithContext(syntaxType, proposal, context);
  }

  public String getHint() {
    return hint;
  }

  public SyntaxType getSyntaxType() {
    return syntaxType;
  }
}
