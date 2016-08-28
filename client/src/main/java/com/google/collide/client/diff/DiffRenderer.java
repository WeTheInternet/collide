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

package com.google.collide.client.diff;

import com.google.collide.client.editor.renderer.PreviousAnchorRenderer;
import com.google.collide.dto.DiffChunkResponse;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * A @{link {@link PreviousAnchorRenderer} for diff chunks
 */
public class DiffRenderer extends PreviousAnchorRenderer {

  private static final AnchorType DIFF_CHUNK_ANCHOR_TYPE =
      AnchorType.create(DiffRenderer.class, "chunk");
  private final String diffBlockStyle;
  private final JsonStringMap<String> chunkStyles;
  private boolean isBeforeFile;

  public DiffRenderer(Document document, Resources resources, boolean isBeforeFile) {
    super(document, DIFF_CHUNK_ANCHOR_TYPE, PreviousAnchorRenderer.ANCHOR_VALUE_IS_STYLE);
    chunkStyles = JsonCollections.createMap();
    Css css = resources.diffRendererCss();
    chunkStyles.put(DiffChunkResponse.DiffType.ADDED_LINE.toString(), css.addedLine());
    chunkStyles.put(DiffChunkResponse.DiffType.REMOVED_LINE.toString(), css.removedLine());
    chunkStyles.put(DiffChunkResponse.DiffType.CHANGED_LINE.toString(), css.changedLine());
    chunkStyles.put(DiffChunkResponse.DiffType.ADDED.toString(), css.added());
    chunkStyles.put(DiffChunkResponse.DiffType.CHANGED.toString(), css.changed());
    chunkStyles.put(DiffChunkResponse.DiffType.REMOVED.toString(), css.removed());
    chunkStyles.put(DiffChunkResponse.DiffType.UNCHANGED.toString(), css.unchanged());
    diffBlockStyle = css.diffBlock();
    this.isBeforeFile = isBeforeFile;
  }

  public interface Css extends CssResource {
    String addedLine();

    String removedLine();

    String changedLine();

    String added();

    String changed();

    String removed();

    String unchanged();

    String diffBlock();
  }

  public interface Resources extends ClientBundle {
    @Source("DiffRenderer.css")
    Css diffRendererCss();
  }

  /**
   * Append a diff chunk to the document.
   *
   * @param chunkType the type of the diff chunk
   * @param chunkText the contents of the diff chunk
   */
  void addDiffChunk(DiffChunkResponse.DiffType chunkType, String chunkText) {
    if (!chunkText.isEmpty()) {
      Line lastLine = document.getLastLine();
      int lastColumn = lastLine.getText().length();

      document.insertText(lastLine, document.getLastLineNumber(), lastColumn, chunkText);
      Anchor anchor = document.getAnchorManager().createAnchor(
          DIFF_CHUNK_ANCHOR_TYPE, lastLine, AnchorManager.IGNORE_LINE_NUMBER, lastColumn);
      anchor.setValue(chunkStyles.get(chunkType.toString()));
      // TODO Below is temp fix to ensure
      // "Left should never have Green. Right should never have Red"
      if (isBeforeFile) {
        if (chunkType == DiffChunkResponse.DiffType.ADDED_LINE
            || chunkType == DiffChunkResponse.DiffType.ADDED) {
          anchor.setValue(diffBlockStyle);
        } else if (chunkType == DiffChunkResponse.DiffType.CHANGED) {
          anchor.setValue(chunkStyles.get(DiffChunkResponse.DiffType.REMOVED.toString()));
        } else if (chunkType == DiffChunkResponse.DiffType.CHANGED_LINE) {
          anchor.setValue(chunkStyles.get(DiffChunkResponse.DiffType.REMOVED_LINE.toString()));
        }
      } else {
        if (chunkType == DiffChunkResponse.DiffType.REMOVED
            || chunkType == DiffChunkResponse.DiffType.REMOVED_LINE) {
          anchor.setValue(diffBlockStyle);
        } else if (chunkType == DiffChunkResponse.DiffType.CHANGED) {
          anchor.setValue(chunkStyles.get(DiffChunkResponse.DiffType.ADDED.toString()));
        } else if (chunkType == DiffChunkResponse.DiffType.CHANGED_LINE) {
          anchor.setValue(chunkStyles.get(DiffChunkResponse.DiffType.ADDED_LINE.toString()));
        }
      }
    }
  }
}
