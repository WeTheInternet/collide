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

import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.client.ClientDocOpFactory;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.util.PositionUtils;
import com.google.collide.shared.ot.Composer;
import com.google.collide.shared.ot.DocOpApplier;
import com.google.collide.shared.ot.DocOpUtils;
import com.google.collide.shared.ot.Inverter;
import com.google.collide.shared.ot.Transformer;
import com.google.collide.shared.ot.Composer.ComposeException;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar;

import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.undo.UndoManagerImpl;
import org.waveprotocol.wave.model.undo.UndoManagerImpl.Algorithms;
import org.waveprotocol.wave.model.undo.UndoManagerPlus;

import java.util.List;

// TODO: restore selection/cursor
/**
 * A class to manage the editor's undo/redo functionality.
 *
 */
public class EditorUndoManager {

  /**
   * Delegates to the document operation algorithms when requested by the Wave
   * undo library.
   */
  private static final UndoManagerImpl.Algorithms<DocOp> ALGORITHMS = new Algorithms<DocOp>() {
    @Override
    public DocOp invert(DocOp operation) {
      return Inverter.invert(ClientDocOpFactory.INSTANCE, operation);
    }

    @Override
    public DocOp compose(List<DocOp> operationsReverse) {
      try {
        return Composer.compose(ClientDocOpFactory.INSTANCE, operationsReverse);
      } catch (ComposeException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public OperationPair<DocOp> transform(DocOp op1, DocOp op2) throws TransformException {
      try {
        com.google.collide.shared.ot.OperationPair ourPair =
            Transformer.transform(ClientDocOpFactory.INSTANCE, op1, op2);
        return new OperationPair<DocOp>(ourPair.clientOp(), ourPair.serverOp());
      } catch (com.google.collide.shared.ot.Transformer.TransformException e) {
        throw new TransformException(e);
      }
    }
  };

  public static EditorUndoManager create(Editor editor, Document document,
                                         SelectionModel selection) {
    return new EditorUndoManager(editor, document, selection,
        new UndoManagerImpl<DocOp>(ALGORITHMS));
  }

  /*
   * TODO: think about which other events should cause a
   * checkpoint. Push out to a toplevel class if it gets complicated.
   */
  /**
   * Produces undo checkpoints at opportune times, such as when the user
   * explicitly moves the cursor (arrow keys or mouse click) or when the user's
   * text mutations change from delete to insert.
   */
  private class CheckpointProducer
      implements
        SelectionModel.CursorListener,
        Editor.BeforeTextListener,
        Editor.TextListener {

    private TextChange.Type previousTextChangeType;

    @Override
    public void onCursorChange(LineInfo lineInfo, int column, boolean isExplicitChange) {
      if (isExplicitChange) {
        undoManager.checkpoint();
      }
    }

    @Override
    public void onBeforeTextChange(TextChange textChange) {
      if (previousTextChangeType != textChange.getType()) {
        undoManager.checkpoint();
      }

      previousTextChangeType = textChange.getType();
    }

    @Override
    public void onTextChange(TextChange textChange) {
      if (textChange.getText().contains("\n")) {
        /*
         * Checkpoint after newlines which tends to be a good granularity for
         * typical editor use
         */
        undoManager.checkpoint();
      }
    }
  }

  private final CheckpointProducer checkpointProducer = new CheckpointProducer();
  private final Document document;
  private final Document.TextListener documentTextListener = new Document.TextListener() {
    @Override
    public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
      if (isMutatingDocument || editor.getEditorDocumentMutator().isMutatingDocument()) {
        // We will handle this text change in the editor text change callback
        return;
      }

      for (int i = 0, n = textChanges.size(); i < n; i++) {
        TextChange textChange = textChanges.get(i);
        // This is a collaborator doc op, which is not undoable by us
        undoManager.nonUndoableOp(DocOpUtils.createFromTextChange(ClientDocOpFactory.INSTANCE,
            textChange));
      }
    }
  };

  private final Editor editor;
  private final Editor.TextListener editorTextListener = new Editor.TextListener() {
    @Override
    public void onTextChange(TextChange textChange) {

      if (isMutatingDocument) {
        // We caused this text change, so don't handle it
        return;
      }

      // This is a user document mutation that is undoable
      undoManager.undoableOp(DocOpUtils.createFromTextChange(ClientDocOpFactory.INSTANCE,
          textChange));
    }
  };

  private boolean isMutatingDocument;
  private final JsonArray<ListenerRegistrar.Remover> listenerRemovers =
      JsonCollections.createArray();
  private final SelectionModel selection;
  private final UndoManagerPlus<DocOp> undoManager;

  private EditorUndoManager(Editor editor, Document document, SelectionModel selection,
      UndoManagerPlus<DocOp> undoManager) {
    this.document = document;
    this.editor = editor;
    this.selection = selection;
    this.undoManager = undoManager;

    listenerRemovers.add(document.getTextListenerRegistrar().add(documentTextListener));
    listenerRemovers.add(editor.getTextListenerRegistrar().add(editorTextListener));
    listenerRemovers.add(selection.getCursorListenerRegistrar().add(checkpointProducer));
    listenerRemovers.add(editor.getBeforeTextListenerRegistrar().add(checkpointProducer));
    listenerRemovers.add(editor.getTextListenerRegistrar().add(checkpointProducer));
  }

  boolean isMutatingDocument() {
    return isMutatingDocument;
  }

  void undo() {
    DocOp undoDocOp = undoManager.undo();
    if (undoDocOp == null) {
      return;
    }

    applyToDocument(undoDocOp);
  }

  void redo() {
    DocOp redoDocOp = undoManager.redo();
    if (redoDocOp == null) {
      return;
    }

    applyToDocument(redoDocOp);
  }

  void teardown() {
    for (int i = 0, n = listenerRemovers.size(); i < n; i++) {
      listenerRemovers.get(i).remove();
    }
  }

  private void applyToDocument(DocOp docOp) {
    JsonArray<TextChange> textChanges;

    isMutatingDocument = true;
    try {
      /*
       * Mutate via the document (instead of editor) since we don't want this to
       * be affected by autoindentation, etc. We also don't want to replace the
       * text in the selection, and the document's mutator will never do this.
       */
      textChanges = DocOpApplier.apply(docOp, document, document);
    } finally {
      isMutatingDocument = false;
    }

    // a retain only doc-op will not produce a text-change
    if (textChanges.size() == 0) {
      return;
    }

    /*
     * There can theoretically be multiple text changes, but we just set
     * selection to the first
     */
    TextChange textChange = textChanges.get(0);
    Position endPosition =
        new Position(new LineInfo(textChange.getEndLine(), textChange.getEndLineNumber()),
            textChange.getEndColumn());
    if (textChange.getType() == TextChange.Type.INSERT) {
      endPosition = PositionUtils.getPosition(endPosition, 1);
    }

    selection.setSelection(new LineInfo(textChange.getLine(), textChange.getLineNumber()),
        textChange.getColumn(), endPosition.getLineInfo(),
        endPosition.getColumn());
  }
}
