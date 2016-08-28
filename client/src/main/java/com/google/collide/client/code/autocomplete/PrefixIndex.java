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

package com.google.collide.client.code.autocomplete;

import com.google.collide.json.shared.JsonArray;


/**
 * Interface of the index structure which supports search by the key prefix.
 *
 * @param <T> value data type
 */
public interface PrefixIndex<T> {
  /**
   * Searches values by the key prefix.
   *
   * @param prefix search key prefix
   * @return values having keys prefixed with {@code prefix}
   */
  JsonArray<? extends T> search(String prefix);
}
