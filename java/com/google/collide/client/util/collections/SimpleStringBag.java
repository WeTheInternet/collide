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
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Simple string multiset implementation on the base of {@link ClientStringMap}.
 *
 */
public class SimpleStringBag implements StringMultiset {

  private ClientStringMap<Counter> delegate;

  public SimpleStringBag() {
    clear();
  }

  @Override
  public void addAll(@Nonnull JsonArray<String> items) {
    // TODO: Check if iterate is faster.
    for (int i = 0, l = items.size(); i < l; i++) {
      add(items.get(i));
    }
  }

  @Override
  public void add(@Nonnull String item) {
    Counter counter = delegate.get(item);
    if (counter == null) {
      counter = new Counter();
      delegate.put(item, counter);
    } else {
      counter.increment();
    }
  }

  @Override
  public void removeAll(@Nonnull JsonArray<String> items) {
    // TODO: Check if iterate is faster.
    for (int i = 0, l = items.size(); i < l; i++) {
      remove(items.get(i));
    }
  }

  @Override
  public void remove(@Nonnull String id) {
    Counter counter = delegate.get(id);
    // TODO: Remove this precondition.
    Preconditions.checkNotNull(counter, "trying to remove item that is not in collection: %s", id);
    if (counter.decrement()) {
      delegate.remove(id);
    }
  }

  @Override
  public boolean contains(@Nonnull String item) {
    return delegate.containsKey(item);
  }

  @Override
  public void clear() {
    delegate = ClientStringMap.create();
  }
}
