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

package com.google.collide.client.code.autocomplete.codegraph.js;

import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.CompletionContext;
import com.google.collide.client.code.autocomplete.codegraph.ProposalBuilder;
import com.google.collide.client.code.autocomplete.codegraph.TemplateProposal;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.codemirror2.JsLocalVariable;
import com.google.collide.codemirror2.JsState;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;

/**
 * JS specific {@link ProposalBuilder} implementation.
 *
 */
class JsProposalBuilder extends ProposalBuilder<JsState> {

  static final String WINDOW_PREFIX = "window.";

  JsProposalBuilder() {
    super(JsState.class);
  }

  @Override
  protected PrefixIndex<TemplateProposal> getTemplatesIndex() {
    return JsConstants.getInstance().getTemplatesTrie();
  }

  @Override
  protected void addShortcutsTo(CompletionContext<JsState> context, JsonStringSet prefixes) {
    if (!context.isThisContext()) {
      String previousContext = context.getPreviousContext();
      if (!previousContext.startsWith(WINDOW_PREFIX)) {
        prefixes.add(WINDOW_PREFIX + previousContext);
      }
    }
  }

  @Override
  protected boolean checkIsThisPrefix(String prefix) {
    return "this.".equals(prefix);
  }

  protected JsonArray<String> getLocalVariables(ParseResult<JsState> parseResult) {
    if (parseResult == null) {
      return JsonCollections.createArray();
    }

    JsonArray<String> result = JsonCollections.createArray();
    JsState jsState = parseResult.getState();
    JsLocalVariable localVariable = jsState.getLocalVariables();
    while (localVariable != null) {
      result.add(localVariable.getName());
      localVariable = localVariable.getNext();
    }
    return result;
  }
}
