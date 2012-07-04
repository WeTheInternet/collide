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

package com.google.collide.server.shared.merge;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Captures the result of a 3-way-merge with {@link JGitMerger}.
 *
 * <p>Note: This class uses identity semantics for equality.
 */
public class MergeResult {
  private final ImmutableList<MergeChunk> mergeChunks;
  private final String mergedText;

  public MergeResult(List<MergeChunk> mergeChunks, String mergedText) {
    this.mergeChunks = ImmutableList.copyOf(mergeChunks);
    this.mergedText = mergedText;
  }

  /**
   * Returns a list of merge chunks that represent the result of the merge in
   * chunks.
   */
  public List<MergeChunk> getMergeChunks() {
    return mergeChunks;
  }

  /**
   * Returns the merged text if the files merge cleanly.
   */
  public String getMergedText() {
    return mergedText;
  }
}
