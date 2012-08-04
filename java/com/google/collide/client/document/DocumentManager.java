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

import com.google.collide.client.AppContext;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.FileTreeModel;
import com.google.collide.client.workspace.FileTreeNode;
import com.google.collide.dto.ConflictChunk;
import com.google.collide.dto.FileContents;
import com.google.collide.dto.NodeConflictDto.ConflictHandle;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.Pair;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.collide.shared.util.ListenerRegistrar;

/**
 * Manager for documents and editors.
 *
 *  Note that a document can be unlinked from a file <em>while</em> it is open
 * in an editor!
 *
 */
public class DocumentManager {

  public static DocumentManager create(FileTreeModel fileTreeModel, AppContext appContext) {
    return new DocumentManager(fileTreeModel, appContext);
  }

  /**
   * Listener for changes to the lifecycle of individual documents.
   */
  public interface LifecycleListener {
    /**
     * Called after the document is created.
     */
    void onDocumentCreated(Document document);

    /**
     * Called after the document is linked to a file.
     */
    void onDocumentLinkedToFile(Document document, FileContents fileContents);

    /**
     * Called after the document is opened in an editor.
     */
    void onDocumentOpened(Document document, Editor editor);

    /**
     * Called after the document is no longer open in an editor.
     */
    void onDocumentClosed(Document document, Editor editor);

    /**
     * Called <em>before</em> the document is unlinked to its file (calling the
     * {@link DocumentMetadata} getters for file-related metadata is okay.)
     */
    void onDocumentUnlinkingFromFile(Document document);

    /**
     * Called after the document has been garbage collected.
     */
    void onDocumentGarbageCollected(Document document);
  }

  /**
   * Listener for the loading of a document.
   */
  public interface GetDocumentCallback {
    void onDocumentReceived(Document document);

    void onUneditableFileContentsReceived(FileContents contents);

    void onFileNotFoundReceived();
  }

  private static final int MAX_CACHED_DOCUMENTS = 4;

  private final FileTreeModel fileTreeModel;

  private final DocumentManagerNetworkController networkController;
  private final DocumentManagerFileTreeModelListener fileTreeModelListener;

  /**
   * All of the documents, ordered by least-recently used documents (index 0 is
   * the least recently used).
   */
  private final JsonArray<Document> documents = JsonCollections.createArray();
  private final JsonStringMap<Document> documentsByFileEditSessionKey = JsonCollections.createMap();

  private final ListenerManager<LifecycleListener> lifecycleListenerManager =
      ListenerManager.create();

  /*
   * TODO: this will need to become a Document -> Editors map
   * eventually
   */
  private Editor editor;

  private DocumentManager(FileTreeModel fileTreeModel, AppContext appContext) {
    this.fileTreeModel = fileTreeModel;
    networkController = new DocumentManagerNetworkController(this, appContext);
    fileTreeModelListener = new DocumentManagerFileTreeModelListener(this, fileTreeModel);
  }

  public void cleanup() {
    fileTreeModelListener.teardown();
    networkController.teardown();

    while (documents.size() > 0) {
      garbageCollectDocument(documents.get(0));
    }
  }

  public ListenerRegistrar<LifecycleListener> getLifecycleListenerRegistrar() {
    return lifecycleListenerManager;
  }

  /**
   * Returns a copy of the list of documents managed by this class.
   */
  public JsonArray<Document> getDocuments() {
    return documents.copy();
  }

  public Document getDocumentByFileEditSessionKey(String fileEditSessionKey) {
    return documentsByFileEditSessionKey.get(fileEditSessionKey);
  }
  
  public void attachToEditor(final Document document, final Editor editor) {
    final Document oldDocument = editor.getDocument();
    if (oldDocument != null) {
      detachFromEditor(editor, oldDocument);
    }

    this.editor = editor;

    markAsActive(document);

    editor.setDocument(document);

    lifecycleListenerManager.dispatch(new Dispatcher<LifecycleListener>() {
      @Override
      public void dispatch(LifecycleListener listener) {
        listener.onDocumentOpened(document, editor);
      }
    });
  }

  private void detachFromEditor(final Editor editor, final Document document) {
    lifecycleListenerManager.dispatch(new Dispatcher<LifecycleListener>() {
      @Override
      public void dispatch(LifecycleListener listener) {
        listener.onDocumentClosed(document, editor);
      }
    });

    clearDocumentState(document);
  }

  /*
   * TODO: in the future, different features will remove
   * non-persistent stuff themselves. For now, clear everything.
   */
  private void clearDocumentState(final Document document) {
    for (Line line = document.getFirstLine(); line != null; line = line.getNextLine()) {
      line.clearTags();
    }
    
    // Column anchors exist on the line via a tag, so those get cleared above
    document.getAnchorManager().clearLineAnchors();
  }

  public void getDocument(PathUtil path, GetDocumentCallback callback) {
    if (fileTreeModel.getWorkspaceRoot() != null) {
      // FileTreeModel is populated so get the file edit session key for this path
      FileTreeNode node = fileTreeModel.getWorkspaceRoot().findChildNode(path);
      if (node != null && node.getFileEditSessionKey() != null) {
        String fileEditSessionKey = node.getFileEditSessionKey();
        Document document = documentsByFileEditSessionKey.get(fileEditSessionKey);
        if (document != null) {
          callback.onDocumentReceived(document);
          return;
        }
      }
    }

    networkController.load(path, callback);
    // handleEditableFileReceived will be called async
  }

  void handleEditableFileReceived(
      FileContents fileContents, JsonArray<GetDocumentCallback> callbacks) {

    /*
     * One last check to make sure we don't already have a Document for this
     * file
     */
    Document document = documentsByFileEditSessionKey.get(fileContents.getFileEditSessionKey());
    if (document == null) {
      document = createDocument(fileContents.getContents(), new PathUtil(fileContents.getPath()),
          fileContents.getFileEditSessionKey(), fileContents.getCcRevision(),
          fileContents.getConflicts(), fileContents.getConflictHandle(), fileContents);
      tryGarbageCollect();
    } else {
      /*
       * Ensure we have the latest path stashed in the metadata. One case where
       * this matters is if a file is renamed, we will have had the old path --
       * this logic will update its path.
       */
      DocumentMetadata.putPath(document, new PathUtil(fileContents.getPath()));
    }

    for (int i = 0, n = callbacks.size(); i < n; i++) {
      callbacks.get(i).onDocumentReceived(document);
    }
  }

  /**
   * @param conflicts only required for documents that are in a conflicted state
   * @param conflictHandle only required for documents that are in a conflicted state
   */
  private Document createDocument(String contents, PathUtil path, String fileEditSessionKey,
      int ccRevision, JsonArray<ConflictChunk> conflicts, ConflictHandle conflictHandle,
      final FileContents fileContents) {

    final Document document = Document.createFromString(contents);

    documents.add(document);

    boolean isLinkedToFile = fileEditSessionKey != null;
    if (isLinkedToFile) {
      documentsByFileEditSessionKey.put(fileEditSessionKey, document);
    }

    DocumentMetadata.putLinkedToFile(document, isLinkedToFile);
    DocumentMetadata.putPath(document, path);
    DocumentMetadata.putFileEditSessionKey(document, fileEditSessionKey);
    DocumentMetadata.putBeginCcRevision(document, ccRevision);
    DocumentMetadata.putConflicts(document, conflicts);
    DocumentMetadata.putConflictHandle(document, conflictHandle);
    
    lifecycleListenerManager.dispatch(new Dispatcher<LifecycleListener>() {
      @Override
      public void dispatch(LifecycleListener listener) {
        listener.onDocumentCreated(document);
      }
    });

    if (isLinkedToFile) {
      lifecycleListenerManager.dispatch(new Dispatcher<LifecycleListener>() {
        @Override
        public void dispatch(LifecycleListener listener) {
          listener.onDocumentLinkedToFile(document, fileContents);
        }
      });
    }

    // Save the fileEditSessionKey into the tree node.
    if (fileTreeModel.getWorkspaceRoot() != null) {
      FileTreeNode node = fileTreeModel.getWorkspaceRoot().findChildNode(path);
      if (node != null) {
        node.setFileEditSessionKey(fileEditSessionKey);
      }
    }

    return document;
  }

  private void markAsActive(Document document) {
    if (documents.peek() != document) {
      // Ensure it is at the top
      documents.remove(document);
      documents.add(document);
    }
  }

  private void tryGarbageCollect() {
    int removeCount = documents.size() - MAX_CACHED_DOCUMENTS;
    for (int i = 0; i < documents.size() && removeCount > 0;) {
      Document document = documents.get(i);

      boolean documentIsOpen = editor != null && editor.getDocument() == document;
      if (documentIsOpen) {
        i++;
        continue;
      }

      garbageCollectDocument(document);
      removeCount--;
    }
  }

  void garbageCollectDocument(final Document document) {
    if (DocumentMetadata.isLinkedToFile(document)) {
      unlinkFromFile(document);
    }

    documents.remove(document);

    lifecycleListenerManager.dispatch(new Dispatcher<LifecycleListener>() {
      @Override
      public void dispatch(LifecycleListener listener) {
        listener.onDocumentGarbageCollected(document);
      }
    });
  }

  public void unlinkFromFile(final Document document) {
    lifecycleListenerManager.dispatch(new Dispatcher<DocumentManager.LifecycleListener>() {
      @Override
      public void dispatch(LifecycleListener listener) {
        listener.onDocumentUnlinkingFromFile(document);
      }
    });

    documentsByFileEditSessionKey.remove(DocumentMetadata.getFileEditSessionKey(document));
    DocumentMetadata.putLinkedToFile(document, false);
  }

  Document getMostRecentlyActiveDocument() {
    return documents.peek();
  }

  /**
   * Returns a potentially empty list of pairs of a document and an editor.
   */
  JsonArray<Pair<Document, Editor>> getOpenDocuments() {
    JsonArray<Pair<Document, Editor>> result = JsonCollections.createArray();
    if (editor == null || editor.getDocument() == null) {
      return result;
    }

    /*
     * TODO: When there are more than one editor, this will not be
     * trivial
     */
    result.add(Pair.of(editor.getDocument(), editor));
    return result;
  }

  public Document getDocumentById(int documentId) {
    for (int i = 0, n = documents.size(); i < n; i++) {
      if (documents.get(i).getId() == documentId) {
        return documents.get(i);
      }
    }
    
    return null;
  }
}
