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

package com.google.collide.dto.shared;

/**
 * Shared constants for keys used in JSON serialized data between client and
 * server.
 * 
 */
public class JsonFieldConstants {
  /**
   * The active client ID for the bootstrap session. This is unique per tab.
   */
  public static final String SESSION_ACTIVE_ID = "activeClient";

  /**
   * The handle for the logged in user (email or name).
   */
  public static final String SESSION_USERNAME = "name";

  /**
   * Unique user ID of the logged in user. This user may have multiple active tabs.
   */
  public static final String SESSION_USER_ID = "sessionID";

  /**
   * The XSRF token used to validate senders of HTTP requests to the Frontend.
   */
  public static final String XSRF_TOKEN = "xsrfToken";

  /**
   * The domain that we must talk to in order to fetch static file content.
   */
  public static final String STATIC_FILE_CONTENT_DOMAIN = "staticContentDomain";
  
  /**
   * The URL for the user's profile image.
   */
  public static final String PROFILE_IMAGE_URL = "profileImageUrl";
}
