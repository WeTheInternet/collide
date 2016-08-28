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
 * Token factory for CSS; uses information from {@link CssState}.
 */
public class CssTokenFactory implements TokenFactory<CssState> {

  @Override
  public void push(String stylePrefix, CssState state, String tokenType, String tokenValue,
      JsonArray<Token> tokens) {
    tokens.add(createToken(stylePrefix, state, tokenType, tokenValue));
  }

  /**
   * Create new {@link CssToken} for the given parameters.
   *
   * <p>This method is shared with {@link HtmlTokenFactory} in order to create
   * tokens of class {@link CssToken} for internal style sheets.
   *
   * @param stylePrefix a.k.a. mode, in this case expected to be "css"
   * @param state CSS parser state at the time when the token was parsed
   * @param tokenType token type
   * @param tokenValue token value
   * @return newly created {@link CssToken}.
   */
  static CssToken createToken(String stylePrefix, CssState state, String tokenType,
      String tokenValue) {
    JsonArray<String> stack = state.getStack();
    String context = (stack != null && stack.size() > 0) ? stack.peek() : null;
    return new CssToken(stylePrefix, TokenType.resolveTokenType(tokenType, tokenValue), tokenValue,
        context);
  }
}
