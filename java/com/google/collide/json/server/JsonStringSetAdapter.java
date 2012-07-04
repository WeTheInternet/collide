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

package com.google.collide.json.server;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.common.collect.Lists;

import java.util.Set;

/**
 * Server wrapper for a {@link java.util.Set} that implements
 * {@link JsonStringSet}.
 *
 */
public class JsonStringSetAdapter implements JsonStringSet {
  private final Set<String> delegate;

  public JsonStringSetAdapter(Set<String> delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean contains(String key) {
    return delegate.contains(key);
  }

  @Override
  public JsonArray<String> getKeys() {
    return new JsonArrayListAdapter<String>(Lists.newArrayList(delegate));
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public void iterate(IterationCallback callback) {
    for (String key : delegate) {
      callback.onIteration(key);
    }
  }

  @Override
  public void add(String key) {
    delegate.add(key);
  }

  @Override
  public void addAll(JsonArray<String> keys) {
    for (int i = 0, n = keys.size(); i < n; i++) {
      add(keys.get(i));
    }
  }

  @Override
  public boolean remove(String key) {
    return delegate.remove(key);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JsonStringSetAdapter) {
      return delegate.equals(((JsonStringSetAdapter) obj).delegate);
    }
    return false;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
