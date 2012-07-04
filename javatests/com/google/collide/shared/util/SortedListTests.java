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

package com.google.collide.shared.util;


import junit.framework.TestCase;

/**
 * Tests for the {@link SortedList} class.
 * 
 */
public class SortedListTests extends TestCase {

  private static final SortedList.Comparator<String> STRING_SORTING_FUNCTION =
      new SortedList.Comparator<String>() {
        @Override
        public int compare(String a, String b) {
          return a.compareTo(b);
        }
      };

  private SortedList<String> sortedList;

  @Override
  protected void setUp() throws Exception {
    sortedList = new SortedList<String>(
        STRING_SORTING_FUNCTION);
  }

  public void testBinarySearchForEmptyArray() {
    assertEquals(0, sortedList.findInsertionIndex(""));
    assertEquals(0, sortedList.findInsertionIndex("a1"));
    assertEquals(-1, sortedList.findIndex(""));
    assertEquals(-1, sortedList.findIndex("a1"));
  }

  public void testBinarySearchForOneElementArray() {
    addElements("a2");

    assertEquals(0, sortedList.findInsertionIndex("a1"));
    assertEquals(0, sortedList.findInsertionIndex("a2"));
    assertEquals(1, sortedList.findInsertionIndex("a3"));
    assertEquals(-1, sortedList.findIndex("a1"));
    assertEquals(0, sortedList.findIndex("a2"));
    assertEquals(-1, sortedList.findIndex("a3"));
  }

  public void testBinarySearchForEqualElementsArray() {
    addElements("a1", "a1", "a1", "a1");

    assertEquals(0, sortedList.findInsertionIndex("a1"));
    assertNotSame(-1, sortedList.findIndex("a1"));
  }

  public void testBinarySearch() {
    addElements("a2", "a4", "a6", "a8");

    assertEquals(0, sortedList.findInsertionIndex("a1"));
    assertEquals(1, sortedList.findInsertionIndex("a3"));
    assertEquals(2, sortedList.findInsertionIndex("a5"));
    assertEquals(3, sortedList.findInsertionIndex("a7"));
    assertEquals(4, sortedList.findInsertionIndex("a9"));

    assertEquals(-1, sortedList.findIndex("a1"));
    assertEquals(-1, sortedList.findIndex("a3"));
    assertEquals(-1, sortedList.findIndex("a5"));
    assertEquals(-1, sortedList.findIndex("a7"));
    assertEquals(-1, sortedList.findIndex("a9"));

    assertBinarySelfSearchResults();
  }

  public void testBinarySearchAfterSort() {
    addElements("b43", "a5", "a4", "a9", "c7", "a8", "x", "z", "y");
    assertEquals("a4,a5,a8,a9,b43,c7,x,y,z", joinAllElements());
    
    assertBinarySelfSearchResults();

    assertEquals(-1, sortedList.findIndex("a1"));
    assertEquals(-1, sortedList.findIndex("a7"));
    assertEquals(-1, sortedList.findIndex("y0"));
  }

  private void assertBinarySelfSearchResults() {
    for (int i = 0, n = sortedList.size(); i < n; ++i) {
      String s = sortedList.get(i);

      int index = sortedList.findIndex(s);
      assertNotSame(-1, index);
      assertEquals(s, sortedList.get(index));

      int insertionIndex = sortedList.findInsertionIndex(s);
      assertTrue(insertionIndex >= 0);
      assertEquals(s, sortedList.get(insertionIndex));
    }
  }

  private void addElements(String... array) {
    for (String s : array) {
      sortedList.add(s);
    }
  }

  private String joinAllElements() {
    String result = "";
    for (int i = 0, n = sortedList.size(); i < n; ++i) {
      if (!result.isEmpty()) {
        result += ",";
      }
      result += sortedList.get(i);
    }
    return result;
  }
}
