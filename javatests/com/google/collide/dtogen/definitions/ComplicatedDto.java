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

package com.google.collide.dtogen.definitions;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;

/**
 * DTO for testing that the DTO generator correctly generates client and server
 * implementations for object graphs (nested arrays, and maps).
 *
 */
@RoutingType(type = ComplicatedDto.TYPE)
public interface ComplicatedDto extends ServerToClientDto, ClientToServerDto {
  
  public static final int TYPE = 12346;
  
  public enum SimpleEnum {
    ONE, TWO, THREE
  }
  
  JsonArray<String> getFooStrings();

  int getIntId();

  SimpleEnum getSimpleEnum();

  JsonStringMap<SimpleDto> getMap();

  JsonArray<SimpleDto> getSimpleDtos();
  
  JsonArray<JsonStringMap<JsonArray<JsonStringMap<JsonStringMap<JsonArray<SimpleDto>>>>>>
      getNightmare();
  
  JsonArray<JsonArray<SimpleEnum>> getArrayOfArrayOfEnum();
}
