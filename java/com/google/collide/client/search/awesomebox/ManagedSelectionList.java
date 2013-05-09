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

package com.google.collide.client.search.awesomebox;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

import elemental.dom.Element;

/**
 * Manages a list of elements which can have selection. Note no guarantees are
 * made when a JsonArray operation which affects the underlying list is
 * performed. If you do something rash, clearSelection or selectFirst or
 * selectLast, to reset into a known state.
 */
public class ManagedSelectionList<T extends ManagedSelectionList.SelectableElement> {

  public static <T extends ManagedSelectionList.SelectableElement> ManagedSelectionList<
      T> create() {
    return new ManagedSelectionList<T>();
  }

  public interface SelectableElement {
    Element getElement();

    /**
     * @return false if the item doesn't want selection.
     */
    boolean onSelected();

    void onSelectionCleared();
  }

  private static final int NO_SELECTION = -1;
  private int selectedIndex = NO_SELECTION;
  private JsonArray<T> elements;

  public ManagedSelectionList() {
    elements = JsonCollections.createArray();
  }

  public T getSelectedElement() {
    return hasSelection() ? elements.get(selectedIndex) : null;
  }

  public boolean selectNext() {
    for (int i = selectedIndex + 1; i < elements.size(); i++) {
      if (elements.get(i).onSelected()) {
        clearSelection();
        selectedIndex = i;
        return true;
      }
    }
    return false;
  }

  public boolean selectPrevious() {
    for (int i = selectedIndex - 1; i >= 0; i--) {
      if (elements.get(i).onSelected()) {
        clearSelection();
        selectedIndex = i;
        return true;
      }
    }
    return false;
  }
  
  /**
   * @param index of item to select
   */
  public void selectIndex(int index) {
    clearSelection();
    if (index < 0 || index >= elements.size()) {
      throw new IndexOutOfBoundsException();
    }
    selectedIndex = index;
    getSelectedElement().onSelected();
  }

  /**
   * Moves the selection either next or previous based on a boolean
   */
  public boolean moveSelection(boolean next) {
    if (!hasSelection()) {
      return next ? selectFirst() : selectLast();
    } else {
      return next ? selectNext() : selectPrevious();
    }
  }

  public boolean selectFirst() {
    selectedIndex = -1;
    if (!selectNext()) {
      selectedIndex = NO_SELECTION;
    }

    return selectedIndex != NO_SELECTION;
  }

  public boolean selectLast() {
    selectedIndex = elements.size();
    if (!selectPrevious()) {
      selectedIndex = NO_SELECTION;
    }

    return selectedIndex != NO_SELECTION;
  }

  public void clearSelection() {
    if (hasSelection()) {
      getSelectedElement().onSelectionCleared();
    }
    selectedIndex = NO_SELECTION;
  }

  public boolean hasSelection() {
    return selectedIndex >= 0 && selectedIndex < elements.size();
  }

  public JsonArray<T> asJsonArray() {
    return elements;
  }

  public void add(T value) {
    elements.add(value);
  }

  public T get(int index) {
    return elements.get(index);
  }

  public int size() {
    return elements.size();
  }

  public T remove(int index) {
    return elements.remove(index);
  }

  public void clear() {
    elements.clear();
  }

}
