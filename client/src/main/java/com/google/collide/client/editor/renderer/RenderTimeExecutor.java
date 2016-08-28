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

package com.google.collide.client.editor.renderer;

import com.google.collide.client.util.Executor;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * An executor that will defer the commands given to {@link #execute(Runnable)}
 * until render-time.
 * 
 */
public class RenderTimeExecutor implements Executor {

  private final JsonArray<Runnable> commands = JsonCollections.createArray();
  
  @Override
  public void execute(Runnable command) {
    commands.add(command);
  }
  
  void executeQueuedCommands() {
    for (int i = 0, n = commands.size(); i < n; i++) {
      commands.get(i).run();
    }
    
    commands.clear();
  }
}
