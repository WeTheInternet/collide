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

/**
 * Client-visible workspace information.
 */
@RoutingType(type = RoutingTypes.WORKSPACEINFO)
public interface WorkspaceInfo extends ServerToClientDto, ClientToServerDto {

  /**
   * The type of this workspaces.
   */
  public enum WorkspaceType {
    ACTIVE, SUBMITTED, TRUNK,   
  }

  /**
   * The user-specific state of the workspace.
   */
  public enum UserWorkspaceState {
    ACTIVE, ARCHIVED
  }

  String getOwningProjectId();

  String getDescription();

  String getId();

  /**
   * The ID of the parent workspace that spawned this workspace, or -1 if one
   * does not exists.
   */
  String getParentId();

  String getCreatedTime();

  String getArchivedTime();

  String getSubmissionTime();

  /**
   * Public workspaces are accessible to anyone with the link.
   */
  Visibility getVisibility();

  /**
   * This is the time that should be used for sorting. It is dependent on the
   * workspaceType.
   */
  String getSortTime();

  String getName();

  RunTarget getRunTarget();

  WorkspaceType getWorkspaceType();

  /**
   * This field will be null for non-submitted workspaces.
   */
  UserDetails getSubmitter();

  /**
   * Returns the current user's {@link Role} for this workspace. If the user is
   * not a member of the workspace, the return value will be {@value Role#NONE}.
   */
  Role getCurrentUserRole();

  /**
   * Returns the current user's {@link Role} for the parent workspace of this
   * workspace. If the user is not a member of the workspace, the return value
   * will be {@value Role#NONE}.
   */
  Role getCurrentUserRoleForParent();
}
