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

package com.google.collide.client.bootstrap;

import com.google.collide.dto.shared.JsonFieldConstants;
import com.google.collide.json.client.Jso;

/**
 * Bootstrap information for the client.
 *
 * <p>This gets injected to the page with the initial page request.
 *
 * <p>It contains a description of the files contained in the project workspace,
 * as well as user identification information and a sessionID token that is sent
 * along with subsequent requests.
 *
 */
public final class BootstrapSession extends Jso {
  /**
   * Use this method to obtain an instance of the Session object.
   */
  public static native BootstrapSession getBootstrapSession() /*-{
    return $wnd['__session'] || {};
  }-*/;

  protected BootstrapSession() {
  }

  /**
   * @return The active client ID for the current tab.
   */
  public String getActiveClientId() {
    return getStringField(JsonFieldConstants.SESSION_ACTIVE_ID);
  }

  /**
   * @return The user's handle. This is his name or email.
   */
  public String getUsername() {
    return getStringField(JsonFieldConstants.SESSION_USERNAME);
  }

  /**
   * @return The user's unique ID (obfuscated GAIA ID).
   */
  public String getUserId() {
    return getStringField(JsonFieldConstants.SESSION_USER_ID);
  }

  /**
   * @return The user's XSRF token that it sends with each request to validate
   *         that it originated from the client.
   */
  public String getXsrfToken() {
    return getStringField(JsonFieldConstants.XSRF_TOKEN);
  }

  /**
   * @return The domain that we must talk to in order to fetch static
   *         file content from user branches.
   */
  public String getStaticContentServingDomain() {
    return getStringField(JsonFieldConstants.STATIC_FILE_CONTENT_DOMAIN);
  }

  /**
   * Updates the client's XSRF token.
   * 
   * @param newXsrfToken
   */
  public void setXsrfToken(String newXsrfToken) {
    this.addField(JsonFieldConstants.XSRF_TOKEN, newXsrfToken);
  }
  
  /**
   * @return The url for the user's profile image.
   */
  public String getProfileImageUrl() {
    return getStringField(JsonFieldConstants.PROFILE_IMAGE_URL);
  }
}
