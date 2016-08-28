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

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Executes the queued commands after {@link #execute(Runnable)} has been called
 * the given number of times.
 * 
 * Any calls to {@link #execute(Runnable)} after the given count has been
 * reached will be executed synchronously.
 * 
 */
public class CountExecutor implements Executor {

  private final int totalCount;
  private int curCount;

  private final JsonArray<Runnable> commands = JsonCollections.createArray();
  
  public CountExecutor(int totalCount) {
    this.totalCount = totalCount;
  }

  /**
   * Called with the intent to either have the given {@code command} executed
   * and/or increase the count.
   * 
   * @param command optional, the command to execute
   */
  @Override
  public void execute(Runnable command) {
    
    if (command != null) {
      commands.add(command);
    }
    
    if (++curCount == totalCount) {
      for (int i = 0, n = commands.size(); i < n; i++) {
        commands.get(i).run();
      }
    } else if (curCount > totalCount) {
      command.run();
    }
  }
}
