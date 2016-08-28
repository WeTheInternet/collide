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

import static com.google.collide.shared.document.util.PositionUtils.getPosition;

import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.EditorDocumentMutator;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.logging.Log;
import com.google.collide.shared.document.Position;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Implementation that allows to apply most common autocompletions.
 *
 */
public class DefaultAutocompleteResult implements AutocompleteResult {

  /**
   * Empty result.
   *
   * <ul>
   * <li>if there is no selection - does nothing
   * <li>if something is selected - deletes selected text
   * </ul>
   */
  public static final DefaultAutocompleteResult EMPTY = new DefaultAutocompleteResult(
      "", 0, 0, 0, 0, PopupAction.CLOSE, "");

  /**
   * Result that moves cursor to the right on 1 character and closes popup.
   *
   * <p>This result is used to bypass unintended user input. For example, when
   * user enters quote twice, first quote is explicitly doubled, and the
   * second one must be bypassed.
   */
  public static final DefaultAutocompleteResult PASS_CHAR = new DefaultAutocompleteResult(
      "", 1, 0, 0, 0, PopupAction.CLOSE, "");

  /**
   * Number of symbols to expand selection to the left before replacement.
   */
  private final int backspaceCount;

  /**
   * Number of symbols to expand selection to the right before replacement.
   */
  private final int deleteCount;

  /**
   * Text to be inserted at cursor position.
   */
  private final String autocompletionText;

  /**
   * Number of chars, relative to beginning of replacement to move cursor right.
   */
  private final int jumpLength;

  /**
   * Length of selection (in chars) before cursor position after jump.
   */
  private final int selectionCount;

  /**
   * String that guards from applying result when context has changed.
   *
   * <p>{@link Autocompleter#applyChanges} checks that text before selection
   * (cursor) is the same as {@link #preContentSuffix} and refuses to apply
   * result if it's not truth.
   *
   * <p>If suffix is matched, then it is removed. That way one can replace
   * template shortcut with real template content.
   */
  private final String preContentSuffix;

  /**
   * Action over popup when completion is applied.
   */
  private final PopupAction popupAction;

  public DefaultAutocompleteResult(String autocompletionText, int jumpLength, int backspaceCount,
      int selectionCount, int deleteCount, PopupAction popupAction, String preContentSuffix) {
    Preconditions.checkState(jumpLength >= 0, "negative jump length");
    Preconditions.checkState(backspaceCount >= 0, "negative backspace count");
    Preconditions.checkState(selectionCount >= 0, "negative select count");
    Preconditions.checkState(deleteCount >= 0, "negative delete count");
    Preconditions.checkState(selectionCount <= jumpLength, "select count > jump length");

    this.autocompletionText = autocompletionText;
    this.jumpLength = jumpLength;
    this.backspaceCount = backspaceCount;
    this.selectionCount = selectionCount;
    this.deleteCount = deleteCount;
    this.popupAction = popupAction;
    this.preContentSuffix = preContentSuffix;
  }

  /**
   * Creates simple textual insertion result.
   *
   * <p>Created instance describes insertion of specified text with matching
   * (see {@link #preContentSuffix}), without additional deletions and without
   * selection; after applying insertion popup is closed.
   */
  public DefaultAutocompleteResult(String autocompletionText, String preContentSuffix,
      int jumpLength) {
    this(autocompletionText, jumpLength, 0, 0, 0, PopupAction.CLOSE, preContentSuffix);
  }

  @VisibleForTesting
  public String getAutocompletionText() {
    return autocompletionText;
  }

  @VisibleForTesting
  public int getJumpLength() {
    return jumpLength;
  }

  @VisibleForTesting
  public int getBackspaceCount() {
    return backspaceCount;
  }

  @VisibleForTesting
  public int getDeleteCount() {
    return deleteCount;
  }

  @Override
  public PopupAction getPopupAction() {
    return popupAction;
  }

  @Override
  public void apply(Editor editor) {
    SelectionModel selection = editor.getSelection();
    Position[] selectionRange = selection.getSelectionRange(false);
    boolean selectionChanged = false;

    // 1) New beginning of selection based on suffix-matching and
    //    backspaceCount
    Position selectionStart = selectionRange[0];
    int selectionStartColumn = selectionStart.getColumn();
    String textBefore = selectionStart.getLine().getText().substring(0, selectionStartColumn);
    if (!textBefore.endsWith(preContentSuffix)) {
      Log.warn(getClass(),
          "expected suffix [" + preContentSuffix + "] do not match [" + textBefore + "]");
      return;
    }
    int matchCount = preContentSuffix.length();

    int leftOffset = backspaceCount + matchCount;
    if (leftOffset > 0) {
      selectionStart = getPosition(selectionStart, -leftOffset);
      selectionChanged = true;
    }

    // 2) Calculate end of selection
    Position selectionEnd = selectionRange[1];
    if (deleteCount > 0) {
      selectionEnd = getPosition(selectionEnd, deleteCount);
      selectionChanged = true;
    }

    // 3) Set selection it was changed.
    if (selectionChanged) {
      selection.setSelection(selectionStart.getLineInfo(), selectionStart.getColumn(),
          selectionEnd.getLineInfo(), selectionEnd.getColumn());
    }

    // 4) Replace selection
    EditorDocumentMutator mutator = editor.getEditorDocumentMutator();
    if (selection.hasSelection() || autocompletionText.length() > 0) {
      mutator.insertText(selectionStart.getLine(), selectionStart.getLineNumber(),
          selectionStart.getColumn(), autocompletionText);
    }

    // 5) Move cursor / set final selection
    selectionEnd = getPosition(selectionStart, jumpLength);
    if (selectionCount == 0) {
      selection.setCursorPosition(selectionEnd.getLineInfo(), selectionEnd.getColumn());
    } else {
      selectionStart = getPosition(selectionStart, jumpLength - selectionCount);
      selection.setSelection(selectionStart.getLineInfo(), selectionStart.getColumn(),
          selectionEnd.getLineInfo(), selectionEnd.getColumn());
    }
  }

  @Override
  public String toString() {
    return "SimpleAutocompleteResult{" +
        "backspaceCount=" + backspaceCount +
        ", deleteCount=" + deleteCount +
        ", autocompletionText='" + autocompletionText + '\'' +
        ", jumpLength=" + jumpLength +
        ", selectionCount=" + selectionCount +
        ", expectedSuffix='" + preContentSuffix + '\'' +
        ", popupAction=" + popupAction +
        '}';
  }
}
