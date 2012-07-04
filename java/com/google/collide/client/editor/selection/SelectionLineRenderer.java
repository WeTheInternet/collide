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

package com.google.collide.client.editor.selection;

import com.google.collide.client.editor.FocusManager;
import com.google.collide.client.editor.renderer.LineRenderer;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.MathUtils;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * A line renderer that styles the lines contained in the user's selection.
 */
public class SelectionLineRenderer implements LineRenderer {

  /**
   * CssResource for the {@link SelectionLineRenderer}.
   */
  public interface Css extends CssResource {
    String selection();
    String inactiveSelection();
  }

  /**
   * ClientBundle for the {@link SelectionLineRenderer}.
   */
  public interface Resources extends ClientBundle {
    @Source("SelectionLineRenderer.css")
    Css editorSelectionLineRendererCss();
  }

  /**
   * Current chunk being rendered's position inside {@link #chunkLengths} and
   * {@link #chunkStyles}
   */
  private int curChunkIndex;

  /**
   * Length of each chunk (there are a maximum of three chunks: beginning
   * non-selected text, selected text, and trailing non-selected text)
   */
  private int[] chunkLengths = new int[3];

  /** Style for each chunk */
  private String[] chunkStyles = new String[3];

  private final Css css;
  private final FocusManager focusManager;
  private final SelectionModel selectionModel;

  SelectionLineRenderer(SelectionModel selectionModel, FocusManager focusManager, Resources res) {
    this.focusManager = focusManager;
    this.css = res.editorSelectionLineRendererCss();
    this.selectionModel = selectionModel;
  }

  @Override
  public void renderNextChunk(Target target) {
    target.render(chunkLengths[curChunkIndex], chunkStyles[curChunkIndex]);
    curChunkIndex++;
  }

  @Override
  public boolean resetToBeginningOfLine(Line line, int lineNumber) {
    if (!selectionModel.hasSelection()
        || !MathUtils.isInRangeInclusive(lineNumber, selectionModel.getSelectionBeginLineNumber(),
            selectionModel.getSelectionEndLineNumber())) {
      return false;
    }

    Position[] selection = selectionModel.getSelectionRange(false);

    /*
     * If this line is the first line of the selection, the column is the
     * selection's column. Otherwise, the column is 0 since this line is
     * entirely in the selection.
     */
    int selectionBeginColumn =
        selection[0].getLineInfo().number() == lineNumber ? selection[0].getColumn() : 0;

    // Similar to above, except for the last line
    int selectionEndColumnExclusive =
        selection[1].getLineInfo().number() == lineNumber ? selection[1].getColumn() : line
            .getText().length();

    if (selectionEndColumnExclusive == 0) {
      // This line doesn't actually have a selection
      return false;
    }

    resetChunks(line, selectionBeginColumn, selectionEndColumnExclusive);

    curChunkIndex = 0;

    return true;
  }

  @Override
  public boolean shouldLastChunkFillToRight() {
    return true;
  }

  private void resetChunks(Line line, int selectionBeginColumn, int selectionEndColumnExclusive) {
    int curChunkIndex = 0;

    if (selectionBeginColumn > 0) {
      /*
       * The selection does not start at the beginning of the line, so the first
       * chunk should be null
       */
      chunkStyles[curChunkIndex] = null;
      chunkLengths[curChunkIndex] = selectionBeginColumn;
      curChunkIndex++;
    }

    chunkStyles[curChunkIndex] =
        focusManager.hasFocus() ? css.selection() : css.inactiveSelection();
    chunkLengths[curChunkIndex] = selectionEndColumnExclusive - selectionBeginColumn;
    curChunkIndex++;

    if (selectionEndColumnExclusive < line.getText().length()) {
      chunkStyles[curChunkIndex] = null;
      chunkLengths[curChunkIndex] = line.getText().length() - selectionEndColumnExclusive;
      curChunkIndex++;
    }

    for (; curChunkIndex < chunkStyles.length; curChunkIndex++) {
      chunkStyles[curChunkIndex] = null;
      chunkLengths[curChunkIndex] = 0;
    }
  }
}
