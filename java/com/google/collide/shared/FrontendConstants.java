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

package com.google.collide.shared;

/**
 * Yes. Another shared constants file across client and server.
 */
public final class FrontendConstants {
  // TODO - might need to move these if the FE doesn't actually look at them
  /** Constants relating to file upload */
  public static final class UploadConstants {
    /** The globally unique ID for this upload session */
    public static final String SESSION_ID = "sessionId";
    /** The ID of the workspace these files are uploading to */
    public static final String WORKSPACE_ID = "workspaceId";
    /** The workspace path for the file */
    public static final String WORKSPACE_PATH = "workspacePath";
    /** The number of file failures in the metadata */
    public static final String FILE_FAILURE_SIZE = "fileFailureCount";
    /** The prefix for each file path in the metadata */
    public static final String FILE_FAILURE_PREFIX = "fileFailure";
    /** The failure message for the upload */
    public static final String FAILURE_MESSAGE = "failureMessage";
    /** The string inside a path that will cause a simulated network failure */
    public static final String SIMULATED_NETWORK_FAILURE_PATH_SUBSTRING = "simulateNetworkFailure";
    /** The string inside a path that will cause a simulated processing failure */
    public static final String SIMULATED_PROCESSING_FAILURE_PATH_SUBSTRING =
        "simulateProcessingFailure";
  }
  
  /**
   * The name of the header for attaching the bootstrap client id to each
   * request.
   */
  public static final String CLIENT_BOOTSTRAP_ID_HEADER = "X-Bootstrap-ClientId";

  /**
   * Path for provisioning a browser channel.
   */
  public static final String BROWSER_CHANNEL_PATH = "/channel/comm";

  /**
   * Test Path required for provisioning a browser channel.
   */
  public static final String BROWSER_CHANNEL_TEST_PATH = "/channel/test";

  /**
   * Query parameter sent with browser channel requests by the client to
   * identify itself to the frontend.
   */
  public static final String CLIENTID_PARAM_NAME = "clientId";

  /**
   * Query parameter used to name a resource in a given workspace.
   */
  public static final String FILE_PARAM_NAME = "file";

  /**
   * Maximum allowed length of project names.
   */
  public static final int MAX_PROJECT_NAME_LEN = 128;

  /**
   * Maximum allowed size for uploaded files.
   */
  public static final long MAX_UPLOAD_FILE_SIZE = 4L * 1024 * 1024;  // 4MB

  /**
   * The name of the header for attaching an XSRF token to each request.
   */
  public static final String XSRF_TOKEN_HEADER = "X-Xsrf-Token";

  /**
   * The name of the header that we keep the Client->Frontend protocol version hash.
   */
  public static final String CLIENT_VERSION_HASH_HEADER = "X-Version-Hash";

  /**
   * The default depth to load the file tree and sub-directories. A depth of -1 indicates infinite
   * depth.
   */
  public static final int DEFAULT_FILE_TREE_DEPTH = -1;

  private FrontendConstants() {
  }
}
