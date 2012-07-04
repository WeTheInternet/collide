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

import static com.google.collide.shared.ot.DocOpTestUtils.*;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.server.ServerDocOpFactory;
import com.google.collide.shared.document.Document;

import junit.framework.TestCase;

/**
 * Tests various functionality related to OT ensuring that in cases where the
 * last Line of a Document is empty (has Line.getText() == ""), there is a
 * RetainLine that covers that empty Line.
 * 
 * For legacy reasons, the composer and transformer currently support inputs
 * that don't follow this requirement. However, if the inputs follow the
 * requirement, their output must also follow the requirement. That is what will
 * be tested by the test methods in this class.
 * 
 */
public class EmptyLastLineRetainLineTests extends TestCase {

  private final TerseDocOpBuilder builder = new TerseDocOpBuilder(ServerDocOpFactory.INSTANCE,
      false);

  private Document doc;

  public void testDocumentMutationsProduceEmptyLastLineRL() {

    // Empty mutations

    {
      // This is an strange edge case, but might as well cover it
      doc = Document.createFromString("");
      DocOp actual = delete(0, 0, 0);
      DocOp expected = builder.rl(1).b();
      assertDocOpEquals(expected, actual);
    }

    {
      // This is an strange edge case, but might as well cover it
      doc = Document.createFromString("");
      DocOp actual = insert(0, 0, "");
      DocOp expected = builder.rl(1).b();
      assertDocOpEquals(expected, actual);
    }

    // Mutations without newlines

    {
      // Ensure no RL
      doc = Document.createFromString("");
      DocOp actual = insert(0, 0, "a");
      DocOp expected = builder.i("a").b();
      assertDocOpEquals(expected, actual);
    }

    {
      doc = Document.createFromString("a");
      DocOp actual = delete(0, 0, 1);
      DocOp expected = builder.d("a").rl(1).b();
      assertDocOpEquals(expected, actual);
    }

    // One/two-line documents and mutations with newlines

    {
      doc = Document.createFromString("");
      DocOp actual = insert(0, 0, "\n");
      DocOp expected = builder.i("\n").rl(1).b();
      assertDocOpEquals(expected, actual);
    }

    {
      doc = Document.createFromString("\n");
      DocOp actual = delete(0, 0, 1);
      DocOp expected = builder.d("\n").rl(1).b();
      assertDocOpEquals(expected, actual);
    }

    // Multiple line documents

    {
      doc = Document.createFromString("\n\n");
      DocOp actual = insert(0, 0, "\n");
      DocOp expected = builder.i("\n").eolR(1).rl(2).b();
      assertDocOpEquals(expected, actual);
    }

    {
      doc = Document.createFromString("\n\n");
      DocOp actual = insert(0, 0, "\n\n");
      DocOp expected = builder.i("\n").i("\n").eolR(1).rl(2).b();
      assertDocOpEquals(expected, actual);
    }

    {
      doc = Document.createFromString("\n\n");
      DocOp actual = delete(0, 0, 1);
      DocOp expected = builder.d("\n").eolR(1).rl(1).b();
      assertDocOpEquals(expected, actual);
    }

    {
      doc = Document.createFromString("\n\n");
      DocOp actual = delete(0, 0, 2);
      DocOp expected = builder.d("\n").d("\n").rl(1).b();
      assertDocOpEquals(expected, actual);
    }

    // Misc

    {
      doc = Document.createFromString("a\n");
      DocOp actual = insert(0, 0, "a");
      DocOp expected = builder.i("a").eolR(2).rl(1).b();
      assertDocOpEquals(expected, actual);
    }
  }

  public void testComposer() {
    DocOp a, b;
    
    // Insert vs Delete
    a = builder.i("a").b();
    b = builder.d("a").rl(1).b();
    assertCompose(builder.rl(1).b(), a, b);
    
    a = builder.i("a\n").rl(1).b();
    b = builder.d("a\n").rl(1).b();
    assertCompose(builder.rl(1).b(), a, b);
    
    a = builder.i("a\n").rl(1).b();
    b = builder.d("a").rl(2).b();
    assertCompose(builder.i("\n").rl(1).b(), a, b);

    // Delete vs Insert
    a = builder.d("a").rl(1).b();
    b = builder.i("a").b();
    assertCompose(builder.d("a").i("a").b(), a, b);

    a = builder.d("\n").rl(1).b();
    b = builder.i("a").b();
    assertCompose(builder.d("\n").i("a").b(), a, b);

    a = builder.d("\n").rl(1).b();
    b = builder.i("\n").rl(1).b();
    assertCompose(builder.d("\n").i("\n").rl(1).b(), a, b);

    // Insert vs Insert
    a = builder.i("\n").rl(1).b();
    b = builder.i("\n").rl(2).b();
    assertCompose(builder.i("\n").i("\n").rl(1).b(), a, b);
    
    a = builder.i("a").b();
    b = builder.r(1).i("\n").rl(1).b();
    assertCompose(builder.i("a\n").rl(1).b(), a, b);

    a = builder.i("\n").rl(1).b();
    b = builder.i("a").rl(2).b();
    assertCompose(builder.i("a\n").rl(1).b(), a, b);

    // Insert vs (Retain or RetainLine) 
    a = builder.i("\n").rl(1).b();
    b = builder.eolR(1).rl(1).b();
    assertCompose(a, a, b);

    a = builder.i("\n").i("\n").rl(1).b();
    b = builder.eolR(1).eolR(1).rl(1).b();
    assertCompose(a, a, b);

    a = builder.i("\n").rl(1).b();
    b = builder.rl(2).b();
    assertCompose(a, a, b);

    a = builder.i("abc\n").rl(1).b();
    b = builder.eolR(4).rl(1).b();
    assertCompose(a, a, b);

    // Delete vs (Retain or RetainLine)
    a = builder.d("a").rl(1).b();
    b = builder.rl(1).b();
    assertCompose(a, a, b);

    a = builder.d("\n").rl(1).b();
    b = builder.rl(1).b();
    assertCompose(a, a, b);
    
    a = builder.d("abc\n").rl(1).b();
    b = builder.rl(1).b();
    assertCompose(a, a, b);

    a = builder.d("\n").d("\n").rl(1).b();
    b = builder.rl(1).b();
    assertCompose(a, a, b);
    
    // Retain vs (RetainLine or Retain)
    a = builder.eolR(1).rl(1).b();
    b = builder.rl(2).b();
    assertCompose(builder.rl(2).b(), a, b);

    a = builder.eolR(1).rl(1).b();
    assertCompose(builder.rl(2).b(), a, a);

    a = builder.eolR(1).rl(1).b();
    b = builder.rl(2).b();
    assertCompose(builder.rl(2).b(), a, b);
    
    // (Retain or RetainLine) vs Delete
    a = builder.eolR(2).rl(1).b();
    b = builder.d("a").eolR(1).rl(1).b();
    assertCompose(builder.d("a").eolR(1).rl(1).b(), a, b);

    a = builder.eolR(2).rl(1).b();
    b = builder.d("a").rl(2).b();
    assertCompose(builder.d("a").eolR(1).rl(1).b(), a, b);

    a = builder.rl(2).b();
    b = builder.d("a").eolR(1).rl(1).b();
    assertCompose(builder.d("a").eolR(1).rl(1).b(), a, b);

    a = builder.rl(2).b();
    b = builder.d("a").rl(2).b();
    assertCompose(builder.d("a").rl(2).b(), a, b);
    
    
  }

  private DocOp delete(int lineNumber, int column, int deleteCount) {
    return asDocOp(doc.deleteText(doc.getLineFinder().findLine(0).line(), column, deleteCount));
  }

  private DocOp insert(int lineNumber, int column, String text) {
    return asDocOp(doc.insertText(doc.getLineFinder().findLine(0).line(), column, text));
  }
}
