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

import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

/**
 * Mock {@link Place}s used for testing.
 *
 */
public class MockPlaces {

  /**
   * A Mock Child.
   *
   * Lets call him "Child A".
   */
  public static class MockChildPlaceA extends MockPlace {

    /**
     * The {@link PlaceNavigationEvent} for Child A.
     */
    public class NavigationEvent extends PlaceNavigationEvent<MockChildPlaceA> {

      int someNumber;
      String someString;

      protected NavigationEvent() {
        super(MockChildPlaceA.this);
      }

      @Override
      public JsonStringMap<String> getBookmarkableState() {
        JsoStringMap<String> keyValues = JsoStringMap.create();
        keyValues.put(SOME_STRING_KEY, someString);
        keyValues.put(SOME_NUMBER_KEY, someNumber + "");
        return keyValues;
      }
    }

    /**
     * The {@link PlaceNavigationHandler} for Child A.
     */
    public static class NavigationHandler extends MockPlaceNavigationHandler<
        MockChildPlaceA.NavigationEvent> {

      @Override
      protected void enterPlace(MockChildPlaceA.NavigationEvent navigationEvent) {
        super.enterPlace(navigationEvent);

        navigationEvent.getPlace().registerChildHandler(GRANDCHILD_A, GRANDCHILD_A_NAV);
      }
    }

    protected MockChildPlaceA() {
      super("MockChildPlaceA");
    }

    @Override
    public MockChildPlaceA.NavigationEvent createNavigationEvent(
        JsonStringMap<String> decodedState) {

      // If this decoded string is not a valid integer, this will throw. Which
      // is fine. Let it break the test.
      return createNavigationEvent(
          decodedState.get(SOME_STRING_KEY), Integer.parseInt(decodedState.get(SOME_NUMBER_KEY)));
    }

    public MockChildPlaceA.NavigationEvent createNavigationEvent(
        String someString, int someNumber) {
      MockChildPlaceA.NavigationEvent navEvent = new MockChildPlaceA.NavigationEvent();
      navEvent.someString = someString;
      navEvent.someNumber = someNumber;
      return navEvent;
    }
  }

  /**
   * A Mock Child with no bookmarkable state.
   *
   * Lets call him "GrandChild B".
   */
  public static class MockGrandChildPlaceA extends MockPlace {

    /**
     * The {@link PlaceNavigationEvent} for GrandChild B.
     */
    public class NavigationEvent extends PlaceNavigationEvent<MockGrandChildPlaceA> {

      protected NavigationEvent() {
        super(MockGrandChildPlaceA.this);
      }

      @Override
      public JsonStringMap<String> getBookmarkableState() {
        return JsoStringMap.create();
      }
    }

    /**
     * The {@link PlaceNavigationHandler} for Child B.
     */
    public static class NavigationHandler extends MockPlaceNavigationHandler<
        MockGrandChildPlaceA.NavigationEvent> {
    }

    protected MockGrandChildPlaceA() {
      super("MockGrandChildPlaceA");
    }

    @Override
    public MockGrandChildPlaceA.NavigationEvent createNavigationEvent(
        JsonStringMap<String> decodedState) {
      return new MockGrandChildPlaceA.NavigationEvent();
    }
  }

  /**
   * A Mock Parent.
   *
   * Lets call him "A".
   */
  public static class MockParentPlaceA extends MockPlace {
    /**
     * The {@link PlaceNavigationEvent} for A.
     */
    public class NavigationEvent extends PlaceNavigationEvent<MockParentPlaceA> {

      String someString;

      protected NavigationEvent() {
        super(MockParentPlaceA.this);
      }

      @Override
      public JsonStringMap<String> getBookmarkableState() {
        JsoStringMap<String> keyValues = JsoStringMap.create();
        keyValues.put(SOME_STRING_KEY, someString);
        return keyValues;
      }
    }

    /**
     * The {@link PlaceNavigationHandler} for A.
     */
    public static class NavigationHandler extends MockPlaceNavigationHandler<
        MockParentPlaceA.NavigationEvent> {

      @Override
      protected void enterPlace(MockParentPlaceA.NavigationEvent navigationEvent) {
        super.enterPlace(navigationEvent);

        // Add a Child to ParentA.
        navigationEvent.getPlace().registerChildHandler(MockPlaces.CHILD_A, CHILD_A_NAV);
      }
    }

    protected MockParentPlaceA() {
      super("MockParentPlaceA");
    }

    @Override
    public MockParentPlaceA.NavigationEvent createNavigationEvent(
        JsonStringMap<String> decodedState) {
      return createNavigationEvent(decodedState.get(SOME_STRING_KEY));
    }

    public MockParentPlaceA.NavigationEvent createNavigationEvent(String someString) {
      MockParentPlaceA.NavigationEvent navEvent = new MockParentPlaceA.NavigationEvent();
      navEvent.someString = someString;
      return navEvent;
    }
  }

  /**
   * A Mock Parent.
   *
   * Lets call him "B".
   */
  public static class MockParentPlaceB extends MockPlace {
    /**
     * The {@link PlaceNavigationEvent} for B.
     */
    public class NavigationEvent extends PlaceNavigationEvent<MockParentPlaceB> {

      protected NavigationEvent() {
        super(MockParentPlaceB.this);
      }

      @Override
      public JsonStringMap<String> getBookmarkableState() {
        // We keep no state.
        return JsoStringMap.create();
      }
    }

    /**
     * The {@link PlaceNavigationHandler} for B.
     */
    public static class NavigationHandler extends MockPlaceNavigationHandler<
        MockParentPlaceB.NavigationEvent> {
    }

    protected MockParentPlaceB() {
      super("MockParentPlaceB");
    }

    @Override
    public MockParentPlaceB.NavigationEvent createNavigationEvent() {
      return new MockParentPlaceB.NavigationEvent();
    }

    @Override
    public MockParentPlaceB.NavigationEvent createNavigationEvent(
        JsonStringMap<String> decodedState) {
      return createNavigationEvent();
    }
  }

  /**
   * Base class for all mock Places.
   */
  public abstract static class MockPlace extends Place {
    protected MockPlace(String placeName) {
      super(placeName);
    }
  }

  /**
   * Base class for mock PlaceNavigationHandlers. Lets us track invocation
   * counts.
   */
  abstract static class MockPlaceNavigationHandler<E extends PlaceNavigationEvent<?>>
      extends PlaceNavigationHandler<E> {
    private int enterCount;
    private int cleanupCount;
    private int reEnterCount;
    private boolean hadNewState;

    MockPlaceNavigationHandler() {
    }

    @Override
    protected void cleanup() {
      cleanupCount++;
    }

    @Override
    protected void enterPlace(E navigationEvent) {
      enterCount++;
    }

    @Override
    protected void reEnterPlace(E navigationEvent, boolean newState) {
      reEnterCount++;
      hadNewState = newState;
    }

    public boolean hadNewState() {
      return hadNewState;
    }

    public int getEnterCount() {
      return enterCount;
    }

    public int getCleanupCount() {
      return cleanupCount;
    }

    public int getReEnterCount() {
      return reEnterCount;
    }

    public void resetCounts() {
      enterCount = 0;
      cleanupCount = 0;
      reEnterCount = 0;
    }
  }

  public static final MockChildPlaceA CHILD_A = new MockChildPlaceA();
  public static final MockChildPlaceA.NavigationHandler CHILD_A_NAV =
      new MockChildPlaceA.NavigationHandler();
  public static final MockGrandChildPlaceA GRANDCHILD_A = new MockGrandChildPlaceA();
  public static final MockGrandChildPlaceA.NavigationHandler GRANDCHILD_A_NAV =
      new MockGrandChildPlaceA.NavigationHandler();
  public static final MockParentPlaceA PARENT_A = new MockParentPlaceA();
  public static final MockParentPlaceA.NavigationHandler PARENT_A_NAV =
      new MockParentPlaceA.NavigationHandler();
  public static final MockParentPlaceB PARENT_B = new MockParentPlaceB();
  public static final MockParentPlaceB.NavigationHandler PARENT_B_NAV =
      new MockParentPlaceB.NavigationHandler();


  public static final String SOME_NUMBER_KEY = "someNumber";
  public static final String SOME_STRING_KEY = "someString";

  public static void resetCounts() {
    CHILD_A_NAV.resetCounts();
    CHILD_A.setIsActive(false, null);

    GRANDCHILD_A_NAV.resetCounts();
    GRANDCHILD_A.setIsActive(false, null);

    PARENT_A_NAV.resetCounts();
    PARENT_A.setIsActive(false, null);

    PARENT_B_NAV.resetCounts();
    PARENT_B.setIsActive(false, null);
  }

  private MockPlaces() {
  }
}
