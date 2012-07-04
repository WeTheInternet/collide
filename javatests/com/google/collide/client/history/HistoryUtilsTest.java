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

package com.google.collide.client.history;

import static com.google.collide.client.history.MockPlaces.PARENT_A;

import com.google.collide.client.history.HistoryUtils.SetHistoryListener;
import com.google.collide.client.history.HistoryUtils.ValueChangeListener;
import com.google.collide.json.client.JsoArray;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * These test cases exercise the static methods in {@link HistoryUtils} for
 * encoding and decoding History information.
 */
public class HistoryUtilsTest extends GWTTestCase {

  // A test times out in 2 seconds.
  static final int WAIT_TIMEOUT_MILLIS = 2000;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  private static class MockUrlListener implements SetHistoryListener, ValueChangeListener {
    int callCount = 0;

    @Override
    public void onHistorySet(String historyString) {
      callCount++;
    }

    @Override
    public void onValueChanged(String historyString) {
      callCount++;
    }
  }

  /**
   * Tests that we get notified of changes to the History token that we made.
   */
  public void testSetHistoryListener() {
    boolean called = false;
    final MockUrlListener urlListener = new MockUrlListener();
    HistoryUtils.addSetHistoryListener(urlListener);

    // It should be called once immediately to snapshot the current URL.
    assertEquals(1, urlListener.callCount);

    JsoArray<PlaceNavigationEvent<?>> snapshot = JsoArray.create();
    snapshot.add(PARENT_A.createNavigationEvent("foo"));

    // Change history. It should inform the UrlListener.
    HistoryUtils.createHistoryEntry(snapshot);
    assertEquals(2, urlListener.callCount);

    // Make a change outside of HistoryUtils. It should NOT fire the listener.

    // TODO: Forge uses FF3.5, which doesnt allow the following to
    // pass. Once forge gets a recent version of firefox, we should turn this
    // test
    // code back on.

    // Browser.getWindow().getLocation().setHash("setexternally");
    //
    // Browser.getWindow().setTimeout(new TimerCallback() {
    // @Override
    // public void fire() {
    // assertEquals(2, urlListener.callCount);
    // finishTest();
    // }
    // }, 100);
    //
    // delayTestFinish(WAIT_TIMEOUT_MILLIS);
  }

  /**
   * Tests that we get notified of changes to the History token that were made
   * externally (like from the back and forward button).
   */
  public void testValueChangeListener() {
    final MockUrlListener urlListener = new MockUrlListener();
    HistoryUtils.addValueChangeListener(urlListener);
    assertEquals(0, urlListener.callCount);

    // TODO: Forge uses FF3.5, which doesnt allow the following to
    // pass. Once forge gets a recent version of firefox, we should turn this
    // test
    // code back on.

    // Browser.getWindow().getLocation().setHash("setexternallyAgain");
    //
    // Browser.getWindow().setTimeout(new TimerCallback() {
    // @Override
    // public void fire() {
    // // It should inform the UrlListener.
    // assertEquals(1, urlListener.callCount);
    //
    // JsoArray<PlaceNavigationEvent<?>> snapshot = JsoArray.create();
    // snapshot.add(PARENT_A.createNavigationEvent("foo"));
    //
    // // Change history. It should NOT inform the UrlListener.
    // HistoryUtils.createHistoryEntry(snapshot);
    // assertEquals(1, urlListener.callCount);
    // finishTest();
    // }
    // }, 100);
    //
    // delayTestFinish(WAIT_TIMEOUT_MILLIS);
  }
}
