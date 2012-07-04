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

import static com.google.collide.codemirror2.TokenType.ATTRIBUTE;
import static com.google.collide.codemirror2.TokenType.TAG;

import com.google.collide.client.code.autocomplete.CodeAnalyzer;
import com.google.collide.client.util.collections.StringMultiset;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.codemirror2.Token;
import com.google.collide.codemirror2.TokenType;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.util.JsonCollections;

import javax.annotation.Nonnull;

/**
 * Analyzes token stream and builds or updates {@link HtmlTagWithAttributes}.
 *
 * <p>For each line we hold:<ol>
 * <li> {@link HtmlTagWithAttributes} unfinished at the beginning of line
 * <li> {@link HtmlTagWithAttributes} unfinished at the end of line
 * <li> list of attributes added to (1) in this line
 * </ol>
 *
 * <p>When line is reparsed:<ol>
 * <li> remove attributes from the list from appropriate tag and clean list
 * <li> set unfinished beginning tag equal to the ending tag of previous line
 * <li> add attributes to list until tag closes
 * <li> build and set unfinished tag at the end of line
 * </ol>
 *
 * <p> That way, during completion we have 2 cases:<ul>
 * <li> for tag that starts and ends in this line we need to parse it's content
 * <li> for tag that is unfinished (starts in previous lines or end in the
 * following lines) we already have parsed tag information.
 * </ul>
 *
 */
public class XmlCodeAnalyzer implements CodeAnalyzer {

  static final String TAG_START_TAG = HtmlAutocompleter.class.getName() + ".startTag";
  static final String TAG_END_TAG = HtmlAutocompleter.class.getName() + ".endTag";
  private static final String TAG_ATTRIBUTES = HtmlAutocompleter.class.getName() + ".attributes";

  @Override
  public void onBeforeParse() {
    // Do nothing.
  }

  @Override
  public void onParseLine(
      TaggableLine previousLine, TaggableLine line, @Nonnull JsonArray<Token> tokens) {
    processLine(previousLine, line, tokens);
  }

  static void processLine(
      TaggableLine previousLine, TaggableLine line, @Nonnull JsonArray<Token> tokens) {
    // Ignore case always, as HTML == XmlCodeAnalyzer for now.
    final boolean ignoreCase = true;

    clearLine(line);
    HtmlTagWithAttributes tag = previousLine.getTag(TAG_END_TAG);
    line.putTag(TAG_START_TAG, tag);

    int index = 0;
    int size = tokens.size();

    boolean inTag = false;
    int lastTagTokenIndex = -1;

    if (tag != null) {
      inTag = true;
      boolean newAttributes = false;
      JsonArray<String> attributes = line.getTag(TAG_ATTRIBUTES);
      if (attributes == null) {
        newAttributes = true;
        attributes = JsonCollections.createArray();
      }

      StringMultiset tagAttributes = tag.getAttributes();

      while (index < size) {
        Token token = tokens.get(index);
        index++;
        TokenType tokenType = token.getType();
        if (ATTRIBUTE == tokenType) {
          String attribute = token.getValue();
          attribute = ignoreCase ? attribute.toLowerCase() : attribute;
          attributes.add(attribute);
          tagAttributes.add(attribute);
        } else if (TAG == tokenType) {
          // Tag closing token
          tag.setDirty(false);
          inTag = false;
          break;
        }
      }
      if (newAttributes && attributes.size() != 0) {
        line.putTag(TAG_ATTRIBUTES, attributes);
      } else if (!newAttributes && attributes.size() == 0) {
        line.putTag(TAG_ATTRIBUTES, null);
      }
    } else {
      line.putTag(TAG_ATTRIBUTES, null);
    }

    while (index < size) {
      Token token = tokens.get(index);
      index++;
      TokenType tokenType = token.getType();
      if (TAG == tokenType) {
        if (inTag) {
          if (">".equals(token.getValue()) || "/>".equals(token.getValue())) {
            // If type is "tag" and content is ">", this is HTML token.
            inTag = false;
          }
        } else {
          // Check that we are in html mode.
          if (CodeMirror2.HTML.equals(token.getMode())) {
            lastTagTokenIndex = index - 1;
            inTag = true;
          }
        }
      }
    }

    if (inTag) {
      if (lastTagTokenIndex != -1) {
        index = lastTagTokenIndex;
        Token token = tokens.get(index);
        index++;
        String tagName = token.getValue().substring(1).trim();
        tag = new HtmlTagWithAttributes(tagName);
        StringMultiset tagAttributes = tag.getAttributes();
        while (index < size) {
          token = tokens.get(index);
          index++;
          TokenType tokenType = token.getType();
          if (ATTRIBUTE == tokenType) {
            String attribute = token.getValue();
            tagAttributes.add(ignoreCase ? attribute.toLowerCase() : attribute);
          }
        }
      }

      // In case when document ends, but last tag is not closed we state that
      // tag content is "complete" - i.e. it will not be updated further.
      if (line.isLastLine()) {
        tag.setDirty(false);
      }

      line.putTag(TAG_END_TAG, tag);
    } else {
      line.putTag(TAG_END_TAG, null);
    }
  }

  @Override
  public void onAfterParse() {
    // Do nothing.
  }

  @Override
  public void onLinesDeleted(JsonArray<TaggableLine> deletedLines) {
    for (TaggableLine line : deletedLines.asIterable()) {
      clearLine(line);
    }
  }

  private static void clearLine(TaggableLine line) {
    HtmlTagWithAttributes tag = line.getTag(TAG_START_TAG);
    if (tag == null) {
      return;
    }
    tag.setDirty(true);
    JsonArray<String> attributes = line.getTag(TAG_ATTRIBUTES);
    if (attributes == null) {
      return;
    }
    tag.getAttributes().removeAll(attributes);
    attributes.clear();
  }
}
