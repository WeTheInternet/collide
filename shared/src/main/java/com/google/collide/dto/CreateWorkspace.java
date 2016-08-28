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
 * Request to create a project.
 *
 */
@RoutingType(type = RoutingTypes.CREATEWORKSPACE)
public interface CreateWorkspace extends ClientToServerDto {

  // TODO: we need a way to express derivation from a golden branch,
  // or golden revision, not just tip-of-trunk
  /**
   * Returns either a workspace id in string form, or {@code null} to derive
   * from the project's trunk.
   *
   * @return workspace id, or {@code null}.
   */
  String getBaseWorkspaceId();

  String getProjectId();

  String getName();
  
  String getDescription();
}
