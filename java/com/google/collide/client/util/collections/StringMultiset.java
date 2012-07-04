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

import com.google.collide.json.shared.JsonArray;

import javax.annotation.Nonnull;

// TODO: Combine with JsonStringSet?
/**
 * <a href="http://en.wikipedia.org/wiki/Multiset">Multiset</a> interface.
 *
 */
public interface StringMultiset {

  void addAll(@Nonnull JsonArray<String> items);

  void add(@Nonnull String item);

  void removeAll(@Nonnull JsonArray<String> items);

  void remove(@Nonnull String item);

  boolean contains(@Nonnull String item);

  void clear();
}
