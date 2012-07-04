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

/**
 * Object that is used for counting, how many times object is added to "bag".
 *
 * <p>New instance have counter equal to 1.
 * <p>{@link #decrement()} returns {@code true} if counter reaches 0.
 */
final class Counter {

  private int counter = 1;

  public final void increment() {
    counter++;
  }

  /**
   * @return {@code true} if counter reaches 0.
   */
  public final boolean decrement() {
    counter--;
    return counter == 0;
  }
}
