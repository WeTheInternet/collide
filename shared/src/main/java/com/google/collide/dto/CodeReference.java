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

/**
 * Representation of a reference from one code chunk to another.
 */
@RoutingType(type = RoutingTypes.CODEREFERENCE)
public interface CodeReference extends ServerToClientDto, CompactJsonDto {
  /**
   * Enumeration of currently supported reference types.
   */
  public static enum Type {
    /** Reference to var declaration or object property declaration. */
    VAR,

    /** Reference to function argument. */
    ARG,

    /** Reference to class declaration. */
    CLASS,

    /** Reference to called method declaration. */
    CALL
  }

  /**
   * @return type of this reference
   */
  @SerializationIndex(1)
  Type getReferenceType();

  /**
   * @return reference start position
   */
  @SerializationIndex(2)
  FilePosition getReferenceStart();

  /**
   * @return reference end position inclusive
   */
  @SerializationIndex(3)
  FilePosition getReferenceEnd();

  /**
   * @return project file path where this reference points
   */
  @SerializationIndex(4)
  String getTargetFilePath();

  /**
   * @return target start position in target file
   */
  @SerializationIndex(5)
  FilePosition getTargetStart();

  /**
   * @return target end position inclusive in target file
   */
  @SerializationIndex(6)
  FilePosition getTargetEnd();

  /**
   * @return target snippet if it is available, {@code null} otherwise
   */
  @SerializationIndex(7)
  String getTargetSnippet();
}
