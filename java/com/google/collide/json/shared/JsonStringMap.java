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

package com.google.collide.json.shared;

/**
 * String Map interface for use in DTOs.
 *
 * On the client it is safe to cast this to a
 * {@link com.google.collide.json.client.Jso}.
 *
 * On the server this is an instance of a wrapper object
 * {@link com.google.collide.json.server.JsonStringMapAdapter}.
 */
public interface JsonStringMap<T> {
  /**
   * Callback to support iterating through the fields on this map.
   *
   * @param <T>
   */
  public interface IterationCallback<T> {
    void onIteration(String key, T value);
  }

  T get(String key);

  JsonArray<String> getKeys();

  boolean isEmpty();

  void iterate(IterationCallback<T> callback);

  void put(String key, T value);

  void putAll(JsonStringMap<T> otherMap);

  /**
   * Removes the item with the given key, and returns it.
   */
  T remove(String key);

  boolean containsKey(String key);

  int size();
}
