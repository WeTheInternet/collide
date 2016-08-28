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

package com.google.collide.clientlibs.invalidation;

import com.google.collide.clientlibs.invalidation.InvalidationManager.Recoverer;
import com.google.collide.shared.invalidations.InvalidationObjectId;
import com.google.collide.shared.util.Timer;
import com.google.collide.shared.util.Timer.Factory;


/**
 * A factory which can return a {@link DropRecoveringInvalidationController}. Helps to dodge some
 * GWT.create dependency stuff from TangoLogger.
 *
 */
class DropRecoveringInvalidationControllerFactory {

  public final InvalidationLogger logger;
  private final Factory timerFactory;

  public DropRecoveringInvalidationControllerFactory(
      InvalidationLogger logger, Timer.Factory timerFactory) {
    this.logger = logger;
    this.timerFactory = timerFactory;
  }

  public DropRecoveringInvalidationController create(InvalidationObjectId<?> objectId,
      InvalidationRegistrar.Listener listener, Recoverer recoverer) {
    return new DropRecoveringInvalidationController(
        logger, objectId, listener, recoverer, timerFactory);
  }
}
