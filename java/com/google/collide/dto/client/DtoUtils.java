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

package com.google.collide.dto.client;

import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.client.Jso;


/**
 * Utilities to simplify working with DTOs.
 */
public class DtoUtils {

  /**
   * Deserializes a string into a DTO of type {@code T}.
   * 
   * @param types list of the deserialized DTO's expected types; should
   *        generally be one or more of of the constants declared in
   *        {@link RoutingTypes}.
   */
  @SuppressWarnings("unchecked")
  public static <T> T parseAsDto(String payload, int... types) {
    ServerToClientDto responseData = (ServerToClientDto) Jso.deserialize(payload);
    for (int type : types) {
      if (responseData.getType() == type) {
        return (T) responseData;
      }
    }
    throw new IllegalArgumentException("Unexpected dto type " + responseData.getType());
  }

  private DtoUtils() {
  }
}
