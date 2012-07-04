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

package com.google.collide.client.editor;

import com.google.collide.client.editor.Buffer.ScrollListener;
import com.google.collide.client.editor.Editor.KeyListener;
import com.google.collide.client.util.UserActivityManager;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerRegistrar.Remover;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * A class that listens to editor events to update the user activity manager on
 * the user's status.
 *
 */
public class EditorActivityManager {

  private JsonArray<Remover> listenerRemovers = JsonCollections.createArray();

  EditorActivityManager(final UserActivityManager userActivityManager,
      ListenerRegistrar<ScrollListener> scrollListenerRegistrar,
      ListenerRegistrar<KeyListener> keyListenerRegistrar) {

    listenerRemovers.add(scrollListenerRegistrar.add(new ScrollListener() {
      @Override
      public void onScroll(Buffer buffer, int scrollTop) {
        userActivityManager.markUserActive();
      }
    }));

    listenerRemovers.add(keyListenerRegistrar.add(new KeyListener() {
      @Override
      public boolean onKeyPress(SignalEvent event) {
        userActivityManager.markUserActive();
        return false;
      }
    }));
  }

  void teardown() {
    for (int i = 0, n = listenerRemovers.size(); i < n; i++) {
      listenerRemovers.get(i).remove();
    }
  }
}
