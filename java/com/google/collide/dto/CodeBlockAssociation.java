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
 */
@RoutingType(type = RoutingTypes.CODEBLOCKASSOCIATION)
public interface CodeBlockAssociation extends ServerToClientDto, CompactJsonDto {

  /**
   * @return file id part of the source code block
   * @see CodeBlock#getId()
   */
  @SerializationIndex(1)
  String getSourceFileId();

  /**
   * @return local ID part of the source code block
   * @see CodeBlock#getId()
   */
  @SerializationIndex(2)
  String getSourceLocalId();

  /**
   * @return file ID part of the target code block
   * @see CodeBlock#getId()
   */
  @SerializationIndex(3)
  String getTargetFileId();

  /**
   * @return local ID part of the target code block
   * @see CodeBlock#getId()
   */
  @SerializationIndex(4)
  String getTargetLocalId();

  /**
   * @return if {@code true}, indicate that this association should be
   *         considered with target code block, otherwise only children of
   *         target code block should be considered.
   */
  @SerializationIndex(5)
  boolean getIsRootAssociation();
}
