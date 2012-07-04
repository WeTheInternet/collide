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

import com.google.collide.client.util.HoverController.HoverListener;
import com.google.collide.client.util.HoverController.UnhoverListener;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

import elemental.events.MouseEvent;
import elemental.html.Document;
import elemental.html.Element;

/**
 * Tests for {@link HoverController}.
 * 
 */
public class HoverControllerTest extends GWTTestCase {

  private static class MockHoverListener implements HoverListener, UnhoverListener {

    private int hoverCalled;
    private int unhoverCalled;

    public void assertHoverCount(int expected) {
      assertEquals(expected, hoverCalled);
    }

    public void assertUnhoverCount(int expected) {
      assertEquals(expected, unhoverCalled);
    }

    @Override
    public void onHover() {
      hoverCalled++;
    }

    @Override
    public void onUnhover() {
      unhoverCalled++;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.util.UtilTestModule";
  }

  /**
   * Tests a basic hover/unhover sequence.
   */
  public void testBasicHoverSequence() {
    final MockHoverListener listener = new MockHoverListener();
    final Element[] elems = createAndAttachElements(3);
    HoverController controller = new HoverController();
    controller.setHoverListener(listener);
    controller.setUnhoverListener(listener);
    controller.setUnhoverDelay(25);
    controller.addPartner(elems[0]);
    controller.addPartner(elems[1]);
    controller.addPartner(elems[2]);
    listener.assertHoverCount(0);
    listener.assertUnhoverCount(0);

    // Mouseover an element. onHover() called synchronously.
    mouseover(elems[1]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // Mouseout an element. onUnhover() is not called because we are still in
    // the unhover delay.
    mouseout(elems[2]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // Wait for the unhover delay.
    delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        listener.assertHoverCount(1);
        listener.assertUnhoverCount(1);

        // Cleanup.
        detachElements(elems);
        finishTest();
      }
    }.schedule(100);
  }

  /**
   * Tests that once we start hovering, subsequent mouseover events do not call
   * {@link MockHoverListener#onHover()}.
   */
  public void testMouseoverWhileHovering() {
    final MockHoverListener listener = new MockHoverListener();
    final Element[] elems = createAndAttachElements(3);
    HoverController controller = new HoverController();
    controller.setHoverListener(listener);
    controller.setUnhoverListener(listener);
    controller.setUnhoverDelay(25);
    controller.addPartner(elems[0]);
    controller.addPartner(elems[1]);
    controller.addPartner(elems[2]);
    listener.assertHoverCount(0);
    listener.assertUnhoverCount(0);

    // Mouseover an element. onHover() is called synchronously.
    mouseover(elems[1]);
    listener.assertHoverCount(1); 
    listener.assertUnhoverCount(0);

    // Mouseover another element.
    mouseover(elems[2]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // Mouseover the same element.
    mouseover(elems[2]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // Cleanup.
    detachElements(elems);
  }

  /**
   * Tests that if the user mouses over an element after mousing out of an
   * element, but before the unhover delay fires, then
   * {@link UnhoverListener#onUnhover()} is not called.
   */
  public void testMouseoverWithinUnhoverDelay() {
    final MockHoverListener listener = new MockHoverListener();
    final Element[] elems = createAndAttachElements(3);
    HoverController controller = new HoverController();
    controller.setHoverListener(listener);
    controller.setUnhoverListener(listener);
    controller.setUnhoverDelay(25);
    controller.addPartner(elems[0]);
    controller.addPartner(elems[1]);
    controller.addPartner(elems[2]);
    listener.assertHoverCount(0);
    listener.assertUnhoverCount(0);

    // mouseover an element. onHover() called synchronously.
    mouseover(elems[1]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // mouseout an element. onUnhover() is not called because we are still in
    // the unhover delay.
    mouseout(elems[2]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // mouseover within the delay. onUnhover() is cancelled.
    mouseover(elems[2]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // Verify unhover is never called.
    delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        listener.assertHoverCount(1);
        listener.assertUnhoverCount(0);

        // Cleanup.
        detachElements(elems);
        finishTest();
      }
    }.schedule(100);
  }

  /**
   * Tests that nothing breaks if no listeners are set.
   */
  public void testNoListeners() {
    final Element[] elems = createAndAttachElements(3);
    HoverController controller = new HoverController();
    controller.setUnhoverDelay(0);
    controller.addPartner(elems[0]);
    controller.addPartner(elems[1]);
    controller.addPartner(elems[2]);

    // mouseover an element.
    mouseover(elems[1]);

    // mouseout an element.
    mouseout(elems[2]);

    // Cleanup.
    detachElements(elems);
  }

  /**
   * Tests that setting the unhover delay to a negative value prevents
   * {@link UnhoverListener#onUnhover()} from being called.
   */
  public void testSetUnhoverDelayNevagtive() {
    final MockHoverListener listener = new MockHoverListener();
    final Element[] elems = createAndAttachElements(3);
    HoverController controller = new HoverController();
    controller.setHoverListener(listener);
    controller.setUnhoverListener(listener);
    controller.setUnhoverDelay(-1);
    controller.addPartner(elems[0]);
    controller.addPartner(elems[1]);
    controller.addPartner(elems[2]);
    listener.assertHoverCount(0);
    listener.assertUnhoverCount(0);

    // mouseover an element. onHover() called synchronously.
    mouseover(elems[1]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // mouseout an element. onUnhover() not called.
    mouseout(elems[2]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // Verify unhover is never called.
    delayTestFinish(1000);
    new Timer() {
      @Override
      public void run() {
        listener.assertHoverCount(1);
        listener.assertUnhoverCount(0);

        // Cleanup.
        detachElements(elems);
        finishTest();
      }
    }.schedule(100);
  }

  /**
   * Tests that setting the unhover delay to zero forces
   * {@link UnhoverListener#onUnhover()} to be called synchronously.
   */
  public void testSetUnhoverDelayZero() {
    final MockHoverListener listener = new MockHoverListener();
    final Element[] elems = createAndAttachElements(3);
    HoverController controller = new HoverController();
    controller.setHoverListener(listener);
    controller.setUnhoverListener(listener);
    controller.setUnhoverDelay(0);
    controller.addPartner(elems[0]);
    controller.addPartner(elems[1]);
    controller.addPartner(elems[2]);
    listener.assertHoverCount(0);
    listener.assertUnhoverCount(0);

    // mouseover an element. onHover() called synchronously.
    mouseover(elems[1]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(0);

    // mouseout an element. onUnhover() called synchronously.
    mouseout(elems[2]);
    listener.assertHoverCount(1);
    listener.assertUnhoverCount(1);

    // Cleanup.
    detachElements(elems);
  }

  private Element[] createAndAttachElements(int count) {
    Document doc = Elements.getDocument();
    Element[] elems = new Element[count];
    for (int i = 0; i < count; i++) {
      Element elem = doc.createDivElement();
      doc.getBody().appendChild(elem);
      elems[i] = elem;
    }
    return elems;
  }

  private void mouseevent(Element target, String type) {
    MouseEvent evt = (MouseEvent) Elements.getDocument().createEvent(Document.Event.MOUSE);
    evt.initMouseEvent(type, true, true, null, 0, 0, 0, 0, 0, false, false, false, false,
        MouseEvent.Button.PRIMARY, null);
    target.dispatchEvent(evt);
  }


  private void mouseout(Element target) {
    mouseevent(target, "mouseout");
  }

  private void mouseover(Element target) {
    mouseevent(target, "mouseover");
  }


  private void detachElements(Element[] elems) {
    for (Element elem : elems) {
      elem.removeFromParent();
    }
  }
}
