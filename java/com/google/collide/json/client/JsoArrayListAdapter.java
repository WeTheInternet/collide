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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * JSOArray backed implementation of a List that implements just enough of the
 * interface to work with a {@link com.google.gwt.view.client.ListDataProvider}.
 * We don't support all methods on List, just the ones we need to get the
 * CellTree to work.
 *
 *  Note that we don't directly subclass List since it would result in all
 * references to List going through a dynamic dispatcher. So we simply wrap and
 * delegate.
 *
 *  CAVEAT: Using any methods on List not implemented will throw an unchecked
 * runtime exception.
 *
 */
public class JsoArrayListAdapter<T> implements List<T> {

  /**
   * Basic iterator interface for use with a JSOArrayListAdapter.
   */
  public class JSOArrayListAdapterIterator implements Iterator<T> {
    int currIndex = 0;

    @Override
    public boolean hasNext() {
      return currIndex >= JsoArrayListAdapter.this.size();
    }

    @Override
    public T next() {
      currIndex += 1;
      return JsoArrayListAdapter.this.get(currIndex);
    }

    @Override
    public void remove() {
      JsoArrayListAdapter.this.remove(currIndex);
    }
  }

  private final JsoArray<T> delegate;
  private final int fromIndex;
  private final int toIndex;

  public JsoArrayListAdapter(JsoArray<T> delegate) {
    this(delegate, 0, delegate.size());
  }

  public JsoArrayListAdapter(JsoArray<T> delegate, int fromIndex, int toIndex) {
    assert (fromIndex <= toIndex) : "fromIndex is > toIndex in JSOArrayListAdapter";

    this.delegate = delegate;
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
  }

  @Override
  public void add(int index, T element) {
    delegate.set(shiftIndex(index), element);
  }

  @Override
  public boolean add(T e) {
    delegate.add(e);
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method addAll is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method addAll is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public boolean contains(Object o) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method contains is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method containsAll is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public T get(int index) {
    return delegate.get(shiftIndex(index));
  }

  @Override
  public int indexOf(Object o) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method indexOf is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public Iterator<T> iterator() {
    return new JSOArrayListAdapterIterator();
  }

  @Override
  public int lastIndexOf(Object o) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method lastIndexOf is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public ListIterator<T> listIterator() {
    // TODO Consider implementing this.
    throw new RuntimeException("Method listIterator is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method listIterator is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public T remove(int index) {
    return delegate.remove(shiftIndex(index));
  }

  @Override
  public boolean remove(Object o) {
    // TODO Consider implementing this.
    throw new RuntimeException(
        "Method remove(Object) is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method removeAll is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method retainAll is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public T set(int index, T element) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method set is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public int size() {
    return toIndex - fromIndex;
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    // Needs to be a live view
    return new JsoArrayListAdapter<T>(delegate, fromIndex, toIndex);
  }

  @Override
  public Object[] toArray() {
    // TODO Consider implementing this.
    throw new RuntimeException("Method toArray is not yet supported for JSOArrayListAdapter!");
  }

  @Override
  public <T> T[] toArray(T[] a) {
    // TODO Consider implementing this.
    throw new RuntimeException("Method toArray is not yet supported for JSOArrayListAdapter!");
  }

  private int shiftIndex(int index) {
    return index + fromIndex;
  }
}
