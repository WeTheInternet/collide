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

package com.google.collide.server.shared.util;

import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;

/**
 * Calculates hashes of file contents and records metrics about the run time.
 */
public class FileHasher {
  public static ByteString getSha1(String contents) {
    ByteString sha1 = ByteString.copyFrom(Hashing.sha1().hashUnencodedChars(contents).asBytes());
    return sha1;
  }
}
