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
 * Selection annotation-related utility methods that are used by both the client
 * and server.
 *
 */
public class SelectionAnnotationUtils {

  /** The annotation prefix */
  public static final String PREFIX = "selection";

  private static final String PREFIX_SLASH = PREFIX + "/";

  /**
   * @return the annotation key for the user with the given email.
   */
  public static String computeAnnotationKey(String email) {
    return PREFIX_SLASH + escapeEmail(email);
  }

  /**
   * @return true if the given annotation key is a selection annotation, false
   *         otherwise
   */
  public static boolean isSelectionAnnotation(String key) {
    return key.startsWith(PREFIX_SLASH);
  }

  /**
   * The '@' character is a reserved character in annotation keys, so we must
   * replace that with another character.
   */
  private static String escapeEmail(String email) {
    return (email == null) ? "" : email.replaceAll("@", "#");
  }
}
