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

package com.google.collide.client.code.parenmatch;

import static com.google.collide.client.editor.search.SearchTestsUtil.createMockViewport;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.client.editor.search.SearchTask.SearchDirection;
import com.google.collide.client.editor.selection.SelectionModel.CursorListener;
import com.google.collide.client.testing.StubIncrementalScheduler;
import com.google.collide.client.util.IncrementalScheduler;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests the ParenMatchHighlighter's ability to find the correct match
 * 
 */
public class ParenMatchHighlighterTests extends TestCase {

  private Document document;
  private ParenMatchHighlighter parenMatchHighlighter;
  private AnchorManager mockAnchorManager;

  public void customSetUp(ImmutableList<String> lines) {
    document = Document.createFromString(Joiner.on('\n').join(lines));
    mockAnchorManager = EasyMock.createMock(AnchorManager.class);

    ParenMatchHighlighter.Css mockCss = EasyMock.createNiceMock(ParenMatchHighlighter.Css.class);
    expect(mockCss.match()).andReturn("whatever");
    replay(mockCss);

    ParenMatchHighlighter.Resources mockResources =
        EasyMock.createNiceMock(ParenMatchHighlighter.Resources.class);
    expect(mockResources.parenMatchHighlighterCss()).andReturn(mockCss);
    replay(mockResources);

    Renderer mockRenderer = EasyMock.createNiceMock(Renderer.class);
    replay(mockRenderer);

    IncrementalScheduler stubScheduler = new StubIncrementalScheduler(10, 1000);

    ListenerRegistrar<CursorListener> listenerRegistrar = ListenerManager.create();

    parenMatchHighlighter =
        new ParenMatchHighlighter(document, createMockViewport(document, lines.size()),
            mockAnchorManager, mockResources, mockRenderer, stubScheduler, listenerRegistrar);
  }

  public void testMatchDownSameLine() {
    customSetUp(ImmutableList.of("(text {in} between)"));
    LineInfo startLineInfo = document.getFirstLineInfo();

    // should find the match on line 1, at column 16
    expect(
        mockAnchorManager.createAnchor(EasyMock.eq(ParenMatchHighlighter.MATCH_ANCHOR_TYPE),
            EasyMock.eq(startLineInfo.line()), EasyMock.eq(0), EasyMock.eq(18))).andReturn(null);

    replay(mockAnchorManager);

    parenMatchHighlighter.search(SearchDirection.DOWN, ')', '(', startLineInfo, 1);

    verify(mockAnchorManager);
  }

  public void testMatchUpSameLine() {
    customSetUp(ImmutableList.of("<img src='img.jpg' name='test'>"));
    LineInfo startLineInfo = document.getFirstLineInfo();

    // should fine the match one line 1, column 0
    expect(
        mockAnchorManager.createAnchor(EasyMock.eq(ParenMatchHighlighter.MATCH_ANCHOR_TYPE),
            EasyMock.eq(startLineInfo.line()), EasyMock.eq(0), EasyMock.eq(0))).andReturn(null);

    replay(mockAnchorManager);

    parenMatchHighlighter.search(SearchDirection.UP, '<', '>', startLineInfo, 31);

    verify(mockAnchorManager);
  }

  /**
   * Starts after first { and searches down for } <code>
   * function foo() {
   *   var temp = 1;
   * }
   * </code>
   */
  public void testMatchDownDifferentLines() {
    customSetUp(ImmutableList.of("function foo() {", "  var temp = 1;", "}"));
    Line startLine = document.getFirstLine();
    Line matchLine = document.getLastLine();

    // should find the match on line 3, column 0
    expect(
        mockAnchorManager.createAnchor(EasyMock.eq(ParenMatchHighlighter.MATCH_ANCHOR_TYPE),
            EasyMock.eq(matchLine), EasyMock.eq(3), EasyMock.eq(0))).andReturn(null);
    replay(mockAnchorManager);

    parenMatchHighlighter.search(SearchDirection.DOWN, '}', '{', new LineInfo(startLine, 1), 16);

    verify(mockAnchorManager);
  }

  /**
   * Starts after last ] and searches up for [ <code>
   * var list = ['str1',
   *    'str2',
   *    'str3']
   * </code>
   */
  public void testMatchUpDifferentLines() {
    final ImmutableList<String> documentText =
        ImmutableList.of("var list = ['str1',", "    'str2',", "    'str3']");
    customSetUp(documentText);
    LineInfo startLineInfo = document.getLastLineInfo();
    Line matchLine = document.getFirstLine();

    // should find the match on line 1, column 11
    expect(
        mockAnchorManager.createAnchor(EasyMock.eq(ParenMatchHighlighter.MATCH_ANCHOR_TYPE),
            EasyMock.eq(matchLine), EasyMock.eq(0), EasyMock.eq(11))).andReturn(null);
    replay(mockAnchorManager);

    parenMatchHighlighter.search(SearchDirection.UP, '[', ']', startLineInfo, 10);

    verify(mockAnchorManager);
  }

  /**
   * Starts after the first [ and searches down for the closing ]. It should
   * skip the open and close brackets in between. <code>
   * var list = ['str1', 
   *    'str2',
   *    ['sub1','sub2'],
   *    'str3']
   * </code>
   */
  public void testMatchWithFalseMatches() {
    final ImmutableList<String> documentText =
        ImmutableList.of("var list = ['str1',", "'str2',", "   ['sub1','sub2'],", "   'str3']");
    customSetUp(documentText);
    Line startLine = document.getFirstLine();
    Line matchLine = document.getLastLine();

    // should find match on line 4, column 11
    expect(
        mockAnchorManager.createAnchor(EasyMock.eq(ParenMatchHighlighter.MATCH_ANCHOR_TYPE),
            EasyMock.eq(matchLine), EasyMock.eq(4), EasyMock.eq(9))).andReturn(null);
    replay(mockAnchorManager);

    parenMatchHighlighter.search(SearchDirection.DOWN, ']', '[', new LineInfo(startLine, 1), 12);

    verify(mockAnchorManager);
  }

}
