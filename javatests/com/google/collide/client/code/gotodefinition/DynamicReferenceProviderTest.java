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
import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeNode;
import com.google.collide.client.workspace.MockOutgoingController;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.codemirror2.Token;
import com.google.collide.dto.DirInfo;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.gwt.regexp.shared.MatchResult;

import javax.annotation.Nullable;

/**
 * Tests for {@link AnchorTagParser}.
 *
 */
public class DynamicReferenceProviderTest extends CodeMirrorTestCase {

  private Document document;
  private DocumentParser parser;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  private DynamicReferenceProvider createDynamicReferenceProvider(String path, String source) {
    PathUtil filePath = new PathUtil(path);
    document = Document.createFromString(source);
    parser = DocumentParser.create(
        document, CodeMirror2.getParser(filePath), new StubIncrementalScheduler(50, 50));
    FileTreeNode root = FileTreeNode.transform(buildSimpleTree());
    FileTreeModel model = new FileTreeModel(new MockOutgoingController());
    model.replaceNode(PathUtil.WORKSPACE_ROOT, root, "1");
    return new DynamicReferenceProvider(path, new DeferringLineParser(parser), model, null);
  }

  public void testLocalAndAbsoluteFileReferences() {
    // Some PathUtil sanity tests.
    String contextPath = "/src/index.html";
    PathUtil contextDir = PathUtil.createExcludingLastN(new PathUtil(contextPath), 1);
    assertEquals("/src", contextDir.getPathString());
    PathUtil filePathInContextDir = PathUtil.concatenate(contextDir, new PathUtil("foo.js"));
    assertEquals("/src/foo.js", filePathInContextDir.getPathString());

    DynamicReferenceProvider provider = createDynamicReferenceProvider("/index.html", "");
    tryFindFileNode(provider, "/asdf.js", null);
    tryFindFileNode(provider, "asdf.js", null);
    tryFindFileNode(provider, "/foo.js", "/foo.js");
    tryFindFileNode(provider, "foo.js", "/foo.js");
    tryFindFileNode(provider, "/src/world.js", "/src/world.js");
    tryFindFileNode(provider, "src/world.js", "/src/world.js");

    provider = createDynamicReferenceProvider("/src/index.html", "");
    tryFindFileNode(provider, "/asdf.js", null);
    tryFindFileNode(provider, "asdf.js", null);
    tryFindFileNode(provider, "/foo.js", "/foo.js");
    tryFindFileNode(provider, "foo.js", null);
    tryFindFileNode(provider, "world.js", "/src/world.js");
    tryFindFileNode(provider, "/src/world.js", "/src/world.js");
    tryFindFileNode(provider, "src/world.js", null);
  }

  private void tryFindFileNode(DynamicReferenceProvider provider, String displayPath,
      @Nullable String expectedFileNodePath) {
    FileTreeNode fileNode = provider.findFileNode(displayPath);
    if (expectedFileNodePath == null) {
      assertNull(fileNode);
    } else {
      assertEquals(expectedFileNodePath, fileNode.getNodePath().getPathString());
    }
  }

  public void testUrlReference() {
    String url1 = "http://www.google.com/";
    String url2 = "http://www.ru/?q=1&p=2";
    String url3 = "https://somesafeurl.com";
    String beforeUrl1Text = "/* start ";
    String middleText = " text ";
    DynamicReferenceProvider dynamicReferenceProvider = createDynamicReferenceProvider("test.js", ""
        + beforeUrl1Text + url1 + middleText + url2 + ". */\n"
        + "var a = 5;\n"
        + "// " + url3 + ".\n");

    int url1StartColumn = beforeUrl1Text.length();
    tryDynamicUrlReference(dynamicReferenceProvider, 0, url1StartColumn - 1, -1, null);
    tryDynamicUrlReference(dynamicReferenceProvider, 0, url1StartColumn, url1StartColumn, url1);
    tryDynamicUrlReference(
        dynamicReferenceProvider, 0, url1StartColumn + url1.length() - 1, url1StartColumn, url1);
    tryDynamicUrlReference(dynamicReferenceProvider, 0, url1StartColumn + url1.length(), -1, null);

    int url2StartColumn = url1StartColumn + url1.length() + middleText.length();
    tryDynamicUrlReference(dynamicReferenceProvider, 0, url2StartColumn - 1, -1, null);
    tryDynamicUrlReference(dynamicReferenceProvider, 0, url2StartColumn, url2StartColumn, url2);
    tryDynamicUrlReference(
        dynamicReferenceProvider, 0, url2StartColumn + url2.length() - 1, url2StartColumn, url2);
    tryDynamicUrlReference(dynamicReferenceProvider, 0, url2StartColumn + url2.length(), -1, null);

    // Need to parse line 1 (second line), otherwise parseLineSync returns null for line 2.
    tryDynamicUrlReference(dynamicReferenceProvider, 1, 0, -1, null);

    int url3StartColumn = 3;
    tryDynamicUrlReference(dynamicReferenceProvider, 2, url3StartColumn - 1, -1, null);
    tryDynamicUrlReference(dynamicReferenceProvider, 2, url3StartColumn, url3StartColumn, url3);
    tryDynamicUrlReference(
        dynamicReferenceProvider, 2, url3StartColumn + url3.length() - 1, url3StartColumn, url3);
    tryDynamicUrlReference(dynamicReferenceProvider, 2, url3StartColumn + url3.length(), -1, null);
  }

  private void tryDynamicUrlReference(DynamicReferenceProvider provider, int lineNumber,
      int column, int referenceStartColumn, @Nullable String url) {
    LineInfo lineInfo = document.getLineFinder().findLine(lineNumber);
    assertNotNull(lineInfo);
    JsonArray<Token> tokens = parser.parseLineSync(lineInfo.line());
    if (tokens == null) {
      throw new RuntimeException(lineInfo.line().getText());
    }
    assertNotNull(tokens);
    NavigableReference.UrlReference reference =
        (NavigableReference.UrlReference) provider.getReferenceAt(lineInfo, column, tokens);

    if (url == null) {
      assertNull(reference);
    } else {
      assertNotNull(reference);
      assertEquals(lineNumber, reference.getLineNumber());
      assertEquals(referenceStartColumn, reference.getStartColumn());
      assertEquals(url, reference.getUrl());
    }
  }

  public void testLocalFileReference() {
    DynamicReferenceProvider dynamicReferenceProvider = createDynamicReferenceProvider("test.html",
        "<script src=\"foo.js\"></script>");

    tryDynamicFileReference(dynamicReferenceProvider, 0, 12, -1, null);
    tryDynamicFileReference(dynamicReferenceProvider, 0, 13, 13, "/foo.js");
    tryDynamicFileReference(dynamicReferenceProvider, 0, 18, 13, "/foo.js");
    tryDynamicFileReference(dynamicReferenceProvider, 0, 19, -1, null);

    dynamicReferenceProvider = createDynamicReferenceProvider("/src/test.html",
        "<script src=\"world.js\"></script>");

    tryDynamicFileReference(dynamicReferenceProvider, 0, 12, -1, null);
    tryDynamicFileReference(dynamicReferenceProvider, 0, 13, 13, "/src/world.js");
    tryDynamicFileReference(dynamicReferenceProvider, 0, 20, 13, "/src/world.js");
    tryDynamicFileReference(dynamicReferenceProvider, 0, 21, -1, null);
  }

  private void tryDynamicFileReference(DynamicReferenceProvider provider, int lineNumber,
      int column, int referenceStartColumn, @Nullable String filePath) {
    LineInfo lineInfo = document.getLineFinder().findLine(lineNumber);
    JsonArray<Token> tokens = parser.parseLineSync(lineInfo.line());
    NavigableReference.FileReference reference =
        (NavigableReference.FileReference) provider.getReferenceAt(lineInfo, column, tokens);
    if (filePath == null) {
      assertNull(reference);
    } else {
      assertNotNull(reference);
      assertEquals(lineNumber, reference.getLineNumber());
      assertEquals(referenceStartColumn, reference.getStartColumn());
      assertEquals(filePath, reference.getTargetFilePath());
    }
  }

  public void testUrlMatches() {
    tryMatchUrl("sdf http://www.google.com/ alsg", "http://www.google.com/");
    tryMatchUrl("http://www.google.com.", "http://www.google.com");
    tryMatchUrl("a www.google.com", null);
    tryMatchUrl("ftp://192.168.1.1:23/somepath/.", "ftp://192.168.1.1:23/somepath/");
    tryMatchUrl("adflkhttp://www.google.com/asdg", null);
    tryMatchUrl("http://www.ru?1=2&2=3 text", "http://www.ru?1=2&2=3");
    tryMatchUrl("text1.http://go/someplace#url=test.", "http://go/someplace#url=test");
    tryMatchUrl("<a href='ftp://myawesomeftp.com/'>link</a>", "ftp://myawesomeftp.com/");
    tryMatchUrl("<img src=\"http://somedomain.com/somepath/someimage.jpg\">",
        "http://somedomain.com/somepath/someimage.jpg");
    tryMatchUrl("many whitespaces    http://www.com/path/foo    ", "http://www.com/path/foo");
    tryMatchUrl("many whitespaces    http://www.com/path/img.png    text",
        "http://www.com/path/img.png");
    // THE FOLLOWING TEST CASE DOES NOT WORK YET! It takes the end of comment ("*/") as URL.
    // tryMatchUrl("/* Go to http://www.google.com/.*/", "http://www.google.com/");
  }

  private void tryMatchUrl(String text, @Nullable String url) {
    DynamicReferenceProvider.REGEXP_URL.setLastIndex(0);
    MatchResult matchResult = DynamicReferenceProvider.REGEXP_URL.exec(text);
    if (url == null) {
      assertNull(matchResult);
      return;
    }
    assertNotNull(matchResult);
    assertEquals(url, matchResult.getGroup(0));
  }

  /**
   * Stole from TreeWalkFileNameSearchImpltest
   */
  private final native DirInfo buildSimpleTree() /*-{
    return {
        // Root node is magic
        nodeType : @com.google.collide.dto.TreeNodeInfo::DIR_TYPE,
        id : "1",
        originId : "1",
        name : "root",
        files : [
            {
                nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                id : "5",
                originId : "5",
                name : "foo.js",
                rootId : "2",
                path : "/foo.js",
                size : "1234"
            }
        ],
        isComplete : true,
        subDirectories : [
            {
                nodeType : @com.google.collide.dto.TreeNodeInfo::DIR_TYPE,
                id : "2",
                originId : "2",
                name : "src",
                path : "/src",
                files : [
                    {
                        nodeType : @com.google.collide.dto.TreeNodeInfo::FILE_TYPE,
                        id : "7",
                        originId : "7",
                        name : "world.js",
                        rootId : "2",
                        path : "/src/world.js",
                        size : "1234"
                    }
                ],
                isComplete : true,
                subDirectories : []
            }
        ]
    };
  }-*/;
}
