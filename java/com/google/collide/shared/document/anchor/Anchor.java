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

package com.google.collide.shared.document.anchor;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerManager.Dispatcher;

// TODO: resolve the looseness in type safety
/*
 * There is loosened type safety in this class for the ListenerManagers to allow
 * for both the read-only interface (ReadOnlyAnchor) and this class to share the
 * same underlying ListenerManager for each event.
 */
/**
 * Model for an anchor in the document that maintains its up-to-date positioning
 * in the document as text changes occur.
 *
 * An anchor should pass {@link AnchorManager#IGNORE_LINE_NUMBER} if its line
 * number will not be used.
 *
 * Similarly, an anchor should pass {@link AnchorManager#IGNORE_COLUMN} if its
 * column will not be used. Anchors with this flag will be called
 * "line anchors".
 *
 * One caveat with line anchors that have the {@link RemovalStrategy#REMOVE}
 * removal strategy is they will be removed when the line's preceding newline
 * character is removed. For example, if the cursor is at (line 2, column 0) and
 * the user presses backspace, line 2's contents will be appended to line 1. In
 * this case, any line anchors on line 2 will be removed. One example where this
 * might be confusing is if the user selects from (line 1, column 0) to (line 2,
 * column 0) and presses delete. This would delete the line anchors on line 2
 * with the {@link RemovalStrategy#REMOVE} strategy even though the user has
 * logically deleted the text on line 1. There used to be a partial exception to
 * this rule to account for this confusin case, but that led to subtle bugs.
 *
 * Anchors can be positioned through the {@link AnchorManager}.
 *
 */
@SuppressWarnings("rawtypes")
public class Anchor implements ReadOnlyAnchor {

  /**
   * Listener that is notified when an anchor is shifted as a result of a text
   * change that affects the line number or column of the anchor. If the anchor
   * ignores the column, it will never receive a callback as a result of a
   * column shift. The same is true for line numbers.
   * 
   * <p>
   * If the anchor moves because of
   * {@link AnchorManager#moveAnchor(Anchor, Line, int, int)}, this will not be
   * called (see {@link MoveListener}).
   */
  public interface ShiftListener extends ShiftListenerImpl<Anchor> {
  }
  
  interface ShiftListenerImpl<A extends ReadOnlyAnchor> {
    void onAnchorShifted(A anchor);
  }

  /**
   * Listener that is notified when an anchor is moved via
   * {@link AnchorManager#moveAnchor(Anchor, Line, int, int)}.
   * 
   * <p>
   * If the anchor shifts because of text changes in the document, this will not
   * be called (see {@link ShiftListener}.
   */
  public interface MoveListener extends MoveListenerImpl<Anchor> {
  }
  
  interface MoveListenerImpl<A extends ReadOnlyAnchor> {
    void onAnchorMoved(A anchor);
  }
  /**
   * Listener that is notified when an anchor is removed.
   */
  public interface RemoveListener extends RemoveListenerImpl<Anchor> {
  }
  
  interface RemoveListenerImpl<A> {
    void onAnchorRemoved(A anchor);
  }

  /**
   * Defines the behavior for this anchor if the text where it is anchored gets
   * deleted.
   */
  public enum RemovalStrategy {
    /**
     * Removes the anchor if the text is deleted. Read the {@link Anchor}
     * javadoc for caveats with line anchors.
     */
    REMOVE,

    /** Shifts the anchor's position if the text is deleted */
    SHIFT
  }

  /**
   * This id is guaranteed to have a lower insertion index than any other id
   * in the same column.
   */
  public static final int ID_FIRST_IN_COLUMN = -1;

  /**
   * Counter for  {@link #id} generation.
   *
   * <p>
   * <a href="http://code.google.com/webtoolkit/doc/latest/DevGuideCodingBasicsCompatibility.html">
   * Actually</a> it is not a 32-bit integer value, but 64-bit double value.
   * So it <a href="http://ecma262-5.com/ELS5_HTML.htm#Section_8.5">can</a>
   * address 2^53 positive integer values.
   */
  private static int nextId = 0;

  private boolean attached = true;

  private int column;

  private InsertionPlacementStrategy insertionPlacementStrategy =
      InsertionPlacementStrategy.DEFAULT;

  /**
   * The type of this anchor.
   */
  private final AnchorType type;

  private final int id;

  /**
   * The client may optionally stuff an opaque, dynamic value into the anchor
   */
  private Object value;

  private Line line;

  private int lineNumber;

  private ListenerManager<ShiftListenerImpl<? extends ReadOnlyAnchor>> shiftListenerManager;
  
  private ListenerManager<MoveListenerImpl<? extends ReadOnlyAnchor>> moveListenerManager;

  private ListenerManager<RemoveListenerImpl<? extends ReadOnlyAnchor>> removeListenerManager;

  private RemovalStrategy removalStrategy = RemovalStrategy.REMOVE;

  Anchor(AnchorType type, Line line, int lineNumber, int column) {
    this.type = type;
    this.id = nextId++;
    this.line = line;
    this.lineNumber = lineNumber;
    this.column = column;
  }

  @Override
  public int getColumn() {
    return column;
  }

  /**
   * @return a unique id that can serve as a stable identifier for this anchor.
   */
  @Override
  public int getId() {
    return id;
  }

  @Override
  public AnchorType getType() {
    return type;
  }

  /**
   * Update the value stored in this anchor
   *
   * @param value the opaque value to store
   */
  public void setValue(Object value) {
    this.value = value;
  }

  /**
   * @param <T>
   * @return the value stored in this anchor. Note that type safety is the
   *         responsibility of the caller.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getValue() {
    return (T) value;
  }

  @Override
  public Line getLine() {
    return line;
  }

  public LineInfo getLineInfo() {
    return new LineInfo(line, lineNumber);
  }

  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  public boolean isLineAnchor() {
    return column == AnchorManager.IGNORE_COLUMN;
  }

  @Override
  public RemovalStrategy getRemovalStrategy() {
    return removalStrategy;
  }

  @Override
  public boolean hasLineNumber() {
    return lineNumber != AnchorManager.IGNORE_LINE_NUMBER;
  }

  @Override
  public boolean isAttached() {
    return attached;
  }

  @Override
  public InsertionPlacementStrategy getInsertionPlacementStrategy() {
    return insertionPlacementStrategy;
  }

  public void setInsertionPlacementStrategy(InsertionPlacementStrategy insertionPlacementStrategy) {
    this.insertionPlacementStrategy = insertionPlacementStrategy;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ListenerRegistrar<ReadOnlyAnchor.ShiftListener> getReadOnlyShiftListenerRegistrar() {
    return (ListenerRegistrar) getShiftListenerRegistrar();
  }

  @SuppressWarnings("unchecked")
  public ListenerRegistrar<ShiftListener> getShiftListenerRegistrar() {
    if (shiftListenerManager == null) {
      shiftListenerManager = ListenerManager.create();
    }

    return (ListenerRegistrar) shiftListenerManager;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ListenerRegistrar<ReadOnlyAnchor.MoveListener> getReadOnlyMoveListenerRegistrar() {
    return (ListenerRegistrar) getMoveListenerRegistrar();
  }

  @SuppressWarnings("unchecked")
  public ListenerRegistrar<MoveListener> getMoveListenerRegistrar() {
    if (moveListenerManager == null) {
      moveListenerManager = ListenerManager.create();
    }

    return (ListenerRegistrar) moveListenerManager;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ListenerRegistrar<ReadOnlyAnchor.RemoveListener> getReadOnlyRemoveListenerRegistrar() {
    return (ListenerRegistrar) getRemoveListenerRegistrar();
  }

  @SuppressWarnings("unchecked")
  public ListenerRegistrar<RemoveListener> getRemoveListenerRegistrar() {
    if (removeListenerManager == null) {
      removeListenerManager = ListenerManager.create();
    }

    return (ListenerRegistrar) removeListenerManager;
  }

  public void setRemovalStrategy(RemovalStrategy removalStrategy) {
    this.removalStrategy = removalStrategy;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getType().toString());
    sb.append(":").append(getId());
    sb.append(" (").append(lineNumber).append(',').append(column).append(")");
    sb.append("[").append(value).append("]");
    sb.append(": ");

    String lineStr = line.toString();
    if (column <= lineStr.length()) {
      int split = Math.max(0, Math.min(column, lineStr.length()));

      sb.append(lineStr.subSequence(0, split));
      sb.append("^");
      sb.append(lineStr.substring(split));
    } else {
      sb.append(lineStr);
      for (int i = 0, n = column - lineStr.length(); i < n; i++) {
        sb.append(" ");
      }
      sb.append("^ (WARNING: the anchor is out-of-bounds)");
    }

    return sb.toString();
  }

  void dispatchShifted() {
    if (shiftListenerManager != null) {
      shiftListenerManager
          .dispatch(new Dispatcher<Anchor.ShiftListenerImpl<? extends ReadOnlyAnchor>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void dispatch(ShiftListenerImpl listener) {
              listener.onAnchorShifted(Anchor.this);
            }
      });
    }
  }
  
  public void dispatchMoved() {
    if (moveListenerManager != null) {
      moveListenerManager
          .dispatch(new Dispatcher<Anchor.MoveListenerImpl<? extends ReadOnlyAnchor>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void dispatch(MoveListenerImpl listener) {
              listener.onAnchorMoved(Anchor.this);
            }
      });
    }
  }
  
  void detach() {
    this.attached = false;
  }
  
  void dispatchRemoved() {
    if (removeListenerManager != null) {
      removeListenerManager
          .dispatch(new Dispatcher<Anchor.RemoveListenerImpl<? extends ReadOnlyAnchor>>() {
        @SuppressWarnings("unchecked")
        @Override
        public void dispatch(RemoveListenerImpl listener) {
          listener.onAnchorRemoved(Anchor.this);
        }
      });
    }
  }

  /**
   * Make sure all calls to this method are surrounded with removal and
   * re-addition to the list(s) it belongs in!
   */
  void setColumnWithoutDispatch(int column) {
    this.column = column;
  }

  void setLineWithoutDispatch(Line line, int lineNumber) {
    checkArgument(hasLineNumber() == (lineNumber != AnchorManager.IGNORE_LINE_NUMBER));
    this.line = line;
    this.lineNumber = lineNumber;
  }

  @Override
  public boolean hasColumn() {
    return column != AnchorManager.IGNORE_COLUMN;
  }
}
