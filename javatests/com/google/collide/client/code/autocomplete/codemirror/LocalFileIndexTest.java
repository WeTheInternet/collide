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

package com.google.collide.client.code.autocomplete.codemirror;

import static com.google.collide.client.code.autocomplete.TestUtils.CTRL_SHIFT_SPACE;
import static com.google.collide.client.code.autocomplete.TestUtils.createNameSet;
import static com.google.collide.shared.util.JsonCollections.createStringSet;

import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.codegraph.ParsingTask;
import com.google.collide.client.code.autocomplete.integration.TaggableLineUtil;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.testutil.CodeMirrorTestCase;
import com.google.collide.client.util.IncrementalScheduler;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar.Remover;

import javax.annotation.Nonnull;

/**
 * Test cases that check that local file index is built and updates correctly.
 *
 */
public class LocalFileIndexTest extends CodeMirrorTestCase {

  private ParsingTask analyzer;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testParse() {
    String text = "var aaa;\nvar bbb;\n";

    MockAutocompleterEnvironment helper = configureHelper(text);
    AutocompleteProposals proposals =
        helper.autocompleter.jsAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SHIFT_SPACE);
    assertEquals("variable set", createStringSet("aaa", "bbb"), createNameSet(proposals));
  }

  private MockAutocompleterEnvironment configureHelper(String text) {
    final MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    helper.setup(new PathUtil("foo.js"), text, 0, 0, true);
    analyzer = new ParsingTask(helper.autocompleter.localPrefixIndexStorage);
    JsonArray<IncrementalScheduler.Task> parseRequests = helper.parseScheduler.requests;

    assertEquals("parsing not scheduled initially", 0, parseRequests.size());

    helper.parser.getListenerRegistrar().add(new DocumentParser.Listener() {
      private boolean asyncParsing;

      @Override
      public void onIterationStart(int lineNumber) {
        asyncParsing = true;
        analyzer.onBeforeParse();
      }

      @Override
      public void onIterationFinish() {
        asyncParsing = false;
        analyzer.onAfterParse();
      }

      @Override
      public void onDocumentLineParsed(
          Line line, int lineNumber, @Nonnull JsonArray<Token> tokens) {
        if (asyncParsing) {
          TaggableLine previousLine = TaggableLineUtil.getPreviousLine(line);
          analyzer.onParseLine(previousLine, line, tokens);
        }
      }
    });

    helper.parser.begin();

    assertEquals("parse scheduled", 1, parseRequests.size());
    parseRequests.get(0).run(50);
    parseRequests.clear();
    return helper;
  }

  public void testDeleteLines() {
    String text = ""
        + "var aaa;\n"
        + "var bbb;\n"
        + "var ccc;\n"
        + "var ddd;\n"
        + "var eee;\n"
        + "var fff;\n";
    MockAutocompleterEnvironment helper = configureHelper(text);

    AutocompleteProposals proposals =
        helper.autocompleter.jsAutocompleter.findAutocompletions(
            helper.editor.getSelection(), CTRL_SHIFT_SPACE);
    assertEquals("variable set", createStringSet(
        "aaa", "bbb", "ccc", "ddd", "eee", "fff"), createNameSet(proposals));

    Document document = helper.editor.getDocument();

    Remover remover = document.getLineListenerRegistrar().add(new Document.LineListener() {
      @Override
      public void onLineAdded(Document document, int lineNumber, JsonArray<Line> addedLines) {
      }

      @Override
      public void onLineRemoved(Document document, int lineNumber,
          JsonArray<Line> removedLines) {
        JsonArray<TaggableLine> deletedLines = JsonCollections.createArray();
        for (final Line line : removedLines.asIterable()) {
          deletedLines.add(line);
        }
        analyzer.onLinesDeleted(deletedLines);
      }
    });

    LineFinder lineFinder = document.getLineFinder();
    LineInfo line2 = lineFinder.findLine(2);
    LineInfo line4 = lineFinder.findLine(4);
    String textToDelete = LineUtils.getText(line2.line(), 0, line4.line(), 0);
    helper.editor.getEditorDocumentMutator().deleteText(line2.line(), 2, 0, textToDelete.length());
    remover.remove();

    JsonArray<IncrementalScheduler.Task> parseRequests = helper.parseScheduler.requests;
    assertEquals("reparse scheduled", 1, parseRequests.size());
    parseRequests.get(0).run(50);

    proposals = helper.autocompleter.jsAutocompleter.findAutocompletions(
        helper.editor.getSelection(), CTRL_SHIFT_SPACE);
    assertEquals("new variable set", createStringSet(
        "aaa", "bbb", "eee", "fff"), createNameSet(proposals));
  }
}
