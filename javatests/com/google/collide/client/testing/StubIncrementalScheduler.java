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

import com.google.collide.client.util.IncrementalScheduler;

/**
 * Stub incremental scheduler that can substituted for testing purposes.
 * Executes all operations synchronously and does not adjust the work amount
 * dynamically.
 */
public class StubIncrementalScheduler implements IncrementalScheduler {

  private final int workGuess;

  public StubIncrementalScheduler(int targetExecutionMs, int workGuess) {
    this.workGuess = workGuess;
  }

  @Override
  public void schedule(Task worker) {
    if (worker == null) {
      return;
    }
    while (true) {
      if (!worker.run(workGuess)) {
        break;
      }
    }
  }

  @Override
  public void cancel() {
    // no-op
  }

  @Override
  public boolean isPaused() {
    // TODO: Auto-generated method stub
    return false;
  }

  @Override
  public void pause() {
    // no-op
  }

  @Override
  public void resume() {
    // no-op
  }

  @Override
  public boolean isBusy() {
    return false;
  }

  @Override
  public void teardown() {
    // no-op
  }
}
