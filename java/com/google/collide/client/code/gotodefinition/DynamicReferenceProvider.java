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

import javax.annotation.Nullable;

import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.FileTreeModel;
import com.google.collide.client.workspace.FileTreeNode;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.LineInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Strictly speaking, this class gives an answer to a question "is there a
 * reference at given file position?". It uses local parser with a delay to
 * find URL links / local file links or anchor references.
 * It is "dynamic" in a sense that it does not keep any state.
 */
public class DynamicReferenceProvider {

  // Word chars, digits or dash, at least one.
  private static final String DOMAIN_CHARS = "[\\w\\-\\d]+";

  // ":" plus at least one digit, optional.
  private static final String PORT_CHARS = "(\\:\\d+)?";

  @VisibleForTesting
  static final RegExp REGEXP_URL =
      RegExp.compile("\\b(https?|ftp)://(" + DOMAIN_CHARS + "\\.)*"
          + DOMAIN_CHARS + PORT_CHARS + "[^\\.\\s\\\"']*(\\.[^\\.\\s\\\"']+)*", "gi");

  private final String contextPath;
  private final DeferringLineParser parser;
  private final FileTreeModel fileTreeModel;
  private final AnchorTagParser anchorTagParser;

  public DynamicReferenceProvider(String contextPath, DeferringLineParser parser,
      FileTreeModel fileTreeModel, @Nullable AnchorTagParser anchorTagParser) {
    this.contextPath = contextPath;
    this.parser = parser;
    this.fileTreeModel = fileTreeModel;
    this.anchorTagParser = anchorTagParser;
  }

  /**
   * Attemps to find a reference at given position. This method cannot find any
   * references if line is not yet parsed. This is always true if the method
   * was not called before. {@code blocking} flag tells whether we should wait
   * until the line is parsed and find a reference.
   * 
   * @param lineInfo line to look reference at
   * @param column column to look reference at
   * @param blocking whether to block until given line is parsed
   * @return found reference at given position or {@code null} if line is not
   *     yet parsed (happens only when {@code blocking} is {@code false} OR
   *     if there's not reference at given position
   */
  NavigableReference getReferenceAt(LineInfo lineInfo, int column, boolean blocking) {
    JsonArray<Token> parsedLineTokens = parser.getParseResult(lineInfo, blocking);
    // TODO: We should get parser state here.
    if (parsedLineTokens == null) {
      return null;
    }

    return getReferenceAt(lineInfo, column, parsedLineTokens);
  }

  @VisibleForTesting
  NavigableReference getReferenceAt(LineInfo lineInfo, int column, JsonArray<Token> tokens) {
    /* We care about:
     *  - "href" attribute values in "a" tag, looking for anchors defined elsewhere,
     *  - all comment and string literals, looking for urls,
     *  - "src" or "href" attribute values, looking for urls and local file paths.
     */

    boolean inAttribute = false;
    boolean inAnchorTag = false;
    boolean inHrefAttribute = false;
    int tokenEndColumn = 0;
    for (int i = 0, l = tokens.size() - 1; i < l; i++) {
      Token token = tokens.get(i);
      TokenType type = token.getType();
      String value = token.getValue();
      int tokenStartColumn = tokenEndColumn;
      tokenEndColumn += value.length();  // Exclusive.
      if (type == TokenType.TAG) {
        if (">".equals(value) || "/>".equals(value)) {
          inAttribute = false;
          inHrefAttribute = false;
        }
        inAnchorTag = "<a".equalsIgnoreCase(value);
        continue;
      } else if (type == TokenType.ATTRIBUTE) {
        if (inAnchorTag && "href".equals(value)) {
          inHrefAttribute = true;
          inAttribute = true;
        } else if ("src".equals(value) || "href".equals(value)) {
          inAttribute = true;
        }
        continue;
      } else if (tokenEndColumn <= column) {
        // Too early.
        continue;
      } else if (tokenStartColumn > column) {
        // We went too far, we have nothing.
        return null;
      } else if (type != TokenType.STRING && type != TokenType.COMMENT) {
        continue;
      }
      // So now the token covers given position and we're in a string/comment or we're in attribute
      // "src" or "href". Awesome!

      int lineNumber = lineInfo.number();
      int valueStartColumn = tokenStartColumn;
      int valueEndColumn = tokenEndColumn;  // Exclusive.
      String valueWithoutQuotes = value;
      if (inAttribute && value.startsWith("\"") && value.endsWith("\"")) {
        valueWithoutQuotes = value.substring(1, value.length() - 1);
        valueStartColumn++;
        valueEndColumn--;
      }

      if (valueStartColumn > column || column >= valueEndColumn) {
        continue;
      }

      // Now check if the value is a workspace file path.
      if (inAttribute) {
        FileTreeNode fileNode = findFileNode(valueWithoutQuotes);
        if (fileNode != null) {
          int filePathEndColumn = valueEndColumn - 1;  // Incl.
          return NavigableReference.createToFile(lineNumber, valueStartColumn, filePathEndColumn,
              fileNode.getNodePath().getPathString());
        }
      }

      // Now check if the value is an URL.
      REGEXP_URL.setLastIndex(0);
      for (MatchResult matchResult = REGEXP_URL.exec(valueWithoutQuotes); matchResult != null;
           matchResult = REGEXP_URL.exec(valueWithoutQuotes)) {
        int matchColumn = valueStartColumn + matchResult.getIndex();
        int matchEndColumn = matchColumn + matchResult.getGroup(0).length() - 1;  // Inclusive.
        if (matchEndColumn < column) {
          // Too early.
          continue;
        }
        if (matchColumn > column) {
          // Too far.
          return null;
        }
        return NavigableReference.createToUrl(lineNumber, matchColumn, matchResult.getGroup(0));
      }

      // Now check if the value is the name of the anchor tag.
      if (inHrefAttribute && valueWithoutQuotes.startsWith("#")) {
        AnchorTagParser.AnchorTag anchorTag = findAnchorTag(valueWithoutQuotes.substring(1));
        if (anchorTag != null) {
          return NavigableReference.createToFile(
              lineNumber, valueStartColumn, valueEndColumn - 1, contextPath,
              anchorTag.getLineNumber(), anchorTag.getColumn());
        }
      }
    }

    return null;
  }

  @VisibleForTesting
  FileTreeNode findFileNode(String displayPath) {
    PathUtil lookupPath = new PathUtil(displayPath);
    if (!displayPath.startsWith("/")) {
      PathUtil contextDir = PathUtil.createExcludingLastN(new PathUtil(contextPath), 1);
      lookupPath = PathUtil.concatenate(contextDir, lookupPath);
    }
    return fileTreeModel.getWorkspaceRoot().findChildNode(lookupPath);
  }

  private AnchorTagParser.AnchorTag findAnchorTag(String name) {
    if (anchorTagParser == null) {
      return null;
    }
    JsonArray<AnchorTagParser.AnchorTag> anchorTags = anchorTagParser.getAnchorTags();
    for (int i = 0; i < anchorTags.size(); i++) {
      if (anchorTags.get(i).getName().equalsIgnoreCase(name)) {
        return anchorTags.get(i);
      }
    }
    return null;
  }
}
