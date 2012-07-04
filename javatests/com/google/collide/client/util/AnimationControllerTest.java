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

import com.google.collide.client.util.AnimationController.State;
import com.google.gwt.junit.client.GWTTestCase;

import elemental.html.Element;

/**
 * Tests for {@link AnimationController}.
 * 
 * Most of the methods in this test only execute on browsers that support
 * animations.
 */
public class AnimationControllerTest extends GWTTestCase {

  /**
   * Check if the browser under test supports animations.
   */
  private static boolean isAnimationSupported() {
    AnimationController ac = new AnimationController.Builder().setFade(true).build();
    return ac.isAnimated;
  }

  /**
   * Check if a command is scheduled to execute on the specified element.
   * 
   * @param elem
   */
  private static native boolean isCommandScheduled(Element elem) /*-{
    return elem.__gwtLastCommand != null;
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.util.UtilTestModule";
  }

  /**
   * Test that the element is hidden synchronously if there are no animations.
   */
  public void testHideNoAnimation() {
    Element elem = Elements.createDivElement();
    AnimationController ac = AnimationController.NO_ANIMATION_CONTROLLER;
    assertFalse(ac.isAnimated);

    // Start the test with the element shown.
    ac.showWithoutAnimating(elem);
    assertTrue(ac.isAnyState(elem, State.SHOWN));

    // Hide the element.
    ac.hide(elem);
    assertTrue(ac.isAnyState(elem, State.HIDDEN));
    assertFalse(isCommandScheduled(elem));
  }

  public void testHideWithoutAnimating() {
    Element elem = Elements.createDivElement();
    AnimationController ac = AnimationController.NO_ANIMATION_CONTROLLER;
    assertFalse(ac.isAnimated);

    assertFalse(ac.isAnyState(elem, State.HIDDEN));
    ac.hideWithoutAnimating(elem);
    assertTrue(ac.isAnyState(elem, State.HIDDEN));
    assertFalse(isCommandScheduled(elem));
  }


  /**
   * Test that the element is shown synchronously if there are no animations.
   */
  public void testShowNoAnimation() {
    Element elem = Elements.createDivElement();
    AnimationController ac = AnimationController.NO_ANIMATION_CONTROLLER;
    assertFalse(ac.isAnimated);

    assertFalse(ac.isAnyState(elem, State.SHOWN));
    ac.show(elem);
    assertTrue(ac.isAnyState(elem, State.SHOWN));
    assertFalse(isCommandScheduled(elem));
  }

  public void testShowWithoutAnimating() {
    Element elem = Elements.createDivElement();
    AnimationController ac = AnimationController.NO_ANIMATION_CONTROLLER;
    assertFalse(ac.isAnimated);

    assertFalse(ac.isAnyState(elem, State.SHOWN));
    ac.showWithoutAnimating(elem);
    assertTrue(ac.isAnyState(elem, State.SHOWN));
    assertFalse(isCommandScheduled(elem));
  }
}
