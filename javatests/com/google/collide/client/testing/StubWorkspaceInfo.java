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

package com.google.collide.client.testing;

import com.google.collide.clientlibs.model.Workspace;
import com.google.collide.clientlibs.network.shared.WorkspaceImpl;
import com.google.collide.dto.Role;
import com.google.collide.dto.RunTarget;
import com.google.collide.dto.UserDetails;
import com.google.collide.dto.Visibility;
import com.google.collide.dto.WorkspaceInfo;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;


/**
 * A stub workspace info which behaves just nice enough to be inserted and used.
 *
 */
public class StubWorkspaceInfo implements WorkspaceInfo {

  public static StubWorkspaceInfo make() {
    String idAndTime = String.valueOf(ID++);
    return new StubWorkspaceInfo(
        idAndTime, idAndTime, DEFAULT_PROJECT_ID, WorkspaceType.ACTIVE, Visibility.PRIVATE);
  }

  public static JsonArray<WorkspaceInfo> createMultiple(int mocks) {
    JsonArray<WorkspaceInfo> workspaces = JsonCollections.createArray();
    for (int i = 0; i < mocks; i++) {
      workspaces.add(StubWorkspaceInfo.make());
    }
    return workspaces;
  }
  
  public static JsonArray<Workspace> createMultipleAsWorkspace(int mocks) {
    JsonArray<Workspace> workspaces = JsonCollections.createArray();
    for (int i = 0; i < mocks; i++) {
      workspaces.add(StubWorkspaceInfo.make().asWorkspace());
    }
    return workspaces;
  }

  public static final String DEFAULT_PROJECT_ID = "TEST_PROJECT";
  public static final String DEFAULT_NAME = "stub";
  public static final String DEFAULT_DESCRIPTION = "stub branch";
  /**
   * Static field to get an id and a time from. Just increases forever.
   */
  private static int ID = 10;

  private String id;
  private String time;
  private String projectId;
  private String name = DEFAULT_NAME;
  private String description = DEFAULT_DESCRIPTION;
  private String parentId;
  private WorkspaceType workspaceType;
  private Role role;
  private Role parentRole;
  private Visibility visibility;

  private StubWorkspaceInfo(String id, String time, String projectId, WorkspaceType workspaceType,
      Visibility visibility) {
    this.id = id;
    this.time = time;
    this.projectId = projectId;
    this.workspaceType = workspaceType;
    this.visibility = visibility;
  }

  @Override
  public String getArchivedTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getCreatedTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOwningProjectId() {
    return projectId;
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public Role getCurrentUserRole() {
    return role;
  }

  @Override
  public Role getCurrentUserRoleForParent() {
    return parentRole;
  }

  @Override
  public RunTarget getRunTarget() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSortTime() {
    return time;
  }

  @Override
  public String getSubmissionTime() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public Visibility getVisibility() {
    return visibility;
  }

  @Override
  public UserDetails getSubmitter() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WorkspaceType getWorkspaceType() {
    return workspaceType;
  }

  @Override
  public int getType() {
    return 0;
  }

  public StubWorkspaceInfo setId(String id) {
    this.id = id;
    return this;
  }

  public StubWorkspaceInfo setTime(String time) {
    this.time = time;
    return this;
  }

  public StubWorkspaceInfo setWorkspaceType(WorkspaceType type) {
    this.workspaceType = type;
    return this;
  }

  public StubWorkspaceInfo setProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  public StubWorkspaceInfo setName(String name) {
    this.name = name;
    return this;
  }

  public StubWorkspaceInfo setDescription(String description) {
    this.description = description;
    return this;
  }

  public StubWorkspaceInfo setRole(Role role) {
    this.role = role;
    return this;
  }

  public StubWorkspaceInfo setParentRole(Role parentRole) {
    this.parentRole = parentRole;
    return this;
  }

  public StubWorkspaceInfo setVisibility(Visibility visibility) {
    this.visibility = visibility;
    return this;
  }
  
  public StubWorkspaceInfo setParentId(String parentId) {
    this.parentId = parentId;
    return this;
  }
  
  public WorkspaceImpl asWorkspace() {
    // TODO: something real?
    return new WorkspaceImpl(null);
  }
}
