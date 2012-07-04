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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * A few tests for incremental scheduler to ensure that it follows it's
 * pause/resume/cancel contract. Doesn't test if the incremental portion works
 * nor is this a strenous test of the component.
 */
public class BasicIncrementalSchedulerTests extends GWTTestCase {

  private BasicIncrementalScheduler scheduler;

  public class StubTask implements IncrementalScheduler.Task {
    private boolean wasCalled = false;

    @Override
    public boolean run(int workAmount) {
      wasCalled = true;
      return false;
    }
  }

  @Override
  public void gwtSetUp() {
    scheduler = new BasicIncrementalScheduler(100, 100);
  }

  public void testCancelDoesntUnpause() {
    scheduler.pause();
    scheduler.cancel();
    assertTrue(scheduler.isPaused());

    scheduler.resume();
    assertFalse(scheduler.isPaused());
  }

  public void testRunsDelegate() {
    scheduler.pause();
    final StubTask stubTask = new StubTask();

    scheduler.schedule(stubTask);
    assertFalse(stubTask.wasCalled);

    scheduler.resume();
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        assertTrue(stubTask.wasCalled);
      }
    });
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.util.UtilTestModule";
  }
}
