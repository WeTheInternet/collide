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
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createCodeGraph;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createFreshness;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createTypeAssociation;

import com.google.collide.client.code.autocomplete.TestUtils;
import com.google.collide.client.codeunderstanding.CodeGraphTestUtils.MockCubeClient;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.TypeAssociation;
import com.google.collide.dto.client.DtoClientImpls;
import com.google.collide.dto.client.DtoClientImpls.CodeBlockImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphResponseImpl;
import com.google.collide.json.client.Jso;
import com.google.collide.json.client.JsoArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Test for CodeGraphSource.
 *
 */
public class CodeGraphSourceTest extends SynchronousTestCase {
  @Override
  public String getModuleName() {
    return "com.google.collide.client.code.autocomplete.codegraph.CodeGraphTestModule";
  }

  private static class UpdateReceiver implements Runnable {
    int runCount;

    @Override
    public void run() {
      runCount++;
    }
  }

  public void testFreshFileTreeWontDestroyLinks() {
    CodeGraphResponseImpl response = DtoClientImpls.MockCodeGraphResponseImpl.make();
    response.setFreshness(createFreshness("0", "1", "1"));

    {
      CodeBlock fileBlock = createCodeBlock("0", "/foo.js", CodeBlock.Type.FILE, 0, 0, 10, 0);
      CodeBlock foo = createCodeBlock(fileBlock, "1", "foo", CodeBlock.Type.FIELD, 0, 0, 1, 0);
      CodeBlock bar = createCodeBlock(fileBlock, "2", "bar", CodeBlock.Type.FIELD, 1, 0, 2, 0);
      createCodeBlock(fileBlock, "3", "bar.doThis", CodeBlock.Type.FUNCTION, 1, 10, 2, 0);
      TypeAssociation typeLink = createTypeAssociation(fileBlock, foo, fileBlock, bar);
      CodeGraphImpl codeGraph = createCodeGraph(fileBlock);
      codeGraph.setTypeAssociations(JsoArray.<TypeAssociation>from(typeLink));

      response.setFullGraphJson(codeGraph.serialize());
    }
    {
      CodeBlockImpl freshFileBlock = createCodeBlock(
          "1", "/foo.js", CodeBlock.Type.FILE, 0, 0, 10, 0);
      createCodeBlock(freshFileBlock, "1", "foo", CodeBlock.Type.FIELD, 0, 0, 1, 0);
      createCodeBlock(freshFileBlock, "2", "foo.baz", CodeBlock.Type.FIELD, 1, 0, 2, 0);
      createCodeBlock(freshFileBlock, "3", "bar", CodeBlock.Type.FIELD, 2, 0, 3, 0);
      createCodeBlock(freshFileBlock, "4", "bar.doThis", CodeBlock.Type.FUNCTION, 2, 10, 3, 0);
      response.setFileTreeJson(Jso.serialize(freshFileBlock));
    }

    MockCubeClient cubeClient = MockCubeClient.create();
    UpdateReceiver updateListener = new UpdateReceiver();

    CodeGraphSource codeGraphSource = new CodeGraphSource(cubeClient, updateListener);
    codeGraphSource.setPaused(false);

    // This will immediately fire api call
    cubeClient.setPath("/foo.js");
    try {
      assertEquals("one api call after setDocument", 1, cubeClient.api.collectedCallbacks.size());
      cubeClient.api.collectedCallbacks.get(0).onMessageReceived(response);
    } finally {
      cubeClient.cleanup();
    }
    assertEquals("one update after data received", 1, updateListener.runCount);
    assertTrue("codeGraphSource received update", codeGraphSource.hasUpdate());

    CodeGraphPrefixIndex prefixIndex = new CodeGraphPrefixIndex(
        codeGraphSource.constructCodeGraph(), SyntaxType.JS);

    assertEquals("search in updated trie", JsonCollections.createStringSet("foo.baz", "foo.doThis"),
        TestUtils.createNameSet(prefixIndex.search("foo.")));
  }
}
