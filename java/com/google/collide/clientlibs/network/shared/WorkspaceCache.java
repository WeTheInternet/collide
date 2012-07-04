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

package com.google.collide.clientlibs.network.shared;

import com.google.collide.clientlibs.model.Workspace;
import com.google.collide.dto.GetWorkspaceMetaDataResponse;
import com.google.collide.dto.WorkspaceInfo;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;


/**
 * A cache which contains any and all {@link Workspace} objects. In the current implementation
 * user's can only access a single workspace but in this helps us handle multiple workspaces in the
 * future.
 *
 */
public class WorkspaceCache {
  /**
   * Map of workspace ID -> {@link WorkspaceInfo}
   */
  private final JsonStringMap<WorkspaceImpl> branches = JsonCollections.createMap();

  /**
   * Returns true if a workspace exists in the cache.
   */
  public boolean isWorkspaceCached(String branchId) {
    return getCachedWorkspaceById(branchId) != null;
  }
  
  /**
   * Returns the {@link WorkspaceInfo} referred to by the workspace ID from the
   * cache.
   */
  public WorkspaceImpl getCachedWorkspaceById(String branchId) {
    return branches.get(branchId);
  }

  /**
   * Caches a workspace; updates the cached {@link Workspace}'s {@link WorkspaceInfo} if it is
   * already cached.
   * 
   * @return the cached {@link Workspace}.
   */
  public WorkspaceImpl cacheWorkspace(
      String workspaceId, final GetWorkspaceMetaDataResponse workspaceInfo) {
    WorkspaceImpl workspace = getCachedWorkspaceById(workspaceId);
    if (workspace == null) {
      workspace = new WorkspaceImpl(workspaceInfo);
      branches.put(workspaceId, workspace);
    } else {
      workspace.setWorkspaceInfo(workspaceInfo);
    }

    return workspace;
  }
}
