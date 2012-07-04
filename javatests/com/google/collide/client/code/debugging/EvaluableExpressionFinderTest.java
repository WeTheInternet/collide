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

package com.google.collide.client.code.debugging;

import junit.framework.TestCase;

/**
 * Tests for {@link EvaluableExpressionFinder}.
 */
public class EvaluableExpressionFinderTest extends TestCase {

  private EvaluableExpressionFinder expressionFinder;
  private String lineText;

  @Override
  protected void setUp() {
    expressionFinder = new EvaluableExpressionFinder();
    lineText = ""
        // start offset = 0
        + "var a = myArray[foo.Bar[baz]][123].qwe_; "
        // start offset = 41
        + "var b = FOO['my[]]]\\'\\\\\\' \"string()']; "
        // start offset = 80
        + "var c = this._foo_[\"asd\"][array[0][1].ssd[sdar[\"string\"]]];";
  }

  public void testNotFound() {
    doTestNotFound(-1, 3, 5, 6, 7, 39, 40, lineText.length(), lineText.length() + 1);
  }

  public void testSingleVariable() {
    doTest("myArray", range(8, 14));
    doTest("foo", range(16, 18));
    doTest("baz", range(24, 26));
    doTest("FOO", range(49, 51));
    doTest("var", range(0, 2));
    doTest("var", range(41, 43));
    doTest("var", range(80, 82));
    doTest("a", 4);  // var a = ...
    doTest("b", 45); // var b = ...
    doTest("c", 84); // var c = ...
  }

  public void testSingleProperty() {
    doTest("foo.Bar", range(19, 22));
  }

  public void testSingleArrayProperty() {
    doTest("foo.Bar[baz]", 23, 27);
  }

  public void testDoubleArrayProperty() {
    doTest("myArray[foo.Bar[baz]]", 15, 28);
  }

  public void testConsecutiveArrayProperties() {
    doTest("myArray[foo.Bar[baz]][123]", range(29, 33));
  }

  public void testMixedProperties() {
    doTest("myArray[foo.Bar[baz]][123].qwe_", range(34, 38));
  }

  public void testQuotesInsideProperties() {
    doTest("FOO['my[]]]\\'\\\\\\' \"string()']", 52, 53, 76, 77);
    doTestNotFound(range(58, 67));
    doTest("string", range(68, 73));
    doTestNotFound(74, 75);
  }

  public void testLongComplexExpression() {
    doTest("this._foo_[\"asd\"][array[0][1].ssd[sdar[\"string\"]]]", 105, 137);
    doTest("this", range(88, 91));
    doTest("this._foo_", range(92, 97));
    doTest("this._foo_[\"asd\"]", 98, 99, 103, 104);
    doTest("array", range(106, 110));
    doTest("array[0]", range(111, 113));
    doTest("array[0][1]", range(114, 116));
    doTest("array[0][1].ssd", range(117, 120));
    doTest("array[0][1].ssd[sdar[\"string\"]]", 121, 136);
    doTest("sdar", range(122, 125));
    doTest("sdar[\"string\"]", 126, 127, 134, 135);
    doTest("string", range(128, 133));
  }

  private void doTest(String expected, int... columns) {
    for (int column : columns) {
      final String assertMessage = "Testing column " + column
          + ", char=" + lineText.substring(column, column + 1);

      EvaluableExpressionFinder.Result result = expressionFinder.find(lineText, column);
      assertNotNull(assertMessage + ", expected a not NULL result", result);
      assertEquals(assertMessage, expected, result.getExpression());
    }
  }

  private void doTestNotFound(int... columns) {
    for (int column : columns) {
      EvaluableExpressionFinder.Result result = expressionFinder.find(lineText, column);
      if (result != null) {
        fail("Expected NULL result for column " + column + ", but got: " + result.getExpression());
      }
    }
  }

  private static int[] range(int start, int end) {
    int[] result = new int[end - start + 1];
    for (int i = start; i <= end; ++i) {
      result[i - start] = i;
    }
    return result;
  }
}
