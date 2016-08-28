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

/**
 * A representation of a single Codemirror2 token.
 *
 * <p>A token consists of a type (function, keyword, variable, etc), and a value
 * (the piece of code that is classified as a single token of the same type).
 *
 */
public class Token {

  public static final Token NEWLINE = new Token("", TokenType.NEWLINE, "\n");

  public static final String LITERAL_PERIOD = ".";

  private final String mode;
  private final TokenType type;
  private final String value;

  public Token(String mode, TokenType type, String value) {
    this.mode = mode;
    this.type = type;
    this.value = value;
  }

  public TokenType getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public String getMode() {
    return mode;
  }

  /**
   * @return the type prefixed with the parsing mode it was found in
   */
  public final String getStyle() {
    return mode + "-" + type.getTypeName();
  }

  @Override
  public String toString() {
    return "Token{"
        + "mode='" + mode + '\''
        + ", type=" + type
        + ", value='" + value + '\''
        + '}';
  }
}
