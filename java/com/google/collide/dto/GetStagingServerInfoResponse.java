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
 *
 * DTO for getting the user's mimic app id and related information.
 *
 *
 */
@RoutingType(type = RoutingTypes.GETSTAGINGSERVERINFORESPONSE)
public interface GetStagingServerInfoResponse extends ServerToClientDto {

  String getStagingServerAppId();

  // The latest / current mimic version ID. 
  int getLatestMimicVersionId();

  // The version ID of the user's mimic, specifically the
  // version id that was most recently deployed.
  int getLastKnownMimicVersionId();

  // If true, user's mimic should be auto-updated.
  boolean getAutoUpdate();

}
