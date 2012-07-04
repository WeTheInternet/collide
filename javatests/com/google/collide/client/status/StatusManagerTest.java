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

package com.google.collide.client.status;

import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.json.client.JsoArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.junit.client.GWTTestCase;


/**
 * These test cases exercise the event pumping of the {@link StatusManager} to
 * the {@link StatusHandler}.
 */
public class StatusManagerTest extends GWTTestCase {

  /**
   * A simple mock handler that lets us record events. Each message event is
   * appended to the statusMesssages array and the number of clear() calls is
   * tracked.
   */
  private class MockStatusHandler implements StatusHandler {
    private int clearCount = 0;
    private JsoArray<StatusMessage> statusMessages = JsoArray.create();

    @Override
    public void clear() {
      clearCount++;
    }

    @Override
    public void onStatusMessage(StatusMessage msg) {
      statusMessages.add(msg);
    }
  }

  private StatusMessage c1;

  private StatusMessage c2;

  private StatusMessage e1;

  private StatusMessage e2;

  private StatusMessage f1;

  private StatusMessage f2;

  private MockStatusHandler handler;

  private StatusMessage l1;

  private StatusMessage l2;

  private StatusManager statusManager;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    statusManager = new StatusManager();
    handler = new MockStatusHandler();
    statusManager.setHandler(handler);

    l1 = new StatusMessage(statusManager, MessageType.LOADING, "l1");
    l2 = new StatusMessage(statusManager, MessageType.LOADING, "l2");

    c1 = new StatusMessage(statusManager, MessageType.CONFIRMATION, "c1");
    c2 = new StatusMessage(statusManager, MessageType.CONFIRMATION, "c2");

    e1 = new StatusMessage(statusManager, MessageType.ERROR, "e1");
    e2 = new StatusMessage(statusManager, MessageType.ERROR, "e2");

    f1 = new StatusMessage(statusManager, MessageType.FATAL, "f1");
    f2 = new StatusMessage(statusManager, MessageType.FATAL, "f2");
  }

  /**
   * Verify that canceling non-active messages triggers no events.
   */
  public void testCancelNonActive() {
    e1.fire();
    l1.fire();
    c2.fire();
    l2.fire();
    l1.cancel();
    c1.fire();
    c1.cancel();
    c2.cancel();
    l2.cancel();
    assertEquals(1, handler.statusMessages.size());
    assertEquals(e1, handler.statusMessages.peek());
    assertEquals(0, handler.clearCount);
  }


  /**
   * Verify that an event cannot be fired after being canceled.
   */
  public void testCancelThenFire() {
    e1.cancel();
    e1.fire();
    assertEquals(0, handler.statusMessages.size());
  }

  /**
   * Verify that delayed fire works.
   */
  public void testDelayFire() {
    l1.fire();
    e1.fireDelayed(100);
    c1.fire();
    assertEquals(2, handler.statusMessages.size());
    assertEquals(c1, handler.statusMessages.peek());
    delayTestFinish(300);
    Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
      @Override
      public boolean execute() {
        assertEquals(3, handler.statusMessages.size());
        assertEquals(e1, handler.statusMessages.peek());
        finishTest();
        return false;
      }
    }, 200);
  }

  /**
   * Verify that expiry works.
   */
  public void testExpire() {
    l1.expire(100);
    l2.fire();
    l1.fire();
    assertEquals(2, handler.statusMessages.size());
    assertEquals(l1, handler.statusMessages.peek());
    assertEquals(0, handler.clearCount);

    delayTestFinish(300);
    Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

      @Override
      public boolean execute() {
        assertEquals(3, handler.statusMessages.size());
        assertEquals(l2, handler.statusMessages.peek());
        finishTest();
        return false;
      }
    }, 200);
  }


  public void testFatalHandoff() {
    MockStatusHandler handoff = new MockStatusHandler();
    f1.fire();
    e1.fire();
    statusManager.setHandler(handoff);
    f2.fire();
    assertEquals(1, handler.statusMessages.size());
    assertEquals(f1, handler.statusMessages.peek());
    assertEquals(1, handler.clearCount);
    assertEquals(1, handoff.statusMessages.size());
    assertEquals(f1, handoff.statusMessages.peek());
  }

  public void testFatalIsFinal() {
    l1.fire();
    c1.fire();
    f1.fire();
    assertEquals(3, handler.statusMessages.size());
    assertEquals(f1, handler.statusMessages.peek());

    e1.fire();
    f1.cancel();
    f2.fire();
    assertEquals(3, handler.statusMessages.size());
    assertEquals(f1, handler.statusMessages.peek());

    f1.fire();
    assertEquals(3, handler.statusMessages.size());
    assertEquals(f1, handler.statusMessages.peek());
  }

  /**
   * Verify that handing off to a new handler clears the current one and fires
   * the active message to the new one.
   */
  public void testHandlerHandoff() {
    MockStatusHandler handoff = new MockStatusHandler();
    c1.fire();
    l1.fire();
    e2.fire();
    statusManager.setHandler(handoff);
    assertEquals(1, handler.clearCount);
    assertEquals(1, handoff.statusMessages.size());
    assertEquals(e2, handoff.statusMessages.peek());
    e2.cancel();
    assertEquals(2, handoff.statusMessages.size());
    assertEquals(c1, handoff.statusMessages.peek());
  }

  /**
   * Verify our gwtSetup is sane.
   */
  public void testInit() {
    assertEquals(0, handler.statusMessages.size());
    assertEquals(0, handler.clearCount);
  }

  /**
   * Verify that firing a non-active event multiple times is a no-op.
   */
  public void testMultipleFireNoop() {
    e1.fire();
    e2.fire();
    assertEquals(2, handler.statusMessages.size());
    assertEquals(e2, handler.statusMessages.peek());

    e1.fire();
    assertEquals(2, handler.statusMessages.size());
    assertEquals(e2, handler.statusMessages.peek());
    assertEquals(0, handler.clearCount);
  }

  /**
   * Verify that firing an active event multiple times issues updates.
   */
  public void testMultipleFireUpdate() {
    e1.fire();
    e2.fire();
    assertEquals(2, handler.statusMessages.size());
    assertEquals(e2, handler.statusMessages.peek());

    e2.fire();
    assertEquals(3, handler.statusMessages.size());
    assertEquals(e2, handler.statusMessages.get(2));
    assertEquals(e2, handler.statusMessages.get(1));
    assertEquals(0, handler.clearCount);
  }

  /**
   * Verify that messages of equivalent priority tie-break by most recent.
   */
  public void testRecentPriority() {
    e1.fire();
    c1.fire();
    e2.fire();
    assertEquals(2, handler.statusMessages.size());
    assertEquals(e2, handler.statusMessages.peek());
  }

  /**
   * Verify that cancellation works.
   */
  public void testSimpleCancel() {
    l1.fire();
    l1.cancel();
    assertEquals(1, handler.statusMessages.size());
    assertEquals(l1, handler.statusMessages.peek());
    assertEquals(1, handler.clearCount);
  }

  /**
   * Verify that a simple message works.
   */
  public void testSimpleMessage() {
    l1.fire();
    assertEquals(1, handler.statusMessages.size());
    assertEquals(l1, handler.statusMessages.peek());
    assertEquals(0, handler.clearCount);
  }

  /**
   * Verify that our priority logic is sane.
   */
  public void testSimplePriority() {
    e1.fire();
    c1.fire();
    l1.fire();
    assertEquals(1, handler.statusMessages.size());
    assertEquals(e1, handler.statusMessages.peek());
  }
}
