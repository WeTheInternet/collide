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

import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.Anchor;

/**
 * Implements a {@link LineRenderer} that wraps a given area in the document
 * with an element with the given class name.
 */
public abstract class SingleChunkLineRenderer implements LineRenderer {

  /**
   * Creates a new renderer using line/column numbers (prone to collaboration
   * issues).
   *
   * @param startLine the first line to render
   * @param startColumn the first column to render
   * @param endLine the last line to render (inclusive)
   * @param endColumn the last column to render (inclusive)
   * @param className the CSS class to apply
   */
  public static SingleChunkLineRenderer create(final int startLine, final int startColumn,
      final int endLine, final int endColumn, String className) {

    return new SingleChunkLineRenderer(className) {
      @Override
      public int startLine() {
        return startLine;
      }

      @Override
      public int startColumn() {
        return startColumn;
      }

      @Override
      public int endLine() {
        return endLine;
      }

      @Override
      public int endColumn() {
        return endColumn;
      }
    };
  }

  /**
   * Creates a new renderer using anchors.
   *
   * @param startAnchor the rendering start point.
   * @param endAnchor the last part to render (note that whatever line or
   *        character the anchor is at will also be rendered.
   * @param className the CSS class name to apply
   */
  public static SingleChunkLineRenderer create(final Anchor startAnchor, final Anchor endAnchor,
      String className) {

    return new SingleChunkLineRenderer(className) {
      @Override
      public int startLine() {
        return startAnchor.getLineNumber();
      }

      @Override
      public int startColumn() {
        return startAnchor.getColumn();
      }

      @Override
      public int endLine() {
        return endAnchor.getLineNumber();
      }

      @Override
      public int endColumn() {
        return endAnchor.getColumn();
      }
    };
  }

  private final String className;

  private int currentLineNumber;
  private int currentLineLength;
  private int linePosition;

  private SingleChunkLineRenderer(String className) {
    this.className = className;
  }

  public abstract int startLine();

  public abstract int startColumn();

  public abstract int endLine();

  public abstract int endColumn();

  @Override
  public void renderNextChunk(Target target) {
    if (currentLineNumber < startLine() || currentLineNumber > endLine()) {
      // Out of the chunk to be rendered.
      render(target, currentLineLength - linePosition, null);
    } else if (currentLineNumber == startLine() && linePosition < startColumn()) {
      // Still out of the chunk to be rendered.
      render(target, startColumn() - linePosition, null);
    } else if (currentLineNumber == endLine() && linePosition > endColumn()) {
      // Right after the chunk was rendered.
      render(target, currentLineLength - linePosition, null);
    } else if (currentLineNumber == endLine()) {
      // Chunk ends at the current line.
      render(target, endColumn() + 1 - linePosition, className);
    } else {
      // The rest of the line belongs to the chunk.
      render(target, currentLineLength - linePosition, className);
    }
  }

  private void render(LineRenderer.Target target, int characterCount, String styleName) {
    target.render(characterCount, styleName);
    linePosition += characterCount;
  }

  @Override
  public boolean resetToBeginningOfLine(Line line, int lineNumber) {
    if (lineNumber < startLine() || lineNumber > endLine()) {
      return false;
    }

    this.currentLineNumber = lineNumber;
    this.currentLineLength = line.getText().length();
    this.linePosition = 0;
    return true;
  }

  @Override
  public boolean shouldLastChunkFillToRight() {
    return false;
  }
}
