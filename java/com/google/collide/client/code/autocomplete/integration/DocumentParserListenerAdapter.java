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

package com.google.collide.client.code.autocomplete.integration;

import com.google.collide.client.code.autocomplete.Autocompleter;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Line;

import javax.annotation.Nonnull;

/**
 * Listener implementation that adapts messages for {@link Autocompleter}.
 */
public class DocumentParserListenerAdapter implements DocumentParser.Listener {

  private final Autocompleter autocompleter;
  private final Editor editor;
  private boolean asyncParsing;
  boolean cursorLineParsed;
  boolean documentParsingFinished;

  public DocumentParserListenerAdapter(Autocompleter autocompleter, Editor editor) {
    this.autocompleter = autocompleter;
    this.editor = editor;
  }

  @Override
  public void onIterationStart(int lineNumber) {
    asyncParsing = true;
    cursorLineParsed = false;
    documentParsingFinished = false;
    autocompleter.getCodeAnalyzer().onBeforeParse();
  }

  @Override
  public void onIterationFinish() {
    asyncParsing = false;
    autocompleter.getCodeAnalyzer().onAfterParse();
    if (documentParsingFinished) {
      autocompleter.onDocumentParsingFinished();
    }
    if (cursorLineParsed) {
      autocompleter.onCursorLineParsed();
    }
  }

  @Override
  public void onDocumentLineParsed(Line line, int lineNumber, @Nonnull JsonArray<Token> tokens) {
    if (asyncParsing) {
      TaggableLine previousLine = TaggableLineUtil.getPreviousLine(line);
      autocompleter.getCodeAnalyzer().onParseLine(previousLine, line, tokens);
      if (editor.getSelection().getCursorLineNumber() == lineNumber) {
        cursorLineParsed = true;
      }
      if (line.getNextLine() == null) {
        documentParsingFinished = true;
      }
    }
  }
}
