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

package com.google.collide.client.autoindenter;

import static com.google.collide.client.code.autocomplete.TestUtils.createDocumentParser;

import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.testutil.CodeMirrorTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.shared.document.Document;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * Tests for {@link Autoindenter} for python files.
 */
public class PyAutoindenterTest extends CodeMirrorTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.autoindenter.TestModule";
  }

  public void testIndentClass() {
    String text = "class Foo:";
    String expected = "class Foo:\n  ";
    checkAutoindenter(text, 0, 10, 0, 10, AutoindenterTest.TRIGGER_ENTER, expected, true);
  }

  public void testIndentMethod() {
    String text = "class Foo:\n  def bar:";
    String expected = "class Foo:\n  def bar:\n    ";
    checkAutoindenter(text, 1, 10, 1, 10, AutoindenterTest.TRIGGER_ENTER, expected, true);
  }

  public void testNoIndentEmptyLine() {
    String text = "class Foo:\n  def bar:\n";
    String expected = "class Foo:\n  def bar:\n\n";
    checkAutoindenter(text, 2, 0, 2, 0, AutoindenterTest.TRIGGER_ENTER, expected, false);
  }

  public void testNoIndentWhitespaceLine() {
    String text = "class Foo:\n  def bar:\n  ";
    String expected = "class Foo:\n  def bar:\n  \n";
    checkAutoindenter(text, 2, 2, 2, 2, AutoindenterTest.TRIGGER_ENTER, expected, false);
  }

  private static void checkAutoindenter(String text, int line1, int column1, int line2, int column2,
      final SignalEvent trigger, String expected, boolean allowScheduling) {
    PathUtil path = new PathUtil("test.py");
    TestUtils.MockIncrementalScheduler parseScheduler = new TestUtils.MockIncrementalScheduler();
    Document document = Document.createFromString(text);
    DocumentParser documentParser = createDocumentParser(path, true, parseScheduler, document);
    Editor editor = Editor.create(new MockAppContext());
    editor.setDocument(document);

    documentParser.begin();
    assertEquals(1, parseScheduler.requests.size());
    parseScheduler.requests.pop().run(300);

    AutoindenterTest.checkAutoindenter(line1, column1, line2, column2, trigger, expected,
        allowScheduling, documentParser, document, editor);
  }
}
