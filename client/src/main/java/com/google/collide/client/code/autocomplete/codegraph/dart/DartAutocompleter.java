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

import com.google.collide.client.code.autocomplete.codegraph.CodeGraphAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.ExplicitAutocompleter;
import com.google.collide.client.code.autocomplete.codegraph.LimitedContextFilePrefixIndex;
import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.codemirror2.SyntaxType;

/**
 * Dart implementation for {@link CodeGraphAutocompleter}
 */
public class DartAutocompleter {

  /**
   * Dart Autocompleter.
   *
   * @return configured instance of Dart autocompleter
   */
  public static CodeGraphAutocompleter create(
      CubeClient cubeClient, LimitedContextFilePrefixIndex contextFilePrefixIndex) {
    return new CodeGraphAutocompleter(SyntaxType.DART, new DartProposalBuilder(), cubeClient,
        contextFilePrefixIndex, new ExplicitAutocompleter());
  }
}
