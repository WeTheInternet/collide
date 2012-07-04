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

import static com.google.collide.dto.DocOpComponent.Type.*;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocOpComponent;
import com.google.collide.dto.shared.DocOpFactory;
import com.google.collide.json.shared.JsonArray;

/**
 * Helper to create document operations via a method call for each component.
 *
 */
public class DocOpCapturer implements DocOpCursor {

  private final JsonArray<DocOpComponent> components;

  private final DocOpFactory factory;

  private final DocOp op;

  private int curComponentType = -1;

  /** Only valid if the current component is delete or insert */
  private String curText = "";

  /** Only valid if the current component is retain or retain line */
  private int curCount;

  /** Only valid if the current component is retain */
  private boolean curHasTrailingNewline;

  private boolean curLineHasNonRetainComponents = false;

  private final boolean shouldCompact;

  /**
   * @param shouldCompact whether similar adjacent components should be
   *        compacted into a single component
   */
  public DocOpCapturer(DocOpFactory factory, boolean shouldCompact) {
    this.factory = factory;
    this.shouldCompact = shouldCompact;

    op = factory.createDocOp();
    components = op.getComponents();
  }

  @Override
  public void delete(String text) {
    checkAndCommitIfRequired(DELETE);
    curComponentType = DELETE;
    curText += text;
    // Reset the variable if starting a new line
    curLineHasNonRetainComponents = !text.endsWith("\n");
  }

  public DocOp getDocOp() {
    commitCurrentComponent();
    return op;
  }

  @Override
  public void insert(String text) {
    checkAndCommitIfRequired(INSERT);
    curComponentType = INSERT;
    curText += text;
    // Reset the variable if starting a new line
    curLineHasNonRetainComponents = !text.endsWith("\n");
  }

  @Override
  public void retain(int count, boolean hasTrailingNewline) {

    if (shouldCompact && !curLineHasNonRetainComponents && hasTrailingNewline) {
      /*
       * Since this line only had retain(s), we can convert it to the more terse
       * retain line
       */
      
      /*
       * curXxx refers to the state prior to the retain we're handling right
       * now. If the ongoing component was a retain, throw it away.
       */
      if (curComponentType == RETAIN && !curHasTrailingNewline) {
        discardCurrentComponent();
      }
      
      retainLine(1);
      return;
    }

    checkAndCommitIfRequired(RETAIN);
    curComponentType = RETAIN;
    curCount += count;
    curHasTrailingNewline = hasTrailingNewline;

    if (curHasTrailingNewline) {
      curLineHasNonRetainComponents = false;
    }
  }

  @Override
  public void retainLine(int lineCount) {
    checkAndCommitIfRequired(RETAIN_LINE);
    curComponentType = RETAIN_LINE;
    curCount += lineCount;
    curLineHasNonRetainComponents = false;
  }

  private void checkAndCommitIfRequired(int newType) {
    /*-
     * Rules for committing:
     * - This instance does NOT compact components of the same type
     *   into a single component, or
     * - The component about to be captured is a different type from the
     *   currently compacting component type, or
     * - The new and current component type is the same and not RETAIN_LINE,
     *   and it ends with a newline
     */
    if (!shouldCompact || curComponentType != newType
        || (curComponentType == RETAIN && curHasTrailingNewline)
        || (curComponentType == DELETE && curText.endsWith("\n"))
        || (curComponentType == INSERT && curText.endsWith("\n"))) {
      commitCurrentComponent();
    }
  }

  private void commitCurrentComponent() {

    if (curComponentType == -1) {
      return;
    }

    switch (curComponentType) {
      case INSERT:
        components.add(factory.createInsert(curText));
        break;

      case DELETE:
        components.add(factory.createDelete(curText));
        break;

      case RETAIN:
        components.add(factory.createRetain(curCount, curHasTrailingNewline));
        break;

      case RETAIN_LINE:
        components.add(factory.createRetainLine(curCount));
        break;

      default:
        throw new IllegalStateException("Cannot handle component type with ordinal "
            + curComponentType);
    }

    discardCurrentComponent();
  }

  private void discardCurrentComponent() {
    curComponentType = -1;
    curHasTrailingNewline = false;
    curText = "";
    curCount = 0;
  }
}
