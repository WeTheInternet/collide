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

import static com.google.collide.codemirror2.TokenType.DEF;
import static com.google.collide.codemirror2.TokenType.PROPERTY;
import static com.google.collide.codemirror2.TokenType.VARIABLE;
import static com.google.collide.codemirror2.TokenType.VARIABLE2;

import com.google.collide.client.code.autocomplete.CodeAnalyzer;
import com.google.collide.client.util.collections.SkipListStringBag;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.util.JsonCollections;

import javax.annotation.Nonnull;

/**
 * This task filters out IDs from parse results and updates collection of
 * discovered IDs.
 *
 */
public class ParsingTask implements CodeAnalyzer {

  private static final String TAG_ID_LIST = ParsingTask.class.getName() + ".idList";

  private final SkipListStringBag fileIndex;
  private final JsonArray<String> idsToRelease = JsonCollections.createArray();

  @Override
  public void onLinesDeleted(JsonArray<TaggableLine> deletedLines) {
    for (TaggableLine line : deletedLines.asIterable()) {
      JsonArray<String> lineIds = line.getTag(TAG_ID_LIST);
      if (lineIds != null) {
        fileIndex.removeAll(lineIds);
      }
    }
  }

  public ParsingTask(SkipListStringBag fileIndex) {
    this.fileIndex = fileIndex;
  }

  @Override
  public void onBeforeParse() {
  }

  @Override
  public void onAfterParse() {
    fileIndex.removeAll(idsToRelease);
    idsToRelease.clear();
  }

  @Override
  public void onParseLine(
      TaggableLine previousLine, TaggableLine line, @Nonnull JsonArray<Token> tokens) {
    JsonArray<String> lineIds = line.getTag(TAG_ID_LIST);
    if (lineIds == null) {
      lineIds = JsonCollections.createArray();
      line.putTag(TAG_ID_LIST, lineIds);
    }

    idsToRelease.addAll(lineIds);
    lineIds.clear();

    for (int i = 0, l = tokens.size(); i < l; i++) {
      Token token = tokens.get(i);
      TokenType type = token.getType();
      if (type == VARIABLE || type == VARIABLE2 || type == PROPERTY || type == DEF) {
        String value = token.getValue();
        if (value.length() > 2) {
          lineIds.add(value);
        }
      }
      // TODO: Process strings that look like ID.
    }

    fileIndex.addAll(lineIds);
  }
}
