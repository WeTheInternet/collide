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

package com.google.collide.client.syntaxhighlighter;

import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.renderer.LineRenderer;
import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar;

import javax.annotation.Nonnull;

/**
 * Syntax highlighter for the Collide editor.
 *
 */
public class SyntaxHighlighter implements DocumentParser.Listener, Renderer.CompletionListener {

  /**
   * Key for {@link Line#getTag} that stores the parsed tokens for that line. We
   * must cache these because of the asynchronous nature of rendering. Once the
   * rendering pass is complete, we clear this cache. So, this cache gets cleared
   * before the browser event loop is run.
   */
  private static final String LINE_TAG_CACHED_TOKENS = "SyntaxHighlighter.cachedTokens";

  public static SyntaxHighlighter create(Document document, Renderer renderer,
      ViewportModel viewport, SelectionModel selection, DocumentParser documentParser,
      Editor.Css editorCss) {
    ListenerRegistrar.RemoverManager removerManager = new ListenerRegistrar.RemoverManager();
    SyntaxHighlighter syntaxHighlighter = new SyntaxHighlighter(document,
        renderer,
        viewport,
        selection,
        documentParser,
        removerManager,
        editorCss);
    removerManager.track(documentParser.getListenerRegistrar().add(syntaxHighlighter));
    removerManager.track(renderer.getCompletionListenerRegistrar().add(syntaxHighlighter));

    return syntaxHighlighter;
  }

  private final Renderer editorRenderer;
  private final SyntaxHighlighterRenderer lineRenderer;
  private final ViewportModel viewport;
  private final JsonArray<Line> linesWithCachedTokens;
  private final DocumentParser documentParser;
  private final ListenerRegistrar.RemoverManager removerManager;

  private SyntaxHighlighter(Document document,
      Renderer editorRenderer,
      ViewportModel viewport,
      SelectionModel selection,
      DocumentParser documentParser,
      ListenerRegistrar.RemoverManager removerManager,
      Editor.Css editorCss) {
    this.editorRenderer = editorRenderer;
    this.viewport = viewport;
    this.documentParser = documentParser;
    this.removerManager = removerManager;
    this.linesWithCachedTokens = JsonCollections.createArray();
    this.lineRenderer = new SyntaxHighlighterRenderer(this, selection, editorCss);
  }

  public LineRenderer getRenderer() {
    return lineRenderer;
  }

  @Override
  public void onIterationStart(int lineNumber) {
    // do nothing
  }

  @Override
  public void onIterationFinish() {
    // do nothing
  }

  @Override
  public void onDocumentLineParsed(Line line, int lineNumber, @Nonnull JsonArray<Token> tokens) {
    if (!viewport.isLineInViewport(line)) {
      return;
    }

    // Save the cached tokens so the async render will have them accessible
    line.putTag(LINE_TAG_CACHED_TOKENS, tokens);
    linesWithCachedTokens.add(line);

    editorRenderer.requestRenderLine(line);
  }

  @Override
  public void onRenderCompleted() {
    // Wipe the cached tokens
    for (int i = 0, n = linesWithCachedTokens.size(); i < n; i++) {
      linesWithCachedTokens.get(i).putTag(LINE_TAG_CACHED_TOKENS, null);
    }

    linesWithCachedTokens.clear();
  }

  public void teardown() {
    removerManager.remove();
  }

  /**
   * Returns the tokens for the given line, or null if the tokens could not be
   * retrieved synchronously
   */
  JsonArray<Token> getTokens(Line line) {
    JsonArray<Token> tokens = line.getTag(LINE_TAG_CACHED_TOKENS);
    /*
     * If we haven't gotten a callback from the parser (hence no cached tokens),
     * try to synchronously parse the line
     */
    return tokens != null ? tokens : documentParser.parseLineSync(line);
  }
}
