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

package com.google.collide.client.util;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.regexp.shared.RegExp;

/**
 * String utility methods that delegate to native Javascript.
 *
 */
public class ClientStringUtils {

  private static final RegExp regexpUppercase = RegExp.compile("[A-Z]");

  /**
   * Returns true if the given string contains any uppercase characters.
   */
  public static boolean containsUppercase(String s) {
    return regexpUppercase.test(s);
  }

  /**
   * Splits the string by the given separator string.
   *
   * <p>If an empty string ("") is used as the separator, the string is
   * split between each character.
   *
   * <p>If there is no chars between separators, or separator and the start
   * or end of line - then empty strings are added to result.
   */
  public static native JsArrayString split(String s, String separator) /*-{
    return s.split(separator);
  }-*/;

  /**
   * Takes a given path separated by PathUtil.SEP and will hack off all but the
   * specified number of paths. I.E. With a directory number of 2 it will turn
   * /my/long/tree/file.txt to .../tree/file.txt.
   *
   *  Also handles long directories such as
   * /mylongdirsisstillonlyonepath/file.txt by specifying maxChars.
   *
   * @param path String path to operate on
   * @param dirs maximum number of directory segments to leave
   * @param maxChars if > 3 specifies the maximum length of the returned string
   *        before it is truncated
   * @param sep Separator to use during split
   *
   * @return
   */
  public static String ellipsisPath(PathUtil path, int dirs, int maxChars, String sep) {
    int components = path.getPathComponentsCount();
    String pathString = path.getPathString();
    if (dirs != 0 && components > dirs) {
      pathString = ".." + PathUtil.createExcludingFirstN(path, components - dirs).getPathString();
    }
    
    if (maxChars > 2 && pathString.length() > maxChars) {
      // goto from the right maxChars - 2 for the length of the ..
      pathString = ".." + pathString.substring(pathString.length() - maxChars + 2);
    }
    return pathString;
  }
}
