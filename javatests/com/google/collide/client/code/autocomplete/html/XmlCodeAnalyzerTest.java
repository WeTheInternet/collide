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

package com.google.collide.client.code.autocomplete.html;

import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.util.JsonCollections;

/**
 * Tests for xml {@link XmlCodeAnalyzer}
 *
 */
public class XmlCodeAnalyzerTest extends SynchronousTestCase {

  private static class MockTagHolder implements TaggableLine {

    JsonStringMap<Object> tags = JsonCollections.createMap();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getTag(String key) {
      return (T) tags.get(key);
    }

    @Override
    public <T> void putTag(String key, T value) {
      tags.put(key, value);
    }

    @Override
    public TaggableLine getPreviousLine() {
      throw new IllegalStateException("unexpected call");
    }

    @Override
    public boolean isFirstLine() {
      return false;
    }

    @Override
    public boolean isLastLine() {
      return false;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testParse() {
    XmlCodeAnalyzer codeAnalyzer = new XmlCodeAnalyzer();

    JsonArray<TaggableLine> lines = JsonCollections.createArray();
    JsonArray<JsonArray<Token>> tokens = JsonCollections.createArray();

    // NOTE: 0 is used for line before the beginning of document.
    for (int i = 0; i <= 3; i++) {
      lines.add(new MockTagHolder());
      tokens.add(JsonCollections.<Token>createArray());
    }
    JsonArray<Token> lineTokens;

    lineTokens = tokens.get(1);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "first"));

    lineTokens = tokens.get(2);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "second"));

    lineTokens = tokens.get(3);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "third"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, ">"));

    codeAnalyzer.onBeforeParse();
    for (int i = 1; i <= 3; i++) {
      codeAnalyzer.onParseLine(lines.get(i - 1), lines.get(i), tokens.get(i));
    }
    codeAnalyzer.onAfterParse();

    assertNull(lines.get(1).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    HtmlTagWithAttributes tag = lines.get(1).getTag(XmlCodeAnalyzer.TAG_END_TAG);
    assertNotNull(tag);
    assertEquals("html", tag.getTagName());
    assertTrue(tag.getAttributes().contains("first"));
    assertTrue(tag.getAttributes().contains("second"));
    assertTrue(tag.getAttributes().contains("third"));

    assertTrue(tag == lines.get(2).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    assertTrue(tag == lines.get(2).getTag(XmlCodeAnalyzer.TAG_END_TAG));

    assertTrue(tag == lines.get(3).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    assertNull(lines.get(3).getTag(XmlCodeAnalyzer.TAG_END_TAG));
  }

  public void testReparse() {
    XmlCodeAnalyzer codeAnalyzer = new XmlCodeAnalyzer();

    JsonArray<TaggableLine> lines = JsonCollections.createArray();
    JsonArray<JsonArray<Token>> tokens = JsonCollections.createArray();

    // NOTE: 0 is used for line before the beginning of document.
    for (int i = 0; i <= 3; i++) {
      lines.add(new MockTagHolder());
      tokens.add(JsonCollections.<Token>createArray());
    }
    JsonArray<Token> lineTokens;

    lineTokens = tokens.get(1);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "first"));

    lineTokens = tokens.get(2);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "second"));

    lineTokens = tokens.get(3);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "third"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, ">"));

    codeAnalyzer.onBeforeParse();
    for (int i = 1; i <= 3; i++) {
      codeAnalyzer.onParseLine(lines.get(i - 1), lines.get(i), tokens.get(i));
    }
    codeAnalyzer.onAfterParse();

    lineTokens = tokens.get(2);
    lineTokens.clear();
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "fixed"));

    codeAnalyzer.onBeforeParse();
    for (int i = 2; i <= 3; i++) {
      codeAnalyzer.onParseLine(lines.get(i - 1), lines.get(i), tokens.get(i));
    }
    codeAnalyzer.onAfterParse();

    assertNull(lines.get(1).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    HtmlTagWithAttributes tag = lines.get(1).getTag(XmlCodeAnalyzer.TAG_END_TAG);
    assertNotNull(tag);
    assertEquals("html", tag.getTagName());
    assertTrue(tag.getAttributes().contains("first"));
    assertTrue(tag.getAttributes().contains("fixed"));
    assertTrue(tag.getAttributes().contains("third"));

    assertTrue(tag == lines.get(2).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    assertTrue(tag == lines.get(2).getTag(XmlCodeAnalyzer.TAG_END_TAG));

    assertTrue(tag == lines.get(3).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    assertNull(lines.get(3).getTag(XmlCodeAnalyzer.TAG_END_TAG));
  }

  public void testIntermediateTags() {
    XmlCodeAnalyzer codeAnalyzer = new XmlCodeAnalyzer();

    JsonArray<TaggableLine> lines = JsonCollections.createArray();
    JsonArray<JsonArray<Token>> tokens = JsonCollections.createArray();

    // NOTE: 0 is used for line before the beginning of document.
    for (int i = 0; i <= 2; i++) {
      lines.add(new MockTagHolder());
      tokens.add(JsonCollections.<Token>createArray());
    }
    JsonArray<Token> lineTokens;

    lineTokens = tokens.get(1);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<first"));

    lineTokens = tokens.get(2);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "first"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, ">"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<second"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "second"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, ">"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<third"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "third"));

    codeAnalyzer.onBeforeParse();
    for (int i = 1; i <= 2; i++) {
      codeAnalyzer.onParseLine(lines.get(i - 1), lines.get(i), tokens.get(i));
    }
    codeAnalyzer.onAfterParse();

    assertNull(lines.get(1).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    HtmlTagWithAttributes tag = lines.get(1).getTag(XmlCodeAnalyzer.TAG_END_TAG);
    assertNotNull(tag);
    assertEquals("first", tag.getTagName());
    assertTrue(tag.getAttributes().contains("first"));

    assertTrue(tag == lines.get(2).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    assertFalse(tag == lines.get(2).getTag(XmlCodeAnalyzer.TAG_END_TAG));
    tag = lines.get(2).getTag(XmlCodeAnalyzer.TAG_END_TAG);
    assertNotNull(tag);
    assertEquals("third", tag.getTagName());
    assertTrue(tag.getAttributes().contains("third"));
  }

  public void testDeleteLine() {
    XmlCodeAnalyzer codeAnalyzer = new XmlCodeAnalyzer();

    JsonArray<TaggableLine> lines = JsonCollections.createArray();
    JsonArray<JsonArray<Token>> tokens = JsonCollections.createArray();

    // NOTE: 0 is used for line before the beginning of document.
    for (int i = 0; i <= 3; i++) {
      lines.add(new MockTagHolder());
      tokens.add(JsonCollections.<Token>createArray());
    }
    JsonArray<Token> lineTokens;

    lineTokens = tokens.get(1);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, "<html"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "first"));

    lineTokens = tokens.get(2);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "second"));

    lineTokens = tokens.get(3);
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.ATTRIBUTE, "third"));
    lineTokens.add(new Token(CodeMirror2.HTML, TokenType.TAG, ">"));

    codeAnalyzer.onBeforeParse();
    for (int i = 1; i <= 3; i++) {
      codeAnalyzer.onParseLine(lines.get(i - 1), lines.get(i), tokens.get(i));
    }
    codeAnalyzer.onAfterParse();

    codeAnalyzer.onLinesDeleted(JsonCollections.createArray(lines.get(2)));

    assertNull(lines.get(1).getTag(XmlCodeAnalyzer.TAG_START_TAG));
    HtmlTagWithAttributes tag = lines.get(1).getTag(XmlCodeAnalyzer.TAG_END_TAG);
    assertNotNull(tag);
    assertEquals("html", tag.getTagName());
    assertTrue(tag.getAttributes().contains("first"));
    assertTrue(tag.getAttributes().contains("third"));
  }
}
