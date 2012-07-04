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

/**
 * A user's membership role in a project or workspace.
 */
public enum Role {
  /**
   * Owners have all privileges of contributors, plus they can edit settings and
   * add members.
   */
  OWNER,

  /**
   * Contributors can edit code. Project contributors can submit to the root
   * workspace of a project.
   */
  CONTRIBUTOR,

  /**
   * Readers can view code and projects, but cannot modify anything.
   */
  READER,

  /**
   * The user's membership status should be revoked, or the request should be
   * ignored.
   */
  NONE,

  /**
   * The user requested membership, but has not been granted membership.
   */
  PENDING,

  /**
   * The user is blocked from requesting membership.
   */
  BLOCKED
}
