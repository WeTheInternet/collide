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

/**
 * Information about a user.
 */
public interface UserDetails {

  /**
   * Utilities to retrieve additional information from {@link UserDetails}.
   */
  class Utils {
    /**
     * Returns the portrait URL with the specified size.
     * 
     * The portrait URL is returned from the server without a size:
     * /path/to/photo.jpg
     * 
     * We insert the size just before the filename. For example, for a 24 pixel portrait:
     * /path/to/s24/photo.jpg
     */
    public static String getPortraitUrl(UserDetails userDetails, int size) {
      String url = userDetails.getPortraitUrl();
      return getSizeSpecificPortraitUrl(url, size);
    }
    
    public static String getSizeSpecificPortraitUrl(String url, int size) {
      if (url == null) {
        return url;
      }
      int lastSlash = url.lastIndexOf('/');
      url = url.substring(0, lastSlash) + "/s" + size + url.substring(lastSlash);
      return url;
    }
  }

  /**
   * Returns a unique ID for the user. This ID should be used in client-to-server
   * requests that identify a specific user.
   */
  String getUserId();

  /**
   * Returns the email address of a user. The email address may be obfuscated
   * depending on the user's privacy settings, and may not be a valid email
   * address.
   */
  String getDisplayEmail();

  /**
   * Returns the display name of the user. If the display name is not available,
   * returns the email.
   */
  String getDisplayName();

  /**
   * Returns the given (first) name of the user. If the given name is not
   * available, returns the display name. If the display name is not available
   * either, returns the email.
   */
  String getGivenName();

  /**
   * Returns the portrait URL with the default size of 24 pixels.
   * 
   * Use {@link Utils#getPortraitUrl(UserDetails, int)} to get the URL of a
   * portrait in any size.
   */
  String getPortraitUrl();

  /**
   * Returns a boolean indicating that this {@link UserDetails} represents the
   * current user.
   */
  boolean isCurrentUser();
}
