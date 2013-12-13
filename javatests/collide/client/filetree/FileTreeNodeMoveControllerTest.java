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

package collide.client.filetree;

import collide.client.filetree.FileTreeNode;
import collide.client.filetree.FileTreeNodeMoveController;

import com.google.collide.client.workspace.TestUtils;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.FileInfo;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.dto.client.DtoClientImpls.DirInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.FileInfoImpl;
import com.google.collide.json.client.JsoArray;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test cases for {@link FileTreeNodeMoveController}
 *
 */
public class FileTreeNodeMoveControllerTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return TestUtils.BUILD_MODULE_NAME;
  }

  private FileTreeNode createFileNode(String name) {
    return FileInfoImpl.make().setName(name).setNodeType(TreeNodeInfo.FILE_TYPE).cast();
  }

  private FileTreeNode createFolderNode(String name) {
    JsoArray<FileInfo> files = JsoArray.create();
    JsoArray<DirInfo> subDirs = JsoArray.create();
    return DirInfoImpl.make()
        .setFiles(files)
        .setSubDirectories(subDirs)
        .setName(name)
        .setNodeType(TreeNodeInfo.DIR_TYPE)
        .cast();
  }

  public void testIsMoveAllowed() {
    // d1
    // --d2
    // -----d3
    // ---------f1.js
    // ---------f2.js
    // --d4
    // -----f3.js
    // d5
    // --f4.js
    // f5.js
    FileTreeNode f1 = createFileNode("f1.js");
    FileTreeNode f2 = createFileNode("f2.js");
    FileTreeNode f3 = createFileNode("f3.js");
    FileTreeNode f4 = createFileNode("f4.js");
    FileTreeNode f5 = createFileNode("f5.js");
    FileTreeNode d3 = createFolderNode("d3");
    d3.addChild(f1);
    d3.addChild(f2);
    FileTreeNode d2 = createFolderNode("d2");
    d2.addChild(d3);
    FileTreeNode d4 = createFolderNode("d4");
    d4.addChild(f3);
    FileTreeNode d1 = createFolderNode("d1");
    d1.addChild(d2);
    d1.addChild(d4);

    FileTreeNode d5 = createFolderNode("d5");
    d5.addChild(f4);

    FileTreeNode root = createFolderNode("");
    root.addChild(d1);
    root.addChild(d5);
    root.addChild(f5);

    FileTreeNodeMoveController moveController = new FileTreeNodeMoveController(null, null, null);
    JsoArray<FileTreeNode> nodesToMove = JsoArray.create();
    
    // Try to move "f1.js".
    nodesToMove.add(f1);
    moveController.setNodesToMove(nodesToMove);
    assertFalse(moveController.isMoveAllowed(d3));
    assertTrue(moveController.isMoveAllowed(d1));
    assertTrue(moveController.isMoveAllowed(d2));
    assertTrue(moveController.isMoveAllowed(d4));
    assertTrue(moveController.isMoveAllowed(d5));
    assertTrue(moveController.isMoveAllowed(root));

    // Try to move "f5.js"
    nodesToMove.clear();
    nodesToMove.add(f5);
    moveController.setNodesToMove(nodesToMove);
    assertFalse(moveController.isMoveAllowed(root));
    assertTrue(moveController.isMoveAllowed(d1));
    assertTrue(moveController.isMoveAllowed(d2));
    assertTrue(moveController.isMoveAllowed(d3));
    assertTrue(moveController.isMoveAllowed(d4));
    assertTrue(moveController.isMoveAllowed(d5));
    
    // Try to move "d3"
    nodesToMove.clear();
    nodesToMove.add(d3);
    moveController.setNodesToMove(nodesToMove);
    assertFalse(moveController.isMoveAllowed(d3));
    assertFalse(moveController.isMoveAllowed(d2));
    assertTrue(moveController.isMoveAllowed(d1));
    assertTrue(moveController.isMoveAllowed(d4));
    assertTrue(moveController.isMoveAllowed(d5));
    assertTrue(moveController.isMoveAllowed(root));
    
    // Try to move "d2"
    nodesToMove.clear();
    nodesToMove.add(d2);
    moveController.setNodesToMove(nodesToMove);
    assertFalse(moveController.isMoveAllowed(d1));
    assertFalse(moveController.isMoveAllowed(d2));
    assertFalse(moveController.isMoveAllowed(d3));
    assertTrue(moveController.isMoveAllowed(d4));
    assertTrue(moveController.isMoveAllowed(d5));
    assertTrue(moveController.isMoveAllowed(root));

    // Try to move "d1"
    nodesToMove.clear();
    nodesToMove.add(d1);
    moveController.setNodesToMove(nodesToMove);
    assertFalse(moveController.isMoveAllowed(root));
    assertFalse(moveController.isMoveAllowed(d1));
    assertFalse(moveController.isMoveAllowed(d2));
    assertFalse(moveController.isMoveAllowed(d3));
    assertFalse(moveController.isMoveAllowed(d4));
    assertTrue(moveController.isMoveAllowed(d5));

  }
}
