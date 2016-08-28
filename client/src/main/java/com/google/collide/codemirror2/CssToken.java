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
 * Token that knows about CSS state stack.
 */
public class CssToken extends Token {

  /**
   * CSS parsing context is the value at the top of state stack.
   */
  private final String context;

  CssToken(String mode, TokenType type, String value, String context) {
    super(mode, type, value);
    this.context = context;
  }

  public String getContext() {
    return context;
  }

  @Override
  public String toString() {
    return "CssToken{"
        + "mode='" + getMode() + '\''
        + ", type=" + getType()
        + ", value='" + getValue() + '\''
        + ", context='" + context + '\''
        + '}';
  }
}
