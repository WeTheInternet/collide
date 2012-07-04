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

import com.google.collide.json.shared.JsonArray;

import junit.framework.TestCase;

/**
 * Tests for the {@link SortedPositionMap} class.
 */
public class SortedPositionMapTests extends TestCase {

  private final SortedPositionMap<Object> map = new SortedPositionMap<Object>();

  private final Object atL2C10 = new Object();
  private final Object atL5C10 = new Object();
  private final Object atL5C15 = new Object();
  private final Object atL9C0 = new Object();

  public void testRemoveRangeAll() {
    map.removeRange(0, 0, 100, 0);
    assertEquals(0, map.size());
  }

  public void testRemoveRangeInclusiveExclusive() {
    map.removeRange(2, 10, 5, 15);
    assertEquals(2, map.size());
    assertEquals(null, map.get(2, 10));
    assertEquals(null, map.get(5, 10));
    assertEquals(atL5C15, map.get(5, 15));
  }

  public void testReplace() {
    assertEquals(atL9C0, map.get(9, 0));
    Object newObject = new Object();
    map.put(9, 0, newObject);
    assertEquals(newObject, map.get(9, 0));
    JsonArray<Object> values = map.values();
    assertEquals(4, values.size());
    assertEquals(atL2C10, values.get(0));
    assertEquals(atL5C10, values.get(1));
    assertEquals(atL5C15, values.get(2));
    assertEquals(newObject, values.get(3));
  }

  public void testValues() {
    JsonArray<Object> values = map.values();
    assertEquals(4, values.size());
    assertEquals(atL2C10, values.get(0));
    assertEquals(atL5C10, values.get(1));
    assertEquals(atL5C15, values.get(2));
    assertEquals(atL9C0, values.get(3));
  }

  @Override
  protected void setUp() throws Exception {
    map.put(2, 10, atL2C10);
    map.put(5, 10, atL5C10);
    map.put(5, 15, atL5C15);
    map.put(9, 0, atL9C0);
  }
}
