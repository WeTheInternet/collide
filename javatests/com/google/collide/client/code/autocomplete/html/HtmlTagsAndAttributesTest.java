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
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.util.collections.SimpleStringBag;
import com.google.collide.json.shared.JsonArray;

/**
 * Test cases for {@link HtmlTagsAndAttributes}.
 *
 */
public class HtmlTagsAndAttributesTest extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  /**
   * Tests that used attributes are excluded.
   */
  public void testExclusion() {
    HtmlTagsAndAttributes htmlAttributes = HtmlTagsAndAttributes.getInstance();
    SimpleStringBag excluded = new SimpleStringBag();

    JsonArray<AutocompleteProposal> all = htmlAttributes
        .searchAttributes("html", excluded, "");
    assertNotNull(all);
    assertTrue(all.size() > 3);

    excluded.add(all.get(2).getName());

    JsonArray<AutocompleteProposal> allButOne = htmlAttributes
        .searchAttributes("html", excluded, "");
    assertNotNull(allButOne);

    assertTrue(all.size() == allButOne.size() + 1);
  }

  /**
   * Tests that attribute names are (prefix-)filtered.
   */
  public void testFiltering() {
    HtmlTagsAndAttributes htmlAttributes = HtmlTagsAndAttributes.getInstance();
    SimpleStringBag excluded = new SimpleStringBag();

    JsonArray<AutocompleteProposal> all = htmlAttributes
        .searchAttributes("html", excluded, "");
    assertNotNull(all);

    JsonArray<AutocompleteProposal> oneLetterFiltered = htmlAttributes
        .searchAttributes("html", excluded, "i");
    assertNotNull(oneLetterFiltered);

    JsonArray<AutocompleteProposal> twoLettersFiltered = htmlAttributes
        .searchAttributes("html", excluded, "it");
    assertNotNull(twoLettersFiltered);

    assertTrue(all.size() > oneLetterFiltered.size());
    for (AutocompleteProposal proposal : oneLetterFiltered.asIterable()) {
      assertTrue(proposal.getName().startsWith("i"));
    }

    assertTrue(oneLetterFiltered.size() > twoLettersFiltered.size());
    for (AutocompleteProposal proposal : twoLettersFiltered.asIterable()) {
      assertTrue(proposal.getName().startsWith("it"));
    }
  }
}
