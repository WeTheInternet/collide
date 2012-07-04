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
import com.google.collide.dtogen.shared.RoutingType;

/**
 * Client-visible file diff.
 *
 */
@RoutingType(type = RoutingTypes.GETFILEDIFF)
public interface GetFileDiff extends ClientToServerDto {
  String getWorkspaceId();

  String getClientId();
  
  NodeMutationDto.MutationType getChangedType();

  String getPath();

  /**
   * Returns an optional node ID of the before node. If this method returns
   * null, the oldest version of the node in he workspace is used.
   */
  String getBeforeNodeId();

  /**
   * Returns an optional node ID of the after node. If this method returns null,
   * the most recent version of the node in he workspace is used.
   */
  String getAfterNodeId();

  /**
   * Returns true if only the stats should be included.
   */
  boolean isStatsOnly();
}
