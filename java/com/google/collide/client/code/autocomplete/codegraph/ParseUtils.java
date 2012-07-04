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

import static com.google.collide.codemirror2.Token.LITERAL_PERIOD;
import static com.google.collide.codemirror2.TokenType.NULL;
import static com.google.collide.codemirror2.TokenType.REGEXP;
import static com.google.collide.codemirror2.TokenType.STRING;
import static com.google.collide.codemirror2.TokenType.VARIABLE;
import static com.google.collide.codemirror2.TokenType.VARIABLE2;
import static com.google.collide.codemirror2.TokenType.WHITESPACE;

import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Set of utilities to perform code parsing and parse results processing.
 */
public class ParseUtils {

  private static final String SPACE = " ";
  private static final String[] CONTEXT_START = new String[] {"[", "(", "{"};
  private static final String SIMPLE_CONTEXT_END = "])";
  private static final String CONTEXT_END = SIMPLE_CONTEXT_END + "}";

  /**
   * Collect ids interleaved with periods, omitting parenthesis groups.
   *
   * @param tokens source tokens array; destroyed in runtime.
   * @param expectingPeriod state before parsing:
   *        {@code true} if period token is expected
   * @param contextParts output collector; only ids are collected
   * @return state after parsing: {@code true} if period token is expected
   */
  static boolean buildInvocationSequenceContext(
      JsonArray<Token> tokens, boolean expectingPeriod, JsonArray<String> contextParts) {
    // right-to-left tokens processing loop.
    while (!tokens.isEmpty()) {
      Token lastToken = tokens.pop();
      TokenType lastTokenType = lastToken.getType();
      String lastTokenValue = lastToken.getValue();

      // Omit whitespaces.
      if (lastTokenType == WHITESPACE) {
        continue;
      }

      if (expectingPeriod) {
        // If we are expecting period, then no other tokens are allowed.
        if ((lastTokenType != NULL) || !LITERAL_PERIOD.equals(lastTokenValue)) {
          return expectingPeriod;
        }
        expectingPeriod = false;
      } else {
        // Not expecting period means that previously processed token (located
        // to the right of the current token) was not id.
        // That way we expect id or parenthesis group.
        if (lastTokenType == VARIABLE || lastTokenType == VARIABLE2
            || lastTokenType == TokenType.PROPERTY) {
          contextParts.add(lastTokenValue);
          // Period is obligatory to the left of id to continue the chain.
          expectingPeriod = true;
        } else if ((lastTokenType == NULL) && (lastTokenValue.length() == 1)
            && SIMPLE_CONTEXT_END.contains(lastTokenValue)) {
          // We are to enter parenthesis group.
          if (!bypassParenthesizedGroup(tokens, lastToken)) {
            // If we were unable to properly close group - exit
            return expectingPeriod;
          }
          // After group is closed, we again expect id or parenthesis group.
        } else {
          // Token type we don't expect - exit
          return expectingPeriod;
        }
      }
    }
    return expectingPeriod;
  }

  /**
   * Pops tokens until context closed with lastToken is not opened,
   * or inconsistency found.
   *
   * @return {@code true} if context was successfully removed from tokens.
   */
  static boolean bypassParenthesizedGroup(JsonArray<Token> tokens, Token lastToken) {
    JsonArray<String> stack = JsonCollections.createArray();
    // Push char that corresponds to opening parenthesis.
    stack.add(CONTEXT_START[(CONTEXT_END.indexOf(lastToken.getValue()))]);
    while (!tokens.isEmpty()) {
      lastToken = tokens.pop();
      String lastTokenValue = lastToken.getValue();
      // Bypass non-parenthesis.
      if (lastToken.getType() != NULL || (lastTokenValue.length() != 1)) {
        continue;
      }

      // Dive deeper.
      if (CONTEXT_END.contains(lastTokenValue)) {
        stack.add(CONTEXT_START[(CONTEXT_END.indexOf(lastToken.getValue()))]);
        continue;
      }

      // Check if token corresponds to stack head
      if (CONTEXT_START[0].equals(lastTokenValue)
          || CONTEXT_START[1].equals(lastTokenValue) || CONTEXT_START[2].equals(lastTokenValue)) {
        if (!stack.peek().equals(lastTokenValue)) {
          // Got opening parenthesis not matching stack -> exit with error.
          return false;
        }
        stack.pop();
        // If initial group is closed - we've finished.
        if (stack.isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Types of situations.
   *
   * <p>When expanding line with letters leads to expanding last token,
   * it means that token is not finished. Strings and comments have such
   * behaviour ({@link #IN_STRING}, {@link #IN_COMMENT}).
   *
   * <p>When parse result is {@code null}, then no further analysis can be done
   * ({@link #NOT_PARSED}).
   *
   * <p>Otherwise (the most common and interesting situation) we suppose to be
   * somewhere in code ({@link #IN_CODE}).
   */
  public enum Context {
    IN_STRING, IN_COMMENT, NOT_PARSED, IN_CODE
  }

  /**
   * Bean that wraps together {@link ParseResult} and {@link Context}.
   *
   * @param <T> language-specific {@link State} type.
   */
  public static class ExtendedParseResult<T extends State> {

    private final ParseResult<T> parseResult;
    private final Context context;

    public ExtendedParseResult(ParseResult<T> parseResult, Context context) {
      this.parseResult = parseResult;
      this.context = context;
    }

    /**
     * @return {@code null} if parsing is failed, or token list is empty.
     */
    public String getLastTokenValue() {
      if (parseResult == null) {
        return null;
      }
      JsonArray<Token> tokens = parseResult.getTokens();
      if (tokens.isEmpty()) {
        return null;
      }
      Token lastToken = tokens.peek();
      return lastToken.getValue();
    }

    ParseResult<T> getParseResult() {
      return parseResult;
    }

    Context getContext() {
      return context;
    }
  }

  /**
   * Parses the line to specified position and returns parse result.
   *
   * @param parser current document parser
   * @param position point of interest
   */
  public static <T extends State> ExtendedParseResult<T> getExtendedParseResult(
      Class<T> stateClass, @Nonnull DocumentParser parser, Position position) {
    int column = position.getColumn();
    String text = position.getLine().getText().substring(0, column);

    // Add space if we are not sure that comment/literal is finished
    boolean addSpace = (column == 0)
        || text.endsWith("*/")
        || text.endsWith("'") || text.endsWith("\"");
    ParseResult<T> result = parser.getState(stateClass, position, addSpace ? SPACE : null);

    if (result == null) {
      return new ExtendedParseResult<T>(null, Context.NOT_PARSED);
    }

    JsonArray<Token> tokens = result.getTokens();
    Token lastToken = tokens.peek();
    Preconditions.checkNotNull(lastToken,
        "Last token expected to be non-null; text='%s', position=%s", text, position);
    TokenType lastTokenType = lastToken.getType();
    String lastTokenValue = lastToken.getValue();
    if (!addSpace) {
      if (lastTokenType == STRING || lastTokenType == REGEXP) {
        return new ExtendedParseResult<T>(result, Context.IN_STRING);
      } else if (lastTokenType == TokenType.COMMENT) {
        return new ExtendedParseResult<T>(result, Context.IN_COMMENT);
      }

      // Python parser, for a purpose of simplicity, parses period and variable
      // name as a single token. If period is not followed by identifier, parser
      // states that this is and error, which is, generally, not truth.
      if ((lastTokenType == TokenType.ERROR) && LITERAL_PERIOD.equals(lastTokenValue)) {
        tokens.pop();
        tokens.add(new Token(lastToken.getMode(), TokenType.NULL, LITERAL_PERIOD));
      }

      return new ExtendedParseResult<T>(result, Context.IN_CODE);
    }

    // Remove / shorten last token to omit added whitespace.
    tokens.pop();
    if (lastTokenType == STRING || lastTokenType == REGEXP || lastTokenType == TokenType.COMMENT) {
      // Whitespace stuck to token - strip it.
      lastTokenValue = lastTokenValue.substring(0, lastTokenValue.length() - 1);
      tokens.add(new Token(lastToken.getMode(), lastTokenType, lastTokenValue));
      if (lastTokenType == STRING || lastTokenType == REGEXP) {
        return new ExtendedParseResult<T>(result, Context.IN_STRING);
      } else {
        return new ExtendedParseResult<T>(result, Context.IN_COMMENT);
      }
    }
    // Otherwise whitespace was stated as a standalone token.
    return new ExtendedParseResult<T>(result, Context.IN_CODE);
  }
}
