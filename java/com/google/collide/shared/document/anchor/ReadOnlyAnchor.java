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

package com.google.collide.shared.document.anchor;

import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.util.ListenerRegistrar;

/**
 * A read-only interface to {@link Anchor}.
 */
public interface ReadOnlyAnchor {

  /**
   * @see Anchor.ShiftListener
   */
  public interface ShiftListener extends Anchor.ShiftListenerImpl<ReadOnlyAnchor> {
  }
  
  /**
   * @see Anchor.MoveListener
   */
  public interface MoveListener extends Anchor.MoveListenerImpl<ReadOnlyAnchor> {
  }
  
  /**
   * @see Anchor.RemoveListener
   */
  public interface RemoveListener extends Anchor.RemoveListenerImpl<ReadOnlyAnchor> {
  }
  
  int getColumn();
  
  int getId();

  <T> T getValue();
  
  AnchorType getType();
  
  Line getLine();
  
  int getLineNumber();
  
  boolean isLineAnchor();
  
  RemovalStrategy getRemovalStrategy();
  
  boolean hasLineNumber();
  
  boolean isAttached();
  
  InsertionPlacementStrategy getInsertionPlacementStrategy();
  
  ListenerRegistrar<ShiftListener> getReadOnlyShiftListenerRegistrar();
  
  ListenerRegistrar<MoveListener> getReadOnlyMoveListenerRegistrar();
  
  ListenerRegistrar<RemoveListener> getReadOnlyRemoveListenerRegistrar();

  boolean hasColumn();
}
