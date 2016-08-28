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

package com.google.collide.server.shared.util;

import com.google.collide.dtogen.server.JsonSerializable;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Utility for wrapping and unwrapping serialized Dtos.
 */
public class Dto {
  public static String get(Message<JsonObject> vertxMsg) {
    String serializedDto = vertxMsg.body().getString("dto", null);
    if (serializedDto == null) {
      throw new IllegalArgumentException("Missing dto field on vertx message!");
    }
    return serializedDto;
  }

  public static <T extends JsonSerializable> JsonObject wrap(T dto) {
    return wrap(dto.toJson());
  }

  public static JsonObject wrap(String serializedDto) {
    return new JsonObject().put("dto", serializedDto);
  }
}
