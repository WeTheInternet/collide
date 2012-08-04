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

package com.google.collide.server.documents;

import java.io.IOException;
import java.util.List;

import com.google.collide.dto.ClientToServerDocOp;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.server.documents.VersionedDocument.DocumentOperationException;
import com.google.collide.server.documents.VersionedDocument.VersionedText;
import com.google.collide.server.shared.merge.ConflictChunk;

/**
 * A single workspace file, backed by a {@link VersionedDocument}. Its contents
 * can be accessed as plain text. However, it can be mutated <em>only</em> via
 * the application of doc ops passed to
 * {@link FileEditSession#consume(List, String, int, DocumentSelection)}.
 *
 */
public interface FileEditSession extends ImmutableFileEditSession {

  public class FileEditSessionClosedException extends IllegalStateException {
    public FileEditSessionClosedException(String resourceId, long closedTimeMs) {
      super(String.format(
          "Operation not allowed on closed FileEditSession [%s], closed [%d] ms ago", resourceId,
          System.currentTimeMillis() - closedTimeMs));
    }
  }

  public interface VersionedTextAndConflictChunks {
    VersionedText getVersionedText();

    List<? extends ConflictChunk> getConflictChunks();
  }

  interface OnCloseListener {
    void onClosed();
  }

  /**
   * Releases the resources for this {@code FileEditSession}. Many methods will
   * throw IllegalStateException if called on a closed session object. However,
   * this method can return before outstanding calls to those methods finish
   * executing.
   */
  void close();

  /**
   * Sets a listener that is called when this file edit session is closed.
   * Only one listener can be set.
   *
   * @param onCloseListener listener to call when file edit session is closed.
   * @throws IllegalStateException if there is already a listener attached
   */
  void setOnCloseListener(OnCloseListener onCloseListener);

  /**
   * Applies a list of doc ops to the backing document.
   *
   * @param docOps the list of doc ops being applied
   * @param authorClientId clientId who sent the doc ops
   * @param intendedCcRevision the revision of the document that the doc ops are
   *        intended to be applied to
   * @param selection see {@link ClientToServerDocOp#getSelection()}
   * @return the result of the consume operation
   * @throws DocumentOperationException if there was a problem with consuming
   *         the document operation
   */
  VersionedDocument.ConsumeResult consume(List<DocOp> docOps, String authorClientId,
      int intendedCcRevision, DocumentSelection selection) throws DocumentOperationException;

  VersionedDocument getDocument();

  /**
   * Saves the file.
   * @throws IOException 
   */
  void save(String currentPath) throws IOException;

  /**
   * @return the text and any conflict chunks, along with a revision
   */
  VersionedTextAndConflictChunks getContentsAndConflictChunks();

  List<ConflictChunk> getConflictChunks();

  /**
   * @return true if the conflict chunk is now resolved. false if the chunk was already resolved
   *         (e.g., a collaborator raced for the resolution and won).
   * @throws IOException 
   */
  boolean resolveConflictChunk(int chunkIndex) throws IOException;
}
