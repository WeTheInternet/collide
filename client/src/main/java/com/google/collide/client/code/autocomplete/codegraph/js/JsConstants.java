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

import com.google.collide.client.code.autocomplete.AbstractTrie;
import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.TemplateProposal;

/**
 * Singleton that holds various JS-specific constants.
 */
class JsConstants {

  private static JsConstants instance;

  /**
   * @return the singleton instance of this class.
   */
  static JsConstants getInstance() {
    if (instance == null) {
      instance = new JsConstants();
    }
    return instance;
  }

  private static void addProposal(AbstractTrie<TemplateProposal> to, String name, String template) {
    to.put(name, new TemplateProposal(name, template));
  }

  private final PrefixIndex<TemplateProposal> templatesTrie;

  private JsConstants() {
    AbstractTrie<TemplateProposal> temp = new AbstractTrie<TemplateProposal>();

    addProposal(temp, "break", "break");
    addProposal(temp, "case", "case %c:");
    addProposal(temp, "continue", "continue");
    addProposal(temp, "default", "default: ");
    addProposal(temp, "delete", "delete ");
    addProposal(temp, "do", "do {%i%c%n} while ()");
    addProposal(temp, "else", "else {%n%c%n}");
    addProposal(temp, "export", "export ");
    addProposal(temp, "for", "for (%c;;) {%i%n}");
    addProposal(temp, "function", "function %c() {%i%n}");
    addProposal(temp, "if", "if (%c) {%i%n}");
    addProposal(temp, "import", "import ");
    addProposal(temp, "in", "in ");
    addProposal(temp, "label", "label ");
    addProposal(temp, "new", "new ");
    addProposal(temp, "return", "return");
    addProposal(temp, "switch", "switch (%c) {%i%n}");
    addProposal(temp, "this", "this");
    addProposal(temp, "typeof", "typeof");
    addProposal(temp, "var", "var ");
    addProposal(temp, "void", "void ");
    addProposal(temp, "while", "while (%c) {%i%n}");
    addProposal(temp, "with", "with ");

    templatesTrie = temp;
  }

  PrefixIndex<TemplateProposal> getTemplatesTrie() {
    return templatesTrie;
  }
}
