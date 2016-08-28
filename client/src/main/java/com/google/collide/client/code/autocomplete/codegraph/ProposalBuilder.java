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

import static com.google.collide.client.code.autocomplete.codegraph.ParseUtils.Context.IN_CODE;
import static com.google.collide.codemirror2.Token.LITERAL_PERIOD;
import static com.google.collide.codemirror2.TokenType.KEYWORD;
import static com.google.collide.codemirror2.TokenType.NULL;
import static com.google.collide.codemirror2.TokenType.VARIABLE;
import static com.google.collide.codemirror2.TokenType.VARIABLE2;
import static com.google.collide.codemirror2.TokenType.WHITESPACE;

import javax.annotation.Nonnull;

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.AutocompleteProposals.Context;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.Autocompleter;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.code.autocomplete.codegraph.ParseUtils.ExtendedParseResult;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.client.JsoStringSet;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

// TODO: Implement autocompletion-session end notification.
/**
 * Builds CompletionContext and proposals list.
 *
 * @param <T> language-specific {@link State} type.
 */
public abstract class ProposalBuilder<T extends State> {

  // TODO: Fix wording.
  private static final String HINT = "Press Ctrl-Shift-Space for alternate completion";

  private final Class<T> stateClass;

  protected ProposalBuilder(Class<T> stateClass) {
    this.stateClass = stateClass;
  }

  /**
   * Add more proposals prefixes based on language specifics.
   */
  protected abstract void addShortcutsTo(CompletionContext<T> context, JsonStringSet prefixes);

  /**
   * Returns language-specific templates.
   *
   * <p>Only lower-case items will match in case-insensitive mode.
   */
  protected abstract PrefixIndex<TemplateProposal> getTemplatesIndex();

  /**
   * Returns local variables visible in the current scope.
   */
  protected abstract JsonArray<String> getLocalVariables(ParseResult<T> parseResult);

  /**
   * Checks if the given prefix denotes "this"/"self" context.
   *
   * <p>Prefix is the beginning of the statement to the last period (including).
   *
   * <p>In case implementation returns {@code true} -
   * {@link CompletionContext#previousContext} is turned to empty in a purpose
   * of shortcutting.
   *
   * TODO: I think we should move this implicit shortcutting to a more
   *               proper place.
   */
  protected abstract boolean checkIsThisPrefix(String prefix);

  /**
   * Constructs context based on text around current cursor position.
   */
  @VisibleForTesting
  public CompletionContext<T> buildContext(
      SelectionModel selection, @Nonnull DocumentParser parser) {
    ExtendedParseResult<T> parseResult = ParseUtils.getExtendedParseResult(
        stateClass, parser, selection.getCursorPosition());
    if (parseResult.getContext() != IN_CODE) {
      return null;
    }
    return buildContext(parseResult);
  }

  protected CompletionContext<T> buildContext(ExtendedParseResult<T> extendedParseResult) {
    Preconditions.checkArgument(extendedParseResult.getContext() == IN_CODE);
    ParseResult<T> parseResult = extendedParseResult.getParseResult();

    JsonArray<Token> tokens = parseResult.getTokens();
    if (tokens.isEmpty()) {
      return new CompletionContext<T>("", "", false, CompletionType.GLOBAL, parseResult, 0);
    }

    int indent = 0;
    if (TokenType.WHITESPACE == tokens.get(0).getType()) {
      indent = tokens.get(0).getValue().length();
    }

    Token lastToken = tokens.pop();
    TokenType lastTokenType = lastToken.getType();

    if (lastTokenType == WHITESPACE) {
      return new CompletionContext<T>("", "", false, CompletionType.GLOBAL, parseResult, indent);
    }

    String lastTokenValue = lastToken.getValue();

    if (lastTokenType == KEYWORD) {
      return new CompletionContext<T>(
          "", lastTokenValue, false, CompletionType.GLOBAL, parseResult, indent);
    }

    boolean expectingPeriod = true;
    String triggeringString;

    // Property autocompletion only when cursor stands after period or id.
    if (lastTokenType == VARIABLE || lastTokenType == VARIABLE2
        || lastTokenType == TokenType.PROPERTY) {
      triggeringString = lastTokenValue;
    } else if ((lastTokenType == NULL) && LITERAL_PERIOD.equals(lastTokenValue)) {
      triggeringString = "";
      expectingPeriod = false;
    } else {
      return new CompletionContext<T>("", "", false, CompletionType.GLOBAL, parseResult, indent);
    }

    JsonArray<String> contextParts = JsonCollections.createArray();
    expectingPeriod = ParseUtils
        .buildInvocationSequenceContext(tokens, expectingPeriod, contextParts);
    contextParts.reverse();

    // If there were no more ids.
    if (contextParts.isEmpty() && expectingPeriod) {
      return new CompletionContext<T>(
          "", triggeringString, false, CompletionType.GLOBAL, parseResult, indent);
    }

    // TODO: What if expectingPeriod == false?
    String previousContext = contextParts.join(".") + ".";
    boolean isThisContext = checkIsThisPrefix(previousContext);

    return new CompletionContext<T>(previousContext, triggeringString, isThisContext,
        CompletionType.PROPERTY, parseResult, indent);
  }

  // TODO: Implement multiline context building.

  /**
   * Build {@link AutocompleteResult} according to current context and
   * selected proposal.
   */
  AutocompleteResult computeAutocompletionResult(
      CodeGraphProposal selectedProposal, String triggeringString) {
    String name = selectedProposal.getName();

    int tailOffset = 0;
    if (selectedProposal.isFunction()) {
      tailOffset = 1;
      name += "()";
    }

    return new DefaultAutocompleteResult(name, triggeringString, name.length() - tailOffset);
  }

  public AutocompleteProposals getProposals(SyntaxType mode,
      @Nonnull DocumentParser parser, SelectionModel selection, ScopeTrieBuilder scopeTrieBuilder) {
    CompletionContext<T> context = buildContext(selection, parser);
    if (context == null) {
      return AutocompleteProposals.EMPTY;
    }

    String triggeringString = context.getTriggeringString();
    JsonArray<AutocompleteProposal> items = doGetProposals(
        context, selection.getCursorPosition(), scopeTrieBuilder);
    return new AutocompleteProposals(
        mode, new Context(triggeringString, context.getIndent()), items, HINT);
  }

  @VisibleForTesting
  JsonArray<AutocompleteProposal> doGetProposals(
      CompletionContext<T> context, Position cursorPosition, ScopeTrieBuilder scopeTrieBuilder) {
    String itemPrefix = context.getTriggeringString();
    boolean ignoreCase = Autocompleter.CASE_INSENSITIVE;
    if (ignoreCase) {
      itemPrefix = itemPrefix.toLowerCase();
    }

    // A set used to avoid duplicates.
    JsoStringSet uniqueNames = JsoStringSet.create();

    // This array will be filled with proposals form different sources:
    // templates; visible names found by parser; matches from code graph.
    JsonArray<AutocompleteProposal> result = JsonCollections.createArray();

    // This also means previousContext == ""
    if (CompletionType.GLOBAL == context.getCompletionType()) {
      // Add templates.
      JsonArray<? extends TemplateProposal> templates = getTemplatesIndex().search(itemPrefix);
      result.addAll(templates);
      for (TemplateProposal template : templates.asIterable()) {
        uniqueNames.add(template.getName());
      }

      // Add visible names found by parser.
      JsonArray<String> localVariables = getLocalVariables(context.getParseResult());
      for (String localVariable : localVariables.asIterable()) {
        if (StringUtils.startsWith(itemPrefix, localVariable, ignoreCase)) {
          if (!uniqueNames.contains(localVariable)) {
            uniqueNames.add(localVariable);
            result.add(new CodeGraphProposal(localVariable, PathUtil.EMPTY_PATH, false));
          }
        }
      }
    }

    // Now use the knowledge about current scope and calculate possible
    // shortcuts in code graph.
    JsonStringSet prefixes = scopeTrieBuilder.calculateScopePrefixes(context, cursorPosition);
    // Let language-specific modifications.
    addShortcutsTo(context, prefixes);

    PrefixIndex<CodeGraphProposal> codeGraphTrie = scopeTrieBuilder.getCodeGraphTrie();
    JsonArray<AutocompleteProposal> codeProposals = JsonCollections.createArray();
    // We're iterate found shortcuts...
    for (String prefix : prefixes.getKeys().asIterable()) {
      JsonArray<? extends CodeGraphProposal> proposals = codeGraphTrie.search(prefix + itemPrefix);
      // Distill raw proposals.
      int prefixLength = prefix.length();
      for (CodeGraphProposal proposal : proposals.asIterable()) {
        // Take part of string between prefix and period.
        String proposalName = proposal.getName();
        int nameEndIndex = proposalName.length();
        int periodIndex = proposalName.indexOf('.', prefixLength);
        if (periodIndex != -1) {
          // TODO: Do we need this?
          nameEndIndex = periodIndex;
        }
        proposalName = proposalName.substring(prefixLength, nameEndIndex);

        if (!uniqueNames.contains(proposalName)) {
          uniqueNames.add(proposalName);
          codeProposals.add(
              new CodeGraphProposal(proposalName, proposal.getPath(), proposal.isFunction()));
        }
      }
    }
    result.addAll(codeProposals);

    return result;
  }
}
