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

package com.google.collide.client.util.collections;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.JavaScriptObject;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;

/**
 * <a href="http://en.wikipedia.org/wiki/Skip_list">Skip List</a>
 * implementation that holds Strings.
 *
 * <p>Actually this class provides some services similar to
 * {@link java.util.SortedSet}, i.e. items can be placed / removed and
 * sequentially accessed.
 *
 * <p>If some value is added two or more times, only one value is placed in set.
 * So, when such value is removed (even one time), it will not appear in search
 * results (until it is added again). Also, removing value that do not exist
 * has no effect.
 *
 * <p>{@code null} values are not allowed for adding / removing / search.
 */
public final class SkipListStringSet {

  private final LevelGenerator levelGenerator;
  private final int maxLevel;

  /**
   * Object that generates integer values with semi-geometric
   * distribution in range [0 .. maxLevel).
   *
   * <p>Mostly, this interface is used for testing purpose.
   */
  @VisibleForTesting
  interface LevelGenerator {
    int generate();
  }

  private static class RandomLevelGenerator implements LevelGenerator {

    private final int maxValue;
    private final double promotionProbability;

    private RandomLevelGenerator(int maxLevel, double promotionProbability) {
      this.maxValue = maxLevel - 1;
      this.promotionProbability = promotionProbability;
    }

    @Override
    public int generate() {
      int result = 0;
      while (Math.random() < promotionProbability && result < maxValue) {
        result++;
      }
      return result;
    }
  }

  /**
   * Skip list node.
   */
  private static class Node extends JavaScriptObject {

    private static native Node createHead(int maxLevel) /*-{
      return new Array(maxLevel);
    }-*/;

    private static native Node create(String value, int maxLevel) /*-{
      var result = new Array(maxLevel);
      result.value = value;
      return result;
    }-*/;

    protected Node() {
    }

    private Node getNext() {
      return get(0);
    }

    private native Node get(int level) /*-{
      return this[level];
    }-*/;

    private native void set(int level, Node node) /*-{
      this[level] = node;
    }-*/;

    private native String getValue() /*-{
      return this.value;
    }-*/;
  }

  private static class SkipListIterator implements Iterator<String> {

    private Node nextItem;

    public SkipListIterator(Node nextItem) {
      this.nextItem = nextItem;
    }

    @Override
    public boolean hasNext() {
      return nextItem.getNext() != null;
    }

    @Override
    public String next() {
      Node next = nextItem.getNext();
      if (next == null) {
        throw new NoSuchElementException();
      }
      nextItem = next;
      return nextItem.getValue();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Height of the most "tall" item.
   */
  private int currentLevel;

  /**
   * Special item that placed before all other items, and do not hold value,
   */
  private final Node head;

  public static SkipListStringSet create() {
    return new SkipListStringSet(8, new RandomLevelGenerator(8, 0.25));
  }

  @VisibleForTesting
  SkipListStringSet(int maxLevel, LevelGenerator levelGenerator) {
    this.levelGenerator = levelGenerator;
    this.maxLevel = maxLevel;
    this.currentLevel = 0;
    this.head = Node.createHead(maxLevel);
  }

  /**
   * Adds value to "set".
   *
   * @param item non-{@code null} value to add.
   */
  public void add(@Nonnull String item) {
    Node backtrace = doSearch(item);
    Node cursor = backtrace.get(0).getNext();

    // If node with specified value exists: do nothing.
    if (cursor != null && cursor.getValue().equals(item)) {
      return;
    }

    int nodeLevel = levelGenerator.generate();

    if (nodeLevel > currentLevel) {
      for (int i = currentLevel + 1; i <= nodeLevel; i++) {
        backtrace.set(i, head);
      }
      currentLevel = nodeLevel;
    }

    Node newNode = Node.create(item, nodeLevel + 1);
    for (int i = 0; i <= nodeLevel; ++i) {
      newNode.set(i, backtrace.get(i).get(i));
      backtrace.get(i).set(i, newNode);
    }
  }

  /**
   * Searches the given node and returns "backtrace".
   */
  private Node doSearch(@Nonnull String item) {
    Node backtrace = Node.createHead(maxLevel);
    Node cursor = head;
    for (int i = currentLevel; i >= 0; --i) {
      Node next = cursor.get(i);
      while (next != null && next.getValue().compareTo(item) < 0) {
        cursor = next;
        next = cursor.get(i);
      }
      backtrace.set(i, cursor);
    }
    return backtrace;
  }

  /**
   * Searches for the earliest (in sorting order) item greater or equal to
   * the given one.
   *
   * @param item non-{@code null} value to search
   */
  public Iterable<String> search(@Nonnull final String item) {
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return new SkipListIterator(doSearch(item).get(0));
      }
    };
  }

  /**
   * Returns {@link Iterable} whose iterators first value is the
   * first item in this collection, so all items could be traversed.
   */
  public Iterable<String> first() {
    return new Iterable<String>() {
      @Override
      public Iterator<String> iterator() {
        return new SkipListIterator(head);
      }
    };
  }

  /**
   * Removes value from "set".
   *
   * @param item non-{@code null} value to search
   */
  public void remove(@Nonnull String item) {
    Node backtrace = doSearch(item);
    Node cursor = backtrace.get(0).getNext();

    // If node with specified do not exist: do nothing.
    if (cursor == null || !cursor.getValue().equals(item)) {
      return;
    }

    for (int i = 0; i <= currentLevel; i++) {
      if (backtrace.get(i).get(i) == cursor) {
        backtrace.get(i).set(i, cursor.get(i));
      }
    }

    while (currentLevel > 0 && head.get(currentLevel) == null) {
      currentLevel--;
    }
  }
}
