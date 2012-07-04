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

package com.google.collide.client.search;

import com.google.collide.client.testing.PlaceGwtTestCase;

/**
 * Tests for the search.
 */
public class SearchTest extends PlaceGwtTestCase {

  @Override
  public String getModuleName() {
    return SearchTestUtils.BUILD_MODULE_NAME;
  }
  
  @Override
  public void gwtTearDown() throws Exception {
    super.gwtTearDown();
  }

  public void testGotoSimpleSearch() {

    // Temporarily breaking local text searching in order to fix dependency
    // inversion.
    // TODO: Make this work again and restore test case.

    /*
     *SearchImpl searchReq = SearchImpl.make(); searchReq.setWorkspaceId(WS_ID);
     * searchReq.setQuery("Foo"); searchReq.setPage(1); SearchResponseImpl
     * searchResp = MockSearchResponseImpl.make(); searchResp.setPage(1);
     * searchResp.setPageCount(10); searchResp.setResultCount(195);
     * JsoArray<SearchResult> results = JsoArray.create();
     * searchResp.setResults(results); for (int i = 0; i < 20; i++) {
     * SearchResultImpl item = SearchResultImpl.make(); JsonArray<Snippet>
     * snippets = JsoArray.create(); final SnippetImpl snippet =
     * SnippetImpl.make(); switch (i % 3) { case 0: break; // no snippet at all
     * case 1: snippet.setSnippetText("this is a one-line snippet text");
     * snippet.setLineNumber(21); snippets.add(snippet); break; case 2:
     * snippet.setSnippetText("this is a two-line snippet");
     * snippet.setLineNumber(1); snippets.add(snippet); SnippetImpl snippet2 =
     * SnippetImpl.make();
     * snippet2.setSnippetText("text, separated by a single newline.");
     * snippet2.setLineNumber(17); snippets.add(snippet2); }
     * item.setSnippets(snippets); item.setTitle("/a/path/file" + i); if (i % 2
     * == 0) { item.setUrl("http://somewhere.else.com/item" + i + ".html"); }
     * results.add(item); }
     *
     *
     * context.getMockFrontendApi().getSearchMockApi().expectAndReturn(searchReq,
     * searchResp);
     *
     * RootPlace.PLACE.dispatchHistory(HistoryUtils.parseHistoryString(
     * "/h/ws=(wsId=" + WS_ID + ",navEx=true)/code/search=(q=Foo,p=1)"));
     *
     * // and check the resulting display state, after a pause to let any dust
     * settle: this.delayTestFinish(500); Scheduler.get().scheduleDeferred(new
     * ScheduledCommand() {
     *
     * @Override public void execute() { SearchContainer.Css css =
     * context.getResources().searchContainerCss(); NodeList nodelist =
     * Browser.getDocument().getElementsByClassName(css.container());
     * assertEquals(1, nodelist.getLength()); Element elem = (Element)
     * nodelist.item(0); // TODO: this is returning "undefined," not a
     * boolean, which I // think is an elemental bug. //
     * assertFalse(elem.isHidden()); assertEquals(2,
     * elem.getChildren().getLength()); assertTrue(((Element)
     * elem.getLastChild()).getClassName().equals(css.pager()));
     *
     * // glance at the results: elem = elem.getChildren().item(0);
     * HTMLCollection children = elem.getChildren(); assertEquals(20,
     * elem.getChildNodes().getLength());
     * assertFalse(elem.getFirstChildElement()
     * .getClassName().equals(css.second())); assertTrue(((Element)
     * elem.getLastChild()).getClassName().equals(css.second())); // Tests case
     * 0 assertEquals(1,
     * elem.getFirstChildElement().getChildNodes().getLength()); AnchorElement
     * anchor = (AnchorElement)
     * elem.getFirstChildElement().getFirstChildElement();
     * assertEquals("http://somewhere.else.com/item0.html", anchor.getHref());
     * assertEquals("/a/path/file0", anchor.getTextContent()); // Tests case 1
     * assertEquals(2, elem.getChildren().item(1).getChildNodes().getLength());
     * anchor = (AnchorElement)
     * elem.getChildren().item(1).getFirstChildElement(); // Href is untestable.
     * assertEquals("/a/path/file1", anchor.getTextContent());
     * assertEquals("21: this is a one-line snippet text",
     * elem.getChildren().item(1).getChildNodes().item(1).getTextContent()); //
     * Tests case 2 assertEquals(3,
     * elem.getChildren().item(2).getChildNodes().getLength()); anchor =
     * (AnchorElement) elem.getChildren().item(2).getFirstChildElement();
     * assertEquals("http://somewhere.else.com/item2.html", anchor.getHref());
     * assertEquals("/a/path/file2", anchor.getTextContent());
     * assertEquals("1: this is a two-line snippet",
     * elem.getChildren().item(2).getChildNodes().item(1).getTextContent());
     * assertEquals("17: text, separated by a single newline.",
     * elem.getChildren().item(2).getChildNodes().item(2).getTextContent());
     *
     * // page 1, no previous: nodelist =
     * Browser.getDocument().getElementsByClassName(css.previous());
     * assertEquals(0, nodelist.getLength());
     *
     * elem = (Element)
     * Browser.getDocument().getElementsByClassName(css.pager()).item(0);
     * children = elem.getChildren(); // we seem to get empty elements (Text?)
     * between the spans, which // explains the even-only numbering until we get
     * to the end.
     * assertTrue(children.item(0).getClassName().equals(css.thispage()));
     * assertEquals("1", children.item(0).getTextContent());
     * assertTrue(children.item(2).getClassName().equals(css.otherpage()));
     * assertEquals("2", children.item(2).getTextContent());
     * assertTrue(children.item(4).getClassName().equals(css.otherpage()));
     * assertEquals("3", children.item(4).getTextContent());
     * assertTrue(children.item(6).getClassName().equals(css.otherpage()));
     * assertEquals("4", children.item(6).getTextContent());
     * assertTrue(children.item(8).getClassName().equals(css.otherpage()));
     * assertEquals("5", children.item(8).getTextContent());
     * assertTrue(children.item(10).getClassName().equals(css.otherpage()));
     * assertEquals("6", children.item(10).getTextContent());
     * assertTrue(children.item(11).getClassName().equals(css.thispage()));
     * assertEquals("...", children.item(11).getTextContent());
     * assertTrue(children.item(12).getClassName().equals(css.next()));
     * assertEquals(13, children.getLength());
     *
     * finishTest(); } });
     */
  }
}
