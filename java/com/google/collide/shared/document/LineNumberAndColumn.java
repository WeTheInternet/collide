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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * A simple structure to store the line number and column.
 *
 */
public final class LineNumberAndColumn implements Comparable<LineNumberAndColumn> {

  public final int column;
  public final int lineNumber;

  public static LineNumberAndColumn from(int lineNumber, int column) {
    return new LineNumberAndColumn(lineNumber, column);
  }

  @VisibleForTesting
  public LineNumberAndColumn(int lineNumber, int column) {
    this.lineNumber = lineNumber;
    this.column = column;
  }

  @Override
  public String toString() {
    return "(" + lineNumber + ", " + column + ")";
  }

  @Override
  public int compareTo(LineNumberAndColumn o) {
    Preconditions.checkNotNull(o);
    int result = this.lineNumber - o.lineNumber;
    return result == 0 ? this.column - o.column : result;
  }
}
