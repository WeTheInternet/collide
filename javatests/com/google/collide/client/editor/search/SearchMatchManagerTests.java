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

import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Buffer.MouseDragListener;
import com.google.collide.client.editor.search.SearchModel.MatchCountListener;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.testing.StubIncrementalScheduler;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.util.ListenerManager;
import com.google.gwt.regexp.shared.RegExp;

import junit.framework.TestCase;

import org.easymock.EasyMock;


public class SearchMatchManagerTests extends TestCase {
  
  private Document document;
  private SelectionModel model;

  @Override
  public void setUp() {
    document = createDocument();
    createSelectionModel(document);
  }
  
  private void createSelectionModel(Document document) {
    Buffer buffer = EasyMock.createNiceMock(Buffer.class);
    ListenerManager<MouseDragListener> listener = ListenerManager.create();
    expect(buffer.getMouseDragListenerRegistrar()).andReturn(listener).anyTimes();
    replay(buffer);
    model = SelectionModel.create(document, buffer);
    model.setSelection(document.getFirstLineInfo(), 0, document.getFirstLineInfo(), 0);
  }

  public void testTotalMatchesChangedListener() {
    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("doug"));

    MatchCountListener callback = EasyMock.createMock(MatchCountListener.class);
    callback.onMatchCountChanged(10);
    callback.onMatchCountChanged(15);
    callback.onMatchCountChanged(0);
    replay(callback);
    
    manager.getMatchCountChangedListenerRegistrar().add(callback);
    
    LineInfo line = document.getFirstLineInfo();
    manager.addMatches(line, 10);
    manager.addMatches(line, 5);
    manager.clearMatches();
  }

  public void testAddGetAndClearMatches() {
    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("testing"));

    LineInfo line = document.getFirstLineInfo();
    manager.addMatches(line, 5);

    line.moveToNext();
    manager.addMatches(line, 5);

    line.moveToNext();
    manager.addMatches(line, 5);

    line.moveToNext();
    manager.addMatches(line, 5);

    assertEquals(20, manager.getTotalMatches());

    manager.clearMatches();
    assertEquals(0, manager.getTotalMatches());

    manager.addMatches(line, 10);
    manager.addMatches(line, 10);

    assertEquals(20, manager.getTotalMatches());
  }

  public void testSelectMatchOnAddMatches() {
    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("do"));

    // Should end up only selecting the first match in line 0
    LineInfo line = document.getFirstLineInfo();
    manager.addMatches(line, 3);
    assertSelection(document.getFirstLineInfo(), 5, 7);

    line.moveToNext();
    manager.addMatches(line, 2);
    assertSelection(document.getFirstLineInfo(), 5, 7);
  }

  public void testFindNextMatchOnLine() {
    LineInfo lineEight = SearchTestsUtil.gotoLineInfo(document, 8);

    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("doug", "gi"));

    // Select the first match then move forward two
    manager.addMatches(lineEight, 7);
    manager.selectNextMatch();
    assertSelection(lineEight, 5, 9);
    manager.selectNextMatch();
    assertSelection(lineEight, 13, 17);
  }

  public void testFindPreviousMatchOnLine() {
    LineInfo lineEight = SearchTestsUtil.gotoLineInfo(document, 8);

    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("doug", "gi"));

    // Select the first match then move forward two
    manager.addMatches(lineEight, 7);
    manager.selectNextMatch();
    manager.selectNextMatch();

    // Go back a few matches
    manager.selectPreviousMatch();
    assertSelection(lineEight, 5, 9);
    manager.selectPreviousMatch();
    assertSelection(lineEight, 0, 4);
  }

  public void testFindPreviousMatchOnPreviousLine() {
    LineInfo lineSix = SearchTestsUtil.gotoLineInfo(document, 6);
    LineInfo lineEight = SearchTestsUtil.gotoLineInfo(document, 8);

    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("doug", "gi"));

    manager.addMatches(lineEight, 7);
    manager.addMatches(lineSix, 1);

    // we should be on the very first match, go back and find the prev match
    manager.selectPreviousMatch();
    assertSelection(lineSix, 41, 45);
    manager.selectPreviousMatch();
    assertSelection(lineEight, 35, 39);
  }

  public void testFindNextMatchOnNextLine() {
    LineInfo lineSix = SearchTestsUtil.gotoLineInfo(document, 6);
    LineInfo lineEight = SearchTestsUtil.gotoLineInfo(document, 8);

    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("doug", "gi"));

    manager.addMatches(lineSix, 1);
    manager.addMatches(lineEight, 7);

    // There is one match on line six so this will move us to line eight
    manager.selectNextMatch();
    assertSelection(lineEight, 0, 4);
    // Now let's iterate through matches and wrap around
    for (int i = 0; i < 6; i++) {
      manager.selectNextMatch();
    }
    manager.selectNextMatch();
    assertSelection(lineSix, 41, 45);
  }

  public void testFindMatchAfterAndBeforePosition() {
    LineInfo lineSix = SearchTestsUtil.gotoLineInfo(document, 6);
    LineInfo lineEight = SearchTestsUtil.gotoLineInfo(document, 8);

    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("doug", "gi"));

    manager.addMatches(lineSix, 1);
    manager.addMatches(lineEight, 7);

    manager.selectNextMatchFromPosition(lineEight, 20);
    assertSelection(lineEight, 23, 27);
    manager.selectPreviousMatchFromPosition(lineEight, 10);
    assertSelection(lineEight, 5, 9);
  }
  
  public void testWrapFindNextWhenMatchesOnSameLine() {
    LineInfo lineEleven = SearchTestsUtil.gotoLineInfo(document, 11);

    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("Awesome", "gi"));
    
    manager.addMatches(lineEleven, 1);
    assertSelection(lineEleven, 0, 7);
    manager.selectNextMatch();
    assertSelection(lineEleven, 8, 15);
    manager.selectNextMatch();
    assertSelection(lineEleven, 0, 7);
  }

  public void testWrapFindPreviousWhenMatchesOnSameLine() {
    LineInfo lineEleven = SearchTestsUtil.gotoLineInfo(document, 11);

    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("Awesome", "gi"));

    manager.addMatches(lineEleven, 1);
    assertSelection(lineEleven, 0, 7);
    manager.selectPreviousMatch();
    assertSelection(lineEleven, 8, 15);
  }

  public void testReplaceMatch() {
    LineInfo lineThree = SearchTestsUtil.gotoLineInfo(document, 3);
    LineInfo lineSix = SearchTestsUtil.gotoLineInfo(document, 6);
    LineInfo lineEight = SearchTestsUtil.gotoLineInfo(document, 8);

    SearchMatchManager manager = createMatchManager(document, model);
    manager.setSearchPattern(RegExp.compile("doug", "gi"));

    // NOTE! Since this a not an editor mutator, selection won't be replaced
    // so you will get newtextoldtext when calling replace.
    manager.addMatches(lineSix, 1);
    assertTrue(manager.replaceMatch("boug"));
    assertEquals("boug", document.getText(lineSix.line(), 41, 4));
 
    document.deleteText(lineSix.line(), 45, 4);
    assertTrue(manager.replaceMatch("soug"));
    assertEquals("soug", document.getText(lineEight.line(), 0, 4));

    model.setSelection(lineThree, 0, lineThree, 0);
    manager.setSearchPattern(RegExp.compile("catwoman"));
    manager.addMatches(lineThree, 1);
    assertTrue(manager.replaceMatch("dogwoman"));
    assertEquals("dogwoman", document.getText(lineThree.line(), 22, 8));
  }

  public void testReplaceAll() {
    SearchTask task = new SearchTask(
        document, createMockViewport(document, 3), new StubIncrementalScheduler(50, 50));

    SearchMatchManager manager = new SearchMatchManager(document, model, document, task);
    manager.setSearchPattern(RegExp.compile("doug", "gi"));
    
    LineInfo lineSix = SearchTestsUtil.gotoLineInfo(document, 6);
    manager.addMatches(lineSix, 1);
    manager.replaceAllMatches("boug");
    
    assertNull(manager.selectNextMatch());
  }
  
  public void testReplaceAllOnlyOneMatch() {
    document = Document.createFromString("foo");
    createSelectionModel(document);

    SearchTask task =
        new SearchTask(document, createMockViewport(document, 1), new StubIncrementalScheduler(
            50, 50));

    SearchMatchManager manager = new SearchMatchManager(document, model, document, task);
    manager.setSearchPattern(RegExp.compile("foo", "gi"));

    LineInfo lineOne = document.getFirstLineInfo();
    manager.addMatches(lineOne, 1);
    manager.replaceAllMatches("notit");

    assertNull(manager.selectNextMatch());
  }

  public void testReplaceRecursive() {
    document = Document.createFromString("foo");
    createSelectionModel(document);

    SearchTask task =
        new SearchTask(document, createMockViewport(document, 1), new StubIncrementalScheduler(
            50, 50));

    SearchMatchManager manager = new SearchMatchManager(document, model, document, task);
    manager.setSearchPattern(RegExp.compile("foo", "gi"));

    LineInfo lineOne = document.getFirstLineInfo();
    manager.addMatches(lineOne, 1);
    manager.replaceAllMatches("foofoo");

    assertNotNull(manager.selectNextMatch());
  }

  void assertSelection(LineInfo line, int start, int end) {
    Position[] selection = model.getSelectionRange(false);
    assertEquals(line, selection[0].getLineInfo());
    assertEquals(line, selection[1].getLineInfo());
    assertEquals(start, selection[0].getColumn());
    assertEquals(end, selection[1].getColumn());
  }

  /**
   * Covers the 99% case when creating, only replaceAll relies on searchTask.
   */
  static SearchMatchManager createMatchManager(Document document, SelectionModel selection) {
    return new SearchMatchManager(
        document, selection, document, EasyMock.createNiceMock(SearchTask.class));
  }
}
