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

import javax.annotation.Nullable;

import com.google.collide.client.status.StatusManager;
import com.google.collide.client.util.ClientTimer;
import com.google.collide.dto.RecoverFromDroppedTangoInvalidationResponse.RecoveredPayload;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.invalidations.InvalidationObjectId;
import com.google.collide.shared.invalidations.InvalidationObjectId.VersioningRequirement;
import com.google.collide.shared.invalidations.InvalidationUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;


/**
 * A manager which is responsible for registering and unregistering objects with Tango and notifying
 * the appropriate listener.
 *
 */
public class InvalidationManager implements InvalidationRegistrar {

  /**
   * The version used when calling {@link InvalidationRegistrar.Listener#onInvalidated} when Tango
   * reports that the invalidation's version was unknown.
   */
  public static InvalidationManager create(StatusManager statusManager, Recoverer recoverer) {
    return new InvalidationManager(recoverer, new DropRecoveringInvalidationControllerFactory(
        logger, ClientTimer.FACTORY));
  }

  /**
   * Allows for clients to recover from missing or squelched payloads.
   */
  public interface Recoverer {

    /**
     * Called from {@link Recoverer#recoverPayloads(InvalidationObjectId, int, Callback)} with the
     * response.
     */
    public interface Callback {
      void onPayloadsRecovered(JsonArray<RecoveredPayload> payloads, int currentObjectVersion);

      void onError();
    }

    /**
     * Called when a recovery is needed.
     *
     * @param objectId the ID of the object that needs to be recovered
     * @param currentClientVersion the last version of the object that was delivered to the client
     */
    void recoverPayloads(InvalidationObjectId<?> objectId, int currentClientVersion, Callback callback);
  }

  private static final InvalidationLogger logger = InvalidationLogger.create();

  // Listener-scoped
  /** Keyed by the object name */
  private final JsonStringMap<InvalidationRegistrar.Listener> listenerMap =
      JsonCollections.createMap();
  /** Keyed by the object name */
  private final JsonStringMap<DropRecoveringInvalidationController>
      dropRecoveringInvalidationControllers = JsonCollections.createMap();

  // Misc
  private final Recoverer recoverer;

  private final DropRecoveringInvalidationControllerFactory
      dropRecoveringInvalidationControllerFactory;

  private InvalidationManager(Recoverer recoverer,
      DropRecoveringInvalidationControllerFactory dropRecoveringInvalidationControllerFactory) {
    if (recoverer == null) {
      recoverer = new Recoverer() {
        @Override
        public void recoverPayloads(
            InvalidationObjectId<?> objectId, int currentClientVersion, Callback callback) {
          // null operation
        }

      };
    }
    this.recoverer = recoverer;
    this.dropRecoveringInvalidationControllerFactory = dropRecoveringInvalidationControllerFactory;
  }

  /**
   * Registers a new listener for a specific object.
   */
  @Override
  public RemovableHandle register(final InvalidationObjectId<?> objectId, Listener eventListener) {
    logger.fine("Registering object: %s", objectId);

    final String objectName = objectId.getName();
    listenerMap.put(objectName, eventListener);

    final DropRecoveringInvalidationController dropRecoveringInvalidationController;
    if (objectId.getVersioningRequirement() == VersioningRequirement.PAYLOADS) {
      dropRecoveringInvalidationController =
          dropRecoveringInvalidationControllerFactory.create(objectId, eventListener, recoverer);
      dropRecoveringInvalidationControllers.put(objectName, dropRecoveringInvalidationController);
    } else {
      dropRecoveringInvalidationController = null;
    }

    return new RemovableHandle() {
      @Override
      public void initializeRecoverer(long nextExpectedVersion) {
        Preconditions.checkState(dropRecoveringInvalidationController != null,
            "You did need initialize a recoverer for this object: " + objectId);
        Preconditions.checkArgument(nextExpectedVersion >= InvalidationUtils.INITIAL_OBJECT_VERSION,
            "You can't initialize the recoverer to a value < " + InvalidationUtils.INITIAL_OBJECT_VERSION
            + ": " + objectId + "(" + nextExpectedVersion + ")");

        // TODO: Fix up the long vs int debacle.
        dropRecoveringInvalidationController.setNextExpectedVersion((int) nextExpectedVersion);
      }

      @Override
      public void remove() {
        unregister(objectId);
      }
    };
  }

  /**
   * Unregisters an object Id.
   */
  @Override
  public void unregister(InvalidationObjectId<?> objectId) {
    logger.fine("Unregistering object Id: %s", objectId);
    final String objectName = objectId.getName();
    listenerMap.remove(objectName);

    DropRecoveringInvalidationController dropRecoveringInvalidationController =
        dropRecoveringInvalidationControllers.remove(objectName);
    if (dropRecoveringInvalidationController != null) {
      dropRecoveringInvalidationController.cleanup();
    }
  }

  public void handleInvalidation(String name, long version, @Nullable String payload) {
    logger.fine("Invalidation for %s: Version: %s Payload: %s", name, version, payload);

    InvalidationRegistrar.Listener listener = listenerMap.get(name);
    if (listener == null) {
      logger.severe("The listener does not exist for this objectId: %s", name);
      return;
    }

    DropRecoveringInvalidationController dropRecoveringInvalidationController =
        dropRecoveringInvalidationControllers.get(name);
    if (dropRecoveringInvalidationController != null) {
      // we check if the payload is the special string indicating null
      boolean isEmptyPayload = payload != null && payload.equals(InvalidationObjectId.EMPTY_PAYLOAD);

      dropRecoveringInvalidationController.handleInvalidated(
          isEmptyPayload ? null : payload, version, isEmptyPayload);
    } else {
      listener.onInvalidated(name, version, payload, null);
    }
  }
}
