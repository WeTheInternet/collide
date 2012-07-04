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

import com.google.gwt.regexp.shared.RegExp;

import junit.framework.TestCase;

/**
 *
 */
public class RegexpUtilTests extends TestCase {

  /* Tests for createWildcardRegex */
  public void testWildcardRegex() {
    RegExp re = RegExpUtils.createRegExpForWildcardPattern("t?st", "g");
    assertEquals("t\\Sst", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("t*t", "g");
    assertEquals("t\\S+t", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("t*?*", "g");
    assertEquals("t\\S+\\S\\S+", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("t*{?}*", "g");
    assertEquals("t\\S+\\{\\S\\}\\S+", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("*", "g");
    assertEquals("\\S+", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("?", "g");
    assertEquals("\\S", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("\\alex", "g");
    assertEquals("\\\\alex", re.getSource());

    // lets test all the escape characters (minus ? and *, i.e. .+()[]{}\)
    re = RegExpUtils.createRegExpForWildcardPattern(".+$^|()[]{}\\", "g");
    assertEquals("\\.\\+\\$\\^\\|\\(\\)\\[\\]\\{\\}\\\\", re.getSource());
  }

  public void testEscapedWildcardRegex() {
    RegExp re = RegExpUtils.createRegExpForWildcardPattern("\\*", "g");
    assertEquals("\\*", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("\\?", "g");
    assertEquals("\\?", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("\\?\\?\\*\\*\\?", "g");
    assertEquals("\\?\\?\\*\\*\\?", re.getSource());
    
    re = RegExpUtils.createRegExpForWildcardPattern("j\\?uni? t*est\\*", "g");
    assertEquals("j\\?uni\\S t\\S+est\\*", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("...\\?", "g");
    assertEquals("\\.\\.\\.\\?", re.getSource());
    
    re = RegExpUtils.createRegExpForWildcardPattern("\\\\*", "g");
    assertEquals("\\\\\\S+", re.getSource());
    
    re = RegExpUtils.createRegExpForWildcardPattern("\\\\?", "g");
    assertEquals("\\\\\\S", re.getSource());

    re = RegExpUtils.createRegExpForWildcardPattern("\\\\\\*", "g");
    assertEquals("\\\\\\*", re.getSource());
  }
  
  public void testGetNumberMatchesIsCorrect() {
    RegExp re = RegExp.compile("a","g");
    assertEquals(2, RegExpUtils.getNumberOfMatches(re, "aall"));
    
    re = RegExp.compile("\\S+", "g");
    assertEquals(1, RegExpUtils.getNumberOfMatches(re,"hahahahahaha"));
    
    re = RegExp.compile("ha","g");
    assertEquals(6, RegExpUtils.getNumberOfMatches(re,"hahahahahaha"));
    
    re = RegExp.compile("haha");
    assertEquals(1, RegExpUtils.getNumberOfMatches(re, "hahahahaha"));
    assertEquals(0, RegExpUtils.getNumberOfMatches(re, "asdffdsa"));
    
    assertEquals(0, RegExpUtils.getNumberOfMatches(null, "hasdf"));
  }
  
  /**
   * This test is kind of a shotgun that just tests a bunch of regex to see if
   * they come out right
   */
  public void testRegexEscape() {
    assertEquals("a\\.\\.x\\{\\}lu\\[s\\]co", RegExpUtils.escape("a..x{}lu[s]co"));
    assertEquals("alex", RegExpUtils.escape("alex"));
    assertEquals("\\*af\\?\\|as\\(df\\|\\)", RegExpUtils.escape("*af?|as(df|)"));
    assertEquals("\\*af\\?\\|as\\(df\\|\\)", RegExpUtils.escape("*af?|as(df|)"));
    assertEquals("j\\$oh\\^n", RegExpUtils.escape("j$oh^n"));
  }
}
