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

package com.google.collide.client.editor.renderer;

import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;

/**
 * A line based renderer strategy that uses anchors to define chunk boundaries.
 *
 *  At any given point in the document, the previous anchor of the given type
 * contains the information necessary to calculate the style of the current
 * chunk.
 */
public class PreviousAnchorRenderer implements LineRenderer {
  /**
   * Calculate the style to use for the given previous anchor.
   */
  public interface AnchorStyleGetter {
    public String getStyle(Anchor previousAnchor);
  }

  /**
   * Default style getter which simply uses the anchor's value as the style.
   */
  private static class AnchorValueIsStyle implements AnchorStyleGetter {
    @Override
    public String getStyle(Anchor previousAnchor) {
      return previousAnchor.getValue();
    }
  }

  public static final AnchorStyleGetter ANCHOR_VALUE_IS_STYLE = new AnchorValueIsStyle();

  /**
   * The render anchor is a positional anchor used to search backwards for the
   * previous diffchunk.
   */
  private static final AnchorType START_SEARCH_ANCHOR_TYPE =
      AnchorType.create(PreviousAnchorRenderer.class, "start");

  protected final Document document;
  private final AnchorType anchorType;
  private final AnchorStyleGetter styleGetter;

  private int nextChunkLength = 0;
  private int linePosition = 0;
  private Line line;
  private Anchor prevAnchor = null;

  public PreviousAnchorRenderer(
      Document document, AnchorType anchorType, AnchorStyleGetter styleGetter) {
    this.document = document;
    this.anchorType = anchorType;
    this.styleGetter = styleGetter;
  }

  @Override
  public void renderNextChunk(Target target) {
    assert (prevAnchor != null);
    assert (nextChunkLength > 0);

    String chunkStyle = styleGetter.getStyle(prevAnchor);
    target.render(nextChunkLength, chunkStyle);
    linePosition += nextChunkLength;
    if (linePosition < line.getText().length()) {
      calculateNextChunk();
    }
  }

  @Override
  public boolean resetToBeginningOfLine(Line line, int lineNumber) {
    // Reset the line rendering state
    this.line = line;
    linePosition = 0;
    nextChunkLength = 0;
    prevAnchor = null;

    calculateNextChunk();
    return nextChunkLength > 0;
  }

  @Override
  public boolean shouldLastChunkFillToRight() {
    return false;
  }

  /**
   * Using the current rendering state, calculate the length and style for the
   * next diff chunk.
   */
  private void calculateNextChunk() {
    // Get the previous anchor for this line
    AnchorManager anchorManager = document.getAnchorManager();

    if (prevAnchor == null) {
      Anchor startSearchAnchor = document.getAnchorManager().createAnchor(
          START_SEARCH_ANCHOR_TYPE, line, AnchorManager.IGNORE_LINE_NUMBER, 1);
      prevAnchor = document.getAnchorManager().getPreviousAnchor(startSearchAnchor, anchorType);
      document.getAnchorManager().removeAnchor(startSearchAnchor);
      if (prevAnchor == null) {
        return;
      }
    } else {
      // We have hit an anchor on this line already, get the next one.
      prevAnchor = anchorManager.getNextAnchor(prevAnchor, anchorType);
    }

    Anchor nextAnchor = anchorManager.getNextAnchor(prevAnchor, anchorType);
    if (nextAnchor != null && nextAnchor.getLine() == line) {
      nextChunkLength = nextAnchor.getColumn() - linePosition;
    } else {
      nextChunkLength = line.getText().length() - linePosition;
    }

    assert (nextChunkLength >= 0) : "Got a negative chunk length";
  }
}
