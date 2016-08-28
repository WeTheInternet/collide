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

package com.google.collide.codemirror2;

import static com.google.collide.codemirror2.Token.LITERAL_PERIOD;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;

/**
 * Token factory that splits some PY tokens to more canonical ones.
 *
 */
class PyTokenFactory implements TokenFactory<State> {

  private static final JsonStringSet KEYWORD_OPERATORS = JsonCollections.createStringSet(
      "and", "in", "is", "not", "or");

  @Override
  public void push(String stylePrefix, State state, String tokenType, String tokenValue,
      JsonArray<Token> tokens) {
    TokenType type = TokenType.resolveTokenType(tokenType, tokenValue);
    if (TokenType.VARIABLE == type) {
      if (tokenValue.startsWith(LITERAL_PERIOD)) {
        tokens.add(new Token(stylePrefix, TokenType.NULL, LITERAL_PERIOD));
        tokenValue = tokenValue.substring(1);
        // TODO: Also cut whitespace (after fixes in CodeMirror).
      }
    } else if (TokenType.OPERATOR == type) {
      if (KEYWORD_OPERATORS.contains(tokenValue)) {
        type = TokenType.KEYWORD;
      }
    } else if (TokenType.ERROR == type) {
      // When parser finds ":" it pushes a new context and specifies indention
      // equal to previous indention + some configured amount. If the next line
      // indention is less than specified then whitespace is marked as ERROR.
      if (StringUtils.isNullOrWhitespace(tokenValue)) {
        type = TokenType.WHITESPACE;
      }
    }
    tokens.add(new Token(stylePrefix, type, tokenValue));
  }
}
