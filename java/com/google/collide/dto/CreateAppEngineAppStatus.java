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
 * 
 * A DTO for responding to requests from the client for creating
 * GAE apps. See the SetupMimic DTO.
 * 
 */
@RoutingType(type = RoutingTypes.CREATEAPPENGINEAPPSTATUS)
public interface CreateAppEngineAppStatus extends ServerToClientDto {

  /**
   * The app creation status.
   */
  public static enum Status {
    OK,
    APP_ID_UNAVAILABLE,
    MAX_APPS,
    // if the user has already created an app using app engine's create app api
    ALREADY_CREATED_APP,
    ERROR
  }

  Status getStatus();
}
