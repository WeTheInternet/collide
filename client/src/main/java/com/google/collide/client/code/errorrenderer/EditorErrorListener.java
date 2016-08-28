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
package com.google.collide.client.code.errorrenderer;

import com.google.collide.client.editor.Editor;
import com.google.collide.dto.CodeError;
import com.google.collide.dto.client.ClientDocOpFactory;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.ot.PositionMigrator;

/**
 * Connection point of Editor and Error Receiver.
 * Attaches to Editor to render received errors.
 */
public class EditorErrorListener implements ErrorReceiver.ErrorListener {

  /** An error receiver which never receives any errors */
  public static ErrorReceiver NOOP_ERROR_RECEIVER = new ErrorReceiver() {
    @Override
    public void setActiveDocument(String fileEditSessionKey) {}

    @Override
    public void addErrorListener(String fileEditSessionKey, ErrorListener listener) {}

    @Override
    public void removeErrorListener(String fileEditSessionKey, ErrorListener listener) {}
  };

  private final Editor editor;
  private final ErrorRenderer errorRenderer;
  private final ErrorReceiver errorReceiver;
  private final PositionMigrator positionMigrator;
  private String currentFileEditSessionKey;

  public EditorErrorListener(
      Editor editor, ErrorReceiver errorReceiver,
      ErrorRenderer errorRenderer) {
    this.editor = editor;
    this.errorReceiver = errorReceiver;
    this.errorRenderer = errorRenderer;
    this.positionMigrator = new PositionMigrator(ClientDocOpFactory.INSTANCE);
  }

  @Override
  public void onErrorsChanged(JsonArray<CodeError> newErrors) {
    if (editor.getDocument() == null) {
      return;
    }
    JsonArray<Line> linesToRender = JsoArray.create();
    getLinesOfErrorsInViewport(errorRenderer.getCodeErrors(), linesToRender);
    getLinesOfErrorsInViewport(newErrors, linesToRender);
    positionMigrator.reset();
    errorRenderer.setCodeErrors(newErrors, positionMigrator);

    for (int i = 0; i < linesToRender.size(); i++) {
      editor.getRenderer().requestRenderLine(linesToRender.get(i));
    }
    editor.getRenderer().renderChanges();
  }

  private void getLinesOfErrorsInViewport(JsonArray<CodeError> errors, JsonArray<Line> lines) {
    LineFinder lineFinder = editor.getDocument().getLineFinder();
    int topLineNumber = editor.getViewport().getTopLineNumber();
    int bottomLineNumber = editor.getViewport().getBottomLineNumber();
    for (int i = 0; i < errors.size(); i++) {
      CodeError error = errors.get(i);
      for (int j = error.getErrorStart().getLineNumber();
           j <= error.getErrorEnd().getLineNumber(); j++) {
        if (j >= topLineNumber && j <= bottomLineNumber) {
          lines.add(lineFinder.findLine(j).line());
        }
      }
    }
  }

  public void cleanup() {
    positionMigrator.stop();
    errorReceiver.removeErrorListener(currentFileEditSessionKey, this);
    currentFileEditSessionKey = null;
  }

  public void onDocumentChanged(Document document, String fileEditSessionKey) {
    if (currentFileEditSessionKey != null) {
      // We no longer want to listen for new errors in old file.
      errorReceiver.removeErrorListener(currentFileEditSessionKey, this);
    }

    currentFileEditSessionKey = fileEditSessionKey;
    positionMigrator.start(document.getTextListenerRegistrar());
    errorReceiver.addErrorListener(currentFileEditSessionKey, this);
    errorReceiver.setActiveDocument(currentFileEditSessionKey);
    errorRenderer.setCodeErrors(JsoArray.<CodeError>create(), positionMigrator);
    editor.addLineRenderer(errorRenderer);
  }
}
