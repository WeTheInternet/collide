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

for (var i in vertx.config)
	console.log(i);

var webFeConfig = {
  port: 13337,
  host: "0.0.0.0",
//  suffix: "/collide",
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
  // password:"s3cret"
  usernames:"James"
}

var workspaceConfig = {
  webRoot: bootstrapConfig.webRoot
  ,plugins: ['gwt', 'ant']
}

var pluginConfig = {
  plugins: ['gwt', 'ant']
  ,'preserve-cwd': true
  ,includes: ['gwt', 'ant']
  ,webRoot: bootstrapConfig.webRoot
  ,staticFiles: bootstrapConfig.staticFiles
}

var filetreeConfig = {
  webRoot: bootstrapConfig.webRoot
  ,packages: ['java', 'demo/src/main/java','plugin','bin/gen', 'javatests', 'bin/test/gen']
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
vertx.deployVerticle("com.google.collide.server.filetree.FileTree", filetreeConfig, 1, function() {
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


//pluginConfig.webRoot['preserve-cwd'] = true;
// Load the plugin controller which allows adding services to collide, such as gwt super dev mode, ant builds, terminal, maven, etc..
vertx.deployWorkerVerticle("com.google.collide.plugin.server.gwt.GwtServerPlugin", pluginConfig, 1, function() {
});
vertx.deployWorkerVerticle("com.google.collide.plugin.server.ant.AntServerPlugin", pluginConfig, 1, function() {
});
