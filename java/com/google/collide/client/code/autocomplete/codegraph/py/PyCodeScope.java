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

package com.google.collide.client.code.autocomplete.codegraph.py;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Class that defines the PY code scope calculated during parsing.
 *
 * <p>This instance holds the reference to parent, so for any line of code we
 * can store a single item; at the same time, many items can share a parent.
 */
public class PyCodeScope {

  /**
   * Enumeration of scope types.
   */
  public enum Type {
    CLASS,
    DEF
  }

  /**
   * Indention of the line with definition.
   */
  private final int indent;

  /**
   * Enum that specifies the type of scope.
   */
  private final Type type;

  /**
   * Name of the scope.
   */
  private final String name;

  /**
   * Parent scope.
   *
   * <p>{@code null} means that this scope is one of the root scopes.
   */
  private final PyCodeScope parent;

  /**
   * Constructs the bean.
   */
  public PyCodeScope(PyCodeScope parent, String name, int indent, Type type) {
    this.parent = parent;
    this.name = name;
    this.indent = indent;
    this.type = type;
  }

  /**
   * Builds the trie prefix for specified scope.
   *
   * <p>In result the root scope goes first, the innermost (the given one) is
   * represented at last position.
   */
  public static JsonArray<String> buildPrefix(PyCodeScope scope) {
    JsonArray<String> result = JsonCollections.createArray();
    while (scope != null) {
      result.add(scope.getName());
      scope = scope.getParent();
    }
    result.reverse();
    return result;
  }

  public int getIndent() {
    return indent;
  }

  public Type getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public PyCodeScope getParent() {
    return parent;
  }
}
