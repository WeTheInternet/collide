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

import static com.google.collide.client.code.autocomplete.AutocompleteResult.PopupAction.CLOSE;
import static com.google.collide.client.code.autocomplete.codegraph.ParseUtils.Context.IN_CODE;
import static com.google.collide.client.code.autocomplete.codegraph.ParseUtils.Context.IN_COMMENT;
import static com.google.collide.client.code.autocomplete.codegraph.ParseUtils.Context.IN_STRING;
import static com.google.collide.client.code.autocomplete.codegraph.ParseUtils.Context.NOT_PARSED;
import static com.google.collide.codemirror2.TokenType.STRING;
import static com.google.gwt.event.dom.client.KeyCodes.KEY_BACKSPACE;
import static org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType.DELETE;

import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitAction;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Object that answers question about explicit actions and autocompletions.
 *
 */
public class ExplicitAutocompleter {

  private static final ExplicitAction RESULT_DELETE_AND_BACKSPACE = new ExplicitAction(
      new DefaultAutocompleteResult("", 0, 1, 0, 1, CLOSE, ""));

  /**
   * Compute left-trimmed text before position.
   *
   * @param position point of interest
   * @return beginning of line with removed spaces
   */
  static String leftTrimmedLineTextBeforePosition(Position position) {
    return position.getLine().getText().substring(0, position.getColumn()).replaceAll("^\\s+", "");
  }

  /**
   * Compute text after position.
   *
   * @param position point of interest
   * @return tail of line
   */
  static String textAfterPosition(Position position) {
    return position.getLine().getText().substring(position.getColumn());
  }

  static String textBeforePosition(Position position) {
    return position.getLine().getText().substring(0, position.getColumn());
  }

  private boolean isExplicitDoublingChar(char keyCode) {
    return "(\"){'}[]".indexOf(keyCode) != -1;
  }

  protected ExplicitAction getExplicitAction(SelectionModel selectionModel,
      SignalEventEssence signal, boolean popupIsShown, @Nonnull DocumentParser parser) {
    char key = signal.getChar();
    if (!popupIsShown && key == '.') {
      return ExplicitAction.DEFERRED_COMPLETE;
    }
    if (isExplicitDoublingChar(key)) {
      return getExplicitDoublingAutocompletion(signal, selectionModel, parser);
    }
    if (DELETE == signal.type && KEY_BACKSPACE == signal.keyCode
        && !signal.ctrlKey && !signal.altKey && !signal.shiftKey && !signal.metaKey) {
      return getExplicitBackspaceAutocompletion(selectionModel, parser);
    }
    if (Character.isLetterOrDigit(key) || key == '_' || key == 0) {
      return ExplicitAction.DEFAULT;
    }
    return popupIsShown ? ExplicitAction.CLOSE_POPUP : ExplicitAction.DEFAULT;
  }

  /**
   * Calculates explicit autocompletion result for "backspace" press.
   *
   * <p>This method works in assumption that there is no selection.
   *
   * <p>One "dangerous" case is when user press "backspace" at the very
   * beginning of the document.
   *
   * @return result that performs "del" or "del+bs" or nothing
   */
  private ExplicitAction getExplicitBackspaceAutocompletion(
      SelectionModel selection, @Nonnull DocumentParser parser) {
    if (selection.hasSelection()) {
      return ExplicitAction.DEFAULT;
    }
    Position cursor = selection.getCursorPosition();

    String textToCursor = leftTrimmedLineTextBeforePosition(cursor);
    String textAfterCursor = textAfterPosition(cursor);

    ParseUtils.ExtendedParseResult<State> extendedParseResult = ParseUtils
        .getExtendedParseResult(State.class, parser, cursor);
    ParseUtils.Context context = extendedParseResult.getContext();
    char right = textAfterCursor.length() > 0 ? textAfterCursor.charAt(0) : 0;

    if (context == IN_STRING) {
      // This means that full token contains only string quotes.
      if ((String.valueOf(right)).equals(extendedParseResult.getLastTokenValue())) {
        return RESULT_DELETE_AND_BACKSPACE;
      }
    } else if (context == IN_CODE) {
      char left = textToCursor.length() > 0 ? textToCursor.charAt(textToCursor.length() - 1) : 0;

      if (left == '(' && right == ')') {
        return RESULT_DELETE_AND_BACKSPACE;
      } else if (left == '{' && right == '}') {
        return RESULT_DELETE_AND_BACKSPACE;
      } else if (left == '[' && right == ']') {
        return RESULT_DELETE_AND_BACKSPACE;
      }
    }

    return ExplicitAction.DEFAULT;
  }

  private ExplicitAction getExplicitDoublingAutocompletion(
      SignalEventEssence trigger, SelectionModel selection, @Nonnull DocumentParser parser) {
    Position[] selectionRange = selection.getSelectionRange(false);
    ParseUtils.ExtendedParseResult<State> extendedParseResult = ParseUtils
        .getExtendedParseResult(State.class, parser, selectionRange[0]);
    ParseUtils.Context context = extendedParseResult.getContext();

    char key = trigger.getChar();

    Preconditions.checkState(key != 0);

    if (context == NOT_PARSED || context == IN_COMMENT) {
      return ExplicitAction.DEFAULT;
    }

    String textAfterCursor = textAfterPosition(selectionRange[1]);
    int nextChar = -1;
    if (textAfterCursor.length() > 0) {
      nextChar = textAfterCursor.charAt(0);
    }

    boolean canPairParenthesis =
        nextChar == -1 || nextChar == ' ' || nextChar == ',' || nextChar == ';' || nextChar == '\n';

    // TODO: Check if user has just fixed pairing?
    if (context != IN_STRING) {
      if ('(' == key && canPairParenthesis) {
        return new ExplicitAction(new DefaultAutocompleteResult("()", "", 1));
      } else if ('[' == key && canPairParenthesis) {
        return new ExplicitAction(new DefaultAutocompleteResult("[]", "", 1));
      } else if ('{' == key && canPairParenthesis) {
        return new ExplicitAction(new DefaultAutocompleteResult("{}", "", 1));
      } else if ('"' == key || '\'' == key) {
        String doubleQuote = key + "" + key;
        if (!textBeforePosition(selectionRange[0]).endsWith(doubleQuote)) {
          return new ExplicitAction(new DefaultAutocompleteResult(doubleQuote, "", 1));
        }
      } else if (!selection.hasSelection() && (key == nextChar)
          && (']' == key || ')' == key || '}' == key)) {
        // Testing what is more useful: pasting or passing.
        JsonArray<Token> tokens = parser.parseLineSync(selectionRange[0].getLine());
        if (tokens != null) {
          int column = selectionRange[0].getColumn();
          String closers = calculateClosingParens(tokens, column);
          String openers = calculateOpenParens(tokens, column);

          int match = StringUtils.findCommonPrefixLength(closers, openers);
          int newMatch = StringUtils.findCommonPrefixLength(key + closers, openers);
          if (newMatch <= match) {
            // With pasting results will be worse -> pass.
            return new ExplicitAction(DefaultAutocompleteResult.PASS_CHAR);
          }
        }
      }
    } else {
      if ((key == nextChar) && ('"' == key || '\'' == key)) {
        ParseResult<State> parseResult = parser.getState(State.class, selectionRange[0], key + " ");
        if (parseResult != null) {
          JsonArray<Token> tokens = parseResult.getTokens();
          Preconditions.checkState(!tokens.isEmpty());
          if (tokens.peek().getType() != STRING) {
            return new ExplicitAction(DefaultAutocompleteResult.PASS_CHAR);
          }
        }
      }
    }

    return ExplicitAction.DEFAULT;
  }

  @VisibleForTesting
  static String calculateOpenParens(JsonArray<Token> tokens, int column) {
    if (column == 0) {
      return "";
    }

    JsonArray<String> parens = JsonCollections.createArray();
    int colSum = 0;
    for (Token token : tokens.asIterable()) {
      String value = token.getValue();

      if ("}".equals(value) || ")".equals(value) || "]".equals(value)) {
        if (parens.size() > 0) {
          if (value.equals(parens.peek())) {
            parens.pop();
          } else {
            parens.clear();
          }
        }
      } else if ("{".equals(value)) {
        parens.add("}");
      } else if ("(".equals(value)) {
        parens.add(")");
      } else if ("[".equals(value)) {
        parens.add("]");
      }

      colSum += value.length();
      if (colSum >= column) {
        break;
      }
    }
    parens.reverse();
    return parens.join("");
  }

  @VisibleForTesting
  static String calculateClosingParens(JsonArray<Token> tokens, int column) {
    StringBuilder result = new StringBuilder();
    int colSum = 0;
    for (Token token : tokens.asIterable()) {
      String value = token.getValue();
      if (colSum >= column) {
        if ("}".equals(value) || ")".equals(value) || "]".equals(value)) {
          result.append(value);
        } else if (token.getType() != TokenType.WHITESPACE) {
          break;
        }
      }
      colSum += value.length();
    }
    return result.toString();
  }
}
