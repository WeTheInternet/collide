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

import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.client.DtoClientImpls.CodeBlockImpl;
import com.google.collide.dto.client.DtoClientImpls.MockCodeBlockImpl;
import com.google.collide.json.client.JsoArray;

/**
 */
public class CodeFileTest extends SynchronousTestCase {

  private static void assertScopeBounds(
      int beginLine, int beginCol, int endLine, int endCol, Scope scope) {
    assertEquals(beginLine, scope.getBeginLineNumber());
    assertEquals(beginCol, scope.getBeginColumn());
    assertEquals(endLine, scope.getEndLineNumber());
    assertEquals(endCol, scope.getEndColumn());
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testLexicalScopeAndObjectScopeAreTheSame() {
    /*
     * Something like var foobar = { foo: function() {}, bar: function() {} }
     */
    CodeBlockImpl fnFoo = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("foo")
        .setChildren(JsoArray.<CodeBlock>create())
        .setStartLineNumber(1)
        .setStartColumn(2)
        .setEndLineNumber(1)
        .setEndColumn(19);
    CodeBlockImpl fnBar = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("bar")
        .setChildren(JsoArray.<CodeBlock>create())
        .setStartLineNumber(2)
        .setStartColumn(2)
        .setEndLineNumber(2)
        .setEndColumn(19);
    CodeBlockImpl varFoobar = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FIELD)
        .setName("foobar")
        .setChildren(JsoArray.<CodeBlock>from(fnFoo, fnBar))
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(3)
        .setEndColumn(0);
    CodeBlockImpl fileCodeBlock = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FILE)
        .setChildren(JsoArray.<CodeBlock>from(varFoobar))
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(3)
        .setEndColumn(0);

    CodeFile codeFile = new CodeFile(new PathUtil("/foobar.js"));
    codeFile.setRootCodeBlock(fileCodeBlock);
    assertEquals(1, codeFile.getRootScope().getSubscopes().size());
    assertEquals(2, codeFile.getRootScope().getSubscopes().get(0).getSubscopes().size());

    assertScopeBounds(0, 0, 3, 0, codeFile.getRootScope());
    assertScopeBounds(0, 0, 3, 0, codeFile.getRootScope().getSubscopes().get(0));
    assertScopeBounds(
        1, 2, 1, 19, codeFile.getRootScope().getSubscopes().get(0).getSubscopes().get(0));
    assertScopeBounds(
        2, 2, 2, 19, codeFile.getRootScope().getSubscopes().get(0).getSubscopes().get(1));
  }

  public void testLexicalScopeAndObjectScopeAreDifferent() {
    /*
     * Something like var foobar = { foo: function() {} }
     *
     * foobar.bar = function() { }
     */
    CodeBlockImpl fnFoo = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("foo")
        .setChildren(JsoArray.<CodeBlock>create())
        .setStartLineNumber(1)
        .setStartColumn(2)
        .setEndLineNumber(1)
        .setEndColumn(19);
    CodeBlockImpl fnBar = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FUNCTION)
        .setName("bar")
        .setChildren(JsoArray.<CodeBlock>create())
        .setStartLineNumber(4)
        .setStartColumn(24)
        .setEndLineNumber(5)
        .setEndColumn(0);
    CodeBlockImpl varFoobar = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FIELD)
        .setName("foobar")
        .setChildren(JsoArray.<CodeBlock>from(fnFoo, fnBar))
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(2)
        .setEndColumn(0);
    CodeBlockImpl fileCodeBlock = MockCodeBlockImpl
        .make()
        .setBlockType(CodeBlock.Type.VALUE_FILE)
        .setChildren(JsoArray.<CodeBlock>from(varFoobar))
        .setStartLineNumber(0)
        .setStartColumn(0)
        .setEndLineNumber(5)
        .setEndColumn(0);

    CodeFile codeFile = new CodeFile(new PathUtil("/foobar.js"));
    codeFile.setRootCodeBlock(fileCodeBlock);
    assertEquals(2, codeFile.getRootScope().getSubscopes().size());
    assertEquals(1, codeFile.getRootScope().getSubscopes().get(0).getSubscopes().size());

    assertScopeBounds(0, 0, 5, 0, codeFile.getRootScope());
    assertScopeBounds(0, 0, 2, 0, codeFile.getRootScope().getSubscopes().get(0));
    assertScopeBounds(4, 24, 5, 0, codeFile.getRootScope().getSubscopes().get(1));
    assertScopeBounds(
        1, 2, 1, 19, codeFile.getRootScope().getSubscopes().get(0).getSubscopes().get(0));
  }

}
