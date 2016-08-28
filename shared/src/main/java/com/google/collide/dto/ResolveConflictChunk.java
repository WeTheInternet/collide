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

import com.google.collide.dto.NodeConflictDto.ConflictHandle;
import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;

/**
 * Message sent by the client to resolve (or un-resolve) a conflict chunk.
 */
@RoutingType(type = RoutingTypes.RESOLVECONFLICTCHUNK)
public interface ResolveConflictChunk extends ClientToServerDto {

  String getWorkspaceId();

  String getFileEditSessionKey();

  /**
   * Array index of the conflict chunk within the file.
   */
  int getConflictChunkIndex();

  /**
   * Set to <code>true</code> to mark the conflict chunk as resolved. It can
   * also be set to <code>false</code> to "un-resolve" a conflict chunk (e.g.,
   * if the user clicks an undo link after resolving a conflict).
   */
  boolean isResolved();
  
  ConflictHandle getConflictHandle();
}
