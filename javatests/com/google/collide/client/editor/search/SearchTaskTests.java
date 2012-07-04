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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.search.SearchTask.SearchDirection;
import com.google.collide.client.editor.search.SearchTask.SearchTaskExecutor;
import com.google.collide.client.testing.StubIncrementalScheduler;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests for the revamped Search Task.
 */
public class SearchTaskTests extends TestCase {

  private Document document;
  private StubIncrementalScheduler scheduler;

  @Override
  public void setUp() {
    document = createDocument();
    scheduler = new StubIncrementalScheduler(10, 1000);
  }

  public void testDocumentFitsInViewport() {
    SearchTask task = new SearchTask(document, createMockViewport(document, 12), scheduler);

    SearchTaskExecutor executor = EasyMock.createMock(SearchTaskExecutor.class);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(true)))
        .andReturn(true).times(12);
    replay(executor);

    task.searchDocument(executor, null);
    verify(executor);
  }

  public void testSchedulerRunsThroughDocument() {
    SearchTask task = new SearchTask(document, createMockViewport(document, 5), scheduler);

    SearchTaskExecutor executor = EasyMock.createMock(SearchTaskExecutor.class);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(true)))
        .andReturn(true).times(5);
    expect(executor.onSearchLine(
        EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(false)))
        .andReturn(true).times(7);
    replay(executor);

    task.searchDocument(executor, null);
    verify(executor);
  }

  public void testDirectionDownWorks() {
    ViewportModel viewport = createMockViewport(document, 5);
    SearchTask task = new SearchTask(document, viewport, scheduler);

    SearchTaskExecutor executor = EasyMock.createMock(SearchTaskExecutor.class);
    expect(executor.onSearchLine(viewport.getTopLine(), 0, true)).andReturn(true);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(true)))
        .andReturn(true).times(3);
    expect(executor.onSearchLine(viewport.getBottomLine(), 4, true)).andReturn(true);

    expect(executor.onSearchLine(
        EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(false)))
        .andReturn(true).times(7);
    replay(executor);

    task.searchDocument(executor, null, SearchDirection.DOWN);
    verify(executor);
  }

  public void testDirectionUpWorks() {
    ViewportModel viewport = createMockViewport(document, 5);
    SearchTask task = new SearchTask(document, viewport, scheduler);

    SearchTaskExecutor executor = EasyMock.createMock(SearchTaskExecutor.class);
    expect(executor.onSearchLine(viewport.getBottomLine(), 4, true)).andReturn(true);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(true)))
        .andReturn(true).times(3);
    expect(executor.onSearchLine(viewport.getTopLine(), 0, true)).andReturn(true);

    expect(executor.onSearchLine(document.getLastLine(), 11, false)).andReturn(true);
    expect(executor.onSearchLine(
        EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(false)))
        .andReturn(true).times(6);
    replay(executor);

    task.searchDocument(executor, null, SearchDirection.UP);
    verify(executor);
  }

  public void testStartingAtLineWorks() {
    ViewportModel viewport = createMockViewport(document, 5);
    SearchTask task = new SearchTask(document, viewport, scheduler);

    LineInfo lineInfo = viewport.getTopLineInfo();
    lineInfo.moveToNext();

    SearchTaskExecutor executor = EasyMock.createMock(SearchTaskExecutor.class);
    expect(executor.onSearchLine(lineInfo.line(), 1, true)).andReturn(true);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(true)))
        .andReturn(true).times(2);
    expect(executor.onSearchLine(viewport.getBottomLine(), 4, true)).andReturn(true);

    expect(executor.onSearchLine(
        EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(false)))
        .andReturn(true).times(7);
    expect(executor.onSearchLine(viewport.getTopLine(), 0, false)).andReturn(true);
    replay(executor);

    task.searchDocumentStartingAtLine(executor, null, SearchDirection.DOWN, lineInfo);
    verify(executor);
  }

  public void testNoWrapWorks() {
    ViewportModel viewport = createMockViewport(document, 5);
    SearchTask task = new SearchTask(document, viewport, scheduler);

    SearchTaskExecutor executor = EasyMock.createMock(SearchTaskExecutor.class);
    expect(executor.onSearchLine(viewport.getBottomLine(), 4, true)).andReturn(true);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(true)))
        .andReturn(true).times(3);
    expect(executor.onSearchLine(viewport.getTopLine(), 0, true)).andReturn(true);
    replay(executor);

    task.setShouldWrapDocument(false);
    task.searchDocument(executor, null, SearchDirection.UP);
    verify(executor);
  }

  public void testHaltViewportSearch() {
    ViewportModel viewport = createMockViewport(document, 5);
    SearchTask task = new SearchTask(document, viewport, scheduler);
    SearchTaskExecutor executor = EasyMock.createMock(SearchTaskExecutor.class);
    expect(executor.onSearchLine(viewport.getTopLine(), 0, true)).andReturn(true);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(true)))
        .andReturn(true).times(3);
    expect(executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.eq(4), EasyMock.eq(true)))
        .andReturn(false);
    replay(executor);

    task.searchDocument(executor, null);
    verify(executor);
  }

  public void testHaltSchedulerSearch() {
    ViewportModel viewport = createMockViewport(document, 5);
    SearchTask task = new SearchTask(document, viewport, scheduler);
    SearchTaskExecutor executor = EasyMock.createMock(SearchTaskExecutor.class);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.anyInt(), EasyMock.eq(true)))
        .andReturn(true).times(5);
    expect(
        executor.onSearchLine(EasyMock.anyObject(Line.class), EasyMock.eq(5), EasyMock.eq(false)))
        .andReturn(false);
    replay(executor);

    task.searchDocument(executor, null);
    verify(executor);
  }
}
