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

import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.util.ListenerRegistrar.Remover;
import com.google.collide.shared.util.SortedList;
import com.google.collide.shared.util.SortedList.OneWayIntComparator;

/**
 * This class takes care of mapping between the different coordinates used by
 * the editor. The two supported systems are:
 * <ul>
 * <li>Offset (x,y) - in pixels, relative to the top left of line 0 in the
 * current document.
 * <li>Line (line, column) - the real line number and column, taking into
 * account spacer objects in between lines. Lines and columns are 0-indexed.
 * </ul>
 */
class CoordinateMap implements Document.LineListener {

  interface DocumentSizeProvider {
    float getEditorCharacterWidth();

    int getEditorLineHeight();

    void handleSpacerHeightChanged(Spacer spacer, int oldHeight);
  }

  private static class OffsetCache {

    private static final SortedList.Comparator<OffsetCache> COMPARATOR =
        new SortedList.Comparator<OffsetCache>() {
          @Override
          public int compare(OffsetCache a, OffsetCache b) {
            return a.offset - b.offset;
          }
        };

    private static final SortedList.OneWayIntComparator<OffsetCache> Y_OFFSET_ONE_WAY_COMPARATOR =
        new SortedList.OneWayIntComparator<OffsetCache>() {
          @Override
          public int compareTo(OffsetCache s) {
            return value - s.offset;
          }
        };

    private static final SortedList.OneWayIntComparator<OffsetCache> LINE_NUMBER_ONE_WAY_COMPARATOR
        = new SortedList.OneWayIntComparator<OffsetCache>() {
          @Override
          public int compareTo(OffsetCache s) {
            return value - s.lineNumber;
          }
        };

    private final int offset;
    private final int height;
    private final int lineNumber;

    private OffsetCache(int offset, int lineNumber, int height) {
      this.offset = offset;
      this.height = height;
      this.lineNumber = lineNumber;
    }
  }

  private static final OffsetCache BEGINNING_EMPTY_OFFSET_CACHE = new OffsetCache(0, 0, 0);
  private static final AnchorType SPACER_ANCHOR_TYPE = AnchorType.create(CoordinateMap.class,
      "spacerAnchorType");
  private static final Spacer.Comparator SPACER_COMPARATOR = new Spacer.Comparator();
  private static final Spacer.OneWaySpacerComparator SPACER_ONE_WAY_COMPARATOR =
      new Spacer.OneWaySpacerComparator();

  /** Used by {@link #getPrecedingOffsetCache(int, int)} */
  private static final int IGNORE = Integer.MIN_VALUE;

  private Document document;
  private DocumentSizeProvider documentSizeProvider;

  /** List of offset cache items, sorted by the offset */
  private SortedList<OffsetCache> offsetCache;

  /**
   * True if there is at least one spacer in the editor, false otherwise (false
   * means a simple height / line height calculation can be used)
   */
  private boolean requiresMapping;

  /** Sorted by line number */
  private SortedList<Spacer> spacers;

  /** Summation of all spacers' heights */
  private int totalSpacerHeight;

  /** Remover for listener */
  private Remover documentLineListenerRemover;

  CoordinateMap(DocumentSizeProvider documentSizeProvider) {
    this.documentSizeProvider = documentSizeProvider;
    requiresMapping = false;
  }

  int convertYToLineNumber(int y) {
    if (y < 0) {
      return 0;
    }

    int lineHeight = documentSizeProvider.getEditorLineHeight();
    if (!requiresMapping) {
      return y / lineHeight;
    }

    OffsetCache precedingOffsetCache = getPrecedingOffsetCache(y, IGNORE);
    int precedingOffsetCacheBottom = precedingOffsetCache.offset + precedingOffsetCache.height;
    int lineNumberRelativeToOffsetCacheLine = (y - precedingOffsetCacheBottom) / lineHeight;

    if (y < precedingOffsetCacheBottom) {
      // y is inside the spacer
      return precedingOffsetCache.lineNumber;
    } else {
      return precedingOffsetCache.lineNumber + lineNumberRelativeToOffsetCacheLine;
    }
  }

  /**
   * Returns the top of the given line.
   */
  int convertLineNumberToY(int lineNumber) {
    int lineHeight = documentSizeProvider.getEditorLineHeight();
    if (!requiresMapping) {
      return lineNumber * lineHeight;
    }

    OffsetCache precedingOffsetCache = getPrecedingOffsetCache(IGNORE, lineNumber);
    int precedingOffsetCacheBottom = precedingOffsetCache.offset + precedingOffsetCache.height;
    int offsetRelativeToOffsetCacheBottom =
        (lineNumber - precedingOffsetCache.lineNumber) * lineHeight;
    return precedingOffsetCacheBottom + offsetRelativeToOffsetCacheBottom;
  }

  /**
   * Returns the first {@link OffsetCache} that is positioned less than or equal
   * to {@code y} or {@code lineNumber}. This methods fills the
   * {@link #offsetCache} if necessary ensuring the returned {@link OffsetCache}
   * is up-to-date.
   *
   * @param y the y, or {@link #IGNORE} if looking up by {@code lineNumber}
   * @param lineNumber the line number, or {@link #IGNORE} if looking up by
   *        {@code y}
   */
  private OffsetCache getPrecedingOffsetCache(int y, int lineNumber) {
    assert (y != IGNORE && lineNumber == IGNORE) || (lineNumber != IGNORE && y == IGNORE);

    final int lineHeight = documentSizeProvider.getEditorLineHeight();
    OffsetCache previousOffsetCache;
    if (y != IGNORE) {
      previousOffsetCache =
          getCachedPrecedingOffsetCacheImpl(OffsetCache.Y_OFFSET_ONE_WAY_COMPARATOR, y);
    } else {
      previousOffsetCache =
          getCachedPrecedingOffsetCacheImpl(OffsetCache.LINE_NUMBER_ONE_WAY_COMPARATOR, lineNumber);
    }

    if (previousOffsetCache == null) {
      if (spacers.size() > 0 && spacers.get(0).getLineNumber() == 0) {
        previousOffsetCache = createOffsetCache(0, 0, spacers.get(0).getHeight());
      } else {
        previousOffsetCache = BEGINNING_EMPTY_OFFSET_CACHE;
      }
    }

    /*
     * Optimization so the common case that the target has previously been
     * computed requires no more computation
     */
    int offsetCacheSize = offsetCache.size();
    if (offsetCacheSize > 0
        && isTargetEarlierThanOffsetCache(y, lineNumber, offsetCache.get(offsetCacheSize - 1))) {
      return previousOffsetCache;
    }

    // This will return this offset cache's matching spacer
    int spacerPos = getPrecedingSpacerIndex(previousOffsetCache.lineNumber);

    /*
     * We want the spacer following this offset cache's spacer, or the first
     * spacer if none were found
     */
    spacerPos++;

    for (int n = spacers.size(); spacerPos < n; spacerPos++) {
      Spacer curSpacer = spacers.get(spacerPos);

      int previousOffsetCacheBottom = previousOffsetCache.offset + previousOffsetCache.height;
      int simpleLinesHeight =
          (curSpacer.getLineNumber() - previousOffsetCache.lineNumber) * lineHeight;
      if (simpleLinesHeight == 0) {
        Log.warn(Spacer.class, "More than one spacer on line " + previousOffsetCache.lineNumber);
      }
      // Create an offset cache for this spacer
      OffsetCache curOffsetCache =
          createOffsetCache(previousOffsetCacheBottom + simpleLinesHeight,
              curSpacer.getLineNumber(), curSpacer.getHeight());
      if (isTargetEarlierThanOffsetCache(y, lineNumber, curOffsetCache)) {
        return previousOffsetCache;
      }

      previousOffsetCache = curOffsetCache;
    }

    return previousOffsetCache;
  }

  /**
   * Returns the {@link OffsetCache} instance in list that has the greatest
   * value less than or equal to the given {@code value}. Returns null if there
   * isn't one.
   *
   * This should only be used by {@link #getPrecedingOffsetCache(int, int)}.
   */
  private OffsetCache getCachedPrecedingOffsetCacheImpl(
      OneWayIntComparator<OffsetCache> comparator, int value) {
    comparator.setValue(value);
    int index = offsetCache.findInsertionIndex(comparator, false);
    return index >= 0 ? offsetCache.get(index) : null;
  }

  private boolean isTargetEarlierThanOffsetCache(int y, int lineNumber, OffsetCache offsetCache) {
    return ((y != IGNORE && y < offsetCache.offset) ||
        (lineNumber != IGNORE && lineNumber < offsetCache.lineNumber));
  }

  private OffsetCache createOffsetCache(int offset, int lineNumber, int height) {
    OffsetCache createdOffsetCache = new OffsetCache(offset, lineNumber, height);
    offsetCache.add(createdOffsetCache);
    return createdOffsetCache;
  }

  private int getPrecedingSpacerIndex(int lineNumber) {
    SPACER_ONE_WAY_COMPARATOR.setValue(lineNumber);
    return spacers.findInsertionIndex(SPACER_ONE_WAY_COMPARATOR, false);
  }

  /**
   * Adds a spacer above the given lineInfo line with height heightPx and
   * returns the created Spacer object.
   *
   * @param lineInfo the line before which the spacer will be inserted
   * @param height the height in pixels of the spacer
   */
  Spacer createSpacer(LineInfo lineInfo, int height, Buffer buffer, String cssClass) {
    int lineNumber = lineInfo.number();
    // create an anchor on the current line
    Anchor anchor =
        document.getAnchorManager().createAnchor(SPACER_ANCHOR_TYPE, lineInfo.line(), lineNumber,
            AnchorManager.IGNORE_COLUMN);
    anchor.setRemovalStrategy(RemovalStrategy.SHIFT);
    // account for the height of the line the spacer is on
    Spacer spacer = new Spacer(anchor, height, this, buffer, cssClass);
    spacers.add(spacer);
    totalSpacerHeight += height;
    invalidateLineNumberAndFollowing(lineNumber);

    requiresMapping = true;

    return spacer;
  }

  boolean removeSpacer(Spacer spacer) {
    int lineNumber = spacer.getLineNumber();
    if (spacers.remove(spacer)) {
      document.getAnchorManager().removeAnchor(spacer.getAnchor());
      totalSpacerHeight -= spacer.getHeight();
      invalidateLineNumberAndFollowing(lineNumber - 1);
      updateRequiresMapping();
      return true;
    }
    return false;
  }

  void handleDocumentChange(Document document) {
    if (documentLineListenerRemover != null) {
      documentLineListenerRemover.remove();
    }

    this.document = document;
    spacers = new SortedList<Spacer>(SPACER_COMPARATOR);
    offsetCache =
        new SortedList<OffsetCache>(OffsetCache.COMPARATOR);

    documentLineListenerRemover = document.getLineListenerRegistrar().add(this);
    requiresMapping = false; // starts with no items in list
    totalSpacerHeight = 0;
  }

  @Override
  public void onLineAdded(Document document, int lineNumber, JsonArray<Line> addedLines) {
    invalidateLineNumberAndFollowing(lineNumber);
  }

  @Override
  public void onLineRemoved(Document document, int lineNumber, JsonArray<Line> removedLines) {
    invalidateLineNumberAndFollowing(lineNumber);
  }

  /**
   * Call this after any line changes (adding/deleting lines, changing line
   * heights). Only invalidate (delete) cache items >= lineNumber, don't
   * recalculate.
   */
  void invalidateLineNumberAndFollowing(int lineNumber) {
    OffsetCache.LINE_NUMBER_ONE_WAY_COMPARATOR.setValue(lineNumber);
    int insertionIndex = offsetCache.findInsertionIndex(OffsetCache.LINE_NUMBER_ONE_WAY_COMPARATOR);
    offsetCache.removeThisAndFollowing(insertionIndex);
  }

  private void updateRequiresMapping() {
    // check to change active status
    requiresMapping = spacers.size() > 0;
  }

  int getTotalSpacerHeight() {
    return totalSpacerHeight;
  }

  void handleSpacerHeightChanged(Spacer spacer, int oldHeight) {
    totalSpacerHeight -= oldHeight;
    totalSpacerHeight += spacer.getHeight();
    invalidateLineNumberAndFollowing(spacer.getLineNumber());
    documentSizeProvider.handleSpacerHeightChanged(spacer, oldHeight);
  }
}
