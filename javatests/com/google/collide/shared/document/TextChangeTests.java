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

package com.google.collide.shared.document;

import junit.framework.TestCase;

/**
 * Tests for {@link TextChange}.
 */
public class TextChangeTests extends TestCase {

  private Document emptyDoc;

  private final String filledDocContents = "Hello\nWorld\nWoot";
  private Document filledDoc;

  public void testEndForDeletion() {
    TextChange deletion = TextChange.createDeletion(filledDoc.getFirstLine(), 0, 3, "Something");
    assertEquals(filledDoc.getFirstLine(), deletion.getEndLine());
    assertEquals(0, deletion.getEndLineNumber());
    assertEquals(3, deletion.getEndColumn());
  }

  public void testEndForInsertion() {
    TextChange insertion;

    insertion =
        TextChange.createInsertion(filledDoc.getFirstLine(), 0, 2, filledDoc.getFirstLine()
            .getNextLine(), 1, "llo\n");
    assertEquals(filledDoc.getFirstLine(), insertion.getEndLine());
    assertEquals(0, insertion.getEndLineNumber());
    assertEquals(5, insertion.getEndColumn());

    insertion =
        TextChange.createInsertion(filledDoc.getFirstLine(), 0, 2, filledDoc.getFirstLine()
            .getNextLine(), 1, "llo\nWor");
    assertEquals(filledDoc.getFirstLine().getNextLine(), insertion.getEndLine());
    assertEquals(1, insertion.getEndLineNumber());
    assertEquals(2, insertion.getEndColumn());

    insertion =
        TextChange.createInsertion(filledDoc.getFirstLine(), 0, 1, filledDoc.getFirstLine(), 0,
            "el");
    assertEquals(filledDoc.getFirstLine(), insertion.getEndLine());
    assertEquals(0, insertion.getEndLineNumber());
    assertEquals(2, insertion.getEndColumn());
  }

  @Override
  protected void setUp() throws Exception {
    emptyDoc = Document.createFromString("");
    filledDoc = Document.createFromString(filledDocContents);
  }
}
