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

import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;

/**
 * Simple map of file extensions to supported MIME-TYPEs.
 *
 */
public class MimeTypes {
  /** MIME type to force a download, rather than display-in-browser */
  public static final String BINARY_MIMETYPE = "application/octet-stream";

  /** MIME type for json payloads */
  public static final String JSON = "application/json";

  /** MIMEtype for zip archives */
  public static final String ZIP_MIMETYPE = "application/zip";

  private static final JsonStringMap<String> mimeTypes = JsonCollections.createMap();
  private static final JsonStringMap<Boolean> imageMimeTypes = JsonCollections.createMap();

  static {
    mimeTypes.put("gif", "image/gif");
    mimeTypes.put("jpg", "image/jpeg");
    mimeTypes.put("jpeg", "image/jpeg");
    mimeTypes.put("jpe", "image/jpeg");
    mimeTypes.put("png", "image/png");
    mimeTypes.put("ico", "image/x-icon");
    mimeTypes.put("xml", "text/xml");
    mimeTypes.put("html", "text/html");
    mimeTypes.put("css", "text/css");
    mimeTypes.put("txt", "text/plain");
    mimeTypes.put("js", "application/javascript");
    mimeTypes.put("json", JSON);
    mimeTypes.put("svg", "image/svg+xml");
    mimeTypes.put("zip", ZIP_MIMETYPE);

    imageMimeTypes.put("image/gif", true);
    imageMimeTypes.put("image/jpeg", true);
    imageMimeTypes.put("image/png", true);
  }

  /**
   * Returns the appropriate MIME-TYPE string for a given file extension. If the
   * MIME-TYPE cannot be found, null is returned. It is the responsibility of
   * callers to provide a suitable default.
   */
  public static String getMimeType(String extension) {
    return mimeTypes.get(extension.toLowerCase());
  }

  /**
   * Takes in a mimetype and returns whether or not it smells like an image.
   */
  public static boolean looksLikeImage(String mimeType) {
    return imageMimeTypes.containsKey(mimeType);
  }

  /**
   * Inspects the file extension and guesses an appropriate mime-type for
   * serving the file.
   *
   * In the absence of a file extension, we simply assume it is text.
   */
  public static String guessMimeType(String path, boolean assumeUtf8) {
    String lastPathComponent = path.substring(path.lastIndexOf('/') + 1, path.length());
    int extensionIndex = lastPathComponent.lastIndexOf('.');

    String extension = "";
    if (extensionIndex >= 0) {
      extension = lastPathComponent.substring(extensionIndex + 1, lastPathComponent.length());
    }

    String mimeType = MimeTypes.getMimeType(extension);
    if (mimeType == null) {
      if (assumeUtf8) {
        mimeType = MimeTypes.getMimeType("txt");
      } else {
        mimeType = MimeTypes.BINARY_MIMETYPE;
      }
    }
    return mimeType;
  }

  private MimeTypes() {
  }
}
