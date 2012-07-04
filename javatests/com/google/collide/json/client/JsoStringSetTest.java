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
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link JsoStringSet}.
 */
public class JsoStringSetTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.json.client.JsonClientTestModule";
  }

  public void testEmptySet() {
    JsoStringSet set = JsoStringSet.create();
    assertEquals(0, set.getKeys().size());
    assertTrue(set.isEmpty());
  }

  public void testGetKeys() {
    doTest(
        JsoArray.from("a", "b", "c"),  // Input data.
        JsoArray.from("a", "b", "c"),  // Expected.
        JsoArray.from("d", "aa", "b0") // Not expected.
    );
  }

  public void testDuplicatedKeys() {
    doTest(
        JsoArray.from("a", "b", "c", "b", "c", "a", "c", "a", "b"),
        JsoArray.from("a", "b", "c"),
        JsoArray.from("d", "aa", "b0")
    );
  }

  public void testEmptyKey() {
    doTestSingleKey("");
  }

  public void testProtoKey() {
    doTestSingleKey("__proto__");
  }

  private void doTestSingleKey(String key) {
    JsoStringSet set = JsoStringSet.create();
    assertFalse(set.contains(key));

    doTest(
        JsoArray.from(key),
        JsoArray.from(key),
        JsoArray.from("d", "aa", "b0")
    );
  }

  public void testAddAll() {
    JsoStringSet set = createSet(JsoArray.from("a", "b", "c"));
    JsonArray<String> oldKeys = set.getKeys();
    JsonArray<String> newKeys = JsoArray.from("a", "x", "y", "x");

    set.addAll(newKeys);
    assertEquals("Size", 5, set.getKeys().size());
    assertContainsAll(set, oldKeys);
    assertContainsAll(set, newKeys);
  }

  private void doTest(JsoArray<String> inputKeys, final JsoArray<String> expectedKeys,
      final JsoArray<String> notExpectedKeys) {
    JsoStringSet set = createSet(inputKeys);
    assertContainsAll(set, inputKeys);
    assertContainsAll(set, expectedKeys);

    if (inputKeys.size() == 0) {
      assertTrue(set.isEmpty());
    } else {
      assertFalse(set.isEmpty());
    }

    JsonArray<String> keys = set.getKeys();
    assertEquals(expectedKeys.size(), keys.size());

    for (int i = 0, n = keys.size(); i < n; ++i) {
      assertEquals(expectedKeys.get(i), keys.get(i));
    }

    for (int i = 0, n = notExpectedKeys.size(); i < n; ++i) {
      assertFalse(set.contains(notExpectedKeys.get(i)));
    }

    set.iterate(new JsonStringSet.IterationCallback() {
      @Override
      public void onIteration(String key) {
        assertTrue(expectedKeys.contains(key));
        assertFalse(notExpectedKeys.contains(key));
      }
    });
  }

  private JsoStringSet createSet(JsoArray<String> keys) {
    JsoStringSet set = JsoStringSet.create();
    for (int i = 0, n = keys.size(); i < n; ++i) {
      set.add(keys.get(i));
    }
    return set;
  }

  private void assertContainsAll(JsoStringSet set, JsonArray<String> keys) {
    for (int i = 0, n = keys.size(); i < n; ++i) {
      assertTrue(set.contains(keys.get(i)));
    }
  }
}
