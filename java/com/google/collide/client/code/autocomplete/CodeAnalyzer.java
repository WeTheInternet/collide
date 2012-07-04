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

package com.google.collide.client.code.autocomplete;

import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;

import javax.annotation.Nonnull;

/**
 * Interface that presents code analyzing facilities.
 */
public interface CodeAnalyzer {

  /**
   * Prepares for new iteration of parsing.
   */
  void onBeforeParse();

  /**
   * Analyzes tokens and updates line meta-information.
   *
   * @param previousLine line that precedes line being parsed
   * @param line line being parsed
   * @param tokens tokens collected on the line
   */
  void onParseLine(TaggableLine previousLine, TaggableLine line, @Nonnull JsonArray<Token> tokens);

  /**
   * Cleans up and perform batch operations at the end of parsing iteration.
   */
  void onAfterParse();

  /**
   * Cleans up meta-information for detached lines.
   *
   * @param deletedLines lines that are now detached
   */
  void onLinesDeleted(JsonArray<TaggableLine> deletedLines);
}
