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

import com.google.collide.client.code.autocomplete.codegraph.CodeGraphAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.LimitedContextFilePrefixIndex;
import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.codemirror2.SyntaxType;

/**
 * Constructor of instance of {@link CodeGraphAutocompleter}
 * configured for JavaScript autocompletion.
 */
public class JsAutocompleter {

  /**
   * Construct instance of JavaScript autocompleter.
   *
   * @return configured instance of  JavaScript autocompleter
   */
  public static CodeGraphAutocompleter create(
      CubeClient cubeClient, LimitedContextFilePrefixIndex contextFilePrefixIndex) {
    return new CodeGraphAutocompleter(
        SyntaxType.JS, new JsProposalBuilder(), cubeClient, contextFilePrefixIndex,
        new JsExplicitAutocompleter());
  }
}
