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

package com.google.collide.json.client;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;

/**
 * Client implementation of a set of strings.
 *
 */
public class JsoStringSet implements JsonStringSet {

  private static final String KEY_PREFIX = "#";

  /**
   * Convenience factory method.
   */
  public static JsoStringSet create() {
    return new JsoStringSet();
  }

  private Jso delegate = Jso.create();

  private JsoStringSet() {
  }

  @Override
  public final boolean contains(String key) {
    return delegate.hasOwnProperty(toInternalKey(key));
  }

  @Override
  public final JsonArray<String> getKeys() {
    JsonArray<String> result = delegate.getKeys();
    for (int i = 0, n = result.size(); i < n; ++i) {
      result.set(i, toPublicKey(result.get(i)));
    }
    return result;
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public final void iterate(IterationCallback callback) {
    JsonArray<String> keys = getKeys();
    for (int i = 0, n = keys.size(); i < n; ++i) {
      callback.onIteration(keys.get(i));
    }
  }

  @Override
  public final void add(String key) {
    delegate.addField(toInternalKey(key), true);
  }

  @Override
  public final void addAll(JsonArray<String> keys) {
    for (int i = 0, n = keys.size(); i < n; ++i) {
      add(keys.get(i));
    }
  }

  @Override
  public final boolean remove(String key) {
    if (contains(key)) {
      delegate.deleteField(toInternalKey(key));
      return true;
    }
    return false;
  }

  @Override
  public void clear() {
    delegate = Jso.create();
  }

  private static String toInternalKey(String key) {
    return KEY_PREFIX + key;
  }

  private static String toPublicKey(String key) {
    return key.substring(KEY_PREFIX.length());
  }
}
