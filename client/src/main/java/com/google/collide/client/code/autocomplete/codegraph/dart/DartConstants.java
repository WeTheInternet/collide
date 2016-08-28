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

import com.google.collide.client.code.autocomplete.AbstractTrie;
import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.TemplateProposal;

/**
 * Dart constants.
 */
class DartConstants {

  private static DartConstants instance;

  static DartConstants getInstance() {
    if (instance == null) {
      instance = new DartConstants();
    }
    return instance;
  }

  private final PrefixIndex<TemplateProposal> proposalTemplateTrie;

  private static void addProposal(AbstractTrie<TemplateProposal> to, String name, String template) {
    to.put(name, new TemplateProposal(name, template));
  }

  public PrefixIndex<TemplateProposal> getSnippetsTemplateTrie() {
    return proposalTemplateTrie;
  }

  private DartConstants() {
    AbstractTrie<TemplateProposal> temp = new AbstractTrie<TemplateProposal>();

    addProposal(temp, "#source", "#source(%c);");
    addProposal(temp, "#import", "#import(%c);");
    addProposal(temp, "#include", "#include(%c);");
    addProposal(temp, "#library", "#library(%c);");
    addProposal(temp, "abstract", "abstract ");
    addProposal(temp, "assert", "assert(%c);");
    addProposal(temp, "break", "break;");
    addProposal(temp, "case", "case %c:");
    addProposal(temp, "catch", "catch (%c) {%i%n}");
    addProposal(temp, "class", "class %c {%i%n}");
    addProposal(temp, "continue", "continue;");
    addProposal(temp, "const", "const ");
    addProposal(temp, "default", "default:");
    addProposal(temp, "do", "do {%i%c%n} while ()");
    addProposal(temp, "else", "else {%n%c%n}");
    addProposal(temp, "extends", "extends ");
    addProposal(temp, "final", "final ");
    addProposal(temp, "for", "for (%c;;) {%i%n}");
    addProposal(temp, "for", "for (%c in %c) {%i%n}");
    addProposal(temp, "function", "function %c() {%i%n}");
    addProposal(temp, "get", "get %c() {%i%n}");
    addProposal(temp, "if", "if (%c) {%i%n}");
    addProposal(temp, "interface", "interface ");
    addProposal(temp, "is", "is %c");
    addProposal(temp, "implements", "implements ");
    addProposal(temp, "native", "native ");
    addProposal(temp, "new", "new %c();");
    addProposal(temp, "operator", "operator ");
    addProposal(temp, "return", "return %c;");
    addProposal(temp, "set", "set %c() {%i%n}");
    addProposal(temp, "static", "static ");
    addProposal(temp, "super", "super(%c)");
    addProposal(temp, "switch", "switch (%c) {%i%n}");
    addProposal(temp, "this", "this");
    addProposal(temp, "throw", "throw ");
    addProposal(temp, "try", "try {%i%c%n}%i%ncatch() {%i%n}");
    addProposal(temp, "typedef", "typedef ");
    addProposal(temp, "var", "var %c");
    addProposal(temp, "void", "void ");
    addProposal(temp, "while", "while (%c) {%i%n}");

    this.proposalTemplateTrie = temp;
  }
}
