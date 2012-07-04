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

package com.google.collide.shared.util;

import com.google.collide.json.shared.JsonArray;

/**
 * A timer implementation that allows for the client to trigger the callback.
 */
public class MockTimer implements Timer {

  public static class Factory implements Timer.Factory {
    private final JsonArray<MockTimer> timers = JsonCollections.createArray();    
    
    @Override
    public Timer createTimer(Runnable runnable) {
      MockTimer timer = new MockTimer(runnable);
      timers.add(timer);
      
      return timer;
    }
    
    public void tickTime(int timeMs) {
      for (int i = 0; i < timers.size(); i++) {
        timers.get(i).tickTime(timeMs);
      }
    }
    
    public void tickToFireAllTimers() {
      tickTime(Integer.MAX_VALUE);
    }
  }
  
  private static final int NOT_SCHEDULED = -1;
  
  private final Runnable runnable;
  private int remainingDelayMs = NOT_SCHEDULED;


  public MockTimer(Runnable runnable) {
    this.runnable = runnable;
  }

  @Override
  public void schedule(int delayMs) {
    remainingDelayMs = delayMs;
  }

  private void tickTime(int timeMs) {
    if (remainingDelayMs >= 0) {
      remainingDelayMs -= timeMs;
      if (remainingDelayMs <= 0) {
        runnable.run();
        remainingDelayMs = NOT_SCHEDULED;
      }
    }
  }
  
  @Override
  public void cancel() {
    remainingDelayMs = NOT_SCHEDULED;
  }
}
