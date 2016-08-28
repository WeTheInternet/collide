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

import com.google.collide.dto.ResolveTreeConflict.ConflictResolutionChoice;
import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;

/**
 * Information that the client needs to display UI for resolving a conflicted
 * line of history.
 * 
 */
@RoutingType(type = RoutingTypes.NODECONFLICTDTO)
public interface NodeConflictDto extends ServerToClientDto, ClientToServerDto {

  public enum SimplifiedConflictType {
    FILE_LEVEL, TREE_LEVEL, RESOLVED;
  }

  /**
   * Handle to the specific conflict. This should be mostly opaque to the
   * client, except for equality checks on the underlying conflict ID to compare
   * instances of this.
   */
  public interface ConflictHandle {

    /**
     * The conflict id that the server uses to look up a conflicted line of
     * history.
     */
    String getConflictId();

    /**
     * The specific conflict in the line of history that we are presenting
     * resolution strategies for.
     */
    int getConflictIndex();
  }

  /** Simple descriptor to present a conflicted path in the UI. */
  public interface ConflictedPath {
    String getPath();

    /** {#TreeNodeInfo.DIR_TYPE} or {@TreeNodeInfo.FILE_TYPE}. */
    int getNodeType();

    String getWorkspaceId();

    String getStartId();

    /**
     * {@code true} if this is a UTF8 file.
     */
    boolean isUtf8();
  }

  /**
   * A handle to the specific conflict on the conflicted line of history that we
   * are presenting resolution UI for.
   */
  ConflictHandle getConflictHandle();

  /**
   * This is a server generated message that describes the conflict.
   */
  String getConflictDescription();

  /**
   * This is a server generated message that describes the child state.
   */
  String getChildDescription();

  /**
   * This is a server generated message that describes the parent state.
   */
  String getParentDescription();

  /** The resolution strategies that are presented to the user. */
  JsonArray<ConflictResolutionChoice> getValidResolutions();

  /**
   * Simple categorization of the conflict so the client need not do any
   * analysis.
   */
  SimplifiedConflictType getSimplifiedConflictType();

  /**
   * The path in the child workspace that is in conflict. If we have more than
   * one dependent conflicts, then they will be present in the
   * {@link #getGroupedConflicts()} list.
   */
  ConflictedPath getChildPath();

  /**
   * Other conflicted lines of history that should be resolved along with this
   * resolution. This can happen in the case of recursive conflicts dropped on
   * extant nodes in the child.
   */
  JsonArray<NodeConflictDto> getGroupedConflicts();

  /**
   * Paths that exist only in the parent workspace, but not in the child (for
   * example, recursive conflicts in the parent workspace) will get associated
   * with this node as "Parent paths". Note that we ensure that the matching
   * Parent path for the elected conflict indicated by the conflict index on
   * this node is at the 0th index in the list.
   * 
   * In other words:
   * 
   * Each individual conflict (a conflicted node, and an index into the list of
   * conflicts on that node) has a relevant child and parent path.
   * 
   * This NodeConflictDto potentially encapsulates multiple grouped conflicted
   * lines of history.
   * 
   * The path that is the "path in parent for our child path" lives in the first
   * spot in the list.
   */
  JsonArray<ConflictedPath> getParentPaths();
}
