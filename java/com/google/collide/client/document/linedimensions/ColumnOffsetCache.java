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

package com.google.collide.client.document.linedimensions;

import com.google.collide.shared.Pair;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.SortedList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A cache object that is attached to each line which caches its offsets.
 *
 */
class ColumnOffsetCache {
  /**
   * An object which caches a map of a line column to its left edge pixel
   * position. This allows efficient lookup of column positions when variable
   * width characters are used.
   */
  public static class ColumnOffset {
    /**
     * Orders a list of ColumnOffsetCache by their column order.
     */
    private static final SortedList.Comparator<ColumnOffset> COLUMN_COMPARATOR =
        new SortedList.Comparator<ColumnOffset>() {
          @Override
          public int compare(ColumnOffset a, ColumnOffset b) {
            return a.column - b.column;
          }
        };

    /**
     * Finds an object in the column offset list by its X value.
     */
    private static final SortedList.OneWayDoubleComparator<ColumnOffset> X_ONE_WAY_COMPARATOR =
        new SortedList.OneWayDoubleComparator<ColumnOffset>() {
          @Override
          public int compareTo(ColumnOffset o) {
            return (int) (value - o.x);
          }
        };

    /**
     * Finds an object in the column offset list by its Column value.
     */
    private static final SortedList.OneWayIntComparator<ColumnOffset> COLUMN_ONE_WAY_COMPARATOR =
        new SortedList.OneWayIntComparator<ColumnOffset>() {
          @Override
          public int compareTo(ColumnOffset o) {
            return value - o.column;
          }
        };

    public final int column;
    public final double x;
    /** Width of previous column's character */
    public final double previousColumnWidth;

    public ColumnOffset(int column, double x, double previousColumnWidth) {
      this.column = column;
      this.x = x;
      this.previousColumnWidth = previousColumnWidth;
    }

    public boolean isZeroWidth() {
      return previousColumnWidth == 0;
    }
  }

  /**
   * Gets the {@link ColumnOffsetCache} stored on the given line. If it does not
   * already exist it will be created. If it already exists but with a different
   * zoomId, it will be cleared, removed and a new one created.
   *
   * @param line The line for this ColumnOffsetCache
   * @param zoomId A unique parameter which identifies the current page zoom
   *        level. This id is checked before the cache is returned. If it does
   *        not match the parameter stored inside the cache being retrieved then
   *        the cache is considered out of date and a new one constructed.
   */
  public static ColumnOffsetCache getOrCreate(Line line, double zoomId) {
    ColumnOffsetCache offsetCache = getUnsafe(line);
    if (offsetCache == null || offsetCache.zoomId != zoomId) {
      offsetCache = createCache(line, zoomId);
    }

    return offsetCache;
  }

  /**
   * Returns a column offset cache without checking its zoom level first.
   *
   * <p>If the zoom level has changed the data in this cache will be stale and
   * need to be rebuilt. Use this only if you aren't going to read it for
   * position data. If you can you should use {@link #getOrCreate}.
   *
   * @return {@code null} if cache doesn't exist
   */
  @Nullable
  public static ColumnOffsetCache getUnsafe(@Nonnull Line line) {
    return line.getTag(LINE_TAG_COLUMN_OFFSET_CACHE);
  }

  /**
   * The internal key used to tag a line with its cache.
   */
  private static final String LINE_TAG_COLUMN_OFFSET_CACHE =
      ColumnOffsetCache.class.getName() + "COLUMN_OFFSET_CACHE";

  /**
   * An object which serves as a flag to indicate that the entire string has
   * been measured. This prevents us from duplicating measurements when the user
   * clicks past the end of a line.
   */
  static final ColumnOffset FULLY_MEASURED =
      new ColumnOffset(Integer.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);

  /**
   * A column offset which is used to early out when the user is requesting
   * column 0 or pixel 0.
   */
  public static final ColumnOffset ZERO_OFFSET = new ColumnOffset(0, 0, 0);

  private final double zoomId;
  private SortedList<ColumnOffset> columnOffsets;
  ColumnOffset measuredOffset = ZERO_OFFSET;

  public ColumnOffsetCache(double zoomId) {
    this.zoomId = zoomId;
  }

  private SortedList<ColumnOffset> getCache() {
    if (columnOffsets == null) {
      columnOffsets = new SortedList<ColumnOffset>(ColumnOffset.COLUMN_COMPARATOR);
    }
    return columnOffsets;
  }

  /**
   * Returns the exact {@link ColumnOffset} or the nearest {@link ColumnOffset}
   * less than the requested column. Clients should check if measurements are
   * needed first by calling {@link #isColumnMeasurementNeeded(int)}.
   */
  public ColumnOffset getColumnOffsetForColumn(int column) {
    assert !isColumnMeasurementNeeded(column) : "Measurement of Column was needed";

    if (column == 0 || columnOffsets == null || columnOffsets.size() == 0) {
      return ZERO_OFFSET;
    }

    ColumnOffset.COLUMN_ONE_WAY_COMPARATOR.setValue(column);
    return getColumnOffset(ColumnOffset.COLUMN_ONE_WAY_COMPARATOR);
  }

  /**
   * Returns the exact {@link ColumnOffset} or the nearest {@link ColumnOffset}
   * less than the requested X. Clients should check if measurements are needed
   * first by calling {@link #isXMeasurementNeeded(double)}.
   *
   * @param defaultCharacterWidth The width that will be returned if the
   *        character is not a special width.
   * @param x The x pixel value
   *
   * @return This function will return the offset which corresponds either to
   *         the base character, or to the last zero-width mark following a base
   *         character. It will also return the width of the current column
   *         character so fractional columns can be determined.
   */
  public Pair<ColumnOffset, Double> getColumnOffsetForX(double x, double defaultCharacterWidth) {
    assert !isXMeasurementNeeded(x) : "Measurement of X was needed";

    if (x == 0 || columnOffsets == null || columnOffsets.size() == 0) {
      return Pair.of(ZERO_OFFSET, defaultCharacterWidth);
    }

    ColumnOffset.X_ONE_WAY_COMPARATOR.setValue(x);
    int index = getColumnOffsetIndex(ColumnOffset.X_ONE_WAY_COMPARATOR, true);
    if (index + 1 >= getCache().size()) {
      return Pair.of(getCache().get(index), defaultCharacterWidth);
    }

    ColumnOffset offset = index < 0 ? ZERO_OFFSET : getCache().get(index);
    ColumnOffset nextOffset = getCache().get(index < 0 ? 0 : index + 1);

    /*
     * So this is confusing but since a column offset always represents the
     * character before its column, we look to the next one to see if it is one
     * column more than us, if it is then we can grab the width of this
     * character from it since its special; otherwise, it's normal.
     */
    return Pair.of(offset, nextOffset.column == offset.column + 1
        ? nextOffset.previousColumnWidth : defaultCharacterWidth);
  }

  /**
   * Returns the last entry in the cache. If no entries are in the cache it will
   * return {@link #ZERO_OFFSET}.
   */
  public ColumnOffset getLastColumnOffsetInCache() {
    if (columnOffsets == null || columnOffsets.size() == 0) {
      return ZERO_OFFSET;
    }

    return columnOffsets.get(columnOffsets.size() - 1);
  }

  /**
   * Returns a {@link ColumnOffset} using the logic in {@link
   * #getColumnOffset(com.google.collide.shared.util.SortedList.OneWayComparator)}
   *
   * @return {@link #ZERO_OFFSET} if there are no items in the cache, otherwise
   *         either the matched {@link ColumnOffset} or one with less than the
   *         requested value.
   */
  private ColumnOffset getColumnOffset(SortedList.OneWayComparator<ColumnOffset> comparator) {
    int index = getColumnOffsetIndex(comparator, false);
    return index < 0 ? ZERO_OFFSET : getCache().get(index);
  }

  /**
   * Gets the index of the column offset based on the given comparator.
   *
   * @param comparator The comparator to use for ordering.
   * @param findLastOffsetIfZeroWidth if true then
   *        {@link #findLastCombiningMarkIfItExistsFromCache(int)} will be
   *        called so that we try to return a {@link ColumnOffset} which is the
   *        last zero-width in a string of zero-width marks. -1 will be returned
   *        if no applicable ColumnOffset is in the cache.
   */
  private int getColumnOffsetIndex(
      SortedList.OneWayComparator<ColumnOffset> comparator, boolean findLastOffsetIfZeroWidth) {
    SortedList<ColumnOffset> cache = getCache();
    int index = cache.findInsertionIndex(comparator, false);
    if (findLastOffsetIfZeroWidth) {
      index = findLastCombiningMarkIfItExistsFromCache(index);
    }
    if (index == 0) {
      ColumnOffset value = cache.get(index);
      /*
       * guards against a condition where the returned offset can be greater
       * than the requested value if there are only greater offsets in the cache
       * than what was requested.
       */
      return comparator.compareTo(value) < 0 ? -1 : 0;
    }
    return index;
  }

  /**
   * Appends an {@link ColumnOffset} with the specified column, x, and
   * previousColumnWidth parameters. Also updates the internal values to note
   * how far we have measured.
   *
   * <p>
   * Note: We expect x to correspond to the left edge of column.
   * </p>
   */
  public void appendOffset(int column, double x, double previousColumnWidth) {
    assert getLastColumnOffsetInCache().x <= x;
    assert getLastColumnOffsetInCache().column < column;
    SortedList<ColumnOffset> cache = getCache();
    ColumnOffset cacheOffset = new ColumnOffset(column, x, previousColumnWidth);
    cache.add(cacheOffset);

    measuredOffset = cacheOffset;
  }

  /**
   * Marks a line's cache dirty starting at the specified column. A column of 0
   * will completely clear the cache.
   */
  public void markDirty(int column) {
    if (column <= 0 || columnOffsets == null || columnOffsets.size() == 0) {
      if (columnOffsets != null) {
        columnOffsets.clear();
      }
      measuredOffset = ZERO_OFFSET;
    } else {
      ColumnOffset.COLUMN_ONE_WAY_COMPARATOR.setValue(column);
      int index = columnOffsets.findInsertionIndex(ColumnOffset.COLUMN_ONE_WAY_COMPARATOR, true);
      assert index != -1 && index <= columnOffsets.size();

      /*
       * we can be in a state where index == size() since the column we want to
       * mark dirty is past the last entry in the cache. If thats the case we
       * only need to update our measuredOffset.
       */
      if (index < columnOffsets.size()) {
        index = findBaseNonCombiningMarkIfItExistsFromCache(index);
        columnOffsets.removeThisAndFollowing(index);
      }
      measuredOffset = index == 0 ? ZERO_OFFSET : columnOffsets.get(index - 1);
    }
  }

  /**
   * Given an index in cache, walks backwards checking the
   * {@link ColumnOffset#isZeroWidth} to find the closest non-zero-width
   * character entry in the cache. If one does not exist (i.e. a` has only one
   * entry for the `) it will walk backwards to the offset entry of the first
   * combining mark (i.e. something like a````, it will return the entry of the
   * first `).
   */
  /*
   * This makes it so when someone deletes a combining mark we find the closest
   * character to its left which is not a combining mark to it (unless its not a
   * special width in which case we return the combining mark offset) and delete
   * the appropriate cache entries from there forward.
   */
  private int findBaseNonCombiningMarkIfItExistsFromCache(int index) {
    for (; index > 0; index--) {
      ColumnOffset currentOffset = columnOffsets.get(index);
      ColumnOffset previousOffset = columnOffsets.get(index - 1);
      /*
       * if I'm not zero width return me, otherwise if the offset before me in
       * cache is not the previous column then we may be something like a grave
       * accent.
       */
      if (!currentOffset.isZeroWidth() || currentOffset.column - 1 != previousOffset.column) {
        return index;
      }
    }
    return index;
  }

  /**
   * Given an index walks forward checking {@link ColumnOffset#isZeroWidth()} to
   * find the last contiguous, zero-width character in the cache. If one does
   * not exist it will return index.
   */
  private int findLastCombiningMarkIfItExistsFromCache(int index) {
    for (; index >= 0 && index < columnOffsets.size() - 1; index++) {
      ColumnOffset currentOffset = columnOffsets.get(index);
      ColumnOffset nextOffset = columnOffsets.get(index + 1);
      // if the next character is not zero-width return me, otherwise march on.
      if (!nextOffset.isZeroWidth() || currentOffset.column + 1 != nextOffset.column) {
        return index;
      }
    }
    return index;
  }

  /**
   * Checks if the column needs measurement before it can be retrieved by the
   * cache.
   */
  public boolean isColumnMeasurementNeeded(int column) {
    return measuredOffset != FULLY_MEASURED && column > measuredOffset.column;
  }

  /**
   * Checks if the x pixel needs measurement before it can be retrieved by the
   * cache.
   */
  public boolean isXMeasurementNeeded(double x) {
    return measuredOffset != FULLY_MEASURED && x >= measuredOffset.x;
  }
  
  /**
   * Removes the cache stored on a line, if any.
   */
  public static void removeFrom(Line line) {
    line.removeTag(LINE_TAG_COLUMN_OFFSET_CACHE);
  }

  private static ColumnOffsetCache createCache(Line line, double zoomId) {
    ColumnOffsetCache offsetCache = new ColumnOffsetCache(zoomId);
    line.putTag(LINE_TAG_COLUMN_OFFSET_CACHE, offsetCache);
    return offsetCache;
  }
}
