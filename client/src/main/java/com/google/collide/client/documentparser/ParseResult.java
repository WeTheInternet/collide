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
package com.google.collide.client.documentparser;

import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * POJO that holds parser state and token array.
 *
 * <p>This object represents line parsing results:
 * array of tokens produced by parser and
 * parser state when parsing is finished.
 *
 * @param <T> actual {@link State} type.
 *
 */
public class ParseResult<T extends State> {

  private final JsonArray<Token> tokens;

  private final T state;

  @VisibleForTesting
  public ParseResult(JsonArray<Token> tokens, T state) {
    Preconditions.checkNotNull(tokens, "tokens");
    Preconditions.checkNotNull(state, "state");
    this.tokens = tokens;
    this.state = state;
  }

  public JsonArray<Token> getTokens() {
    return tokens;
  }

  public T getState() {
    return state;
  }
}
