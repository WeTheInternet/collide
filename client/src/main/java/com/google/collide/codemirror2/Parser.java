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
 * Interface that represents CodeMirror parser.
 */
public interface Parser {

  /**
   * @return {@code true} if {@link #indent} is supported by this parser
   */
  boolean hasSmartIndent();

  /**
   * @return syntax type (language) served by this parser
   */
  SyntaxType getSyntaxType();

  /**
   * @param stateAbove parser state before current line
   * @param text left-trimmed content of the line to be indented
   * @return proposed number of spaces before the text
   */
  int indent(State stateAbove, String text);

  /**
   * @return newly constructed "before the first line" state
   */
  State defaultState();

  /**
   * Consumes characters from input, updates state
   * and pushes recognized token to output.
   */
  void parseNext(Stream stream, State parserState, JsonArray<Token> tokens);

  /**
   * Wraps text in JS object used by native parser.
   */
  Stream createStream(String text);

  /**
   * Extracts the name (mode) from the given {@link State}.
   */
  String getName(State state);
}
