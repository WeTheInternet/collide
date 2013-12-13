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

import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeModel.NodeRequestCallback;
import collide.client.filetree.FileTreeModelNetworkController.OutgoingController;
import collide.client.filetree.FileTreeNode;

import com.google.collide.client.util.PathUtil;

/**
 * A no-op mock of {@link OutgoingController}.
 */
public class MockOutgoingController extends OutgoingController {

  public MockOutgoingController() {
    super(null);
  }

  void requestWorkspaceNode(
      FileTreeModel fileTreeModel, PathUtil path, NodeRequestCallback callback) {
  }
  
  void requestDirectoryChildren(
      FileTreeModel fileTreeModel, FileTreeNode node, NodeRequestCallback callback) {
  }
}
