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

package com.google.collide.shared.util;

import junit.framework.TestCase;

/**
 * Test cases for {@link StringUtils}.
 */
public class StringUtilsTest extends TestCase {

  public void testStartsWith() {
    // Case (mis)match.
    assertTrue(StringUtils.startsWith("bar", "Barbeque", true));
    assertTrue(StringUtils.startsWith("bar", "barbeque", true));
    assertFalse(StringUtils.startsWith("bar", "Barbeque", false));
    assertTrue(StringUtils.startsWith("bar", "barbeque", false));

    // Prefix mismatch.
    assertFalse(StringUtils.startsWith("bad", "barbeque", true));
    assertFalse(StringUtils.startsWith("bad", "barbeque", false));

    // Empty prefix.
    assertTrue(StringUtils.startsWith("", "barbeque", true));
    assertTrue(StringUtils.startsWith("", "barbeque", false));

    // Empty content.
    assertFalse(StringUtils.startsWith("bar", "", true));
    assertFalse(StringUtils.startsWith("bar", "", false));

    // Short content.
    assertFalse(StringUtils.startsWith("bar", "b", true));
    assertFalse(StringUtils.startsWith("bar", "b", false));

    // Empty prefix, empty content.
    assertTrue(StringUtils.startsWith("", "", true));
    assertTrue(StringUtils.startsWith("", "", false));
  }

  public void testSplit() {
    assertEquals("a|b|c", StringUtils.split("abc", "").join("|"));
    assertEquals("a|b|c", StringUtils.split("a b c", " ").join("|"));
    assertEquals("a|b|c", StringUtils.split("a, b, c", ", ").join("|"));
    assertEquals("a, b, c", StringUtils.split("a, b, c", " ,").join("|"));
    assertEquals("|a||b|", StringUtils.split(".a..b.", ".").join("|"));
  }
}
