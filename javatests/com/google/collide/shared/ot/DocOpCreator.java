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

import org.junit.Assert;

/**
 * Utility class to create document operations with one non-retain component by
 * reducing the burden of manually retaining, insert/deleting, and retaining.
 * The document operations all assume the document has a trailing newline.
 *
 */
public class DocOpCreator {

  private final DocOpBuilder builder;

  public DocOpCreator(DocOpFactory factory) {
    this.builder = new DocOpBuilder(factory, false);
  }

  /**
   * Creates a document operation that deletes the characters denoted by the
   * given range.
   *
   * @param size The initial size of the document.
   * @param location The location the characters to delete.
   * @param characters The characters to delete.
   * @return The document operation.
   */
  public DocOp delete(int size, int location, String characters) {
    assertTrailingNewline(size, location, characters);
    return builder.retain(location, false).delete(characters)
        .retain(size - location - characters.length(), true).build();
  }

  /**
   * Creates a document operation that acts as the identity on a document.
   *
   * @param size The size of the document.
   * @return The document operation.
   */
  public DocOp identity(int size) {
    return builder.retain(size, true).build();
  }

  /**
   * Creates a document operation that inserts the given characters at the given
   * location.
   *
   * @param size The initial size of the document.
   * @param location The location at which to insert characters.
   * @param characters The characters to insert.
   * @return The document operation.
   */
  public DocOp insert(int size, int location, String characters) {
    assertTrailingNewline(size, location, characters);
    return builder.retain(location, false).insert(characters).retain(size - location, true).build();
  }

  protected void assertTrailingNewline(int size, int location, String characters) {
    Assert.assertTrue("Must have trailing newline", size - location - characters.length() >= 1);
  }
}
