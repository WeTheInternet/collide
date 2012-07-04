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

package com.google.collide.client.code.autocomplete.integration;

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.integration.AutocompleteUiController.Resources;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.core.client.GWT;

/**
 * Tests for AutocompleteComponent.
 */
public class AutocompleteUiControllerTest extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return
        "com.google.collide.client.TestCode";
  }

  public void testSetItems() {
    Editor editor = Editor.create(new MockAppContext());
    editor.setDocument(Document.createFromString(""));
    AutocompleteUiController box = new AutocompleteUiController(
        editor, (Resources) GWT.create(Resources.class));
    JsonArray<AutocompleteProposal> items = JsonCollections.createArray();
    items.add(new AutocompleteProposal("First"));
    items.add(new AutocompleteProposal("Second"));
    items.add(new AutocompleteProposal("Third"));
    AutocompleteProposals proposals = new AutocompleteProposals(SyntaxType.NONE, "", items);
    box.positionAndShow(proposals);
    assertEquals(3, box.getList().size());
  }
}
