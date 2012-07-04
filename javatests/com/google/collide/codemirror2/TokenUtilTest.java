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

package com.google.collide.codemirror2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.Pair;
import com.google.collide.shared.util.JsonCollections;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit4 test for {@link TokenUtil}.
 */
public class TokenUtilTest {

  private JsonArray<Pair<Integer, String>> modes;

  @Before
  public void setUp() {
    JsonArray<Token> tokens = JsonCollections.createArray();
    tokens.add(new Token("html", null, "hhhhhhhh"));
    tokens.add(new Token("css", null, "ccc"));
    tokens.add(new Token("css", null, "CC"));
    tokens.add(new Token("javascript", null, "jjjjj"));
    tokens.add(new Token("html", null, "h"));
    modes = TokenUtil.buildModes("#", tokens);
  }

  @Test
  public void findModeForColumn() {
    assertThrows("Column should be >= 0 but was -1", -1);

    // Let the mode at column = 0 be the mode of the first tag.
    // This makes sense because visually new line is sufficiently separate
    // from the previous line so that whatever is the mode of the first tag
    // on this line determines the mode of the first column.
    assertEquals("html", TokenUtil.findModeForColumn("html", modes, 0));
    assertEquals("html", TokenUtil.findModeForColumn("html", modes, 1));
    assertEquals("html", TokenUtil.findModeForColumn("html", modes, 8));
    assertEquals("css", TokenUtil.findModeForColumn("html", modes, 9));
    assertEquals("css", TokenUtil.findModeForColumn("html", modes, 10));
    assertEquals("css", TokenUtil.findModeForColumn("html", modes, 11));
    assertEquals("css", TokenUtil.findModeForColumn("html", modes, 12));
    assertEquals("css", TokenUtil.findModeForColumn("html", modes, 13));
    assertEquals("javascript", TokenUtil.findModeForColumn("html", modes, 14));
    assertEquals("javascript", TokenUtil.findModeForColumn("html", modes, 16));
    assertEquals("html", TokenUtil.findModeForColumn("html", modes, 19));
    assertEquals("html", TokenUtil.findModeForColumn("html", modes, 100));
    assertEquals("html", TokenUtil.findModeForColumn("html",
        JsonCollections.<Pair<Integer, String>>createArray(), 0));
    assertEquals("html", TokenUtil.findModeForColumn("html",
        JsonCollections.<Pair<Integer, String>>createArray(), 1));
  }

  @Test
  public void buildModes() {
    assertEquals(4, modes.size());
    assertEquals(0, modes.get(0).first.intValue());
    assertEquals("html", modes.get(0).second);
    assertEquals(8, modes.get(1).first.intValue());
    assertEquals("css", modes.get(1).second);
    assertEquals(13, modes.get(2).first.intValue());
    assertEquals("javascript", modes.get(2).second);
    assertEquals(18, modes.get(3).first.intValue());
    assertEquals("html", modes.get(3).second);
    assertTrue(TokenUtil.buildModes("html", JsonCollections.<Token>createArray()).isEmpty());
  }

  @Test
  public void addPlaceholders() {
    JsonStringMap<JsonArray<Token>> splitTokenMap = JsonCollections.<JsonArray<Token>>createMap();
    splitTokenMap.put("a", JsonCollections.<Token>createArray());
    splitTokenMap.put("b", JsonCollections.<Token>createArray());
    splitTokenMap.put("c", JsonCollections.<Token>createArray());

    TokenUtil.addPlaceholders("c", splitTokenMap, 4);
    assertEquals(3, splitTokenMap.size());
    assertEquals(1, splitTokenMap.get("a").size());

    Token tokenA = splitTokenMap.get("a").get(0);
    assertEquals(TokenType.WHITESPACE, tokenA.getType());
    assertEquals("a", tokenA.getMode());
    assertEquals("    ", tokenA.getValue());
    assertEquals(1, splitTokenMap.get("b").size());

    Token tokenB = splitTokenMap.get("b").get(0);
    assertEquals(TokenType.WHITESPACE, tokenB.getType());
    assertEquals("b", tokenB.getMode());
    assertEquals("    ", tokenB.getValue());

    assertEquals(0, splitTokenMap.get("c").size());
  }

  private void assertThrows(String expectedMessage, int column) {
    try {
      TokenUtil.findModeForColumn("html", modes, column);
      fail("Expected to throw an exception");
    } catch (IllegalArgumentException e) {
      // Expected exception
      assertEquals(expectedMessage, e.getMessage());
    }
  }
}
