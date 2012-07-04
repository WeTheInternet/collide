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
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;

/**
 * User metadata for a workspace. Not a top level message, but a type that is
 * contained in some other messages.
 */
@RoutingType(type = RoutingTypes.GETWORKSPACEMETADATARESPONSE)
public interface GetWorkspaceMetaDataResponse extends ClientToServerDto, ServerToClientDto {

  /**
   * The name of this workspace.
   */
  String getWorkspaceName();
  
  /**
   * Returns the list of open files for the current user.
   */
  JsonArray<String> getLastOpenFiles();

  /**
   * Returns the user's set run configuration.
   */
  RunTarget getRunTarget();
}
