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

package com.google.collide.client.codeunderstanding;

import com.google.collide.client.communication.FrontendApi;
import com.google.collide.client.util.DeferredCommandExecutor;
import com.google.collide.client.util.logging.Log;
import com.google.collide.clientlibs.invalidation.InvalidationRegistrar;
import com.google.collide.dto.CodeGraphRequest;
import com.google.collide.dto.CodeGraphResponse;
import com.google.collide.dto.CubePing;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.client.DtoUtils;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * A response distributor for {@link CubeState}.
 *
 * Presents a high level API for end clients.
 *
 */
public class CubeClient
    implements CubeState.CubeResponseDistributor, InvalidationRegistrar.Listener {

  /**
   * A command and listener that requests state refresh after text changes.
   *
   * <p>Idle timeout is 10 x 100ms = 1s.
   */
  private class RefreshWatchdog extends DeferredCommandExecutor {

    protected RefreshWatchdog() {
      super(100);
    }

    @Override
    protected boolean execute() {
      state.refresh();
      return false;
    }
  }

  /**
   * Request sender / response receiver & logic.
   */
  private final CubeState state;

  /**
   * List of subscribers.
   */
  private final JsonArray<CubeUpdateListener> listeners = JsoArray.create();

  /**
   * Text changes detector / idle period actor.
   */
  private final RefreshWatchdog refreshWatchdog = new RefreshWatchdog();

  /**
   * Constructs object, and initialises it's state with given parameters.
   * 
   * @param api Cube API
   */
  CubeClient(
      FrontendApi.RequestResponseApi<CodeGraphRequest, CodeGraphResponse> api) {
    Preconditions.checkNotNull(api);
    state = new CubeState(api, this);
  }

  public void addListener(CubeUpdateListener listener) {
    listeners.add(listener);
  }

  /**
   * Prevents further response distribution / processing.
   */
  void cleanup() {
    refreshWatchdog.cancel();
    listeners.clear();
    state.dismiss();
  }

  @Override
  public void notifyListeners(CubeDataUpdates updates) {
    if (!CubeDataUpdates.hasUpdates(updates)) {
      return;
    }
    int l = listeners.size();
    for (int i = 0; i < l; i++) {
      listeners.get(i).onCubeResponse(state.getData(), updates);
    }
  }

  @Override
  public void onInvalidated(String objectName, long version, @Nullable String payload,
      AsyncProcessingHandle asyncProcessingHandle) {
    if (payload == null) {
      // darn - we lost the payload; this just means that we should refresh
      // state just in case its changed (which may be inefficient if our state
      // actually isn't out of date)
    } else {
      CubePing message;
      try {
        message = DtoUtils.parseAsDto(payload, RoutingTypes.CUBEPING);
      } catch (Exception e) {
        Log.warn(getClass(), "Failed to deserialize Tango payload", e);
        return;
      }

      // TODO: We should use message.getFullGraphFreshness()
    }

    // if we haven't returned out yet, that means we need to refresh state
    state.refresh();
  }

  public CubeData getData() {
    return state.getData();
  }

  public void removeListener(CubeUpdateListener listener) {
    listeners.remove(listener);
  }

  void setPath(String filePath) {
    Preconditions.checkNotNull(filePath);
    state.setFilePath(filePath);
    refreshWatchdog.cancel();
    state.refresh();
  }

  void refresh() {
    refreshWatchdog.schedule(10);
  }
}
