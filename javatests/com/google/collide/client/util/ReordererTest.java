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

import com.google.collide.shared.util.Reorderer;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/*
 * The reason these are in the client is because Reorderer used to be client-only, but was moved
 * to shared code, but the tests weren't migrated to be pure java.
 */
/**
 * Tests for {@link Reorderer}.
 */
public class ReordererTest extends GWTTestCase {

  private static class ItemSink implements Reorderer.ItemSink<Integer> {
    int lastVersion;

    ItemSink(int firstExpectedVersion) {
      this.lastVersion = firstExpectedVersion - 1;
    }
    
    @Override
    public void onItem(Integer item, int version) {
      assertEquals(++lastVersion, version);
      assertEquals(item.intValue(), version);
    }
  }

  private class RequiredTimeoutCallback implements Reorderer.TimeoutCallback {
    private int requiredLastVersionDispatched;

    RequiredTimeoutCallback(int requiredLastVersionDispatched) {
      this.requiredLastVersionDispatched = requiredLastVersionDispatched;
    }

    @Override
    public void onTimeout(int lastVersionDispatched) {
      assertEquals(requiredLastVersionDispatched, lastVersionDispatched);
      finishTest();
    }
  }

  private static final Reorderer.TimeoutCallback FAIL_TIMEOUT_CALLBACK =
      new Reorderer.TimeoutCallback() {
          @Override
        public void onTimeout(int lastVersionDispatched) {
          fail("Timeout should not have been called");
        }
      };

  Reorderer<Integer> reorderer;
  ItemSink sink;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.util.UtilTestModule";
  }

  public void testInOrder() {
    {
      ItemSink sink = new ItemSink(1);
      Reorderer<Integer> reorderer =
          Reorderer.create(1, sink, 1, FAIL_TIMEOUT_CALLBACK, ClientTimer.FACTORY);
      reorderer.acceptItem(1, 1);
      reorderer.acceptItem(2, 2);
      reorderer.acceptItem(3, 3);
      reorderer.acceptItem(4, 4);
      reorderer.acceptItem(5, 5);
      assertEquals(sink.lastVersion, 5);
    }

    {
      ItemSink sink = new ItemSink(3);
      Reorderer<Integer> reorderer =
          Reorderer.create(3, sink, 1, FAIL_TIMEOUT_CALLBACK, ClientTimer.FACTORY);
      reorderer.acceptItem(3, 3);
      reorderer.acceptItem(4, 4);
      reorderer.acceptItem(5, 5);
      assertEquals(sink.lastVersion, 5);
    }
  }

  public void testOutOfOrderNoTimeout() {

    {
      // Immediate out-of-order
      ItemSink sink = new ItemSink(1);
      Reorderer<Integer> reorderer =
          Reorderer.create(1, sink, Integer.MAX_VALUE, FAIL_TIMEOUT_CALLBACK, ClientTimer.FACTORY);
      reorderer.acceptItem(2, 2);
      reorderer.acceptItem(1, 1);
      assertEquals(sink.lastVersion, 2);
    }

    {
      // Two out-of-order
      ItemSink sink = new ItemSink(1);
      Reorderer<Integer> reorderer =
          Reorderer.create(1, sink, Integer.MAX_VALUE, FAIL_TIMEOUT_CALLBACK, ClientTimer.FACTORY);
      reorderer.acceptItem(1, 1);
      reorderer.acceptItem(2, 2);
      reorderer.acceptItem(5, 5);
      reorderer.acceptItem(4, 4);
      reorderer.acceptItem(3, 3);
      assertEquals(sink.lastVersion, 5);
    }

    {
      // Massive out-of-order
      ItemSink sink = new ItemSink(1);
      Reorderer<Integer> reorderer =
          Reorderer.create(1, sink, Integer.MAX_VALUE, FAIL_TIMEOUT_CALLBACK, ClientTimer.FACTORY);
      reorderer.acceptItem(5, 5);
      reorderer.acceptItem(3, 3);
      reorderer.acceptItem(1, 1);
      reorderer.acceptItem(4, 4);
      reorderer.acceptItem(2, 2);
      assertEquals(sink.lastVersion, 5);
    }
  }

  public void testOutOfOrderTimeout() {
    ItemSink sink = new ItemSink(1);
    RequiredTimeoutCallback timeoutCallback = new RequiredTimeoutCallback(3);

    Reorderer<Integer> reorderer =
        Reorderer.create(1, sink, 1, timeoutCallback, ClientTimer.FACTORY);
    reorderer.acceptItem(1, 1);
    reorderer.acceptItem(2, 2);
    reorderer.acceptItem(3, 3);
    reorderer.acceptItem(5, 5);
    assertEquals(sink.lastVersion, 3);
    delayTestFinish(100);
  }
  
  public void testOutOfOrderThenFillInGapThenTimeout() {
    ItemSink sink = new ItemSink(1);
    RequiredTimeoutCallback timeoutCallback = new RequiredTimeoutCallback(4);

    Reorderer<Integer> reorderer =
        Reorderer.create(1, sink, 1, timeoutCallback, ClientTimer.FACTORY);
    reorderer.acceptItem(1, 1);
    reorderer.acceptItem(3, 3);
    reorderer.acceptItem(4, 4);
    reorderer.acceptItem(6, 6);
    reorderer.acceptItem(2, 2);
    assertEquals(sink.lastVersion, 4);
    delayTestFinish(100);
  }
  
  public void testOutOfOrderTimeoutDisabled() {
    ItemSink sink = new ItemSink(1);
    Reorderer<Integer> reorderer =
        Reorderer.create(1, sink, 1, FAIL_TIMEOUT_CALLBACK, ClientTimer.FACTORY);
    reorderer.setTimeoutEnabled(false);
    
    reorderer.acceptItem(1, 1);
    reorderer.acceptItem(2, 2);
    reorderer.acceptItem(3, 3);
    reorderer.acceptItem(5, 5);
    assertEquals(sink.lastVersion, 3);

    ensureThatFailTimeoutWillNotBeCalled();
  }
  
  public void testOutOfOrderTimeoutDisabledAndThenEnabled() {
    ItemSink sink = new ItemSink(1);
    RequiredTimeoutCallback timeoutCallback = new RequiredTimeoutCallback(3);
    
    Reorderer<Integer> reorderer =
        Reorderer.create(1, sink, 1, timeoutCallback, ClientTimer.FACTORY);
    reorderer.setTimeoutEnabled(false);
    
    reorderer.acceptItem(1, 1);
    reorderer.acceptItem(2, 2);
    reorderer.acceptItem(3, 3);
    reorderer.acceptItem(5, 5);
    assertEquals(sink.lastVersion, 3);
    
    reorderer.setTimeoutEnabled(true);
    
    delayTestFinish(100);
  }
  
  public void testSkipToVersion() {
    ItemSink sink = new ItemSink(1);
    Reorderer<Integer> reorderer =
        Reorderer.create(1, sink, 1, FAIL_TIMEOUT_CALLBACK, ClientTimer.FACTORY);
    
    reorderer.acceptItem(1, 1);
    reorderer.acceptItem(2, 2);
    reorderer.acceptItem(3, 3);
    reorderer.acceptItem(5, 5);
    reorderer.acceptItem(6, 6);
    reorderer.acceptItem(8, 8);
    reorderer.acceptItem(7, 7);
    assertEquals(sink.lastVersion, 3);

    /*
     * The timeout callback would normally be called, but our skipToVersion below will cancel it and
     * get us back on track. It will also try to process any queued doc ops including the given
     * version (5) and after.
     */
    
    // Override what the sink should expect next
    sink.lastVersion = 4;
    
    reorderer.skipToVersion(5);
    assertEquals(sink.lastVersion, 8);

    ensureThatFailTimeoutWillNotBeCalled();
  }
  
  public void testQueueUntilSkipToVersionCalled() {
    ItemSink sink = new ItemSink(5);
    Reorderer<Integer> reorderer =
        Reorderer.create(0, sink, 1, FAIL_TIMEOUT_CALLBACK, ClientTimer.FACTORY);
    reorderer.queueUntilSkipToVersionIsCalled();
    
    reorderer.acceptItem(5, 5);
    reorderer.acceptItem(6, 6);
    reorderer.acceptItem(7, 7);
    
    assertNotSame(7, sink.lastVersion);
    
    reorderer.skipToVersion(5);
    
    assertEquals(7, sink.lastVersion);

    ensureThatFailTimeoutWillNotBeCalled();
  }
  
  private void ensureThatFailTimeoutWillNotBeCalled() {
    new Timer() {
      @Override
      public void run() {
        /*
         * If the timeout was still enabled, FAIL_TIMEOUT_CALLBACK would have been queued/will run
         * before this logic does
         */
        finishTest();
      }
    }.schedule(1);
    
    delayTestFinish(100);
  }
}
