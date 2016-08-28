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
 * A list of mutations to the workspace tree to perform.These get sent to the FE, applied to the
 * file tree, and then broadcast to other active clients in the workspace.
 *
 * A rename is just a type of MOVE operation.
 */
@RoutingType(type = RoutingTypes.WORKSPACETREEUPDATE)
public interface WorkspaceTreeUpdate extends ClientToServerDto {

  /**
   * The active client ID of the author of the mutation.
   */
  String getAuthorClientId();

  /**
   * The mutations
   */
  JsonArray<Mutation> getMutations();
}

