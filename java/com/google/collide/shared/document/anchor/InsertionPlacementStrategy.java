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

/**
 * A strategy that determines where an anchor will be placed after a text
 * insertion at the anchor's position.
 *
 * For line anchors, this strategy is consulted when a newline is inserted
 * anywhere on the anchor's line. For column anchors, this strategy is consulted
 * when a character is inserted at the anchor's column.
 *
 */
public interface InsertionPlacementStrategy {

  /**
   * Constants for the two positions that are possible placement targets.
   */
  public enum Placement {
    EARLIER, LATER
  }

  /**
   * Always stays at the earlier position.
   */
  public static final InsertionPlacementStrategy EARLIER =
      new InsertionPlacementStrategy() {
        @Override
        public Placement determineForInsertion(Anchor anchor, Line line, int column) {
          return Placement.EARLIER;
        }
      };

  /**
   * Always moves to the later position.
   */
  public static final InsertionPlacementStrategy LATER =
      new InsertionPlacementStrategy() {
        @Override
        public Placement determineForInsertion(Anchor anchor, Line line, int column) {
          return Placement.LATER;
        }
      };

  /**
   * The default behavior for line anchors is to stay on the line,
   * and the default behavior for normal anchors is to move with
   * the insertion.
   */
  public static final InsertionPlacementStrategy DEFAULT =
      new InsertionPlacementStrategy() {
        @Override
        public Placement determineForInsertion(Anchor anchor, Line line, int column) {
          InsertionPlacementStrategy currentStrategy =
              anchor.isLineAnchor() ? EARLIER : LATER;
          return currentStrategy.determineForInsertion(anchor, line, column);
        }
      };

  Placement determineForInsertion(Anchor anchor, Line line, int column);
}
