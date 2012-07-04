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

package com.google.collide.dto;

// TODO: These should be moved to an Editor2-specific package
/**
 * Models one component of a document operation for the Collide code editor.
 *
 * With exception of RetainLine, all DocOpComponents must only touch one line.
 * If there is a multiline insert, that must be split into multiple Inserts, one
 * per line.
 *
 * @see DocOp
 */
public interface DocOpComponent {

  /**
   * Models a deletion within a document operation. This contains the text to be
   * deleted. The deletion must not span multiple lines (can only contain a
   * maximum of one newline character, which must be the last character.)
   */
  public interface Delete extends DocOpComponent {
    String getText();
  }

  /**
   * Models an insertion within a document operation. This contains the text to
   * be inserted. The insertion must not span multiple lines (can only contain a
   * maximum of one newline character, which must be the last character.)
   */
  public interface Insert extends DocOpComponent {
    String getText();
  }

  /*-
   * The Retain must indicate whether it is covering the end of the line. This
   * is exposed by the hasTrailingNewline() method.
   *
   * Here's an example of why this is required: Imagine we are composing
   * operations A and B in a two line document:
   *   "Hello\n"
   *   "World"
   *
   * A is modifying the first line and second line, and B is modifying the
   * second line.
   *
   * A is
   *   {(Insert:"This is line 1"), (Retain:6, true), (Insert:"2"),
   *    (Retain:5)}
   *
   * B is
   *   {(RetainLine:1), (Insert:"This is line 2"), Retain(6, false)}.
   *
   * The composer begins and sees B is retaining one line. It processes
   * A's components until A finishes the first line, at which point it can move
   * on to B's next component. If A's Retain did not contain the newline
   * flag, the composer would continue through to A's (Insert:"2") component
   * thinking that insertion is still on the first line. After that, it would
   * see RetainLine and think the composition is illegal since RetainLine
   * must span the entire line, but the previous component (Insert:"2") did
   * not end with a newline.
   */
  /**
   * Models a retain within a document operation. This contains the number of
   * characters to be retained, and a flag indicating that the last character
   * being retained is a newline character. This retain must not span multiple
   * lines (the characters being retained can only have one newline character.)
   */
  public interface Retain extends DocOpComponent {
    int getCount();

    boolean hasTrailingNewline();
  }

  /**
   * Models a retain through the rest of the line (or the entire line if it is
   * the only components on a line) within a document operation. The purpose of
   * this is allowing for a component that is agnostic to the actual number of
   * characters on some lines. This contains the number of lines to be retained.
   * This is the only DocOpComponent that can span multiple lines.
   *
   * The {@link #getLineCount()} must always be greater than 0.
   */
  public interface RetainLine extends DocOpComponent {
    int getLineCount();
  }

  /*
   * TODO: saw extreme weirdness when this was an enum. DevMode in
   * Chrome would overflow the stack when trying to JSON.stringify this.
   * console.logging this showed a seemingly infinite depth of Objects
   */
  public static class Type {
    public static final int DELETE = 0;
    public static final int INSERT = 1;
    public static final int RETAIN = 2;
    public static final int RETAIN_LINE = 3;
  }

  public int getType();
}
