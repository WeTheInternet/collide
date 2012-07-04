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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.collide.dto.ProjectInfo;
import com.google.collide.dto.Role;
import com.google.collide.dto.WorkspaceInfo;
import com.google.collide.shared.util.RoleUtils.Authenticator;
import com.google.collide.shared.util.RoleUtils.ProjectAuthenticator;
import com.google.collide.shared.util.RoleUtils.WorkspaceAuthenticator;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link RoleUtils}.
 */
public class RoleUtilsTests {

  ProjectInfo mockProject;
  WorkspaceInfo mockWorkspace;

  @Before
  public void createMocks() {
    mockProject = createMock(ProjectInfo.class);
    mockWorkspace = createMock(WorkspaceInfo.class);
  }

  @Test
  public void testIsWorkspaceReadOnly() {
    assertTrue(RoleUtils.isWorkspaceReadOnly(Role.READER, false));
    assertTrue(RoleUtils.isWorkspaceReadOnly(Role.READER, true));

    assertFalse(RoleUtils.isWorkspaceReadOnly(Role.CONTRIBUTOR, false));
    assertTrue(RoleUtils.isWorkspaceReadOnly(Role.CONTRIBUTOR, true));

    assertFalse(RoleUtils.isWorkspaceReadOnly(Role.OWNER, false));
    assertTrue(RoleUtils.isWorkspaceReadOnly(Role.OWNER, true));
  }

  @Test
  public void testWorkspaceOwnerAuthenticator() {
    runWorkspaceAuthenticatorTests(RoleUtils.WORKSPACE_OWNER_AUTHENTICATOR, true, false, false);
  }

  @Test
  public void testWorkspaceContributorAuthenticator() {
    runWorkspaceAuthenticatorTests(RoleUtils.WORKSPACE_CONTRIBUTOR_AUTHENTICATOR, true, true,
        false);
  }

  @Test
  public void testWorkspaceReaderAuthenticator() {
    runWorkspaceAuthenticatorTests(RoleUtils.WORKSPACE_READER_AUTHENTICATOR, true, true, true);
  }

  @Test
  public void testProjectOwnerAuthenticator() {
    runProjectAuthenticatorTests(RoleUtils.PROJECT_OWNER_AUTHENTICATOR, true, false, false);
  }

  @Test
  public void testProjectContributorAuthenticator() {
    runProjectAuthenticatorTests(RoleUtils.PROJECT_CONTRIBUTOR_AUTHENTICATOR, true, true, false);
  }

  @Test
  public void testProjectReaderAuthenticator() {
    runProjectAuthenticatorTests(RoleUtils.PROJECT_READER_AUTHENTICATOR, true, true, true);
  }

  private void runProjectAuthenticatorTests(ProjectAuthenticator authenticator, boolean owner,
      boolean contributor, boolean reader) {
    // Test isAuthorized(Role)
    runAuthenticatorTests(authenticator, owner, contributor, reader);

    // Test isAuthorized(ProjectInfo)
    expect(mockProject.getCurrentUserRole()).andReturn(Role.OWNER);
    replay(mockProject, mockWorkspace);
    assertEquals(owner, authenticator.isAuthorized(mockProject));
    verifyAndReset(mockProject, mockWorkspace);

    expect(mockProject.getCurrentUserRole()).andReturn(Role.CONTRIBUTOR);
    replay(mockProject, mockWorkspace);
    assertEquals(contributor, authenticator.isAuthorized(mockProject));
    verifyAndReset(mockProject, mockWorkspace);

    expect(mockProject.getCurrentUserRole()).andReturn(Role.READER);
    replay(mockProject, mockWorkspace);
    assertEquals(reader, authenticator.isAuthorized(mockProject));
    verifyAndReset(mockProject, mockWorkspace);

    expect(mockProject.getCurrentUserRole()).andReturn(null);
    replay(mockProject, mockWorkspace);
    assertFalse(authenticator.isAuthorized(mockProject));
    verifyAndReset(mockProject, mockWorkspace);
  }

  private void runWorkspaceAuthenticatorTests(WorkspaceAuthenticator authenticator, boolean owner,
      boolean contributor, boolean reader) {
    // Test isAuthorized(Role)
    runAuthenticatorTests(authenticator, owner, contributor, reader);

    // Test isAuthorized(WorkspaceInfo)
    expect(mockWorkspace.getCurrentUserRole()).andReturn(Role.OWNER);
    replay(mockProject, mockWorkspace);
    assertEquals(owner, authenticator.isAuthorized(mockWorkspace));
    verifyAndReset(mockProject, mockWorkspace);

    expect(mockWorkspace.getCurrentUserRole()).andReturn(Role.CONTRIBUTOR);
    replay(mockProject, mockWorkspace);
    assertEquals(contributor, authenticator.isAuthorized(mockWorkspace));
    verifyAndReset(mockProject, mockWorkspace);

    expect(mockWorkspace.getCurrentUserRole()).andReturn(Role.READER);
    replay(mockProject, mockWorkspace);
    assertEquals(reader, authenticator.isAuthorized(mockWorkspace));
    verifyAndReset(mockProject, mockWorkspace);

    expect(mockWorkspace.getCurrentUserRole()).andReturn(null);
    replay(mockProject, mockWorkspace);
    assertFalse(authenticator.isAuthorized(mockWorkspace));
    verifyAndReset(mockProject, mockWorkspace);
  }

  private void runAuthenticatorTests(Authenticator authenticator, boolean owner,
      boolean contributor, boolean reader) {
    assertEquals(owner, authenticator.isAuthorized(Role.OWNER));
    assertEquals(contributor, authenticator.isAuthorized(Role.CONTRIBUTOR));
    assertEquals(reader, authenticator.isAuthorized(Role.READER));
    assertFalse(authenticator.isAuthorized((Role) null));
  }

  private void verifyAndReset(Object... objects) {
    verify(objects);
    reset(objects);
  }
}
