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

import com.google.collide.dto.client.DtoClientImpls.WorkspaceInfoImpl;
import com.google.collide.dto.RunTarget;
import com.google.collide.dto.WorkspaceInfo;

/**
 * A helper class which does some casting which may not be valid for unit tests. Nothing really
 * requires us to do this in a helper but it is nice.
 */
public class WorkspaceInfoMutator {
  /**
   * Updates a workspace's {@link RunTarget}.
   */
  public void updateRunTarget(WorkspaceInfo workspace, RunTarget target) {
    WorkspaceInfoImpl workspaceImpl = (WorkspaceInfoImpl) workspace;
    workspaceImpl.setRunTarget(target);
  }
}