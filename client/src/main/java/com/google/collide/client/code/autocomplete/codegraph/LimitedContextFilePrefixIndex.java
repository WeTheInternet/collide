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

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.util.collections.SkipListStringBag;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Object that holds / searches IDs.
 *
 */
public class LimitedContextFilePrefixIndex {

  private final SkipListStringBag items;

  private final int limit;

  public LimitedContextFilePrefixIndex(int limit, SkipListStringBag items) {
    Preconditions.checkNotNull(items);
    this.limit = limit;
    this.items = items;
  }

  public AutocompleteProposals search(SyntaxType syntaxType, String prefix) {
    final JsonArray<AutocompleteProposal> result = JsonCollections.createArray();

    boolean hasMore = false;
    for (String id : items.search(prefix)) {
      if (!id.startsWith(prefix)) {
        break;
      }
      if (result.size() == limit) {
        hasMore = true;
        break;
      }
      result.add(new CodeGraphProposal(id));
    }
    String hint = hasMore ? "First " + limit + " possible completions are shown." : null;
    return new AutocompleteProposals(syntaxType, prefix, result, hint);
  }
}
