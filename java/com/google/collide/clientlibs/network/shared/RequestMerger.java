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

package com.google.collide.clientlibs.network.shared;

import com.google.collide.client.util.QueryCallback;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Joiner;


/**
 * An object which prevents multiple identical requests from being sent across
 * the wire to the frontend. When a new requests matches an existing request its
 * callback is queued. After the initial request returns all callbacks are
 * notified of success or failure. This object should only be used for requests
 * which cause no server-side side effects (i.e. creating project).
 *
 * @param <R> The type sent to the query callback on success or failure
 */
public class RequestMerger<R> {

  /**
   * Static factory to create a RequestMerger
   *
   * @param <R> Type sent to the query callback
   */
  public static <R> RequestMerger<R> create() {
    return new RequestMerger<R>();
  }

  /**
   * Convenience constant for merging all requests.
   */
  public static final String ALL_REQUESTS = "ALL";

  private JsonStringMap<JsonArray<QueryCallback<R>>> requestMap = JsonCollections.createMap();

  /**
   * Adds a new request to the map, returning true if the exact requestHash is a
   * currently outstanding request and should not be sent over the wire.
   *
   * @param requestHash Unique hash identifying this request's parameters.
   * @param callback Callback to call on success/failure.
   *
   * @return true to indicate that this is a new request, false to indicate that
   *         it is a duplicate of an existing request and has been queued.
   */
  public boolean handleNewRequest(String requestHash, QueryCallback<R> callback) {
    JsonArray<QueryCallback<R>> callbacks = requestMap.get(requestHash);
    boolean existed = callbacks != null;
    if (callbacks == null) {
      callbacks = JsonCollections.createArray();
      requestMap.put(requestHash, callbacks);
    }
    callbacks.add(callback);
    return !existed;
  }

  /**
   * Call to notify all callbacks waiting on a request of failure.
   */
  public void handleQueryFailed(String requestHash, FailureReason reason) {
    JsonArray<QueryCallback<R>> callbacks = requestMap.remove(requestHash);
    if (callbacks == null) {
      return;
    }
    for (int i = 0; i < callbacks.size(); i++) {
      callbacks.get(i).onFail(reason);
    }
  }

  /**
   * Handles notifying all callbacks waiting on a request of success.
   */
  public void handleQuerySuccess(String requestHash, R result) {
    JsonArray<QueryCallback<R>> callbacks = requestMap.remove(requestHash);
    if (callbacks == null) {
      return;
    }
    for (int i = 0; i < callbacks.size(); i++) {
      callbacks.get(i).onQuerySuccess(result);
    }
  }

  /**
   * Clears any callbacks waiting on the given request.
   */
  public void clearCallbacksWaitingForRequest(String requestHash) {
    requestMap.remove(requestHash);
  }

  /**
   * @return the number of callbacks merged and waiting on a given request.
   */
  public boolean hasCallbacksWaitingForRequest(String requestHash) {
    JsonArray<QueryCallback<R>> callbacks = requestMap.get(requestHash);
    return callbacks != null && callbacks.size() > 0;
  }

  /**
   * Creates a '|' separated hash string from a list of string values. Calls
   * {@link Object#toString()} on each value passed in to make the hash.
   */
  public static String createHash(Object... values) {
    return Joiner.on("|").join(values);
  }
}
