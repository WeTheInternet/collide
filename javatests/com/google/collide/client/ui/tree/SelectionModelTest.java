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

package com.google.collide.client.ui.tree;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import collide.client.filetree.FileTreeNode;
import collide.client.filetree.FileTreeNodeDataAdapter;
import collide.client.filetree.FileTreeNodeRenderer;
import collide.client.treeview.NodeDataAdapter;
import collide.client.treeview.NodeRenderer;
import collide.client.treeview.SelectionModel;
import collide.client.treeview.Tree;

import com.google.collide.json.client.JsoArray;
import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;


/**
 * Tests the {@link SelectionModel}.
 */
public class SelectionModelTest extends GWTTestCase {

  interface TestResources extends Tree.Resources, FileTreeNodeRenderer.Resources {
  }

  private static void checkNodeArray(
      JsoArray<FileTreeNode> expected, JsoArray<FileTreeNode> actual) {
    assertEquals("Array sizes don't line up!", expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      assertEquals("expected: " + expected.get(i).getName() + " != " + actual.get(i).getName(),
          expected.get(i), actual.get(i));
    }
  }

  private Tree<FileTreeNode> mockTree;
  private SelectionModel<FileTreeNode> mockSelectionModel;
  private TestResources resources;

  @Override
  public String getModuleName() {
    return TreeTestUtils.BUILD_MODULE_NAME;
  }

  @Override
  public void gwtTearDown() throws Exception {
    super.gwtTearDown();
  }

  /**
   * Tests select responses to ctrl clicks across tiers in the tree.
   */
  public void testCtrlSelectAcrossTiers() {
    FileTreeNode root = mockTree.getModel().getRoot();

    // Render the tree.
    mockTree.renderTree(-1);

    SignalEvent ctrlSignalEvent = new MockSignalEvent(true, false);

    // Select a bunch of nodes at the same tier.
    FileTreeNode AD1 = getNodeByPath(0);
    assertNotNull("Node did not get rendered!", AD1.getRenderedTreeNode());
    assertFalse(AD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    JsoArray<FileTreeNode> expectedSelects = JsoArray.create();

    // Select the first top level dir.
    mockSelectionModel.selectNode(AD1, ctrlSignalEvent);
    expectedSelects.add(AD1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select the second top level dir
    FileTreeNode BD1 = getNodeByPath(1);
    mockSelectionModel.selectNode(BD1, ctrlSignalEvent);
    expectedSelects.add(BD1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(BD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select deeper. We should not allow cross depth selecting and should
    // replace it with just the new select.
    FileTreeNode AF2 = getNodeByPath(0, 0);
    assertNotNull("Node did not get rendered!", AF2.getRenderedTreeNode());
    assertFalse(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Change the select.
    mockSelectionModel.selectNode(AF2, ctrlSignalEvent);
    expectedSelects.clear();
    expectedSelects.add(AF2);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(AD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select another peer node.
    FileTreeNode CF2 = getNodeByPath(0, 2);
    assertNotNull("Node did not get rendered!", CF2.getRenderedTreeNode());
    assertFalse(CF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Change the select.
    mockSelectionModel.selectNode(CF2, ctrlSignalEvent);
    expectedSelects.add(CF2);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertTrue(CF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select another peer node.
    FileTreeNode BF2 = getNodeByPath(0, 1);
    assertNotNull("Node did not get rendered!", BF2.getRenderedTreeNode());
    assertFalse(BF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Change the select.
    mockSelectionModel.selectNode(BF2, ctrlSignalEvent);
    // We need to enforce sort order. AF2, BF2, CF2.
    expectedSelects.splice(1, 0, BF2);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertTrue(BF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertTrue(CF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Ensure that if we ctrl click back higher in the stack that we clear the
    // selected list.
    FileTreeNode AF1 = getNodeByPath(3);
    assertNotNull("Node did not get rendered!", AF1.getRenderedTreeNode());
    assertFalse(AF1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Change the select.
    mockSelectionModel.selectNode(AF1, ctrlSignalEvent);
    expectedSelects.clear();
    expectedSelects.add(AF1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AF1.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(BF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(CF2.getRenderedTreeNode().isSelected(resources.treeCss()));
  }

  /**
   * Tests select responses to ctrl clicks that should result in select toggling
   */
  public void testCtrlSelectToggle() {
    FileTreeNode root = mockTree.getModel().getRoot();

    // Render the tree.
    mockTree.renderTree(-1);

    SignalEvent ctrlSignalEvent = new MockSignalEvent(true, false);

    // Select a bunch of nodes at the same tier.
    FileTreeNode AD1 = getNodeByPath(0);
    assertNotNull("Node did not get rendered!", AD1.getRenderedTreeNode());
    assertFalse(AD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    JsoArray<FileTreeNode> expectedSelects = JsoArray.create();

    // Select the first top level dir.
    mockSelectionModel.selectNode(AD1, ctrlSignalEvent);
    expectedSelects.add(AD1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select the second top level dir
    FileTreeNode BD1 = getNodeByPath(1);
    mockSelectionModel.selectNode(BD1, ctrlSignalEvent);
    expectedSelects.add(BD1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(BD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select the first file.
    FileTreeNode AF1 = getNodeByPath(3);
    assertNotNull("Node did not get rendered!", AF1.getRenderedTreeNode());
    assertFalse(AF1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Change the select.
    mockSelectionModel.selectNode(AF1, ctrlSignalEvent);
    expectedSelects.add(AF1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AF1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Now toggle the second dir.
    mockSelectionModel.selectNode(BD1, ctrlSignalEvent);
    expectedSelects.remove(BD1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertFalse(BD1.getRenderedTreeNode().isSelected(resources.treeCss()));
  }

  /**
   * Tests select responses to shift clicks that should do range selects.
   */
  public void testShiftSelect() {
    FileTreeNode root = mockTree.getModel().getRoot();

    // Render the tree.
    mockTree.renderTree(-1);

    SignalEvent shiftSignalEvent = new MockSignalEvent(false, true);

    FileTreeNode AD1 = getNodeByPath(0);
    assertNotNull("Node did not get rendered!", AD1.getRenderedTreeNode());
    assertFalse(AD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    JsoArray<FileTreeNode> expectedSelects = JsoArray.create();

    // Select the first top level dir.
    mockSelectionModel.selectNode(AD1, shiftSignalEvent);
    expectedSelects.add(AD1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AD1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Shift select the last top level file.
    FileTreeNode BF1 = getNodeByPath(4);
    mockSelectionModel.selectNode(BF1, shiftSignalEvent);
    expectedSelects.add(getNodeByPath(1));
    expectedSelects.add(getNodeByPath(2));
    expectedSelects.add(getNodeByPath(3));
    expectedSelects.add(getNodeByPath(4));
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(BF1.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertTrue(getNodeByPath(2).getRenderedTreeNode().isSelected(resources.treeCss()));
    assertTrue(getNodeByPath(3).getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select the last file. It should zero the shift selection.
    mockSelectionModel.selectNode(BF1, shiftSignalEvent);
    expectedSelects.clear();
    expectedSelects.add(BF1);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(BF1.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(getNodeByPath(0).getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(getNodeByPath(1).getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(getNodeByPath(2).getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(getNodeByPath(3).getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select deeper. We should not allow cross depth selecting and
    // should replace it with just the new select.
    FileTreeNode AF2 = getNodeByPath(0, 0);
    assertNotNull("Node did not get rendered!", AF2.getRenderedTreeNode());
    assertFalse(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Change the select.
    mockSelectionModel.selectNode(AF2, shiftSignalEvent);
    expectedSelects.clear();
    expectedSelects.add(AF2);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(BF1.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select the adjacent peer node.
    FileTreeNode BF2 = getNodeByPath(0, 1);
    assertNotNull("Node did not get rendered!", BF2.getRenderedTreeNode());
    assertFalse(BF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Change the select.
    mockSelectionModel.selectNode(BF2, shiftSignalEvent);
    expectedSelects.add(BF2);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertTrue(BF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Select the last peer node.
    FileTreeNode DF2 = getNodeByPath(0, 3);
    assertNotNull("Node did not get rendered!", DF2.getRenderedTreeNode());
    assertFalse(DF2.getRenderedTreeNode().isSelected(resources.treeCss()));

    // Change the select.
    mockSelectionModel.selectNode(DF2, shiftSignalEvent);
    expectedSelects.add(getNodeByPath(0, 2));
    expectedSelects.add(getNodeByPath(0, 3));
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(DF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertTrue(getNodeByPath(0, 2).getRenderedTreeNode().isSelected(resources.treeCss()));

    // Ensure that if we shift click the last one we clear the selected list.
    mockSelectionModel.selectNode(DF2, shiftSignalEvent);
    expectedSelects.clear();
    expectedSelects.add(DF2);
    checkNodeArray(expectedSelects, mockSelectionModel.getSelectedNodes());
    assertTrue(DF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(AF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(BF2.getRenderedTreeNode().isSelected(resources.treeCss()));
    assertFalse(getNodeByPath(0, 2).getRenderedTreeNode().isSelected(resources.treeCss()));
  }
  
  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();

    resources = GWT.create(TestResources.class);

    NodeDataAdapter<FileTreeNode> dataAdapter = new FileTreeNodeDataAdapter();
    NodeRenderer<FileTreeNode> dataRenderer = FileTreeNodeRenderer.create(resources);
    Tree.Model<FileTreeNode> model =
        new Tree.Model<FileTreeNode>(dataAdapter, dataRenderer, resources);

    mockTree = new Tree<FileTreeNode>(new Tree.View<FileTreeNode>(resources), model);
    mockTree.getModel()
        .setRoot(
            FileTreeNode.transform(TreeTestUtils
                .createMockTree(TreeTestUtils.CLIENT_NODE_INFO_FACTORY)));
    mockSelectionModel = mockTree.getSelectionModel();
  }

  private FileTreeNode getNodeByPath(int... indices) {
    FileTreeNode cursor = mockTree.getModel().getRoot();
    for (int i = 0; i < indices.length; i++) {
      cursor = cursor.getUnifiedChildren().get(indices[i]);
    }
    return cursor;
  }
}
