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
 * Information about coding errors for a single file.
 *
 * @see CodeError
 *
 */
@RoutingType(type = RoutingTypes.CODEERRORS)
public interface CodeErrors extends ServerToClientDto {

  /**
   * @return key of the file edit session of the file containing errors
   */
  String getFileEditSessionKey();

  /**
   * @return array of all errors found in a file
   */
  JsonArray<CodeError> getCodeErrors();
}
