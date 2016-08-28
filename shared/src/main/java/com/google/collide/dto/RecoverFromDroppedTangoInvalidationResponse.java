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
import com.google.collide.json.shared.JsonArray;

/**
 * Response to {@link RecoverFromDroppedTangoInvalidation} that contains the
 * payloads requested by the client.
 *
 */
@RoutingType(type = RoutingTypes.RECOVERFROMDROPPEDTANGOINVALIDATIONRESPONSE)
public interface RecoverFromDroppedTangoInvalidationResponse extends ServerToClientDto {

  /**
   * Small DTO representing a recovered payload
   */
  public interface RecoveredPayload {
    public int getPayloadVersion();

    public String getPayload();
  }

  /**
   * List of payloads recovered from the server. Is always monotonically
   * increasing but may have holes if there was no payload for a given version.
   */
  JsonArray<RecoveredPayload> getPayloads();

  /** The current version of the object */
  int getCurrentObjectVersion();
}
