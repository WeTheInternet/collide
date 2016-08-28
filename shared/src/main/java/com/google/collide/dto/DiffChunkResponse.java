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
 * DTO to capture a diff chunk, which may span an entire line or part of it.
 *
 */
public interface DiffChunkResponse {

  public enum DiffType {

    /**
     * An unchanged chunk.
     */
    UNCHANGED,

    /**
     * A modified chunk.
     */
    CHANGED,

    /**
     * An added chunk.
     */
    ADDED,

    /**
     * A removed chunk.
     */
    REMOVED,

    /**
     * The unchanged part of a line that has a <code>CHANGED</code> portion.
     */
    CHANGED_LINE,

    /**
     * The unchanged part of a line that has a <code>ADDED</code> portion.
     */
    ADDED_LINE,

    /**
     * The unchanged part of a line that has a <code>REMOVED</code> portion.
     */
    REMOVED_LINE;
  }

  String getAfterData();

  String getBeforeData();

  DiffType getDiffType();
}
