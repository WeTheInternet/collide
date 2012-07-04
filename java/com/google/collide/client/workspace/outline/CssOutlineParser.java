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

import static com.google.collide.client.workspace.outline.OutlineNode.OUTLINE_NODE_ANCHOR_TYPE;
import static com.google.collide.client.workspace.outline.OutlineNode.OutlineNodeType.CSS_CLASS;
import static com.google.collide.shared.document.anchor.AnchorManager.IGNORE_LINE_NUMBER;

import com.google.collide.client.documentparser.AsyncParser;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.codemirror2.CssToken;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.common.base.Preconditions;

/**
 * Parser for CSS files.
 *
 * Consumes tokens from codemirror2, produces OutlineNodes.
 */
public class CssOutlineParser extends AsyncParser<OutlineNode> implements OutlineParser {

  /**
   * Parent object that is notified, when parsing is complete.
   */
  private final OutlineConsumer consumer;

  /**
   * Root node.
   *
   * Root node is created only once, updated by model.
   */
  private final OutlineNode root;

  /**
   * Anchor that denotes a first position where we get "tag" or "atom" token.
   */
  private Anchor tagAnchor;

  /**
   * Outline node name represented as array of strings.
   */
  private JsonArray<String> tagName;

  /**
   * Flag that indicates, that there were unused space token.
   */
  private boolean spaceBetween;

  /**
   * Handle for listener unregistration.
   */
  private ListenerRegistrar.Remover listenerRemover;

  @Override
  public void onParseLine(Line line, int lineNumber, JsonArray<Token> tokens) {
    int column = 0;
    for (Token token : tokens.asIterable()) {
      TokenType type = token.getType();
      String value = token.getValue();
      column = column + value.length();

      if (TokenType.WHITESPACE == type || TokenType.NEWLINE == type) {
        spaceBetween = true;
        continue;
      }

      if (TokenType.COMMENT == type || TokenType.VARIABLE == type || TokenType.NUMBER == type) {
        continue;
      }

      Preconditions.checkState(token instanceof CssToken,
          "Expected CssToken, but received %s", token);
      String context = ((CssToken) token).getContext();

      boolean freeContext = context == null || "@media{".equals(context);
      if (!freeContext || ",".equals(value) || "}".equals(value) || "{".equals(value)) {
        // Node is finished, push it to the list.
        if (tagAnchor != null) {
          OutlineNode item = new OutlineNode(tagName.join(""), CSS_CLASS, root, lineNumber, 0);
          addData(item);
          item.setEnabled(true);
          item.setAnchor(tagAnchor);
          tagAnchor = null;
          tagName.clear();
        }
        spaceBetween = false;
      } else {
        if (tagAnchor == null) {
          tagAnchor = line.getDocument().getAnchorManager().createAnchor(
              OUTLINE_NODE_ANCHOR_TYPE, line, IGNORE_LINE_NUMBER, column - value.length());
        }
        if (spaceBetween && tagName.size() > 0) {
          tagName.add(" ");
        }
        spaceBetween = false;
        tagName.add(value);
      }
    }
    spaceBetween = true;
  }

  @Override
  public void onAfterParse(JsonArray<OutlineNode> nodes) {
    consumer.onOutlineParsed(nodes);
  }

  @Override
  public void onBeforeParse() {
    // TODO: restore tagName and tagAnchor
    tagName.clear();
    spaceBetween = false;
    detachLastAnchor();
  }

  private void detachLastAnchor() {
    if (tagAnchor != null) {
      if (tagAnchor.isAttached()) {
        tagAnchor.getLine().getDocument().getAnchorManager().removeAnchor(tagAnchor);
      }
      tagAnchor = null;
    }
  }

  @Override
  public void onCleanup(JsonArray<OutlineNode> nodes) {
    final int l = nodes.size();
    if (l > 0) {
      AnchorManager anchorManager =
          nodes.get(0).getAnchor().getLine().getDocument().getAnchorManager();
      for (int i = 0; i < l; i++) {
        Anchor anchor = nodes.get(i).getAnchor();
        if (anchor.isAttached()) {
          anchorManager.removeAnchor(anchor);
        }
      }
    }
  }

  @Override
  public void cleanup() {
    super.cleanup();
    detachLastAnchor();
    listenerRemover.remove();
    listenerRemover = null;
  }

  public CssOutlineParser(ListenerRegistrar<DocumentParser.Listener> parserListenerRegistrar,
      OutlineConsumer consumer) {
    this.consumer = consumer;
    listenerRemover = parserListenerRegistrar.add(this);
    root = new OutlineNode("css-root", OutlineNode.OutlineNodeType.ROOT, null, 0, 0);
    tagName = JsonCollections.createArray();
  }

  @Override
  public OutlineNode getRoot() {
    return root;
  }
}
