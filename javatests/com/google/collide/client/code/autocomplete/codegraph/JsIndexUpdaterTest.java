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

package com.google.collide.client.code.autocomplete.codegraph;

import com.google.collide.client.code.autocomplete.codegraph.js.JsCodeScope;
import com.google.collide.client.code.autocomplete.codegraph.js.JsIndexUpdater;
import com.google.collide.client.code.autocomplete.integration.TaggableLineUtil;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.JsonCollections;

/**
 * Test cases for {@link JsIndexUpdater}
 *
 */
public class JsIndexUpdaterTest extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testSimpleCases() {
    String mode = SyntaxType.JS.getName();

    String text = ""
        + "function a(b, c) {\n"
        + "  d.prototype = function() {\n"
        + "  }\n"
        + "}\n"
        + "var e = {\n"
        + "  f : function ( ) {\n"
        + "    callMyFunctionWithCallback(function(/* Knock-knock! */) {\n";
    Document document = Document.createFromString(text);
    Line line = document.getFirstLine();
    TaggableLine previousLine;

    JsonArray<Token> tokens1 = JsonCollections.createArray();
    tokens1.add(new Token(mode, TokenType.KEYWORD, "function"));
    tokens1.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens1.add(new Token(mode, TokenType.VARIABLE, "a"));
    tokens1.add(new Token(mode, TokenType.NULL, "("));
    tokens1.add(new Token(mode, TokenType.VARIABLE, "b"));
    tokens1.add(new Token(mode, TokenType.NULL, ","));
    tokens1.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens1.add(new Token(mode, TokenType.VARIABLE, "c"));
    tokens1.add(new Token(mode, TokenType.NULL, ")"));
    tokens1.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens1.add(new Token(mode, TokenType.NULL, "{"));
    tokens1.add(new Token(mode, TokenType.NEWLINE, "\n"));

    JsonArray<Token> tokens2 = JsonCollections.createArray();
    tokens2.add(new Token(mode, TokenType.WHITESPACE, "  "));
    tokens2.add(new Token(mode, TokenType.VARIABLE, "d"));
    tokens2.add(new Token(mode, TokenType.NULL, "."));
    tokens2.add(new Token(mode, TokenType.VARIABLE, "prototype"));
    tokens2.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens2.add(new Token(mode, TokenType.NULL, "="));
    tokens2.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens2.add(new Token(mode, TokenType.KEYWORD, "function"));
    tokens2.add(new Token(mode, TokenType.NULL, "("));
    tokens2.add(new Token(mode, TokenType.NULL, ")"));
    tokens2.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens2.add(new Token(mode, TokenType.NULL, "{"));
    tokens2.add(new Token(mode, TokenType.NEWLINE, "\n"));

    JsonArray<Token> tokens3 = JsonCollections.createArray();
    tokens3.add(new Token(mode, TokenType.WHITESPACE, "  "));
    tokens3.add(new Token(mode, TokenType.NULL, "}"));
    tokens3.add(new Token(mode, TokenType.NEWLINE, "\n"));

    JsonArray<Token> tokens4 = JsonCollections.createArray();
    tokens4.add(new Token(mode, TokenType.NULL, "}"));
    tokens4.add(new Token(mode, TokenType.NEWLINE, "\n"));

    JsonArray<Token> tokens5 = JsonCollections.createArray();
    tokens5.add(new Token(mode, TokenType.KEYWORD, "var"));
    tokens5.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens5.add(new Token(mode, TokenType.DEF, "e"));
    tokens5.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens5.add(new Token(mode, TokenType.NULL, "="));
    tokens5.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens5.add(new Token(mode, TokenType.NULL, "{"));
    tokens5.add(new Token(mode, TokenType.NEWLINE, "\n"));

    JsonArray<Token> tokens6 = JsonCollections.createArray();
    tokens6.add(new Token(mode, TokenType.WHITESPACE, "  "));
    tokens6.add(new Token(mode, TokenType.PROPERTY, "f"));
    tokens6.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens6.add(new Token(mode, TokenType.NULL, ":"));
    tokens6.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens6.add(new Token(mode, TokenType.KEYWORD, "function"));
    tokens6.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens6.add(new Token(mode, TokenType.NULL, "("));
    tokens6.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens6.add(new Token(mode, TokenType.NULL, ")"));
    tokens6.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens6.add(new Token(mode, TokenType.NULL, "{"));
    tokens6.add(new Token(mode, TokenType.NEWLINE, "\n"));

    JsonArray<Token> tokens7 = JsonCollections.createArray();
    tokens7.add(new Token(mode, TokenType.WHITESPACE, "  "));
    tokens7.add(new Token(mode, TokenType.VARIABLE, "callMyFunctionWithCallback"));
    tokens7.add(new Token(mode, TokenType.NULL, "("));
    tokens7.add(new Token(mode, TokenType.KEYWORD, "function"));
    tokens7.add(new Token(mode, TokenType.NULL, "("));
    tokens7.add(new Token(mode, TokenType.COMMENT, "/* Knock-knock! */"));
    tokens7.add(new Token(mode, TokenType.NULL, ")"));
    tokens7.add(new Token(mode, TokenType.WHITESPACE, " "));
    tokens7.add(new Token(mode, TokenType.NULL, "{"));
    tokens7.add(new Token(mode, TokenType.NEWLINE, "\n"));

    JsIndexUpdater indexUpdater = new JsIndexUpdater();
    indexUpdater.onBeforeParse();

    previousLine = TaggableLineUtil.getPreviousLine(line);
    indexUpdater.onParseLine(previousLine, line, tokens1);
    JsCodeScope aScope = line.getTag(JsIndexUpdater.TAG_SCOPE);
    assertNotNull(aScope);
    assertEquals("a", aScope.getName());

    line = line.getNextLine();
    previousLine = TaggableLineUtil.getPreviousLine(line);
    indexUpdater.onParseLine(previousLine, line, tokens2);
    JsCodeScope dProtoScope = line.getTag(JsIndexUpdater.TAG_SCOPE);
    assertNotNull(dProtoScope);
    assertEquals("d.prototype", dProtoScope.getName());
    assertTrue(dProtoScope.getParent() == aScope);
    assertEquals("a-d-prototype", JsCodeScope.buildPrefix(dProtoScope).join("-"));

    line = line.getNextLine();
    previousLine = TaggableLineUtil.getPreviousLine(line);
    indexUpdater.onParseLine(previousLine, line, tokens3);
    assertTrue(line.getTag(JsIndexUpdater.TAG_SCOPE) == aScope);

    line = line.getNextLine();
    previousLine = TaggableLineUtil.getPreviousLine(line);
    indexUpdater.onParseLine(previousLine, line, tokens4);
    assertFalse(line.getTag(JsIndexUpdater.TAG_SCOPE) == aScope);

    line = line.getNextLine();
    previousLine = TaggableLineUtil.getPreviousLine(line);
    indexUpdater.onParseLine(previousLine, line, tokens5);
    JsCodeScope eScope = line.getTag(JsIndexUpdater.TAG_SCOPE);
    assertNotNull(eScope);
    assertEquals("e", eScope.getName());
    assertFalse(eScope.getParent() == aScope);

    line = line.getNextLine();
    previousLine = TaggableLineUtil.getPreviousLine(line);
    indexUpdater.onParseLine(previousLine, line, tokens6);
    JsCodeScope fScope = line.getTag(JsIndexUpdater.TAG_SCOPE);
    assertNotNull(fScope);
    assertEquals("f", fScope.getName());
    assertTrue(fScope.getParent() == eScope);

    line = line.getNextLine();
    previousLine = TaggableLineUtil.getPreviousLine(line);
    indexUpdater.onParseLine(previousLine, line, tokens7);
    JsCodeScope namelessScope = line.getTag(JsIndexUpdater.TAG_SCOPE);
    assertNotNull(namelessScope);
    assertNull(namelessScope.getName());
    assertTrue(namelessScope.getParent() == fScope);

    indexUpdater.onAfterParse();
  }
}
