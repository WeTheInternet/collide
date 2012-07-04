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

package com.google.collide.client.code.autocomplete.codemirror;

import com.google.collide.client.code.autocomplete.MockAutocompleterEnvironment;
import com.google.collide.client.code.autocomplete.codegraph.py.PyCodeScope;
import com.google.collide.client.code.autocomplete.codegraph.py.PyIndexUpdater;
import com.google.collide.client.code.autocomplete.integration.TaggableLineUtil;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.testutil.CodeMirrorTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Line;

import javax.annotation.Nonnull;

/**
 * Test for {@link PyIndexUpdater}.
 */
public class PyIndexUpdaterTest extends CodeMirrorTestCase {
  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testScopes() {
    MockAutocompleterEnvironment helper = new MockAutocompleterEnvironment();
    String text = ""
        + "# Comment\n"
        + "class Foo:\n"
        + "  \"Foo is very clever and open-minded\"\n"
        + "  def goo(self, dwarf):\n"
        + "    whatever = [\n"
        + "\"whenever\"\n"
        + "               ]\n"
        + "    # Fais ce que dois, advienne que pourra\n"
        + "\n"
        + "  def roo(gnome):\n"
        + "    self.this = def class\n"
        + "    # La culture, c'est ce qui reste quand on a tout oublié.\n"
        + "class Bar:\n"
        // Also test that different indention scheme works well.
        + " \"Bar is a unit of pressure, roughly equal to the atmospheric pressure on Earth\"\n"
        + " def far(self):\n"
        + "  The kingdom of FAR FAR Away, Donkey? That's where we're going! FAR! FAR!... away.\n";
    helper.setup(new PathUtil("foo.py"), text, 0, 0, true);

    final PyIndexUpdater analyzer = new PyIndexUpdater();

    helper.parser.getListenerRegistrar().add(new DocumentParser.Listener() {
      private boolean asyncParsing;

      @Override
      public void onIterationStart(int lineNumber) {
        asyncParsing = true;
        analyzer.onBeforeParse();
      }

      @Override
      public void onIterationFinish() {
        asyncParsing = false;
        analyzer.onAfterParse();
      }

      @Override
      public void onDocumentLineParsed(
          Line line, int lineNumber, @Nonnull JsonArray<Token> tokens) {
        if (asyncParsing) {
          TaggableLine previousLine = TaggableLineUtil.getPreviousLine(line);
          analyzer.onParseLine(previousLine, line, tokens);
        }
      }
    });

    helper.parser.begin();
    helper.parseScheduler.requests.get(0).run(20);

    //# Comment
    Line line = helper.editor.getDocument().getFirstLine();
    PyCodeScope scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertNull(scope);

    //class Foo:
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertNotNull(scope);
    assertEquals(PyCodeScope.Type.CLASS, scope.getType());
    assertEquals("Foo", PyCodeScope.buildPrefix(scope).join("#"));
    PyCodeScope prevScope = scope;

    //  "Foo is very clever and open-minded"
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    //  def goo(self, dwarf):
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertNotNull(scope);
    assertEquals(PyCodeScope.Type.DEF, scope.getType());
    assertEquals("Foo#goo", PyCodeScope.buildPrefix(scope).join("#"));
    prevScope = scope;

    //    whatever = [
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    //"whenever"
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    //               ]
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    //    # Fais ce que dois, advienne que pourra
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    //
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    //  def roo(gnome):
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertNotNull(scope);
    assertEquals(PyCodeScope.Type.DEF, scope.getType());
    assertEquals("Foo#roo", PyCodeScope.buildPrefix(scope).join("#"));
    prevScope = scope;

    //    self.this = def class
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    //    # La culture, c'est ce qui reste quand on a tout oublié.
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    //class Bar:
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertNotNull(scope);
    assertEquals(PyCodeScope.Type.CLASS, scope.getType());
    assertEquals("Bar", PyCodeScope.buildPrefix(scope).join("#"));
    prevScope = scope;

    // "Bar is a unit of pressure, roughly equal to the atmospheric pressure on Earth"
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);

    // def far(self):
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertNotNull(scope);
    assertEquals(PyCodeScope.Type.DEF, scope.getType());
    assertEquals("Bar#far", PyCodeScope.buildPrefix(scope).join("#"));
    prevScope = scope;

    //  The kingdom of FAR FAR Away, Donkey? That's where we're going! FAR! FAR!... away.
    line = line.getNextLine();
    scope = line.getTag(PyIndexUpdater.TAG_SCOPE);
    assertTrue(scope == prevScope);
  }
}
