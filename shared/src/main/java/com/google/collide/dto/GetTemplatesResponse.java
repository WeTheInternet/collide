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
import com.google.collide.json.shared.JsonStringMap;

/**
 * Tells the client about templates. The template map is indexed by human name,
 * with the value being the tag used to request that template.
 *
 */
@RoutingType(type = RoutingTypes.GETTEMPLATESRESPONSE)
public interface GetTemplatesResponse extends ServerToClientDto {
  JsonStringMap<String> getTemplates();
}
