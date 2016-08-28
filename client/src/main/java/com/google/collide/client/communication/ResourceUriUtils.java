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

package com.google.collide.client.communication;

import com.google.collide.client.util.PathUtil;

import elemental.client.Browser;
import elemental.html.Location;

/**
 * Utility class to work with resource URIs and local paths.
 *
 */
public class ResourceUriUtils {
  
  /**
   * @see #getAbsoluteResourceUri(String)
   */
  public static String getAbsoluteResourceUri(PathUtil path) {
    return getAbsoluteResourceUri(path.getPathString());
  }

  /**
   * Calculates an absolute URI of a resource by a given path.
   *
   * @param path relative path from the workspace root
   * @return an absolute URI
   */
  public static String getAbsoluteResourceUri(String path) {
    return getAbsoluteResourceBaseUri() + ensurePrefixSlash(path);
  }

  public static String getAbsoluteResourceBaseUri() {
    return getBaseUri() + "/res";
  }

  private static String getBaseUri() {
    Location location = Browser.getWindow().getLocation();
    return location.getProtocol() + "//" + location.getHost();
  }
  
  public static String extractBaseUri(String absoluteUri) {
    int pos = absoluteUri.indexOf("://");
    if (pos != -1) {
      pos += 3;
    } else {
      pos = 0;
    }

    pos = absoluteUri.indexOf("/", pos);
    if (pos != -1) {
      return absoluteUri.substring(0, pos);
    } else {
      return absoluteUri;
    }
  }

  private static String ensurePrefixSlash(String path) {
    if (path.startsWith("/")) {
      return path;
    }
    return "/" + path;
  }
}
