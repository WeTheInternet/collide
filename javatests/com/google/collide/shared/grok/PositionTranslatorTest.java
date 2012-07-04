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
package com.google.collide.shared.grok;

import com.google.collide.shared.document.LineNumberAndColumn;

import junit.framework.TestCase;

/**
 * Unit test for PositionTranslator.
 *
 */
public class PositionTranslatorTest extends TestCase {

  public void testOffsetToLineNumber() {
    String src =
        "0123\n"
      + "56\n"
      + "\n"
      + "9";
    PositionTranslator positionTranslator = new PositionTranslator(src);
    LineNumberAndColumn result = positionTranslator.getLineNumberAndColumn(0);
    assertEquals(0, result.lineNumber);
    assertEquals(0, result.column);

    result = positionTranslator.getLineNumberAndColumn(4);
    assertEquals(0, result.lineNumber);
    assertEquals(4, result.column);

    result = positionTranslator.getLineNumberAndColumn(5);
    assertEquals(1, result.lineNumber);
    assertEquals(0, result.column);

    result = positionTranslator.getLineNumberAndColumn(7);
    assertEquals(1, result.lineNumber);
    assertEquals(2, result.column);

    result = positionTranslator.getLineNumberAndColumn(8);
    assertEquals(2, result.lineNumber);
    assertEquals(0, result.column);

    result = positionTranslator.getLineNumberAndColumn(9);
    assertEquals(3, result.lineNumber);
    assertEquals(0, result.column);
  }
}
