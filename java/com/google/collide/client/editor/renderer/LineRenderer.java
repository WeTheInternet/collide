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

import javax.annotation.Nullable;

/**
 * An interface for a renderer that renders a line at a time.
 */
public interface LineRenderer {

  /**
   * Receives the style to render the next {@code characterCount} characters.
   * This is how the renderer "renders".
   */
  public interface Target {
    void render(int characterCount, @Nullable String styleName);
  }

  /**
   * Called when the LineRenderer should render its next chunk by calling
   * {@link Target#render(int, String)} on {@code target}.
   */
  void renderNextChunk(Target target);

  /**
   * Called when a line is about to be rendered.
   *
   * @return true if this LineRenderer wants to participate in the rendering of
   *         this line
   */
  boolean resetToBeginningOfLine(Line line, int lineNumber);

  /**
   * @return true if last chunk (\n character) style should fill to right until
   *         visible end of the editor line
   */
  boolean shouldLastChunkFillToRight();
}
