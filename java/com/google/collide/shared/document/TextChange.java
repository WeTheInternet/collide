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
 * Models a text change on the document.
 *
 */
public class TextChange {

  public enum Type {
    INSERT, DELETE
  }

  public static TextChange createDeletion(Line line, int lineNumber, int column,
      String deletedText) {
    return new TextChange(Type.DELETE, line, lineNumber, column, line, lineNumber, deletedText);
  }

  public static TextChange createInsertion(Line line, int lineNumber, int column, Line lastLine,
      int lastLineNumber, String text) {
    return new TextChange(Type.INSERT, line, lineNumber, column, lastLine, lastLineNumber, text);
  }

  // This class implements equals and hashCode, make sure to update!
  private final int column;
  private final Line lastLine;
  private final int lastLineNumber;
  private final Line line;
  private final int lineNumber;
  private final String text;
  private final Type type;

  public TextChange(Type type, Line line, int lineNumber, int column, Line lastLine,
      int lastLineNumber, String text) {
    this.type = type;
    this.line = line;
    this.lineNumber = lineNumber;
    this.column = column;
    this.lastLine = lastLine;
    this.lastLineNumber = lastLineNumber;
    this.text = text;
  }

  public int getColumn() {
    return column;
  }

  /**
   * Returns the last line "touched", meaning the last line that was either
   * created or mutated. For example, an insertion of "\n" would have
   * {@link #getLine()} return the line receiving that insertion, and this
   * method would return the newly inserted line.
   */
  public Line getLastLine() {
    return lastLine;
  }

  /**
   * Returns the line number corresponding to {@link #getLastLine()}.
   */
  public int getLastLineNumber() {
    return lastLineNumber;
  }

  /**
   * For insertations, returns the line that received the ending character
   * of {@link #getText()}; for deletions, returns {@link #getLine()}.
   */
  public Line getEndLine() {
    if (type == Type.DELETE) {
      return line;
    }
    return text.endsWith("\n") ? lastLine.getPreviousLine() : lastLine;
  }

  /**
   * @return line number corresponding to {@link #getEndLine()}.
   */
  public int getEndLineNumber() {
    if (type == Type.DELETE) {
      return lineNumber;
    }
    return text.endsWith("\n") ? lastLineNumber - 1 : lastLineNumber;
  }

  /**
   * For insertions, returns the column where the last character of
   * {@link #getText()} was inserted (on the {@link #getEndLine()});
   * for deletions, returns {@link #getColumn()}.
   */
  public int getEndColumn() {
    if (type == Type.DELETE) {
      return column;
    }

    // Need "- 1" in all returns below since we're returning an inclusive value

    // If there were no newlines in the inserted text, it's simple
    if (line == lastLine) {
      return column + text.length() - 1;
    }

    /*
     * If the last char of text is a newline, then it is endLine's last
     * character
     */
    if (text.endsWith("\n")) {
      return getEndLine().getText().length() - 1;
    }

    /*
     * If it is a non-newline character on a multi-line insertion, then find the
     * length of the insertion text's last line
     */
    int lastLineStartIndexInText = text.lastIndexOf('\n') + 1;
    return text.length() - lastLineStartIndexInText - 1;
  }

  /**
   * Returns the line that received the text change.
   */
  public Line getLine() {
    return line;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getText() {
    return text;
  }

  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    // Calling type.toString() will prevent it from becoming a simple ordinal
    switch (type) {
      case INSERT:
        return "I(" + column + ", " + text + ")";

      case DELETE:
        return "D(" + column + ", " + text + ")";

      default:
        return "Unknown type (ordinal is " + type.ordinal() + ")";
    }
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + column;
    result = 31 * result + lastLine.hashCode();
    result = 31 * result + lastLineNumber;
    result = 31 * result + line.hashCode();
    result = 31 * result + lineNumber;
    result = 31 * result + text.hashCode();
    result = 31 * result + type.hashCode();
    
    return result;
  }

  @Override
  public boolean equals(Object otherObj) {
    if (!(otherObj instanceof TextChange)) {
      return false;
    }
    
    TextChange o = (TextChange) otherObj;
    return o.column == column && o.lastLine == lastLine && o.lastLineNumber == lastLineNumber
        && o.line == line && o.lineNumber == lineNumber && o.text.equals(text)
        && o.type.equals(type);
  }
}
