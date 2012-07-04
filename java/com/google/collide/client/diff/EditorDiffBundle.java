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

package com.google.collide.client.diff;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.EditorBundle;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.syntaxhighlighter.SyntaxHighlighter;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.UserActivityManager;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.codemirror2.Parser;
import com.google.collide.shared.document.Document;
import com.google.common.base.Preconditions;

/**
 * Like the {@link EditorBundle} this class groups together the various editor
 * components.
 *
 */
public class EditorDiffBundle {
  private final Editor editor;
  private DocumentParser parser;
  private SyntaxHighlighter syntaxHighlighter;
  private DiffRenderer diffRenderer;
  private final UserActivityManager userActivityManager;
  private final Editor.Css editorCss;

  public EditorDiffBundle(AppContext appContext) {
    this.editor = Editor.create(appContext);
    this.userActivityManager = appContext.getUserActivityManager();
    this.editorCss = appContext.getResources().workspaceEditorCss();

    this.editor.setLeftGutterVisible(false);
    this.editor.getBuffer().setColumnMarkerVisibility(false);
  }

  public DiffRenderer getDiffRenderer() {
    return diffRenderer;
  }

  public Editor getEditor() {
    return editor;
  }

  public SyntaxHighlighter getSyntaxHighlighter() {
    return syntaxHighlighter;
  }

  /**
   * Replaces the document for the editor and related components.
   */
  public void setDocument(Document document, PathUtil path, DiffRenderer diffRenderer) {
    this.diffRenderer = diffRenderer;

    if (syntaxHighlighter != null) {
      editor.removeLineRenderer(syntaxHighlighter.getRenderer());
      syntaxHighlighter.teardown();
      syntaxHighlighter = null;
      parser.teardown();
      parser = null;
    }


    Parser codeMirrorParser = CodeMirror2.getParser(path);
    parser = DocumentParser.create(document, codeMirrorParser, userActivityManager);
    Preconditions.checkNotNull(parser);

    editor.setDocument(document);
    editor.addLineRenderer(diffRenderer);

    syntaxHighlighter =
        SyntaxHighlighter.create(document, editor.getRenderer(), editor.getViewport(),
            editor.getSelection(), parser, editorCss);
    editor.addLineRenderer(syntaxHighlighter.getRenderer());

    parser.begin();
  }
}
