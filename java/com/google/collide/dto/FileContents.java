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

import com.google.collide.dto.NodeConflictDto.ConflictHandle;
import com.google.collide.json.shared.JsonArray;

/**
 * DTO sent from the server to the client containing the contents of a File in
 * response to a {@link GetFileContents} message sent on the browser channel.
 */
public interface FileContents {

  public enum ContentType {
    TEXT,
    IMAGE,
    UNKNOWN_BINARY
  }

  /**
   * @return the (concurrency control) revision of the document
   */
  int getCcRevision();

  /**
   * The contents of the file encoded as a String.
   * 
   * If the ContentType is IMAGE, then this will be a Base64 encoded version of
   * the file.
   * 
   * If the ContentType is UNKNOWN_BINARY then this is null since we cannot do
   * anything useful yet with those bits. Consumers on the client can follow up
   * with a request in an iFrame or a tab or something. For now we don't handle
   * this in the Branch UI.
   */
  String getContents();

  ContentType getContentType();

  /**
   * @return the mimeType that the server would have served this file up as if
   *         it were fetched normally.
   */
  String getMimeType();

  /**
   * The key used for uniquely identifying this file's edit session.
   * 
   * This is set only if the file is UTF-8 text.
   */
  String getFileEditSessionKey();

  /**
   * @return the file path for the file we are retrieving the contents for.
   */
  String getPath();

  /**
   * @return the set of conflict chunks resulting from a merge.
   */
  JsonArray<ConflictChunk> getConflicts();

  /** An opaque handle to an associate out of date conflict on this file. */
  ConflictHandle getConflictHandle();

  /**
   * The list of serialized DocumentSelection DTO selections for collaborators
   * in this file.
   */
  JsonArray<String> getSelections();
}
