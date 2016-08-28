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
 * DTO for basic information for displaying a node in a history listing.
 */
public interface NodeHistoryInfo extends TreeNodeInfo {
  // This is a long serialized to a decimal string. We do this because Longs
  // wont fit in JS numeric types.
  String getCreationTime();

  String getPredecessorId();
}
