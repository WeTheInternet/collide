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

package com.google.collide.client.code.debugging;

import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectId;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;

/**
 * A cache of {@link RemoteObjectNode} objects.
 *
 */
class RemoteObjectNodeCache {
  private final JsonStringMap<JsonArray<RemoteObjectNode>> cache = JsonCollections.createMap();

  /**
   * Convenience shorthand over the generic {@link JsonStringMap.IterationCallback}.
   */
  public interface IterationCallback extends JsonStringMap.IterationCallback<
      JsonArray<RemoteObjectNode>> {
  }

  public boolean contains(RemoteObjectNode node) {
    String cacheKey = getCacheKey(node);
    if (cacheKey == null) {
      return false;
    }

    JsonArray<RemoteObjectNode> array = cache.get(cacheKey);
    return array != null && array.contains(node);
  }

  public JsonArray<RemoteObjectNode> get(RemoteObjectId objectId) {
    String cacheKey = getCacheKey(objectId);
    return cacheKey == null ? null : cache.get(cacheKey);
  }

  public void put(RemoteObjectNode node) {
    String cacheKey = getCacheKey(node);
    if (cacheKey == null) {
      return;
    }

    JsonArray<RemoteObjectNode> array = cache.get(cacheKey);
    if (array == null) {
      array = JsonCollections.createArray();
      cache.put(cacheKey, array);
    }
    array.add(node);
  }

  public void remove(RemoteObjectNode node) {
    String cacheKey = getCacheKey(node);
    if (cacheKey == null) {
      return;
    }

    JsonArray<RemoteObjectNode> array = cache.get(cacheKey);
    if (array != null) {
      array.remove(node);
      if (array.isEmpty()) {
        cache.remove(cacheKey);
      }
    }
  }

  public void iterate(IterationCallback callback) {
    cache.iterate(callback);
  }

  private static String getCacheKey(RemoteObjectNode node) {
    if (node == null || node.getRemoteObject() == null) {
      return null;
    }
    return getCacheKey(node.getRemoteObject().getObjectId());
  }

  private static String getCacheKey(RemoteObjectId objectId) {
    return objectId == null ? null : objectId.toString();
  }
}
