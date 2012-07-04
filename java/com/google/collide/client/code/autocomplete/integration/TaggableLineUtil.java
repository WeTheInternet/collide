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

package com.google.collide.client.code.autocomplete.integration;

import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Line;

import javax.annotation.Nonnull;

/**
 * {@link com.google.collide.shared.TaggableLine} utility class.
 *
 */
public class TaggableLineUtil {

  private static final TaggableLine NULL_PREVIOUS_LINE = new TaggableLine() {

    @Override
    public <T> T getTag(String key) {
      return null;
    }

    @Override
    public <T> void putTag(String key, T value) {
      throw new IllegalStateException("Can't put tag to null line");
    }

    @Override
    public TaggableLine getPreviousLine() {
      throw new IllegalStateException("There is no minus 2'nd line");
    }

    @Override
    public boolean isFirstLine() {
      // It is line before the first line, so it is not first.
      return false;
    }

    @Override
    public boolean isLastLine() {
      // For sure we have at least one more line (first line).
      return false;
    }
  };

  @Nonnull
  public static TaggableLine getPreviousLine(@Nonnull Line line) {
    Line previousLine = line.getPreviousLine();

    if (previousLine != null) {
      return previousLine;
    } else {
      return NULL_PREVIOUS_LINE;
    }
  }
}
