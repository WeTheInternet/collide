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

package com.google.collide.dto;

import com.google.collide.dtogen.shared.CompactJsonDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.SerializationIndex;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;

/**
 * An interface representing a continuous namespace in the source code. It may
 * be a function, a class, an object or just anonymous namespace.
 *
 * Code blocks have start offset relative to the file start, length,
 * type and optional name.
 *
 * This interface is based on CodeBlock proto from Code Search
 */
@RoutingType(type = RoutingTypes.CODEBLOCK)
public interface CodeBlock extends ServerToClientDto, CompactJsonDto {
  /**
   * Enumeration of currently supported code block types.
   */
  public static enum Type {

    UNDEFINED(0),
    FUNCTION(1),
    FIELD(2),
    FILE(3),
    CLASS(4),
    UNRESOLVED_REFERENCE(5),
    PACKAGE(6);

    public static final int VALUE_UNDEFINED = 0;
    public static final int VALUE_FUNCTION = 1;
    public static final int VALUE_FIELD = 2;
    public static final int VALUE_FILE = 3;
    public static final int VALUE_CLASS = 4;
    public static final int VALUE_UNRESOLVED_REFERENCE = 5;
    public static final int VALUE_PACKAGE = 6;

    public final int value;

    public static Type valueOf(int value) {
      switch (value) {
        case VALUE_UNDEFINED: return UNDEFINED;
        case VALUE_FUNCTION: return FUNCTION;
        case VALUE_FIELD: return FIELD;
        case VALUE_FILE: return FILE;
        case VALUE_CLASS: return CLASS;
        case VALUE_UNRESOLVED_REFERENCE: return UNRESOLVED_REFERENCE;
        case VALUE_PACKAGE: return PACKAGE;
      }
      throw new IllegalStateException("Unknown type value: " + value);
    }

    Type(int serializedValue) {
      this.value = serializedValue;
    }
  }

  /**
   * The semantics of ID is different depending on code block type.
   * For FILE code blocks it is a workspace-unique long, for other code blocks
   * it is a file-unique int. Full ID will involve both components.
   *
   * @return id
   */
  @SerializationIndex(1)
  String getId();

  /**
   * @return the type of this code block
   */
  @SerializationIndex(2)
  int getBlockType();

  /**
   * @return the end column of this code block relative to the first column on the line
   */
  @SerializationIndex(3)
  int getEndColumn();

  /**
   * @return the end line number of this code block relative to the file start
   */
  @SerializationIndex(4)
  int getEndLineNumber();

  /**
   * @return the name of this code block or {@code null} if the code block
   *         is anonymous
   */
  @SerializationIndex(5)
  String getName();

  /**
   * @return the start column of this code block relative to the first column on the line
   */
  @SerializationIndex(6)
  int getStartColumn();

  /**
   * @return the start line number of this code block relative to the file start
   */
  @SerializationIndex(7)
  int getStartLineNumber();

  /**
   * @return the list of the nested code blocks
   */
  @SerializationIndex(8)
  JsonArray<CodeBlock> getChildren();
}
