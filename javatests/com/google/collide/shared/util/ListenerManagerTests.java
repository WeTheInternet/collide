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

import com.google.collide.shared.util.ListenerRegistrar.Remover;

import junit.framework.TestCase;

/**
 * Basic tests for listener manager.
 */
public class ListenerManagerTests extends TestCase {

  public class TestCallback {
    public int count;

    public void dispatch() {
      count += 1;
    }
  }

  public class Dispatcher implements ListenerManager.Dispatcher<TestCallback> {
    @Override
    public void dispatch(TestCallback listener) {
      listener.dispatch();
    }
  }

  private ListenerManager<TestCallback> listenerManager;

  @Override
  public void setUp() {
    listenerManager = ListenerManager.create();
  }

  public void testCallbackCalled() {
    TestCallback callback = new TestCallback();
    listenerManager.add(callback);
    // duplicate add should be prevented
    listenerManager.add(callback);

    listenerManager.dispatch(new Dispatcher());
    assertEquals(1, callback.count);

    listenerManager.remove(callback);
    listenerManager.dispatch(new Dispatcher());
    // shouldn't be called agains ince it was removed
    assertEquals(1, callback.count);
  }

  public void testMultipleCallbacksCalled() {
    TestCallback callback1 = new TestCallback();
    TestCallback callback2 = new TestCallback();
    listenerManager.add(callback1);
    listenerManager.add(callback2);

    listenerManager.dispatch(new Dispatcher());
    assertEquals(1, callback1.count);
    assertEquals(1, callback2.count);
  }

  public void testAddsQueuedDuringDispatch() {
    final TestCallback toAdd = new TestCallback();
    TestCallback callback = new TestCallback() {
      @Override
      public void dispatch() {
        super.dispatch();
        listenerManager.add(toAdd);
      }
    };

    listenerManager.add(callback);
    listenerManager.dispatch(new Dispatcher());

    assertEquals(0, toAdd.count);
    assertEquals(1, callback.count);
    listenerManager.dispatch(new Dispatcher());

    assertEquals(1, toAdd.count);
    assertEquals(2, callback.count);
  }

  public void testRemovesQueuedDuringDispatch() {
    final TestCallback toRemove = new TestCallback();
    TestCallback callback = new TestCallback() {
      @Override
      public void dispatch() {
        super.dispatch();
        listenerManager.remove(toRemove);
      }
    };

    listenerManager.add(callback);
    listenerManager.add(toRemove);
    listenerManager.dispatch(new Dispatcher());

    assertEquals(1, toRemove.count);
    assertEquals(1, callback.count);
    listenerManager.dispatch(new Dispatcher());

    assertEquals(1, toRemove.count);
    assertEquals(2, callback.count);
  }

  public void testRemover() {
    final TestCallback callback = new TestCallback();

    Remover remover = listenerManager.add(callback);
    listenerManager.dispatch(new Dispatcher());
    remover.remove();
    listenerManager.dispatch(new Dispatcher());
    assertEquals(1, callback.count);
  }

  public void testRemoverManager() {
    final ListenerRegistrar.RemoverManager manager = new ListenerRegistrar.RemoverManager();
    final TestCallback callback1 = new TestCallback();
    final TestCallback callback2 = new TestCallback();
    final TestCallback callback3 = new TestCallback();

    manager.track(listenerManager.add(callback1));
    manager.track(listenerManager.add(callback2));
    manager.track(listenerManager.add(callback3));

    manager.remove();
    listenerManager.dispatch(new Dispatcher());
    assertEquals(0, callback1.count);
    assertEquals(0, callback2.count);
    assertEquals(0, callback3.count);
  }

  public void testRemoverManagerDoesNotFailWhenNoRemovers() {
    final ListenerRegistrar.RemoverManager manager = new ListenerRegistrar.RemoverManager();
    manager.remove();
  }
}
