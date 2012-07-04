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

package com.google.collide.client.code.autocomplete;

import com.google.collide.client.testutil.SynchronousTestCase;

/**
 * Test cases for {@link AbstractTrie}.
 */
public class AbstractTrieTest extends SynchronousTestCase {

  AbstractTrie<String> trie;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    trie = TestUtils.createStringTrie("a", "ab", "ac");
  }

  public void testDuplicates() {
    trie = TestUtils.createStringTrie("abc", "def", "abc");
    assertEquals("abc,def", AbstractTrie.collectSubtree(trie.getRoot()).join(","));
    assertEquals("abc", trie.search("ab").join(","));
  }

  public void testfindAutocompletions() {
    assertEquals("a,ab,ac", trie.search("").join(","));
    assertEquals("ac", trie.search("ac").join(""));
    assertEquals(0, trie.search("b").size());
  }

  public void testfindNode() {
    TrieNode<String> node = AbstractTrie.findNode("", trie.getRoot());
    assertEquals(trie.getRoot(), node);

    node = AbstractTrie.findNode("a", trie.getRoot());
    assertEquals("a", node.getPrefix());

    node = AbstractTrie.findNode("b", trie.getRoot());
    assertNull(node);
  }

  public void testGetAllLeavesInSubtree() {
    assertEquals("a,ab,ac", AbstractTrie.collectSubtree(trie.getRoot()).join(","));
  }

  public void testInsertIntoTrie() {
    trie.put("foo", "foo");
    assertNotNull(AbstractTrie.findNode("foo", trie.getRoot()));
  }

  public void testTrieSetup() {
    assertEquals("", trie.getRoot().getPrefix());
  }

  public void testPopulateTrie() {
    trie = TestUtils.createStringTrie("a.b", "a.c", "a.b.c");
    assertNotNull(AbstractTrie.findNode("a.b", trie.getRoot()));
    assertNotNull(AbstractTrie.findNode("a.c", trie.getRoot()));
    assertNotNull(AbstractTrie.findNode("a.b.c", trie.getRoot()));
  }

  public void testFindAutocompletions() {
    trie = TestUtils.createStringTrie("a.b", "a.c", "a.b.c");
    assertEquals("a.b.c", trie.search("a.b.").join(""));
  }

  public void testCL21928774() {
    // Prior to CL 21928774 this test would fail with ClassCastException
    trie = new AbstractTrie<String>();
    trie.put("__proto__", "__proto__");
  }
}
