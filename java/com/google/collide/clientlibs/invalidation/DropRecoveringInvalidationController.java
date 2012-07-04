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

package com.google.collide.clientlibs.invalidation;

import com.google.collide.clientlibs.invalidation.InvalidationManager.Recoverer;
import com.google.collide.clientlibs.invalidation.InvalidationRegistrar.Listener;
import com.google.collide.clientlibs.invalidation.InvalidationRegistrar.Listener.AsyncProcessingHandle;
import com.google.collide.dto.RecoverFromDroppedTangoInvalidationResponse.RecoveredPayload;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.invalidations.InvalidationObjectId;
import com.google.collide.shared.invalidations.InvalidationUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.Reorderer;
import com.google.collide.shared.util.Timer;
import com.google.collide.shared.util.Reorderer.ItemSink;
import com.google.collide.shared.util.Reorderer.TimeoutCallback;
import com.google.collide.shared.util.Timer.Factory;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Random;

/**
 * A controller for ensuring all (even dropped payload) invalidations are given to the
 * {@link Listener} in-order. This will queue out-of-order invalidations (if prior invalidations
 * were dropped), perform out-of-band recovery, and replay invalidations in-order to the listener.
 *
 */
class DropRecoveringInvalidationController {

  private static final int ERROR_RETRY_DELAY_MS = 15000;
  
  private final InvalidationLogger logger;
  private final Factory timerFactory;
  private final InvalidationObjectId<?> objectId;
  private final Recoverer recoverer;
  
  /** Reorderer of invalidations, storing the payload. */
  private final Reorderer<String> invalidationReorderer;
  private boolean isRecovering;
  
  private class ReordererSink implements ItemSink<String> {
    
    private final Listener listener;
    
    private boolean isListenerProcessingAsync;
    private final AsyncProcessingHandle asyncProcessingHandle = new AsyncProcessingHandle() {
      @Override
      public void startedAsyncProcessing() {
        isListenerProcessingAsync = true;
      }

      @Override
      public void finishedAsyncProcessing() {
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
          @Override
          public void execute() {
            isListenerProcessingAsync = false;
            dispatchQueuedPayloads();
          }
        });
      }
    };

    private final JsonArray<String> queuedPayloads = JsonCollections.createArray();
    private int firstQueuedPayloadVersion;
    
    ReordererSink(Listener listener) {
      this.listener = listener;
    }
    
    @Override
    public void onItem(String payload, int version) {
      if (!isListenerProcessingAsync) {
        listener.onInvalidated(objectId.getName(), version, payload, asyncProcessingHandle);
      } else {
        if (queuedPayloads.isEmpty()) {
          firstQueuedPayloadVersion = version;
        }
        
        queuedPayloads.add(payload);
      }
    }
    
    void handleSetNextExpectedVersion(int nextExpectedVersion) {
      while (!queuedPayloads.isEmpty() && firstQueuedPayloadVersion < nextExpectedVersion) {
        queuedPayloads.remove(0);
        firstQueuedPayloadVersion++;
      }
    }

    private void dispatchQueuedPayloads() {
      while (!queuedPayloads.isEmpty() && !isListenerProcessingAsync) {
        listener.onInvalidated(objectId.getName(), firstQueuedPayloadVersion++,
            queuedPayloads.remove(0), asyncProcessingHandle);
      }
    }
  }
  
  private final ReordererSink reorderedItemSink;
  
  private final TimeoutCallback outOfOrderCallback = new TimeoutCallback() {
    @Override
    public void onTimeout(int lastVersionDispatched) {
      logger.fine(
          "Entering recovery for object %s, last version %s, due to out-of-order.", objectId,
          lastVersionDispatched);
      recover();
    } 
  };
  
  DropRecoveringInvalidationController(InvalidationLogger logger, InvalidationObjectId<?> objectId,
      InvalidationRegistrar.Listener listener, Recoverer recoverer,
      Timer.Factory timerFactory) {
    this.logger = logger;
    this.objectId = objectId;
    this.recoverer = recoverer;
    this.timerFactory = timerFactory;
    
    reorderedItemSink = new ReordererSink(listener);
    
    /*
     * We don't know the actual version to expect at this point, so we will just queue until our
     * client calls through with a version to expect
     */
    // TimeoutMs is very small since tango will not deliver out-of-order items.
    invalidationReorderer =
        Reorderer.create(0, reorderedItemSink, 1, outOfOrderCallback, timerFactory);
    invalidationReorderer.queueUntilSkipToVersionIsCalled();
    
    // Disable timeout until we know the next expected version
    invalidationReorderer.setTimeoutEnabled(false);
  }
  
  void cleanup() {
    invalidationReorderer.cleanup();
  }
  
  void setNextExpectedVersion(int nextExpectedVersion) {
    reorderedItemSink.handleSetNextExpectedVersion(nextExpectedVersion);
    invalidationReorderer.skipToVersion(nextExpectedVersion);
    invalidationReorderer.setTimeoutEnabled(true);
  }

  /**
   * @param version the version of the payload, or
   *        {@link InvalidationUtils#UNKNOWN_VERSION}
   */
  void handleInvalidated(String payload, long version, boolean isEmptyPayload) {
    if (version > Integer.MAX_VALUE) {
      // 1 invalidation per sec would take 24k days to exhaust the positive integer range
      throw new IllegalStateException("Version space exhausted (int on client)");
    }

    // Check if we dropped a payload or msised an invalidation
    if (version == InvalidationUtils.UNKNOWN_VERSION || (payload == null && !isEmptyPayload)) {
      logger.fine("Entering recovery due to unknown version or missing payload."
          + " Object id: %s, Version: %s, Payload(%s): %s", objectId, version, isEmptyPayload,
          payload);
      recover();
      return;
    }
    
    invalidationReorderer.acceptItem(payload, (int) version);
  }
  
  void recover() {
    if (isRecovering) {
      return;
    }
    
    isRecovering = true;
    
    int currentClientVersion = invalidationReorderer.getNextExpectedVersion() - 1;
    
    // Disable timeout, as it would trigger a recover
    invalidationReorderer.setTimeoutEnabled(false);
    
    // Perform XHR, feed results into reorderer, get callbacks
    recoverer.recoverPayloads(objectId, currentClientVersion, new Recoverer.Callback() {
      @Override
      public void onPayloadsRecovered(JsonArray<RecoveredPayload> payloads, int currentVersion) {
        for (int i = 0; i < payloads.size(); i++) {
          RecoveredPayload payload = payloads.get(i);
          invalidationReorderer.acceptItem(payload.getPayload(), payload.getPayloadVersion());
        }
        
        invalidationReorderer.skipToVersion(currentVersion + 1);
        isRecovering = false;
        invalidationReorderer.setTimeoutEnabled(true);
      }

      @Override
      public void onError() {
        // Consider ourselves to be in the "retrying" state during this delay
        timerFactory.createTimer(new Runnable() {
          @Override
          public void run() {
            isRecovering = false;
            
            // This will end up retrying
            invalidationReorderer.setTimeoutEnabled(true);
          }
        }).schedule(ERROR_RETRY_DELAY_MS + Random.nextInt(ERROR_RETRY_DELAY_MS / 10));
      }
    });
  }
}
