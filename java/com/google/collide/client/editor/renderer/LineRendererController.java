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

package com.google.collide.client.editor.renderer;

import com.google.collide.client.document.linedimensions.LineDimensionsUtils;
import com.google.collide.client.editor.Buffer;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.SortedList;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;
import elemental.html.SpanElement;

/**
 * A class to maintain the list of {@link LineRenderer LineRenderers} and render
 * a line by delegating to each of the renderers.
 */
class LineRendererController {

  /*
   * TODO: consider recycling these if GC performance during
   * rendering is an issue
   */
  private static class LineRendererTarget implements LineRenderer.Target {

    private static class Comparator implements SortedList.Comparator<LineRendererTarget> {
      @Override
      public int compare(LineRendererTarget a, LineRendererTarget b) {
        return a.remainingCount - b.remainingCount;
      }
    }

    /** The line renderer for which this is the target */
    private final LineRenderer lineRenderer;

    /**
     * The remaining number of characters that should receive {@link #styleName}
     * . Once this is 0, the {@link #lineRenderer} will be asked to render its
     * next chunk
     */
    private int remainingCount;

    /** The style to be applied to the {@link #remainingCount} */
    private String styleName;

    public LineRendererTarget(LineRenderer lineRenderer) {
      this.lineRenderer = lineRenderer;
    }

    @Override
    public void render(int characterCount, String styleName) {
      remainingCount = characterCount;
      this.styleName = styleName;
    }
  }

  /**
   * A sorted list storing targets for the line renderers that are participating
   * in rendering the current line
   */
  private final SortedList<LineRendererTarget> currentLineRendererTargets;

  /**
   * A list of all of the line renderers that are registered on the editor (Note
   * that some may not be participating in the current line)
   */
  private final JsonArray<LineRenderer> lineRenderers;
  private final Buffer buffer;

  LineRendererController(Buffer buffer) {
    this.buffer = buffer;
    currentLineRendererTargets = new SortedList<LineRendererController.LineRendererTarget>(
        new LineRendererTarget.Comparator());
    lineRenderers = JsonCollections.createArray();
  }

  void addLineRenderer(LineRenderer lineRenderer) {
    if (!lineRenderers.contains(lineRenderer)) {
      /*
       * Prevent line renderer from appearing twice in the list if it is already
       * added.
       */
      lineRenderers.add(lineRenderer);
    }
  }

  void removeLineRenderer(LineRenderer lineRenderer) {
    lineRenderers.remove(lineRenderer);
  }

  void renderLine(Line line, int lineNumber, Element targetElement, boolean isTargetElementEmpty) {

    currentLineRendererTargets.clear();

    if (!resetLineRenderers(line, lineNumber)) {
      // No line renderers are participating, so exit early.
      setTextContentSafely(targetElement, line.getText());
      return;
    }

    if (!isTargetElementEmpty) {
      targetElement.setInnerHTML("");
    }

    Element contentElement = Elements.createSpanElement();
    contentElement.getStyle().setDisplay(CSSStyleDeclaration.Display.INLINE_BLOCK);
    for (int indexInLine = 0, lineSize = line.getText().length();
        indexInLine < lineSize && ensureAllRenderersHaveARenderedNextChunk();) {

      int chunkSize = currentLineRendererTargets.get(0).remainingCount;
      if (chunkSize == 0) {
        // Bad news, revert to naive rendering and log
        setTextContentSafely(targetElement, line.getText());
        Log.error(getClass(), "Line renderers do not have remaining chunks");
        return;
      }

      renderChunk(line.getText(), indexInLine, chunkSize, contentElement);
      markChunkRendered(chunkSize);

      indexInLine += chunkSize;
    }
    targetElement.appendChild(contentElement);

    if (line.getText().endsWith("\n")) {
      Element lastChunk = (Element) contentElement.getLastChild();
      Preconditions.checkState(lastChunk != null, "This line has no chunks!");
      if (!StringUtils.isNullOrWhitespace(lastChunk.getClassName())) {
        contentElement.getStyle().setProperty("float", "left");
        Element newlineCharacterElement = createLastChunkElement(targetElement);
        // Created on demand only because it is rarely used.
        Element remainingSpaceElement = null;
        for (int i = 0, n = currentLineRendererTargets.size(); i < n; i++) {
          LineRendererTarget target = currentLineRendererTargets.get(i);
          if (target.styleName != null) {
            if (!target.lineRenderer.shouldLastChunkFillToRight()) {
              newlineCharacterElement.addClassName(target.styleName);
            } else {
              if (remainingSpaceElement == null) {
                newlineCharacterElement.getStyle().setProperty("float", "left");
                remainingSpaceElement = createLastChunkElement(targetElement);
                remainingSpaceElement.getStyle().setWidth("100%");
              }
              // Also apply to last chunk element so that there's no gap.
              newlineCharacterElement.addClassName(target.styleName);
              remainingSpaceElement.addClassName(target.styleName);
            }
          }
        }
      }
    }
  }

  private static Element createLastChunkElement(Element parent) {
    // we need to give them a whitespace element so that it can be styled.
    Element whitespaceElement = Elements.createSpanElement();
    whitespaceElement.setTextContent("\u00A0");
    whitespaceElement.getStyle().setDisplay("inline-block");
    parent.appendChild(whitespaceElement);
    return whitespaceElement;
  }

  /**
   * Ensures all renderer targets (that want to render) have rendered each of
   * their next chunks.
   */
  private boolean ensureAllRenderersHaveARenderedNextChunk() {
    while (currentLineRendererTargets.size() > 0
        && currentLineRendererTargets.get(0).remainingCount == 0) {
      LineRendererTarget target = currentLineRendererTargets.get(0);
      try {
        target.lineRenderer.renderNextChunk(target);
      } catch (Throwable t) {
        // Cause naive rendering
        target.remainingCount = 0;
        Log.warn(getClass(), "An exception was thrown from renderNextChunk", t);
      }
      
      if (target.remainingCount > 0) {
        currentLineRendererTargets.repositionItem(0);
      } else {
        // Remove the line renderer because it has broken our contract
        currentLineRendererTargets.remove(0);
        Log.warn(getClass(), "The line renderer " + target.lineRenderer
            + " is lacking a next chunk, removing from rendering");
      }
    }

    return currentLineRendererTargets.size() > 0;
  }

  /**
   * Marks the chunk rendered on all the renderers.
   */
  private void markChunkRendered(int chunkSize) {
    for (int i = 0, n = currentLineRendererTargets.size(); i < n; i++) {
      LineRendererTarget target = currentLineRendererTargets.get(i);
      target.remainingCount -= chunkSize;
    }
  }

  /**
   * Renders the chunk by creating a span with all of the individual line
   * renderer's styles.
   */
  private void renderChunk(String lineText, int lineIndex, int chunkLength, Element targetElement) {
    SpanElement element = Elements.createSpanElement();
    // TODO: file a Chrome bug, place link here
    element.getStyle().setDisplay(CSSStyleDeclaration.Display.INLINE_BLOCK);
    setTextContentSafely(element, lineText.substring(lineIndex, lineIndex + chunkLength));
    applyStyles(element);
    targetElement.appendChild(element);
  }

  private void applyStyles(Element element) {
    for (int i = 0, n = currentLineRendererTargets.size(); i < n; i++) {
      LineRendererTarget target = currentLineRendererTargets.get(i);
      if (target.styleName != null) {
        element.addClassName(target.styleName);
      }
    }
  }

  /**
   * Resets the line renderers, preparing for a new line to be rendered.
   *
   * This method fills the {@link #currentLineRendererTargets} with targets for
   * line renderers that will participate in rendering this line.
   *
   * @return true if there is at least one line renderer participating for the
   *         given @{link Line} line.
   */
  private boolean resetLineRenderers(Line line, int lineNumber) {
    boolean hasAtLeastOneParticipatingLineRenderer = false;
    for (int i = 0; i < lineRenderers.size(); i++) {
      LineRenderer lineRenderer = lineRenderers.get(i);
      boolean isParticipating = lineRenderer.resetToBeginningOfLine(line, lineNumber);
      if (isParticipating) {
        currentLineRendererTargets.add(new LineRendererTarget(lineRenderer));
        hasAtLeastOneParticipatingLineRenderer = true;
      }
    }

    return hasAtLeastOneParticipatingLineRenderer;
  }

  private void setTextContentSafely(Element element, String text) {
    String cleansedText = text.replaceAll("\t", LineDimensionsUtils.getTabAsSpaces());
    element.setTextContent(cleansedText);
  }
}
