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

package com.google.collide.shared.util;


import com.google.collide.dto.WorkspaceInfo;
import com.google.collide.dto.server.DtoServerImpls.MockWorkspaceInfoImpl;
import com.google.collide.dto.server.DtoServerImpls.WorkspaceInfoImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.WorkspaceUtils.WorkspaceNode;

import junit.framework.TestCase;

import java.util.Comparator;

/**
 * Tests for {@link WorkspaceUtils}.
 * 
 */
public class WorkspaceUtilsTests extends TestCase {

  public void testGetWorkspaceHierarchy() {
    // Create a list of workspaces.
    JsonArray<WorkspaceInfo> workspaces = JsonCollections.createArray();
    WorkspaceInfo a = makeWorkspace("a", null);
    WorkspaceInfo b = makeWorkspace("b", null);
    WorkspaceInfo aa = makeWorkspace("aa", a);
    WorkspaceInfo ab = makeWorkspace("ab", a);
    WorkspaceInfo ac = makeWorkspace("ac", a);
    WorkspaceInfo aaa = makeWorkspace("aaa", aa);
    WorkspaceInfo aab = makeWorkspace("aab", aa);
    WorkspaceInfo aac = makeWorkspace("aac", aa);
    workspaces.add(aa);
    workspaces.add(ab);
    workspaces.add(aab);
    workspaces.add(ac);
    workspaces.add(aac);
    workspaces.add(b);
    workspaces.add(aaa);
    workspaces.add(a);

    // Add an element with a parent that is not mapped.
    WorkspaceInfo zaa = makeWorkspace("zaa", makeWorkspace("unmapped", null));
    workspaces.add(zaa);

    // Create an unsorted hierarchy.
    JsonArray<WorkspaceNode> roots =
        WorkspaceUtils.getWorkspaceHierarchy(workspaces, new Comparator<WorkspaceInfo>() {
          @Override
          public int compare(WorkspaceInfo o1, WorkspaceInfo o2) {
            return o1.getId().compareTo(o2.getId());
          }
        });
    assertEquals(3, roots.size());
    assertEquals(a, roots.get(0).getWorkspace());
    assertEquals(b, roots.get(1).getWorkspace());
    assertEquals(zaa, roots.get(2).getWorkspace());

    // b and daa are empty parents.
    assertEquals(0, roots.get(1).getChildCount());
    assertEquals(0, roots.get(2).getChildCount());

    // a contains unsorted children.
    assertChildrenUnordered(roots.get(0), aa, ab, ac);

    // aa contains unsorted children.
    assertChildrenUnordered(roots.get(0).getChild(0), aaa, aab, aac);
  }

  /**
   * Test {@link WorkspaceUtils#getWorkspaceHierarchy} with a map
   * of root workspaces.
   */
  public void testGetWorkspaceHierarchyAllRoots() {
    JsonArray<WorkspaceInfo> workspaces = JsonCollections.createArray();
    workspaces.add(makeWorkspace("b", null));
    workspaces.add(makeWorkspace("a", null));
    workspaces.add(makeWorkspace("c", null));

    JsonArray<WorkspaceNode> roots = WorkspaceUtils.getWorkspaceHierarchy(workspaces);
    assertEquals(3, roots.size());
    for (int i = 0; i < 3; i++) {
      assertEquals(0, roots.get(i).getChildCount());
    }
    // Due to the nature of maps, the nodes are not in a specified order.
  }

  /**
   * Test {@link WorkspaceUtils#getWorkspaceHierarchy} if one of
   * the workspaces refers to a parent that is not mapped.
   */
  public void testGetWorkspaceHierarchyNonExistentParent() {
    JsonArray<WorkspaceInfo> workspaces = JsonCollections.createArray();
    WorkspaceInfo unmapped = makeWorkspace("unmapped", null);
    workspaces.add(makeWorkspace("b", unmapped));
    workspaces.add(makeWorkspace("a", null));
    workspaces.add(makeWorkspace("c", null));

    JsonArray<WorkspaceNode> roots = WorkspaceUtils.getWorkspaceHierarchy(workspaces);
    assertEquals(3, roots.size());
    for (int i = 0; i < 3; i++) {
      assertEquals(0, roots.get(i).getChildCount());
    }
    // Due to the nature of maps, the nodes are not in a specified order.
  }

  public void testGetWorkspaceHierarchySorted() {
    // Create a list of workspaces.
    JsonArray<WorkspaceInfo> workspaces = JsonCollections.createArray();
    WorkspaceInfo a = makeWorkspace("a", null);
    WorkspaceInfo b = makeWorkspace("b", null);
    WorkspaceInfo aa = makeWorkspace("aa", a);
    WorkspaceInfo ab = makeWorkspace("ab", a);
    WorkspaceInfo ac = makeWorkspace("ac", a);
    WorkspaceInfo aaa = makeWorkspace("aaa", aa);
    WorkspaceInfo aab = makeWorkspace("aab", aa);
    WorkspaceInfo aac = makeWorkspace("aac", aa);
    workspaces.add(aa);
    workspaces.add(ab);
    workspaces.add(aab);
    workspaces.add(ac);
    workspaces.add(aac);
    workspaces.add(b);
    workspaces.add(aaa);
    workspaces.add(a);

    // Add an element with a parent that is not mapped.
    WorkspaceInfo zaa = makeWorkspace("zaa", makeWorkspace("unmapped", null));
    workspaces.add(zaa);

    // Create a sorted hierarchy.
    JsonArray<WorkspaceNode> roots =
        WorkspaceUtils.getWorkspaceHierarchy(workspaces, new Comparator<WorkspaceInfo>() {
          @Override
          public int compare(WorkspaceInfo o1, WorkspaceInfo o2) {
            return o1.getId().compareTo(o2.getId());
          }
        });
    assertEquals(3, roots.size());
    assertEquals(a, roots.get(0).getWorkspace());
    assertEquals(b, roots.get(1).getWorkspace());
    assertEquals(zaa, roots.get(2).getWorkspace());

    // b and daa are empty parents.
    assertEquals(0, roots.get(1).getChildCount());
    assertEquals(0, roots.get(2).getChildCount());

    // a contains sorted children.
    assertChildren(roots.get(0), aa, ab, ac);

    // aa contains sorted children.
    assertChildren(roots.get(0).getChild(0), aaa, aab, aac);
  }

  /**
   * Test that
   * {@link WorkspaceUtils#getWorkspaceHierarchy}
   * sorts the root nodes.
   */
  public void testGetWorkspaceHierarchySortedRoots() {
    // Create a list of workspaces.
    JsonArray<WorkspaceInfo> workspaces = JsonCollections.createArray();
    WorkspaceInfo a = makeWorkspace("a", null);
    WorkspaceInfo b = makeWorkspace("b", null);
    WorkspaceInfo c = makeWorkspace("c", null);
    workspaces.add(b);
    workspaces.add(a);
    workspaces.add(c);

    // Create a sorted hierarchy.
    JsonArray<WorkspaceNode> roots =
        WorkspaceUtils.getWorkspaceHierarchy(workspaces, new Comparator<WorkspaceInfo>() {
          @Override
          public int compare(WorkspaceInfo o1, WorkspaceInfo o2) {
            return o1.getId().compareTo(o2.getId());
          }
        });
    assertEquals(3, roots.size());
    assertEquals(a, roots.get(0).getWorkspace());
    assertEquals(b, roots.get(1).getWorkspace());
    assertEquals(c, roots.get(2).getWorkspace());
  }

  public void testWorkspaceNodeAddChild() {
    WorkspaceNode parentNode = makeWorkspaceNode("parent", null);
    assertEquals(0, parentNode.getChildCount());

    WorkspaceInfo child0 = makeWorkspace("child0", parentNode.getWorkspace());
    WorkspaceNode childNode0 = new WorkspaceNode(child0);
    parentNode.addChild(childNode0);
    assertEquals(1, parentNode.getChildCount());
    assertEquals(childNode0, parentNode.getChild(0));

    WorkspaceInfo child1 = makeWorkspace("child1", parentNode.getWorkspace());
    WorkspaceNode childNode1 = new WorkspaceNode(child1);
    parentNode.addChild(childNode1);
    assertEquals(2, parentNode.getChildCount());
    assertEquals(childNode0, parentNode.getChild(0));
    assertEquals(childNode1, parentNode.getChild(1));
  }

  public void testWorkspaceNodeSortChildren() {
    // Create a hierarchy.
    WorkspaceNode a = makeWorkspaceNode("a", null);

    WorkspaceNode ab = makeWorkspaceNode("ab", a);
    WorkspaceNode aa = makeWorkspaceNode("aa", a);
    WorkspaceNode ac = makeWorkspaceNode("ac", a);

    WorkspaceNode aaa = makeWorkspaceNode("aaa", aa);
    WorkspaceNode aac = makeWorkspaceNode("aac", aa);
    WorkspaceNode aab = makeWorkspaceNode("aab", aa);

    // Sort non-recursively.
    a.sortChildren(new Comparator<WorkspaceNode>() {
      @Override
      public int compare(WorkspaceNode o1, WorkspaceNode o2) {
        return o1.getWorkspace().getId().compareTo(o2.getWorkspace().getId());
      }
    });

    // Assert the children sorted.
    assertEquals(aa, a.getChild(0));
    assertEquals(ab, a.getChild(1));
    assertEquals(ac, a.getChild(2));

    // Assert the grandchildren are NOT sorted.
    assertEquals(aaa, aa.getChild(0));
    assertEquals(aac, aa.getChild(1));
    assertEquals(aab, aa.getChild(2));
  }

  public void testWorkspaceNodeSortChildrenRecursive() {
    // Create a hierarchy.
    WorkspaceNode a = makeWorkspaceNode("a", null);

    WorkspaceNode ab = makeWorkspaceNode("ab", a);
    WorkspaceNode aa = makeWorkspaceNode("aa", a);
    WorkspaceNode ac = makeWorkspaceNode("ac", a);

    WorkspaceNode aaa = makeWorkspaceNode("aaa", aa);
    WorkspaceNode aac = makeWorkspaceNode("aac", aa);
    WorkspaceNode aab = makeWorkspaceNode("aab", aa);

    // Sort non-recursively.
    a.sortChildren(new Comparator<WorkspaceNode>() {
      @Override
      public int compare(WorkspaceNode o1, WorkspaceNode o2) {
        return o1.getWorkspace().getId().compareTo(o2.getWorkspace().getId());
      }
    }, true);

    // Assert the children sorted.
    assertEquals(aa, a.getChild(0));
    assertEquals(ab, a.getChild(1));
    assertEquals(ac, a.getChild(2));

    // Assert the grandchildren are also sorted.
    assertEquals(aaa, aa.getChild(0));
    assertEquals(aab, aa.getChild(1));
    assertEquals(aac, aa.getChild(2));
  }

  /**
   * Assert that a {@link WorkspaceNode} contains exactly the expected children
   * workspaces, in order.
   * 
   * @param node the node to check
   * @param expected the expected values, in order
   */
  private void assertChildren(WorkspaceNode node, WorkspaceInfo... expected) {
    // Check the size.
    int size = expected.length;
    assertEquals("Size mismatch", size, node.getChildCount());

    // Check the values.
    for (int i = 0; i < size; i++) {
      assertEquals(expected[i], node.getChild(i).getWorkspace());
    }
  }

  /**
   * Assert that a {@link WorkspaceNode} contains exactly the expected children
   * workspaces, in any order. Does not handle duplicate expected values.
   * 
   * @param node the node to check
   * @param expected the expected values, in order
   */
  private void assertChildrenUnordered(WorkspaceNode node, WorkspaceInfo... expected) {
    // Check the size.
    int size = expected.length;
    assertEquals("Size mismatch", size, node.getChildCount());

    // Check the values.
    for (int i = 0; i < size; i++) {
      WorkspaceInfo toFind = expected[i];
      boolean found = false;
      for (int j = 0; j < size; j++) {
        if (node.getChild(i).getWorkspace() == toFind) {
          found = true;
          break;
        }
      }
      if (!found) {
        fail("Node does not contain expected value " + expected);
      }
    }
  }

  private WorkspaceInfo makeWorkspace(String id, WorkspaceInfo parent) {
    WorkspaceInfoImpl ws = MockWorkspaceInfoImpl.make();
    ws.setId(id);
    if (parent != null) {
      ws.setParentId(parent.getId());
    }
    return ws;
  }

  private WorkspaceNode makeWorkspaceNode(String id, WorkspaceNode parent) {
    WorkspaceInfo parentWs = (parent == null) ? null : parent.getWorkspace();
    WorkspaceInfo ws = makeWorkspace(id, parentWs);
    WorkspaceNode node = new WorkspaceNode(ws);
    if (parent != null) {
      parent.addChild(node);
    }
    return node;
  }
}
