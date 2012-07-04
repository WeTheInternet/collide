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

package com.google.collide.client.workspace;

import com.google.collide.client.AppContext;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.client.DtoClientImpls.KeepAliveImpl;
import com.google.gwt.user.client.Timer;

/**
 * Utility class for keep-alive timer inside of a workspace.
 *
 */
public class KeepAliveTimer {

  private final Timer timer;

  private final int keepAliveTimerIntervalMs;

  public KeepAliveTimer(final AppContext appContext,
      int keepAliveTimerIntervalMs) {
    this.keepAliveTimerIntervalMs = keepAliveTimerIntervalMs;
    if (keepAliveTimerIntervalMs > 0) {

      timer = new Timer() {
        @Override
        public void run() {
          appContext.getFrontendApi().KEEP_ALIVE.send(KeepAliveImpl.make());
        }
      };
    } else {
      timer = null;
      Log.warn(getClass(), "Keep-alive interval is not set.");
    }
  }

  public void start() {
    if (timer != null) {
      timer.scheduleRepeating(keepAliveTimerIntervalMs);
    }
  }

  public void cancel() {
    if (timer == null) {
      Log.warn(this.getClass(), "Client is leaving a workspace that has no keep-alive timer.");
    } else {
      timer.cancel();
    }
  }
}
