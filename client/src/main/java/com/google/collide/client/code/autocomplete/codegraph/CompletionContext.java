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

import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.codemirror2.State;

/**
 * Data bean that represents textual context around cursor position.
 *
 * @param <T> language-specific {@link State} type.
 *
 */
public class CompletionContext<T extends State> {

  /**
   * Type of autocomplete.
   */
  private final CompletionType completionType;

  /**
   * Flag that indicates that we are addressing object own items.
   */
  private final boolean isThisContext;

  /**
   * Prefix (preceding expression) used for trie truncating.
   */
  private final String previousContext;

  /**
   * Autocompleter triggering string.
   *
   * <p>String that looks like uncompleted identifier.
   */
  private final String triggeringString;

  /**
   * Result of parsing of line to the cursor position.
   *
   * @see ParseUtils#getExtendedParseResult
   */
  private final ParseResult<T> parseResult;

  /**
   * Proposed indention for the next line.
   */
  private final int indent;

  public CompletionContext(String previousContext, String triggeringString, boolean thisContext,
      CompletionType completionType, ParseResult<T> parseResult, int indent) {
    this.previousContext = previousContext;
    this.triggeringString = triggeringString;
    this.isThisContext = thisContext;
    this.completionType = completionType;
    this.parseResult = parseResult;
    this.indent = indent;
  }

  public CompletionType getCompletionType() {
    return completionType;
  }

  public boolean isThisContext() {
    return isThisContext;
  }

  public String getPreviousContext() {
    return previousContext;
  }

  public String getTriggeringString() {
    return triggeringString;
  }

  public ParseResult<T> getParseResult() {
    return parseResult;
  }

  public int getIndent() {
    return indent;
  }
}
