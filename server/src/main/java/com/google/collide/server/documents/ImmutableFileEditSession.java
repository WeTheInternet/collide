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

package com.google.collide.server.documents;

import com.google.protobuf.ByteString;

/**
 * A read-only view of a {@link FileEditSession}.
 */
public interface ImmutableFileEditSession {

  String getContents();
  
  int getSize();
  
  ByteString getSha1();

  String getFileEditSessionKey();

  boolean hasChanges();

  /**
   * @return last known saved full path of the file
   */
  String getSavedPath();

  boolean hasUnresolvedConflictChunks();
}
