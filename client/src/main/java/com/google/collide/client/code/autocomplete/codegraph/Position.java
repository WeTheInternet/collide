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

package com.google.collide.client.code.autocomplete.codegraph;

/**
 * Encapsulates (line, column) pair.
 */
public class Position implements Comparable<Position> {
  private final int line;
  private final int col;

  private Position(int line, int col) {
    this.line = line;
    this.col = col;
  }

  public int getLine() {
    return this.line;
  }

  public int getColumn() {
    return this.col;
  }

  @Override
  public String toString() {
    return "(" + line + ", " + col + ")";
  }

  public static Position from(int line, int col) {
    return new Position(line, col);
  }

  @Override
  public int compareTo(Position o) {
    int result = this.line - o.line;
    return result == 0 ? this.col - o.col : result;
  }
}
