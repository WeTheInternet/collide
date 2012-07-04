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

import com.google.collide.json.shared.JsonArray;

/**
 * Token factory for HTML. Depending on the mode it creates either {@link Token}
 * or {@link CssToken}.
 *
 * <p>By creating {@link CssToken} we capture the context which is part of the
 * {@link CssState}.
 */
class HtmlTokenFactory implements TokenFactory<HtmlState> {

  @Override
  public void push(String stylePrefix, HtmlState htmlState, String tokenType, String tokenValue,
      JsonArray<Token> tokens) {
    Token token;
    if (CodeMirror2.CSS.equals(stylePrefix)) {
      CssState cssState = htmlState.getCssState();
      token = CssTokenFactory.createToken(stylePrefix, cssState, tokenType, tokenValue);
    } else {
      token =
          new Token(stylePrefix, TokenType.resolveTokenType(tokenType, tokenValue), tokenValue);
    }
    tokens.add(token);
  }
}
