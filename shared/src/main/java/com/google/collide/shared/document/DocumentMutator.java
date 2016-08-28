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

import javax.annotation.Nullable;

/**
 * An interface that allows mutation of a document.
 */
public interface DocumentMutator {

  /**
   * Deletes the text from the given start point ({@code line} and
   * {@code column}) with the given length ({@code deleteCount}). If the deleted
   * text spans multiple lines, the {@link Line Lines} where the deletion
   * started and ended may be joined and the deleted lines will be detached from
   * the document.
   * 
   * @param line the line containing the begin position for the delete
   * @param column the column (inclusive) where the delete will begin
   * @param deleteCount the number of characters (including newlines) to delete
   * @return the change that led to the deletion of the text, or {@code null}
   *         if no changes applied
   */
  @Nullable
  TextChange deleteText(Line line, int column, int deleteCount);

  /**
   * Similar to {@link DocumentMutator#deleteText(Line, int, int)} but accepts a
   * line number for more efficient deletion.
   *
   * @return {@code null} if no changes applied
   */
  @Nullable
  TextChange deleteText(Line line, int lineNumber, int column, int deleteCount);

  /**
   * Similar to
   * {@link DocumentMutator#insertText(Line, int, int, String, boolean)} but
   * uses the default behavior.
   *
   * @return {@code null} if no changes applied
   */
  @Nullable
  TextChange insertText(Line line, int column, String text);

  /**
   * Similar to {@link DocumentMutator#insertText(Line, int, String)} but
   * accepts a line number for more efficient insertion.
   *
   * @return {@code null} if no changes applied
   */
  @Nullable
  TextChange insertText(Line line, int lineNumber, int column, String text);

  /**
   * Inserts the text starting at the given {@code line} and {@code column}. If
   * the text spans multiple lines, multiple {@link Line Lines} will be created.
   *
   * @param canReplaceSelection whether the mutator is allowed to replace the
   *        selection (if it exists) with the given text. Passing true does not
   *        guarantee the mutator will choose to replace the selection; passing
   *        false guarantees the mutator will never replace the selection
   * @return the change that led to the insertion of text; if the selection was
   *         replaced, this will only be the insertion text change, not the
   *         deletion text change; if selection was deleted, but nothing was
   *         inserted, then deletion text change is returned;
   *         {@code null} if no changes applied
   */
  @Nullable
  TextChange insertText(Line line, int lineNumber, int column, String text,
      boolean canReplaceSelection);
}
