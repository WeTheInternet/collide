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

import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter.ExplicitAction;
import com.google.collide.client.code.autocomplete.codegraph.ExplicitAutocompleter;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.shared.document.anchor.ReadOnlyAnchor;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.event.dom.client.KeyCodes;

import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;

import javax.annotation.Nonnull;

/**
 * Implementation that adds JS-specific cases.
 */
class JsExplicitAutocompleter extends ExplicitAutocompleter {

  @Override
  protected ExplicitAction getExplicitAction(SelectionModel selectionModel,
      SignalEventEssence signal, boolean popupIsShown, @Nonnull DocumentParser parser) {
    if (checkEnterTrigger(signal) && checkCursorBetweenCurlyBraces(selectionModel)) {
      String text = selectionModel.getCursorPosition().getLine().getText();
      int indent = StringUtils.lengthOfStartingWhitespace(text);
      String newLine = "\n" + StringUtils.getSpaces(indent);
      String emptyLine = newLine + "  ";
      return new ExplicitAction(
          new DefaultAutocompleteResult(emptyLine + newLine, "", emptyLine.length()));
    }
    return super.getExplicitAction(selectionModel, signal, popupIsShown, parser);
  }

  /**
   * Checks trigger to be plain "Enter" key press.
   *
   * <p>"Shift-Enter" also works to avoid "sticky-shift" issue:
   * when someone quickly types "Shift-[" (-> "{") and then
   * press "Enter" while "Shift" is not depressed.
   */
  private static boolean checkEnterTrigger(SignalEventEssence trigger) {
    return KeySignalType.INPUT == trigger.type
        && KeyCodes.KEY_ENTER == trigger.keyCode
        && !trigger.altKey && !trigger.ctrlKey && !trigger.metaKey;
  }

  /**
   * Checks that cursor is situated between curly braces.
   */
  private static boolean checkCursorBetweenCurlyBraces(SelectionModel selectionModel) {
    if (!selectionModel.hasSelection()) {
      ReadOnlyAnchor cursor = selectionModel.getCursorAnchor();
      String text = cursor.getLine().getText();
      int column = cursor.getColumn();
      if (column > 0 && column < text.length()) {
        if (text.charAt(column - 1) == '{' && text.charAt(column) == '}') {
          return true;
        }
      }
    }
    return false;
  }
}
