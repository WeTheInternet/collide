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

package com.google.collide.shared.ot;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocOpComponent;
import com.google.collide.dto.DocOpComponent.Delete;
import com.google.collide.dto.DocOpComponent.Insert;
import com.google.collide.dto.DocOpComponent.Retain;
import com.google.collide.dto.DocOpComponent.RetainLine;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;


/**
 */
public class DocOpApplier {

  public static JsonArray<TextChange> apply(DocOp docOp, Document document) {
    return apply(docOp, document, document);
  }

  public static JsonArray<TextChange> apply(DocOp docOp, Document document,
      DocumentMutator documentMutator) {
    DocOpApplier docOpApplier = new DocOpApplier(docOp, document, documentMutator);
    docOpApplier.apply();
    return docOpApplier.textChanges;
  }

  private int column;
  private final JsonArray<DocOpComponent> components;
  private int componentIndex;
  private final Document document;
  private DocumentMutator documentMutator;

  /**
   * If true, we are definitely finished and cannot accepts any more doc op
   * components.
   */
  private boolean isFinished;

  private LineInfo lineInfo;
  private final JsonArray<TextChange> textChanges = JsonCollections.createArray();

  private DocOpApplier(DocOp docOp, Document document, DocumentMutator documentMutator) {
    this.components = docOp.getComponents();
    this.document = document;
    this.documentMutator = documentMutator;

    lineInfo = new LineInfo(document.getFirstLine(), 0);
  }

  public void apply() {
    for (; componentIndex < components.size(); componentIndex++) {
      DocOpComponent component = components.get(componentIndex);

      switch (component.getType()) {
        case DocOpComponent.Type.INSERT:
          handleInsert((Insert) component);
          break;

        case DocOpComponent.Type.DELETE:
          handleDelete((Delete) component);
          break;

        case DocOpComponent.Type.RETAIN:
          handleRetain((Retain) component);
          break;

        case DocOpComponent.Type.RETAIN_LINE:
          handleRetainLine((RetainLine) component);
          break;
      }
    }
  }

  private void handleDelete(Delete deleteOp) {
    Preconditions.checkArgument(!isFinished, "Unexpected finished while handling delete");
    Preconditions.checkArgument(lineInfo.line().getText().substring(column).startsWith(
        deleteOp.getText()), "To-be-deleted text isn't actually at location");

    StringBuilder text = new StringBuilder(deleteOp.getText());
    while (componentIndex + 1 < components.size()
        && components.get(componentIndex + 1).getType() == DocOpComponent.Type.DELETE) {
      componentIndex++;
      text.append(((Delete) components.get(componentIndex)).getText());
    }

    addTextChange(documentMutator.deleteText(lineInfo.line(), lineInfo.number(), column,
        text.length()));
  }

  private void handleInsert(Insert insertOp) {
    Preconditions.checkArgument(!isFinished, "Unexpected finished while handling insert");

    StringBuilder text = new StringBuilder();
    int newLineDelta = 0;
    int newColumn = column;

    // Offset the componentIndex for the first iteration
    componentIndex--;

    do {
      componentIndex++;
      String insertOpText = ((Insert) components.get(componentIndex)).getText();
      text.append(insertOpText);

      if (insertOpText.endsWith("\n")) {
        newLineDelta++;
        newColumn = 0;
      } else {
        newColumn += insertOpText.length();
      }
    } while (componentIndex + 1 < components.size()
        && components.get(componentIndex + 1).getType() == DocOpComponent.Type.INSERT);

    addTextChange(documentMutator.insertText(lineInfo.line(), lineInfo.number(), column,
        text.toString()));

    for (; newLineDelta > 0; newLineDelta--) {
      moveToNextLine();
    }

    column = newColumn;
  }

  private void addTextChange(TextChange textChange) {
    if (textChange != null) {
      textChanges.add(textChange);
    }
  }

  private void handleRetain(Retain retainOp) {
    Preconditions.checkArgument(!isFinished, "Unexpected finished while handling retain");

    if (retainOp.hasTrailingNewline()) {
      moveToNextLine();
    } else {
      column += retainOp.getCount();
    }
  }

  private void handleRetainLine(RetainLine retainLineOp) {
    Preconditions.checkArgument(!isFinished, "Unexpected finished while handling retain line");

    int lineCount = retainLineOp.getLineCount();
    int newLineNumber = lineInfo.number() + lineCount;
    if (newLineNumber < document.getLineCount()) {
      lineInfo = document.getLineFinder().findLine(lineInfo.number() + lineCount);
      column = 0;
    } else {
      // We have spanned the entire document
      isFinished = true;
    }
  }

  private void moveToNextLine() {
    boolean didMove = lineInfo.moveToNext();
    Preconditions.checkArgument(didMove, "Did not actually move to next line");

    column = 0;
  }
}
