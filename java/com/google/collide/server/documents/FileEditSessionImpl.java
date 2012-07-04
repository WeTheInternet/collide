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

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.server.documents.VersionedDocument.DocumentOperationException;
import com.google.collide.server.documents.VersionedDocument.VersionedText;
import com.google.collide.server.shared.merge.ConflictChunk;
import com.google.collide.server.shared.merge.MergeChunk;
import com.google.collide.server.shared.merge.MergeResult;
import com.google.collide.server.shared.util.FileHasher;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.Anchor.ShiftListener;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.InsertionPlacementStrategy;
import com.google.collide.shared.ot.DocOpUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.protobuf.ByteString;

import org.vertx.java.core.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Default implementation of {@link FileEditSession}.
 * 
 * <p>
 * This class is thread-safe.
 * 
 */
final class FileEditSessionImpl implements FileEditSession {

  /**
   * Bundles together a snapshot of the text of this file with any conflict chunks.
   */
  private static class VersionedTextAndConflictChunksImpl
      implements
        VersionedTextAndConflictChunks {
    private final VersionedText text;
    private final List<AnchoredConflictChunk> conflictChunks;

    VersionedTextAndConflictChunksImpl(
        VersionedText text, List<AnchoredConflictChunk> conflictChunks) {
      this.text = text;
      this.conflictChunks = conflictChunks;
    }

    @Override
    public VersionedText getVersionedText() {
      return text;
    }

    @Override
    public List<AnchoredConflictChunk> getConflictChunks() {
      return conflictChunks;
    }
  }

  private static class AnchoredConflictChunk extends ConflictChunk {

    private static final AnchorType CONFLICT_CHUNK_START_LINE =
        AnchorType.create(FileEditSessionImpl.class, "conflictChunkStart");
    private static final AnchorType CONFLICT_CHUNK_END_LINE =
        AnchorType.create(FileEditSessionImpl.class, "conflictChunkEnd");

    public final Anchor startLineAnchor;
    public final Anchor endLineAnchor;

    public AnchoredConflictChunk(ConflictChunk chunk, VersionedDocument doc) {
      super(chunk, chunk.isResolved());

      // Add anchors at the conflict regions' boundaries, so their position/size
      // gets adjusted automatically as the user enters text in and around them.
      startLineAnchor = doc.addAnchor(
          CONFLICT_CHUNK_START_LINE, chunk.getStartLine(), AnchorManager.IGNORE_COLUMN);
      startLineAnchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
      startLineAnchor.getShiftListenerRegistrar().add(new ShiftListener() {
          @Override
        public void onAnchorShifted(Anchor anchor) {
          setStartLine(anchor.getLineNumber());
        }
      });
      endLineAnchor =
          doc.addAnchor(CONFLICT_CHUNK_END_LINE, chunk.getEndLine(), AnchorManager.IGNORE_COLUMN);
      endLineAnchor.setInsertionPlacementStrategy(InsertionPlacementStrategy.LATER);
      endLineAnchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
      endLineAnchor.getShiftListenerRegistrar().add(new ShiftListener() {
          @Override
        public void onAnchorShifted(Anchor anchor) {
          setEndLine(anchor.getLineNumber());
        }
      });
    }
  }

  /**
   * Given a merge result from the originally conflicted state, construct conflict chunks for it.
   */
  private static List<ConflictChunk> constructConflictChunks(MergeResult mergeResult) {
    List<ConflictChunk> conflicts = Lists.newArrayList();
    for (MergeChunk mergeChunk : mergeResult.getMergeChunks()) {
      if (mergeChunk.hasConflict()) {
        conflicts.add(new ConflictChunk(mergeChunk));
      }
    }
    return conflicts;
  }

  /**
   * Document that contains the file contents.
   */
  private VersionedDocument contents;

  /** The list of conflict chunks for this file. */
  private final List<AnchoredConflictChunk> conflictChunks = Lists.newArrayList();

  /*
   * The size and sha1 fields don't actually need to stay in lock-step with the doc contents since
   * there's no public API for retrieving a snapshot of both values. Thus, we don't need blocking
   * synchronization. We do however need to ensure that updates made by one thread are seen by other
   * threads, so they must be declared volatile.
   */

  /** Size of the file, in bytes. Lazily computed by {@link #getSize()}. */
  private Integer size = null;

  /** SHA-1 hash of the file contents. Lazily computed by {@link #getSha1()}. */
  private ByteString sha1 = null;

  /** CC revision of the document that we last saved */
  private int lastSavedCcRevision;

  /** CC revision of the document after the last mutation was applied */
  private int lastMutationCcRevision;

  /** True if the file-edit session has been closed */
  private boolean closed = false;

  /** When this file edit session was closed. Makes sense only if closed = true. */
  private long closedTimeMs;

  /** Time that this FileEditSession was created (millis since epoch) */
  private final long createdAt = System.currentTimeMillis();

  private OnCloseListener onCloseListener;

  /** The ID of the resource this edit session is opened for. */
  private final String resourceId;

  private final Logger logger;

  protected String lastSavedPath;

  /**
   * Constructs a {@link FileEditSessionImpl} for a file.
   * 
   * @param resourceId the identifier for the resource we are editing.
   * @param initialContents the initial contents of the file
   * @param mergeResult if non-null the merge info related to the out of date
   */
  FileEditSessionImpl(String resourceId, String path, String initialContents,
      @Nullable MergeResult mergeResult, Logger logger) {
    this.resourceId = resourceId;
    this.lastSavedPath = path;
    this.logger = logger;
    this.contents = new VersionedDocument(initialContents, logger);

    if (mergeResult != null) {
      // Construct conflict chunks.
      List<ConflictChunk> chunks = constructConflictChunks(mergeResult);
      this.contents = new VersionedDocument(mergeResult.getMergedText(), logger);
      if (chunks.size() == 0) {
        logger.error(String.format("Non-null MergeResult passed to FileEditSession for file that"
            + " should have merged cleanly: [%s]", this));
      }

      for (ConflictChunk chunk : chunks) {
        this.conflictChunks.add(new AnchoredConflictChunk(chunk, contents));
      }
    }

    this.lastSavedCcRevision = contents.getCcRevision();
    this.lastMutationCcRevision = 0;

    logger.debug(String.format("FileEditSession [%s] was created at [%d]", this, createdAt));
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      if (!closed) {
        logger.warn(
            String.format("FileEditSession [%s] finalized without being closed first", this));
        close();
      }
    } catch (Throwable thrown) {
      logger.error(
          String.format("Uncaught Throwable in FileEditSessionImpl.finalize of [%s]", this),
          thrown);
    } finally {
      super.finalize();
    }
  }

  private void checkNotClosed() {
    if (closed) {
      throw new FileEditSessionClosedException(resourceId, closedTimeMs);
    }
  }

  @Override
  public void close() {
    // if already closed, do nothing and silently return
    if (!closed) {
      closed = true;
      return;
    }
    closedTimeMs = System.currentTimeMillis();

    // TODO: Maybe change the semantics of this method to block until
    // all outstanding calls to other methods guarded by checkNotClosed()
    // finish. IncrementableCountDownLatch would do the trick.

    if (hasChanges()) {
      logger.warn(String.format("FileEditSession [%s] closed while dirty", this));
    }

    if (onCloseListener != null) {
      onCloseListener.onClosed();
    }
  }

  @Override
  public synchronized void setOnCloseListener(OnCloseListener listener) {
    if (this.onCloseListener != null) {
      throw new IllegalStateException("One listener already registered.");
    }
    this.onCloseListener = listener;
  }

  @Override
  public VersionedDocument.ConsumeResult consume(List<DocOp> docOps, String authorClientId,
      int intendedCcRevision, DocumentSelection selection) throws DocumentOperationException {

    checkNotClosed();

    boolean containsMutation = DocOpUtils.containsMutation(docOps);

    VersionedDocument.ConsumeResult result =
        contents.consume(docOps, authorClientId, intendedCcRevision, selection);

    if (containsMutation) {
      lastMutationCcRevision = contents.getCcRevision();

      // Reset the cached size and SHA-1. We'll wait until someone actually calls getSize() or
      // getSha1() to recompute them.
      size = null;
      sha1 = null;
    }
    return result;
  }

  private String getText() {
    return contents.asText().text;
  }

  @Override
  public String getContents() {
    checkNotClosed();   
    return getText();
  }

  public int getCcRevision() {
    return lastMutationCcRevision;
  }
  
  @Override
  public int getSize() {
    checkNotClosed();

    if (size == null) {
      try {
        size = getText().getBytes("UTF-8").length;
      } catch (UnsupportedEncodingException e) {
        // UTF-8 is a charset required by Java spec, per javadoc of
        // java.nio.charset.Charset, so this can't happen.
        throw new RuntimeException("UTF-8 not supported in this JVM?!", e);
      }
    }
    return size;
  }

  @Override
  public ByteString getSha1() {
    checkNotClosed();

    if (sha1 == null) {
      sha1 = FileHasher.getSha1(getText());
    }
    return sha1;
  }

  @Override
  public VersionedDocument getDocument() {
    checkNotClosed();
    return contents;
  }

  @Override
  public String getFileEditSessionKey() {
    // probably ok to call on a closed FileEditSession
    return resourceId;
  }

  @Override
  public boolean hasChanges() {
    return lastSavedCcRevision < lastMutationCcRevision;
  }


  @Override
  public void save(String currentPath) throws IOException {
    checkNotClosed();

    if (currentPath == null) {
      logger.fatal(String.format("We do do not know the path for edit session [%s]!", this));
      return;
    }

    // Get a consistent snapshot of the raw text and conflict chunks
    VersionedTextAndConflictChunksImpl snapshot = getContentsAndConflictChunks();
    String text = snapshot.getVersionedText().text;
    List<AnchoredConflictChunk> conflictChunks = snapshot.getConflictChunks();

    if (hasUnresolvedConflictChunks(conflictChunks)) {
      // TODO: There are conflict chunks in this file that need resolving.
      saveConflictChunks(currentPath, text, conflictChunks);
    } else {

      // Remove all conflict chunk anchors from the document
      for (AnchoredConflictChunk conflict : conflictChunks) {
        contents.removeAnchor(conflict.startLineAnchor);
        contents.removeAnchor(conflict.endLineAnchor);
      }
      conflictChunks.clear();
    }

    saveChanges(currentPath, text);

    lastSavedCcRevision = snapshot.getVersionedText().ccRevision;
    lastSavedPath = currentPath;
    logger.debug(String.format("Saved file [%s]", this));
  }

  private void saveChanges(String path, String text) throws IOException {
    /*
     * TODO: what we really should do is track lastModified. Then we can lock,
     * check the lastModified, and merge in any local FS changes that happened
     * since we last saved.
     * 
     * We should also listen on "documents.fileSystemEvents" for to get
     * notified instantly when the FS version changes, so we can eagerly apply
     * the delta and push to clients. 
     */
    logger.debug(String.format("Saving file [%s]", path));
    
    File file = new File(path);
    Files.write(text, file, Charsets.UTF_8);
  }

  private void saveConflictChunks(
      String path, String text, List<AnchoredConflictChunk> conflictChunks) {
    // TODO: Write the conflict chunks to some out of band location.
  }

  @Override
  public List<ConflictChunk> getConflictChunks() {
    return Lists.newArrayList((Iterable<? extends ConflictChunk>) conflictChunks);
  }

  @Override
  public VersionedTextAndConflictChunksImpl getContentsAndConflictChunks() {
    checkNotClosed();

    return new VersionedTextAndConflictChunksImpl(contents.asText(), Lists.newArrayList(
        conflictChunks));
  }

  @Override
  public boolean resolveConflictChunk(int chunkIndex) throws IOException {
    checkNotClosed();

    AnchoredConflictChunk chunk = conflictChunks.get(chunkIndex);
    if (chunk.isResolved()) {
      /*
       * This chunk can't be resolved because it is already resolved. This can happen if another
       * collaborator resolved the chunk, but this client did not get the notification until they
       * sent their own resolve message.
       */
      return false;
    }
    chunk.markResolved(true);

    logger.debug(String.format("Resolved conflict #%d in file [%s]", chunkIndex, this));

    /*
     * Immediately save the file.
     * 
     * TODO: how to store chunk resolution?
     */
    // TODO: Resolve path prior to calling save.
    save(getSavedPath());
    return true;
  }

  @Override
  public boolean hasUnresolvedConflictChunks() {
    return hasUnresolvedConflictChunks(getConflictChunks());
  }

  private static boolean hasUnresolvedConflictChunks(List<? extends ConflictChunk> conflictChunks) {
    for (ConflictChunk conflict : conflictChunks) {
      if (!conflict.isResolved()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return resourceId;
  }

  @Override
  public String getSavedPath() {
    return lastSavedPath;
  }
}
