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

import static com.google.collide.client.code.autocomplete.html.CompletionType.ATTRIBUTE;
import static com.google.collide.client.code.autocomplete.html.CompletionType.ELEMENT;

import javax.annotation.Nonnull;

import com.google.collide.client.code.autocomplete.AutocompleteController;
import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.client.code.autocomplete.LanguageSpecificAutocompleter;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.code.autocomplete.codegraph.CodeGraphAutocompleter;
import com.google.collide.client.code.autocomplete.css.CssAutocompleter;
import com.google.collide.client.code.autocomplete.html.HtmlAutocompleteProposals.HtmlProposalWithContext;
import com.google.collide.client.code.autocomplete.integration.TaggableLineUtil;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.collections.StringMultiset;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.codemirror2.HtmlState;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.codemirror2.TokenUtil;
import com.google.collide.codemirror2.XmlContext;
import com.google.collide.codemirror2.XmlState;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.Pair;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.event.dom.client.KeyCodes;

/**
 * Autocompleter for HTML.
 *
 *
 */
public class HtmlAutocompleter extends LanguageSpecificAutocompleter {

  private static final String ELEMENT_SEPARATOR_CLOSE = ">";

  private static final String ELEMENT_SELF_CLOSE = " />";

  private static final String ELEMENT_SEPARATOR_OPEN_FINISHTAG = "</";

  private static final String ATTRIBUTE_SEPARATOR_OPEN = "=\"";

  private static final String ATTRIBUTE_SEPARATOR_CLOSE = "\"";

  private static final HtmlTagsAndAttributes htmlAttributes = HtmlTagsAndAttributes.getInstance();

  @VisibleForTesting
  static final AnchorType MODE_ANCHOR_TYPE =
      AnchorType.create(HtmlAutocompleter.class, "mode");

  public static final AutocompleteResult RESULT_SLASH = new DefaultAutocompleteResult("/", "", 1);

  /**
   * Bean that holds {@link #findTag} results.
   */
  private static class FindTagResult {
    /**
     * Index of last start-of-TAG token before cursor; -1 => not in this line.
     */
    int startTagIndex = -1;

    /**
     * Index of last end-of-TAG token before cursor; -1 => not in this line.
     */
    int endTagIndex = -1;

    /**
     * Token that "covers" the cursor; left token if cursor touches 2 tokens,
     */
    Token inToken = null;

    /**
     * Number of characters between "inToken" start and the cursor position.
     */
    int cut = 0;

    /**
     * Indicates that cursor is located inside tag.
     */
    boolean inTag;
  }

  public static HtmlAutocompleter create(CssAutocompleter cssAutocompleter,
      CodeGraphAutocompleter jsAutocompleter) {
    return new HtmlAutocompleter(cssAutocompleter, jsAutocompleter);
  }

  /**
   * Finds token at cursor position and computes first and last token indexes
   * of surrounding tag.
   */
  private static FindTagResult findTag(JsonArray<Token> tokens, boolean startsInTag, int column) {
    FindTagResult result = new FindTagResult();
    result.inTag = startsInTag;

    // Number of tokens in line.
    final int size = tokens.size();

    // Sum of lengths of processed tokens.
    int colCount = 0;

    // Index of next token.
    int index = 0;

    while (index < size) {
      Token token = tokens.get(index);
      colCount += token.getValue().length();
      TokenType type = token.getType();
      index++;
      if (TokenType.TAG == type) {
        // Toggle "inTag" flag and update tag bounds.
        if (result.inTag) {
          // Refer to XmlCodeAnalyzer parsing code notes.
          if (">".equals(token.getValue()) || "/>".equals(token.getValue())) {
            result.endTagIndex = index - 1;
            // Exit the loop if cursor is inside a closed tag.
            if (result.inToken != null) {
              return result;
            }
            result.inTag = false;
          }
        } else {
          if (CodeMirror2.HTML.equals(token.getMode())) {
            result.startTagIndex = index - 1;
            result.endTagIndex = -1;
            result.inTag = true;
          }
        }
      }
      // If token at cursor position is not found yet...
      if (result.inToken == null) {
        if (colCount >= column) {
          // We've found it at last!
          result.inToken = token;
          result.cut = colCount - column;
          if (!result.inTag) {
            // No proposals for text content.
            return result;
          }
        }
      }
    }

    return result;
  }

  /**
   * Builds {@link HtmlTagWithAttributes} from {@link FindTagResult} and tokens.
   *
   * <p>Scanning is similar to scanning in {@link XmlCodeAnalyzer}.
   */
  private static HtmlTagWithAttributes buildTag(
      FindTagResult findTagResult, JsonArray<Token> tokens) {
    int index = findTagResult.startTagIndex;
    Token token = tokens.get(index);
    index++;
    String tagName = token.getValue().substring(1).trim();

    HtmlTagWithAttributes result = new HtmlTagWithAttributes(tagName);

    StringMultiset tagAttributes = result.getAttributes();
    while (index < findTagResult.endTagIndex) {
      token = tokens.get(index);
      index++;
      TokenType tokenType = token.getType();
      if (TokenType.ATTRIBUTE == tokenType) {
        tagAttributes.add(token.getValue().toLowerCase());
      }
    }

    result.setDirty(false);
    return result;
  }

  private CssAutocompleter cssAutocompleter;
  private CodeGraphAutocompleter jsAutocompleter;

  private DirtyStateTracker dirtyScope;
  private final Runnable dirtyScopeDelegate = new Runnable() {
    @Override
    public void run() {
      resetDirtyScope();
      scheduleRequestForUpdatedProposals();
    }
  };

  private HtmlAutocompleter(CssAutocompleter cssAutocompleter,
      CodeGraphAutocompleter jsAutocompleter) {
    super(SyntaxType.HTML);
    this.cssAutocompleter = cssAutocompleter;
    this.jsAutocompleter = jsAutocompleter;
  }

  @Override
  protected void attach(
      DocumentParser parser, AutocompleteController controller, PathUtil filePath) {
    super.attach(parser, controller, filePath);
    if (cssAutocompleter != null) {
      cssAutocompleter.attach(parser, controller, filePath);
    }
    if (jsAutocompleter != null) {
      jsAutocompleter.attach(parser, controller, filePath);
    }
  }

  @Override
  public AutocompleteResult computeAutocompletionResult(ProposalWithContext proposal) {
    if (!(proposal instanceof HtmlProposalWithContext)) {
      if (proposal.getSyntaxType() == SyntaxType.JS) {
        return jsAutocompleter.computeAutocompletionResult(proposal);
      } else if (proposal.getSyntaxType() == SyntaxType.CSS) {
        return cssAutocompleter.computeAutocompletionResult(proposal);
      } else {
        throw new IllegalStateException(
            "Unexpected mode: " + proposal.getSyntaxType().getName());
      }
    }

    HtmlProposalWithContext htmlProposal = (HtmlProposalWithContext) proposal;
    AutocompleteProposal selectedProposal = proposal.getItem();
    String triggeringString = proposal.getContext().getTriggeringString();
    String selectedName = selectedProposal.getName();

    switch (htmlProposal.getType()) {
      case ELEMENT:
        if (htmlAttributes.isSelfClosedTag(selectedName)) {
          return new DefaultAutocompleteResult(
              selectedName + ELEMENT_SELF_CLOSE, triggeringString,
              selectedName.length());
        }
        return new DefaultAutocompleteResult(
            selectedName + ELEMENT_SEPARATOR_CLOSE + ELEMENT_SEPARATOR_OPEN_FINISHTAG
                + selectedName + ELEMENT_SEPARATOR_CLOSE, triggeringString,
            selectedName.length() + ELEMENT_SEPARATOR_CLOSE.length());

      case ATTRIBUTE:
        return new DefaultAutocompleteResult(
            selectedName + ATTRIBUTE_SEPARATOR_OPEN + ATTRIBUTE_SEPARATOR_CLOSE,
            triggeringString, selectedName.length() + ATTRIBUTE_SEPARATOR_OPEN.length());

      default:
        throw new IllegalStateException(
            "Invocation of this method in not allowed for type " + htmlProposal.getType());
    }
  }

  @Override
  public ExplicitAction getExplicitAction(SelectionModel selectionModel,
      SignalEventEssence signal, boolean popupIsShown) {
    Position cursor = selectionModel.getCursorPosition();
    int cursorColumn = cursor.getColumn();
    Line cursorLine = cursor.getLine();
    String mode = getModeForColumn(cursorLine, cursorColumn);

    if (cssAutocompleter != null && CodeMirror2.CSS.equals(mode)) {
      return cssAutocompleter.getExplicitAction(selectionModel, signal, popupIsShown);
    } else if (jsAutocompleter != null && CodeMirror2.JAVASCRIPT.equals(mode)) {
      return jsAutocompleter.getExplicitAction(selectionModel, signal, popupIsShown);
    } else if (mode == null) {
      // This is possible if line is new and hasn't been processed yet.
      // We prefer to avoid annoying autocompletions.
      return ExplicitAction.DEFAULT;
    }

    char signalChar = signal.getChar();
    if (signalChar == '/') {
      if (selectionModel.hasSelection()) {
        return ExplicitAction.DEFAULT;
      }
      if (cursorColumn == 0 || '<' != cursorLine.getText().charAt(cursorColumn - 1)) {
        return ExplicitAction.DEFAULT;
      }
      ParseResult<HtmlState> parseResult = getParser().getState(HtmlState.class, cursor, null);
      if (parseResult != null) {
        XmlState xmlState = parseResult.getState().getXmlState();
        if (xmlState != null) {
          XmlContext xmlContext = xmlState.getContext();
          if (xmlContext != null) {
            String tagName = xmlContext.getTagName();
            if (tagName != null) {
              String addend = "/" + tagName + ELEMENT_SEPARATOR_CLOSE;
              return new ExplicitAction(new DefaultAutocompleteResult(addend, "", addend.length()));
            }
          }
        }
      }
      return ExplicitAction.DEFAULT;
    }
    if (!popupIsShown && (signalChar != 0)
        && (KeyCodes.KEY_ENTER != signalChar)
        && ('>' != signalChar)) {
      return ExplicitAction.DEFERRED_COMPLETE;
    }
    return ExplicitAction.DEFAULT;
  }

  /**
   * Finds autocomplete proposals based on the incomplete string.
   *
   * <p>Triggered
   *
   * <p>This method is triggered when:<ul>
   *   <li>popup is hidden and user press ctrl-space (event consumed),
   *       and explicit autocompletion failed
   *   <li><b>or</b> popup is shown
   * </ul>
   */
  @Override
  public AutocompleteProposals findAutocompletions(
      SelectionModel selection, SignalEventEssence trigger) {
    resetDirtyScope();

    Position cursor = selection.getCursorPosition();
    final Line line = cursor.getLine();
    final int column = cursor.getColumn();

    DocumentParser parser = getParser();
    JsonArray<Token> tokens = parser.parseLineSync(line);
    if (tokens == null) {
      // This line has never been parsed yet. No variants.
      return AutocompleteProposals.EMPTY;
    }

    // We do not ruin parse results for "clean" lines.
    if (parser.isLineDirty(cursor.getLineNumber())) {
      // But "processing" of "dirty" line is harmless.
      XmlCodeAnalyzer.processLine(TaggableLineUtil.getPreviousLine(line), line, tokens);
    }
    String initialMode = parser.getInitialMode(line);
    JsonArray<Pair<Integer, String>> modes = TokenUtil.buildModes(initialMode, tokens);
    putModeAnchors(line, modes);
    String mode = TokenUtil.findModeForColumn(initialMode, modes, column);

    if (cssAutocompleter != null && CodeMirror2.CSS.equals(mode)) {
      return cssAutocompleter.findAutocompletions(selection, trigger);
    } else if (jsAutocompleter != null && CodeMirror2.JAVASCRIPT.equals(mode)) {
      return jsAutocompleter.findAutocompletions(selection, trigger);
    }

    if (selection.hasSelection()) {
      // Do not autocomplete in HTML when something is selected.
      return AutocompleteProposals.EMPTY;
    }

    HtmlTagWithAttributes tag = line.getTag(XmlCodeAnalyzer.TAG_START_TAG);
    boolean inTag = tag != null;

    if (column == 0) {
      // On first column we either add attribute or do nothing.
      if (inTag) {
        JsonArray<AutocompleteProposal> proposals = htmlAttributes.searchAttributes(
            tag.getTagName(), tag.getAttributes(), "");
        return new HtmlAutocompleteProposals("", proposals, ATTRIBUTE);
      }
      return AutocompleteProposals.EMPTY;
    }

    FindTagResult findTagResult = findTag(tokens, inTag, column);

    if (!findTagResult.inTag || findTagResult.inToken == null) {
      // Ooops =(
      return AutocompleteProposals.EMPTY;
    }

    // If not unfinished tag at the beginning of line surrounds cursor...
    if (findTagResult.startTagIndex >= 0) {
      // Unfinished tag at he end of line may be used...
      if (findTagResult.endTagIndex == -1) {
        tag = line.getTag(XmlCodeAnalyzer.TAG_END_TAG);
        if (tag == null) {
          // Ooops =(
          return AutocompleteProposals.EMPTY;
        }
      } else {
        // Or new (temporary) object constructed.
        tag = buildTag(findTagResult, tokens);
      }
    }

    TokenType type = findTagResult.inToken.getType();
    String value = findTagResult.inToken.getValue();
    value = value.substring(0, value.length() - findTagResult.cut);
    if (TokenType.TAG == type) {
      value = value.substring(1).trim();
      return new HtmlAutocompleteProposals(
          value, htmlAttributes.searchTags(value.toLowerCase()), ELEMENT);
    }
    if (TokenType.WHITESPACE == type || TokenType.ATTRIBUTE == type) {
      value = (TokenType.ATTRIBUTE == type) ? value : "";
      JsonArray<AutocompleteProposal> proposals = htmlAttributes.searchAttributes(
          tag.getTagName(), tag.getAttributes(), value);
      dirtyScope = tag;
      dirtyScope.setDelegate(dirtyScopeDelegate);
      if (tag.isDirty()) {
        return AutocompleteProposals.PARSING;
      }
      return new HtmlAutocompleteProposals(value, proposals, ATTRIBUTE);
    }

    return AutocompleteProposals.EMPTY;
  }

  @Override
  protected void pause() {
    super.pause();
    resetDirtyScope();
  }

  private void resetDirtyScope() {
    if (dirtyScope != null) {
      dirtyScope.setDelegate(null);
      dirtyScope = null;
    }
  }

  @Override
  public void cleanup() {
  }

  /**
   * Updates line meta-information.
   *
   * @param line line being parsed
   * @param tokens tokens collected on the line
   */
  public void updateModeAnchors(TaggableLine line, @Nonnull JsonArray<Token> tokens) {
    String initialMode = getParser().getInitialMode(line);
    JsonArray<Pair<Integer, String>> modes = TokenUtil.buildModes(initialMode, tokens);
    putModeAnchors(line, modes);
  }

  @VisibleForTesting
  String getModeForColumn(Line line, int column) {
    DocumentParser parser = getParser();
    String mode = parser.getInitialMode(line);

    JsonArray<Anchor> anchors = AnchorManager.getAnchorsByTypeOrNull(line, MODE_ANCHOR_TYPE);
    if (anchors != null) {
      for (Anchor anchor : anchors.asIterable()) {
        if (anchor.getColumn() >= column) {
          // We'll use the previous mode.
          break;
        }
        mode = anchor.getValue();
      }
    }
    return mode;
  }

  @VisibleForTesting
  void putModeAnchors(@Nonnull TaggableLine currentLine,
      @Nonnull JsonArray<Pair<Integer, String>> modes) {
    Preconditions.checkState(currentLine instanceof Line);
    // TODO: pull AnchorManager.getAnchorsByTypeOrNull to
    // TaggableLine interface (for decoupling).
    Line line = (Line) currentLine;
    AnchorManager anchorManager = line.getDocument().getAnchorManager();
    Preconditions.checkNotNull(anchorManager);
    JsonArray<Anchor> oldAnchors =
        AnchorManager.getAnchorsByTypeOrNull(line, MODE_ANCHOR_TYPE);
    if (oldAnchors != null) {
      for (Anchor anchor : oldAnchors.asIterable()) {
        anchorManager.removeAnchor(anchor);
      }
    }
    for (Pair<Integer, String> pair : modes.asIterable()) {
      Anchor anchor = anchorManager.createAnchor(MODE_ANCHOR_TYPE, line,
          AnchorManager.IGNORE_LINE_NUMBER, pair.first);
      anchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
      anchor.setValue(pair.second);
    }
  }
}
