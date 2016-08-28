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

package com.google.collide.json.shared;

/**
 * Integer Map interface.
 *
 * @param <T> the type contained as value in the map
 */
public interface JsonIntegerMap<T> {

  /**
   * Callback interface for int,double key value pairs.
   */
  public interface IterationCallback<T> {
    void onIteration(int key, T val);
  }

  boolean hasKey(int key);

  T get(int key);

  void put(int key, T val);
  
  boolean isEmpty();
  
  void erase(int key);
  
  /**
   * Iterates through the contents and calls back out to a callback.
   *
   * @param cb callback object
   */
  void iterate(IterationCallback<T> cb);
}
