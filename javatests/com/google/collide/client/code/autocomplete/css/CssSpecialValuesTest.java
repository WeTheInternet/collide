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
import com.google.gwt.core.client.JsArrayString;

public class CssSpecialValuesTest extends SynchronousTestCase {

  CssPartialParser cssPartialParser;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    cssPartialParser = CssPartialParser.getInstance();
  }

  public void testAngle() {
    JsArrayString proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("19deg", "<angle>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("222rad", "<angle>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("22grad", "<angle>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("-22grad", "<angle>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("22.2grad", "<angle>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("-22.2grad", "<angle>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("22foo", "<angle>");
    assertEquals(0, proposals.length());

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("-22foo", "<angle>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("", "<angle>");
    assertEquals(0, proposals.length());
  }

  public void testColor() {
    JsArrayString proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("olive", "<color>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("white", "<color>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("#fa0", "<color>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("#f9a711", "<color>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("rgb(10,10,10)", "<color>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "rgb(10,  10,    10)", "<color>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "rgb(10%,10%,10%)", "<color>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "rgb(10%,  10%,    10%)", "<color>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("blueishgreen", "<color>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("rgb()", "<color>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("#f", "<color>");
    assertEquals(0, proposals.length());
  }

  public void testCounter() {
    JsArrayString proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "counter(par-num, upper-roman)", "<counter>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "counter(par-num)", "<counter>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "c(par-num, upper-roman)", "<counter>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("foo", "<counter>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("", "<counter>");
    assertEquals(0, proposals.length());
  }

  public void testFrequency() {
    JsArrayString proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("100kHz", "<frequency>");
    assertTrue(proposals.length() > 0);

    // Note: this seems wrong, but is permissible as per the CSS2 spec.
    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("-10Hz", "<frequency>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("100", "<frequency>");
    assertEquals(0, proposals.length());

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("foo", "<frequency>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("", "<frequency>");
    assertEquals(0, proposals.length());
  }

  public void testInteger() {
    JsArrayString proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("19", "<integer>");
    assertTrue(proposals.length() > 0);

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("-2200", "<integer>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("+22", "<integer>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("vvv", "<integer>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("22.", "<integer>");
    assertEquals(0, proposals.length());

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("22.2grad", "<integer>");
    assertEquals(0, proposals.length());
  }

  public void testNumber() {
    JsArrayString proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("19", "<number>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("22.2", "<number>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("-22", "<number>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("+22", "<number>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("vvv", "<number>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("22.", "<number>");
    assertEquals(0, proposals.length());

    proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("22.2grad", "<number>");
    assertEquals(0, proposals.length());
  }

  public void testPercentage() {
    JsArrayString proposals =
        cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("10em", "<length>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("0mm", "<length>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("10px", "<length>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("foo", "<length>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("", "<length>");
    assertEquals(0, proposals.length());
  }

  public void testShape() {
    JsArrayString proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "rect(10em, 10em, 10em, 10em)", "<shape>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "rect(10px,     10px  ,    10px,   10px)", "<shape>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "r(10em, 10em, 10em, 10em)", "<shape>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("foo", "<shape>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("", "<shape>");
    assertEquals(0, proposals.length());
  }


  public void testUri() {
    JsArrayString proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "https://mail.google.com", "<uri>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals(
        "http://www.google.com", "<uri>");
    assertTrue(proposals.length() > 0);

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("wwww", "<uri>");
    assertEquals(0, proposals.length());

    proposals = cssPartialParser.checkIfSpecialValueAndGetSpecialValueProposals("", "<uri>");
    assertEquals(0, proposals.length());
  }
}
