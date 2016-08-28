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
 * Represents the set of files/directories under some base directory, arranged into a tree
 * structure.
 */
@RoutingType(type = RoutingTypes.GETDIRECTORYRESPONSE)
public interface GetDirectoryResponse extends ServerToClientDto {
  /**
   * The path of the base directory. If it refers to the workspace root then it
   * will be "/".
   */
  String getPath();

  /**
   * The tree of files and directories.
   */
  DirInfo getBaseDirectory();

  /**
   * The tree's version number.
   */
  String getRootId();
}
