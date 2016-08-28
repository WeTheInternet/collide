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

import com.google.collide.json.shared.JsonIntegerMap;
import com.google.collide.shared.util.Timer.Factory;

/**
 * A utility class that accepts out-of-order versioned items and delivers them to an
 * {@link ItemSink} in-order.
 */
public class Reorderer<T> {
  
  public interface ItemSink<T> {
    void onItem(T item, int version);
  }

  public interface TimeoutCallback {
    void onTimeout(int lastVersionDispatched);
  }
  
  /**
   * @param firstVersionToExpect the first version to expect
   * @param timeoutMs the amount of time to wait before triggering the {@code timeoutCallback} for
   *        dropped items
   */
  public static <T> Reorderer<T> create(int firstVersionToExpect, ItemSink<T> itemSink,
      int timeoutMs, TimeoutCallback timeoutCallback, Factory timerFactory) {
    return new Reorderer<T>(
        firstVersionToExpect, itemSink, timeoutMs, timeoutCallback, timerFactory);
  }

  private boolean isQueueingUntilSkipToVersionCalled;
  private int nextExpectedVersion;
  private final ItemSink<T> itemSink;
  private JsonIntegerMap<T> itemsByVersion = JsonCollections.createIntegerMap();
  
  private boolean isTimeoutEnabled = true;
  private final int timeoutMs;
  private final TimeoutCallback timeoutCallback;
  private final Timer timeoutTimer;
  private final Runnable timeoutTimerRunnable = new Runnable() {
    @Override
    public void run() {
      timeoutCallback.onTimeout(nextExpectedVersion - 1);
    }
  };
  
  private Reorderer(int firstVersionToExpect, ItemSink<T> itemSink, int timeoutMs,
      TimeoutCallback timeoutCallback, Timer.Factory timerFactory) {
    this.itemSink = itemSink;
    this.timeoutMs = timeoutMs;
    this.timeoutCallback = timeoutCallback;
    this.nextExpectedVersion = firstVersionToExpect;
    
    timeoutTimer = timerFactory.createTimer(timeoutTimerRunnable);
  }

  public void cleanup() {
    setTimeoutEnabled(false);
  }
  
  public int getNextExpectedVersion() {
    return nextExpectedVersion;
  }
  
  /**
   * Enables the timeout feature.
   *
   * <p>
   * If there are out-of-order items queued, this will immediately start the timeout timer.
   */
  public void setTimeoutEnabled(boolean isTimeoutEnabled) {
    this.isTimeoutEnabled = isTimeoutEnabled;
    if (isTimeoutEnabled) {
      scheduleTimeoutIfEnabledAndNecessary();
    } else {
      cancelTimeout();
    }
  }
  
  /**
   * Allows the client to skip ahead to a version. For example, if a client fills in the gap
   * out-of-band, this should be called afterward to begin reordering at the latest version.
   */
  public void skipToVersion(int nextVersionToExpect) {
    this.nextExpectedVersion = nextVersionToExpect;
    isQueueingUntilSkipToVersionCalled = false;
    
    // Cancel any pending timer (we'll re-set one later if necessary)
    cancelTimeout();
    
    // Remove items for old versions that we no longer care about
    removeOldVersions(nextExpectedVersion - 1);
    
    // See if we have any items at the new and following versions
    dispatchQueuedStartingAtNextExpectedVersion();

    scheduleTimeoutIfEnabledAndNecessary();
  }

  /*
   * This is purposefully not a setXxx since canceling the queueing requires dispatching queued
   * version, etc. and that's not a code path we're going to use immediately, so punting.
   */
  public void queueUntilSkipToVersionIsCalled() {
    isQueueingUntilSkipToVersionCalled = true;
  }
  
  private void removeOldVersions(final int maxVersionToRemove) {
    // Simulate a set from a map
    final JsonIntegerMap<Void> oldVersionsToRemove = JsonCollections.createIntegerMap();
    itemsByVersion.iterate(new JsonIntegerMap.IterationCallback<T>() {
      @Override
      public void onIteration(int version, T val) {
        if (version <= maxVersionToRemove) {
          // Can't remove in-place, so queue for removal
          oldVersionsToRemove.put(version, null);
        }
      }
    });
    
    oldVersionsToRemove.iterate(new JsonIntegerMap.IterationCallback<Void>() {
      @Override
      public void onIteration(int version, Void val) {
        itemsByVersion.erase(version);
      }
    });
  }
  
  public void acceptItem(T item, int version) {
    if (version < nextExpectedVersion) {
      // Ignore, we've already passed this version onto our client
      return;
    }
    
    final boolean hadPreviouslyStoredItems = !itemsByVersion.isEmpty();
    itemsByVersion.put(version, item);
    
    if (isQueueingUntilSkipToVersionCalled) {
      // The item is stored, exit
      return;
    }
    
    if (version == nextExpectedVersion) {
      cancelTimeout();
      dispatchQueuedStartingAtNextExpectedVersion();
      
      /*
       * E.g. previous to this call, nextExpectedVersion=3, we have queued 4, 5, 7. We move to
       * nextExpectedVersion=6 and are now waiting on it to come in, so schedule a timeout.
       */
      scheduleTimeoutIfEnabledAndNecessary();
      
    } else {
      if (!hadPreviouslyStoredItems) {
        /*
         * This is the first time we've missed the item at nextExpectedVersion. We wouldn't always
         * want to do this when we receive a future item because that would keep resetting the
         * timeout for the nextExpectedVersion item.
         *
         * For example, imagine we are waiting for v2 (assume it got dropped indefinitely). When we
         * get v3, itemsByVersion will be empty, so we schedule a timeout. If/when we get v4, v5,
         * v6, ..., we don't want to keep resetting the timeout for each item that comes in -- we'd
         * never actually timeout in a busy session!
         */
        scheduleTimeoutIfEnabled();
      }
    }
  }
  
  /**
   * Dispatches to the {@link ItemSink}. This should be the ONLY place that dispatches given the
   * subtle pause behavior that is possible.
   */
  private void dispatchQueuedStartingAtNextExpectedVersion() {
    for (; itemsByVersion.hasKey(nextExpectedVersion); nextExpectedVersion++) {
      T item = itemsByVersion.get(nextExpectedVersion);
      itemsByVersion.erase(nextExpectedVersion);
      
      itemSink.onItem(item, nextExpectedVersion);
    }
  }
  
  private void scheduleTimeoutIfEnabledAndNecessary() {
    if (!itemsByVersion.isEmpty()) {
      scheduleTimeoutIfEnabled();
    }
  }
  
  private void scheduleTimeoutIfEnabled() {
    if (isTimeoutEnabled) {
      timeoutTimer.schedule(timeoutMs);
    }
  }
  
  private void cancelTimeout() {
    timeoutTimer.cancel();
  }
}
