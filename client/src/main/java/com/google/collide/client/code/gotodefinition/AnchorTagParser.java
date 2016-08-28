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

package com.google.collide.client.code.gotodefinition;

import com.google.collide.client.documentparser.AsyncParser;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.documentparser.ParseResult;
import com.google.collide.client.util.logging.Log;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.codemirror2.XmlState;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.StringUtils;

/**
 * Extracts anchor tags with names using document parser, asynchronously.
 */
public class AnchorTagParser extends AsyncParser<AnchorTagParser.AnchorTag> {

  static class AnchorTag implements AsyncParser.LineAware {
    private final int lineNumber;
    private final int column;
    private final String name;

    AnchorTag(int lineNumber, int column, String name) {
      this.lineNumber = lineNumber;
      this.column = column;
      this.name = name;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
    }

    public int getColumn() {
      return column;
    }

    public String getName() {
      return name;
    }
  }

  private final ListenerRegistrar.Remover listenerRemover;
  private final DocumentParser parser;
  private JsonArray<AnchorTag> anchorTags;

  private boolean inAnchorTag = false;
  private boolean inNameAttribute = false;

  /**
   * Creates and registers anchor parser.
   */
  public AnchorTagParser(DocumentParser parser) {
    this.parser = parser;
    listenerRemover = parser.getListenerRegistrar().add(this);
  }

  @Override
  public void onParseLine(Line line, int lineNumber, JsonArray<Token> tokens) {
    ParseResult<XmlState> parserState =
        parser.getState(XmlState.class, new Position(new LineInfo(line, lineNumber), 0), null);
    if (parserState != null && parserState.getState() != null
        && parserState.getState().getContext() != null
        && "a".equalsIgnoreCase(parserState.getState().getContext().getTagName())) {
      inAnchorTag = true;
    }
    int tokenEndColumn = 0;
    for (int i = 0, l = tokens.size() - 1; i < l; i++) {
      Token token = tokens.get(i);
      TokenType type = token.getType();
      String value = token.getValue();
      int tokenStartColumn = tokenEndColumn;
      tokenEndColumn += value.length();
      if (type == TokenType.TAG) {
        if (">".equals(value) || "/>".equals(value)) {
          inAnchorTag = false;
          inNameAttribute = false;
        } else if ("<a".equalsIgnoreCase(value)) {
          inAnchorTag = true;
          inNameAttribute = false;
        }
        continue;
      } else if (inAnchorTag && type == TokenType.ATTRIBUTE
          && ("name".equals(value) || "id".equals(value))) {
        inNameAttribute = true;
        continue;
      } else if (inNameAttribute && type == TokenType.STRING) {
        int valueStartColumn = tokenStartColumn;
        if (value.startsWith("\"") && value.endsWith("\"")) {
          value = value.substring(1, value.length() - 1);
          valueStartColumn++;
        }
        if (value.length() > 0 && !StringUtils.isNullOrWhitespace(value)) {
          Log.debug(getClass(), "Found anchor tag with name \"" + value + "\" at ("
              + lineNumber + "," + valueStartColumn + ")");
          addData(new AnchorTag(lineNumber, valueStartColumn, value));
        }
      }
    }
  }

  @Override
  protected void onAfterParse(JsonArray<AnchorTag> tags) {
    anchorTags = tags;
  }

  public JsonArray<AnchorTag> getAnchorTags() {
    return anchorTags;
  }

  /**
   * Cleanup object.
   *
   * After cleanup is invoked this instance should never be used.
   */
  @Override
  public void cleanup() {
    super.cleanup();
    listenerRemover.remove();
  }
}
