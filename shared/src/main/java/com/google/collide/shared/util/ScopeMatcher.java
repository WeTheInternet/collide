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

package com.google.collide.shared.util;

/**
 * Searches lines of text to find the matching scopeEnd character. It will keep
 * a stack of additional scopeStart characters it comes across to find the right
 * scopeEnd. Examples of this are for finding matching { }, [ ] and ( ) pairs.
 */
public class ScopeMatcher {
  private final boolean searchingForward;
  private final char scopeEndChar;
  private final char scopeStartChar;
  private int stack = 0;
  private int nextMatchColumn;

  public ScopeMatcher(boolean searchingForward, char start, char end) {
    this.searchingForward = searchingForward;
    scopeStartChar = start;
    scopeEndChar = end;
  }

  /**
   * Search the next line for {@link #scopeEndChar}, keeping the {@link #stack}
   * in mind.
   * 
   * @param text
   * @return columnIndex, or -1 if no match on this line.
   */
  public int searchNextLine(String text) {
    nextMatchColumn = searchingForward ? 0 : text.length() - 1;
    while (true) {
      if (isStartColumnNext(text)) {
        stack++;
      } else if (nextMatchColumn >= 0) {
        stack--;
      }

      if (stack == 0 && nextMatchColumn >= 0) {
        return nextMatchColumn;
      } else if (nextMatchColumn == -1) {
        break;
      }
      proceedForward();
    }
    return -1;
  }

  /**
   * Move to the next occurrence of either {@link #scopeStartChar} or
   * {@link #scopeEndChar} and update {@link #nextMatchColumn}.
   */
  private boolean isStartColumnNext(String text) {
    int startCharColumn;
    if (searchingForward) {
      startCharColumn = text.indexOf(scopeStartChar, nextMatchColumn);
      nextMatchColumn = text.indexOf(scopeEndChar, nextMatchColumn);
      if ((startCharColumn < nextMatchColumn || nextMatchColumn == -1) && startCharColumn != -1) {
        nextMatchColumn = startCharColumn;
        return true;
      }
    } else {
      if (nextMatchColumn == -1) {
        /*
         * TODO: Firefox/Chrome bug where lastIndexOf with a
         * negative offset parameter will return a match for the first
         * character. http://code.google.com/p/google-web-toolkit/issues/detail?id=6615
         */
        return false;
      }
      startCharColumn = text.lastIndexOf(scopeStartChar, nextMatchColumn);
      nextMatchColumn = text.lastIndexOf(scopeEndChar, nextMatchColumn);
      if ((startCharColumn > nextMatchColumn || nextMatchColumn == -1) && startCharColumn != -1) {
        nextMatchColumn = startCharColumn;
        return true;
      }
    }
    return false;
  }

  private void proceedForward() {
    if (searchingForward) {
      nextMatchColumn++;
    } else {
      nextMatchColumn--;
    }
  }
}
