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

package com.google.collide.client.workspace;

import com.google.collide.client.code.FileTreeNodeDataAdapter;
import com.google.collide.client.code.FileTreeNodeRenderer;
import com.google.collide.client.code.debugging.DebuggingModel;
import com.google.collide.client.code.debugging.DebuggingModelController;
import com.google.collide.client.code.popup.EditorPopupController;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.testing.CommunicationGwtTestCase;
import com.google.collide.client.testing.StubWorkspaceInfo;
import com.google.collide.client.ui.tree.Tree;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.client.ui.tree.TreeNodeLabelRenamer;
import com.google.collide.client.util.Elements;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.FileInfo;
import com.google.collide.dto.GetWorkspace;
import com.google.collide.dto.GetWorkspaceResponse;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.dto.WorkspaceInfo;
import com.google.collide.dto.client.DtoClientImpls.DirInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.GetWorkspaceImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

import elemental.html.Element;
import elemental.html.IFrameElement;

/**
 */
public class FileTreeContextMenuControllerTest extends CommunicationGwtTestCase {

  private FileTreeContextMenuController controller;
  private Tree<FileTreeNode> tree;

  @Override
  public String getModuleName() {
    return TestUtils.BUILD_MODULE_NAME;
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();

    // Create our tree, with a model of a few dummy nodes.
    FileTreeNodeDataAdapter dataAdapter = new FileTreeNodeDataAdapter();
    FileTreeNodeRenderer nodeRenderer =
        FileTreeNodeRenderer.create(context.getResources());
    Tree.View<FileTreeNode> view = new Tree.View<FileTreeNode>(context.getResources());
    Tree.Model<FileTreeNode> model =
        new Tree.Model<FileTreeNode>(dataAdapter, nodeRenderer, context.getResources());
    // An empty root directory.
    DirInfoImpl mockDirInfo = DirInfoImpl.make();
    mockDirInfo.setNodeType(TreeNodeInfo.DIR_TYPE);
    JsoArray<FileInfo> files = JsoArray.create();
    mockDirInfo.setFiles(files);
    JsoArray<DirInfo> subdirs = JsoArray.create();
    mockDirInfo.setSubDirectories(subdirs);
    mockDirInfo.setName("");
    mockDirInfo.setIsComplete(true);
    FileTreeNode root = FileTreeNode.transform(mockDirInfo);
    model.setRoot(root);
    tree = new Tree<FileTreeNode>(view, model);

    // Create all the other objects we need, or mocks for them...
    Place place = new Place("mockPlace") {
      @Override
      public PlaceNavigationEvent<? extends Place> createNavigationEvent(
          JsonStringMap<String> decodedState) {
        return null;
      }
    };
    FileTreeModel fileTreeModel = new FileTreeModel(new MockOutgoingController());
    DebuggingModel debuggingModel = new DebuggingModel();
    Editor editor = Editor.create(context);
    EditorPopupController editorPopupController =
        EditorPopupController.create(context.getResources(), editor);
    DebuggingModelController debuggingModelController = DebuggingModelController.create(
        place, context, debuggingModel, editor, editorPopupController, null);

    JsoStringMap<String> templates = JsoStringMap.create();
    FileTreeUiController uiController = FileTreeUiController.create(place,
        fileTreeModel,
        tree,
        context,
        debuggingModelController);
    TreeNodeLabelRenamer<FileTreeNode> nodeRenamer =
        new TreeNodeLabelRenamer<FileTreeNode>(nodeRenderer, dataAdapter,
            context.getResources().workspaceNavigationFileTreeNodeRendererCss());

    // ...all by way of getting to create the thing we actually want:
    controller = new FileTreeContextMenuController(place,
        uiController,
        fileTreeModel,
        nodeRenamer,
        context,
        debuggingModelController);
  }

  @Override
  public void gwtTearDown() throws Exception {
    super.gwtTearDown();
    Element iframe =
        Elements.getDocument().getElementById(FileTreeContextMenuController.DOWNLOAD_FRAME_ID);
    if (iframe != null) {
      iframe.removeFromParent();
    }
  }

  public void testDownloadUnknownWorkspace() {
    FileTreeNode data = null;
    TreeNodeElement<FileTreeNode> parentTreeNode = tree.getNode(tree.getModel().getRoot());
    controller.handleDownload(parentTreeNode, true);

    Element iframe =
        Elements.getDocument().getElementById(FileTreeContextMenuController.DOWNLOAD_FRAME_ID);
    assertFalse("No iframe added", iframe == null);
    String url = ((IFrameElement) iframe).getSrc();
    // TODO: fix
    // assertTrue("Bad url: " + url, url.contains("/workspace-" + MOCK_WORKSPACE_ID + ".zip?"));
  }

  public void testDownloadWorkspace() {
    FileTreeNode data = null;
    TreeNodeElement<FileTreeNode> parentTreeNode = tree.getNode(tree.getModel().getRoot());
    expectMockWorkspaceInfo("Mock Workspace");
    controller.handleDownload(parentTreeNode, true);

    Element iframe =
        Elements.getDocument().getElementById(FileTreeContextMenuController.DOWNLOAD_FRAME_ID);
    assertFalse(iframe == null);
    String url = ((IFrameElement) iframe).getSrc();
    assertTrue("Bad url: " + url, url.contains("/Mock_Workspace.zip?rt=zip&"));
    assertTrue("Bad url: " + url, url.endsWith("&file=/"));
    iframe.removeFromParent();

    controller.handleDownload(null, true);

    iframe = Elements.getDocument().getElementById(FileTreeContextMenuController.DOWNLOAD_FRAME_ID);
    assertFalse(iframe == null);
    url = ((IFrameElement) iframe).getSrc();
    assertTrue("Bad url: " + url, url.contains("/Mock_Workspace.zip?rt=zip&"));
    assertTrue("Bad url: " + url, url.endsWith("&file=/"));
    iframe.removeFromParent();
  }

  public void testBadCharacterWorkspace() {
    FileTreeNode data = null;
    TreeNodeElement<FileTreeNode> parentTreeNode = tree.getNode(tree.getModel().getRoot());
    expectMockWorkspaceInfo("M o\tc/k:W\\o;r'k\"s&p?a#c%e");
    controller.handleDownload(parentTreeNode, true);

    Element iframe =
        Elements.getDocument().getElementById(FileTreeContextMenuController.DOWNLOAD_FRAME_ID);
    assertFalse(iframe == null);
    String url = ((IFrameElement) iframe).getSrc();
    assertTrue("Bad url: " + url, url.contains("/M_o_c_k_W_o_r_k_s%26p%3Fa%23c%25e.zip?"));
  }

  private GetWorkspace makeRequest() {
    return GetWorkspaceImpl.make();
  }

  private void expectMockWorkspaceInfo(final String name) {
    GetWorkspace request = makeRequest();
    GetWorkspaceResponse response = new GetWorkspaceResponse() {
      WorkspaceInfo info = StubWorkspaceInfo
          .make()
          .setName(name)
          .setDescription("description of a workspace " + name)
          .setParentId("mockParentWorkspaceId");

      @Override
      public WorkspaceInfo getWorkspace() {
        return info;
      }

      @Override
      public int getType() {
        return 0;
      }
    };

    // TODO: fix?
    // context.getMockFrontendApi().getGetWorkspacesMockApi().expectAndReturn(request, response);
  }
}
