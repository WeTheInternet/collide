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
import com.google.collide.json.shared.JsonArrayIterator;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Wraps a {@link java.util.List} for use on the server.
 */
public class JsonArrayListAdapter<T> implements JsonArray<T> {

  private final List<T> delegate;

  public JsonArrayListAdapter(List<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void add(T item) {
    delegate.add(item);
  }

  @Override
  public void addAll(JsonArray<? extends T> array) {
    for (int i = 0; i < array.size(); i++) {
      add(array.get(i));
    }
  }

  /**
   * Returns this array as a {@link List}. The returned instance backs this
   * array, so any changes made to the list will affect this array.
   *
   *  If you have used this list as a sparse array you will end up with objects
   * which are unsafe and cannot be casted. It is recommend you iterate and
   * create your own list from the result.
   */
  public List<T> asList() {
    return delegate;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public boolean contains(T item) {
    return delegate.contains(item);
  }

  @Override
  public JsonArray<T> copy() {
    return new JsonArrayListAdapter<T>(new ArrayList<T>(delegate));
  }

  @Override
  public T get(int index) {
    return delegate.get(index);
  }

  @Override
  public int indexOf(T item) {
    return delegate.indexOf(item);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public T peek() {
    return get(delegate.size() - 1);
  }

  @Override
  public T pop() {
    return remove(delegate.size() - 1);
  }

  @Override
  public T remove(int index) {
    T result = get(index);
    delegate.remove(index);
    return result;
  }

  @Override
  public boolean remove(T item) {
    return delegate.remove(item);
  }

  @Override
  public void reverse() {
    Collections.reverse(delegate);
  }

  @Override
  public void set(int index, T item) {
    delegate.set(index, item);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public JsonArray<T> slice(int start, int end) {
    
    JsonArray<T> sliced = new JsonArrayListAdapter<T>(new ArrayList<T>());
    for (int i = start; i < end && i < size(); i++) {
      sliced.add(get(i));
    }

    return sliced;
  }

  @Override
  public void sort(Comparator<? super T> comparator) {
    Collections.sort(delegate, comparator);
  }

  @Override
  public JsonArray<T> splice(int index, int deleteCount) {
    return spliceImpl(index, deleteCount, false, null);
  }

  @Override
  public JsonArray<T> splice(int index, int deleteCount, T value) {
    return spliceImpl(index, deleteCount, true, value);
  }

  private JsonArray<T> spliceImpl(int index, int deleteCount, boolean hasValue, T value) {
    JsonArray<T> removedArray = new JsonArrayListAdapter<T>(new ArrayList<T>());
    for (int i = deleteCount; i > 0; i--) {
      removedArray.add(remove(index));
    }

    if (hasValue) {
      delegate.add(index, value);
    }

    return removedArray;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');

    for (int i = 0; i < size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      T value = get(i);
      sb.append(value == null ? "null" : value.toString());
    }
    sb.append(']');
    return sb.toString();
  }

  @Override
  public String join(String separator) {
    return Joiner.on(separator).join(delegate);
  }

  @Override
  public Iterable<T> asIterable() {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new JsonArrayIterator<T>(JsonArrayListAdapter.this);
      }
    };
  }
}
