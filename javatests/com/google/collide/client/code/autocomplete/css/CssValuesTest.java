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

package com.google.collide.client.code.autocomplete.css;

import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.junit.Before;

/**
 *
 */

public class CssValuesTest extends SynchronousTestCase {

  CssPartialParser cssPartialParser;

  @Override
  public String getModuleName() {
    return "com.google.cofllide.client.TestCode";
  }

  @Override
  @Before
  public void gwtSetUp() {
    cssPartialParser = CssPartialParser.getInstance();
  }

  public void testAzimuth() {
    JsArray<JavaScriptObject> valuesForAllSlots = cssPartialParser.getPropertyValues("azimuth");
    assertEquals(2, valuesForAllSlots.length());
  }

  public void testBackgroundAttachment() {
    JsArray<JavaScriptObject> valuesForAllSlots =
        cssPartialParser.getPropertyValues("background-attachment");
    assertEquals(1, valuesForAllSlots.length());
  }

  public void testBorderLeft() {
    JsArray<JavaScriptObject> valuesForAllSlots = cssPartialParser.getPropertyValues("border-left");
    assertEquals(3, valuesForAllSlots.length());
  }

  public void testBorderTopStyle() {
    JsArray<JavaScriptObject> valuesForAllSlots =
        cssPartialParser.getPropertyValues("border-top-style");
    assertEquals(1, valuesForAllSlots.length());
  }

  public void testBorderWidth() {
    JsArray<JavaScriptObject> valuesForAllSlots =
        cssPartialParser.getPropertyValues("border-width");
    assertEquals(2, valuesForAllSlots.length());
  }

  public void testBottom() {
    JsArray<JavaScriptObject> valuesForAllSlots = cssPartialParser.getPropertyValues("bottom");
    assertEquals(1, valuesForAllSlots.length());
  }

  public void testColor() {
    JsArray<JavaScriptObject> valuesForAllSlots = cssPartialParser.getPropertyValues("color");
    assertEquals(1, valuesForAllSlots.length());
  }

  public void testWidth() {
    JsArray<JavaScriptObject> valuesForAllSlots = cssPartialParser.getPropertyValues("width");
    assertEquals(1, valuesForAllSlots.length());
  }

  public void testZIndex() {
    JsArray<JavaScriptObject> valuesForAllSlots = cssPartialParser.getPropertyValues("z-index");
    assertEquals(1, valuesForAllSlots.length());
  }
}
