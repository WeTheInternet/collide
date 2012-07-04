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

import com.google.collide.clientlibs.invalidation.InvalidationRegistrar;
import com.google.collide.shared.invalidations.InvalidationObjectId;

/**
 * Mock version of {@link InvalidationRegistrar} that does not fire any
 * invalidations
 */
public class MockInvalidationRegistrar implements InvalidationRegistrar {

  @Override
  public RemovableHandle register(InvalidationObjectId<?> objectId, Listener eventListener) {
    return new RemovableHandle() {
      @Override
      public void remove() {
      }

      @Override
      public void initializeRecoverer(long nextExpectedVersion) {
      }
    };
  }

  @Override
  public void unregister(InvalidationObjectId<?> objectId) {
  }
}
