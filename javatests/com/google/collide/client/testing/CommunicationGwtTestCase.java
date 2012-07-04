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

package com.google.collide.client.testing;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * Test case superclass for tests driving mock communication APIs.
 *
 */
public abstract class CommunicationGwtTestCase extends BootstrappedGwtTestCase {

  /** milliseconds after test method completion to wait before finishTest(). */
  public static final int DEFAULT_TIMEOUT = 500;
  
  protected MockAppContext context;

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    context = new MockAppContext();
  }

  @Override
  public void gwtTearDown() throws Exception {
    super.gwtTearDown();
    try {
      context.assertIsDrained();
    } catch (Exception t) {
      // teardown can't fail the test, but at least we can warn people...
      System.err.println("Unmet expectations remain in MockAppContext at teardown");
      throw t;
    }
    context = null;
  }

  protected void waitForMocksToDrain() {
    waitForMocksToDrain(DEFAULT_TIMEOUT);
  }

  protected void waitForMocksToDrain(int timeout) {
    delayTestFinish(timeout);

    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        // and wait for a second event loop cycle, to be sure we're last:
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
          @Override
          public void execute() {
            context.assertIsDrained();
            finishTest();
          }
        });
      }
    });
  }

}
