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

package com.google.collide.client.code.autocomplete.codegraph.py;

import com.google.collide.client.code.autocomplete.CodeAnalyzer;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;

import javax.annotation.Nonnull;

/**
 * Python specific code analyzer.
 *
 * <p>This class calculates scope for each line of code.
 */
public class PyIndexUpdater implements CodeAnalyzer {

  public static final String TAG_SCOPE = PyIndexUpdater.class.getName() + ".scope";

  /**
   * Python keyword for function definition.
   */
  private static final String LITERAL_DEF = "def";

  /**
   * Python keyword for class definition.
   */
  private static final String LITERAL_CLASS = "class";


  @Override
  public void onBeforeParse() {
  }

  @Override
  public void onParseLine(
      TaggableLine previousLine, TaggableLine line, @Nonnull JsonArray<Token> tokens) {
    PyCodeScope prevScope = previousLine.getTag(TAG_SCOPE);

    PyCodeScope scope = prevScope;

    int indent = 0;
    int n = tokens.size();
    int i = 0;
    if (n > 0) {
      Token token = tokens.get(i);
      if (TokenType.WHITESPACE == token.getType()) {
        i++;
        indent = token.getValue().length();
      }
      if (n >= i + 3
          && TokenType.KEYWORD == tokens.get(i).getType()
          && TokenType.WHITESPACE == tokens.get(i + 1).getType()
          && TokenType.VARIABLE == tokens.get(i + 2).getType()) {
        String keyword = tokens.get(i).getValue();
        boolean isClass = LITERAL_CLASS.equals(keyword);
        boolean isDef = LITERAL_DEF.equals(keyword);
        if (isClass || isDef) {
          while (prevScope != null) {
            if (prevScope.getIndent() < indent) {
              break;
            }
            prevScope = prevScope.getParent();
          }
          PyCodeScope.Type type = isDef ? PyCodeScope.Type.DEF : PyCodeScope.Type.CLASS;
          scope = new PyCodeScope(prevScope, tokens.get(i + 2).getValue(), indent, type);
        }
      }
    }

    line.putTag(TAG_SCOPE, scope);
  }

  @Override
  public void onAfterParse() {
  }

  @Override
  public void onLinesDeleted(JsonArray<TaggableLine> deletedLines) {
  }
}
