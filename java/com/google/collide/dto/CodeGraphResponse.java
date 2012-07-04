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
 * Code graph response object. May have
 *
 */
@RoutingType(type = RoutingTypes.CODEGRAPHRESPONSE)
public interface CodeGraphResponse extends ServerToClientDto {
  /**
   * @return freshness of the different response segments
   */
  CodeGraphFreshness getFreshness();

  /**
   * @return json-serialized {@link CodeGraph} object which includes
   *         code blocks and associations from immutable workspace libs
   *         (unused as of Jun 14)
   */
  String getLibsSubgraphJson();

  /**
   * @return json-serialized {@link CodeGraph} object which includes
   *         code blocks from the context file (unused as of Jun 14)
   */
  String getFileTreeJson();

  /**
   * @return json-serialized {@link CodeGraph} object which includes
   *         code blocks from all mutable workspace files
   *         (unused as of Jun 14)
   */
  String getWorkspaceTreeJson();

  /**
   * @return json-serialized {@link CodeGraph} object which includes
   *         links (associations and cross-references) between
   *         mutable workspace files (unused as of Jun 14)
   */
  String getWorkspaceLinksJson();

  /**
   * @return json-serialized {@link CodeGraph} object which includes
   *         code block trees of all workspace and library files and
   *         links (associations and cross-references) between
   *         workspace files and libs
   */
  String getFullGraphJson();

  /**
   * @return json-serialized {@link CodeReferences} object which includes
   *         references for the context file
   */
  String getFileReferencesJson();
}
