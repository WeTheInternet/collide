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

package com.google.collide.client.code.autocomplete.codegraph;

import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Test cases for {@link ExplicitAutocompleter}.
 */
public class ExplicitAutocompleterTest extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testCalculateClosingParens() {
    JsonArray<Token> tokens = JsonCollections.createArray();
    tokens.add(new Token("", TokenType.NULL, "{"));
    tokens.add(new Token("", TokenType.NULL, "foo"));
    tokens.add(new Token("", TokenType.NULL, "["));
    tokens.add(new Token("", TokenType.NULL, "]"));
    tokens.add(new Token("", TokenType.WHITESPACE, " "));
    tokens.add(new Token("", TokenType.NULL, ")"));
    tokens.add(new Token("", TokenType.NULL, "}"));
    tokens.add(new Token("", TokenType.NULL, "bar"));
    tokens.add(new Token("", TokenType.NULL, "]"));
    tokens.add(new Token("", TokenType.NULL, "bar"));

    //{foo[] )}bar]bar
    //0123456789

    assertEquals("", ExplicitAutocompleter.calculateClosingParens(tokens, 4));
    assertEquals("])}", ExplicitAutocompleter.calculateClosingParens(tokens, 5));
    assertEquals(")}", ExplicitAutocompleter.calculateClosingParens(tokens, 6));
  }

  public void testCalculateOpenParens() {
    JsonArray<Token> tokens = JsonCollections.createArray();
    tokens.add(new Token("", TokenType.NULL, "{"));
    tokens.add(new Token("", TokenType.NULL, "foo"));
    tokens.add(new Token("", TokenType.NULL, "["));
    tokens.add(new Token("", TokenType.NULL, "moo"));
    tokens.add(new Token("", TokenType.NULL, "("));
    tokens.add(new Token("", TokenType.WHITESPACE, " "));
    tokens.add(new Token("", TokenType.NULL, ")"));
    tokens.add(new Token("", TokenType.WHITESPACE, " "));
    tokens.add(new Token("", TokenType.NULL, "{"));
    tokens.add(new Token("", TokenType.NULL, "]"));
    tokens.add(new Token("", TokenType.WHITESPACE, " "));
    tokens.add(new Token("", TokenType.NULL, ")"));
    tokens.add(new Token("", TokenType.NULL, "("));

    //{foo[moo( ) {] ](
    //012345678901234567

    assertEquals("", ExplicitAutocompleter.calculateOpenParens(tokens, 0));
    assertEquals("}", ExplicitAutocompleter.calculateOpenParens(tokens, 1));
    assertEquals("}", ExplicitAutocompleter.calculateOpenParens(tokens, 2));
    assertEquals("]}", ExplicitAutocompleter.calculateOpenParens(tokens, 5));
    assertEquals(")]}", ExplicitAutocompleter.calculateOpenParens(tokens, 9));
    assertEquals("]}", ExplicitAutocompleter.calculateOpenParens(tokens, 11));
    assertEquals("}]}", ExplicitAutocompleter.calculateOpenParens(tokens, 13));
    assertEquals("", ExplicitAutocompleter.calculateOpenParens(tokens, 14));
    assertEquals("", ExplicitAutocompleter.calculateOpenParens(tokens, 16));
    assertEquals(")", ExplicitAutocompleter.calculateOpenParens(tokens, 17));
  }
}
