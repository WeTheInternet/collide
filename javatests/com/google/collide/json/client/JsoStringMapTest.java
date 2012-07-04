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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test cases for {@link JsoStringMap}.
 */
public class JsoStringMapTest extends GWTTestCase {

  private static final String[] TEST_KEYS = new String[]{
      "something",
      "",
      "__defineGetter",
      "__defineSetter__",
      "__lookupGetter__",
      "__lookupSetter__",
      "constructor",
      "hasOwnProperty",
      "isPrototypeOf",
      "propertyIsEnumerable",
      "toLocaleString",
      "toString",
      "valueOf",
      //"__proto__"
  };

  private static final class Foo {
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.json.client.JsonClientTestModule";
  }

  public void testEmpty() {
    JsoStringMap<Foo> map = JsoStringMap.create();
    assertTrue("emptiness", map.isEmpty());
    JsoArray<String> keys = map.getKeys();
    assertEquals("key list emptiness", 0, keys.size());
  }

  public void testIsEmpty() {
    for (int i = 0, size = TEST_KEYS.length; i < size; i++) {
      String key = TEST_KEYS[i];
      JsoStringMap<Foo> map = JsoStringMap.create();
      map.put(key, new Foo());
      assertFalse("isEmpty with '" + key + "'", map.isEmpty());
    }
  }

  public void testGetFromEmpty() {
    for (int i = 0, size = TEST_KEYS.length; i < size; i++) {
      String key = TEST_KEYS[i];
      JsoStringMap<Foo> map = JsoStringMap.create();
      Foo foo = map.get(key);
      assertNull(".get('" + key + "') result", foo);
    }
  }

  public void testDeleteFromEmpty() {
    for (int i = 0, size = TEST_KEYS.length; i < size; i++) {
      String key = TEST_KEYS[i];
      JsoStringMap<Foo> map = JsoStringMap.create();
      Foo foo = map.remove(key);
      assertNull(".remove('" + key + "') result", foo);
    }
  }

  public void testAddAndDeleteFromEmpty() {
    for (int i = 0, size = TEST_KEYS.length; i < size; i++) {
      String key = TEST_KEYS[i];
      JsoStringMap<Foo> map = JsoStringMap.create();
      Foo foo = new Foo();
      map.put(key, foo);
      Foo getFoo = map.get(key);
      assertTrue(".get('" + key + "') result", foo == getFoo);
      Foo deletedFoo = map.remove(key);
      assertTrue(".remove('" + key + "') result", foo == deletedFoo);
    }
  }

  public void testGetKeys() {
    JsoStringMap<Foo> map = JsoStringMap.create();
    Foo foo = new Foo();
    for (int i = 0, size = TEST_KEYS.length; i < size; i++) {
      map.put(TEST_KEYS[i], foo);
    }

    JsoArray<String> keys = map.getKeys();
    assertEquals("number of keys", TEST_KEYS.length, keys.size());
    for (int i = 0, size = TEST_KEYS.length; i < size; i++) {
      String key = TEST_KEYS[i];
      assertTrue("has key('" + key + "')", keys.contains(key));
    }
  }
}
