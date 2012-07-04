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

package com.google.collide.client.code.lang;

import com.google.collide.codemirror2.SyntaxType;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test cases for {@link LanguageHelperResolver}.
 *
 */
public class LanguageHelperResolverTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testAllSyntaxTypesResolvable() {
    SyntaxType[] types = SyntaxType.values();
    for (int i = 0, l = types.length; i < l; i++) {
      SyntaxType syntaxType = types[i];
      try {
        LanguageHelperResolver.getHelper(syntaxType);
      } catch (Exception ex) {
        fail("Can't obtain helper for " + syntaxType);
      }
    }
  }
}
