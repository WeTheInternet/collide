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

import static com.google.collide.client.editor.search.SearchTestsUtil.createDocument;
import static com.google.collide.client.editor.search.SearchTestsUtil.createMockViewport;
import static com.google.collide.client.editor.search.SearchTestsUtil.gotoLineInfo;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.not;
import static org.easymock.EasyMock.or;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.collide.client.AppContext;
import com.google.collide.client.Resources;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.renderer.LineRenderer;
import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.client.editor.search.SearchMatchRenderer.Css;
import com.google.collide.client.editor.search.SearchModel.SearchProgressListener;
import com.google.collide.client.editor.search.SearchTestsUtil.StubMatchManager;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.testing.StubIncrementalScheduler;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.gwt.regexp.shared.RegExp;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests the search model
 */
public class SearchModelTests extends TestCase {

  private static AppContext createMockContext() {
    AppContext mockContext = EasyMock.createMock(AppContext.class);
    Resources mockResources = EasyMock.createMock(Resources.class);
    Css mockCss = EasyMock.createMock(Css.class);

    expect(mockContext.getResources()).andReturn(mockResources).anyTimes();
    expect(mockResources.searchMatchRendererCss()).andReturn(mockCss).anyTimes();
    expect(mockCss.match()).andReturn("match").anyTimes();

    replay(mockCss, mockResources, mockContext);
    return mockContext;
  }
  
  private static SelectionModel createMockSelectionModel(Document document) {
    SelectionModel selection = EasyMock.createMock(SelectionModel.class);

    expect(selection.getCursorLine()).andReturn(document.getFirstLine()).anyTimes();
    expect(selection.getCursorLineNumber()).andReturn(0).anyTimes();

    replay(selection);
    return selection;
  }

  public void testSetQueryNullOrEmpty() {
    Document document = createDocument();

    // Setup the renderer
    Renderer mockRenderer = EasyMock.createNiceMock(Renderer.class);
    mockRenderer.addLineRenderer(anyObject(LineRenderer.class));
    mockRenderer.removeLineRenderer(anyObject(LineRenderer.class));
    replay(mockRenderer);

    AppContext mockContext = createMockContext();
    ViewportModel mockView = createMockViewport(document, 4);

    SearchModel model = SearchModel.createWithManagerAndScheduler(mockContext,
        document,
        mockRenderer,
        mockView,
        new StubMatchManager(document),
        new StubIncrementalScheduler(10, 1000),
        createMockSelectionModel(document));
    // Almost nothing is performed here since there was no previous query
    model.setQuery("");
    model.setQuery("a");
    model.setQuery("");

    try {
      model.setQuery(null);
      fail("Did not throw Illegal Argument Exception on Null");
    } catch (IllegalArgumentException e) {
    }

    // verify
    verify(mockRenderer, mockView, mockContext);
  }

  public void testSetQueryReturnsNumberOfMatches() {
    Document document = createDocument();

    AppContext mockContext = createMockContext();
    ViewportModel mockView = createMockViewport(document, 4);
    StubMatchManager mockMatchManager = new StubMatchManager(document);

    // Setup Callback
    SearchProgressListener callback = EasyMock.createMock(SearchProgressListener.class);
    callback.onSearchBegin();
    callback.onSearchDone();
    callback.onSearchBegin();
    callback.onSearchDone();
    callback.onSearchBegin();
    callback.onSearchDone();
    callback.onSearchBegin();
    callback.onSearchDone();
    replay(callback);

    // None of these get called during this test
    Renderer mockRenderer = EasyMock.createNiceMock(Renderer.class);
    replay(mockRenderer);

    SearchModel model = SearchModel.createWithManagerAndScheduler(mockContext,
        document,
        mockRenderer,
        mockView,
        mockMatchManager,
        new StubIncrementalScheduler(10, 1000),
        createMockSelectionModel(document));
    model.setQuery("when", callback);
    assertEquals(1, mockMatchManager.getTotalMatches());

    model.setQuery("When", callback);
    assertEquals(1, mockMatchManager.getTotalMatches());

    model.setQuery("Doug", callback);
    assertEquals(8, mockMatchManager.getTotalMatches());

    model.setQuery("tiger", callback);
    assertEquals(3, mockMatchManager.getTotalMatches());

    model.setQuery("", callback);
    assertEquals(0, mockMatchManager.getTotalMatches());

    // verify
    verify(callback, mockRenderer, mockView, mockContext);
  }

  public void testCallbackCalledWhenAllLinesFitInViewport() {
    Document document = createDocument();

    AppContext mockContext = createMockContext();
    ViewportModel mockView =
        createMockViewport(document, SearchTestsUtil.DOCUMENT_LINES.size() - 1);
    StubMatchManager mockMatchManager = new StubMatchManager(document);

    // Setup Callback
    SearchProgressListener callback = EasyMock.createMock(SearchProgressListener.class);
    callback.onSearchBegin();
    callback.onSearchDone();
    callback.onSearchBegin();
    callback.onSearchDone();
    replay(callback);

    // None of these get called during this test
    Renderer mockRenderer = EasyMock.createNiceMock(Renderer.class);
    replay(mockRenderer);

    SearchModel model = SearchModel.createWithManagerAndScheduler(mockContext,
        document,
        mockRenderer,
        mockView,
        mockMatchManager,
        new StubIncrementalScheduler(10, 1000),
        createMockSelectionModel(document));
    model.setQuery("when", callback);
    model.setQuery("Doug", callback);

    // verify callback is called
    verify(callback, mockRenderer, mockView, mockContext);
  }

  public void testMatchManagerIsCalledCorrectly() {
    Document document = createDocument();

    AppContext mockContext = createMockContext();
    ViewportModel mockView = createMockViewport(document, 4);

    // Let's test the behavior towards match manager this time
    SearchMatchManager mockMatchManager = EasyMock.createMock(SearchMatchManager.class);
    mockMatchManager.clearMatches();
    expectLastCall().times(2);
    expect(mockMatchManager.getTotalMatches()).andReturn(8).times(2);
    mockMatchManager.setSearchPattern(anyObject(RegExp.class));

    // Mocking the behavior of addMatches is a bit more difficult...
    LineInfo lineSixInfo = gotoLineInfo(document, 6);
    LineInfo lineEightInfo = gotoLineInfo(document, 8);
    mockMatchManager.addMatches(lineSixInfo, 1);
    mockMatchManager.addMatches(lineEightInfo, 7);
    mockMatchManager.addMatches(not(or(eq(lineSixInfo), eq(lineEightInfo))), eq(0));
    expectLastCall().times(10);
    replay(mockMatchManager);

    // None of these get called during this test
    Renderer mockRenderer = EasyMock.createNiceMock(Renderer.class);
    replay(mockRenderer);

    SearchModel model = SearchModel.createWithManagerAndScheduler(mockContext,
        document,
        mockRenderer,
        mockView,
        mockMatchManager,
        new StubIncrementalScheduler(10, 1000),
        createMockSelectionModel(document));
    model.setQuery("Doug");

    // verify callback is called
    verify(mockRenderer, mockView, mockContext);
  }
}
