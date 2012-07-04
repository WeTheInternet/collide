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

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.Autocompleter;
import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.code.autocomplete.codegraph.CodeGraphAutocompleterTest.MockProposalBuilder;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.client.DtoClientImpls.CodeBlockImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphImpl;
import com.google.collide.dto.client.DtoClientImpls.MockCodeBlockImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.client.JsoStringSet;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.JsonCollections;

/**
 * Tests for ProposalBuilder.
 *
 */
public class ScopeTrieBuilderTest extends SynchronousTestCase {
  static final int LAST_COLUMN = 99;
  static final String OBJECT_1 = "MyObject";
  static final String METHOD_1 = "method1";
  static final String METHOD_2 = "method2";

  private PathUtil path;

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    path = new PathUtil("/foo.js");
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  private CodeBlockImpl createCodeBlockTree() {
    // We create code blocks for the following pseudo-code:
    // MyObject {
    // function method1() {
    // }
    // function method2() {
    // var var1 {
    // foo: ''
    // }
    // }
    // }
    CodeBlockImpl contextFile = MockCodeBlockImpl
        .make()
        .setId("0")
        .setName(path.getPathString())
        .setBlockType(CodeBlock.Type.VALUE_FILE)
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(0)
        .setEndColumn(LAST_COLUMN)
        .setChildren(JsoArray.<CodeBlock>create());

    CodeBlockImpl objectBlock = MockCodeBlockImpl
        .make()
        .setId("1")
        .setBlockType(CodeBlock.Type.VALUE_FIELD)
        .setName(OBJECT_1)
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(0)
        .setEndColumn(LAST_COLUMN)
        .setChildren(JsoArray.<CodeBlock>create());
    contextFile.getChildren().add(objectBlock);

    CodeBlockImpl method1 = MockCodeBlockImpl
        .make()
        .setId("2")
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName(METHOD_1)
        .setStartLineNumber(0)
        .setStartColumn(10)
        .setEndLineNumber(0)
        .setEndColumn(49)
        .setChildren(JsoArray.<CodeBlock>create());
    CodeBlockImpl method2 = MockCodeBlockImpl
        .make()
        .setId("3")
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName(METHOD_2)
        .setStartLineNumber(0)
        .setStartColumn(50)
        .setEndLineNumber(0)
        .setEndColumn(LAST_COLUMN)
        .setChildren(JsoArray.<CodeBlock>create());
    objectBlock.getChildren().add(method1);
    objectBlock.getChildren().add(method2);

    CodeBlockImpl localVar = MockCodeBlockImpl
        .make()
        .setId("4")
        .setBlockType(CodeBlock.Type.VALUE_FIELD)
        .setName("var1")
        .setStartLineNumber(0)
        .setStartColumn(60)
        .setEndLineNumber(0)
        .setEndColumn(79)
        .setChildren(JsoArray.<CodeBlock>create());
    method2.getChildren().add(localVar);

    CodeBlockImpl localVarField = MockCodeBlockImpl
        .make()
        .setId("5")
        .setBlockType(CodeBlock.Type.VALUE_FIELD)
        .setName("foo")
        .setStartLineNumber(0)
        .setStartColumn(65)
        .setEndLineNumber(0)
        .setEndColumn(74)
        .setChildren(JsoArray.<CodeBlock>create());
    localVar.getChildren().add(localVarField);

    return contextFile;
  }

  /**
   * Check that produced proposals are equal to the given ones.
   * @param builder proposal producer
   * @param expected expected proposal, concatenated with "," separator
   * @param triggeringString context
   * @param isThisContext flag indicating "this."-like previous context
   * @return produced proposals (for deeper analysis)
   */
  private JsonArray<AutocompleteProposal> checkProposals(ScopeTrieBuilder builder,
      String[] expected, String triggeringString, boolean isThisContext) {
    Position cursor = new Position(
        Document.createEmpty().getFirstLineInfo(), triggeringString.length());
    ProposalBuilder<State> proposalBuilder = new MockProposalBuilder();
    CompletionContext<State> context = new CompletionContext<State>(
        "", triggeringString, isThisContext, CompletionType.GLOBAL, null, 0);
    JsonArray<AutocompleteProposal> proposals = proposalBuilder.doGetProposals(
        context, cursor, builder);

    assertEquals(JsonCollections.createStringSet(expected), TestUtils.createNameSet(proposals));
    return proposals;
  }

  private ScopeTrieBuilder createScopeTrieBuilder(CodeBlock codeBlock) {
    CodeFile codeFile = new CodeFile(path);
    codeFile.setRootCodeBlock(codeBlock);
    return new ScopeTrieBuilder(codeFile, SyntaxType.JS);
  }

  public void testEmptyDataSources() {
    CodeFile codeFile = new CodeFile(path);
    checkProposals(new ScopeTrieBuilder(codeFile, SyntaxType.JS), new String[0], "foo", false);
  }

  public void testExternalFileTopLevelProposals() {
    CodeBlockImpl file1 = MockCodeBlockImpl.make().setBlockType(CodeBlock.Type.VALUE_FILE)
        .setChildren(JsoArray.<CodeBlock>create())
        .setId("0")
        .setName("/file1.js");
    file1.getChildren().add(MockCodeBlockImpl
        .make()
        .setId("1")
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("foobar")
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(0)
        .setEndColumn(49));
    file1.getChildren().add(MockCodeBlockImpl
        .make()
        .setId("2")
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("barbaz")
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(0)
        .setEndColumn(49));

    CodeBlockImpl file2 = MockCodeBlockImpl.make().setBlockType(CodeBlock.Type.VALUE_FILE)
        .setChildren(JsoArray.<CodeBlock>create())
        .setId("1")
        .setName("/file2.js");
    file2.getChildren().add(MockCodeBlockImpl
        .make()
        .setId("1")
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("foobaz")
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(0)
        .setEndColumn(49));
    file2.getChildren().add(MockCodeBlockImpl
        .make()
        .setId("2")
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("barfoo")
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(0)
        .setEndColumn(49));

    CodeFile codeFile = new CodeFile(path);
    ScopeTrieBuilder builder = new ScopeTrieBuilder(codeFile, SyntaxType.JS);

    JsoStringMap<CodeBlock> codeBlockMap = JsoStringMap.create();
    codeBlockMap.put(file1.getName(), file1);
    codeBlockMap.put(file2.getName(), file2);
    builder.setCodeGraph(CodeGraphImpl.make().setCodeBlockMap(codeBlockMap));

    JsonArray<AutocompleteProposal> proposals = checkProposals(
        builder, new String[] {"foobar", "foobaz"}, "foo", false);
    assertEquals(new PathUtil("/file1.js"),
        TestUtils.findProposalByName(proposals, "foobar").getPath());
    assertEquals(new PathUtil("/file2.js"),
        TestUtils.findProposalByName(proposals, "foobaz").getPath());

    checkProposals(builder, new String[] {"barbaz", "barfoo", "foobar", "foobaz"}, "", false);

    // Check this proposals do not receive top-level items.
    checkProposals(builder, new String[0], "", true);
  }

  public void testLexicalScopeCompletionIncludesAncestorScopesAndChildScopes() {
    ScopeTrieBuilder builder = setupBuilder();
    Position cursor = new Position(Document.createEmpty().getFirstLineInfo(), 82);
    ProposalBuilder<State> proposalBuilder = new MockProposalBuilder();
    CompletionContext<State> context = new CompletionContext<State>(
        "", "", false, CompletionType.GLOBAL, null, 0);
    JsonArray<AutocompleteProposal> proposals = proposalBuilder.doGetProposals(
        context, cursor, builder);
    JsonStringSet expected = JsonCollections.createStringSet(OBJECT_1, METHOD_2, METHOD_1, "var1");
    assertEquals(expected, TestUtils.createNameSet(proposals));
  }

  public void testCaseInsensitiveSearch() {
    assertTrue(Autocompleter.CASE_INSENSITIVE);
    ScopeTrieBuilder builder = setupBuilder();
    Position cursor = new Position(Document.createEmpty().getFirstLineInfo(), 82);
    ProposalBuilder<State> proposalBuilder = new MockProposalBuilder();
    CompletionContext<State> context = new CompletionContext<State>(
        "", "MEtH", false, CompletionType.GLOBAL, null, 0);
    JsonArray<AutocompleteProposal> proposals = proposalBuilder.doGetProposals(
        context, cursor, builder);
    JsonStringSet expected = JsonCollections.createStringSet("method2", "method1");
    assertEquals(expected, TestUtils.createNameSet(proposals));
  }

  public void testThisCompletionIncludesDirectParentScope() {
    ScopeTrieBuilder builder = setupBuilder();
    Position cursor = new Position(Document.createEmpty().getFirstLineInfo(), 86);
    ProposalBuilder<State> proposalBuilder = new MockProposalBuilder();
    CompletionContext<State> context = new CompletionContext<State>(
        "", "", true, CompletionType.PROPERTY, null, 0);
    JsonArray<AutocompleteProposal> proposals = proposalBuilder.doGetProposals(
        context, cursor, builder);
    assertEquals(JsonCollections.createStringSet(METHOD_2, METHOD_1),
        TestUtils.createNameSet(proposals));
  }

  public void testScopeEndBoundry() {
    ScopeTrieBuilder builder = setupBuilder();
    ProposalBuilder<State> proposalBuilder = new MockProposalBuilder();
    CompletionContext<State> context = new CompletionContext<State>(
        "", "", true, CompletionType.PROPERTY, null, 0);

    Position cursor = new Position(Document.createEmpty().getFirstLineInfo(), LAST_COLUMN + 1);
    assertFalse(proposalBuilder.doGetProposals(context, cursor, builder).isEmpty());

    cursor = new Position(Document.createEmpty().getFirstLineInfo(), LAST_COLUMN + 2);
    assertTrue(proposalBuilder.doGetProposals(context, cursor, builder).isEmpty());
  }

  /**
   * Checks that even if we have problems with resolving scope, we try to
   * search with raw "previousContext".
   */
  public void testBareScopePrefixMinimum() {
    ScopeTrieBuilder scopeTrieBuilder = new ScopeTrieBuilder(new CodeFile(path), SyntaxType.JS);

    String previousContext = "moo.";
    CompletionContext<State> context = new CompletionContext<State>(
        previousContext, "", false, CompletionType.PROPERTY, null, 0);

    Position cursor = new Position(Document.createEmpty().getFirstLineInfo(), 1);
    JsoStringSet prefixes = scopeTrieBuilder.calculateScopePrefixes(context, cursor);

    assertNotNull(prefixes);
    assertFalse(prefixes.isEmpty());
    assertTrue(prefixes.contains(previousContext));
  }

  /**
   * Checks that for scoped position "full scope path + previous context" is
   * included to prefix list.
   */
  public void testFullLexicalScopePrefix() {
    ScopeTrieBuilder builder = setupBuilder();

    String previousContext = "moo.";
    CompletionContext<State> context = new CompletionContext<State>(
        previousContext, "", false, CompletionType.PROPERTY, null, 0);

    Position cursor = new Position(Document.createEmpty().getFirstLineInfo(), 20);
    JsoStringSet prefixes = builder.calculateScopePrefixes(context, cursor);

    assertNotNull(prefixes);
    assertFalse(prefixes.isEmpty());
    assertTrue(prefixes.contains(OBJECT_1 + "." + METHOD_1 + "." + previousContext));
  }

  public void testScopeResolutionForExpandingContext() {
    ScopeTrieBuilder builder = setupBuilder();
    ProposalBuilder<State> proposalBuilder = new MockProposalBuilder();

    CompletionContext<State> context = new CompletionContext<State>(
        "", "", true, CompletionType.PROPERTY, null, 0);
    Position cursor = new Position(Document.createEmpty().getFirstLineInfo(), LAST_COLUMN + 2);
    assertTrue(proposalBuilder.doGetProposals(context, cursor, builder).isEmpty());

    context = new CompletionContext<State>(
        OBJECT_1 + ".", "", true, CompletionType.PROPERTY, null, 0);
    cursor = new Position(Document.createEmpty().getFirstLineInfo(), LAST_COLUMN + 2);
    assertFalse(proposalBuilder.doGetProposals(context, cursor, builder).isEmpty());
  }

  private ScopeTrieBuilder setupBuilder() {
    CodeBlock contextFile = createCodeBlockTree();
    JsoStringMap<CodeBlock> codeBlockMap = JsoStringMap.create();
    codeBlockMap.put(contextFile.getName(), contextFile);
    ScopeTrieBuilder builder = createScopeTrieBuilder(contextFile);
    builder.setCodeGraph(CodeGraphImpl.make().setCodeBlockMap(codeBlockMap));
    return builder;
  }
}
