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

import static com.google.collide.shared.ot.DocOpApplierTests.Operation.DELETE;
import static com.google.collide.shared.ot.DocOpApplierTests.Operation.INSERT;

import com.google.collide.dto.server.ServerDocOpFactory;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.TextChange;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests for {@link DocOpApplier}.
 *
 */
public class DocOpApplierTests extends TestCase {

  public enum Operation {
    DELETE, INSERT
  }

  private class MockDocumentMutator implements DocumentMutator {

    private int index;
    private List<Operation> operations = Lists.newArrayList();
    private List<Position> positions = Lists.newArrayList();
    private List<String> texts = Lists.newArrayList();

    public MockDocumentMutator(Object... alternatingPositionOperationAndText) {
      for (int i = 0; i < alternatingPositionOperationAndText.length;) {
        int lineNumber = (Integer) alternatingPositionOperationAndText[i++];
        int column = (Integer) alternatingPositionOperationAndText[i++];
        positions.add(createPosition(lineNumber, column));
        operations.add((Operation) alternatingPositionOperationAndText[i++]);
        texts.add((String) alternatingPositionOperationAndText[i++]);
      }
    }

    private Position createPosition(int lineNumber, int column) {
      return new Position(doc.getLineFinder().findLine(lineNumber), column);
    }

    @Override
    public TextChange deleteText(Line line, int column, int deleteCount) {
      throw new IllegalStateException(
          "DocOpApplier knows its line number and should not call the inefficient deleteText");
    }

    @Override
    public TextChange deleteText(Line line, int lineNumber, int column, int deleteCount) {
      assertPosition(line, lineNumber, column);
      assertEquals(operations.get(index), DELETE);
      assertEquals(texts.get(index).length(), deleteCount);
      index++;

      return doc.deleteText(line, lineNumber, column, deleteCount);
    }

    @Override
    public TextChange insertText(Line line, int column, String text) {
      throw new IllegalStateException(
          "DocOpApplier knows its line number and should not call the inefficient insertText");
    }

    @Override
    public TextChange insertText(Line line, int lineNumber, int column, String text) {
      assertPosition(line, lineNumber, column);
      assertEquals(operations.get(index), INSERT);
      assertEquals(texts.get(index), text);
      index++;

      return doc.insertText(line, lineNumber, column, text);
    }

    @Override
    public TextChange insertText(Line line, int lineNumber, int column, String text,
        boolean canReplaceSelection) {

      // This mutator impl does not care to replace the selection
      return insertText(line, lineNumber, column, text);
    }

    private void assertPosition(Line line, int lineNumber, int column) {
      assertEquals(positions.get(index).getLine(), line);
      assertEquals(positions.get(index).getLineNumber(), lineNumber);
      assertEquals(positions.get(index).getColumn(), column);
    }
  }

  private Document doc = Document.createFromString("");

  private TerseDocOpBuilder b = new TerseDocOpBuilder(ServerDocOpFactory.INSTANCE, false);

  public void testVerySimpleInsertion() {
    DocOpApplier.apply(b.i("a").b(), doc, new MockDocumentMutator(0, 0, INSERT, "a"));
  }

  public void testMultilineSimpleInsertion() {
    DocOpApplier.apply(b.i("a\n").i("b\n").b(), doc,
        new MockDocumentMutator(0, 0, INSERT, "a\nb\n"));
  }

  public void testMultilineSimpleMutations() {
    DocOpApplier.apply(b.i("a\n").i("b\n").b(), doc,
        new MockDocumentMutator(0, 0, INSERT, "a\nb\n"));
    DocOpApplier.apply(b.d("a\n").d("b\n").i("c\n").i("d").b(), doc, new MockDocumentMutator(0, 0,
        DELETE, "a\nb\n", 0, 0, INSERT, "c\nd"));

  }
}
