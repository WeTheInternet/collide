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

import com.google.collide.dto.server.ServerDocOpFactory;

import junit.framework.TestCase;

/**
 *
 */
public class PositionTransformerTests extends TestCase {

  private TerseDocOpBuilder b;

  public void testDeleteDocOpScenarios() {
    {
      // Deletion before position
      PositionTransformer transformer = new PositionTransformer(0, 5);
      transformer.transform(b.d("h").b());
      assertPositionTransformerEquals(0, 4, transformer);
    }

    {
      // Deletion ending at position
      PositionTransformer transformer = new PositionTransformer(0, 1);
      transformer.transform(b.d("h").b());
      assertPositionTransformerEquals(0, 0, transformer);
    }

    {
      // Deletion containing position
      PositionTransformer transformer = new PositionTransformer(0, 1);
      transformer.transform(b.d("hello").b());
      assertPositionTransformerEquals(0, 0, transformer);
    }

    {
      // Deletion after position
      PositionTransformer transformer = new PositionTransformer(0, 0);
      transformer.transform(b.r(5).d("hello").b());
      assertPositionTransformerEquals(0, 0, transformer);
    }

    {
      // Deletion with newline before position on next line
      PositionTransformer transformer = new PositionTransformer(1, 1);
      transformer.transform(b.d("hello\n").b());
      assertPositionTransformerEquals(0, 1, transformer);
    }

    {
      // Deletion with newline after position
      PositionTransformer transformer = new PositionTransformer(0, 0);
      transformer.transform(b.r(5).d("hello\n").b());
      assertPositionTransformerEquals(0, 0, transformer);
    }

    {
      // Deletion with newline where position is on newline
      PositionTransformer transformer = new PositionTransformer(0, 1);
      transformer.transform(b.d("h\n").b());
      assertPositionTransformerEquals(0, 0, transformer);
    }

    {
      // Deletion with newline where position is on newline
      PositionTransformer transformer = new PositionTransformer(5, 1);
      transformer.transform(b.rl(3).d("\n").rl(5).b());
      assertPositionTransformerEquals(4, 1, transformer);
    }
  }

  public void testInsertDocOpScenarios() {
    {
      // Insertion before position
      PositionTransformer transformer = new PositionTransformer(0, 1);
      transformer.transform(b.i("hello").b());
      assertPositionTransformerEquals(0, 6, transformer);
    }

    {
      // Insertion at position
      PositionTransformer transformer = new PositionTransformer(0, 0);
      transformer.transform(b.i("hello").b());
      assertPositionTransformerEquals(0, 5, transformer);
    }

    {
      // Insertion after position
      PositionTransformer transformer = new PositionTransformer(0, 0);
      transformer.transform(b.r(5).i("hello").b());
      assertPositionTransformerEquals(0, 0, transformer);
    }

    {
      // Insertion with newline before position
      PositionTransformer transformer = new PositionTransformer(0, 1);
      transformer.transform(b.i("hello\n").b());
      assertPositionTransformerEquals(1, 1, transformer);
    }

    {
      // Insertion with newline before position
      PositionTransformer transformer = new PositionTransformer(4, 1);
      transformer.transform(b.i("hello\n").b());
      assertPositionTransformerEquals(5, 1, transformer);
    }

    {
      // Insertion with newline at position
      PositionTransformer transformer = new PositionTransformer(0, 0);
      transformer.transform(b.i("hello\n").b());
      assertPositionTransformerEquals(1, 0, transformer);
    }

    {
      // Insertion with newline after position
      PositionTransformer transformer = new PositionTransformer(0, 0);
      transformer.transform(b.r(5).i("hello\n").b());
      assertPositionTransformerEquals(0, 0, transformer);
    }
  }

  public void testRetainDocOpScenarios() {
    {
      // Retain before position
      PositionTransformer transformer = new PositionTransformer(0, 5);
      transformer.transform(b.r(4).b());
      assertPositionTransformerEquals(0, 5, transformer);
    }

    {
      // Retain surrounding position
      PositionTransformer transformer = new PositionTransformer(0, 5);
      transformer.transform(b.r(40).b());
      assertPositionTransformerEquals(0, 5, transformer);
    }

    {
      /*
       * Retain after position (this is an odd test case, but just ensuring the
       * r(40) doesn't affect anything)
       */
      PositionTransformer transformer = new PositionTransformer(0, 5);
      transformer.transform(b.r(10).r(40).b());
      assertPositionTransformerEquals(0, 5, transformer);
    }

    {
      // Retain with newline containing position
      PositionTransformer transformer = new PositionTransformer(0, 5);
      transformer.transform(b.eolR(40).b());
      assertPositionTransformerEquals(0, 5, transformer);
    }
  }

  public void testRetainLineDocOpScenarios() {
    {
      // Retain line before position
      PositionTransformer transformer = new PositionTransformer(4, 5);
      transformer.transform(b.rl(2).b());
      assertPositionTransformerEquals(4, 5, transformer);
    }

    {
      // Retain line surrounding position
      PositionTransformer transformer = new PositionTransformer(0, 5);
      transformer.transform(b.rl(40).b());
      assertPositionTransformerEquals(0, 5, transformer);
    }

    {
      /*
       * Retain line after position (this is an odd test case, but just ensuring
       * the rl(40) doesn't affect anything)
       */
      PositionTransformer transformer = new PositionTransformer(0, 5);
      transformer.transform(b.eolR(10).rl(40).b());
      assertPositionTransformerEquals(0, 5, transformer);
    }
  }

  @Override
  protected void setUp() throws Exception {
    b = new TerseDocOpBuilder(ServerDocOpFactory.INSTANCE, false);
  }

  private static void assertPositionTransformerEquals(int expectedLineNumber, int expectedColumn,
      PositionTransformer positionTransformer) {
    assertEquals(expectedLineNumber, positionTransformer.getLineNumber());
    assertEquals(expectedColumn, positionTransformer.getColumn());
  }
}
