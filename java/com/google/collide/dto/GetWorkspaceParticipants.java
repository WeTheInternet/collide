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
import com.google.collide.json.shared.JsonArray;

/**
 * Request the current participants of a workspace.
 */
@RoutingType(type = RoutingTypes.GETWORKSPACEPARTICIPANTS)
public interface GetWorkspaceParticipants extends ClientToServerDto {
  String getWorkspaceId();

  /**
   * An array of participant IDs to request. The client only requests
   * participant info for participants that it has not seen.
   * 
   * <p>
   * If the list of participants is null or not set, all participants are
   * requested.
   */
  JsonArray<String> getParticipantIds();
}
