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

package com.google.collide.client.util.collections;

import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringSet;
import com.google.collide.json.shared.JsonArray;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

/**
 * Test cases for {@link SkipListStringSet}.
 *
 */
public class SkipListStringSetTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.util.UtilTestModule";
  }

  public void testEmpty() {
    SkipListStringSet set = SkipListStringSet.create();
    Iterator iterator = set.search("").iterator();
    assertFalse("emptiness", iterator.hasNext());
    set.remove("foo");
  }

  public void testAdd() {
    SkipListStringSet set = SkipListStringSet.create();
    set.add("foo");
    Iterator iterator = set.search("foo").iterator();
    assertEquals("found added element", "foo", iterator.next());
    assertFalse("no more elements", iterator.hasNext());
  }

  public void testSearch() {
    SkipListStringSet set = SkipListStringSet.create();
    set.add("3");
    set.add("1");
    set.add("2");
    Iterator iterator = set.search("2").iterator();
    assertEquals("found target", "2", iterator.next());
    assertEquals("found next", "3", iterator.next());
    assertFalse("no more elements", iterator.hasNext());
  }

  public void testSearchAbsent() {
    SkipListStringSet set = SkipListStringSet.create();
    set.add("4");
    set.add("1");
    set.add("3");
    Iterator iterator = set.search("2").iterator();
    assertEquals("found least greater than", "3", iterator.next());
    assertEquals("found next", "4", iterator.next());
    assertFalse("no more elements", iterator.hasNext());
  }

  public void testRemove() {
    SkipListStringSet set = SkipListStringSet.create();
    set.add("4");
    set.add("2");
    set.add("1");
    set.add("3");

    set.remove("3");
    Iterator iterator = set.search("2").iterator();
    assertEquals("found target", "2", iterator.next());
    assertEquals("found next", "4", iterator.next());
    assertFalse("no more elements", iterator.hasNext());
  }

  public void testCorrectnessOnRandomData() {
    Random rnd = new Random(42);
    for (int i = 128; i <= 4096; i = i * 2) {
      checkCorrectnessOnRandomData(rnd, i, 100);
    }
    checkCorrectnessOnRandomData(rnd, 16384, 10000);
  }

  /**
   * This method is used to check correctness of implementation on random data.
   *
   * <p>During test random keys are generated. With some probability they are
   * either added or removed form set.
   *
   * <p>To avoid useless operations, in situation when we were going to remove key
   * that is not in set, already, we add it. Vice versa, do not add keys that are
   * already in set.
   *
   * <p>All operations are applied to instance of target implementation and to
   * instance of class that allows similar functionality (set). So resulting key
   * set should be similar. This is explicitly checked.
   *
   * @param rnd randomness source
   * @param limit number of operations to perform
   * @param range how many different keys can appear in test
   */
  private void checkCorrectnessOnRandomData(final Random rnd, int limit, int range) {
    // Origin instance.
    JsoStringSet mirror = JsoStringSet.create();

    // Array to hold recently generated keys.
    JsoArray<String> values = JsoArray.create();

    // Target instance, with fully deterministic behavior.(driven by our
    // oscillator).
    SkipListStringSet target = new SkipListStringSet(8, new SkipListStringSet.LevelGenerator() {
      @Override
      public int generate() {
        int result = 0;
        while (rnd.nextInt(4) == 0 && result < 7) {
          result++;
        }
        return result;
      }
    });

    for (int i = 0; i < limit; i++) {
      boolean remove = false;
      if (values.size() > 0) {
        // Actually we simulate that set is growing
        // i.e. more key added than removed).
        remove = rnd.nextDouble() > 0.6;
      }

      // Value is either generated or selected from recent values list.
      String value;
      if (remove) {
        // When we are going to remove, recent values is a good choice
        // to choose from.
        value = values.get(rnd.nextInt(values.size()));
        // Eventually, if can't remove - add.
        if (!mirror.contains(value)) {
          remove = false;
        }
      } else {
        value = String.valueOf(rnd.nextInt(range));
        // If key already in set - remove it.
        if (mirror.contains(value)) {
          remove = true;
        } else {
          values.add(value);
        }
      }

      // Perform operation on both instances.
      if (remove) {
        target.remove(value);
        mirror.remove(value);
      } else {
        target.add(value);
        mirror.add(value);
      }
    }

    // Get sorted list of set of keys.
    JsonArray<String> keys = mirror.getKeys();
    keys.sort(new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o1.compareTo(o2);
      }
    });

    // Check that keys in search result has the same order and values.
    Iterator iterator = target.search(keys.get(0)).iterator();
    for (int i = 0, l = keys.size(); i < l; i++) {
      assertEquals("values", keys.get(i), iterator.next());
    }
    assertFalse("no more values", iterator.hasNext());

    // Check that all keys are properly selectable.
    for (int i = 0, l = keys.size(); i < l; i++) {
      String value = keys.get(i);
      assertEquals("search", value, target.search(value).iterator().next());
    }
  }
}
