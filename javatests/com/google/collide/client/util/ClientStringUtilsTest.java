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

import com.google.gwt.junit.client.GWTTestCase;

/**
 *
 */
public class ClientStringUtilsTest extends GWTTestCase {
  public static final String SEP = PathUtil.SEP;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.util.UtilTestModule";
  }

  public void testNoEllipsis() {
    // Ensure both variations do not have ellipsis
    String stringPath = "/simple/path.txt";
    PathUtil path = new PathUtil(stringPath);

    String result = ClientStringUtils.ellipsisPath(path, 2, 0, SEP);
    assertEquals(stringPath, result);

    result = ClientStringUtils.ellipsisPath(new PathUtil(stringPath.substring(1)), 2, 0, SEP);
    assertEquals(stringPath, result);
    
    stringPath = "/short.txt";
    result = ClientStringUtils.ellipsisPath(new PathUtil(stringPath), 2, 0, SEP);
    assertEquals(stringPath, result);
    
    stringPath = "/some/path/short.txt";
    result = ClientStringUtils.ellipsisPath(new PathUtil(stringPath), 5, 0, SEP);
    assertEquals(stringPath, result);
  }
  
  public void testEllipsisPaths() {
    String stringPath = "/my/simple/path.txt";
    PathUtil path = new PathUtil(stringPath);

    String result = ClientStringUtils.ellipsisPath(path, 2, 0, SEP);
    assertEquals("../simple/path.txt", result);
    
    result = ClientStringUtils.ellipsisPath(path, 1, 0, SEP);
    assertEquals("../path.txt", result);
    
    result = ClientStringUtils.ellipsisPath(path, 5, 0, SEP);
    assertEquals(stringPath, result);
  }
  
  public void testMaxCharacters() {
    String stringPath = "/some/reallylongdir/is/very/annoying.txt";
    PathUtil path = new PathUtil(stringPath);
    
    String result = ClientStringUtils.ellipsisPath(path, 2, 20, SEP);
    assertEquals("../very/annoying.txt", result);
    assertEquals(20, result.length());
    
    result = ClientStringUtils.ellipsisPath(path, 2, 4, SEP);
    assertEquals("..xt", result);
    assertEquals(4, result.length());

    result = ClientStringUtils.ellipsisPath(path, 0, 2, SEP);
    assertEquals(stringPath, result);
  }
}
