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

import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Implementation of sorted multiset of strings based
 * on {@link SkipListStringSet}.
 *
 * <p>This collection allows storing Strings so that <ul>
 * <li>adding / removing item is performed in {@code O(1)} if there is no equal
 * item in collection, otherwise {@code O(lg N)}
 * <li>searching item is performed in {@code O(lg N)}
 * <li>search finds earliest (in sorting order) item greater or equal to given
 * <li>fetching next item is performed in {@code O(1)}
 * <li>every unique item appears only once when iterating
 * <li>iterator returns items in the sorted order
 * </ul>
 *
 * <p>Actually this object is composed of skip list (for sorted search result)
 * and map (for better performance).
 *
 * <p>{@link #remove} will fail in situation then item counter should become
 * less than zero.
 *
 * <p>{@link #search} is delegated to {@link SkipListStringSet} instance.
 *
 * <p>{@code null} items should not be passed to add / remove / search.
 *
 */
public final class SkipListStringBag implements StringMultiset {

  private JsonStringMap<Counter> itemCounters;

  private SkipListStringSet itemsSet;

  public SkipListStringBag() {
    clear();
  }

  @Override
  public final void addAll(@Nonnull JsonArray<String> items) {
    // TODO: Check if iterate is faster.
    for (int i = 0, l = items.size(); i < l; i++) {
      add(items.get(i));
    }
  }

  @Override
  public final void add(@Nonnull String item) {
    Counter counter = itemCounters.get(item);
    if (counter == null) {
      counter = new Counter();
      itemCounters.put(item, counter);
      itemsSet.add(item);
    } else {
      counter.increment();
    }
  }

  @Override
  public final void removeAll(@Nonnull JsonArray<String> items) {
    // TODO: Check if iterate is faster.
    for (int i = 0, l = items.size(); i < l; i++) {
      remove(items.get(i));
    }
  }

  @Override
  public final void remove(@Nonnull String item) {
    Counter counter = itemCounters.get(item);
    // TODO: Remove this precondition.
    Preconditions.checkNotNull(counter, "trying to remove absent item: %s", item);
    if (counter.decrement()) {
      itemCounters.remove(item);
      itemsSet.remove(item);
    }
  }

  @Override
  public boolean contains(@Nonnull String item) {
    return itemCounters.containsKey(item);
  }

  public final Iterable<String> search(@Nonnull String item) {
    return itemsSet.search(item);
  }

  @Override
  public void clear() {
    itemCounters = ClientStringMap.create();
    itemsSet = SkipListStringSet.create();
  }
}
