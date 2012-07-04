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
import com.google.collide.json.shared.JsonStringMap;
import com.google.common.collect.Lists;

import java.util.Map;

/**
 * Server wrapper for a {@link java.util.Map} that implements
 * {@link JsonStringMap}.
 *
 */
public class JsonStringMapAdapter<T> implements JsonStringMap<T> {
  private final Map<String, T> delegate;

  public JsonStringMapAdapter(Map<String, T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public T get(String key) {
    return delegate.get(key);
  }

  @Override
  public JsonArray<String> getKeys() {
    return new JsonArrayListAdapter<String>(Lists.newArrayList(delegate.keySet()));
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public void iterate(IterationCallback<T> callback) {
    for (String key : delegate.keySet()) {
      callback.onIteration(key, delegate.get(key));
    }
  }

  @Override
  public void put(String key, T value) {
    delegate.put(key, value);
  }

  @Override
  public void putAll(JsonStringMap<T> otherMap) {
    JsonArray<String> keys = otherMap.getKeys();
    for (int i = 0, n = keys.size(); i < n; i++) {
      String key = keys.get(i);
      put(key, otherMap.get(key));
    }
  }

  @Override
  public T remove(String key) {
    return delegate.remove(key);
  }

  @Override
  public boolean containsKey(String key) {
    return delegate.containsKey(key);
  }

  @Override
  public int size() {
    return delegate.size();
  }
  
  @Override
  public String toString() {
    return delegate.toString();
  }
}
