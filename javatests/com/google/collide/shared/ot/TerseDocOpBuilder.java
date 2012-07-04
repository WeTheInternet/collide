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
 * A document operation builder with a terse API aimed at improving productivity
 * when writing tests.
 */
public class TerseDocOpBuilder {

  private final DocOpBuilder docOpBuilder;

  /**
   * @param shouldCompact whether to compact similar document operation
   *        components (e.g. R(5) followed by R(5) would be compacted to R(10))
   */
  public TerseDocOpBuilder(DocOpFactory factory, boolean shouldCompact) {
    docOpBuilder = new DocOpBuilder(factory, shouldCompact);
  }

  /**
   * @see DocOpBuilder#build()
   */
  public DocOp b() {
    return docOpBuilder.build();
  }

  /**
   * @see DocOpBuilder#delete(String)
   */
  public TerseDocOpBuilder d(String text) {
    docOpBuilder.delete(text);
    return this;
  }

  /**
   * Adds a retain component for {@code count} characters where the last
   * character is a newline.
   *
   * Note: "eol" stands for end-of-line.
   *
   * @see DocOpBuilder#retain(int, boolean)
   * @see #r(int)
   */
  public TerseDocOpBuilder eolR(int count) {
    docOpBuilder.retain(count, true);
    return this;
  }

  /**
   * @see DocOpBuilder#insert(String)
   */
  public TerseDocOpBuilder i(String text) {
    docOpBuilder.insert(text);
    return this;
  }

  /**
   * Adds a retain component for {@code count} characters where the last
   * character is NOT a newline.
   *
   * @see DocOpBuilder#retain(int, boolean)
   * @see #eolR(int)
   */
  public TerseDocOpBuilder r(int count) {
    docOpBuilder.retain(count, false);
    return this;
  }

  /**
   * @see DocOpBuilder#retainLine(int)
   */
  public TerseDocOpBuilder rl(int lineCount) {
    docOpBuilder.retainLine(lineCount);
    return this;
  }
}
