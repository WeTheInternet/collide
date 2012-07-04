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

package com.google.collide.shared.ot;

import com.google.collide.dto.DocOp;

/**
 * A class to transform a document position with a document operation so that
 * the the relative position is maintained even after the document operation is
 * applied.
 */
public class PositionTransformer {

  private class Transformer implements DocOpCursor {
    private int docOpColumn;
    private int docOpLineNumber;

    @Override
    public void delete(String text) {
      if (lineNumber == docOpLineNumber) {
        if (column >= docOpColumn) {
          // We need to update the position because the deletion is before it
          int columnOfLastDeletedChar = docOpColumn + text.length() - 1;
          if (column <= columnOfLastDeletedChar) {
            /*
             * The position is inside the deleted region, but we want to keep it
             * alive, so it's final position is the column where the delete
             * collapses to
             */
            column = docOpColumn;
          } else {
            /*
             * The position is after the deletion region, offset the position to
             * account for the delete
             */
            column -= text.length();
          }
        }
      } else if (text.endsWith("\n") && lineNumber == docOpLineNumber + 1) {
        /*
         * This line and the next are being joined and our position is on the
         * next line. Bring the position onto this line.
         */
        lineNumber = docOpLineNumber;
        column += docOpColumn;
      } else if (text.endsWith("\n") && lineNumber > docOpLineNumber) {
        lineNumber--;
      }
    }

    @Override
    public void insert(String text) {
      if (lineNumber == docOpLineNumber) {
        if (column >= docOpColumn) {
          if (text.endsWith("\n")) {
            // Splitting the lines
            lineNumber++;
            column = column - docOpColumn;
          } else {
            column += text.length();
          }
        }
      } else if (lineNumber > docOpLineNumber && text.endsWith("\n")) {
        lineNumber++;
      }

      if (text.endsWith("\n")) {
        skipDocOpLines(1);
      } else {
        docOpColumn += text.length();
      }
    }

    @Override
    public void retain(int count, boolean hasTrailingNewline) {
      if (hasTrailingNewline) {
        skipDocOpLines(1);
      } else {
        docOpColumn += count;
      }
    }

    @Override
    public void retainLine(int lineCount) {
      skipDocOpLines(lineCount);
    }

    private void skipDocOpLines(int lineCount) {
      docOpLineNumber += lineCount;
      docOpColumn = 0;
    }
  }

  private int column;
  private int lineNumber;

  public PositionTransformer(int lineNumber, int column) {
    this.column = column;
    this.lineNumber = lineNumber;
  }

  public void transform(DocOp op) {
    Transformer transformer = new Transformer();
    DocOpUtils.accept(op, transformer);
  }

  public int getColumn() {
    return column;
  }

  public int getLineNumber() {
    return lineNumber;
  }
}
