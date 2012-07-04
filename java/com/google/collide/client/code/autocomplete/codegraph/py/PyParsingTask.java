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

import static com.google.collide.codemirror2.TokenType.KEYWORD;
import static com.google.collide.codemirror2.TokenType.VARIABLE;
import static com.google.collide.codemirror2.TokenType.WHITESPACE;

import com.google.collide.client.code.autocomplete.codegraph.Position;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Python specific parsing task.
 *
 * <p>This class recognizes variables and function names in stream of tokens.
 *
 */
@Deprecated
class PyParsingTask /*extends ParsingTask*/ {

  /**
   * Delimiter that starts new code block.
   */
  private static final String LITERAL_COLON = ":";

  /**
   * Delimiter that starts parameters definition when encountered in
   * function definition.
   */
  private static final String LITERAL_LEFT_PARANTHESIS = "(";

  /**
   * Delimiter that can separate multiple variables in lvalue.
   */
  private static final String LITERAL_COMMA = ",";

  /**
   * Assignment operator token.
   */
  private static final String LITERAL_EQUALS = "=";

  /**
   * Runs one-line parsing iteration. During parsing it fills the result scope
   * with identifiers.
   */
//  @Override
  protected void processLine(int lineNumber, JsonArray<Token> tokens) {
    PyParseContext context = new PyParseContext();
    // TODO: should either remove column information from system
    //               or add column information to tokens
    int fakeTokenColumn = 0;

    // Last token in always newline, see DocumentParserWorker.
    final int l = tokens.size() - 1;
    for (int i = 0; i < l; i++) {
      Token token = tokens.get(i);
      assert token != null;

      // Ignore whitespaces
      if (WHITESPACE == token.getType()) {
        continue;
      }

      // Common precondition: context is not too short
      if (context.predPred == null) {
        context.push(token);
        continue;
      }

      PyToken predPred = context.predPred;
      PyToken pred = context.pred;

      // In case we get ":" or "(" it may be a function definition
      if (LITERAL_COLON.equals(token.getValue())
          || LITERAL_LEFT_PARANTHESIS.equals(token.getValue())) {
        if ((KEYWORD == predPred.getType()) && "def".equals(predPred.getContent())) {
//          getResultScope().addToken(lineNumber, fakeTokenColumn,
//              new CodeToken(pred.getContent(), pred.getType(), true));
        }
        fakeTokenColumn++;
        context.push(token);
        continue;
      }

      // When we get ", id," construction,
      // then do not reset accumulator (ids before first comma)
      if (LITERAL_COMMA.equals(token.getValue())) {
        if (LITERAL_COMMA.equals(predPred.getContent())) {
          if (VARIABLE == pred.getType()) {
            context.pushSavingAccumulator(token);
            continue;
          }
        }
        context.push(token);
        continue;
      }

      // When we got "id =" then register all remembered ids as variables
      if (LITERAL_EQUALS.equals(token.getValue())) {
        if (VARIABLE == context.pred.getType()) {
          context.accumulator.add(context.pred);
          while (!context.accumulator.isEmpty()) {
            PyToken nextVar = context.accumulator.pop();
//            getResultScope().addToken(lineNumber, fakeTokenColumn,
//                new CodeToken(nextVar.getContent(), nextVar.getType()));
            fakeTokenColumn++;
          }
        }
        context.push(token);
        continue;
      }

      // When we get "id1, id2" construction
      // then remember id1, because it is going to be pushed out of context.
      if (VARIABLE == token.getType()) {
        if (VARIABLE == context.predPred.getType()) {
          if (LITERAL_COMMA.equals(context.pred.getContent())) {
            context.pushAndAddToAccumulator(token);
            continue;
          }
        }
        context.push(token);
        continue;
      }

      context.push(token);
    }
    context.push(tokens.get(l));
  }

  /**
   * Context that holds a number of previous tokens.
   *
   * <p>Some tokens are saved to accumulator. Usually accumulator holds
   * identifier-tokens within the same context.
   */
  private class PyParseContext {

    PyToken pred = null;
    PyToken predPred = null;
    final JsonArray<PyToken> accumulator = JsonCollections.createArray();

    void push(Token token) {
      accumulator.clear();
      pushSavingAccumulator(token);
    }

    void pushAndAddToAccumulator(Token token) {
      accumulator.add(predPred);
      pushSavingAccumulator(token);
    }

    void pushSavingAccumulator(Token token) {
      predPred = pred;
      pred = new PyToken(token, 0, 0);
    }
  }


  /**
   * Immutable combination of token and position.
   *
   * <p>Dispatches common messages to token.
   */
  private static class PyToken {

    final Token token;
    final Position position;

    PyToken(Token token, int lineNumber, int column) {
      this.token = token;
      this.position = Position.from(lineNumber, column);
    }

    String getContent() {
      return token.getValue();
    }

    public TokenType getType() {
      return token.getType();
    }
  }
}
