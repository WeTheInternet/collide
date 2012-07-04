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

import com.google.collide.json.shared.JsonIntegerMap;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Server wrapper for a {@link java.util.Map} that implements
 * {@link JsonIntegerMap}.
 *
 * @param <T> the type contained as value in the map
 */
public class JsonIntegerMapAdapter<T> implements JsonIntegerMap<T> {
  private final Map<Integer, T> delegate;

  public JsonIntegerMapAdapter(Map<Integer, T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean hasKey(int key) {
    return delegate.containsKey(key);
  }

  @Override
  public T get(int key) {
    return delegate.get(key);
  }

  @Override
  public void put(int key, T value) {
    delegate.put(key, value);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public void erase(int key) {
    delegate.remove(key);
  }

  @Override
  public void iterate(JsonIntegerMap.IterationCallback<T> cb) {
    for (Entry<Integer, T> entry : delegate.entrySet()) {
      cb.onIteration(entry.getKey().intValue(), entry.getValue());
    }
  }
}
