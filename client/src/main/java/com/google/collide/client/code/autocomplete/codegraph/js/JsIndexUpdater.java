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

import javax.annotation.Nonnull;

import com.google.collide.client.code.autocomplete.CodeAnalyzer;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;

/**
 * JavaScript specific code analyzer.
 *
 * <p>This class calculates scope for each line of code.
 */
public class JsIndexUpdater implements CodeAnalyzer {

  public static final String TAG_SCOPE = JsIndexUpdater.class.getName() + ".scope";
  private static final String TAG_STATE = JsIndexUpdater.class.getName() + ".state";

  /**
   * Tag for storing incomplete scope name.
   */
  private static final String TAG_NAME = JsIndexUpdater.class.getName() + ".sName";

  private static final String LITERAL_FUNCTION = "function";
  private static final String LITERAL_PERIOD = ".";
  private static final String LITERAL_ASSIGN = "=";
  private static final String LITERAL_COLON = ":";
  private static final String LITERAL_OPEN_BRACKET = "(";
  private static final String LITERAL_CLOSE_BRACKET = ")";
  private static final String LITERAL_COMMA = ",";
  private static final String LITERAL_OPEN_CURLY = "{";
  private static final String LITERAL_CLOSE_CURLY = "}";

  /**
   * Checks if the token describes some name (variable, function) or
   * name part (property).
   */
  private static boolean isName(TokenType type) {
      return TokenType.VARIABLE == type || TokenType.VARIABLE2 == type
          || TokenType.DEF == type || TokenType.PROPERTY == type;
  }

  private static boolean isFunctionKeyword(TokenType tokenType, String tokenValue) {
    return TokenType.KEYWORD == tokenType && LITERAL_FUNCTION.equals(tokenValue);
  }

  /**
   * Enumeration of situations that may occur during scope parsing.
   */
  private enum State {
    /**
     * Without particular context.
     */
    NONE,

    /**
     * Keyword "function" met, expecting name.
     */
    FUNCTION,

    /**
     * Context with intention to define named function.
     *
     * <p>Expecting "("
     */
    NAMED_FUNCTION,

    /**
     * Inside params definition.
     */
    FUNCTION_PARAMS,

    /**
     * Function name and params are known, expecting block start.
     */
    FULLY_QUALIFIED,

    /**
     * (Variable) name met. Expecting "=", ":", or "."
     */
    NAME,

    /**
     * Got something like "name1.", expecting next name.
     */
    SUBNAME,

    /**
     * Got something like "name1.name2 =", or "name1:".
     *
     * <p>Expecting "function" keyword.
     */
    ASSIGN,
  }

  /**
   * Bean that holds line-parsing context.
   */
  public static class Context {

    private final State state;
    private final String name;
    private final JsCodeScope scope;

    /**
     * Constructs context from saved values.
     */
    public Context(TaggableLine line) {
      scope = line.getTag(TAG_SCOPE);
      name = line.getTag(TAG_NAME);

      State tempState = line.getTag(TAG_STATE);
      if (tempState == null) {
        tempState = State.NONE;
      }
      state = tempState;
    }

    private Context(State state, String name, JsCodeScope scope) {
      this.state = state;
      this.name = name;
      this.scope = scope;
    }

    private void saveToLine(TaggableLine line) {
      line.putTag(TAG_SCOPE, scope);
      line.putTag(TAG_NAME, name);
      line.putTag(TAG_STATE, state);
    }

    public JsCodeScope getScope() {
      return scope;
    }
  }

  /**
   * Calculates updated context based on previous line context and tokens.
   *
   * <p>Note: currently we do not create root scope, so {@code null} scope
   * corresponds to root.
   */
  public static Context calculateContext(TaggableLine previousLine, JsonArray<Token> tokens) {
    Context context = new Context(previousLine);
    State state = context.state;
    String name = context.name;
    JsCodeScope scope = context.scope;

    int size = tokens.size();
    int index = 0;
    while (index < size) {
      Token token = tokens.get(index);
      index++;

      TokenType tokenType = token.getType();

      if (TokenType.WHITESPACE == tokenType
          || TokenType.NEWLINE == tokenType
          || TokenType.COMMENT == tokenType) {
        // TODO: Parse JsDocs.
        continue;
      }

      String tokenValue = token.getValue();

      switch (state) {
        case NONE:
          if (isFunctionKeyword(tokenType, tokenValue)) {
            state = State.FUNCTION;
          } else if (isName(tokenType)) {
            name = tokenValue;
            state = State.NAME;
          } else if (LITERAL_OPEN_CURLY.equals(tokenValue)) {
            scope = new JsCodeScope(scope, null);
            name = null;
          } else if (LITERAL_CLOSE_CURLY.equals(tokenValue)) {
            if (scope != null) {
              scope = scope.getParent();
            }
          }
          break;

        case FUNCTION:
          if (isName(tokenType)) {
            name = tokenValue;
            state = State.NAMED_FUNCTION;
          } else {
            index--;
            name = null;
            state = State.NONE;
          }
          break;

        case NAMED_FUNCTION:
          if (LITERAL_OPEN_BRACKET.equals(tokenValue)) {
            state = State.FUNCTION_PARAMS;
          } else {
            index--;
            name = null;
            state = State.NONE;
          }
          break;

        case FUNCTION_PARAMS:
          if (LITERAL_CLOSE_BRACKET.equals(tokenValue)) {
            state = State.FULLY_QUALIFIED;
          } else if (isName(tokenType) || LITERAL_COMMA.equals(tokenValue)) {
            // Do nothing.
          } else {
            index--;
            name = null;
            state = State.NONE;
          }
          break;

        case FULLY_QUALIFIED:
          if (LITERAL_OPEN_CURLY.equals(tokenValue)) {
            scope = new JsCodeScope(scope, name);
            name = null;
            state = State.NONE;
          } else {
            index--;
            name = null;
            state = State.NONE;
          }
          break;

        case NAME:
          if (LITERAL_PERIOD.equals(tokenValue)) {
            state = State.SUBNAME;
          } else if (LITERAL_ASSIGN.equals(tokenValue) || LITERAL_COLON.equals(tokenValue)) {
            state = State.ASSIGN;
          } else {
            index--;
            name = null;
            state = State.NONE;
          }
          break;

        case SUBNAME:
          if (isName(tokenType)) {
            name = name + "." + tokenValue;
            state = State.NAME;
          } else {
            index--;
            name = null;
            state = State.NONE;
          }
          break;

        case ASSIGN:
          if (isFunctionKeyword(tokenType, tokenValue)) {
            state = State.NAMED_FUNCTION;
          } else if (LITERAL_OPEN_CURLY.equals(tokenValue)) {
            scope = new JsCodeScope(scope, name);
            name = null;
            state = State.NONE;
          } else {
            index--;
            name = null;
            state = State.NONE;
          }
          break;

        default:
          throw new IllegalStateException("Unexpected state [" + state + "]");
      }
    }

    return new Context(state, name, scope);
  }

  @Override
  public void onBeforeParse() {
  }

  @Override
  public void onParseLine(
      TaggableLine previousLine, TaggableLine line, @Nonnull JsonArray<Token> tokens) {
    calculateContext(previousLine, tokens).saveToLine(line);
  }

  @Override
  public void onAfterParse() {
  }

  @Override
  public void onLinesDeleted(JsonArray<TaggableLine> deletedLines) {
  }
}
