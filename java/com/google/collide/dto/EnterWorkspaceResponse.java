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

import com.google.collide.dto.GetSyncStateResponse.SyncState;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;

/**
 * Response to a client's request to enter a workspace.
 * 
 */
@RoutingType(type = RoutingTypes.ENTERWORKSPACERESPONSE)
public interface EnterWorkspaceResponse extends ServerToClientDto {
  boolean isReadOnly();

  /**
   * @return the file tree
   */
  GetDirectoryResponse getFileTree();

  /**
   * @return the current workspace participants, including this user
   */
  JsonArray<ParticipantUserDetails> getParticipants();
  
  /**
   * @return the version of the participants returned by {@link #getParticipants()}
   */
  String getParticipantsNextVersion();

  /**
   * @return the sync state of the workspace
   */
  SyncState getSyncState();

  /**
   * @return the current user-specific workspace settings.
   */
  GetWorkspaceMetaDataResponse getUserWorkspaceMetadata();
  
  /**
   * @return the keep-alive timer's interval in ms.
   */
  int getKeepAliveTimerIntervalMs();

  /**
   * @return the workspaceId of this response
   */
  String getWorkspaceId();
  
  /**
   * @return retrieves the workspace info for this workspace
   */
  WorkspaceInfo getWorkspaceInfo();

  String getWorkspaceSessionHost();
}
