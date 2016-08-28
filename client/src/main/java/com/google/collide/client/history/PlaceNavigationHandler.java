package com.google.collide.client.history;

import com.google.gwt.event.shared.EventHandler;

/**
 * Handles navigating to a {@link Place}.
 *
 * Child {@link Place}s should be wired up when this handler is invoked.
 */
public abstract class PlaceNavigationHandler<E extends PlaceNavigationEvent<?>>
    implements EventHandler {

  /**
   * Invoked when we navigate away from a particular Place. Logically equivalent
   * to popping a Place off the Place stack.
   *
   * Subclasses should implement cleanup if any state leaks outside of the
   * {@link Place}'s scope.
   */
  protected void cleanup() {
    // Empty default implementation.
  }

  /**
   * Invoked when we navigate to a particular place that ALREADY IS ACTIVE on
   * the Place stack. For example, if the current Place stack is "A/B/C" and we
   * dispatch "B" on Place "A" then this method will be called. It is subtly
   * different from {@link #enterPlace(PlaceNavigationEvent)} in that we have an
   * opportunity to skip heavy weight setup code here that was run when
   * {@link #enterPlace(PlaceNavigationEvent)} first navigated us to this place.
   *
   * <p>Note that the associated Place's scope is NOT RESET before invoking
   * reEnterPlace by the Place framework.
   *
   * <p>reEnterPlace can be called any number of times and is NOT matched by calls
   * to {@link #cleanup()} in the framework.
   *
   * <p>The Default implementation simply simulates a regular non-re-entrant
   * dispatch. That is, a cleanup/enter pair with a call to reset the scope in
   * between them. Concrete implementations have the opportunity to override
   * this if they choose to short circuit expensive work done in
   * {@link #enterPlace(PlaceNavigationEvent)}.
   *
   * @param navigationEvent the PlaceNavigationEvent associated with the
   *        navigation to the new {@link Place}.
   * @param hasNewState whether or not the navigationEvent has new bookmarkable
   *        state.
   */
  protected void reEnterPlace(E navigationEvent, boolean hasNewState) {
    cleanup();
    navigationEvent.getPlace().resetScope();
    enterPlace(navigationEvent);
  }

  /**
   * Invoked when we navigate to a particular place. Logically equivalent to
   * pushing onto the Place stack. Code that runs within this method receives a
   * {@link PlaceNavigationEvent} whose associated {@link Place} has a clean
   * scope.
   *
   * All calls to enterPlace are matched by a call to {@link #cleanup()} before
   * subsequent invocations if enterPlace.
   *
   * @param navigationEvent the PlaceNavigationEvent associated with the
   *        navigation to the new {@link Place}.
   */
  protected abstract void enterPlace(E navigationEvent);
}
