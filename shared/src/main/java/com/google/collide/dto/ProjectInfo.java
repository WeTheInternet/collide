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
 * Information about a project. Not a top level message, but a type that is
 * contained in some other messages.
 */
public interface ProjectInfo {

  /**
   * The user-specific state of the workspace.
   */
  public enum UserProjectState {
    ACTIVE, ARCHIVED
  }

  String getLogoUrl();

  String getId();

  String getName();

  String getSummary();

  /*
   * TODO: Make this a list of roots when we support them.
   */
  String getRootWsId();

  /**
   * Returns the current user's {@link Role} for this project. If the
   * user is not a member of the project, the return value will be
   * {@value Role#NONE}.
   */
  Role getCurrentUserRole();
}
