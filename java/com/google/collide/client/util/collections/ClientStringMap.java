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

package com.google.collide.client.util.collections;

import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

/**
 * Implementation with "__proto__" key workaround.
 *
 * @param <T> type of stored values.
 */
public final class ClientStringMap<T> implements JsonStringMap<T> {

  private static final String PROTO_KEY = "__proto__";

  private final JsoStringMap<T> delegate = JsoStringMap.create();
  private boolean protoFlag;
  private T protoValue;

  /**
   * Convenience factory method.
   */
  public static <T> ClientStringMap<T> create() {
    return new ClientStringMap<T>();
  }

  @Override
  public T get(String key) {
    if (PROTO_KEY.equals(key)) {
      return protoValue;
    }
    return delegate.get(key);
  }

  @Override
  public JsoArray<String> getKeys() {
    JsoArray<String> result = delegate.getKeys();
    if (protoFlag) {
      result.add(PROTO_KEY);
    }
    return result;
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty() && !protoFlag;
  }

  @Override
  public void iterate(IterationCallback<T> tIterationCallback) {
    delegate.iterate(tIterationCallback);
    if (protoFlag) {
      tIterationCallback.onIteration(PROTO_KEY, protoValue);
    }
  }

  @Override
  public void put(String key, T value) {
    if (PROTO_KEY.equals(key)) {
      protoValue = value;
      protoFlag = true;
    } else {
      delegate.put(key, value);
    }
  }

  @Override
  public void putAll(JsonStringMap<T> otherMap) {
    otherMap.iterate(new IterationCallback<T>() {
      @Override
      public void onIteration(String key, T value) {
        put(key, value);
      }
    });
  }

  @Override
  public T remove(String key) {
    if (PROTO_KEY.equals(key)) {
      T result = protoValue;
      protoValue = null;
      protoFlag = false;
      return result;
    } else {
      return delegate.remove(key);
    }
  }

  @Override
  public boolean containsKey(String key) {
    if (PROTO_KEY.equals(key)) {
      return protoFlag;
    }
    return delegate.containsKey(key);
  }

  @Override
  public int size() {
    int result = delegate.size();
    if (protoFlag) {
      result++;
    }
    return result;
  }
}
