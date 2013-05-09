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

import junit.framework.TestCase;

import elemental.dom.Element;

/**
 * Tests the managed selection list used by AwesomeBox sections.
 *
 */
public class ManagedSelectionListTest extends TestCase {
  
  private class StubSelectableElement implements ManagedSelectionList.SelectableElement {
    private boolean isSelected = false;

    @Override
    public Element getElement() {
      // we don't actually have to return a real element for testing
      return null;
    }

    @Override
    public boolean onSelected() {
      isSelected = true;
      return true;
    }

    @Override
    public void onSelectionCleared() {
      isSelected = false;
    }

    public boolean getIsSelected() {
      return isSelected;
    }
  }
  
  private JsonArray<StubSelectableElement> createStubs(int size) {
    JsonArray<StubSelectableElement> elements = JsonCollections.createArray();
    for (int i = 0; i < size; i++) {
      elements.add(new StubSelectableElement());
    }
    return elements;
  }
  
  ManagedSelectionList<StubSelectableElement> elements =
      new ManagedSelectionList<StubSelectableElement>();

  public void testNoSelectionWhenEmpty() {
    assertFalse(elements.hasSelection());
    assertEquals(0, elements.size());

    elements.add(new StubSelectableElement());
    assertFalse(elements.hasSelection());
    elements.asJsonArray().clear();
    assertFalse(elements.hasSelection());
  }
  
  public void testSelectionNextAndPrev() {
    JsonArray<StubSelectableElement> stubs = createStubs(5);
    elements.asJsonArray().addAll(stubs);
    
    assertFalse(elements.hasSelection());
    elements.moveSelection(true);
    assertTrue(elements.hasSelection());
    assertSame(stubs.get(0), elements.getSelectedElement());
    assertTrue(stubs.get(0).getIsSelected());
    
    elements.moveSelection(true);
    elements.moveSelection(true);
    elements.moveSelection(true);

    assertTrue(elements.hasSelection());
    assertFalse(stubs.get(0).getIsSelected());
    assertTrue(stubs.get(3).getIsSelected());
    assertSame(stubs.get(3), elements.getSelectedElement());

    elements.moveSelection(false);
    elements.moveSelection(false);
    elements.moveSelection(false);
    
    assertTrue(elements.hasSelection());
    assertSame(stubs.get(0), elements.getSelectedElement());
    assertTrue(stubs.get(0).getIsSelected());
    assertFalse(stubs.get(3).getIsSelected());
  }
  
  public void testSelectFirstAndLastItem() {
    JsonArray<StubSelectableElement> stubs = createStubs(5);
    elements.asJsonArray().addAll(stubs);

    elements.selectFirst();
    assertSame(stubs.get(0), elements.getSelectedElement());
    assertTrue(stubs.get(0).getIsSelected());

    elements.selectLast();
    assertSame(stubs.get(4), elements.getSelectedElement());
    assertTrue(stubs.get(4).getIsSelected());
  }
  
  public void testClearSelection() {
    JsonArray<StubSelectableElement> stubs = createStubs(5);
    elements.asJsonArray().addAll(stubs);

    elements.selectLast();
    assertTrue(elements.hasSelection());
    
    elements.clearSelection();
    assertFalse(elements.hasSelection());
  }

  public void testSelectIndex() {
    JsonArray<StubSelectableElement> stubs = createStubs(5);
    elements.asJsonArray().addAll(stubs);

    elements.selectIndex(3);
    assertTrue(elements.hasSelection());
    assertSame(stubs.get(3), elements.getSelectedElement());

    try {
      elements.selectIndex(10);
      fail("Didn't throw exception when index was out of bounds");
    } catch (IndexOutOfBoundsException ex) {
      // pass test
    }
    
    try {
      elements.selectIndex(-1);
      fail("Didn't throw exception when index was out of bounds");
    } catch (IndexOutOfBoundsException ex) {
      // pass test
    }
  }
  
  public void testNoMoveSelectionAtBoundaries() {
    JsonArray<StubSelectableElement> stubs = createStubs(5);
    elements.asJsonArray().addAll(stubs);

    elements.selectLast();
    assertFalse(elements.selectNext());

    elements.selectFirst();
    assertFalse(elements.selectPrevious());
  }
}
