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

package com.google.collide.client.code.autocomplete.html;

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.json.shared.JsonArray;
import com.google.common.base.Preconditions;

/**
 * Html-specific implementation.
 */
public class HtmlAutocompleteProposals extends AutocompleteProposals {

  static class HtmlProposalWithContext extends ProposalWithContext {

    private final CompletionType type;

    public HtmlProposalWithContext(
        AutocompleteProposal proposal, Context context, CompletionType type) {
      super(SyntaxType.HTML, proposal, context);
      this.type = type;
    }

    public CompletionType getType() {
      return type;
    }
  }

  private final CompletionType type;

  public HtmlAutocompleteProposals(
      String context, JsonArray<AutocompleteProposal> items, CompletionType type) {
    super(SyntaxType.HTML, context, items);
    this.type = type;
  }

  @Override
  public ProposalWithContext select(AutocompleteProposal proposal) {
    Preconditions.checkState(items.contains(proposal));
    return new HtmlProposalWithContext(proposal, context, type);
  }
}
