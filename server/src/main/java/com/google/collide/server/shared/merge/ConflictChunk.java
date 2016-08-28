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

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Represents a chunk of text that could not be automatically merged. After the
 * necessary edits, it can be marked as resolved. It can subsequently also be
 * marked as unresolved (i.e. you can undo marking it as resolved).
 *
 */
public class ConflictChunk extends MergeChunk {

  private boolean isResolved = false;

  public ConflictChunk(MergeChunk chunk) {
    this(chunk, false);
  }

  public ConflictChunk(MergeChunk chunk, boolean isResolved) {
    super(chunk);
    this.isResolved = isResolved;
  }

  public boolean isResolved() {
    return isResolved;
  }

  public void markResolved(boolean isResolved) {
    this.isResolved = isResolved;
  }

  @Override
  public String toString() {
    return toStringFields(Objects.toStringHelper(this)).toString();
  }

  @Override
  protected ToStringHelper toStringFields(ToStringHelper toStringHelper) {
    return super.toStringFields(toStringHelper).add("isResolved", isResolved);
  }
}
