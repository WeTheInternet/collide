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

import com.google.collide.client.code.autocomplete.AbstractTrie;
import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.js.JsCodeScope;
import com.google.collide.client.code.autocomplete.codegraph.js.JsIndexUpdater;
import com.google.collide.client.code.autocomplete.codegraph.py.PyCodeScope;
import com.google.collide.client.code.autocomplete.codegraph.py.PyIndexUpdater;
import com.google.collide.client.code.autocomplete.integration.TaggableLineUtil;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.client.util.logging.Log;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.CodeGraph;
import com.google.collide.dto.CodeBlock.Type;
import com.google.collide.json.client.JsoStringSet;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.grok.GrokUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Builds a list of completion proposals from a few sources
 * (context file, external files and language constructs)
 *
 */
public class ScopeTrieBuilder {

  private static void addLexicalPrefixes(
      JsoStringSet prefixes, JsonArray<String> path, String commonSuffix) {
    for (int i = 0; i <= path.size(); i++) {
      String currentPrefix = (i == 0) ? "" : path.slice(0, i).join(".") + ".";
      prefixes.add(currentPrefix + commonSuffix);
    }
  }
  private static void addThisPrefixes(JsoStringSet prefixes, JsonArray<String> path) {
    String thisPrefix = path.slice(0, path.size() - 1).join(".") + ".";
    prefixes.add(thisPrefix);
  }

  private static void debugLog(CodeBlock codeBlock, String indent) {
    Log.debug(ScopeTrieBuilder.class, indent + CodeBlock.Type.valueOf(codeBlock.getBlockType())
        + " " + codeBlock.getName()
        + "#" + codeBlock.getId()
        + "[" + codeBlock.getStartLineNumber() + ":" + codeBlock.getStartColumn() + ","
        + codeBlock.getEndLineNumber() + ":" + codeBlock.getEndColumn() + "]");
  }

  private static void debugLogTree(CodeBlock root, String indent) {
    if (root == null) {
      Log.debug(ScopeTrieBuilder.class, "null code block");
      return;
    }
    debugLog(root, indent);
    indent = "  " + indent;
    for (int i = 0; i < root.getChildren().size(); i++) {
      debugLogTree(root.getChildren().get(i), indent);
    }
  }

  private PrefixIndex<CodeGraphProposal> externalTrie = new AbstractTrie<CodeGraphProposal>();

  private final CodeFile contextFile;

  private final SyntaxType mode;

  public ScopeTrieBuilder(CodeFile contextFile, SyntaxType mode) {
    this.contextFile = contextFile;
    this.mode = mode;
  }

  public JsoStringSet calculateScopePrefixes(CompletionContext context, Position cursor) {
    JsoStringSet result = JsoStringSet.create();
    boolean isThisContext = context.isThisContext();
    Line line = cursor.getLine();

    if (!isThisContext) {
      result.add(context.getPreviousContext());
    }

    //PY specific.
    PyCodeScope pyScope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    if (pyScope != null) {
      JsonArray<String> path = PyCodeScope.buildPrefix(pyScope);
      if (isThisContext && pyScope.getType() == PyCodeScope.Type.DEF && path.size() > 1) {
        addThisPrefixes(result, path);
      } else {
        addLexicalPrefixes(result, path, context.getPreviousContext());
      }
      return result;
    }

    // Fallback - use pre-calculated results (valid at the end of line).
    JsCodeScope jsScope = line.getTag(JsIndexUpdater.TAG_SCOPE);

    @SuppressWarnings("unchecked")
    // Trying to get results up to cursor position.
    ParseResult<State> parseResult = context.getParseResult();
    if (parseResult != null) {
      jsScope = JsIndexUpdater.calculateContext(
          TaggableLineUtil.getPreviousLine(line), parseResult.getTokens()).getScope();
    }

    if (jsScope != null) {
      JsonArray<String> path = JsCodeScope.buildPrefix(jsScope);
      if (isThisContext && path.size() > 1) {
        addThisPrefixes(result, path);
      } else {
        addLexicalPrefixes(result, path, context.getPreviousContext());
      }
    }

    int lineNumber = cursor.getLineNumber();
    int column = cursor.getColumn();
    column = Math.max(0, column - context.getPreviousContext().length());
    final Scope scope = contextFile.findScope(lineNumber, column, true);

    // Can't calculate scope or matching codeBlock or it is root scope.
    if (scope == null || scope == contextFile.getRootScope()) {
      return result;
    }

    CodeBlock codeBlock = scope.getCodeBlock();
    JsonArray<String> prefix = buildPrefix(codeBlock);
    // Add prefixes corresponding to outer scopes.
    if (isThisContext && Type.VALUE_FUNCTION == codeBlock.getBlockType() && prefix.size() > 1) {
      addThisPrefixes(result, prefix);
    } else {
      addLexicalPrefixes(result, prefix, context.getPreviousContext());
    }
    return result;
  }

  /**
   * Builds sequence on names that represents path to given {@link CodeBlock}.
   *
   * <p>Given block name is the last item in resulting sequence.
   */
  private JsonArray<String> buildPrefix(@Nonnull CodeBlock codeBlock) {
    Preconditions.checkNotNull(codeBlock);

    CodeBlock current = codeBlock;
    JsonArray<String> prefix = JsonCollections.createArray();
    while (current != null && Type.VALUE_FILE != current.getBlockType()) {
      prefix.add(current.getName());
      current = contextFile.getReferences().getParent(current);
    }
    prefix.reverse();

    return prefix;
  }

  public void setCodeGraph(CodeGraph codeGraph) {
    CodeBlock contextFileCodeBlock = GrokUtils.findFileCodeBlock(codeGraph,
        contextFile.getFilePath().getPathString());
    contextFile.setRootCodeBlock(contextFileCodeBlock);
    externalTrie = new CodeGraphPrefixIndex(codeGraph, mode, contextFile.getFilePath());
  }

  public PrefixIndex<CodeGraphProposal> getCodeGraphTrie() {
    Preconditions.checkNotNull(externalTrie);
    return externalTrie;
  }
}
