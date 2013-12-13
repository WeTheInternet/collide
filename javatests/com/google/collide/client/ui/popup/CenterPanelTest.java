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

package com.google.collide.client.ui.popup;

import collide.client.util.Elements;

import com.google.collide.client.ui.popup.CenterPanel.Resources;
import com.google.collide.client.ui.popup.CenterPanel.View;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

import elemental.dom.Element;

/**
 * Tests for {@link CenterPanel}.
 * 
 */
public class CenterPanelTest extends GWTTestCase {

  /**
   * The timeout allowed for deferred commands.
   */
  private static final int DEFERRED_COMMAND_TIMEOUT = 2000;

  @Override
  public String getModuleName() {
    return PopupTestUtils.BUILD_MODULE_NAME;
  }

  private Element content;
  private CenterPanel panel;
  private Resources resources;
  private String styleGlass;
  private String styleGlassVisible;
  private String styleContent;
  private String styleContentVisible;
  private View view;

  public void testCreate() {
    // The content should be added to the content container.
    assertEquals(view.contentContainer, content.getParentElement());

    // The popup is not showing or attached.
    assertFalse(panel.isShowing());
    assertNull(view.popup.getParentElement());
  }

  public void testHide() {
    // Show the popup.
    panel.show();
    delayTestFinish(DEFERRED_COMMAND_TIMEOUT);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        assertTrue(panel.isShowing());

        // Hide the panel. Styles are removed in this event loop, but the popup
        // isn't detached until the animation ends.
        panel.hide();
        assertHiding();
      }
    });
  }

  public void testHideThenShow() {
    // Show the popup.
    panel.show();
    delayTestFinish(DEFERRED_COMMAND_TIMEOUT);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        assertTrue(panel.isShowing());

        panel.hide();
        panel.show();
        assertShowing();
      }
    });
  }

  public void testHideTwice() {
    // Show the popup.
    panel.show();
    delayTestFinish(DEFERRED_COMMAND_TIMEOUT);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        assertTrue(panel.isShowing());

        // Hide the panel. Styles are removed in this event loop, but the popup
        // isn't detached until the animation ends.
        panel.hide();
        panel.hide(); // No-op.
        assertHiding();
      }
    });
  }

  public void testShow() {
    // Initial state is detached and not visible.
    assertFalse(panel.isShowing());
    assertTrue(view.glass.getClassName().contains(styleGlass));
    assertFalse(view.glass.getClassName().contains(styleGlassVisible));
    assertTrue(view.contentContainer.getClassName().contains(styleContent));
    assertFalse(view.contentContainer.getClassName().contains(styleContentVisible));
    assertNull(view.popup.getParentElement());

    // Show the popup. The popup is attached in this event loop, but styles are
    // not yet added.
    panel.show();
    assertShowing();
  }

  /**
   * Tests that the panel can be shown and hidden in the same event loop.
   */
  public void testShowThenHide() {
    panel.show();
    panel.hide();
    assertHiding();
  }

  public void testShowTwice() {
    panel.show();
    panel.show(); // No-op.
    assertShowing();
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();

    resources = GWT.create(CenterPanel.Resources.class);
    content = Elements.createDivElement();
    content.setInnerHTML("hello world");
    panel = CenterPanel.create(resources, content);
    view = panel.getView();

    styleGlass = view.css.glass();
    styleGlassVisible = view.css.glassVisible();
    styleContent = view.css.content();
    styleContentVisible = view.css.contentVisible();
  }

  @Override
  protected void gwtTearDown() throws Exception {
    super.gwtTearDown();

    // Remove the popup from the DOM.
    panel.hide();
  }

  /**
   * Asserts that the panel is transitioning to a hiding state, and eventually
   * is removed from the DOM. This method should be called after
   * {@link CenterPanel#hide()} is called.
   */
  private void assertHiding() {
    // Styles are removed in this event loop, but the popup isn't detached until
    // the animation ends.
    assertFalse(panel.isShowing());
    assertTrue(view.glass.getClassName().contains(styleGlass));
    assertFalse(view.glass.getClassName().contains(styleGlassVisible));
    assertTrue(view.contentContainer.getClassName().contains(styleContent));
    assertFalse(view.contentContainer.getClassName().contains(styleContentVisible));
    assertNotNull(view.popup.getParentElement());

    // The panel is removed from the DOM after the animation completes.
    int animDuration = view.getAnimationDuration();
    delayTestFinish(animDuration * 2); // Reset the timeout if already async.
    new Timer() {
      @Override
      public void run() {
        assertFalse(panel.isShowing());
        assertTrue(view.glass.getClassName().contains(styleGlass));
        assertFalse(view.glass.getClassName().contains(styleGlassVisible));
        assertTrue(view.contentContainer.getClassName().contains(styleContent));
        assertFalse(view.contentContainer.getClassName().contains(styleContentVisible));
        assertNull(view.popup.getParentElement());
        finishTest();
      }
    }.schedule(animDuration + 1);
  }

  /**
   * Asserts that the panel is transitioning to a showing state, and eventually
   * becomes visible. This method should be called after
   * {@link CenterPanel#show()} is called.
   */
  private void assertShowing() {
    // The popup is attached in this event loop, but styles are not yet added.
    assertTrue(panel.isShowing());
    assertTrue(view.glass.getClassName().contains(styleGlass));
    assertFalse(view.glass.getClassName().contains(styleGlassVisible));
    assertTrue(view.contentContainer.getClassName().contains(styleContent));
    assertFalse(view.contentContainer.getClassName().contains(styleContentVisible));
    assertNotNull(view.popup.getParentElement());

    // The visible styles are added after a deferred command to ensure that the
    // browser respects the CSS transitions.
    delayTestFinish(DEFERRED_COMMAND_TIMEOUT);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        assertTrue(panel.isShowing());
        assertTrue(view.glass.getClassName().contains(styleGlass));
        assertTrue(view.glass.getClassName().contains(styleGlassVisible));
        assertTrue(view.contentContainer.getClassName().contains(styleContent));
        assertTrue(view.contentContainer.getClassName().contains(styleContentVisible));
        assertNotNull(view.popup.getParentElement());
        finishTest();
      }
    });
  }
}
