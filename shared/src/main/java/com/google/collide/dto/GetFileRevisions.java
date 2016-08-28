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

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;

/**
 * Request to get revision information for a file
 *
 */
@RoutingType(type = RoutingTypes.GETFILEREVISIONS)
public interface GetFileRevisions extends ClientToServerDto {

  String getClientId();

  /**
   * path is correct with respect to the PathRootId.
   */
  String getPathRootId();

  String getPath();

  String getWorkspaceId();

  /**
   * The number of revisions to return. When filtering is true, server will
   * apply filtering and skip unimportant nodes
   */
  int getNumOfRevisions();

  boolean filtering();

  /**
   * The root at which to start the search. If null, the search will use the
   * current tip.
   */
  String getRootId();
  
  /**
   * Bound the search by a minimum ID.
   */
  String getMinId();
  
  boolean getIncludeBranchRevision();

  boolean getIncludeMostRecentRevision();

}
