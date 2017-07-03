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

package com.google.collide.server.workspace;

import com.google.collide.dto.server.DtoServerImpls.GetWorkspaceMetaDataResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.RunTargetImpl;
import com.google.collide.server.shared.BusModBase;
import com.google.collide.server.shared.util.Dto;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import xapi.log.X_Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent workspace state.
 */
public class WorkspaceState extends BusModBase {
  private RunTargetImpl runTarget = RunTargetImpl.make().setRunMode("PREVIEW_CURRENT_FILE");
  private String addressBase;
  private String lastOpenedFileId;
  private String webRoot;

  @Override
  public void start() {
    super.start();
    this.addressBase = getOptionalStringConfig("address", "workspace");
    this.webRoot = getMandatoryStringConfig("webRoot");

    vertx.eventBus()
        .consumer(addressBase + ".getMetaData", requestEvent -> {
            final GetWorkspaceMetaDataResponseImpl metaData =
                GetWorkspaceMetaDataResponseImpl.make()
                    .setRunTarget(runTarget).setWorkspaceName(webRoot);
            X_Log.trace(getClass(), "Last opened file: ", lastOpenedFileId);
            if (lastOpenedFileId != null) {
              // Resolve file to a path.
              vertx.eventBus().<JsonObject>send("tree.getCurrentPaths", new JsonObject().put(
                  "resourceIds", new JsonArray().add(lastOpenedFileId)),
                  async -> {
                      Message<JsonObject> event = async.result();
                      List<String> openFiles = new ArrayList<String>();
                      openFiles.addAll(event.body().getJsonArray("paths").getList());
                      metaData.setLastOpenFiles(openFiles);
                      requestEvent.reply(Dto.wrap(metaData));
                  });
            }
        });

    vertx.eventBus()
        .<JsonObject>consumer(addressBase + ".setLastOpenedFile", event->lastOpenedFileId = event.body().getString("resourceId"));

    vertx.eventBus()
        .<JsonObject>consumer(addressBase + ".updateRunTarget",event-> {
            RunTargetImpl runTarget = RunTargetImpl.fromJsonString(Dto.get(event));
            WorkspaceState.this.runTarget = runTarget;
        });
  }
}
