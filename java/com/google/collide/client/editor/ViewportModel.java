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
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.MathUtils;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * The model for the editor's viewport. This model also listens for events that
 * affect the viewport, and adjusts the bounds accordingly. For example, it
 * listens as a scroll listener and shifts the viewport when the user scrolls.
 *
 * The lifecycle of this class is tied to the current document in the editor. If
 * the document is replaced, a new instance of this class is used for the new
 * document.
 */
public class ViewportModel
    implements
      Buffer.ScrollListener,
      Buffer.ResizeListener,
      Document.LineListener,
      SelectionModel.CursorListener,
      Buffer.SpacerListener {

  private static final AnchorType VIEWPORT_MODEL_ANCHOR_TYPE = AnchorType.create(
      ViewportModel.class, "viewport model");

  /**
   * Listener that is notified of viewport changes.
   */
  public interface Listener {
    /**
     * Called when the content of the viewport changes, for example a line gets
     * added or removed. This will not be called if the text changes within a
     * line.
     *
     * @param added true if the given {@code lineInfo} was added, false if
     *        removed (note that when removed, the lines will no longer be in
     *        the document)
     * @param lines the lines added or removed (see the parameters on
     *        {@link Document.LineListener})
     */
    void onViewportContentChanged(ViewportModel viewport, int lineNumber, boolean added,
        JsonArray<Line> lines);

    /**
     * Called when the viewport is shifted, meaning at least one of its edges
     * points to new lines.
     *
     * @param oldTop may be null if this is the first range set
     * @param oldBottom may be null if this is the first range set
     */
    void onViewportShifted(ViewportModel viewport, LineInfo top, LineInfo bottom, LineInfo oldTop,
        LineInfo oldBottom);

    /**
     * Called when the line number of the viewport top or bottom changes.
     *
     * One example of where this differs from {@link #onViewportShifted} is if a
     * collaborator is typing above our viewport. When she presses ENTER, it our
     * viewport's line numbers will change, but will still point to the same
     * line instances. Therefore, this method would be called but not
     * {@link #onViewportShifted}.
     */
    void onViewportLineNumberChanged(ViewportModel viewport, Edge edge);
  }
  
  public enum Edge {
    TOP, BOTTOM
  }

  private class ChangeDispatcher implements ListenerManager.Dispatcher<Listener> {
    private LineInfo oldTop;
    private LineInfo oldBottom;

    @Override
    public void dispatch(Listener listener) {
      listener.onViewportShifted(ViewportModel.this, topAnchor.getLineInfo(),
          bottomAnchor.getLineInfo(), oldTop, oldBottom);
    }

    private void dispatch(LineInfo oldTop, LineInfo oldBottom) {
      this.oldTop = oldTop;
      this.oldBottom = oldBottom;
      listenerManager.dispatch(this);
    }
  }

  static ViewportModel create(Document document, SelectionModel selection, Buffer buffer) {
    return new ViewportModel(document, selection, buffer);
  }

  private final Anchor.ShiftListener anchorShiftedListener =
      new Anchor.ShiftListener() {
        private final Dispatcher<Listener> lineNumberChangedDispatcher =
            new ListenerManager.Dispatcher<ViewportModel.Listener>() {
              @Override
              public void dispatch(Listener listener) {
                listener.onViewportLineNumberChanged(ViewportModel.this, curChangedEdge);
              }
            };

        private Edge curChangedEdge;
        
        @Override
        public void onAnchorShifted(Anchor anchor) {
          curChangedEdge = anchor == topAnchor ? Edge.TOP : Edge.BOTTOM;
          listenerManager.dispatch(lineNumberChangedDispatcher);
        }
      };

  private final AnchorManager anchorManager;
  private Anchor bottomAnchor;
  private final Buffer buffer;
  private final Document document;
  private final ChangeDispatcher changeDispatcher;
  private final ListenerManager<Listener> listenerManager;
  private final SelectionModel selection;
  private Anchor topAnchor;
  private final ListenerRegistrar.RemoverManager removerManager =
      new ListenerRegistrar.RemoverManager();

  private ViewportModel(Document document, SelectionModel selection, Buffer buffer) {
    this.document = document;
    this.anchorManager = document.getAnchorManager();
    this.buffer = buffer;
    this.changeDispatcher = new ChangeDispatcher();
    this.listenerManager = ListenerManager.create();
    this.selection = selection;

    attachHandlers();
  }

  public LineInfo getBottom() {
    return bottomAnchor.getLineInfo();
  }

  public Line getBottomLine() {
    return bottomAnchor.getLine();
  }

  public int getBottomLineNumber() {
    return bottomAnchor.getLineNumber();
  }

  public LineInfo getBottomLineInfo() {
    return bottomAnchor.getLineInfo();
  }

  public Document getDocument() {
    return document;
  }

  public ListenerRegistrar<Listener> getListenerRegistrar() {
    return listenerManager;
  }

  public LineInfo getTop() {
    return topAnchor.getLineInfo();
  }

  public Line getTopLine() {
    return topAnchor.getLine();
  }

  public LineInfo getTopLineInfo() {
    return topAnchor.getLineInfo();
  }

  public int getTopLineNumber() {
    return topAnchor.getLineNumber();
  }

  public void initialize() {
    resetPosition();
  }

  /**
   * Returns whether the text of the given {@code lineNumber} is fully visible
   * in the viewport, determined by the current scrollTop and height of the
   * buffer.
   *
   * Note: This does not check whether any spacers above the given line are
   * fully visible.
   */
  public boolean isLineNumberFullyVisibleInViewport(int lineNumber) {
    int lineTop = buffer.calculateLineTop(lineNumber);
    int scrollTop = buffer.getScrollTop();
    int lineHeight = buffer.getEditorLineHeight();
    return lineTop >= scrollTop && (lineTop + lineHeight) <= scrollTop + buffer.getHeight();
  }

  public boolean isLineInViewport(Line line) {
    // Lines in the viewport will always return a line number
    int lineNumber = LineUtils.getCachedLineNumber(line);
    return (lineNumber != -1) && isLineNumberInViewport(lineNumber);
  }

  public boolean isLineNumberInViewport(int lineNumber) {
    /*
     * TODO: fix this to only do the expensive
     * calculation when a spacer is in the viewport.
     */
    /*
     * When the viewport is covered entirely by a spacer, the lines set as top
     * and bottom aren't actually visible, so check using their absolute buffer
     * positions.
     */
    int bufferTop = buffer.getScrollTop();
    int bufferBottom = bufferTop + buffer.getHeight();
    int lineTop = buffer.calculateLineTop(lineNumber);
    int lineBottom = lineTop + buffer.getEditorLineHeight();
    return !(bufferTop > lineBottom || bufferBottom < lineTop);
  }

  @Override
  public void onCursorChange(LineInfo lineInfo, int column, boolean isExplicitChange) {
    int cursorLineNumber = lineInfo.number();
    int cursorLeft = buffer.calculateColumnLeft(lineInfo.line(), column);
    int cursorTop = buffer.calculateLineTop(cursorLineNumber);
    int scrollLeft = buffer.getScrollLeft();
    int scrollTop = buffer.getScrollTop();
    int newScrollLeft = scrollLeft;
    int newScrollTop = scrollTop;

    int lineHeight = buffer.getEditorLineHeight();
    int bufferHeight = buffer.getHeight();

    int preCursorLineTop = Math.max(cursorTop - lineHeight, 0);
    int postCursorLineBottom = cursorTop + 2 * lineHeight;

    if (preCursorLineTop < scrollTop) {
      newScrollTop = preCursorLineTop;
    } else {
      if (postCursorLineBottom > scrollTop + bufferHeight) {
        newScrollTop = postCursorLineBottom - bufferHeight;
      }
    }

    if (cursorLeft < scrollLeft) {
      newScrollLeft = cursorLeft;
    } else {
      int bufferWidth = buffer.getWidth();
      int columnWidth = (int) buffer.getEditorCharacterWidth();
      if (cursorLeft + columnWidth > scrollLeft + bufferWidth) {
        newScrollLeft = cursorLeft + columnWidth - bufferWidth;
      }
    }

    if (newScrollTop != scrollTop || newScrollLeft != scrollLeft) {
      setBufferScrollAsync(newScrollLeft, newScrollTop);
    }
  }

  @Override
  public void onLineAdded(Document document, final int lineNumber,
      final JsonArray<Line> addedLines) {
    if (adjustViewportBoundsForLineAdditionOrRemoval(document, lineNumber)) {
      listenerManager.dispatch(new Dispatcher<ViewportModel.Listener>() {
        @Override
        public void dispatch(Listener listener) {
          listener.onViewportContentChanged(ViewportModel.this, lineNumber, true, addedLines);
        }
      });
    }
  }

  @Override
  public void onLineRemoved(Document document, final int lineNumber,
      final JsonArray<Line> removedLines) {
    if (adjustViewportBoundsForLineAdditionOrRemoval(document, lineNumber)) {
      listenerManager.dispatch(new Dispatcher<ViewportModel.Listener>() {
        @Override
        public void dispatch(Listener listener) {
          listener.onViewportContentChanged(ViewportModel.this, lineNumber, false,
              removedLines);
        }
      });
    }
  }

  @Override
  public void onScroll(Buffer buffer, int scrollTop) {
    moveViewportToScrollTop(scrollTop);
  }

  @Override
  public void onResize(Buffer buffer, int documentHeight, int viewportHeight, int scrollTop) {
    moveViewportToScrollTop(scrollTop);
  }

  private void moveViewportToScrollTop(int scrollTop) {
    int newTopLineNumber = buffer.convertYToLineNumber(scrollTop, true);
    int numLinesToShow = buffer.getFlooredHeightInLines() + 1;
    moveViewportToLineNumber(newTopLineNumber, numLinesToShow);
  }

  public void teardown() {
    removeAnchors();
    detachHandlers();
  }

  /**
   * Adjusts the viewport bounds after a line is added or removed, returning
   * whether there an adjustment was made.
   */
  private boolean adjustViewportBoundsForLineAdditionOrRemoval(Document document, int lineNumber) {
    int bottomLineNumber = bottomAnchor.getLineNumber();
    int topLineNumber = topAnchor.getLineNumber();
    int lastVisibleLineNumber = topLineNumber + buffer.getFlooredHeightInLines();

    /*
     * The "lastVisibleLineNumber != bottomLineNumber" catches the case where
     * the viewport's last line is deleted, causing the bottom anchor to shift
     * up a line before this method is called. So, the lineNumber will not be in
     * the range of the top and bottom anchors.
     */
    if (MathUtils.isInRangeInclusive(lineNumber, topLineNumber, bottomLineNumber)
        || lastVisibleLineNumber != bottomLineNumber) {

      // Update the viewport to cope with the line addition or removal
      int shiftAmount = lastVisibleLineNumber - bottomLineNumber;
      if (shiftAmount != 0) {
        shiftBottomAnchor(shiftAmount);
      }

      return true;
    } else {
      return false;
    }
  }

  private void attachHandlers() {
    removerManager.track(buffer.getScrollListenerRegistrar().add(this));
    removerManager.track(buffer.getResizeListenerRegistrar().add(this));
    removerManager.track(document.getLineListenerRegistrar().add(this));
    removerManager.track(selection.getCursorListenerRegistrar().add(this));
    removerManager.track(buffer.getSpacerListenerRegistrar().add(this));
  }

  private void detachHandlers() {
    removerManager.remove();
  }

  private void moveViewportToLineNumber(int topLineNumber, int numLinesToShow) {
    LineFinder lineFinder = buffer.getDocument().getLineFinder();

    LineInfo newTop = lineFinder.findLine(getTop(), topLineNumber);

    int targetBottomLineNumber = newTop.number() + numLinesToShow - 1;
    LineInfo newBottom =
        lineFinder.findLine(getBottom(),
            Math.min(document.getLastLineNumber(), targetBottomLineNumber));

    setRange(newTop, newBottom);
  }

  public void shiftHorizontally(boolean right) {
    int deltaScrollLeft = right ? 20 : -20;
    setBufferScrollAsync(buffer.getScrollLeft() + deltaScrollLeft, buffer.getScrollTop());
  }

  public void shiftVertically(boolean down, boolean byPage) {
    int deltaAbsoluteScrollTop =
        byPage ? buffer.getHeight() - buffer.getEditorLineHeight() : buffer.getEditorLineHeight();
    int deltaScrollTop = down ? deltaAbsoluteScrollTop : -deltaAbsoluteScrollTop;
    setBufferScrollAsync(buffer.getScrollLeft(), buffer.getScrollTop() + deltaScrollTop);
  }

  public void jumpTo(boolean end) {
    int newScrollTop = end ? buffer.getScrollHeight() - buffer.getHeight() : 0;
    setBufferScrollAsync(buffer.getScrollLeft(), newScrollTop);
  }

  private void removeAnchors() {
    anchorManager.removeAnchor(topAnchor);
    anchorManager.removeAnchor(bottomAnchor);

    topAnchor = null;
    bottomAnchor = null;
  }

  private void resetPosition() {
    LineInfo firstLine = new LineInfo(document.getFirstLine(), 0);
    int lastLineNumber = Math.min(document.getLastLineNumber(), buffer.getFlooredHeightInLines());
    LineInfo lastLine = document.getLineFinder().findLine(lastLineNumber);
    setRange(firstLine, lastLine);
  }

  /**
   * Sets scroll top of the buffer asynchronously. This allows some events to be
   * processed in the browser event loop between renders. (For example, without
   * the asynchronous posting, holding down page down would only render one
   * frame a second.)
   */
  private void setBufferScrollAsync(final int scrollLeft, final int scrollTop) {
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        buffer.setScrollLeft(Math.max(0, scrollLeft));

        /* We'll synchronously get a callback and shift our viewport model to this new scroll top */
        buffer.setScrollTop(Math.max(0, scrollTop));
      }
    });
  }

  private void setRange(LineInfo newTop, LineInfo newBottom) {
    LineInfo oldTop;
    LineInfo oldBottom;

    if (topAnchor == null || bottomAnchor == null) {
      oldTop = oldBottom = null;
      topAnchor =
          anchorManager.createAnchor(VIEWPORT_MODEL_ANCHOR_TYPE, newTop.line(), newTop.number(),
              AnchorManager.IGNORE_COLUMN);
      topAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);
      topAnchor.getShiftListenerRegistrar().add(anchorShiftedListener);

      bottomAnchor =
          anchorManager.createAnchor(VIEWPORT_MODEL_ANCHOR_TYPE, newBottom.line(),
              newBottom.number(), AnchorManager.IGNORE_COLUMN);
      bottomAnchor.setRemovalStrategy(RemovalStrategy.SHIFT);
      bottomAnchor.getShiftListenerRegistrar().add(anchorShiftedListener);

    } else {
      oldTop = topAnchor.getLineInfo();
      oldBottom = bottomAnchor.getLineInfo();

      if (oldTop.equals(newTop) && oldBottom.equals(newBottom)) {
        return;
      }

      anchorManager.moveAnchor(topAnchor, newTop.line(), newTop.number(),
          AnchorManager.IGNORE_COLUMN);
      anchorManager.moveAnchor(bottomAnchor, newBottom.line(), newBottom.number(),
          AnchorManager.IGNORE_COLUMN);
    }

    changeDispatcher.dispatch(oldTop, oldBottom);
  }

  /**
   * @param lineCount if negative, the bottom anchor will shift upward that many
   *        lines
   */
  private void shiftBottomAnchor(int lineCount) {
    LineInfo bottomLineInfo = bottomAnchor.getLineInfo();
    if (lineCount < 0) {
      for (; lineCount < 0; lineCount++) {
        bottomLineInfo.moveToPrevious();
      }
    } else {
      for (; lineCount > 0; lineCount--) {
        bottomLineInfo.moveToNext();
      }
    }

    setRange(topAnchor.getLineInfo(), bottomLineInfo);
  }

  @Override
  public void onSpacerAdded(Spacer spacer) {
    updateBoundsAfterSpacerChanged(spacer.getLineNumber());
  }

  @Override
  public void onSpacerHeightChanged(Spacer spacer, int oldHeight) {
    updateBoundsAfterSpacerChanged(spacer.getLineNumber());
  }

  @Override
  public void onSpacerRemoved(Spacer spacer, Line oldLine, int oldLineNumber) {
    updateBoundsAfterSpacerChanged(oldLineNumber);
  }

  private void updateBoundsAfterSpacerChanged(int spacerLineNumber) {
    if (spacerLineNumber < getTopLineNumber() || spacerLineNumber > getBottomLineNumber()) {
      return;
    }

    int newBottomLineNumber =
        buffer.convertYToLineNumber(buffer.getScrollTop() + buffer.getHeight(), true);
    setRange(getTop(), document
        .getLineFinder().findLine(getBottom(), newBottomLineNumber));
  }

}
