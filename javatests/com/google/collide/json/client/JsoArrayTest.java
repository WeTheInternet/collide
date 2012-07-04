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

import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Comparator;

/**
 * Tests for JsoArray.
 *
 */
public class JsoArrayTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.collide.json.client.JsonClientTestModule";
  }

  public void testAddAllMissingDisjoint() {
    JsoArray<String> a = JsoArray.create();
    a.add("a1");
    a.add("a2");

    JsoArray<String> b = JsoArray.create();
    b.add("b1");
    b.add("b2");

    JsonCollections.addAllMissing(a, b);
    assertEquals(4, a.size());
    assertEquals(2, b.size());
    assertEquals("a1,a2,b1,b2", a.join(","));
  }

  public void testAddAllMissingIntersecting() {
    JsoArray<String> a = JsoArray.create();
    a.add("a1");
    a.add("a2");

    JsoArray<String> b = JsoArray.create();
    b.add("a1");
    b.add("b2");

    JsonCollections.addAllMissing(a, b);
    assertEquals(3, a.size());
    assertEquals("a1,a2,b2", a.join(","));
  }

  public void testAddAllMissingDuplicates() {
    JsoArray<String> a = JsoArray.create();
    a.add("a1");
    a.add("a2");
    a.add("a1");

    JsoArray<String> b = JsoArray.create();
    b.add("b1");
    b.add("b2");
    b.add("b1");

    JsonCollections.addAllMissing(a, b);
    assertEquals(6, a.size());
    assertEquals("a1,a2,a1,b1,b2,b1", a.join(","));
  }

  public void testAddAllMissingEmpty() {
    JsoArray<String> a = JsoArray.create();
    a.add("a1");
    JsonCollections.addAllMissing(a, JsoArray.<String>create());
    assertEquals(1, a.size());
    assertEquals("a1", a.get(0));
  }

  public void testAddAllMissingNull() {
    JsoArray<String> a = JsoArray.create();
    a.add("a1");
    JsonCollections.addAllMissing(a, null);
    assertEquals(1, a.size());
    assertEquals("a1", a.get(0));
  }

  public void testAddAllMissingSelfToSelf() {
    JsoArray<String> a = JsoArray.create();
    a.add("a1");
    a.add("a2");

    JsonCollections.addAllMissing(a, a);
    assertEquals(2, a.size());
    assertEquals("a1,a2", a.join(","));
  }

  public void testEqualsBothNull() {
    assertTrue(JsonCollections.equals((JsoArray<?>)null, null));
  }

  public void testEqualsOneNull() {
    JsoArray<String> a = JsoArray.create();
    a.add("a0");
    a.add("a1");
    assertFalse(JsonCollections.equals(a, null));
    assertFalse(JsonCollections.equals(null, a));
  }

  public void testEqualsDifferentSize() {
    JsoArray<String> aSmall = JsoArray.create();
    aSmall.add("a0");
    aSmall.add("a1");
    JsoArray<String> aLarge = JsoArray.create();
    aLarge.add("a0");
    assertFalse(JsonCollections.equals(aSmall, aLarge));
  }

  public void testEquals() {
    JsoArray<String> a0 = JsoArray.create();
    a0.add("a0");
    a0.add("a1");
    JsoArray<String> a1 = JsoArray.create();
    a1.add("a0");
    a1.add("a1");
    assertTrue(JsonCollections.equals(a0, a0));
    assertTrue(JsonCollections.equals(a0, a1));
  }

  public void testNotEquals() {
    JsoArray<String> a = JsoArray.create();
    a.add("a0");
    a.add("a1");
    JsoArray<String> b = JsoArray.create();
    b.add("a0");
    b.add("b1");
    assertFalse(JsonCollections.equals(a, b));
  }

  public void testSortWithComparator() {
    JsoArray<String> a = JsoArray.create();
    a.add("b");
    a.add("a");
    a.add("c");
    assertArray(a, "b", "a", "c");

    a.sort(new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o1.compareTo(o2);
      }
    });
    assertArray(a, "a", "b", "c");

    a.sort(new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o2.compareTo(o1);
      }
    });
    assertArray(a, "c", "b", "a");
  }

  public void testReverse() {
    JsoArray<String> a = JsoArray.from("a", "b", "c");
    a.reverse();
    assertArray(a, "c", "b", "a");
  }

  public void testOutOfBounds() {
    JsoArray<Integer> numbers = JsoArray.from(0);
    try {
      numbers.set(2, 2);
    } catch (IndexOutOfBoundsException ex) {
      // We expect this.
      return;
    }
    fail("IndexOutOfBoundsException didn't occur");
  }

  /**
   * Assert that an array contains exactly the expected values, in order.
   *
   * @param <T> the data type of the array
   * @param actual the actual array to check
   * @param expected the expected values, in order
   */
  private <T> void assertArray(JsoArray<T> actual, T... expected) {
    // Check the size.
    int size = expected.length;
    assertEquals("Size mismatch", size, actual.size());

    // Check the values.
    for (int i = 0; i < size; i++) {
      assertEquals(expected[i], actual.get(i));
    }
  }
}
