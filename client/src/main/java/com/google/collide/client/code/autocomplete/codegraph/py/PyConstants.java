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

package com.google.collide.client.code.autocomplete.codegraph.py;

import com.google.collide.client.code.autocomplete.AbstractTrie;
import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.TemplateProposal;

/**
 * Singleton that holds various PY-specific constants.
 */
class PyConstants {

  private static PyConstants instance;

  /**
   * @return the singleton instance of this class.
   */
  static PyConstants getInstance() {
    if (instance == null) {
      instance = new PyConstants();
    }
    return instance;
  }

  private static void addProposal(AbstractTrie<TemplateProposal> to, String name, String template) {
    to.put(name, new TemplateProposal(name, template));
  }

  private final PrefixIndex<TemplateProposal> templatesTrie;

  private PyConstants() {
    AbstractTrie<TemplateProposal> temp = new AbstractTrie<TemplateProposal>();

    addProposal(temp, "and", "and ");
    addProposal(temp, "assert", "assert ");
    addProposal(temp, "break", "break%d");
    addProposal(temp, "class", "class %c:%i\"\"");
    addProposal(temp, "continue", "continue%d");
    addProposal(temp, "def", "def ");
    addProposal(temp, "del", "del ");
    // TODO: Reindent current line.
    addProposal(temp, "elif", "elif ");
    // TODO: Reindent current line.
    addProposal(temp, "else", "else%i");
    // TODO: Reindent current line.
    addProposal(temp, "except", "except %c:%i");
    addProposal(temp, "exec", "exec ");
    // TODO: Reindent current line.
    addProposal(temp, "finally", "finally:%i");
    addProposal(temp, "for", "for %c in :");
    addProposal(temp, "from", "from %c import ");
    addProposal(temp, "global", "global ");
    addProposal(temp, "if", "if %c:");
    addProposal(temp, "import", "import ");
    addProposal(temp, "in", "in ");
    addProposal(temp, "is", "is ");
    addProposal(temp, "lambda", "lambda %c: ");
    addProposal(temp, "not", "not ");
    addProposal(temp, "or", "or ");
    addProposal(temp, "pass", "pass%d");
    addProposal(temp, "print", "print ");
    addProposal(temp, "raise", "raise ");
    addProposal(temp, "return", "return ");
    addProposal(temp, "try", "try:%i");
    addProposal(temp, "with", "with ");
    addProposal(temp, "while", "while %c:%i");
    addProposal(temp, "yield", "yield ");

    templatesTrie = temp;
  }

  PrefixIndex<TemplateProposal> getTemplatesTrie() {
    return templatesTrie;
  }
}
