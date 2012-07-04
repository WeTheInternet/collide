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

import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;

/**
 * A DTO for the roll up of a single changed to a file or directory. The file or
 * directory can be modified (files only), added, removed, moved, or copied.
 *
 *  This change may NOT be a conflicting mutation. Conflicts are captured in a
 * separate NodeConflictDto.
 *
 */
@RoutingType(type = RoutingTypes.NODEMUTATIONDTO)
public interface NodeMutationDto extends ServerToClientDto {

  /**
   * The type of mutation. Note the MOVED and COPIED types may also carry
   * modifications. They are represented as MOVED_AND_EDITED, COPIED_AND_EDITED
   * and should have different before and after string keys (unless they are
   * null edits).
   */
  public static enum MutationType {
    ADDED, DELETED, EDITED, MOVED, COPIED, MOVED_AND_EDITED, COPIED_AND_EDITED;
  }

  MutationType getMutationType();

  String getFileEditSessionKey();

  String getNewPath();

  String getOldPath();

  /**
   * @return true if this node represents a FILE, or false if this node
   *         represents a DIRECTORY
   */
  boolean isFile();

  String getWorkspaceId();

  DiffStatsDto getDiffStats();
}
