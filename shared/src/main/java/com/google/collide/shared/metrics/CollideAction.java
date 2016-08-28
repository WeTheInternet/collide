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

package com.google.collide.shared.metrics;

/**
 * The specific implementation of a Action. These are the specific Actions that can be performed in
 * Collide.
 *
 */
public enum CollideAction implements Action {
  JOIN_COLLABORATIVE_SESSION,
  POST_FEEDBACK,
  CREATE_PROJECT,
  DELETE_PROJECT,
  OPEN_PROJECT,
  CREATE_WORKSPACE,
  OPEN_WORKSPACE,
  DELETE_WORKSPACE,
  CREATE_FILE,
  OPEN_FILE,
  DELETE_FILE,
  SYNC_WORKSPACE,
  RESOLVE_CONFLICT,
  SEARCH,
  SELECT_AUTOCOMPLETE_PROPOSAL,
  TREE_CONFLICT,
  FILE_CONFLICT,
  FORK_WORKSPACE,
  VISIT_LANDING_PAGE,
  LOAD_TEMPLATE,
  UPLOAD_FINISHED
}
