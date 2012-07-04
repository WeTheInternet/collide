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

import com.google.collide.client.code.autocomplete.codegraph.CompletionContext;
import com.google.collide.client.code.autocomplete.codegraph.CompletionType;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.codemirror2.JsState;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;

// TODO: We need to add some CUBE+client tests for typical cases.
/**
 * Test cases for JS specific things performed by {@link JsProposalBuilder}.
 */
public class JsProposalBuilderTest extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testGlobalShortcuts() {
    JsProposalBuilder jsProposalBuilder = new JsProposalBuilder();
    CompletionContext<JsState> context = new CompletionContext<JsState>(
        "", "con", false, CompletionType.GLOBAL, null, 0);
    JsonStringSet prefixes = JsonCollections.createStringSet();
    prefixes.add("");
    prefixes.add("Tofu.");
    jsProposalBuilder.addShortcutsTo(context, prefixes);
    assertEquals(JsonCollections.createStringSet("", "Tofu.", "window."), prefixes);
  }

  public void testPropertyShortcuts() {
    JsProposalBuilder jsProposalBuilder = new JsProposalBuilder();
    CompletionContext<JsState> context = new CompletionContext<JsState>(
        "console.", "deb", false, CompletionType.PROPERTY, null, 0);
    JsonStringSet prefixes = JsonCollections.createStringSet();
    prefixes.add("console.");
    prefixes.add("Tofu.console.");
    jsProposalBuilder.addShortcutsTo(context, prefixes);
    assertEquals(JsonCollections.createStringSet("console.", "Tofu.console.", "window.console."),
        prefixes);
  }

  public void testThisShortcuts() {
    JsProposalBuilder jsProposalBuilder = new JsProposalBuilder();
    CompletionContext<JsState> context = new CompletionContext<JsState>(
        "this.console.", "deb", true, CompletionType.PROPERTY, null, 0);
    JsonStringSet prefixes = JsonCollections.createStringSet();
    prefixes.add("Tofu.console.");
    jsProposalBuilder.addShortcutsTo(context, prefixes);
    assertEquals(JsonCollections.createStringSet("Tofu.console."), prefixes);
  }
}
