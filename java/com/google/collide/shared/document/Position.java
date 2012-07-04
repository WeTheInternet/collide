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

/**
 * A class to encapsulate a position in the document given in terms of a line
 * and column.
 *
 * This should only be used by methods that return the pair. Methods that need
 * to take a line and column as input should take them as separate parameters to
 * reduce code complexity and object allocations.
 *
 * This class is immutable.
 *
 */
public class Position {
  private final LineInfo lineInfo;
  private final int column;

  public Position(LineInfo lineInfo, int column) {
    // Defensively copy to ensure immutabilty
    this.lineInfo = lineInfo.copy();
    this.column = column;
  }

  public int getColumn() {
    return column;
  }

  public Line getLine() {
    return lineInfo.line();
  }

  /**
   * Returns the line info object, if set by the creator.
   */
  public LineInfo getLineInfo() {
    // Defensively copy to ensure immutabilty
    return lineInfo.copy();
  }

  public int getLineNumber() {
    return lineInfo.number();
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 37 * result + lineInfo.hashCode();
    result = 37 * result + column;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Position)) {
      return false;
    }

    Position o = (Position) obj;
    return lineInfo.equals(o.lineInfo) && column == o.column;
  }

  @Override
  public String toString() {
    return "Position(" + lineInfo + ", " + column + ")";
  }
}
