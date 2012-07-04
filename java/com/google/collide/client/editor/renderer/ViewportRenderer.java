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

import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.ViewportModel.Edge;
import com.google.collide.client.editor.renderer.Renderer.LineLifecycleListener;
import com.google.collide.client.testing.DebugAttributeSetter;
import com.google.collide.client.util.Elements;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.gwt.user.client.Timer;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;

import java.util.EnumSet;

/*
 * TODO: I need to do another pass at the rendering paths after
 * having written the first round. There some edge cases that I handle, but not
 * in the cleanest way (these are mostly around joining lines at the top and
 * bottom of the viewport.)
 */

// Note: All Lines in viewport's range will have a cached line number
/**
 */
public class ViewportRenderer {

  /**
   * The animation controller deals with edge cases caused by multiple
   * animations being queued up, usually from the user holding down the enter or
   * backspace key so lines have their top rapidly changed.
   * 
   */
  private class AnimationController {
    private final Editor.View editorView;
    private boolean isAnimating = false;
    private boolean isDisabled = false;
    private final Timer animationFinishedTimer = new Timer() {
      @Override
      public void run() {
        isAnimating = false;
        if (isDisabled) {
          // Re-enable animation
          AnimationController.this.editorView.setAnimationEnabled(true);
          isDisabled = false;
        }
      }
    };

    AnimationController(Editor.View editorView) {
      this.editorView = editorView;
    }

    /**
     * This is called right before a group of DOM changes that would cause an
     * animation (ex: setting element top). If elements on the page are already
     * in the middle of an animation, disable animations for a short period,
     * then re-enable them. Keep rescheduling the re-enable timer on additional
     * calls.
     */
    void onBeforeAnimationStarted() {
      if (isAnimating) {
        if (!isDisabled) {
          editorView.setAnimationEnabled(false);
          isDisabled = true;
        }
        animationFinishedTimer.cancel();
      } else {
        isAnimating = true;
      }
      animationFinishedTimer.schedule(Editor.ANIMATION_DURATION);
    }
  }

  /** Key for a {@link Line#getTag} that stores the rendered DOM element */
  public static final String LINE_TAG_LINE_ELEMENT = "ViewportRenderer.element";

  /**
   * Key for a {@link Line#getTag} that stores a reference to the anchor that is
   * used to cache the line number for this line (since we cache line numbers
   * for lines in the viewport)
   */
  private static final String LINE_TAG_LINE_NUMBER_CACHE_ANCHOR =
      "ViewportRenderer.lineNumberCacheAnchor";

  private static final AnchorType OLD_VIEWPORT_ANCHOR_TYPE = AnchorType.create(
      ViewportRenderer.class, "Old viewport");

  private static final AnchorType VIEWPORT_RENDERER_ANCHOR_TYPE = AnchorType.create(
      ViewportRenderer.class, "viewport renderer");

  public static boolean isRendered(Line line) {
    return getLineElement(line) != null;
  }

  private static Element getLineElement(Line line) {
    return line.getTag(LINE_TAG_LINE_ELEMENT);
  }

  /**
   * Control queued animations by disabling future animations temporarily if
   * another animation happens in here.
   */
  private final AnimationController animationController;
  private final Buffer buffer;
  private final Document document;
  private final ListenerManager<LineLifecycleListener> lineLifecycleListenerManager;
  private final LineRendererController lineRendererController;
  private final ViewportModel viewport;

  /**
   * The bottom of the viewport when last rendered, or null if the viewport
   * hasn't been rendered yet
   */
  private Anchor viewportOldBottomAnchor;
  /**
   * The top of the viewport when last rendered, or null if the viewport hasn't
   * been rendered yet
   */
  private Anchor viewportOldTopAnchor;

  ViewportRenderer(Document document, Buffer buffer, ViewportModel viewport,
      Editor.View editorView, ListenerManager<LineLifecycleListener> lineLifecycleListenerManager) {
    this.document = document;
    this.buffer = buffer;
    this.lineLifecycleListenerManager = lineLifecycleListenerManager;
    this.lineRendererController = new LineRendererController(buffer);
    this.viewport = viewport;
    this.animationController = new AnimationController(editorView);
  }

  private void placeOldViewportAnchors() {
    AnchorManager anchorManager = document.getAnchorManager();
    if (viewportOldTopAnchor == null) {
      viewportOldTopAnchor =
          anchorManager.createAnchor(OLD_VIEWPORT_ANCHOR_TYPE, viewport.getTopLine(),
              viewport.getTopLineNumber(), AnchorManager.IGNORE_COLUMN);
      viewportOldTopAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);

      viewportOldBottomAnchor =
          anchorManager.createAnchor(OLD_VIEWPORT_ANCHOR_TYPE, viewport.getBottomLine(),
              viewport.getBottomLineNumber(), AnchorManager.IGNORE_COLUMN);
      viewportOldBottomAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);

    } else {
      anchorManager.moveAnchor(viewportOldTopAnchor, viewport.getTopLine(),
          viewport.getTopLineNumber(), AnchorManager.IGNORE_COLUMN);
      anchorManager.moveAnchor(viewportOldBottomAnchor, viewport.getBottomLine(),
          viewport.getBottomLineNumber(), AnchorManager.IGNORE_COLUMN);
    }
  }

  void addLineRenderer(LineRenderer lineRenderer) {
    lineRendererController.addLineRenderer(lineRenderer);
  }

  void removeLineRenderer(LineRenderer lineRenderer) {
    lineRendererController.removeLineRenderer(lineRenderer);
  }

  void render() {
    renderViewportShift(true);
  }

  /**
   * Re-renders the lines marked as dirty. If the line was not previously
   * rendered, it will not be rendered here.
   */
  void renderDirtyLines(JsonArray<Line> dirtyLines) {
    int maxLineLength = buffer.getMaxLineLength();
    int newMaxLineLength = maxLineLength;

    for (int i = 0, n = dirtyLines.size(); i < n; i++) {
      Line line = dirtyLines.get(i);
      Element lineElement = getLineElement(line);
      if (lineElement == null) {
        /*
         * The line may have been in the viewport when marked as dirty, but it
         * is not anymore
         */
        continue;
      }

      if (buffer.hasLineElement(lineElement)) {
        lineRendererController.renderLine(line, LineUtils.getCachedLineNumber(line), lineElement,
            false);
      }

      int lineLength = line.getText().length();
      if (lineLength > newMaxLineLength) {
        newMaxLineLength = lineLength;
      }
    }

    if (newMaxLineLength != maxLineLength) {
      // TODO: need to shrink back down if the max line shrinks
      buffer.setMaxLineLength(newMaxLineLength);
    }
  }

  /**
   * Renders changes to the content of the viewport at a line level, for example
   * line removals or additions.
   * 
   * @param removedLines these lines were in the viewport at time of removal
   */
  void renderViewportContentChange(int beginLineNumber, JsonArray<Line> removedLines) {

    // Garbage collect the elements of removed lines
    for (int i = 0, n = removedLines.size(); i < n; i++) {
      Line line = removedLines.get(i);
      garbageCollectLine(line);
    }
    /*
     * New lines being rendered were at +createOffset below the viewport before
     * this render pass.
     */
    /*
     * TODO: This won't be correct if deletion DocOps from the
     * frontend are also applied in the same rendering pass as a deletion.
     */
    int createOffset = removedLines.size() * buffer.getEditorLineHeight();
    if (beginLineNumber <= viewport.getBottomLineNumber()) {
      // Only fill or update lines if the content change affects the viewport
      LineInfo beginLine =
          viewport.getDocument().getLineFinder().findLine(viewport.getBottom(), beginLineNumber);
      animationController.onBeforeAnimationStarted();
      fillOrUpdateLines(beginLine.line(), beginLine.number(), viewport.getBottomLine(),
          viewport.getBottomLineNumber(), createOffset);
    }
  }

  void renderViewportLineNumbersChanged(EnumSet<Edge> changedEdges) {
    if (changedEdges.contains(ViewportModel.Edge.TOP)) {
      /*
       * Collaborator added/removed lines above us, update the viewport lines
       * since their tops may have changed
       */
      LineInfo top = viewport.getTop();
      LineInfo bottom = viewport.getBottom();
      fillOrUpdateLines(top.line(), top.number(), bottom.line(), bottom.number(), 0);
    }
  }
  
  /**
   * Renders changes to the viewport positioning.
   */
  void renderViewportShift(boolean forceRerender) {
    LineInfo oldTop = viewportOldTopAnchor != null ? viewportOldTopAnchor.getLineInfo() : null;
    LineInfo oldBottom =
        viewportOldBottomAnchor != null ? viewportOldBottomAnchor.getLineInfo() : null;

    LineInfo top = viewport.getTop();
    LineInfo bottom = viewport.getBottom();

    if (oldTop == null || oldBottom == null || bottom.number() < oldTop.number()
        || top.number() > oldBottom.number() || forceRerender) {
      /*
       * The viewport does not overlap with its old position, GC old lines and
       * render all of the new ones (or we are forced to rerender everything)
       */
      if (oldTop != null && oldBottom != null) {
        garbageCollectLines(oldTop.line(), oldTop.number(), oldBottom.line(), oldBottom.number());
      }

      fillOrUpdateLines(top.line(), top.number(), bottom.line(), bottom.number(), 0);

    } else {
      // There is some overlap, so be more efficient with our update
      if (oldTop.number() < top.number()) {
        // The viewport moved down, need to GC the offscreen lines
        garbageCollectLines(top.line().getPreviousLine(), top.number() - 1, oldTop.line(),
            oldTop.number());
      } else if (oldTop.number() > top.number()) {
        // The viewport moved up, need to fill with lines
        fillOrUpdateLines(top.line(), top.number(), oldTop.line().getPreviousLine(),
            oldTop.number() - 1, 0);
      }

      if (oldBottom.number() < bottom.number()) {
        // The viewport moved down, need to fill with lines
        fillOrUpdateLines(oldBottom.line().getNextLine(), oldBottom.number() + 1, bottom.line(),
            bottom.number(), 0);
      } else if (oldBottom.number() > bottom.number()) {
        // The viewport moved up, need to GC the offscreen lines
        garbageCollectLines(bottom.line().getNextLine(), bottom.number() + 1, oldBottom.line(),
            oldBottom.number());
      }
    }
  }

  /**
   * Once torn down, this instance cannot be used again.
   */
  void teardown() {
  }

  /**
   * Fills the buffer in the given range (inclusive).
   * 
   * @param beginLineNumber the line number to start filling from
   * @param endLineNumber the line number of last line (inclusive) to finish
   *        filling
   * @param createOffset the offset in pixels that this line would be at before
   *        this rendering pass, used to animate in from offscreen
   */
  public void fillOrUpdateLines(Line beginLine, int beginLineNumber, Line endLine,
      int endLineNumber, int createOffset) {
    Line curLine = beginLine;
    int curLineNumber = beginLineNumber;
    if (curLineNumber <= endLineNumber) {
      for (; curLineNumber <= endLineNumber && curLine != null; curLineNumber++) {
        createOrUpdateLineElement(curLine, curLineNumber, createOffset);
        curLine = curLine.getNextLine();
      }
    } else {
      for (; curLineNumber >= endLineNumber && curLine != null; curLineNumber--) {
        createOrUpdateLineElement(curLine, curLineNumber, createOffset);
        curLine = curLine.getPreviousLine();
      }
    }
  }

  private void garbageCollectLine(final Line line) {
    lineLifecycleListenerManager.dispatch(new Dispatcher<Renderer.LineLifecycleListener>() {
      @Override
      public void dispatch(LineLifecycleListener listener) {
        listener.onRenderedLineGarbageCollected(line);
      }
    });

    Element element = line.getTag(LINE_TAG_LINE_ELEMENT);
    if (element != null && buffer.hasLineElement(element)) {
      element.removeFromParent();
      line.putTag(LINE_TAG_LINE_ELEMENT, null);
    }

    handleLineLeftViewport(line);
  }

  /*
   * TODO: consider taking LineInfo and a offset for each of begin
   * and end. Callers right now do the offset manually, but that might lead to
   * them giving us a null Line or out-of-bounds line number
   */
  public void garbageCollectLines(Line beginLine, int beginNumber, Line endLine, int endNumber) {

    if (beginNumber > endNumber) {
      // Swap so beginNumber < endNumber
      Line tmpLine = beginLine;
      int tmpNumber = beginNumber;
      beginLine = endLine;
      beginNumber = endNumber;
      endLine = tmpLine;
      endNumber = tmpNumber;
    }

    Line curLine = beginLine;
    for (int curNumber = beginNumber; curNumber <= endNumber && curLine != null; curNumber++) {
      garbageCollectLine(curLine);
      curLine = curLine.getNextLine();
    }
  }

  private Element createOrUpdateLineElement(final Line line, final int lineNumber, 
      int createOffset) {
    int top = buffer.calculateLineTop(lineNumber);
    Element element = getLineElement(line);
    boolean isCreatingElement = element == null;
    if (isCreatingElement) {
      element = Elements.createDivElement();
      element.getStyle().setPosition(CSSStyleDeclaration.Position.ABSOLUTE);
      lineRendererController.renderLine(line, lineNumber, element, true);
      line.putTag(LINE_TAG_LINE_ELEMENT, element);
    }
    new DebugAttributeSetter().add("lineNum", Integer.toString(lineNumber)).on(element);

    if (!buffer.hasLineElement(element)) {
      element.getStyle().setTop(top + createOffset, CSSStyleDeclaration.Unit.PX);
      buffer.addLineElement(element);
      if (createOffset != 0) {
        /*
         * TODO: When enabling editor animations, reinvestigate
         * need for below
         */
        // Force a browser layout so CSS3 animations transition properly.
        //element.getClientWidth();
        element.getStyle().setTop(top, CSSStyleDeclaration.Unit.PX);
      }
      handleLineEnteredViewport(line, lineNumber, element);
    } else {
      element.getStyle().setTop(top, CSSStyleDeclaration.Unit.PX);
    }

    if (isCreatingElement) {
      lineLifecycleListenerManager.dispatch(new Dispatcher<Renderer.LineLifecycleListener>() {
        @Override
        public void dispatch(LineLifecycleListener listener) {
          listener.onRenderedLineCreated(line, lineNumber);
        }
      });
    } else {
      lineLifecycleListenerManager.dispatch(new Dispatcher<Renderer.LineLifecycleListener>() {
        @Override
        public void dispatch(LineLifecycleListener listener) {
          listener.onRenderedLineShifted(line, lineNumber);
        }
      });
    }

    return element;
  }

  private void handleLineEnteredViewport(Line line, int lineNumber, Element lineElement) {
    assert line.getTag(LINE_TAG_LINE_NUMBER_CACHE_ANCHOR) == null;

    Anchor anchor =
        line.getDocument()
            .getAnchorManager()
            .createAnchor(VIEWPORT_RENDERER_ANCHOR_TYPE, line, lineNumber,
                AnchorManager.IGNORE_COLUMN);

    // Stash this anchor as a line tag so we can remove it easily
    line.putTag(LINE_TAG_LINE_NUMBER_CACHE_ANCHOR, anchor);

    int lineLength = line.getText().length();
    if (lineLength > buffer.getMaxLineLength()) {
      buffer.setMaxLineLength(lineLength);
    }
  }

  private void handleLineLeftViewport(Line line) {
    Anchor anchor = line.getTag(LINE_TAG_LINE_NUMBER_CACHE_ANCHOR);
    if (anchor == null) {
      /*
       * The line was in the viewport when the change occurred, but never
       * rendered with it there, so it never got a line number cache anchor
       * assigned
       */
      return;
    }

    line.getDocument().getAnchorManager().removeAnchor(anchor);
    line.putTag(LINE_TAG_LINE_NUMBER_CACHE_ANCHOR, null);
  }

  void handleRenderCompleted() {
    placeOldViewportAnchors();
  }
}
