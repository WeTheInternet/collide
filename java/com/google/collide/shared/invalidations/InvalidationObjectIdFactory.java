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

package com.google.collide.shared.invalidations;

import com.google.collide.dto.EndUploadSessionFinished;
import com.google.collide.dto.WorkspaceTreeUpdate;
import com.google.collide.shared.invalidations.InvalidationObjectId.VersioningRequirement;
import com.google.collide.shared.invalidations.InvalidationUtils.InvalidationObjectPrefix;

/**
 * A factory which can create a new {@link InvalidationObjectId}.
 *
 */
public class InvalidationObjectIdFactory {

  /** Creates an InvalidationObjectIdFactory ignoring the workspace id */
  public static InvalidationObjectIdFactory create() {
    return new InvalidationObjectIdFactory("IGNORED");
  }

  private final String workspaceId;

  public InvalidationObjectIdFactory(String workspaceId) {
    this.workspaceId = workspaceId;
  }

  public InvalidationObjectId<WorkspaceTreeUpdate> makeWorkspaceFileTreeId() {
    return new InvalidationObjectId<WorkspaceTreeUpdate>(
        InvalidationObjectPrefix.FILE_TREE_MUTATION, workspaceId, VersioningRequirement.PAYLOADS);
  }

  public InvalidationObjectId<String> makeFileTreeInvalidatedId() {
    return new InvalidationObjectId<String>(InvalidationObjectPrefix.FILE_TREE_INVALIDATED,
        workspaceId, VersioningRequirement.VERSION_ONLY);
  }

  public InvalidationObjectId<EndUploadSessionFinished> makeEndUploadSessionFinished(
      String sessionId) {
    return new InvalidationObjectId<EndUploadSessionFinished>(
        InvalidationObjectPrefix.END_UPLOAD_SESSION_FINISHED, sessionId,
        VersioningRequirement.PAYLOADS);
  }
}
