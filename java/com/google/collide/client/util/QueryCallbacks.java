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

package com.google.collide.client.util;

import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.common.base.Preconditions;

/**
 * Class for common implementations of a {@link QueryCallback}.
 */
public class QueryCallbacks {

  /**
   * A callback used when no action will be taken on success or failure (most useful to ensure the
   * cache is populated for speed).
   */
  public static class NoOpCallback<E> implements QueryCallback<E> {
    @Override
    public void onFail(FailureReason reason) {}

    @Override
    public void onQuerySuccess(E result) {}
  }

  /**
   * Generic class which logs an optional warning when a query failure occurs.
   */
  public static abstract class SimpleCallback<E> implements QueryCallback<E> {
    private final String failWarningMessage;

    public SimpleCallback(String failWarningMessage) {
      Preconditions.checkNotNull(failWarningMessage, "Warning message is required");
      this.failWarningMessage = failWarningMessage;
    }

    @Override
    public void onFail(FailureReason reason) {
      if (!failWarningMessage.isEmpty()) {
        Log.warn(getClass(), failWarningMessage);
      }
    }
  }

  /**
   * A call which automatically delegates to another {@link QueryCallback} when a failure occurs.
   */
  public static abstract class DelegateFailureCallback<REQ, RESP> implements QueryCallback<REQ> {
    private final QueryCallback<RESP> callback;

    public DelegateFailureCallback(QueryCallback<RESP> callback) {
      this.callback = callback;
    }

    /** Dispatches a successful result */
    protected void dispatch(RESP result) {
      if (callback != null) {
        callback.onQuerySuccess(result);
      }
    }

    @Override
    public void onFail(FailureReason reason) {
      if (callback != null) {
        callback.onFail(reason);
      }
    }
  }

  /**
   * Similar to {@link SimpleCallback} but will funnel execution through the given {@link Executor}.
   * Implements should override {@link ExecutorDelegatingCallback#onExecute(boolean, Object)}.
   */
  public static abstract class ExecutorDelegatingCallback<E> extends SimpleCallback<E>
      implements Runnable {

    private final Executor executor;
    private E result;
    private boolean failed;

    public ExecutorDelegatingCallback(Executor executor, String failWarningMessage) {
      super(failWarningMessage);
      this.executor = executor;
    }

    @Override
    public void onQuerySuccess(E result) {
      this.result = result;
      failed = false;
      executor.execute(this);
    }

    @Override
    public void onFail(FailureReason reason) {
      super.onFail(reason);
      failed = true;
      executor.execute(this);
    }

    @Override
    public void run() {
      onExecute(failed, result);
    }

    public abstract void onExecute(boolean failed, E result);
  }
}
