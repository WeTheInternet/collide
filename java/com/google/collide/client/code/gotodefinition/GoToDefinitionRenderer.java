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

package com.google.collide.client.code.gotodefinition;

import javax.annotation.Nullable;

import com.google.collide.client.code.popup.EditorPopupController;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.renderer.LineRenderer;
import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.util.Elements;
import com.google.collide.json.client.JsoArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Objects;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.html.Element;
/**
 * A line renderer that highlights code references "on demand".
 * It always highlights only one reference at a time.
 *
 */
public class GoToDefinitionRenderer implements LineRenderer {

  private static final int POPUP_DELAY_MS = 500;

  /**
   * CssResource for the editor.
   */
  public interface Css extends CssResource {
    String referenceLink();

    String referencePopup();
  }

  /**
   * ClientBundle for the editor.
   */
  public interface Resources extends ClientBundle {
    @Source({"GoToDefinitionRenderer.css"})
    Css goToDefinitionCss();
  }

  private class SnippetPopupRenderer implements EditorPopupController.PopupRenderer {
    private final Element root = Elements.createDivElement();
    private final Element textContainer =
        (Element) root.appendChild(Elements.createPreElement(
            resources.goToDefinitionCss().referencePopup()));
    private String content;

    @Override
    public Element renderDom() {
      textContainer.setTextContent(content);
      return root;
    }

    public void setContent(String content) {
      this.content = content;
    }
  }

  private final Resources resources;
  private final Editor editor;
  private final EditorPopupController popupController;
  private final SnippetPopupRenderer snippetPopupRenderer;
  private EditorPopupController.Remover popupRemover;

  private int currentLineNumber;
  private int currentLineLength;
  // Current render position on current line.
  private int linePosition;
  // Reference that should be highlighted on next render.
  private NavigableReference highlightedReference;
  // Whether we have rendered the highlighted reference on current line.
  private boolean renderedHighlightedReference;

  public GoToDefinitionRenderer(Resources res, Editor editor,
      EditorPopupController popupController) {
    this.resources = res;
    this.editor = editor;
    this.popupController = popupController;
    
    snippetPopupRenderer = new SnippetPopupRenderer();
  }

  @Override
  public void renderNextChunk(LineRenderer.Target target) {
    if (highlightedReference == null || renderedHighlightedReference) {
      // No references to render. So render the rest of the line with null.
      renderNothingAndProceed(target, currentLineLength - linePosition);
    } else if (highlightedReference.getLineNumber() == currentLineNumber &&
        highlightedReference.getStartColumn() == linePosition) {
      // Reference starts at current line and current line position.
      int referenceLength;
      // Reference ends at current line.
      referenceLength = highlightedReference.getEndColumn() + 1 - linePosition;
      renderReferenceAndProceed(target, referenceLength);
    } else {
      // Wait until we get to the highlighted reference.
      renderNothingAndProceed(target, highlightedReference.getStartColumn() - linePosition);
    }
  }

  private void renderReferenceAndProceed(LineRenderer.Target target, int characterCount) {
    target.render(characterCount, resources.goToDefinitionCss().referenceLink());
    renderedHighlightedReference = true;
    linePosition += characterCount;
  }

  private void renderNothingAndProceed(LineRenderer.Target target, int characterCount) {
    target.render(characterCount, null);
    linePosition += characterCount;
  }

  @Override
  public boolean resetToBeginningOfLine(Line line, int lineNumber) {
    if (highlightedReference == null ||
        highlightedReference.getLineNumber() > lineNumber ||
        highlightedReference.getLineNumber() < lineNumber) {
      return false;
    }
    this.renderedHighlightedReference = false;
    this.currentLineNumber = lineNumber;
    this.currentLineLength = line.getText().length();
    this.linePosition = 0;
    return true;
  }

  @Override
  public boolean shouldLastChunkFillToRight() {
    return false;
  }

  /**
   * Highlights given reference. This automatically turns off highlighting
   * of the previously highlighed reference.
   * {@code renderChanges()} must be called to make the changes effective in UI.
   *
   * @param reference reference to highlight
   */
  public void highlightReference(@Nullable NavigableReference reference, Renderer renderer,
      LineFinder lineFinder) {
    if (Objects.equal(highlightedReference, reference)) {
      return;
    }
    // Request clear of currently highlighted reference.
    if (highlightedReference != null) {
      requestRenderReference(highlightedReference, renderer, lineFinder);
    }
    highlightedReference = reference;
    if (reference != null) {
      requestRenderReference(reference, renderer, lineFinder);
    } else {
      removeTooltip();
    }
  }

  /**
   * Un-highlights currently highlighted reference.
   */
  public void resetReferences(Renderer renderer, LineFinder lineFinder) {
    highlightReference(null, renderer, lineFinder);
  }

  private void requestRenderReference(NavigableReference reference, Renderer renderer,
      LineFinder lineFinder) {
    renderer.requestRenderLine(lineFinder.findLine(reference.getLineNumber()).line());
    createTooltipForReference(reference);
  }

  private void createTooltipForReference(NavigableReference reference) {
    removeTooltip();
    if (reference.getSnippet() == null && reference.getTargetName() == null) {
      return;
    }

    // TODO: Fix scroller appearing in front of cursor if snippet is too long.
    String tooltipContent = JsoArray.from(
        StringUtils.ensureNotEmpty(reference.getTargetName(), ""),
        StringUtils.ensureNotEmpty(reference.getSnippet(), "")).join("\n");
    snippetPopupRenderer.setContent(tooltipContent);
    LineInfo lineInfo = editor.getDocument().getLineFinder().findLine(reference.getLineNumber());
    popupRemover = popupController.showPopup(lineInfo, reference.getStartColumn(),
        reference.getEndColumn(), null, snippetPopupRenderer, null, VerticalAlign.TOP,
        false /* shouldCaptureOutsideClickOnClose */, POPUP_DELAY_MS);
  }

  private void removeTooltip() {
    if (popupRemover != null) {
      popupRemover.remove();
    }
  }
}
