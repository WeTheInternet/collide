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
 * Unit Tests for TextUtility functions.
 */
public class TextUtilsTest extends TestCase {
  
  public void testFindNextCharacterInclusive() {
    String text = "à\n";
    assertEquals(0, TextUtils.findNextCharacterInclusive(text, 0));
    assertEquals(2, TextUtils.findNextCharacterInclusive(text, 1));
    assertEquals(2, TextUtils.findNextCharacterInclusive(text, 2));
    
    assertEquals(1, TextUtils.findNextCharacterInclusive(text.substring(0, 1), 1));
  }

  public void testBasicTextFindCharacter() {
    String text = "alex";
    assertEquals(1, TextUtils.findNonMarkNorOtherCharacter(text, 0));
    assertEquals(2, TextUtils.findNonMarkNorOtherCharacter(text, 1));
    assertEquals(3, TextUtils.findNonMarkNorOtherCharacter(text, 2));
    assertEquals(5, TextUtils.findNonMarkNorOtherCharacter(text, 3));

    assertEquals(3, TextUtils.findPreviousNonMarkNorOtherCharacter(text, 4));
    assertEquals(2, TextUtils.findPreviousNonMarkNorOtherCharacter(text, 3));
    assertEquals(1, TextUtils.findPreviousNonMarkNorOtherCharacter(text, 2));
    assertEquals(0, TextUtils.findPreviousNonMarkNorOtherCharacter(text, 1));
    assertEquals(-1, TextUtils.findPreviousNonMarkNorOtherCharacter(text, 0));
  }
  
  public void testCombiningMarkFindCharacter() {
    // Note: First a is a a + ` combining mark
    String text = "\tà=à\r\n";
    assertEquals(1, TextUtils.findNonMarkNorOtherCharacter(text, 0));
    // skip over combining mark
    assertEquals(3, TextUtils.findNonMarkNorOtherCharacter(text, 1));
    // skip over \r
    assertEquals(6, TextUtils.findNonMarkNorOtherCharacter(text, 4));
    assertEquals(8, TextUtils.findNonMarkNorOtherCharacter(text, 6));

    // skip over \r
    assertEquals(4, TextUtils.findPreviousNonMarkNorOtherCharacter(text, 6));
    // go from = to before a
    assertEquals(1, TextUtils.findPreviousNonMarkNorOtherCharacter(text, 3));
  }

  public void testControlCharactersFindCharacter() {
    String text = "\t\t\t\r\n";
    assertEquals(1, TextUtils.findNonMarkNorOtherCharacter(text, 0));
    assertEquals(2, TextUtils.findNonMarkNorOtherCharacter(text, 1));
    // skip over \r
    assertEquals(4, TextUtils.findNonMarkNorOtherCharacter(text, 2));
    assertEquals(6, TextUtils.findNonMarkNorOtherCharacter(text, 4));
  }

  public void testBasicTextFindNextWord() {
    String text = "alex lusco was here";
    assertEquals(5, TextUtils.findNextWord(text, 0, true));
    assertEquals(11, TextUtils.findNextWord(text, 5, true));
    assertEquals(15, TextUtils.findNextWord(text, 11, true));
    assertEquals(19, TextUtils.findNextWord(text, 15, true));
    assertEquals(20, TextUtils.findNextWord(text, 19, true));

    assertEquals(4, TextUtils.findNextWord(text, 0, false));
    assertEquals(10, TextUtils.findNextWord(text, 4, false));
    assertEquals(14, TextUtils.findNextWord(text, 10, false));
    assertEquals(19, TextUtils.findNextWord(text, 14, false));
    assertEquals(20, TextUtils.findNextWord(text, 19, false));
  }
  
  public void testBasicTextFindPreviousWord() {
    String text = "alex lusco was here";
    assertEquals(14, TextUtils.findPreviousWord(text, 19, true));
    assertEquals(10, TextUtils.findPreviousWord(text, 14, true));
    assertEquals(4, TextUtils.findPreviousWord(text, 10, true));
    assertEquals(0, TextUtils.findPreviousWord(text, 4, true));
    assertEquals(-1, TextUtils.findPreviousWord(text, 0, true));

    assertEquals(15, TextUtils.findPreviousWord(text, 19, false));
    assertEquals(11, TextUtils.findPreviousWord(text, 15, false));
    assertEquals(5, TextUtils.findPreviousWord(text, 11, false));
    assertEquals(0, TextUtils.findPreviousWord(text, 5, false));
    assertEquals(-1, TextUtils.findPreviousWord(text, 0, false));
  }
  
  public void testStopAtNewLineWhenFindNextWord() {
    String text = "alex\nalex\n";
    assertEquals(5, TextUtils.findNextWord(text, 0, true));
    assertEquals(4, TextUtils.findNextWord(text, 0, false));
    assertEquals(10, TextUtils.findNextWord(text, 5, true));
    assertEquals(9, TextUtils.findNextWord(text, 4, false));
  }
  
  public void testIdentifierFindWord() {
    String text = "$(test.alex, 3)";
    assertEquals(1, TextUtils.findNextWord(text, 0, true));
    assertEquals(2, TextUtils.findNextWord(text, 1, true));
    assertEquals(6, TextUtils.findNextWord(text, 2, true));
    assertEquals(7, TextUtils.findNextWord(text, 6, true));
    assertEquals(11, TextUtils.findNextWord(text, 7, true));
    assertEquals(13, TextUtils.findNextWord(text, 11, true));
    assertEquals(14, TextUtils.findNextWord(text, 13, true));
    assertEquals(16, TextUtils.findNextWord(text, 14, true));
    assertEquals(11, TextUtils.findNextWord(text, 7, false));
    assertEquals(12, TextUtils.findNextWord(text, 11, false));
    
    assertEquals(13, TextUtils.findPreviousWord(text, 14, false));
    assertEquals(11, TextUtils.findPreviousWord(text, 13, false));
    assertEquals(7, TextUtils.findPreviousWord(text, 11, false));
    assertEquals(6, TextUtils.findPreviousWord(text, 7, false));
    assertEquals(2, TextUtils.findPreviousWord(text, 6, false));
    assertEquals(1, TextUtils.findPreviousWord(text, 2, false));
    assertEquals(0, TextUtils.findPreviousWord(text, 1, false));
    assertEquals(11, TextUtils.findPreviousWord(text, 13, true));
    assertEquals(7, TextUtils.findPreviousWord(text, 11, true));
  }
  
  public void testCombiningMarkFindWord() {
    String text = "\tà=à\r";
    assertEquals(1, TextUtils.findNextWord(text, 0, true));
    assertEquals(3, TextUtils.findNextWord(text, 1, true));
    assertEquals(4, TextUtils.findNextWord(text, 3, true));
    assertEquals(6, TextUtils.findNextWord(text, 4, true));

    assertEquals(5, TextUtils.findPreviousWord(text, 6, true));
    assertEquals(4, TextUtils.findPreviousWord(text, 5, true));
    assertEquals(0, TextUtils.findPreviousWord(text, 3, true));
    assertEquals(1, TextUtils.findPreviousWord(text, 3, false));
  }
  
  public void testCountWhitespaceAtBegginingOfLine() {
    assertEquals(4, TextUtils.countWhitespacesAtTheBeginningOfLine("\t\t  A"));
    assertEquals(4, TextUtils.countWhitespacesAtTheBeginningOfLine("\t\t  "));
    assertEquals(4, TextUtils.countWhitespacesAtTheBeginningOfLine("\t\t  \n"));
  }
}
