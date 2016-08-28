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

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.CompactJsonDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.SerializationIndex;
import com.google.collide.dtogen.shared.ServerToClientDto;

/**
 * Represents character position in a single file.
 * 
 */
@RoutingType(type = RoutingTypes.FILEPOSITION)
public interface FilePosition extends ClientToServerDto, ServerToClientDto, CompactJsonDto {

  /**
   * @return line number starting from 0
   */
  @SerializationIndex(1)
  int getLineNumber();

  /**
   * @return column number starting from 0
   */
  @SerializationIndex(2)
  int getColumn();
}
