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

package com.google.collide.client.code.autocomplete.html;

import javax.annotation.Nullable;

/**
 * Class that tracks "dirty" state and sends notifications to delegate.
 */
public class DirtyStateTracker {

  /**
   * Flag that indicates that parsing of this tag is not finished yet.
   */
  private boolean dirty;

  /**
   * Delegate to be notified when object becomes "clean".
   */
  private Runnable delegate;

  public DirtyStateTracker() {
    setDirty(true);
  }

  public boolean isDirty() {
    return dirty;
  }

  protected void setDirty(boolean dirty) {
    this.dirty = dirty;
    if (!dirty && delegate != null) {
      delegate.run();
    }
  }

  public void setDelegate(@Nullable Runnable delegate) {
    this.delegate = delegate;
  }
}
