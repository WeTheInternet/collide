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

import java.util.ArrayList;
import java.util.List;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import xapi.log.X_Log;

import com.google.collide.dto.server.DtoServerImpls.GetWorkspaceMetaDataResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.RunTargetImpl;
import com.google.collide.server.shared.util.Dto;

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
    this.addressBase = getOptionalStringConfig("address", "documents");
    this.webRoot = getMandatoryStringConfig("webRoot");

    vertx.eventBus()
        .registerHandler(addressBase + ".getMetaData", new Handler<Message<JsonObject>>() {
            @Override
          public void handle(final Message<JsonObject> requestEvent) {
            final GetWorkspaceMetaDataResponseImpl metaData =
                GetWorkspaceMetaDataResponseImpl.make()
                    .setRunTarget(runTarget).setWorkspaceName(webRoot);
            X_Log.error(lastOpenedFileId);
            if (lastOpenedFileId != null) {
              // Resolve file to a path.
              vertx.eventBus().send("tree.getCurrentPaths", new JsonObject().putArray(
                  "resourceIds", new JsonArray().addString(lastOpenedFileId)),
                  new Handler<Message<JsonObject>>() {
                      @Override
                    public void handle(Message<JsonObject> event) {
                      List<String> openFiles = new ArrayList<String>();
                      openFiles.add((String) event.body.getArray("paths").toArray()[0]);
                      metaData.setLastOpenFiles(openFiles);
                      requestEvent.reply(Dto.wrap(metaData));
                    }
                  });
            }
          }
        });

    vertx.eventBus()
        .registerHandler(addressBase + ".setLastOpenedFile", new Handler<Message<JsonObject>>() {
            @Override
          public void handle(Message<JsonObject> event) {
            lastOpenedFileId = event.body.getString("resourceId");
          }
        });

    vertx.eventBus()
        .registerHandler(addressBase + ".updateRunTarget", new Handler<Message<JsonObject>>() {
            @Override
          public void handle(Message<JsonObject> event) {
            RunTargetImpl runTarget = RunTargetImpl.fromJsonString(Dto.get(event));
            WorkspaceState.this.runTarget = runTarget;
          }
        });
  }
}
