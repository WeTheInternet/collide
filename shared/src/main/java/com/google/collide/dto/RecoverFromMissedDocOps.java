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
 * Lets a client re-synchronize with the server's version of a file after being
 * momentarily offline or missing a doc op broadcast.
 *
 * <p>
 * To catch up to the server, the client needs to first ensure that his unacked
 * doc ops were applied. He can resend those via {@link #getClientId()} and
 * {@link #getDocOps2()}. The server will ignore the doc ops if has already seen
 * them.
 *
 * <p>
 * The client also needs to catch up with recent changes made by others. The
 * server will provide all doc ops applied from {@link #getCurrentCcRevision()}
 * +1 onwards in the {@link RecoverFromMissedDocOpsResponse#getDocOps()}. That
 * list will include the client's own re-sent doc ops (if any). Their position
 * will depend on whether the original request made it through to the server: if
 * yes, then they'll be near the beginning of the list of applied doc ops; if
 * not they will be at the end.
 */
@RoutingType(type = RoutingTypes.RECOVERFROMMISSEDDOCOPS)
public interface RecoverFromMissedDocOps extends ClientToServerDto {
  String getWorkspaceId();

  String getFileEditSessionKey();

  /**
   * Revision of the client's document. This will be the intended revision for
   * any doc ops being re-sent. Also, the list of applied doc ops in the
   * {@link RecoverFromMissedDocOpsResponse} will start (exclusive) at this
   * revision.
   */
  int getCurrentCcRevision();

  /**
   * Optional. Set when the client needs to resend un-acked doc ops.
   */
  String getClientId();

  /**
   * Optional. Unacked doc-ops that may need to be applied (depending on whether
   * the server received them prior to the client disconnection).
   * 
   * These are the serialized {@link DocOp}s.
   */
  JsonArray<String> getDocOps2();
}
