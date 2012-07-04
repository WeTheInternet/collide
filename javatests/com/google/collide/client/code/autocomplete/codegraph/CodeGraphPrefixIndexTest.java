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

package com.google.collide.client.code.autocomplete.codegraph;

import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createCodeBlock;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createInheritanceAssociation;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createRootImportAssociation;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createTypeAssociation;

import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.ImportAssociation;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.shared.util.JsonCollections;

import org.junit.Ignore;

/**
 * Tests for code block => autocomplete proposal translator.
 */
public class CodeGraphPrefixIndexTest extends SynchronousTestCase {
  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testCaseInsensitiveSearch() {
    CodeBlock fileBar = createCodeBlock("1", "/bar.js", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock varBar = createCodeBlock(fileBar, "11", "Bar", CodeBlock.Type.FIELD, 0, 0, 0, 99);
    createCodeBlock(fileBar, "12", "doThis", CodeBlock.Type.FUNCTION, 1, 0, 10, 99);
    createCodeBlock(fileBar, "13", "doThat", CodeBlock.Type.FUNCTION, 11, 0, 20, 99);

    JsoStringMap<CodeBlock> files = JsoStringMap.<CodeBlock>create();
    files.put(fileBar.getName(), fileBar);
    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setCodeBlockMap(files);

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(codeGraph, SyntaxType.JS);
    assertEquals(JsonCollections.createStringSet("doThat", "doThis"),
        TestUtils.createNameSet(prefixIndex.search("dot")));
  }

  public void testTypeAssociations() {
    /*
     * /bar.js:
     *
     * Bar {
     *   doThis: function() {},
     *   doThat: function() {}
     * }
     */
    CodeBlock fileBar = createCodeBlock("1", "/bar.js", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock varBar = createCodeBlock(fileBar, "11", "Bar", CodeBlock.Type.FIELD, 0, 0, 0, 99);
    createCodeBlock(fileBar, "12", "Bar.doThis", CodeBlock.Type.FUNCTION, 1, 0, 10, 99);
    createCodeBlock(fileBar, "13", "Bar.doThat", CodeBlock.Type.FUNCTION, 11, 0, 20, 99);

    /*
     * /foo.js:
     *
     * // @type {Bar}
     * var Foo;
     */
    CodeBlock fileFoo = createCodeBlock("0", "/foo.js", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock varFoo = createCodeBlock("11", "Foo", CodeBlock.Type.FIELD, 0, 0, 0, 99);
    fileFoo.getChildren().add(varFoo);

    JsoStringMap<CodeBlock> files = JsoStringMap.<CodeBlock>create();
    files.put(fileBar.getName(), fileBar);
    files.put(fileFoo.getName(), fileFoo);
    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setCodeBlockMap(files);
    codeGraph.setTypeAssociations(JsoArray.from(
        createTypeAssociation(fileFoo, varFoo, fileBar, varBar)));

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(codeGraph, SyntaxType.JS);
    assertEquals(JsonCollections.createStringSet("Foo.doThat", "Foo.doThis"),
        TestUtils.createNameSet(prefixIndex.search("Foo.")));
  }

  @Ignore
  public void testTypeAssociationChain() {
    CodeBlock fileFoo = createCodeBlock("0", "/foo.js", CodeBlock.Type.FILE, 0, 0, 99, 0);
    createCodeBlock(fileFoo, "11", "Foo", CodeBlock.Type.FIELD, 0, 0, 99, 0);
    CodeBlock typeFoo = createCodeBlock(
        fileFoo, "12", "Foo.prototype", CodeBlock.Type.FIELD, 0, 0, 99, 0);
    createCodeBlock(fileFoo, "13", "Foo.prototype.doThis", CodeBlock.Type.FUNCTION, 11, 0, 20, 0);

    CodeBlock fileBar = createCodeBlock("1", "/bar.js", CodeBlock.Type.FILE, 0, 0, 10, 0);
    createCodeBlock(fileBar, "11", "Bar", CodeBlock.Type.FIELD, 0, 0, 1, 0);
    CodeBlock typeBar = createCodeBlock(
        fileBar, "12", "Bar.prototype", CodeBlock.Type.FIELD, 0, 4, 1, 0);
    CodeBlock fieldFoo = createCodeBlock(
        fileBar, "13", "Bar.prototype.foo", CodeBlock.Type.FIELD, 0, 14, 1, 0);

    CodeBlock varBaz = createCodeBlock(fileBar, "14", "baz", CodeBlock.Type.FIELD, 5, 0, 6, 0);

    JsoStringMap<CodeBlock> files = JsoStringMap.<CodeBlock>create();
    files.put(fileBar.getName(), fileBar);
    files.put(fileFoo.getName(), fileFoo);

    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setCodeBlockMap(files);
    codeGraph.setTypeAssociations(JsoArray.from(
        createTypeAssociation(fileBar, varBaz, fileBar, typeBar),
        createTypeAssociation(fileBar, fieldFoo, fileFoo, typeFoo)));

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(codeGraph, SyntaxType.JS);
    assertEquals(JsonCollections.createStringSet("baz.foo.doThis"),
        TestUtils.createNameSet(prefixIndex.search("baz.foo.")));

  }
  public void testMultipleLinkRepresentatives() {
    /*
     * /window.js:
     *
     * Window.prototype.doThis = function() {}
     */
    CodeBlock fileWindow = createCodeBlock("1", "/bar.js", CodeBlock.Type.FILE, 0, 0, 0, 99);
    createCodeBlock(fileWindow, "11", "Window", CodeBlock.Type.FIELD, 0, 0, 0, 99);
    CodeBlock typeWindow = createCodeBlock(fileWindow, "12", "Window.prototype",
        CodeBlock.Type.FIELD, 0, 0, 0, 99);
    createCodeBlock(fileWindow, "13", "Window.prototype.doThis",
        CodeBlock.Type.FUNCTION, 0, 0, 0, 99);

    /*
     * /jquery.js:
     *
     * Window.prototype.doThat = function() {}
     */
    CodeBlock fileJquery = createCodeBlock("0", "/jquery.js", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock varJqueryWindow = createCodeBlock(fileJquery, "11", "Window",
        CodeBlock.Type.FIELD, 0, 0, 0, 99);
    createCodeBlock(fileJquery, "12", "Window.prototype",
        CodeBlock.Type.FIELD, 0, 0, 0, 99);
    createCodeBlock(fileJquery, "13", "Window.prototype.doThat",
        CodeBlock.Type.FUNCTION, 0, 0, 0, 99);

    /*
     * /decl.js:
     *
     * // @type{Window}
     * var top;
     */
    CodeBlock fileDecl = createCodeBlock("2", "/decl.js", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock varWindow = createCodeBlock(fileDecl, "11", "top",
        CodeBlock.Type.FIELD, 0, 0, 0, 99);

    JsoStringMap<CodeBlock> files = JsoStringMap.<CodeBlock>create();
    files.put(fileWindow.getName(), fileWindow);
    files.put(fileJquery.getName(), fileJquery);
    files.put(fileDecl.getName(), fileDecl);

    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setCodeBlockMap(files);
    codeGraph.setTypeAssociations(JsoArray.from(
        createTypeAssociation(fileDecl, varWindow, fileWindow, typeWindow)));

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(codeGraph, SyntaxType.JS);
    assertEquals(JsonCollections.createStringSet("top.doThat", "top.doThis"),
        TestUtils.createNameSet(prefixIndex.search("top.")));
  }

  public void testInheritanceAssociations() {
    /*
     * /bar.js:
     *
     * function Bar() {};
     * Bar.prototype.doThis = function() {};
     * Bar.prototype.doThat = function() {};
     *
     * // @extends {Bar}
     * function Foo() {};
     * Foo.prototype.doThird = function() {};
     */
    CodeBlock fileBar = createCodeBlock("1", "/bar.js", CodeBlock.Type.FILE, 0, 0, 0, 99);
    createCodeBlock(fileBar, "11", "Bar", CodeBlock.Type.FUNCTION, 0, 0, 0, 99);
    CodeBlock prototypeBar = createCodeBlock(
        fileBar, "12", "Bar.prototype", CodeBlock.Type.FIELD, 0, 0, 0, 99);
    createCodeBlock(fileBar, "13", "Bar.prototype.doThis", CodeBlock.Type.FUNCTION, 1, 0, 10, 99);
    createCodeBlock(fileBar, "14", "Bar.prototype.doThat", CodeBlock.Type.FUNCTION, 11, 0, 20, 99);

    createCodeBlock(fileBar, "15", "Foo", CodeBlock.Type.FUNCTION, 100, 0, 199, 99);
    CodeBlock prototypeFoo = createCodeBlock(
        fileBar, "16", "Foo.prototype", CodeBlock.Type.FIELD, 100, 0, 199, 99);
    createCodeBlock(
        fileBar, "17", "Foo.prototype.doThird", CodeBlock.Type.FUNCTION, 101, 0, 120, 99);

    JsoStringMap<CodeBlock> files = JsoStringMap.<CodeBlock>create();
    files.put(fileBar.getName(), fileBar);
    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setCodeBlockMap(files);
    codeGraph.setInheritanceAssociations(JsoArray.from(
        createInheritanceAssociation(fileBar, prototypeFoo, fileBar, prototypeBar)));

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(codeGraph, SyntaxType.JS);
    assertEquals(JsonCollections.createStringSet(
        "Foo.prototype.doThird", "Foo.prototype.doThis", "Foo.prototype.doThat"),
        TestUtils.createNameSet(prefixIndex.search("Foo.prototype.")));
  }

  public void testRootImportAssociation() {
    /*
     * /lib.py:
     *
     * def foo:
     *   return 42;
     */
    CodeBlock fileLib = createCodeBlock("1", "/lib.py", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock funFoo = createCodeBlock(fileLib, "11", "foo", CodeBlock.Type.FUNCTION, 0, 0, 0, 99);

    /*
     * /api/ext/util.py:
     *
     * def bar:
     *   return None;
     */
    CodeBlock fileUtil = createCodeBlock("2", "/api/ext/util.py", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock funBar = createCodeBlock(fileUtil, "11", "bar", CodeBlock.Type.FUNCTION, 0, 0, 0, 99);

    /*
     * /main.py:
     *
     * import lib
     * from api.ext import util
     */
    CodeBlock fileMain = createCodeBlock("3", "/main.py", CodeBlock.Type.FILE, 0, 0, 0, 99);
    ImportAssociation importLib = createRootImportAssociation(fileMain, fileLib);
    ImportAssociation importUtil = createRootImportAssociation(fileMain, fileUtil);

    JsoStringMap<CodeBlock> files = JsoStringMap.<CodeBlock>create();
    files.put(fileLib.getId(), fileLib);
    files.put(fileMain.getId(), fileMain);
    files.put(fileUtil.getId(), fileUtil);

    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setCodeBlockMap(files);
    codeGraph.setImportAssociations(JsoArray.from(importLib, importUtil));

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(codeGraph, SyntaxType.PY);
    assertEquals(JsonCollections.createStringSet("lib.foo"),
        TestUtils.createNameSet(prefixIndex.search("lib.f")));
    assertEquals(JsonCollections.createStringSet("util.bar"),
        TestUtils.createNameSet(prefixIndex.search("util.")));

  }

  public void testNoGlobalNamespace() {
    /*
     * /file1.py:
     *
     * def foo1:
     *   return 42;
     */
    CodeBlock file1 = createCodeBlock("1", "/file1.py", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock funFoo1 = createCodeBlock(file1, "11", "foo1", CodeBlock.Type.FUNCTION, 0, 0, 0, 99);

    /*
     * /file2.py:
     *
     * def foo2:
     *   return 24;
     */
    CodeBlock file2 = createCodeBlock("2", "/file2.py", CodeBlock.Type.FILE, 0, 0, 0, 99);
    CodeBlock funFoo2 = createCodeBlock(file2, "21", "foo2", CodeBlock.Type.FUNCTION, 0, 0, 0, 99);

    JsoStringMap<CodeBlock> files = JsoStringMap.<CodeBlock>create();
    files.put(file1.getId(), file1);
    files.put(file2.getId(), file2);
    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setCodeBlockMap(files);

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(
        codeGraph, SyntaxType.PY, new PathUtil("/file1.py"));
    assertEquals(JsonCollections.createStringSet("foo1"),
        TestUtils.createNameSet(prefixIndex.search("f")));
  }

  public void testFilesWithSameName() {
    /*
     * /file1.py:
     *
     * def foo1:
     *   return 1;
     *
     * def foo3:
     *   return 3;
     */
    CodeBlock file1a = createCodeBlock("1", "/file1.py", CodeBlock.Type.FILE, 0, 0, 3, 0);
    CodeBlock funFoo1a = createCodeBlock(
        file1a, "11", "foo1", CodeBlock.Type.FIELD, 0, 0, 1, 0);
    CodeBlock funFoo3 = createCodeBlock(
        file1a, "11", "foo3", CodeBlock.Type.FIELD, 1, 0, 2, 0);

    /*
     * /file1.py:
     *
     * def foo1:
     *   return 1;
     *
     * def foo2:
     *   return 2;
     */
    CodeBlock file1b = createCodeBlock("2", "/file1.py", CodeBlock.Type.FILE, 0, 0, 3, 0);
    CodeBlock funFoo1b = createCodeBlock(
        file1b, "21", "foo1", CodeBlock.Type.FUNCTION, 0, 0, 0, 99);
    CodeBlock funFoo2 = createCodeBlock(
        file1b, "22", "foo2", CodeBlock.Type.FUNCTION, 1, 0, 1, 99);

    JsoStringMap<CodeBlock> files = JsoStringMap.<CodeBlock>create();
    files.put(file1a.getId(), file1a);
    files.put(file1b.getId(), file1b);

    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setCodeBlockMap(files);

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(
        codeGraph, SyntaxType.PY, new PathUtil("/file1.py"));
    assertEquals(JsonCollections.createStringSet("foo1", "foo2", "foo3"),
        TestUtils.createNameSet(prefixIndex.search("f")));
  }

  public void testPackages() {
    CodeBlock defaultPackage = createCodeBlock("p1", "", CodeBlock.Type.PACKAGE, 0, 0, 0, 0);
    CodeBlock pkgGoogle = createCodeBlock(
        defaultPackage, "p2", "google", CodeBlock.Type.PACKAGE, 0, 0, 0, 0);
    CodeBlock pkgAppengine = createCodeBlock(
        defaultPackage, "p3", "google.appengine", CodeBlock.Type.PACKAGE, 0, 0, 0, 0);
    CodeBlock pkgExt = createCodeBlock(
        defaultPackage, "p4", "google.ext", CodeBlock.Type.PACKAGE, 0, 0, 0, 0);

    CodeGraphImpl codeGraph = CodeGraphImpl.make();
    codeGraph.setDefaultPackage(defaultPackage);
    codeGraph.setCodeBlockMap(JsoStringMap.<CodeBlock>create());

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(codeGraph, SyntaxType.PY);
    assertEquals(JsonCollections.createStringSet("google"),
        TestUtils.createNameSet(prefixIndex.search("goo")));

    assertEquals(JsonCollections.createStringSet("google.appengine", "google.ext"),
        TestUtils.createNameSet(prefixIndex.search("google.")));

    assertEquals(JsonCollections.createStringSet("google.appengine"),
        TestUtils.createNameSet(prefixIndex.search("google.a")));

    assertEquals(JsonCollections.createStringSet("google.ext"),
        TestUtils.createNameSet(prefixIndex.search("google.e")));

  }
}
