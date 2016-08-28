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

package com.google.collide.shared.util;

import com.google.collide.dto.ProjectInfo;
import com.google.collide.dto.Role;
import com.google.collide.dto.WorkspaceInfo;

/**
 * Utility methods for {@link Role} related functionality.
 */
public class RoleUtils {

  /**
   * A tool that authenticates a user.
   */
  public static interface Authenticator {
    /**
     * Check if the user role satisfies the minimum required authorization.
     * 
     * @param userRole the user role
     * @return true if authorized, false if not authorized or userRole is null
     */
    boolean isAuthorized(Role userRole);
  }

  /**
   * A tool that authenticates a user for a project.
   */
  public static interface ProjectAuthenticator extends Authenticator {
    /**
     * Check if the user role satisfies the minimum required authorization for
     * the specified project.
     * 
     * @param project the project to authorize
     * @return true if authorized, false if not authorized or project is null
     */
    boolean isAuthorized(ProjectInfo project);
  }

  /**
   * A tool that authenticates a user for a workspace.
   */
  public static interface WorkspaceAuthenticator extends Authenticator {
    /**
     * Check if the user role satisfies the minimum required authorization.
     * 
     * @param workspace the workspace to authorize
     * @return true if authorized, false if not authorized or workspace is null
     */
    boolean isAuthorized(WorkspaceInfo workspace);
  }

  /**
   * An authenticator that compares the user's role against an array of implied
   * roles.
   */
  private static class AuthenticatorImpl implements ProjectAuthenticator, WorkspaceAuthenticator {
    private final Role[] impliedRoles;

    private AuthenticatorImpl(Role... impliedRoles) {
      this.impliedRoles = impliedRoles;
    }

    @Override
    public boolean isAuthorized(Role userRole) {
      for (Role aRole : impliedRoles) {
        if (aRole.equals(userRole)) {
          return true;
        }
      }

      return false;
    }

    @Override
    public boolean isAuthorized(ProjectInfo project) {
      return (project == null) ? false : isAuthorized(project.getCurrentUserRole());
    }

    @Override
    public boolean isAuthorized(WorkspaceInfo workspace) {
      return (workspace == null) ? false : isAuthorized(workspace.getCurrentUserRole());
    }
  }

  public static WorkspaceAuthenticator WORKSPACE_OWNER_AUTHENTICATOR = new AuthenticatorImpl(
      Role.OWNER);
  public static WorkspaceAuthenticator WORKSPACE_CONTRIBUTOR_AUTHENTICATOR = new AuthenticatorImpl(
      Role.OWNER, Role.CONTRIBUTOR);
  public static WorkspaceAuthenticator WORKSPACE_READER_AUTHENTICATOR = new AuthenticatorImpl(
      Role.OWNER, Role.CONTRIBUTOR, Role.READER);
  public static ProjectAuthenticator PROJECT_OWNER_AUTHENTICATOR =
      new AuthenticatorImpl(Role.OWNER);
  public static ProjectAuthenticator PROJECT_CONTRIBUTOR_AUTHENTICATOR = new AuthenticatorImpl(
      Role.OWNER, Role.CONTRIBUTOR);
  public static ProjectAuthenticator PROJECT_READER_AUTHENTICATOR = new AuthenticatorImpl(
      Role.OWNER, Role.CONTRIBUTOR, Role.READER);

  /**
   * Checks if a workspace is read only for the current user.
   * 
   * @param userRole the current users workspace role, or null if unknown
   * @param forceReadOnly true to force read only
   * @return true if user has read only permissions, or if forceReadOnly is true
   */
  public static boolean isWorkspaceReadOnly(Role userRole, boolean forceReadOnly) {
    boolean isReadOnlyForUser = forceReadOnly;
    if (!isReadOnlyForUser) {
      isReadOnlyForUser = !WORKSPACE_CONTRIBUTOR_AUTHENTICATOR.isAuthorized(userRole);
    }
    return isReadOnlyForUser;
  }
}
