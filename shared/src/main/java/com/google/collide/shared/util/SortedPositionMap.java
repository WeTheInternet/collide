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

package com.google.collide.shared.util;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.SortedList.Comparator;
import com.google.collide.shared.util.SortedList.OneWayComparator;

/**
 * A map with a key of (line number, column) and an arbitrary value.
 *
 */
public class SortedPositionMap<T> {

  private class Entry {
    private final int lineNumber;
    private final int column;
    private final T value;

    private Entry(int lineNumber, int column, T value) {
      this.lineNumber = lineNumber;
      this.column = column;
      this.value = value;
    }
  }

  /**
   * A class to be used as a cached one-way comparator instance.
   *
   * Methods on this class are NOT re-entrant (though I can't imagine a scenario
   * where the execution would lead to re-entrancy.)
   */
  private class Finder implements OneWayComparator<Entry> {
    private int lineNumber;
    private int column;

    @Override
    public int compareTo(Entry o) {
      return LineUtils.comparePositions(lineNumber, column, o.lineNumber, o.column);
    }

    private Entry findEntry(int lineNumber, int column) {
      this.lineNumber = lineNumber;
      this.column = column;
      return list.find(this);
    }

    private int findInsertionIndex(int lineNumber, int column) {
      this.lineNumber = lineNumber;
      this.column = column;
      return list.findInsertionIndex(this);
    }
  }

  private final Comparator<Entry> comparator = new Comparator<Entry>() {
    @Override
    public int compare(Entry a, Entry b) {
      return LineUtils.comparePositions(a.lineNumber, a.column, b.lineNumber, b.column);
    }
  };

  private final Finder finder = new Finder();
  private final SortedList<Entry> list;

  public SortedPositionMap() {
    list = new SortedList<Entry>(comparator);
  }

  public T get(int lineNumber, int column) {
    Entry entry = finder.findEntry(lineNumber, column);
    return entry != null ? entry.value : null;
  }

  /**
   * Puts the value at the given position, replacing any existing value (which
   * will be returned).
   */
  public T put(int lineNumber, int column, T value) {
    Entry existingEntry = finder.findEntry(lineNumber, column);
    if (existingEntry != null) {
      list.remove(existingEntry);
    }

    list.add(new Entry(lineNumber, column, value));

    return existingEntry != null ? existingEntry.value : null;
  }

  public void putAll(SortedPositionMap<T> positionToToken) {
    for (int i = 0, n = positionToToken.list.size(); i < n; i++) {
      Entry entry = positionToToken.list.get(i);
      put(entry.lineNumber, entry.column, entry.value);
    }
  }

  /**
   * Removes the values in the given range (begin is inclusive, end is
   * exclusive).
   */
  public void removeRange(int beginLineNumber, int beginColumn, int endLineNumber, int endColumn) {
    int index = finder.findInsertionIndex(beginLineNumber, beginColumn);

    while (index < list.size()) {
      Entry entry = list.get(index);
      if (LineUtils.comparePositions(entry.lineNumber, entry.column, endLineNumber, endColumn)
            >= 0) {
        // This item is past the end, we're done!
        return;
      }

      list.remove(index);

      // No need to increment index since we just removed an item
    }
  }

  public int size() {
    return list.size();
  }

  public JsonArray<T> values() {
    JsonArray<T> values = JsonCollections.createArray();
    for (int i = 0, n = list.size(); i < n; i++) {
      values.add(list.get(i).value);
    }

    return values;
  }
}
