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

package com.google.collide.client.util;

import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonIntegerMap;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayNumber;

/**
 * Creates a lightweight map with integer keys based on a JavaScript object.
 *
 * @param <T> the type contained as value in the map
 */
public class JsIntegerMap<T> extends JavaScriptObject implements JsonIntegerMap<T> {

  /**
   * Create a new empty map.
   *
   * @param <T> the type of values to be stored in the map
   * @return an empty map
   */
  public static native <T> JsIntegerMap<T> create() /*-{
    return {};
  }-*/;

  protected JsIntegerMap() {
  }

  /**
   * Removes the mapping for this key from the map.
   *
   * @param key
   */
  @Override
  public final native void erase(int key) /*-{
    delete this[key];
  }-*/;

  /**
   * Returns the value associated with the specified key.
   *
   * @param key
   * @return the value associated with the key
   */
  @Override
  public final native T get(int key) /*-{
    return this[key];
  }-*/;

  /**
   * Removes and returns the value associated with the specified key.
   */
  public final T remove(int key) {
    T value = get(key);
    erase(key);
    return value;
  }
  
  /**
   * Returns an array containing all the values in this map.
   *
   * @return a snapshot of the values contained in the map
   */
  public final native JsArrayNumber getKeys() /*-{
    var data = [];
    for (var prop in this) {
      var val = Number(prop);
      if (!isNaN(val)) {
        data.push(val);
      }
    }
    return data;
  }-*/;

  /**
   * Returns an array containing all the values in this map.
   *
   * @return a snapshot of the values contained in the map
   */
  public final native JsoArray<T> getValues() /*-{
    var data = [];
    for (var i in this) {
      if (this.hasOwnProperty(i)) {
        data.push(this[i]);
      }
    }
    return data;
  }-*/;

  /**
   * Returns true if this map has an entry for the specified key.
   *
   * @param key
   * @return true if this map has an entry for the given key
   */
  @Override
  public final native boolean hasKey(int key) /*-{
    return this.hasOwnProperty(key);
  }-*/;

  @Override
  public final native void iterate(JsonIntegerMap.IterationCallback<T> cb) /*-{
    for (var key in this) {
      if (this.hasOwnProperty(key)) {
        cb.
        @com.google.collide.json.shared.JsonIntegerMap.IterationCallback::onIteration(ILjava/lang/Object;)
        (parseInt(key),this[key]);
      }
    }
  }-*/;

  /**
   * Associates the specified value with the specified key in this map.
   *
   * @param key key with which the value will be associated
   * @param val value to be associated with key
   */
  @Override
  public final native void put(int key, T val) /*-{
    this[key] = val;
  }-*/;

  @Override
  public final native boolean isEmpty() /*-{
    for (var i in this) {
      if (this.hasOwnProperty(i)) {
        return false;
      }
    }
    
    return true;
  }-*/;
}
