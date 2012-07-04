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
import com.google.collide.json.shared.JsonArray;

/**
 * Response with a set of projects.
 */
@RoutingType(type = RoutingTypes.GETPROJECTSRESPONSE)
public interface GetProjectsResponse extends ServerToClientDto {

  JsonArray<ProjectInfo> getProjects();

  /**
   * Returns the IDs of hidden projects.
   */
  JsonArray<String> getHiddenProjectIds();

  /**
   * Returns the ID of the last project that the user viewed.
   */
  String getActiveProjectId();
  
  /**
   * Returns the next version of the tango object representing this user's
   * membership change events.
   */
  String getUserMembershipChangeNextVersion();
}
