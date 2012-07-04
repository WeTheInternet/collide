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

import com.google.collide.dto.DocOp;
import com.google.collide.dto.shared.DocOpFactory;

/**
 * Helper to create document operations via a builder pattern.
 *
 */
public class DocOpBuilder {

  private DocOpCapturer capturer;
  private final Runnable createCapturer;

  public DocOpBuilder(final DocOpFactory factory, final boolean shouldCompact) {
    createCapturer = new Runnable() {
      @Override
      public void run() {
        capturer = new DocOpCapturer(factory, shouldCompact);
      }
    };

    createCapturer.run();
  }

  /**
   * Builds and returns the document operation, and prepares the builder for
   * another building sequence.
   */
  public DocOp build() {
    DocOp op = capturer.getDocOp();
    createCapturer.run();
    return op;
  }

  /**
   * Adds a delete component for the given {@code text} to the document
   * operation being built.
   */
  public DocOpBuilder delete(String text) {
    capturer.delete(text);
    return this;
  }

  /**
   * Adds an insert component for the given {@code text} to the document
   * operation being built.
   */
  public DocOpBuilder insert(String text) {
    capturer.insert(text);
    return this;
  }

  /**
   * Adds a retain component for the given number of characters to the document
   * operation being built.
   */
  public DocOpBuilder retain(int count, boolean hasTrailingNewline) {
    capturer.retain(count, hasTrailingNewline);
    return this;
  }

  /**
   * Adds a retain line component for the given number of lines to the document
   * operation being built.
   */
  public DocOpBuilder retainLine(int lineCount) {
    capturer.retainLine(lineCount);
    return this;
  }
}
