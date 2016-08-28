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

package com.google.collide.client.code.autocomplete.codegraph.js;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;

/**
 * Class that defines the JS code scope calculated during parsing.
 *
 * <p>This instance holds the reference to parent, so for any line of code we
 * can store a single item; at the same time, many items can share a parent.
 *
 */
public class JsCodeScope {

  /**
   * Builds the trie prefix for specified scope.
   *
   * <p>In result the root scope goes first, the innermost (the given one) is
   * represented at last position.
   */
  public static JsonArray<String> buildPrefix(JsCodeScope scope) {
    JsonArray<String> result = JsonCollections.createArray();
    while (scope != null) {
      String scopeName = scope.getName();
      if (scopeName != null) {
        JsonArray<String> parts = StringUtils.split(scopeName, ".");
        parts.reverse();
        result.addAll(parts);
      }
      scope = scope.getParent();
    }
    result.reverse();
    return result;
  }

  /**
   * Parent scope.
   *
   * <p>{@code null} means that this scope is one of the root scopes.
   */
  private final JsCodeScope parent;

  private final String name;

  public JsCodeScope(JsCodeScope parent, String name) {
    this.parent = parent;
    this.name = name;
  }

  public JsCodeScope getParent() {
    return parent;
  }

  public String getName() {
    return name;
  }
}
