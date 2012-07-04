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
import static org.easymock.EasyMock.verify;

import com.google.collide.client.Resources;
import com.google.collide.client.editor.renderer.LineRenderer.Target;
import com.google.collide.client.editor.search.SearchMatchRenderer.Css;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.RegExpUtils;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * TODO: when single matches can be highlighted separately implement
 * tests for this, the current stuff is very basic
 *
 */
public class SearchMatchRendererTests extends TestCase {
  private static final String MATCH_CSS = "match";

  private Resources createMockResource() {
    Resources mockResource = EasyMock.createMock(Resources.class);
    Css mockCss = EasyMock.createMock(Css.class);
    expect(mockCss.match()).andReturn(MATCH_CSS).anyTimes();
    expect(mockResource.searchMatchRendererCss()).andReturn(mockCss).anyTimes();
    
    replay(mockCss, mockResource);
    return mockResource;
  }
  
  /**
   * Creates a mock target expecting render events for lengths in lengthList
   *
   * @param startInMatch if true starts expecting MATCH_CSS
   * @param selectedMatchIndex will use selected_match_css for the item at this
   *        lengthList index if >= 0 and is inMatch at that index
   */
  private Target createMockTarget(ImmutableList<Integer> lengthList, boolean startInMatch) {
    Target mockTarget = EasyMock.createMock(Target.class);
    
    boolean inMatch = startInMatch;
    for (int i = 0; i < lengthList.size(); i++) {
      mockTarget.render(lengthList.get(i), inMatch ? MATCH_CSS : null);
      inMatch = !inMatch;
    }
    replay(mockTarget);
    return mockTarget;
  }

  public void testRegularMatchHighlight() {
    SearchMatchManager mockMatchManager = EasyMock.createNiceMock(SearchMatchManager.class);
    replay(mockMatchManager);
    
    SearchModel mockSearchModel = EasyMock.createMock(SearchModel.class);
    expect(mockSearchModel.getQuery()).andReturn("Doug").anyTimes();
    expect(mockSearchModel.getSearchPattern()).andReturn(
        RegExpUtils.createRegExpForWildcardPattern("Doug", "gi")).anyTimes();
    expect(mockSearchModel.getMatchManager()).andReturn(mockMatchManager).anyTimes();
    replay(mockSearchModel);
    
    Resources mockResources = createMockResource();
    SearchMatchRenderer renderer = new SearchMatchRenderer(mockResources, mockSearchModel);
    
    // Now ask it about each line in our document and check to see if its right
    Document doc = SearchTestsUtil.createDocument();
    LineInfo lineInfo = doc.getFirstLineInfo();
    for (int i = 0; i < 6; i++) {
      assertFalse(renderer.resetToBeginningOfLine(lineInfo.line(), lineInfo.number()));
      lineInfo.moveToNext();
    }
    
    // Check that this line is parsed correctly
    ImmutableList<Integer> lengthList = ImmutableList.of(41, 4);
    Target mockTarget = createMockTarget(lengthList, false);
    assertTrue(renderer.resetToBeginningOfLine(lineInfo.line(), lineInfo.number()));
    for (int i = 0; i < lengthList.size(); i++) {
      renderer.renderNextChunk(mockTarget);
    }
    lineInfo.moveToNext();
    verify(mockTarget);
    
    assertFalse(renderer.resetToBeginningOfLine(lineInfo.line(), lineInfo.number()));
    lineInfo.moveToNext();
    
    // The next fun line
    lengthList = ImmutableList.of(4,1,4,4,4,1,4,1,4,3,4,1,4);
    mockTarget = createMockTarget(lengthList, true);
    assertTrue(renderer.resetToBeginningOfLine(lineInfo.line(), lineInfo.number()));
    for (int i = 0; i < lengthList.size(); i++) {
      renderer.renderNextChunk(mockTarget);
    }
    lineInfo.moveToNext();
    verify(mockTarget);
    
    assertFalse(renderer.resetToBeginningOfLine(lineInfo.line(), lineInfo.number()));
    lineInfo.moveToNext();
    assertFalse(renderer.resetToBeginningOfLine(lineInfo.line(), lineInfo.number()));
  }
}
