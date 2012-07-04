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

package com.google.collide.client.editor;

import com.google.collide.client.editor.Editor.BeforeTextListener;
import com.google.collide.client.editor.Editor.TextListener;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerManager.Dispatcher;

/**
 * A document mutator for the editor which will notify editor text listeners
 * whenever a editor-initiated document mutation occurs.
 *
 */
public class EditorDocumentMutator implements DocumentMutator {

  private final ListenerManager<BeforeTextListener> beforeTextListenerManager = ListenerManager
      .create();
  private final Editor editor;
  private boolean isMutatingDocument;
  private final ListenerManager<TextListener> textListenerManager = ListenerManager.create();

  EditorDocumentMutator(Editor editor) {
    this.editor = editor;
  }

  @Override
  public TextChange deleteText(Line line, int column, int deleteCount) {
    return deleteText(line, line.getDocument().getLineFinder().findLine(line).number(), column,
        deleteCount);
  }

  @Override
  public TextChange deleteText(Line line, int lineNumber, int column, int deleteCount) {
    String deletedText = editor.getDocument().getText(line, column, deleteCount);
    return deleteText(line, lineNumber, column, deletedText);
  }

  private TextChange deleteText(Line line, int lineNumber, int column, String deletedText) {
    if (editor.isReadOnly()) {
      return null;
    }

    TextChange textChange = TextChange.createDeletion(line, lineNumber, column, deletedText);
    dispatchBeforeTextChange(textChange);
    isMutatingDocument = true;
    editor.getDocument().deleteText(line, lineNumber, column, deletedText.length());
    isMutatingDocument = false;
    dispatchTextChange(textChange);

    return textChange;
  }

  /**
   * If there is a selection, the inserted text will replace the selected text.
   *
   * @see DocumentMutator#insertText(Line, int, String)
   */
  @Override
  public TextChange insertText(Line line, int column, String text) {
    return insertText(line, line.getDocument().getLineFinder().findLine(line).number(), column,
        text);
  }

  @Override
  public TextChange insertText(Line line, int lineNumber, int column, String text) {
    return insertText(line, lineNumber, column, text, true);
  }

  @Override
  public TextChange insertText(Line line, int lineNumber, int column, String text,
      boolean canReplaceSelection) {
    if (editor.isReadOnly()) {
      return null;
    }

    TextChange textChange = null;

    SelectionModel selection = editor.getSelection();
    if (canReplaceSelection && selection.hasSelection()) {
      Position[] selectionRange = selection.getSelectionRange(true);
      /*
       * TODO: this isn't going to scale for document-sized
       * selections, need to change some APIs
       */
      Line beginLine = selectionRange[0].getLine();
      int beginLineNumber = selectionRange[0].getLineNumber();
      int beginColumn = selectionRange[0].getColumn();
      String textToDelete =
          LineUtils.getText(beginLine, beginColumn,
              selectionRange[1].getLine(), selectionRange[1].getColumn());
      textChange = deleteText(beginLine, beginLineNumber, beginColumn, textToDelete);

      // The insertion should go where the selection was
      line = beginLine;
      lineNumber = beginLineNumber;
      column = beginColumn;
    }

    if (text.length() == 0) {
      return textChange;
    }

    /*
     * The contract for the before text change event is to pass the insertion
     * line as the "last" line since the text hasn't been inserted yet.
     */
    textChange = TextChange.createInsertion(line, lineNumber, column, line, lineNumber, text);
    dispatchBeforeTextChange(textChange);
    isMutatingDocument = true;
    editor.getDocument().insertText(line, lineNumber, column, text);
    isMutatingDocument = false;
    dispatchTextChange(textChange);

    return textChange;
  }

  /**
   * Returns true if this mutator is currently mutating the document.
   */
  public boolean isMutatingDocument() {
    return isMutatingDocument;
  }

  void dispatchBeforeTextChange(final TextChange textChange) {
    beforeTextListenerManager.dispatch(new Dispatcher<BeforeTextListener>() {
      @Override
      public void dispatch(BeforeTextListener listener) {
        listener.onBeforeTextChange(textChange);
      }
    });
  }

  void dispatchTextChange(final TextChange textChange) {
    textListenerManager.dispatch(new Dispatcher<TextListener>() {
      @Override
      public void dispatch(TextListener listener) {
        listener.onTextChange(textChange);
      }
    });
  }

  ListenerRegistrar<BeforeTextListener> getBeforeTextListenerRegistrar() {
    return beforeTextListenerManager;
  }

  ListenerRegistrar<TextListener> getTextListenerRegistrar() {
    return textListenerManager;
  }
}
