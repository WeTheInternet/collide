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

package com.google.collide.client.autoindenter;

import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.EditorDocumentMutator;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.StringUtils;
import com.google.collide.shared.util.TextUtils;
import com.google.collide.shared.util.ListenerRegistrar.Remover;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.regexp.shared.RegExp;

/**
 * A class responsible for automatically adding indentation when appropriate.
 */
public class Autoindenter {

  private static final RegExp WHITESPACES = RegExp.compile("^\\s*$");

  private interface IndentationStrategy {

    Runnable handleTextChange(TextChange textChange, EditorDocumentMutator editorDocumentMutator);
  }

  private static class PreviousLineMatchingIndentationStrategy implements IndentationStrategy {
    @Override
    public Runnable handleTextChange(
        TextChange textChange, final EditorDocumentMutator editorDocumentMutator) {
      String text = textChange.getText();
      if (!"\n".equals(text)) {
        return null;
      }

      final int toInsert = TextUtils.countWhitespacesAtTheBeginningOfLine(
          textChange.getLine().getText());

      if (toInsert == 0) {
        return null;
      }

      final Line line = LineUtils.getLine(textChange.getLine(), 1);
      final int lineNumber = textChange.getLineNumber() + 1;
      return new Runnable() {
        @Override
        public void run() {
          String addend = StringUtils.getSpaces(toInsert);
          editorDocumentMutator.insertText(line, lineNumber, 0, addend);
        }
      };
    }
  }

  private static class CodeMirrorIndentationStrategy implements IndentationStrategy {
    private final DocumentParser documentParser;

    CodeMirrorIndentationStrategy(DocumentParser parser) {
      documentParser = parser;
    }

    @Override
    public Runnable handleTextChange(
        TextChange textChange, final EditorDocumentMutator editorDocumentMutator) {
      String text = textChange.getText();

      if (!"\n".equals(text)) {
        // TODO: We should incrementally apply autoindention to
        //               multiline pastes.
        // TODO: Take electric characters into account:
        //               documentParser.getElectricCharacters.
        return null;
      }

      // TODO: Ask parser to reparse changed line.
      final Line line = LineUtils.getLine(textChange.getLine(), 1);

      // Special case: pressing ENTER in the middle of whitespaces line should
      // not fix indentation (use case: press ENTER on empty line).
      Line prevLine = textChange.getLine();
      if (WHITESPACES.test(prevLine.getText()) && WHITESPACES.test(line.getText())) {
        return null;
      }

      final int lineNumber = textChange.getLineNumber() + 1;
      final int indentation = documentParser.getIndentation(line);
      if (indentation < 0) {
        return null;
      }

      final int oldIndentation = TextUtils.countWhitespacesAtTheBeginningOfLine(line.getText());
      if (indentation == oldIndentation) {
        return null;
      }

      return new Runnable() {
        @Override
        public void run() {
          if (indentation < oldIndentation) {
            editorDocumentMutator.deleteText(line, lineNumber, 0, oldIndentation - indentation);
          } else {
            String addend = StringUtils.getSpaces(indentation - oldIndentation);
            editorDocumentMutator.insertText(line, lineNumber, 0, addend);
          }
        }
      };
    }
  }

  /**
   * Creates an instance of {@link Autoindenter} that is configured to take on
   * the appropriate indentation strategy depending on the document parser.
   */
  public static Autoindenter create(DocumentParser documentParser, Editor editor) {
    if (documentParser.getSyntaxType() != SyntaxType.NONE && documentParser.hasSmartIndent()) {
      return new Autoindenter(new CodeMirrorIndentationStrategy(documentParser), editor);
    }
    return new Autoindenter(new PreviousLineMatchingIndentationStrategy(), editor);
  }

  private final Editor editor;
  private final IndentationStrategy indentationStrategy;
  private boolean isMutatingDocument;
  private final Editor.TextListener textListener = new Editor.TextListener() {
    @Override
    public void onTextChange(TextChange textChange) {
      handleTextChange(textChange);
    }
  };
  private final Remover textListenerRemover;

  private Autoindenter(IndentationStrategy indentationStrategy, Editor editor) {
    this.indentationStrategy = indentationStrategy;
    this.editor = editor;

    textListenerRemover = editor.getTextListenerRegistrar().add(textListener);
  }

  public void teardown() {
    textListenerRemover.remove();
  }

  private void handleTextChange(TextChange textChange) {
    if (isMutatingDocument || editor.isMutatingDocumentFromUndoOrRedo()
        || textChange.getType() != TextChange.Type.INSERT) {
      return;
    }

    final Runnable mutator = indentationStrategy.handleTextChange(
        textChange, editor.getEditorDocumentMutator());

    if (mutator == null) {
      return;
    }

    // We shouldn't be touching the document in this callback, so defer.
    Scheduler.get().scheduleFinally(new ScheduledCommand() {
      @Override
      public void execute() {
        isMutatingDocument = true;
        try {
          mutator.run();
        } finally {
          isMutatingDocument = false;
        }
      }
    });
  }
}
