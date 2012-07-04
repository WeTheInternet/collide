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

import static com.google.collide.codemirror2.TokenType.NULL;
import static com.google.collide.codemirror2.TokenType.VARIABLE;
import static com.google.collide.codemirror2.TokenType.WHITESPACE;

import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import javax.annotation.Nullable;

/**
 * Test cases for {@link ProposalBuilder}.
 *
 */
public class ProposalBuilderTest extends SynchronousTestCase {

  private static final RegExp PARSER = RegExp.compile("^(([A-Za-z]+)|([(+).])|(\\s+))");

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testTriggeringString() {
    checkContext("empty", "", null, "");
    checkContext("simple", "foo", null, "foo");
    checkContext("spaceless expression", "bar", null, "foo+bar");
    checkContext("expression", "bar", null, " + bar");
    checkContext("period, space, id", "get", null, ".  get");
    checkContext("property", "get", null, "foo.get");
    checkContext("empty property", "", "foo.", "foo.");
    checkContext("empty cascade-property", "", "foo.foo.", "foo.foo.");
  }

  private void checkContext(String message, String triggeringString, @Nullable String prevContext,
      String text) {
    ProposalBuilder<State> proposalBuilder = new CodeGraphAutocompleterTest.MockProposalBuilder();
    CompletionContext<State> context = proposalBuilder.buildContext(parse(text));
    assertEquals(
        message + ": triggering string", triggeringString, context.getTriggeringString());
    if (prevContext != null) {
      assertEquals(
          message + ": previous context", prevContext, context.getPreviousContext());
    }
  }

  private ParseUtils.ExtendedParseResult<State> parse(String text) {
    JsonArray<Token> tokens = JsonCollections.createArray();
    while (text.length() > 0) {
      MatchResult result = PARSER.exec(text);
      if (result == null) {
        throw new IllegalArgumentException("Can't parse: " + text);
      }
      String value;
      TokenType type;
      if (result.getGroup(2) != null) {
        value = result.getGroup(2);
        type = VARIABLE;
      } else if (result.getGroup(3) != null) {
        value = result.getGroup(3);
        type = NULL;
      } else if (result.getGroup(4) != null) {
        value = result.getGroup(4);
        type = WHITESPACE;
      } else {
        throw new IllegalArgumentException("Can't parse: " + result.getGroup(1));
      }
      tokens.add(new Token("test", type, value));
      text = text.substring(value.length());
    }
    ParseResult<State> parseResult = new ParseResult<State>(tokens, TestUtils.createMockState());
    return new ParseUtils.ExtendedParseResult<State>(parseResult, ParseUtils.Context.IN_CODE);
  }

  public void testPreviousContextTrimming() {
    String text = "   goog.le  ";

    checkContext("before property", "", "goog.", text.substring(0, 8));
    checkContext("after property", "le", "goog.", text.substring(0, 10));
    checkContext("after property and space", "", "", text.substring(0, 11));
  }

  public void testStripFunctionCallBraces() {
    checkContext("brace", "", "", "getFoo()");
    checkContext("braces and period", "", "getFoo.", "getFoo().");
    checkContext("id in braces and period", "", "getFoo.", "getFoo(bar).");
    checkContext("braces, period, id", "getBar", "getFoo.", "getFoo().getBar");
    checkContext("new in braces, period", "", ".", "(new Foo()).");
  }
}
