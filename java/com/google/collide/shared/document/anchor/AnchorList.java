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
import com.google.common.annotations.VisibleForTesting;

/**
 * List with anchor-specific optimizations. This class assumes all the anchors
 * in the list will be on the same line.
 */
@VisibleForTesting
public class AnchorList extends SortedList<Anchor> {

  private static class Comparator implements SortedList.Comparator<Anchor> {
    @Override
    public int compare(Anchor a, Anchor b) {
      int comparison = a.getColumn() - b.getColumn();
      if (comparison == 0) {
        comparison = (a.getId() > b.getId()) ? 1 : (a.getId() < b.getId()) ? -1 : 0;
      }
      return comparison;
    }
  }

  private static class OneWayComparator implements SortedList.OneWayComparator<Anchor> {
    private int column;
    private int id;

    @Override
    public int compareTo(Anchor o) {
      int comparison = column - o.getColumn();
      if (comparison == 0) {
        comparison = (id > o.getId()) ? 1 : (id < o.getId()) ? -1 : 0;
      }
      return comparison;
    }
  }

  private static final Comparator COMPARATOR = new Comparator();

  private final OneWayComparator oneWayComparator = new OneWayComparator();

  public AnchorList() {
    super(COMPARATOR);
  }

  // TODO: second parameter is always -1. Remove?
  public int findInsertionIndex(int columnNumber, int id) {
    oneWayComparator.column = columnNumber;
    oneWayComparator.id = id;
    return findInsertionIndex(oneWayComparator);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size(); i++) {
      sb.append(get(i).toString());
      sb.append("\n");
    }
    return sb.toString();
  }
}
