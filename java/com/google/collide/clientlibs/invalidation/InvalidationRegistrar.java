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

import com.google.collide.dto.client.DtoUtils;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.shared.invalidations.InvalidationObjectId;
import com.google.collide.shared.util.ListenerRegistrar.Remover;

import javax.annotation.Nullable;


/**
 * An object which can register and unregister for Tango invalidations.
 */
public interface InvalidationRegistrar {

  /**
   * A handle to the registration for invalidations.
   *
   * <p>
   * Clients that registered with {@code recoverMissingPayloads=true} must call
   * {@link #initializeRecoverer(long)} after receiving the initial object
   * contents.
   *
   */
  public interface RemovableHandle extends Remover {

    /**
     * Sets the next expected version of the object.
     * 
     * <p>
     * This is required to bootstrap the ability to recover missing payloads or
     * squelched invalidations.
     * 
     * <p>
     * This can be called multiple times to tell the tango recoverer that your
     * model is now at a new version.
     */
    void initializeRecoverer(long nextExpectedVersion);
  }

  /**
   * A listener for Tango object invalidation.
   */
  public interface Listener {
    
    /**
     * A handle to notify the caller of {@link Listener#onInvalidated} that
     * this invalidation will be processed asychronously. This means the caller will not call again
     * until the current invalidation has finished being processed.
     */
    public interface AsyncProcessingHandle {
      /**
       * Notifies the caller that the invalidation will be processed asychronously.
       *
       * <p>
       * {@link #finishedAsyncProcessing()} must be called when the invalidation has been processed.
       */
      void startedAsyncProcessing();
      
      /**
       * Notifies the caller of {@link Listener#onInvalidated} that the
       * invalidation has finished being processed.
       */
      void finishedAsyncProcessing();
    }
    
    /**
     * @param payload the invalidation payload, or null if no payload arrived
     * @param asyncProcessingHandle for objects that require payload recovery, this will tell the
     *        recovery stack to allow for async processing of this payload
     */
    void onInvalidated(String objectName, long version, @Nullable String payload,
        AsyncProcessingHandle asyncProcessingHandle);
  }

  /**
   * A listener which handles deserializing a Tango payload into a given DTO
   * type.
   *
   * @param <T> the type of dto.
   */
  public abstract static class DtoListener<T extends ServerToClientDto> implements Listener {
    private final int routingType;

    public DtoListener(int routingType) {      
      this.routingType = routingType;
    }

    @Override
    public void onInvalidated(String objectName, long version, String payload,
        AsyncProcessingHandle asyncProcessingHandle) {
      T dto = null;
      if (payload != null) {
        dto = DtoUtils.parseAsDto(payload, routingType);
      }
      onInvalidatedDto(objectName, version, dto, asyncProcessingHandle);
    }

    /**
     * @see Listener#onInvalidated
     */
    public abstract void onInvalidatedDto(String objectName, long version, T dto,
        AsyncProcessingHandle asyncProcessingHandle);
  }

  /**
   * Registers the client to receive invalidations for the given object.
   *
   * <p>
   * If the client requires recovering from missing payloads ({@code
   * recoverMissingPayloads} is true), the suggested usage pattern is:
   * <ul>
   * <li>Register for Tango invalidations using this method</li>
   * <li>Out-of-band (e.g. via XHR) fetch the initial contents of the object
   * </li>
   * <li>Call {@link RemovableHandle#initializeRecoverer} with the fetched
   * object's version</li>
   * </ul>
   */
  public RemovableHandle register(InvalidationObjectId<?> objectId, Listener eventListener);

  public void unregister(InvalidationObjectId<?> objectId);
}
