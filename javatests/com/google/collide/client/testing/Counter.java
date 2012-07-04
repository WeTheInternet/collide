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

package com.google.collide.client.testing;

/**
 * A local-scope object that can be "final" (for access by inner class methods)
 * but count test calls.
 * 
 */
public class Counter {

  private int value;

  public Counter() {
    value = 0;
  }

  public Counter(int initialValue) {
    value = initialValue;
  }
  
  public int getValue() {
    return value;
  }

  public void decrement() {
    value--;
  }

  public void decrement(int decrBy) {
    value -= decrBy;
  }

  public void increment() {
    value++;
  }

  public void increment(int incrBy) {
    value += incrBy;
  }
}
