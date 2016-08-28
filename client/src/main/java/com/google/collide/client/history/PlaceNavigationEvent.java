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

import com.google.collide.clientlibs.navigation.NavigationToken;

/**
 * Event fired to signal a transition to a particular {@link Place}.
 *
 *  These events are cheap to instantiate, and should be used as vehicles for
 * delivering any state needed by the {@link PlaceNavigationHandler} to perform
 * the transition.
 */
public abstract class PlaceNavigationEvent<P extends Place> implements NavigationToken {

  private final P associatedPlace;

  protected PlaceNavigationEvent(P associatedPlace) {
    this.associatedPlace = associatedPlace;
  }

  public P getPlace() {
    return associatedPlace;
  }

  /**
   * @return true if this navigation event is active and is the leaf of the
   *         place chain.
   */
  public boolean isActiveLeaf() {
    return getPlace().isActiveLeaf();
  }

  @Override
  public String getPlaceName() {
    return associatedPlace.getName();
  }

  @Override
  public String toString() {
    return "PlaceNavigationEvent{" + "associatedPlace=" + associatedPlace.toString() + ", isLeaf="
        + getPlace().isLeaf() + "}";
  }
}
