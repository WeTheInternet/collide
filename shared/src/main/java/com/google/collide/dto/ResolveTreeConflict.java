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
 * DTO to resolve a single tree conflict.
 * 
 * 
 */
@RoutingType(type = RoutingTypes.RESOLVETREECONFLICT)
public interface ResolveTreeConflict extends ClientToServerDto {

  public enum ConflictResolutionChoice {
    CHOOSE_PARENT, CHOOSE_CHILD, CHILD_AND_RENAME, PARENT_AND_RENAME;
  }

  ConflictResolutionChoice getResolutionChoice();

  ConflictHandle getConflictHandle();

  String getWorkspaceId();

  // If the resolution choice is RENAME_REMOTE or RENAME_LOCAL, the new path is
  // set here.
  String getNewPath();
}
