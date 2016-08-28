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

import com.google.collide.shared.util.SortedList;

/**
 * List for line anchors with specific optimizations for line anchors.
 */
public class LineAnchorList extends SortedList<Anchor> {

  private static class Comparator implements SortedList.Comparator<Anchor> {
    @Override
    public int compare(Anchor a, Anchor b) {
      return a.getLineNumber() - b.getLineNumber();
    }
  }

  private static class OneWayComparator implements SortedList.OneWayComparator<Anchor> {
    private int lineNumber;

    @Override
    public int compareTo(Anchor o) {
      return lineNumber - o.getLineNumber();
    }
  }

  private static final Comparator COMPARATOR = new Comparator();

  private final OneWayComparator oneWayComparator = new OneWayComparator();

  public LineAnchorList() {
    super(COMPARATOR);
  }

  public int findInsertionIndex(int lineNumber) {
    oneWayComparator.lineNumber = lineNumber;
    return findInsertionIndex(oneWayComparator);
  }
}
