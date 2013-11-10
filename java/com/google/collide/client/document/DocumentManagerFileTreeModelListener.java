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

package com.google.collide.client.document;

import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeNode;

import com.google.collide.client.editor.Editor;
import com.google.collide.client.history.RootPlace;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.Pair;
import com.google.collide.shared.document.Document;

/**
 * Listener for relevant file tree model events (such as a node being removed) and updates the
 * document manager accordingly.
 *
 */
class DocumentManagerFileTreeModelListener implements FileTreeModel.TreeModelChangeListener {

  private final DocumentManager documentManager;
  private final FileTreeModel fileTreeModel;

  DocumentManagerFileTreeModelListener(
      DocumentManager documentManager, FileTreeModel fileTreeModel) {

    this.documentManager = documentManager;
    this.fileTreeModel = fileTreeModel;

    attachListeners();
  }

  private void attachListeners() {
    fileTreeModel.addModelChangeListener(this);
  }

  void teardown() {
    fileTreeModel.removeModelChangeListener(this);
  }

  @Override
  public void onNodeAdded(PathUtil parentDirPath, FileTreeNode newNode) {}

  @Override
  public void onNodeMoved(
      PathUtil oldPath, FileTreeNode node, PathUtil newPath, FileTreeNode newNode) {
    // Update the document path if the document exists.
    String fileEditSessionKey = (node == null) ? null : node.getFileEditSessionKey();
    if (fileEditSessionKey != null) {
      Document document = documentManager.getDocumentByFileEditSessionKey(fileEditSessionKey);
      if (document != null) {
        DocumentMetadata.putPath(document, newPath);
        return;
      }
    }
  }

  @Override
  public void onNodesRemoved(JsonArray<FileTreeNode> oldNodes) {
    JsonArray<Document> documents = documentManager.getDocuments();
    JsonArray<Pair<Document, Editor>> openDocuments = documentManager.getOpenDocuments();

    for (int k = 0; k < oldNodes.size(); k++) {
      // Note that this can be a parent directory
      PathUtil removedPath = oldNodes.get(k).getNodePath();

      for (int i = 0, n = documents.size(); i < n; i++) {
        Document document = documents.get(i);

        if (DocumentMetadata.isLinkedToFile(document)) {
          PathUtil path = DocumentMetadata.getPath(document);
          if (path == null || !removedPath.containsPath(path)) {
            continue;
          }

          updateEditorsForFileInvalidated(document, openDocuments, false);
        }
      }
    }
  }

  private void updateEditorsForFileInvalidated(Document document,
      JsonArray<Pair<Document, Editor>> openDocuments, boolean switchToReadOnly) {

    boolean isDocumentOpen = false;
    for (int j = 0; j < openDocuments.size(); j++) {
      Pair<Document, Editor> documentAndEditor = openDocuments.get(j);
      if (documentAndEditor.first == document) {
        if (!switchToReadOnly) {
          /*
           * TODO: in future CL, update UI to handle tabs, currently we display a
           * NoFileSelected place holder page in editor
           */
          RootPlace.PLACE.fireChildPlaceNavigation(WorkspacePlace.PLACE.createNavigationEvent());
        } else {
          documentAndEditor.second.setReadOnly(true);
        }
        documentManager.unlinkFromFile(document);
        isDocumentOpen = true;
      }
    }

    if (!isDocumentOpen) {
      documentManager.garbageCollectDocument(document);
    }
  }

  @Override
  public void onNodeReplaced(FileTreeNode oldNode, FileTreeNode newNode) {
    JsonArray<Document> documents = documentManager.getDocuments();
    JsonArray<Pair<Document, Editor>> openDocuments = documentManager.getOpenDocuments();
    PathUtil nodePath = newNode.getNodePath();

    for (int i = 0, n = documents.size(); i < n; i++) {
      Document document = documents.get(i);
      if (DocumentMetadata.isLinkedToFile(document) && nodePath.containsPath(
          DocumentMetadata.getPath(document))) {
        updateEditorsForFileInvalidated(document, openDocuments, true);
      }
    }
  }
}
