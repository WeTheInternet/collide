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

import java.util.Comparator;

/**
 * Defines a simple interface for a list/array.
 *
 * When used with DTOs:
 *
 * On the client it is safe to cast this to a
 * {@link com.google.collide.json.client.JsoArray}.
 *
 * Native to JavaScript "sparse" arrays are not supported.
 *
 * On the server, this is an instance of
 * {@link com.google.collide.json.server.JsonArrayListAdapter} which
 * is a wrapper around a List.
 *
 */
public interface JsonArray<T> {

  void add(T item);

  void addAll(JsonArray<? extends T> item);

  void clear();

  boolean contains(T item);

  JsonArray<T> copy();

  T get(int index);

  int indexOf(T item);

  boolean isEmpty();

  String join(String separator);

  T peek();

  T pop();

  T remove(int index);

  Iterable<T> asIterable();

  boolean remove(T item);

  void reverse();

  /**
   * Assigns a new value to the slot with specified index.
   *
   * @throws IndexOutOfBoundsException if index is not in [0..length) range
   */
  void set(int index, T item);

  /**
   * Sorts the array according to the comparator. Mutates the array.
   */
  void sort(Comparator<? super T> comparator);

  int size();

  JsonArray<T> slice(int start, int end);

  JsonArray<T> splice(int index, int deleteCount, T value);

  JsonArray<T> splice(int index, int deleteCount);
}
