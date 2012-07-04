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

package com.google.collide.client.editor.search;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

/**
 */
public class SearchTestsUtil {
  // I needed some lines for searching
  public static final ImmutableList<String> DOCUMENT_LINES =
      ImmutableList.of("What do tigers dream of?",
          "When they take a little tiger snooze",
          "Do they dream of mauling zebras",
          "Or halli Barry in her catwoman suit.",
          "Don't you worry you're pretty striped head",
          "Were gonna get you back to Tyson and your cozy tiger bed",
          "And then were gonna find our best friend Doug",
          "And then were gonna give him a best friend hug",
          "Doug Doug Oh Doug Doug Dougie Doug Doug",
          "But if he's been murdered by crystal meth tweekers...",
          "Well then were shit outta luck.",
          "Awesome Awesome");

  /**
   * Mocks a document with the util document lines, does not replay the document
   * before returning.
   */
  public static Document createDocument() {
    return Document.createFromString(Joiner.on('\n').join(DOCUMENT_LINES));
  }
  
  /**
   * Creates a viewport from a document
   *
   * @param lines Number of lines to include in viewport
   */
  public static ViewportModel createMockViewport(Document document, int lines) {
    ViewportModel mockViewport = EasyMock.createMock(ViewportModel.class);

    Line curLine = document.getFirstLine();
    expect(mockViewport.getTopLine()).andReturn(curLine).anyTimes();
    expect(mockViewport.getTopLineNumber()).andReturn(0).anyTimes();
    expect(mockViewport.getTopLineInfo()).andAnswer(lineInfoFactory(curLine, 0)).anyTimes();
    for (int i = 0; i < lines - 1 && curLine.getNextLine() != null; i++) {
      curLine = curLine.getNextLine();
    }
    expect(mockViewport.getBottomLine()).andReturn(curLine).anyTimes();
    expect(mockViewport.getBottomLineNumber()).andReturn(lines - 1).anyTimes();
    expect(mockViewport.getBottomLineInfo()).andAnswer(
        lineInfoFactory(curLine, lines - 1)).anyTimes();

    replay(mockViewport);
    return mockViewport;
  }


  /**
   * Retrieves the line info of a given line
   */
  public static LineInfo gotoLineInfo(Document document, int line) {
    Line curLine = document.getFirstLine();
    for (int i = 0; curLine != null && i < line; i++) {
      curLine = curLine.getNextLine();
    }
    return new LineInfo(curLine, line);
  }

  /**
   * Produces a new line info with the given parameters everytime it is called.
   * This is required due to LineInfo being mutable.
   */
  public static IAnswer<LineInfo> lineInfoFactory(final Line line, final int number) {
    return new IAnswer<LineInfo>() {
      @Override
      public LineInfo answer() throws Throwable {
        return new LineInfo(line, number);
      }
    };
  }


  /**
   * Stub of a mock manager that easily keeps track of the number of matches
   * added without a bunch of other logic. Don't call methods such as findNext
   * or findPrevious matches it won't appreciate it.
   *
   */
  public static class StubMatchManager extends SearchMatchManager {

    public StubMatchManager(Document document) {
      super(document, EasyMock.createNiceMock(SelectionModel.class), EasyMock.createNiceMock(
          DocumentMutator.class), EasyMock.createNiceMock(SearchTask.class));
    }

    @Override
    public void addMatches(LineInfo line, int matches) {
      totalMatches += matches;
    }

    @Override
    public void clearMatches() {
      totalMatches = 0;
    }
  }
}
