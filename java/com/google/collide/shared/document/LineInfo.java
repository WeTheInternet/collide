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

/*
 * TODO: mutable lineinfos end up being expensive because the
 * defensive copies. Make LineInfo immutable.
 */
/*
 * TODO: I ended up passing LineInfo around frequently for a (line,
 * lineNumber) pair just because it exists, but it leads to uglier code
 * (lineInfo.number() instead of lineNumber) and more objects, reconsider this
 * thing's use case (originally it was for returning a Line+LineNumber pair from
 * a method
 */
/**
 * A POJO for a {@link Line} and its line number. This is mostly for returning
 * that pair of information, not for retaining longer-term since line numbers
 * shift constantly, and this class will not get updated.
 *
 */
public class LineInfo implements Comparable<LineInfo> {

  private Line line;

  private int number;

  public LineInfo(Line line, int number) {
    this.line = line;
    this.number = number;
  }

  @Override
  public int compareTo(LineInfo o) {
    return number - o.number;
  }

  public LineInfo copy() {
    return new LineInfo(line, number);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LineInfo)) {
      return false;
    }

    LineInfo other = (LineInfo) obj;
    return line.equals(other.line) && number == other.number;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 37 * result + number;
    result = 37 * result + line.hashCode();
    return result;
  }

  public Line line() {
    return line;
  }

  public boolean moveToNext() {
    return moveToImpl(line.getNextLine(), number + 1);
  }

  public boolean moveToPrevious() {
    return moveToImpl(line.getPreviousLine(), number - 1);
  }

  public int number() {
    return number;
  }

  @Override
  public String toString() {
    return "" + number + ": " + line.toString();
  }

  private boolean moveToImpl(Line newLine, int newNumber) {
    if (newLine == null) {
      return false;
    }

    line = newLine;
    number = newNumber;

    return true;
  }

  public boolean moveTo(boolean iterateForward) {
    return iterateForward ? moveToNext() : moveToPrevious();
  }
}
