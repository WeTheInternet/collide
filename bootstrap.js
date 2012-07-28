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

load("vertx.js")

var eb = vertx.eventBus;
var bootstrapConfig = vertx.config;

if (!bootstrapConfig || !bootstrapConfig.webRoot || !bootstrapConfig.staticFiles) {
  console.error("Collide does not know which directory to serve :(!");
}

var webFeConfig = {
  port: 8080,
  host: "0.0.0.0",
  bridge: true,
  webRoot: bootstrapConfig.webRoot,
  staticFiles: bootstrapConfig.staticFiles,

  // This defines which messages from the client we will let through, as well as what
  // messages we want to let through to the client from the server.

  // TODO: Fill this in. Currently allowing EVERYTHING "{}" through.
  // TODO: We should auto-generate this.
  in_permitted: [
    {}
  ],
  out_permitted: [
    {}
  ]
};

var participantListConfig = {
  // TODO: Pick this up off the command line when launching collide.
  // password="s3cret"
}

var workspaceConfig = {
  webRoot: bootstrapConfig.webRoot
}

// Start the FE server. Starting several instances to handle concurrent HTTP requests.
vertx.deployVerticle("com.google.collide.server.fe.WebFE", webFeConfig, 10, function() {
  // Server was started.
});

// Load the participant list verticle that manages auth and client identity.
vertx.deployVerticle("com.google.collide.server.participants.Participants", participantListConfig, 1, function() {
  // Server was started.
});

// Load the collaborative document sessions that manages OT and file content flushes to disk.
vertx.deployVerticle("com.google.collide.server.documents.EditSessions", null, 1, function() {
  // Server was started.
});

// Load the collaborative document sessions that manages OT and file content flushes to disk.
vertx.deployVerticle("com.google.collide.server.filetree.FileTree", null, 1, function() {
  // Server was started.
});

// Load the collaborative document sessions that manages OT and file content flushes to disk.
vertx.deployVerticle("com.google.collide.server.workspace.WorkspaceState", workspaceConfig, 1, function() {
  // Server was started.
});

// Load the maven controller that handles running maven tasks on behalf of a user.
vertx.deployVerticle("com.google.collide.server.maven.MavenController", workspaceConfig, 1, function() {
	// Server was started.
});
