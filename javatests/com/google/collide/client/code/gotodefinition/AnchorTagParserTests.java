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

package com.google.collide.client.code.gotodefinition;

import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.testing.StubIncrementalScheduler;
import com.google.collide.client.testutil.CodeMirrorTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;


public class AnchorTagParserTests extends CodeMirrorTestCase {

  private static final String SOURCE = ""
      + "<html>\n"
      + "  <body>\n"
      + "    <a></a>\n"
      + "    <a \n"
      + "       name=\"aName1\"/>\n"  // name attribute with quotes
      + "    <a name=aName2/>\n"  // name attribute without quotes
      + "    <a id=\"aId1\"/>\n"  // id attribute
      + "    <a id=\"\"/>\n"  // empty id
      + "    <div id=\"aId1\"/>\n"  // not A tag
      + "  </body>\n"
      + "</html>\n";

  @Override
  public String getModuleName() {
    return "com.google.collide.client.code.gotodefinition.GoToDefinitionTestModule";
  }

  public void testCollectedAnchors() {
    PathUtil filePath = new PathUtil("index.html");
    Document document = Document.createFromString(SOURCE);
    DocumentParser parser = DocumentParser.create(
        document, CodeMirror2.getParser(filePath), new StubIncrementalScheduler(50, 50));
    AnchorTagParser anchorParser = new AnchorTagParser(parser);
    parser.begin();
    JsonArray<AnchorTagParser.AnchorTag> anchorTags = anchorParser.getAnchorTags();
    assertEquals(3, anchorTags.size());
    assertAnchorTag(anchorTags.get(0), "aName1", 4, 13);
    assertAnchorTag(anchorTags.get(1), "aName2", 5, 12);
    assertAnchorTag(anchorTags.get(2), "aId1", 6, 11);
  }

  private void assertAnchorTag(AnchorTagParser.AnchorTag a, String name, int lineNumber,
      int column) {
    assertEquals(name, a.getName());
    assertEquals(lineNumber, a.getLineNumber());
    assertEquals(column, a.getColumn());
  }
}
