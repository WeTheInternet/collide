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

import static com.google.collide.client.history.MockPlaces.CHILD_A;
import static com.google.collide.client.history.MockPlaces.CHILD_A_NAV;
import static com.google.collide.client.history.MockPlaces.GRANDCHILD_A;
import static com.google.collide.client.history.MockPlaces.GRANDCHILD_A_NAV;
import static com.google.collide.client.history.MockPlaces.PARENT_A;
import static com.google.collide.client.history.MockPlaces.PARENT_A_NAV;
import static com.google.collide.client.history.MockPlaces.PARENT_B;
import static com.google.collide.client.history.MockPlaces.PARENT_B_NAV;

import com.google.collide.client.history.MockPlaces.MockChildPlaceA;
import com.google.collide.client.history.MockPlaces.MockParentPlaceA;
import com.google.collide.client.history.MockPlaces.MockParentPlaceB;
import com.google.collide.clientlibs.navigation.NavigationToken;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests our implementation of Hierarchical history as implemented in
 * {@link Place}.
 */
public class PlaceTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();

    // Reset the Root scope, since that isn't done automatically.
    RootPlace.PLACE.resetScope();

    // Reset the handler invocation counts.
    MockPlaces.resetCounts();

    RootPlace.PLACE.registerChildHandler(PARENT_A, PARENT_A_NAV);
    RootPlace.PLACE.registerChildHandler(PARENT_B, PARENT_B_NAV);
  }

  /**
   * Tests that we can dispatch a line of {@link NavigationToken}s parsed from the
   * History string
   */
  public void testDispatchHistory() {
    // Build up a History String of Root/ParentA/ChildA, that looks like
    // "/h/mockparenta=(someString=foo)/mockchilda=(someString=foo,someNumber=42)/"
    String historyString =
        HistoryUtils.createHistoryString(JsonCollections.<NavigationToken>createArray(
            PARENT_A.createNavigationEvent("foo"), CHILD_A.createNavigationEvent("foo", 42),
            GRANDCHILD_A.createNavigationEvent()));
    JsonArray<NavigationToken> historyPieces = HistoryUtils.parseHistoryString(historyString);

    // Bring the state of the app in line with the History String.
    RootPlace.PLACE.dispatchHistory(historyPieces);

    // Examine the state of the world.
    assertTrue(RootPlace.PLACE.isActive());
    assertTrue(PARENT_A.isActive());
    assertFalse(PARENT_B.isActive());
    assertEquals(1, PARENT_A_NAV.getEnterCount());
    assertEquals(0, PARENT_A_NAV.getReEnterCount());
    assertEquals(0, PARENT_B_NAV.getEnterCount());
    assertEquals(0, PARENT_B_NAV.getReEnterCount());
    assertTrue(CHILD_A.isActive());
    assertEquals(1, CHILD_A_NAV.getEnterCount());
    assertEquals(0, CHILD_A_NAV.getReEnterCount());
    assertTrue(GRANDCHILD_A.isActive());
    assertEquals(1, GRANDCHILD_A_NAV.getEnterCount());
    assertEquals(0, GRANDCHILD_A_NAV.getReEnterCount());
  }

  public void testMultipleHandlers() {
    MockParentPlaceA.NavigationHandler nav2 = new MockParentPlaceA.NavigationHandler();
    RootPlace.PLACE.registerChildHandler(PARENT_A, nav2);
    MockParentPlaceA.NavigationEvent navEvent = PARENT_A.createNavigationEvent("asdf");
    RootPlace.PLACE.fireChildPlaceNavigation(navEvent);

    // Verify both handlers got called
    assertTrue(PARENT_A.isActive());
    assertEquals(1, PARENT_A_NAV.getEnterCount());
    assertEquals(0, PARENT_A_NAV.getReEnterCount());
    assertEquals(0, PARENT_A_NAV.getCleanupCount());
    assertEquals(1, nav2.getEnterCount());

    // Navigate away
    MockParentPlaceB.NavigationEvent parentBNavEvent = PARENT_B.createNavigationEvent();
    RootPlace.PLACE.fireChildPlaceNavigation(parentBNavEvent);

    // Verify that both cleanup handlers were called
    assertFalse(PARENT_A.isActive());
    assertEquals(1, PARENT_A_NAV.getCleanupCount());
    assertEquals(1, nav2.getCleanupCount());
  }

  /**
   * Tests navigating to a child Place.
   */
  public void testNavigateToChildPlace() {
    String someString = "foo";
    int someNumber = 42;

    // Lets trigger a navigation to Parent B.
    MockParentPlaceB.NavigationEvent parentBNavEvent = PARENT_B.createNavigationEvent();
    RootPlace.PLACE.fireChildPlaceNavigation(parentBNavEvent);

    // Verify we went to Parent B from Root.
    assertTrue(PARENT_B.isActive());
    assertEquals(1, PARENT_B_NAV.getEnterCount());
    assertEquals(0, PARENT_B_NAV.getReEnterCount());
    assertEquals(0, PARENT_B_NAV.getCleanupCount());

    // Try to get to Child A from Parent B. We know that this should not be
    // possible.
    MockChildPlaceA.NavigationEvent childNavEvent =
        CHILD_A.createNavigationEvent(someString, someNumber);
    PARENT_B.fireChildPlaceNavigation(childNavEvent);

    // We should still have not done the navigation.
    assertFalse(CHILD_A.isActive());
    assertEquals(0, CHILD_A_NAV.getEnterCount());
    assertEquals(0, CHILD_A_NAV.getReEnterCount());

    // Lets trigger a navigation to Parent A.
    MockParentPlaceA.NavigationEvent parentANavEvent = PARENT_A.createNavigationEvent(someString);
    RootPlace.PLACE.fireChildPlaceNavigation(parentANavEvent);

    // Verify we are no longer in B and that we did not change any of the
    // handler invocation counts
    assertFalse(PARENT_B.isActive());
    assertEquals(1, PARENT_B_NAV.getCleanupCount());
    assertFalse(CHILD_A.isActive());
    assertEquals(1, PARENT_B_NAV.getEnterCount());
    assertEquals(0, CHILD_A_NAV.getEnterCount());
    assertEquals(0, CHILD_A_NAV.getReEnterCount());

    // Verify that we are in Parent A.
    assertTrue(PARENT_A.isActive());
    assertEquals(1, PARENT_A_NAV.getEnterCount());

    // Try to get to Child A from Parent A.
    PARENT_A.fireChildPlaceNavigation(childNavEvent);

    // We should now be in A.
    assertTrue(CHILD_A.isActive());
    assertEquals(1, CHILD_A_NAV.getEnterCount());
    assertEquals(0, CHILD_A_NAV.getReEnterCount());

    // Verify the other Places did not get weird.
    assertTrue(RootPlace.PLACE.isActive());
    assertTrue(PARENT_A.isActive());
    assertFalse(PARENT_B.isActive());
    assertEquals(1, PARENT_A_NAV.getEnterCount());
    assertEquals(0, PARENT_A_NAV.getReEnterCount());
    assertEquals(1, PARENT_B_NAV.getEnterCount());
    assertEquals(0, PARENT_B_NAV.getReEnterCount());
    assertEquals(1, PARENT_B_NAV.getCleanupCount());
  }

  /**
   * Tests navigating to a child Place when on a Parent place is not active.
   */
  public void testNavigateToChildPlaceNotActive() {

    // Try to get to Child A from Parent A when Parent A is not active.
    MockChildPlaceA.NavigationEvent childNavEvent = CHILD_A.createNavigationEvent("foo", 42);
    PARENT_A.fireChildPlaceNavigation(childNavEvent);

    // We should have not done the navigation.
    assertEquals(0, CHILD_A_NAV.getEnterCount());
    assertEquals(0, CHILD_A_NAV.getReEnterCount());
  }

  public void testNavigateToSamePlace() {
    // Lets trigger a navigation to Parent B.
    MockParentPlaceA.NavigationEvent parentANavEvent = PARENT_A.createNavigationEvent("foo");
    RootPlace.PLACE.fireChildPlaceNavigation(parentANavEvent);

    // Verify we went to Parent B from Root.
    assertTrue(PARENT_A.isActive());
    assertEquals(1, PARENT_A_NAV.getEnterCount());
    assertEquals(0, PARENT_A_NAV.getReEnterCount());
    assertEquals(0, PARENT_A_NAV.getCleanupCount());

    RootPlace.PLACE.fireChildPlaceNavigation(PARENT_A.createNavigationEvent("foo"));

    // Verify that we didn't double navigate, but rather did a re-entrant
    // dispatch that did not deliver any new state.
    assertTrue(PARENT_A.isActive());
    assertEquals(1, PARENT_A_NAV.getEnterCount());
    assertEquals(1, PARENT_A_NAV.getReEnterCount());
    assertFalse(PARENT_A_NAV.hadNewState());
    assertEquals(0, PARENT_A_NAV.getCleanupCount());

    // Now try again with different params to the event
    RootPlace.PLACE.fireChildPlaceNavigation(PARENT_A.createNavigationEvent("bar"));

    // Verify that we re-entrantly dispatched and had some new state.
    assertTrue(PARENT_A.isActive());
    assertEquals(1, PARENT_A_NAV.getEnterCount());
    assertEquals(2, PARENT_A_NAV.getReEnterCount());
    assertTrue(PARENT_A_NAV.hadNewState());
    assertEquals(0, PARENT_A_NAV.getCleanupCount());
  }
}
