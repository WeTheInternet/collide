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

import javax.annotation.Nonnull;

import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.logging.Log;
import com.google.collide.clientlibs.navigation.NavigationToken;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.SimpleEventBus;

/**
 * A tier in Hierarchical History.
 *
 *  We use Places to control application level navigations. Each time you
 * navigate to a Place, a history token is created.
 *
 * Note to self. Holy crazy generics batman!
 */
public abstract class Place {
  private class Scope {
    private PlaceNavigationEvent<?> currentChildPlaceNavigation = null;
    private final SimpleEventBus eventBus = new SimpleEventBus();
    private final JsoStringMap<
        JsoArray<PlaceNavigationHandler<PlaceNavigationEvent<Place>>>> handlers =
        JsoStringMap.create();
    private final JsoStringMap<Place> knownChildPlaces = JsoStringMap.create();
  }

  /**
   * Used to iterate over the state key/value map present in a child
   * {@link PlaceNavigationEvent} to ensure that everything lines up with the
   * specified history token.
   */
  private static class StateMatcher implements IterationCallback<String> {
    JsonStringMap<String> historyState;
    boolean matches;

    StateMatcher(JsonStringMap<String> historyState) {
      this.matches = true;
      this.historyState = historyState;
    }

    @Override
    public void onIteration(String key, String value) {
      matches = matches && (historyState.get(key) != null) && historyState.get(key).equals(value);
    }
  }

  /**
   * If we detect more than 20 {@link Place}s when walking the active set, then
   * we can assume we have a cycle somewhere. This acts as a bound.
   */
  private static final int PLACE_LIMIT = 20;

  private static void cleanupChild(Place parent, Place child) {
    JsoArray<PlaceNavigationHandler<PlaceNavigationEvent<Place>>> handlers =
        parent.scope.handlers.get(child.getName().toLowerCase());

    assert (handlers != null && handlers.size() > 0) : "Child handle disappeared from parent.";

    for (int j = 0, n = handlers.size(); j < n; j++) {
      PlaceNavigationHandler<PlaceNavigationEvent<Place>> handler = handlers.get(j);
      handler.cleanup();
    }
    child.setIsActive(false, null);
  }

  private boolean createHistoryToken = true;
  private Place currentParentPlace;
  private boolean isActive = false;
  private boolean isStrict = true;
  private final String name;
  private Scope scope = new Scope();

  protected Place(String placeName) {
    this.name = placeName;
  }

  /**
   * @return The Place that is the parent of this Place (earlier on the active
   *         Place stack). Will return {@code null} if this Place is not active.
   */
  public Place getParentPlace() {
    return currentParentPlace;
  }

  /**
   * @return The current {@link PlaceNavigationEvent} that is the direct child
   *         of this Place.
   */
  public PlaceNavigationEvent<?> getCurrentChildPlaceNavigation() {
    return scope.currentChildPlaceNavigation;
  }

  /**
   * Walks the active {@link Place}s and returns a snapshot of the current state
   * of the application, which can be used to create an entry in History.
   */
  public JsoArray<PlaceNavigationEvent<?>> collectHistorySnapshot() {
    return collectActiveChildPlaceNavigationEvents();
  }

  JsoArray<PlaceNavigationEvent<?>> collectActiveChildPlaceNavigationEvents() {
    JsoArray<PlaceNavigationEvent<?>> snapshot = JsoArray.create();
    PlaceNavigationEvent<?> child = getCurrentChildPlaceNavigation();

    int placeCount = 0;

    while (child != null) {

      // Detect a cycle and shiny.
      if (placeCount > PLACE_LIMIT) {
        Log.error(getClass(), "We probably have a cycle in our Place chain!");
        throw new RuntimeException("Cycle detected in Place chain!");
      }

      if (child.getPlace().isActive()) {
        snapshot.add(child);
        placeCount++;
      }
      child = child.getPlace().getCurrentChildPlaceNavigation();
    }

    return snapshot;
  }

  public PlaceNavigationEvent<? extends Place> createNavigationEvent() {
    return createNavigationEvent(JsoStringMap.<String>create());
  }

  /**
   * Should create the associated {@link PlaceNavigationEvent} for the concrete
   * Place implementation. We pass along key/value pairs that were decoded from
   * the History Token, if there were any. This will be an empty, non-null map
   * if there were no such key/values passed in.
   *
   *  Implementors are responsible for interpreting the <String,String> map and
   * initializing the {@link PlaceNavigationEvent} appropriately.
   *
   * @param decodedState key/value pairs encoded in the {@link HistoryPiece}
   *        associated with this Place.
   * @return the {@link PlaceNavigationEvent} associated with this Place
   */
  public abstract PlaceNavigationEvent<? extends Place> createNavigationEvent(
      JsonStringMap<String> decodedState);

  /**
   * This creates a child {@link PlaceNavigationEvent}s based on a
   * {@link HistoryPiece}.
   *
   * If there is no such child Place registered to us, then we return {@code
   * null}.
   *
   * @return the {@link PlaceNavigationEvent} for the child Place that is keyed
   *         by the name present in the inputed {@link HistoryPiece}, or {@code
   *         null} if there is no such child Place directly reachable from this
   *         Place.
   */
  private PlaceNavigationEvent<?> decodeChildNavigationEvent(NavigationToken childHistoryPiece) {
    Place childPlace = getRegisteredChild(childHistoryPiece.getPlaceName());

    if (childPlace == null) {
      Log.warn(getClass(),
          "Attempting to decode a Child navigation event for a Place that was not registered to"
              + " us.",
          "Parent: ",
          getName(),
          " Potential Child: ",
          childHistoryPiece.getPlaceName(),
          " State: ",childHistoryPiece.getBookmarkableState());
      return null;
    }

    return childPlace.createNavigationEvent(childHistoryPiece.getBookmarkableState());
  }

  /**
   * Dispatch cleanup to current place and all its subchildren
   *
   * @param includeCurrentChildPlace whether to clean up everything or just the
   *        subplaces
   */
  private void dispatchCleanup(boolean includeCurrentChildPlace) {
    if (getCurrentChildPlaceNavigation() != null) {
      JsoArray<PlaceNavigationEvent<?>> activeChildren = collectActiveChildPlaceNavigationEvents();

      // Decide if want to cleanup everything, or only subplaces
      int cleanLimit = includeCurrentChildPlace ? 0 : 1;

      // Walk the active subtree going bottom up, firing their cleanup handlers,
      // and letting them know they are inactive.
      for (int i = activeChildren.size() - 1; i >= cleanLimit; i--) {
        Place childPlace = activeChildren.get(i).getPlace();
        Place place = (i > 0) ? activeChildren.get(i - 1).getPlace() : this;
        cleanupChild(place, childPlace);
      }
    }
  }

  /**
   * Takes in an array of {@link HistoryPiece}s representing Place navigations
   * rooted at this Place, and dispatches them in order.
   *
   *  This method is intelligent, in that it will not re-dispatch Place
   * navigations that are already active. The last piece of the incoming history
   * pieces is always dispatched though.
   *
   *  This method will walk the common links until it encounters one of the
   * following scenarios:
   *
   *  1. Our current active Place chain ran out, and we have 1 or more pieces of
   * history to turn into events and dispatch.
   *
   *  2. The history pieces are shorter than active Place chain (like when you
   * click back), in which case we simple dispatch the last item in the history
   * pieces, on the appropriate parent Place's scope.
   *
   * NOTE: If you call this method without first calling
   * {@link #disableHistorySnapshotting()}, then each tier of the history
   * dispatch will result in a history token. If you do disable snapshotting,
   * please be nice and re-enable it when you are done by calling
   * {@link #enableHistorySnapshotting()}.
   *
   */
  public void dispatchHistory(JsonArray<NavigationToken> historyPieces) {

    // Terminate if there are no more pieces to dispatch.
    if (historyPieces.isEmpty()) {
      return;
    }

    NavigationToken piece = historyPieces.get(0);
    PlaceNavigationEvent<?> child = getCurrentChildPlaceNavigation();

    // The active Place chain ran out, go ahead and dispatch for real.
    if (child == null || !isActive()) {
      dispatchHistoryNow(historyPieces);
      return;
    }

    // Compare child to see if it is the same.
    if (historyPieceMatchesPlaceEvent(child, piece)) {

      // We dispatch if this is the last history piece.
      if (historyPieces.size() == 1) {
        dispatchHistoryNow(historyPieces);
      } else {

        // Recurse downwards passing the remainder of the history array.
        child.getPlace().dispatchHistory(historyPieces.slice(1, historyPieces.size()));
      }

      return;
    }

    // If we get here, then we know that we have reached the end of the common
    // overlap with the active Places and the history pieces.
    dispatchHistoryNow(historyPieces);
  }

  void dispatchHistoryNow(JsonArray<NavigationToken> historyPieces) {

    // Terminate if there are no more pieces to dispatch.
    if (historyPieces.isEmpty()) {
      return;
    }

    PlaceNavigationEvent<?> childNavEvent = decodeChildNavigationEvent(historyPieces.get(0));

    if (childNavEvent == null) {
      Log.warn(getClass(), "Attempted to dispatch a line of history rooted at: ", getName(),
          " but we had no such children.", historyPieces);
      return;
    }

    // Navigate to the child. This should invoke the PlaceNavigationHandler and
    // register any subsequent child Places.
    fireChildPlaceNavigation(childNavEvent);

    // Recurse downwards passing the remainder of the history array.
    childNavEvent.getPlace().dispatchHistoryNow(historyPieces.slice(1, historyPieces.size()));
  }

  public void disableHistorySnapshotting() {
    this.createHistoryToken = false;
  }

  public void enableHistorySnapshotting() {
    this.createHistoryToken = true;
  }

  /**
   * Dispatches a navigation event to the scope of this Place.
   */
  public void fireChildPlaceNavigation(@Nonnull PlaceNavigationEvent<? extends Place> event) {
    @SuppressWarnings("unchecked")
    PlaceNavigationEvent<Place> navigationEvent = (PlaceNavigationEvent<Place>) event;

    Log.info(getClass(), "Firing nav event "+event);
    // Make sure that we contain such a child registered to our scope.
    if (navigationEvent == null
        || getRegisteredChild(navigationEvent.getPlace().getName()) == null) {
      Log.warn(getClass(), "Attempted to navigate to a child place that was not registered to us.",
          navigationEvent, navigationEvent.getBookmarkableState());
      return;
    }

    // If we are not currently active, then we are not allowed to fire child
    // place navigations.
    if (!isActive && isStrict()) {
      Log.warn(getClass(), "Attempted to navigate to a child place when we were not active",
          navigationEvent, RootPlace.PLACE.collectHistorySnapshot().join(PathUtil.SEP));
      return;
    }

    // Whether or not the Place we are navigating to is the same type as the
    // Place we are currently in.
    boolean isReEntrantDispatch = false;

    // Inform the previous active child (and all active sub Places in that
    // chain) that he is no longer active.
    if (scope.currentChildPlaceNavigation != null
        && scope.currentChildPlaceNavigation.getPlace().isActive()) {
      isReEntrantDispatch =
          scope.currentChildPlaceNavigation.getPlace() == navigationEvent.getPlace();

      // Cleanup the old Place stack rooted at our current child place. If this
      // is a re-entrant dispatch, then we want to skip cleaning up ourselves.
      dispatchCleanup(!isReEntrantDispatch);
    }

    // Only reset the scope if we are navigating to a totally new Place.
    if (!isReEntrantDispatch) {

      // This ensures that when the child handler runs, it gets a clean scope
      // and therefore can't leak references to handler code.
      navigationEvent.getPlace().resetScope();
    }

    JsoArray<PlaceNavigationHandler<PlaceNavigationEvent<Place>>> handlers =
        scope.handlers.get(navigationEvent.getPlace().getName().toLowerCase());

    if (handlers == null || handlers.isEmpty()) {
      Log.warn(getClass(), "Firing navigation event with no registered handlers", navigationEvent);
    }

    for (int i = 0, n = handlers.size(); i < n; i++) {
      PlaceNavigationHandler<PlaceNavigationEvent<Place>> handler = handlers.get(i);

      if (isReEntrantDispatch) {
        handler.reEnterPlace(
            navigationEvent, !placesMatch(scope.currentChildPlaceNavigation, navigationEvent));
      } else {
        handler.enterPlace(navigationEvent);
      }
    }

    // Tell the new one that he is active AFTER invoking place navigation
    // handlers.
    navigationEvent.getPlace().setIsActive(true, this);
    scope.currentChildPlaceNavigation = navigationEvent;

    // This should default to true. It gets set to false if we are replaying a
    // line of history, in which case it really doesn't make sense to snapshot
    // each tier of the replay.
    if (createHistoryToken) {

      // Snapshot the active history by asking the RootPlace for all active
      // Places.
      JsoArray<PlaceNavigationEvent<?>> historySnapshot = RootPlace.PLACE.collectHistorySnapshot();

      // Set the History string now.
      HistoryUtils.createHistoryEntry(historySnapshot);
    }
  }


  /**
   * Dispatches a sequence of navigation events to the scope of this Place.
   *
   *  The navigationEvents must be non-null.
   *
   * NOTE: If you call this method without first calling
   * {@link #disableHistorySnapshotting()}, then each tier of the history
   * dispatch will result in a history token. If you do disable snapshotting,
   * please be nice and re-enable it when you are done by calling
   * {@link #enableHistorySnapshotting()}.
   */
  public void fireChildPlaceNavigations(JsoArray<PlaceNavigationEvent<?>> navigationEvents) {

    // Terminate if there are no more pieces to dispatch.
    if (navigationEvents.isEmpty()) {
      return;
    }

    Place place = this;
    for (int i = 0, n = navigationEvents.size(); i < n; i++) {
      PlaceNavigationEvent<?> childNavEvent = navigationEvents.get(i);

      // Navigate to the child. This should invoke the PlaceNavigationHandler
      // and register any subsequent child Places.
      place.fireChildPlaceNavigation(childNavEvent);
      place = childNavEvent.getPlace();
    }
  }

  /**
   * Dispatches an event on our Place's Scope and all currently active child
   * Places.
   */
  public void fireEvent(GwtEvent<?> event) {
    Place currPlace = this;
    while (currPlace != null && currPlace.isActive()) {
      fireEventInScope(currPlace, event);

      PlaceNavigationEvent<?> activeChildNavigation = currPlace.getCurrentChildPlaceNavigation();
      currPlace = (activeChildNavigation == null) ? null : activeChildNavigation.getPlace();
    }
  }

  /**
   * Dispatches an event on the specified Place's Scope {@link SimpleEventBus}.
   */
  private void fireEventInScope(Place place, GwtEvent<?> event) {
    // If we are not currently active, then we are not allowed to fire anything.
    if (!isActive) {
      Log.warn(getClass(), "Attempted to fire a simple event when we were not active", event);
      return;
    }

    place.scope.eventBus.fireEvent(event);
  }

  public String getName() {
    return name;
  }

  protected Place getRegisteredChild(String childName) {
    return scope.knownChildPlaces.get(childName.toLowerCase());
  }

  private boolean historyPieceMatchesPlaceEvent(
      PlaceNavigationEvent<?> event, NavigationToken historyPiece) {

    // First match the name. We use toLowerCase() to make it resilient to users
    // typing in URLs and mixing up cases.
    final boolean namesMatch =
        event.getPlace().getName().toLowerCase().equals(historyPiece.getPlaceName().toLowerCase());

    // We also want to make sure the state that was passed in the parsed history
    // match our live state.
    StateMatcher matcher = new StateMatcher(historyPiece.getBookmarkableState());

    // Now we match all state key/values.
    JsonStringMap<String> state = event.getBookmarkableState();
    state.iterate(matcher);

    return namesMatch && matcher.matches;
  }

  /**
   * Compare two Places (including parameters) to see if they match
   *
   *  We say a place matches if all the state in the Place we are navigating to
   * is already contained in the current Place.
   */
  protected boolean placesMatch(PlaceNavigationEvent<?> current, PlaceNavigationEvent<?> next) {
    if (current == null) {
      return false;
    }

    StateMatcher matcher = new StateMatcher(current.getBookmarkableState());

    JsonStringMap<String> state = next.getBookmarkableState();
    state.iterate(matcher);

    return matcher.matches;
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean isLeaf() {
    PlaceNavigationEvent<?> activeChildPlaceNavigation = getCurrentChildPlaceNavigation();
    return activeChildPlaceNavigation == null || !activeChildPlaceNavigation.getPlace().isActive();
  }

  /**
   * @return true if this navigation event is active and is the leaf of the
   *         place chain.
   */
  public boolean isActiveLeaf() {
    return isLeaf() && isActive();
  }

  protected boolean isStrict() {
    return isStrict;
  }

  /**
   * Registers a {@link PlaceNavigationHandler} to deal with navigations to a
   * particular child Place.
   *
   * <p>Subclasses are encouraged to restrict the types of children Places that
   * can be registered in their public API. But this API is still visible and
   * doesn't restrict the type of Place that can be added as a child.
   *
   * @param <C> subclass of {@link Place} that is the child place we are
   *        registering. <C> must a Place that is initialized by handler
   *        of appropriate type
   */
  public <C extends Place> void registerChildHandler(
      C childPlace, PlaceNavigationHandler<? extends PlaceNavigationEvent<C>> handler) {
    String placeName = childPlace.getName().toLowerCase();
    JsoArray<PlaceNavigationHandler<PlaceNavigationEvent<Place>>> placeHandlers =
        scope.handlers.get(placeName);
    if (placeHandlers == null) {
      placeHandlers = JsoArray.create();
    }

    @SuppressWarnings("unchecked")  // We promise this cast is okay...
    PlaceNavigationHandler<PlaceNavigationEvent<Place>> placeNavigationHandler =
        (PlaceNavigationHandler<PlaceNavigationEvent<Place>>) ((Object) handler);
    placeHandlers.add(placeNavigationHandler);
    scope.handlers.put(placeName, placeHandlers);
    scope.knownChildPlaces.put(placeName, childPlace);
  }

  /**
   * Registers an {@link EventHandler} on our Scope's {@link SimpleEventBus}.
   * Dispatches on
   *
   * @param <T> the type of the {@link EventHandler}
   */
  public <T extends EventHandler> void registerSimpleEventHandler(
      GwtEvent.Type<T> eventType, T handler) {
    scope.eventBus.addHandler(eventType, handler);
  }

  void resetScope() {
    scope = new Scope();
  }

  /**
   * Sets whether or not this Place is currently active. A Place is not allowed
   * to dispatch to its scope if it is not active.
   *
   * Note that this method is protected and not private simply because the
   * {@link RootPlace} needs to be able to set this.
   */
  protected void setIsActive(boolean isActive, Place currentParentPlace) {
    this.isActive = isActive;
    this.currentParentPlace = currentParentPlace;
  }

  /**
   * Determines whether or not the place should throw exceptions if it is not active.
   * @param isStrict
   * @return
   */
  public void setIsStrict(boolean isStrict) {
    this.isStrict = isStrict;
  }

  /**
   * Leaves the current place, re-entering its parent
   */
  public void leave() {
    /*
     * This precondition is somewhat arbitrary, as long as a place is active and
     * not RootPlace then it should be fine to leave it. For now it's nice since
     * it restricts the scope of this method.
     */
    Preconditions.checkState(isActiveLeaf(), "Place must be the active leaf to be left");

    /*
     * TODO: This implementation means you can't leave an immediate
     * child of the RootPlace, which seems okay for now. When we rewrite the
     * place framework we'll make this more general.
     */
    Place parent = getParentPlace();
    Preconditions.checkNotNull(parent, "Parent cannot be null");

    Place grandParent = parent.getParentPlace();
    Preconditions.checkNotNull(grandParent, "Grandparent cannot be null");

    grandParent.fireChildPlaceNavigation(parent.getCurrentChildPlaceNavigation());
  }

  @Override
  public String toString() {
    return "Place{name=" + name + ", isActive=" + isActive + "}";
  }
}
