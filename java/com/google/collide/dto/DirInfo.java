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

import com.google.collide.json.shared.JsonArray;

/**
 * DTO for client-visible directory information in a directory listing.
 *
 * This defines a recursive structure.
 */
public interface DirInfo extends TreeNodeInfo {
  JsonArray<FileInfo> getFiles();

  JsonArray<DirInfo> getSubDirectories();

  /**
   * @return whether or not this node has been populated. We support lazy
   *         querying of the file tree structure. If this is false, then it
   *         means we need to run another query for file/directory information
   *         starting at this node.
   */
  boolean isComplete();
}
