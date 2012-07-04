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

package com.google.collide.client.documentparser;

import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * An adapter for {@link DocumentParser.Listener} that performs only async
 * parsing while collecting some line-dependent data.
 */
public abstract class AsyncParser<T extends AsyncParser.LineAware>
    implements DocumentParser.Listener {

  /**
   * Data item base interface. It is important that they are associated with
   * lines because we clear data for lines that are invalidated and need to
   * be parsed again.
   */
  public static interface LineAware {

    /**
     * @return line number this data belongs to
     */
    int getLineNumber();
  }

  /**
   * Called before parsing has started.
   */
  protected void onBeforeParse() {}

  /**
   * Called when the data parsed earlier is discarded. Implementations
   * should clear everything associated with this data. This method may
   * be called on any time and should not depend on parsing cycle state.
   *
   * @param cleanedData data items that will be deleted
   */
  protected void onCleanup(JsonArray<T> cleanedData) {}

  /**
   * Called when a line is being parsed. This is guaranteed to be called after
   * {@link #onBeforeParse} and before {@link #onAfterParse}.
   *
   * @param line line being parsed
   * @param lineNumber line number being parsed
   * @param tokens tokens collected on the line
   */
  protected void onParseLine(Line line, int lineNumber, JsonArray<Token> tokens) {}

  /**
   * Called just after chunk parsing has finished. This does not mean that
   * parsing for the whole file has finished. You have to use what you get
   * here.
   *
   * @param nodes resulting array of all nodes collected so far for the whole
   *        file
   */
  protected void onAfterParse(JsonArray<T> nodes) {}

  /**
   * Flag that prevents work after instance was cleaned.
   */
  private boolean detached;

  /**
   * Flag indicating that block of lines is being parsed
   */
  private boolean iterating;

  /**
   * Array of data items.
   *
   * When new block of lines is parsed, this list is truncated.
   */
  private JsonArray<T> nodes = JsonCollections.createArray();

  @Override
  public final void onDocumentLineParsed(
      Line line, int lineNumber, @Nonnull JsonArray<Token> tokens) {
    if (detached) {
      return;
    }
    if (!iterating) {
      return;
    }
    onParseLine(line, lineNumber, tokens);
  }

  @Override
  public final void onIterationFinish() {
    Preconditions.checkState(iterating);
    if (detached) {
      return;
    }
    iterating = false;
    onAfterParse(nodes);
  }

  @Override
  public final void onIterationStart(int lineNumber) {
    Preconditions.checkState(!iterating);
    if (detached) {
      return;
    }
    iterating = true;
    cutTail(lineNumber);
    onBeforeParse();
  }

  /**
   * Adds given data to collected set while parsing.
   *
   * @param dataNode data to add
   */
  protected final void addData(T dataNode) {
    nodes.add(dataNode);
  }

  private void cutTail(int lineNumber) {
    if (nodes.size() != 0) {
      int cutTailIndex = findCutTailIndex(nodes, lineNumber);
      if (cutTailIndex < nodes.size()) {
        JsonArray<T> tail = nodes.splice(cutTailIndex, nodes.size() - cutTailIndex);
        onCleanup(tail);
      }
    }
  }

  /**
   * Find index for tail truncation in nodes array.
   *
   * <p>We support nodes array sorted by line number.
   * Now we use that property in modified binary search.
   */
  @VisibleForTesting
  public static <T extends LineAware> int findCutTailIndex(JsonArray<T> nodes,
      int lineNumber) {
    int low = 0;
    int high = nodes.size() - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      if (lineNumber > nodes.get(mid).getLineNumber()) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return low;
  }

  /**
   * Cleanup object. Overriding implementations should call this method before
   * any custom cleanup.
   *
   * After cleanup is invoked this instance should never be used.
   */
  public void cleanup() {
    cutTail(0);
    detached = true;
  }
}
