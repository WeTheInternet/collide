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

package com.google.collide.client.code.autocomplete.css;

import com.google.collide.client.code.autocomplete.AbstractTrie;
import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Holder of all possible CSS attributes.
 */
public class CssTrie {
  private static final JsonArray<String> ELEMENTS = JsoArray.from("azimuth",
      "background",
      "background-attachment",
      "background-color",
      "background-image",
      "background-position",
      "background-repeat",
      "border",
      "border-bottom",
      "border-bottom-color",
      "border-bottom-style",
      "border-bottom-width",
      "border-collapse",
      "border-color",
      "border-left",
      "border-left-color",
      "border-left-style",
      "border-left-width",
      "border-right",
      "border-right-color",
      "border-right-style",
      "border-right-width",
      "border-spacing",
      "border-style",
      "border-top",
      "border-top-color",
      "border-top-style",
      "border-top-width",
      "border-width",
      "bottom",
      "caption-side",
      "clear",
      "clip",
      "color",
      "content",
      "counter-increment",
      "counter-reset",
      "cue",
      "cue-after",
      "cue-before",
      "cursor",
      "direction",
      "display",
      "elevation",
      "empty-cells",
      "float",
      "font",
      "font-family",
      "font-size",
      "font-style",
      "font-variant",
      "font-weight",
      "height",
      "left",
      "letter-spacing",
      "line-height",
      "list-style",
      "list-style-image",
      "list-style-position",
      "list-style-type",
      "margin",
      "margin-bottom",
      "margin-left",
      "margin-right",
      "margin-top",
      "max-height",
      "max-width",
      "min-height",
      "min-width",
      "orphans",
      "outline",
      "outline-color",
      "outline-style",
      "outline-width",
      "overflow",
      "padding",
      "padding-bottom",
      "padding-left",
      "padding-right",
      "padding-top",
      "page-break-after",
      "page-break-before",
      "page-break-inside",
      "pause",
      "pause-after",
      "pause-before",
      "pitch",
      "pitch-range",
      "play-during",
      "position",
      "quotes",
      "richness",
      "right",
      "speak",
      "speak-header",
      "speak-numeral",
      "speak-punctuation",
      "speech-rate",
      "stress",
      "table-layout",
      "text-align",
      "text-decoration",
      "text-indent",
      "text-transform",
      "top",
      "unicode-bidi",
      "vertical-align",
      "visibility",
      "voice-family",
      "volume",
      "white-space",
      "widows",
      "width",
      "word-spacing",
      "z-index");

  public static AbstractTrie<AutocompleteProposal> createTrie() {
    AbstractTrie<AutocompleteProposal> result = new AbstractTrie<AutocompleteProposal>();
    for (String name : ELEMENTS.asIterable()) {
      result.put(name, new AutocompleteProposal(name));
    }
    return result;
  }

  /**
   * Finds all autocompletions and filters them based on a) the prefix that the
   * user has already typed, and b) the properties that are already present.
   *
   * @param prefix the prefix of the property that the user has already typed
   * @param completedProperties the properties that are already in the document
   *        for the given property
   * @return an array of autocompletions, or an empty array if there are no
   *         autocompletion proposals
   */
  public static JsonArray<AutocompleteProposal> findAndFilterAutocompletions(
      AbstractTrie<AutocompleteProposal> trie, String prefix,
      JsonArray<String> completedProperties) {
    prefix = prefix.toLowerCase();
    JsonArray<AutocompleteProposal> allProposals = trie.search(prefix);
    JsonArray<AutocompleteProposal> result = JsonCollections.createArray();
    for (AutocompleteProposal proposal : allProposals.asIterable()) {
      if (!completedProperties.contains(proposal.getName())) {
        result.add(proposal);
      }
    }
    return result;
  }
}
