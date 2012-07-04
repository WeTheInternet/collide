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

package com.google.collide.shared.grok;

import com.google.collide.shared.document.LineNumberAndColumn;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * A class to translate between position representations, such as (line number,
 * column) and text offset.
 *
 */
@GwtCompatible
public class PositionTranslator {

  private static List<Integer> calculateNewlineOffsets(String fileContents) {
    Preconditions.checkNotNull(fileContents);
    List<Integer> newlineOffsets = Lists.newArrayList();
    for (int i = -1; (i = fileContents.indexOf('\n', i + 1)) >= 0;) {
      newlineOffsets.add(i);
    }

    return newlineOffsets;
  }

  /**
   * Stores the offset for the newline character on line i, where i is the index
   */
  private final List<Integer> newlineOffsets;

  public PositionTranslator(String fileContents) {
    this.newlineOffsets = calculateNewlineOffsets(fileContents);
  }

  public LineNumberAndColumn getLineNumberAndColumn(int offset) {
    int binarySearchResult = Collections.binarySearch(newlineOffsets, offset);
    int lineNumber = binarySearchResult >= 0 ? binarySearchResult : -(binarySearchResult + 1);
    int offsetOfFirstCharacterOnLine = getOffsetOfPreviousNewline(lineNumber) + 1;
    return new LineNumberAndColumn(lineNumber, offset - offsetOfFirstCharacterOnLine);
  }

  public int getOffset(int lineNumber, int column) {
    int offsetOfPreviousNewline = getOffsetOfPreviousNewline(lineNumber);
    return offsetOfPreviousNewline + 1 + column;
  }

  /**
   * Returns -1 if this is the first line.
   */
  private int getOffsetOfPreviousNewline(int lineNumber) {
    return lineNumber > 0 ? newlineOffsets.get(lineNumber - 1) : -1;
  }
}
