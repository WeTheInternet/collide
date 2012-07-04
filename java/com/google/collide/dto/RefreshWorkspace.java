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
 * Notification to the client that it needs to refresh part of its workspace.
 * This might be needed, for example, if the workspace was involved in a merge.
 */
@RoutingType(type = RoutingTypes.REFRESHWORKSPACE)
public interface RefreshWorkspace extends ServerToClientDto {

  String getWorkspaceId();

  /**
   * Directory that needs to be refreshed. "/" is sent if the whole workspace
   * needs to be refreshed.
   */
  String getBasePath();
}
