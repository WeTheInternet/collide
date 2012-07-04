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

package com.google.collide.client.testutil;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.impl.SchedulerImpl;

/**
 * Scheduler implementation that allows to disable scheduling at runtime.
 *
 */
public class TestSchedulerImpl extends SchedulerImpl {

  /**
   * Implementation that ignores scheduled tasks.
   */
  public static class NoOpScheduler extends Scheduler {

    @Override
    public void scheduleDeferred(ScheduledCommand scheduledCommand) {}

    @Override
    public void scheduleEntry(RepeatingCommand repeatingCommand) {}

    @Override
    public void scheduleEntry(ScheduledCommand scheduledCommand) {}

    @Override
    public void scheduleFinally(RepeatingCommand repeatingCommand) {}

    @Override
    public void scheduleFinally(ScheduledCommand scheduledCommand) {}

    @Override
    public void scheduleFixedDelay(RepeatingCommand repeatingCommand, int i) {}

    @Override
    public void scheduleFixedPeriod(RepeatingCommand repeatingCommand, int i) {}

    @Override
    public void scheduleIncremental(RepeatingCommand repeatingCommand) {}
  }

  /**
   * Implementation that throws exception on its methods invocation.
   */
  public static class AngryScheduler extends Scheduler {

    @Override
    public void scheduleDeferred(ScheduledCommand scheduledCommand) {
      throw new IllegalStateException("No scheduling allowed");
    }

    @Override
    public void scheduleEntry(RepeatingCommand repeatingCommand)  {
      throw new IllegalStateException("No scheduling allowed");
    }

    @Override
    public void scheduleEntry(ScheduledCommand scheduledCommand)  {
      throw new IllegalStateException("No scheduling allowed");
    }

    @Override
    public void scheduleFinally(RepeatingCommand repeatingCommand)  {
      throw new IllegalStateException("No scheduling allowed");
    }

    @Override
    public void scheduleFinally(ScheduledCommand scheduledCommand)  {
      throw new IllegalStateException("No scheduling allowed");
    }

    @Override
    public void scheduleFixedDelay(RepeatingCommand repeatingCommand, int i)  {
      throw new IllegalStateException("No scheduling allowed");
    }

    @Override
    public void scheduleFixedPeriod(RepeatingCommand repeatingCommand, int i)  {
      throw new IllegalStateException("No scheduling allowed");
    }

    @Override
    public void scheduleIncremental(RepeatingCommand repeatingCommand)  {
      throw new IllegalStateException("No scheduling allowed");
    }
  }

  public static final Scheduler NO_OP_SCHEDULER_IMPL = new NoOpScheduler();

  private static Scheduler scheduler;

  @Override
  public void scheduleDeferred(ScheduledCommand cmd) {
    if (scheduler != null) {
      scheduler.scheduleDeferred(cmd);
    } else {
      super.scheduleDeferred(cmd);
    }
  }

  @Override
  public void scheduleEntry(RepeatingCommand cmd) {
    if (scheduler != null) {
      scheduler.scheduleEntry(cmd);
    } else {
      super.scheduleEntry(cmd);
    }
  }

  @Override
  public void scheduleEntry(ScheduledCommand cmd) {
    if (scheduler != null) {
      scheduler.scheduleEntry(cmd);
    } else {
      super.scheduleEntry(cmd);
    }
  }

  @Override
  public void scheduleFinally(RepeatingCommand cmd) {
    if (scheduler != null) {
      scheduler.scheduleFinally(cmd);
    } else {
      super.scheduleFinally(cmd);
    }
  }

  @Override
  public void scheduleFinally(ScheduledCommand cmd) {
    if (scheduler != null) {
      scheduler.scheduleFinally(cmd);
    } else {
      super.scheduleFinally(cmd);
    }
  }

  @Override
  public void scheduleFixedDelay(RepeatingCommand cmd, int delayMs) {
    if (scheduler != null) {
      scheduler.scheduleFixedDelay(cmd, delayMs);
    } else {
      super.scheduleFixedDelay(cmd, delayMs);
    }
  }

  @Override
  public void scheduleFixedPeriod(RepeatingCommand cmd, int delayMs) {
    if (scheduler != null) {
      scheduler.scheduleFixedPeriod(cmd, delayMs);
    }
    super.scheduleFixedPeriod(cmd, delayMs);
  }

  @Override
  public void scheduleIncremental(RepeatingCommand cmd) {
    if (scheduler != null) {
      scheduler.scheduleIncremental(cmd);
    } else {
      super.scheduleIncremental(cmd);
    }
  }

  public static void setNoOp(boolean noOp) {
    if (noOp) {
      if (scheduler != null) {
        throw new IllegalStateException("Can enter noOp mode only from standard mode");
      }
      scheduler = NO_OP_SCHEDULER_IMPL;
    } else {
      if (scheduler != NO_OP_SCHEDULER_IMPL) {
        throw new IllegalStateException("Can enter standard mode only from noOp mode");
      }
      scheduler = null;
    }
  }

  public static void runWithSpecificScheduler(Runnable runnable, Scheduler taskScheduler) {
    if (scheduler != NO_OP_SCHEDULER_IMPL) {
      throw new IllegalStateException("Can run runWithSpecificScheduler only from noOp mode");
    }

    try {
      scheduler = taskScheduler;
      runnable.run();

      if (scheduler != taskScheduler) {
        throw new IllegalStateException("Scheduler has been changed during execution");
      }
    } finally {
      scheduler = NO_OP_SCHEDULER_IMPL;
    }
  }
}
