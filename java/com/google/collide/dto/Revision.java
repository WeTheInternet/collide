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
 * Wrapper class for a revision for a certain file resource. Holds information that the Timeline
 * class needs to draw timeline nodes and information, as well as the nodeId from version control to
 * fetch the file contents at that revision.
 */
public interface Revision {
  public enum RevisionType {
    BRANCH,
    /**
     * SYNC_SOURCE contains the content from parent.
     */
    SYNC_SOURCE,
    /**
     * SYNC_MERGED contains the merged content from parent and local. It may
     * have conflicts.
     */
    SYNC_MERGED, AUTO_SAVE, DELETE, MOVE, COPY;
  }

  String getTimestamp();

  String getNodeId();

  String getRootId();

  RevisionType getRevisionType();

  boolean getHasUnresolvedConflicts();

  boolean getIsFinalResolution(); 

  /**
   * Get the number of nodes skipped between previous node and this node.-1
   * means UNKNOWN number of skipped nodes.
   */
  int getPreviousNodesSkipped();
}
