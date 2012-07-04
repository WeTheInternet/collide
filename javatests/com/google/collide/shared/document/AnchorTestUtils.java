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

package com.google.collide.shared.document;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;

/**
 * Utility methods for testing anchors.
 */
public final class AnchorTestUtils {

  private AnchorTestUtils() {
  }

  static void assertAnchorPosition(Anchor anchor, int lineNumber, boolean ignoresLineNumber,
      int column) {
    assertTrue(anchor.isAttached());
    assertEquals(anchor.getLine().getDocument().getLineFinder().findLine(lineNumber).line(),
        anchor.getLine());
    assertEquals(ignoresLineNumber ? AnchorManager.IGNORE_LINE_NUMBER : lineNumber,
        anchor.getLineNumber());
    assertEquals(column, anchor.getColumn());
    
    if (anchor.hasLineNumber()) {
      assertTrue(anchor.getLine().getDocument().getAnchorManager().getLineAnchors()
          .findIndex(anchor) != -1);
    }
  }

  static void assertAnchorPositions(Object... anchorAndLineNumbersAndColumnsAlternating) {
    for (int i = 0; i < anchorAndLineNumbersAndColumnsAlternating.length; i++) {
      Anchor anchor = (Anchor) anchorAndLineNumbersAndColumnsAlternating[i];
      int lineNumber = (Integer) anchorAndLineNumbersAndColumnsAlternating[++i];
      int column = (Integer) anchorAndLineNumbersAndColumnsAlternating[++i];

      assertEquals(lineNumber, anchor.getLineNumber());
      assertEquals(column, anchor.getColumn());
    }
  }

  static void assertAnchorLineNumbers(Object... anchorAndLineNumbersAlternating) {
    for (int i = 0; i < anchorAndLineNumbersAlternating.length; i++) {
      Anchor anchor = (Anchor) anchorAndLineNumbersAlternating[i];
      int lineNumber = (Integer) anchorAndLineNumbersAlternating[++i];

      assertEquals(lineNumber, anchor.getLineNumber());
    }
  }

  static void assertAnchorColumns(Object... anchorAndColumnsAlternating) {
    for (int i = 0; i < anchorAndColumnsAlternating.length; i++) {
      Anchor anchor = (Anchor) anchorAndColumnsAlternating[i];
      int column = (Integer) anchorAndColumnsAlternating[++i];

      assertEquals(column, anchor.getColumn());
    }
  }
}
