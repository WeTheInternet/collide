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
 * A message meant to emulate a Tango invalidation using the push channel as the transport.
 *
 */
@RoutingType(type = RoutingTypes.INVALIDATIONMESSAGE)
public interface InvalidationMessage extends ServerToClientDto {
  
  /**
   * Returns the Tango object name.
   */
  public String getObjectName();
  
  /**
   * Returns the version encoded in a string representation of the long value.
   */
  public String getVersion();
  
  /**
   * Returns the payload, or null if it was too large.
   */
  public String getPayload();
}
