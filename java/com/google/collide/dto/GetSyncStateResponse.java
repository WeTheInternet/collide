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

import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;

/**
 * Request the current sync state of a workspace.
 *
 */
@RoutingType(type = RoutingTypes.GETSYNCSTATERESPONSE)
public interface GetSyncStateResponse extends ServerToClientDto {

  /**
   * The states that the syncing process can be in. Default is SHOULD_SYNC,
   * which is before the user selects to sync from parent.
   */
  public enum SyncState {
    /**
     * We have no changes, and there is nothing to pull in from the parent.
     */
    NOTHING_TO_SUBMIT,

    /**
     * There are changes available in the parent to be pulled in. Those changes
     * don't conflict with anything in our workspace.
     */
    SHOULD_SYNC,

    // TODO: SHOULD_SYNC_HAS_CONFLICTS is unused until we have
    // the FE notifications of changes in parent.
    /**
     * There are changes available in the parent to be pulled in. Those changes
     * have one or more conflicts with changes in our workspace.
     */
    SHOULD_SYNC_HAS_CONFLICTS,

    /**
     * We have synced, but there were conflicts. We are in the process of
     * resolving them.
     */
    RESOLVING_CONFLICTS,

    /**
     * We have all the parents changes (synced to parent's tip), and have
     * changes to submit.
     */
    READY_TO_SUBMIT;
  }

  SyncState getSyncState();
}