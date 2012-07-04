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
package com.google.collide.client.workspace.outline;

import static com.google.collide.client.workspace.outline.CssOutlineParser.findCutTailIndex;
import static com.google.collide.client.workspace.outline.OutlineNode.OutlineNodeType.FUNCTION;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link CssOutlineParser}.
 */
public class CssOutlineParserTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testCutTail() {
    JsonArray<OutlineNode> nodes = JsonCollections.createArray();
    for (int i = 1; i <= 7; i++) {
      for (int j = 0; j < i; j++) {
        nodes.add(new OutlineNode("", FUNCTION, null, i, 0));
      }
    }

    assertEquals(0, findCutTailIndex(nodes, 0));

    // f(x) = x * (x - 1) / 2
    assertEquals(0, findCutTailIndex(nodes, 1));
    assertEquals(1, findCutTailIndex(nodes, 2));
    assertEquals(3, findCutTailIndex(nodes, 3));
    assertEquals(6, findCutTailIndex(nodes, 4));
    assertEquals(10, findCutTailIndex(nodes, 5));
    assertEquals(15, findCutTailIndex(nodes, 6));
    assertEquals(21, findCutTailIndex(nodes, 7));
    assertEquals(28, findCutTailIndex(nodes, 8));
  }
}
