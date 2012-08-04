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

package com.google.collide.client.code.debugging;

import javax.annotation.Nullable;

import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.codemirror2.JsState;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;

/**
 * Encapsulates an algorithm to find a shortest evaluable JavaScript expression
 * by a given position in the text.
 *
 */
class EvaluableExpressionFinder {

  /**
   * Represents the result of the evaluable expression search.
   */
  interface Result {

    /**
     * @return column of the first expression's character (inclusive)
     */
    int getStartColumn();

    /**
     * @return column of the last expression's character (inclusive)
     */
    int getEndColumn();

    /**
     * @return the expression found
     */
    String getExpression();
  }

  /**
   * @see #find(LineInfo, int, DocumentParser)
   */
  @VisibleForTesting
  Result find(String text, int column) {
    if (column < 0 || column >= text.length()) {
      return null;
    }

    int left = -1;
    int right = -1;

    char ch = text.charAt(column);

    // A special case of pointing to a quote character that is next to a square bracket.
    if (StringUtils.isQuote(ch)) {
      if (column > 0 && text.charAt(column - 1) == '[') {
        ch = '[';
        --column;
      } else if (column + 1 < text.length() && text.charAt(column + 1) == ']') {
        ch = ']';
        ++column;
      } else {
        return null;
      }
    }

    if (ch == '.' || isValidCharacterForJavaScriptName(ch)) {
      left = expandLeftBorder(text, column);
      right = expandRightBorder(text, column);
    } else if (ch == '[') {
      right = expandRightBracket(text, column);
      if (right != -1) {
        left = expandLeftBorder(text, column);
      }
    } else if (ch == ']') {
      right = column;
      left = expandLeftBracket(text, column);
      if (left != -1) {
        left = expandLeftBorder(text, left);
      }
    }

    // A special case of pointing to a numeric array index inside square brackets.
    if (left != -1 && right != -1 && isOnlyDigits(text, left, right)) {
      if (left > 0 && text.charAt(left - 1) == '['
          && right + 1 < text.length() && text.charAt(right + 1) == ']') {
        left = expandLeftBorder(text, left - 1);
        ++right;
      } else {
        return null;
      }
    }

    if (left != -1 && right != -1) {
      final int startColumn = left;
      final int endColumn = right;
      final String expression = text.substring(left, right + 1);

      return new Result() {
        @Override
        public int getStartColumn() {
          return startColumn;
        }

        @Override
        public int getEndColumn() {
          return endColumn;
        }

        @Override
        public String getExpression() {
          return expression;
        }
      };
    }

    return null;
  }

  private static int expandLeftBorder(String text, int column) {
    while (column > 0) {
      char ch = text.charAt(column - 1);
      if (ch == '.' || isValidCharacterForJavaScriptName(ch)) {
        --column;
      } else if (ch == ']') {
        column = expandLeftBracket(text, column - 1);
      } else {
        break;
      }
    }
    return column;
  }

  private static int expandLeftBracket(String text, int column) {
    int bracketLevel = 1;

    for (--column; column >= 0; --column) {
      char ch = text.charAt(column);
      if (StringUtils.isQuote(ch)) {
        column = expandLeftQuote(text, column);
        if (column == -1) {
          return -1;
        }
      } else if (ch == ']') {
        ++bracketLevel;
      } else if (ch == '[') {
        --bracketLevel;
        if (bracketLevel == 0) {
          return column;
        }
      } else if (ch != '.' && !isValidCharacterForJavaScriptName(ch)) {
        return -1;
      }
    }

    return -1;
  }

  private static int expandLeftQuote(String text, int column) {
    char quote = text.charAt(column);
    if (!StringUtils.isQuote(quote)) {
      return -1;
    }

    for (--column; column >= 0; --column) {
      char ch = text.charAt(column);
      if (ch == quote) {
        // Check for escape chars.
        boolean escapeChar = false;
        for (int i = column - 1;; --i) {
          if (i >= 0 && text.charAt(i) == '\\') {
            escapeChar = !escapeChar;
          } else {
            if (!escapeChar) {
              return column;
            }
            column = i + 1;
            break;
          }
        }
      }
    }

    return -1;
  }

  private static int expandRightBorder(String text, int column) {
    while (column + 1 < text.length()) {
      char ch = text.charAt(column + 1);
      if (isValidCharacterForJavaScriptName(ch)) {
        ++column;
      } else {
        break;
      }
    }
    return column;
  }

  private static int expandRightBracket(String text, int column) {
    int bracketLevel = 1;

    for (++column; column < text.length(); ++column) {
      char ch = text.charAt(column);
      if (StringUtils.isQuote(ch)) {
        column = expandRightQuote(text, column);
        if (column == -1) {
          return -1;
        }
      } else if (ch == '[') {
        ++bracketLevel;
      } else if (ch == ']') {
        --bracketLevel;
        if (bracketLevel == 0) {
          return column;
        }
      } else if (ch != '.' && !isValidCharacterForJavaScriptName(ch)) {
        return -1;
      }
    }

    return -1;
  }

  private static int expandRightQuote(String text, int column) {
    char quote = text.charAt(column);
    if (!StringUtils.isQuote(quote)) {
      return -1;
    }

    boolean escapeChar = false;
    for (++column; column < text.length(); ++column) {
      char ch = text.charAt(column);
      if (ch == '\\') {
        escapeChar = !escapeChar;
      } else {
        if (ch == quote && !escapeChar) {
          return column;
        }
        escapeChar = false;
      }
    }

    return -1;
  }

  private static boolean isOnlyDigits(String text, int left, int right) {
    for (int i = left; i <= right; ++i) {
      if (!StringUtils.isNumeric(text.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isValidCharacterForJavaScriptName(char ch) {
    return StringUtils.isAlphaNumOrUnderscore(ch) || ch == '$';
  }

  /**
   * Finds a shortest evaluable JavaScript expression around a given position
   * in the document.
   *
   * @param lineInfo the line to examine
   * @param column the seed position to start with
   * @param parser document parser
   * @return a new instance of {@link Result}, or {@code null} if no expression
   *         was found
   */
  Result find(LineInfo lineInfo, int column, @Nullable DocumentParser parser) {
    Result result = find(lineInfo.line().getText(), column);
    if (result == null || parser == null) {
      return result;
    }

    // Use the parser information to determine if we are inside a comment
    // or a string or any other place that does not make sense to evaluate.
    Position endPosition = new Position(lineInfo, result.getEndColumn() + 1);
    ParseResult<JsState> parseResult = parser.getState(JsState.class, endPosition, null);
    if (parseResult == null) {
      return result;
    }

    JsonArray<Token> tokens = parseResult.getTokens();
    Token lastToken = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);

    if (lastToken != null) {
      switch (lastToken.getType()) {
        case ATOM:
        case COMMENT:
        case KEYWORD:
        case NUMBER:
        case STRING:
        case REGEXP:
          return null;
      }
    }

    return result;
  }
}
