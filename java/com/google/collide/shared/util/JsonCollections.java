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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.Nullable;

import com.google.collide.json.server.JsonArrayListAdapter;
import com.google.collide.json.server.JsonIntegerMapAdapter;
import com.google.collide.json.server.JsonStringMapAdapter;
import com.google.collide.json.server.JsonStringSetAdapter;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonIntegerMap;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringSet;
import com.google.common.base.Equivalence;
import com.google.common.base.Objects;
import com.google.gwt.core.shared.GWT;

/**
 * A set of static factory methods for lightweight collections.
 *
 */
public final class JsonCollections {

  public interface Implementation {
    <T> JsonArray<T> createArray();
    <T> JsonStringMap<T> createMap();
    <T> JsonIntegerMap<T> createIntegerMap();
    JsonStringSet createStringSet();
  }

  private static class PureJavaImplementation implements Implementation {
    @Override
    public <T> JsonArray<T> createArray() {
      return new JsonArrayListAdapter<T>(new ArrayList<T>());
    }

    @Override
    public <T> JsonStringMap<T> createMap() {
      return new JsonStringMapAdapter<T>(new HashMap<String, T>());
    }

    @Override
    public JsonStringSet createStringSet() {
      return new JsonStringSetAdapter(new HashSet<String>());
    }

    @Override
    public <T> JsonIntegerMap<T> createIntegerMap() {
      return new JsonIntegerMapAdapter<T>(new HashMap<Integer, T>());
    }
  }

  // If running in pure java (server code or tests) or in dev mode, use the pure java impl
  private static Implementation implementation = !GWT.isClient() || !GWT.isScript() ?
      new PureJavaImplementation() : null;

  public static void setImplementation(Implementation implementation) {
    JsonCollections.implementation = implementation;
  }

  public static <T> JsonArray<T> createArray() {
    return implementation.createArray();
  }

  public static <T> JsonStringMap<T> createMap() {
    return implementation.createMap();
  }

  public static <T> JsonIntegerMap<T> createIntegerMap() {
    return implementation.createIntegerMap();
  }

  public static <T> JsonArray<T> createArray(T... items) {
    JsonArray<T> array = createArray();
    for (int i = 0, n = items.length; i < n; i++) {
      array.add(items[i]);
    }

    return array;
  }

  public static <T> JsonArray<T> createArray(Iterable<T> items) {
    JsonArray<T> array = createArray();
    for (Iterator<T> it = items.iterator(); it.hasNext(); ) {
      array.add(it.next());
    }

    return array;
  }

  public static JsonStringSet createStringSet() {
    return implementation.createStringSet();
  }

  public static JsonStringSet createStringSet(String... items) {
    JsonStringSet set = createStringSet();
    for (int i = 0, n = items.length; i < n; i++) {
      set.add(items[i]);
    }
    return set;
  }

  public static JsonStringSet createStringSet(Iterator<String> iterator) {
    JsonStringSet set = createStringSet();
    while (iterator.hasNext()) {
      set.add(iterator.next());
    }
    return set;
  }

  // TODO: Is it used?
  public static <T> void addAllMissing(JsonArray<T> self, JsonArray<T> b) {

    if (b == null || self == b) {
      return;
    }

    JsonArray<T> addList = createArray();
    for (int i = 0, n = b.size(); i < n; i++) {
      T addCandidate = b.get(i);
      if (!self.contains(addCandidate)) {
        addList.add(addCandidate);
      }
    }
    self.addAll(addList);
  }

  /**
   * Check if two lists are equal. The lists are equal if they are both the same
   * size, and the items at every index are equal. Returns true if both lists
   * are null.
   *
   * @param <T> the data type of the arrays
   */
  public static <T> boolean equals(JsonArray<T> a, JsonArray<T> b) {
    return equals(a, b, null);
  }

  /**
   * Check if two lists are equal. The lists are equal if they are both the same
   * size, and the items at every index are equal according to the provided
   * equator. Returns true if both lists are null.
   *
   * @param equivalence if null the {@link Object#equals(Object)} is used to
   *        determine item equality.
   *
   * @param <T> the data type of the arrays
   */
  public static <T> boolean equals(
      JsonArray<T> a, JsonArray<T> b, @Nullable Equivalence<T> equivalence) {
    if (a == b) {
      // Same list or both null.
      return true;
    } else if (a == null || b == null) {
      // One list is null, the other is not.
      return false;
    } else if (a.size() != b.size()) {
      // Different sizes.
      return false;
    } else {
      // Check the elements in the array.
      for (int i = 0; i < a.size(); i++) {
        T itemA = a.get(i);
        T itemB = b.get(i);
        // if the equator is null we just the equals method and some null checking
        if (equivalence == null && !Objects.equal(itemA, itemB)) {
          return false;
        } else if (equivalence != null && !equivalence.equivalent(itemA, itemB)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Check if two maps are equal. The maps are equal if they have exactly the
   * same set of keys value pairs.
   *
   * @param <T> the data type of the arrays
   */
  public static <T> boolean equals(final JsonStringMap<T> a, final JsonStringMap<T> b) {
    return equals(a, b, null);
  }

  /**
   * Check if two maps are equal. The maps are equal if they have exactly the
   * same set of keys value pairs. Checks the values using a custom
   * {@link Equivalence} check.
   *
   * @param equivalence if null {@link Objects#equal(Object, Object)} is used to
   *        verify equivalence.
   *
   * @param <T> the data type of the arrays
   */
  public static <T> boolean equals(
      final JsonStringMap<T> a, final JsonStringMap<T> b, @Nullable Equivalence<T> equivalence) {
    if (a == b) {
      // Same map or both null.
      return true;
    } else if (a == null || b == null) {
      // One map is null, the other is not.
      return false;
    } else {
      JsonArray<String> keys = a.getKeys();
      if (!equals(keys, b.getKeys())) {
        return false;
      }

      for (int i = 0; i < keys.size(); i++) {
        String key = keys.get(i);
        T valueA = a.get(key);
        T valueB = b.get(key);
        boolean isNotEquivalent = (equivalence == null && !Objects.equal(valueA, valueB))
            || (equivalence != null && !equivalence.equivalent(valueA, valueB));
        if (isNotEquivalent) {
          return false;
        }
      }

      return true;
    }
  }
}
