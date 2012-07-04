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
import com.google.collide.client.editor.FocusManager;
import com.google.collide.client.editor.Spacer;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.ViewportModel.Edge;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.ScheduledCommandExecutor;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.document.util.PositionUtils;
import com.google.collide.shared.document.util.LineUtils.LineVisitor;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar.Remover;

import java.util.EnumSet;

/*-
 * TODO:
 * - store enough info so we can safely skip the render step if
 * this was off-screen
 */
/**
 * A class to track changes in the document or editor state that result in a
 * render pass.
 */
class ChangeTracker
    implements
      Document.TextListener,
      ViewportModel.Listener,
      SelectionModel.SelectionListener,
      Buffer.SpacerListener,
      FocusManager.FocusListener {

  enum ChangeType {
    /** The viewport's top or bottom are now pointing to different lines */
    VIEWPORT_SHIFT,
    /** The viewport had a line added or removed */
    VIEWPORT_CONTENT,
    /** The contents of a line has changed */
    DIRTY_LINE,
    /** The selection has changed */
    SELECTION,
    /** The viewport's top or bottom line numbers have changed */
    VIEWPORT_LINE_NUMBER
  }

  private class RenderCommand extends ScheduledCommandExecutor {
    @Override
    protected void execute() {
      /*
       * TODO: think about whether a render pass can cause a
       * change, if so, need to fix some stuff here like clearing/cloning the
       * changes BEFORE calling out to the renderer
       */

      try {
        renderer.renderChanges();
      } catch (Throwable t) {
        Log.error(getClass(), t);
      }

      clearChangeState();
    }
  }

  /*
   * TODO: More cleanly group the variables that track changed
   * state
   */
  /** Tracks the types of changes that occurred */
  private final EnumSet<ChangeType> changes;
  /**
   * Tracks whether there was a content change that requires updating the top of
   * existing following lines
   */
  private boolean hadContentChangeThatRepositionsFollowingLines;
  /** List of lines that need to be re-rendered */
  private final JsonArray<Line> dirtyLines;

  private final LineUtils.LineVisitor dirtyMarkingLineVisitor = new LineVisitor() {
    @Override
    public boolean accept(Line line, int lineNumber, int beginColumn, int endColumn) {
      requestRenderLine(line);
      return true;
    }
  };

  private final Buffer buffer;
  private final JsonArray<Remover> listenerRemovers;
  /** Command that is scheduled-finally from any callback */
  private final RenderCommand renderCommand = new RenderCommand();
  private final Renderer renderer;
  private final SelectionModel selection;
  private final ViewportModel viewport;
  /**
   * List of lines that were removed. These were in the viewport at time of
   * removal (and hence were most likely rendered)
   */
  private final JsonArray<Line> viewportRemovedLines;
  /**
   * The line number of the topmost line that was added or removed, or
   * {@value Integer#MAX_VALUE} if there weren't any of these changes
   */
  private int topmostContentChangedLineNumber;

  private final EnumSet<ViewportModel.Edge> viewportLineNumberChangedEdges = EnumSet
      .noneOf(ViewportModel.Edge.class);
  
  ChangeTracker(Renderer renderer, Buffer buffer, Document document, ViewportModel viewport,
      SelectionModel selection, FocusManager focusManager) {
    this.buffer = buffer;
    this.renderer = renderer;
    this.selection = selection;
    this.listenerRemovers = JsonCollections.createArray();
    this.changes = EnumSet.noneOf(ChangeType.class);
    this.viewportRemovedLines = JsonCollections.createArray();
    this.dirtyLines = JsonCollections.createArray();
    this.viewport = viewport;

    attach(buffer, document, viewport, selection, focusManager);

    clearChangeState();
  }

  public EnumSet<ChangeType> getChanges() {
    return changes;
  }

  public JsonArray<Line> getDirtyLines() {
    return dirtyLines;
  }

  public JsonArray<Line> getViewportRemovedLines() {
    return viewportRemovedLines;
  }

  public int getTopmostContentChangedLineNumber() {
    return topmostContentChangedLineNumber;
  }

  /**
   * Returns whether the {@link ChangeType#VIEWPORT_CONTENT} change type was one
   * that requires updating the position of the
   * {@link #getTopmostContentChangedLineNumber()} and all following
   * lines.
   */
  public boolean hadContentChangeThatUpdatesFollowingLines() {
    return hadContentChangeThatRepositionsFollowingLines;
  }
  
  public EnumSet<ViewportModel.Edge> getViewportLineNumberChangedEdges() {
    return viewportLineNumberChangedEdges;
  }

  @Override
  public void onSelectionChange(Position[] oldSelectionRange, Position[] newSelectionRange) {
    /*
     * We only need to redraw those lines that either entered or left the
     * selection
     */
    Position[] viewportRange = getViewportRange();
    if (oldSelectionRange != null && newSelectionRange != null) {
      JsonArray<Position[]> differenceRanges =
          PositionUtils.getDifference(oldSelectionRange, newSelectionRange);
      for (int i = 0, n = differenceRanges.size(); i < n; i++) {
        markVisibleLinesDirty(differenceRanges.get(i), viewportRange);
      }
    } else if (oldSelectionRange != null) {
      markVisibleLinesDirty(oldSelectionRange, viewportRange);
    } else if (newSelectionRange != null) {
      markVisibleLinesDirty(newSelectionRange, viewportRange);
    }
  }

  private void markVisibleLinesDirty(Position[] dirtyRange, Position[] viewportRange) {
    /*
     * We can safely intersect with the viewport. The typical danger in doing
     * this right now is by the time things render, the viewport could have
     * shifted. But, in that case, the new viewport will have to be rendered
     * anyway, and thus the selection in that new viewport would be drawn
     * regardless of any dirty-marking we do here.
     */
    Position[] visibleRange = PositionUtils.getIntersection(dirtyRange, viewportRange);
    if (visibleRange != null) {
      PositionUtils.visit(dirtyMarkingLineVisitor, visibleRange[0], visibleRange[1]);
    }
  }

  private Position[] getViewportRange() {
    return new Position[] {new Position(viewport.getTop(), 0),
        new Position(viewport.getBottom(), viewport.getBottomLine().getText().length() - 1)};
  }

  @Override
  public void onTextChange(Document document, JsonArray<TextChange> textChanges) {

    for (int i = 0, n = textChanges.size(); i < n; i++) {
      /*
       * For insertion, the second line through the second-to-last line can't
       * have existed in the document, so no point in marking them dirty.
       */
      TextChange textChange = textChanges.get(i);

      Line line = textChange.getLine();
      Line lastLine = textChange.getLastLine();

      if (dirtyLines.indexOf(line) == -1) {
        dirtyLines.add(line);
      }

      if (line != lastLine && dirtyLines.indexOf(lastLine) == -1) {
        dirtyLines.add(lastLine);
      }
    }

    scheduleRender(ChangeType.DIRTY_LINE);
  }

  @Override
  public void onViewportContentChanged(ViewportModel viewport, int lineNumber,
      boolean added, JsonArray<Line> lines) {
    int relevantContentChangedLineNumber;
    if (!added && viewport.getTopLineNumber() == lineNumber - 1) {
      // TODO: rework this case is handled naturally
      /*
       * This handles the top viewport line being "removed" by backspacing on
       * column 0. In this case, no one else draws the new top line of the
       * viewport (the one that got the previous top line's contents appended to
       * it).
       */
      relevantContentChangedLineNumber = viewport.getTopLineNumber();
    } else {
      relevantContentChangedLineNumber = lineNumber;
    }

    if (!added) {
      for (int i = 0, n = lines.size(); i < n; i++) {
        Line curLine = lines.get(i);
        if (ViewportRenderer.isRendered(curLine)) {
          viewportRemovedLines.add(curLine);
        }
      }
    }

    /*
     * If there is a spacer in the viewport below the line number change, line numbers shift
     * non-uniformly around it.
     */
    /*
     * TODO: actually implement the check for spacers in the viewport, not just
     * the document.
     */
    handleContentChange(relevantContentChangedLineNumber, buffer.hasSpacers());
  }

  @Override
  public void onViewportLineNumberChanged(ViewportModel viewport, Edge edge) {
    viewportLineNumberChangedEdges.add(edge);
    scheduleRender(ChangeType.VIEWPORT_LINE_NUMBER);
  }

  @Override
  public void onViewportShifted(ViewportModel viewport, LineInfo top, LineInfo bottom,
      LineInfo oldTop, LineInfo oldBottom) {
    scheduleRender(ChangeType.VIEWPORT_SHIFT);
  }

  @Override
  public void onFocusChange(boolean hasFocus) {
    /*
     * Schedule re-rendering of the lines in the selection so that we can update
     * the selection color based on focused state. Note that by the time we are
     * rendering, the selection could have changed. This is OK since in that
     * case, the new selection has to be rendered anyway, and it will render in
     * the correct color.
     */
    if (selection.hasSelection()) {
      markVisibleLinesDirty(selection.getSelectionRange(true), getViewportRange());
    }
  }

  public void requestRenderLine(Line line) {
    if (dirtyLines.indexOf(line) == -1) {
      dirtyLines.add(line);
    }

    scheduleRender(ChangeType.DIRTY_LINE);
  }

  void teardown() {
    for (int i = 0, n = listenerRemovers.size(); i < n; i++) {
      listenerRemovers.get(i).remove();
    }

    renderCommand.cancel();
  }

  private void attach(Buffer buffer, Document document, ViewportModel viewport,
      SelectionModel selection, FocusManager focusManager) {
    listenerRemovers.add(focusManager.getFocusListenerRegistrar().add(this));
    listenerRemovers.add(document.getTextListenerRegistrar().add(this));
    listenerRemovers.add(viewport.getListenerRegistrar().add(this));
    listenerRemovers.add(selection.getSelectionListenerRegistrar().add(this));
    listenerRemovers.add(buffer.getSpacerListenerRegistrar().add(this));
  }

  private void clearChangeState() {
    changes.clear();
    viewportRemovedLines.clear();
    dirtyLines.clear();
    topmostContentChangedLineNumber = Integer.MAX_VALUE;
    hadContentChangeThatRepositionsFollowingLines = false;
    viewportLineNumberChangedEdges.clear();
  }

  @Override
  public void onSpacerAdded(Spacer spacer) {
    handleContentChange(spacer.getLineNumber(), true);
  }

  @Override
  public void onSpacerRemoved(Spacer spacer, Line oldLine, int oldLineNumber) {
    handleContentChange(oldLineNumber, true);
  }

  @Override
  public void onSpacerHeightChanged(Spacer spacer, int oldHeight) {
    handleContentChange(spacer.getLineNumber(), true);
  }

  private void handleContentChange(int lineNumber, boolean requiresRepositioningFollowingLines) {
    hadContentChangeThatRepositionsFollowingLines |= requiresRepositioningFollowingLines;
    if (topmostContentChangedLineNumber > lineNumber) {
      topmostContentChangedLineNumber = lineNumber;
    }
    scheduleRender(ChangeType.VIEWPORT_CONTENT);
  }

  private void scheduleRender(ChangeType change) {
    changes.add(change);
    renderCommand.scheduleFinally();
  }
}
