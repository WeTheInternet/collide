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

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.json.shared.JsonArray;

/**
 * Serialized doc op (and related data) sent from the client to the server.
 *
 * <p>
 * Note that this message is sent/received on our BrowserChannel, NOT on the
 * REST API for our frontend.
 */
@RoutingType(type = RoutingTypes.CLIENTTOSERVERDOCOP)
public interface ClientToServerDocOp extends ClientToServerDto {
  String getWorkspaceId();

  String getFileEditSessionKey();

  /**
   * @return the (concurrency control) revision of the document that the
   *         client's doc op applies to
   */
  int getCcRevision();

  /**
   * @return the author of this doc op
   */
  String getClientId();

  /**
   * Serialized DTOs. This needs to be a String since we have a custom deserializer on the server.
   * 
   * {@link DocOp} DTOs.
   */
  JsonArray<String> getDocOps2();

  /**
   * If the user has explicitly changed the selection since the last time this
   * DTO was sent, this will return the selection of the user. The "explicit"
   * distinction is important: In most cases while the user is typing, the
   * cursor will be moved implicitly, and passing those incremental cursor
   * position changes via this field is not required.
   *
   * <p>
   * The positions inside the selection already account for the positional
   * changes that may occur due to the document operations within this DTO.
   *
   * <p>
   * For example imagine the document is empty prior to this DTO. The user types
   * 'a', and then explicitly positions his cursor at column 0 again. The user
   * then types 'b'. The position of the selection (both cursor and base
   * positions) will be column 1.
   *
   * <p>   * 
   * {@link DocumentSelection} DTO.
   */
  DocumentSelection getSelection();
}
