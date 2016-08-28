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

import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Represents a part of a document that is the result of a 3-way merge.
 * 
 * <p>
 * Note: This class uses identity semantics for equality.
 * 
 */
public class MergeChunk {

  private final static int MAX_DISPLAY_LENGTH = 8;

  // Set iff hasConflict is true.
  private int startLine;

  // Set iff hasConflict is true.
  private int endLine;

  // Set iff hasConflict is true.
  private String baseData;

  // Set iff hasConflict is true.
  private String parentData;

  // Set iff hasConflict is true.
  private String childData;

  // Set iff hasConflict is false.
  private String mergedData;

  private boolean hasConflict;

  public MergeChunk() {
  }

  /*
   * Copy-constructor.
   */
  public MergeChunk(MergeChunk chunk) {
    this.startLine = chunk.startLine;
    this.endLine = chunk.endLine;
    this.baseData = chunk.baseData;
    this.childData = chunk.childData;
    this.parentData = chunk.parentData;
    this.mergedData = chunk.mergedData;
    this.hasConflict = chunk.hasConflict;
  }

  public String getBaseData() {
    return StringUtils.nullToEmpty(baseData);
  }

  public int getEndLine() {
    return endLine;
  }

  public String getChildData() {
    return StringUtils.nullToEmpty(childData);
  }

  public String getMergedData() {
    return mergedData;
  }

  public String getParentData() {
    return StringUtils.nullToEmpty(parentData);
  }

  public int getStartLine() {
    return startLine;
  }

  public boolean hasConflict() {
    return hasConflict;
  }

  public void setBaseData(String baseData) {
    this.baseData = baseData;
  }

  public void setEndLine(int line) {
    this.endLine = line;
  }

  public void setHasConflict(boolean hasConflict) {
    this.hasConflict = hasConflict;
  }

  public void setChildData(String childData) {
    this.childData = childData;
  }

  public void setMergedData(String mergedData) {
    this.mergedData = mergedData;
  }

  public void setParentData(String parentData) {
    this.parentData = parentData;
  }

  public void setStartLine(int line) {
    this.startLine = line;
  }

  @Override
  public String toString() {
    return toStringFields(Objects.toStringHelper(this)).toString();
  }

  protected ToStringHelper toStringFields(ToStringHelper toStringHelper) {
    return toStringHelper
        .add(
            "baseData",
            StringUtils.truncateAtMaxLength(StringUtils.nullToEmpty(baseData), MAX_DISPLAY_LENGTH,
                true))
        .add(
            "parentData",
            StringUtils.truncateAtMaxLength(StringUtils.nullToEmpty(parentData), MAX_DISPLAY_LENGTH,
                true))
        .add(
            "childData",
            StringUtils.truncateAtMaxLength(StringUtils.nullToEmpty(childData), MAX_DISPLAY_LENGTH,
                true))
        .add(
            "mergedData",
            StringUtils.truncateAtMaxLength(StringUtils.nullToEmpty(mergedData), MAX_DISPLAY_LENGTH,
                true)).add("hasConflict", hasConflict).add("startLine", startLine)
        .add("endLine", endLine);
  }
}
