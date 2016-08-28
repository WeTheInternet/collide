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
 * Response containing information for deploying a workspace (the app.yamls, app
 * ids, version ids, etc). This is done on the server side because the deploy
 * button is on the project landing page, where we can't get app.yamls from
 * workspaces easily.
 * 
 */
@RoutingType(type = RoutingTypes.GETDEPLOYINFORMATIONRESPONSE)
public interface GetDeployInformationResponse extends ServerToClientDto {

  public interface DeployInformation {
    // the path of the app.yaml file that this information came from.
    String getAppYamlPath();

    // The app id from the app.yaml
    String getAppId();

    // The app version from the app.yaml
    String getVersion();
  }

  // An array of all the app.yaml's and their information
  // in the workspace.
  JsonArray<DeployInformation> getDeployInformation();
}
