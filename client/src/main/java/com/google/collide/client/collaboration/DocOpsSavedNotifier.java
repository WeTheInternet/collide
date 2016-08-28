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

package com.google.collide.client.collaboration;

import java.util.List;

import com.google.collide.client.collaboration.FileConcurrencyController.DocOpListener;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.document.DocumentMetadata;
import com.google.collide.dto.DocOp;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonIntegerMap;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;
import com.google.common.base.Preconditions;

/**
 * A utility class to register for callbacks when all of the doc ops in a particular scope are
 * saved (the server has successfully received and applied them to the document.)
 */
public class DocOpsSavedNotifier {

  public abstract static class Callback {
    
    private final DocOpListener docOpListener = new DocOpListener() {
      @Override
      public void onDocOpAckReceived(int documentId, DocOp serverHistoryDocOp, boolean clean) {
        Integer remainingAcks = remainingAcksByDocumentId.get(documentId);
        if (remainingAcks == null) {
          // We have already reached our ack count for this document ID
          return;
        }
        
        remainingAcks--;
        if (remainingAcks == 0) {
          remainingAcksByDocumentId.erase(documentId);
          tryCallback();
        } else {
          remainingAcksByDocumentId.put(documentId, remainingAcks);
        }
      }

      @Override
      public void onDocOpSent(int documentId, List<DocOp> docOps) {
      }
    };
    
    private RemoverManager remover;
    private JsonIntegerMap<Integer> remainingAcksByDocumentId;
    
    public abstract void onAllDocOpsSaved();

    private void initialize(
        RemoverManager remover, JsonIntegerMap<Integer> remainingAcksByDocumentId) {
      this.remover = remover;
      this.remainingAcksByDocumentId = remainingAcksByDocumentId;
    }
    
    /**
     * Stops listening for the doc ops to be saved.
     */
    protected void cancel() {
      remover.remove();
      
      remover = null;
      remainingAcksByDocumentId = null;
    }

    private void tryCallback() {
      if (!isWaiting()) {
        // Only callback after all documents' doc ops have been acked
        cancel();
        onAllDocOpsSaved();
      }
    }
    
    boolean isWaiting() {
      return remainingAcksByDocumentId != null && !remainingAcksByDocumentId.isEmpty();
    }
  }

  private final DocumentManager documentManager;
  private final CollaborationManager collaborationManager;

  public DocOpsSavedNotifier(
      DocumentManager documentManager, CollaborationManager collaborationManager) {
    this.documentManager = documentManager;
    this.collaborationManager = collaborationManager;
  }
  
  /**
   * @see #notifyForFiles(Callback, String...)
   */
  public boolean notifyForWorkspace(Callback callback) {
    JsonArray<Document> documents = documentManager.getDocuments();
    int[] documentIds = new int[documents.size()];
    for (int i = 0; i < documentIds.length; i++) {
      documentIds[i] = documents.get(i).getId();
    }

    return notifyForDocuments(callback, documentIds);
  }

  /**
   * @see #notifyForDocuments(Callback, int...)
   */
  public boolean notifyForFiles(Callback callback, String... fileEditSessionKeys) {
    int[] documentIds = new int[fileEditSessionKeys.length];
    for (int i = 0; i < documentIds.length; i++) {
      Document document = documentManager.getDocumentByFileEditSessionKey(fileEditSessionKeys[i]);
      Preconditions.checkNotNull(document,
          "Document for given fileEditSessionKey [" + fileEditSessionKeys[i] + "] does not exist");

      documentIds[i] = document.getId();
    }

    return notifyForDocuments(callback, documentIds);
  }

  /**
   * @return whether we are waiting for unacked or queued doc ops
   */
  public boolean notifyForDocuments(Callback callback, int... documentIds) {
    RemoverManager remover = new RemoverManager();
    JsonIntegerMap<Integer> remainingAcksByDocumentId = JsonCollections.createIntegerMap();

    for (int i = 0; i < documentIds.length; i++) {
      int documentId = documentIds[i];

      if (!DocumentMetadata.isLinkedToFile(documentManager.getDocumentById(documentId))) {
        // Ignore unlinked files
        continue;
      }
      
      DocumentCollaborationController documentCollaborationController =
          collaborationManager.getDocumentCollaborationController(documentId);
      Preconditions.checkNotNull(documentCollaborationController,
          "Could not find collaboration controller document ID [" + documentId + "]");

      FileConcurrencyController fileConcurrencyController =
          documentCollaborationController.getFileConcurrencyController();
      int remainingAcks = computeRemainingAcks(fileConcurrencyController);
      if (remainingAcks > 0) {
        remainingAcksByDocumentId.put(documentId, remainingAcks);
        remover.track(
            fileConcurrencyController.getDocOpListenerRegistrar().add(callback.docOpListener));
      }
    }
    
    callback.initialize(remover, remainingAcksByDocumentId);
    
    // If there aren't any unacked or queued doc ops, this will callback immediately
    callback.tryCallback();
    
    return callback.isWaiting();
  }

  private static int computeRemainingAcks(FileConcurrencyController fileConcurrencyController) {
    return fileConcurrencyController.getUnackedClientOpCount()
        + fileConcurrencyController.getQueuedClientOpCount();
  }
}
