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

package com.google.collide.client.code.autocomplete.codegraph.dart;

import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.CompletionContext;
import com.google.collide.client.code.autocomplete.codegraph.ProposalBuilder;
import com.google.collide.client.code.autocomplete.codegraph.TemplateProposal;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.codemirror2.DartState;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;

/**
 * Dart implementation of {@link ProposalBuilder}.
 */
class DartProposalBuilder extends ProposalBuilder<DartState> {

  DartProposalBuilder() {
    super(DartState.class);
  }

  @Override
  protected PrefixIndex<TemplateProposal> getTemplatesIndex() {
    return DartConstants.getInstance().getSnippetsTemplateTrie();
  }

  @Override
  protected boolean checkIsThisPrefix(String prefix) {
    return "this.".equals(prefix);
  }

  @Override
  protected void addShortcutsTo(CompletionContext<DartState> context, JsonStringSet prefixes) {
  }

  @Override
  protected JsonArray<String> getLocalVariables(ParseResult<DartState> parseResult) {
    // TODO : implement.
    return JsonCollections.createArray();
  }
}
