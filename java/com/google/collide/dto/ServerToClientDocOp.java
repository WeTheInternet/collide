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
 * Serialized doc op (and related data) sent from the server to the client.
 *
 */
@RoutingType(type = RoutingTypes.SERVERTOCLIENTDOCOP)
public interface ServerToClientDocOp extends ServerToClientDto {

  String getWorkspaceId();

  /**
   * @return the (concurrency control) revision of the document after the doc op
   *         was applied
   */
  int getAppliedCcRevision();

  /**
   * @return the author of this doc op
   */
  String getClientId();

  /**
   * Applied DocOp ack.
   * 
   * {@link DocOp} DTO.
   */
  DocOp getDocOp2();

  String getFileEditSessionKey();

  /**
   * @return the current path to the mutated file.
   */
  String getFilePath();

  /**
   * If non-null, the positions contained by this field will be valid after
   * {@link #getDocOp2()} has been applied to the document.
   *
   * Note that this string is actually the JSON-serialized form of a
   * {@link DocumentSelection} DTO.
   *
   * @see ClientToServerDocOp#getSelection()
   */
  DocumentSelection getSelection();
}
