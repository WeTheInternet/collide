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
package com.google.collide.client.code.autocomplete.codegraph.js;

import static com.google.collide.codemirror2.TokenType.DEF;
import static com.google.collide.codemirror2.TokenType.KEYWORD;
import static com.google.collide.codemirror2.TokenType.NEWLINE;
import static com.google.collide.codemirror2.TokenType.VARIABLE;
import static com.google.collide.codemirror2.TokenType.VARIABLE2;
import static com.google.collide.codemirror2.TokenType.WHITESPACE;

import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;

/**
 * JavaScript specific parsing task.
 *
 * <p>This class recognizes variables and function names in stream of tokens.
 */
@Deprecated
class JsParsingTask /*extends ParsingTask*/ {

  private static final String LITERAL_FUNCTION = "function";

  private static boolean isVariable(TokenType type) {
      return VARIABLE == type || VARIABLE2 == type || DEF == type;
  }

  /**
   * Parsing context stores 2 previous tokens to make it possible to find some
   * of the cases when a token is recognized as a function.
   *
   * Tries to understand the following sequences of tokens to extract
   * function names: <ul>
   * <li>function <i>id</i></li>
   * <li><i>id</i> : function </li>
   * <li><i>id</i> = function </li>
   * <li><i>id</i>(</li>
   * </ul>
   */
  private static class JsParsingContext {

    private static final String COLON_OPERATOR = ":";
    private static final String EQUALS_OPERATOR = "=";
    private static final String LEFT_PAR = "(";

    private Token pred = null;
    private Token predpred = null;

    /**
     * Finds cases when a token might be recognized as a function.
     *
     * <p>nextToken is not pushed to the context, but used for analysis.
     * That way we have 3 sequential tokens to analyse:
     * {@code predPred, pred, nextToken}.
     *
     * @param nextToken a token that is going to be pushed to context
     * @return a token that might be a function or {@code null} if there is
     *         no sign of a function
     */
    Token getFunctionToken(Token nextToken) {
      if (pred == null) {
        return null;
      }
      if (KEYWORD == pred.getType() && LITERAL_FUNCTION.equals(pred.getValue())) {
        return nextToken;
      }
      if (LEFT_PAR.equals(nextToken.getValue()) && isVariable(pred.getType())) {
        return pred;
      }
      if (KEYWORD == nextToken.getType() && LITERAL_FUNCTION.equals(nextToken.getValue())
          && predpred != null && isVariable(predpred.getType())
          && (COLON_OPERATOR.equals(pred.getValue()) || EQUALS_OPERATOR.equals(pred.getValue()))) {
        return predpred;
      }
      return null;
    }

    /**
     * Consumes next token.
     *
     * <p>Tokens that hold no information are omitted.
     *
     * @param nextToken token to push to context
     */
    private void nextToken(Token nextToken) {
      TokenType nextTokenType = nextToken.getType();
      if (WHITESPACE != nextTokenType && NEWLINE != nextTokenType) {
        predpred = pred;
        pred = nextToken;
      }
    }
  }

//  @Override
  protected void processLine(int lineNumber, JsonArray<Token> tokens) {
    JsParsingContext context = new JsParsingContext();
    for (int i = 0; i < tokens.size(); i++) {
      Token token = tokens.get(i);
      if (isVariable(token.getType())) {
//        getResultScope().addToken(
//            lineNumber, 0, new CodeToken(token.getValue().trim(), token.getType()));
      }
      Token functionToken = context.getFunctionToken(token);
      if (functionToken != null) {
//        getResultScope().addToken(lineNumber, 0,
//            new CodeToken(functionToken.getValue().trim(), functionToken.getType(), true));
      }
      context.nextToken(token);
    }
  }
}
