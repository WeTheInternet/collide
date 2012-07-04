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

/**
 * Asks the server about template types that might be used to populate a new
 * project at creation time.
 */
@RoutingType(type = RoutingTypes.GETTEMPLATES)
public interface GetTemplates extends ClientToServerDto {
  /**
   * Locale for template names returned.  If unset, the server will look for
   * answers in locale "default", which we happen to initialize with English
   * names.
   */
  public String getLocale();
}
