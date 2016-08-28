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

package com.google.collide.client.filehistory;

import com.google.collide.client.AppContext;
import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.communication.FrontendApi;
import com.google.collide.client.diff.EditorDiffContainer;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.logging.Log;
import com.google.collide.clientlibs.model.Workspace;
import com.google.collide.dto.DiffChunkResponse;
import com.google.collide.dto.DiffChunkResponse.DiffType;
import com.google.collide.dto.FileContents;
import com.google.collide.dto.GetFileRevisions;
import com.google.collide.dto.GetFileRevisionsResponse;
import com.google.collide.dto.Revision;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls.DiffChunkResponseImpl;
import com.google.collide.dto.client.DtoClientImpls.GetFileRevisionsImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.common.base.Preconditions;

/**
 * Handles queries from the file history classes for getting revisions and diffs
 * of those revisions
 */
class FileHistoryApi {

  private final AppContext appContext;

  private final EditorDiffContainer editorDiffContainer;
  private final Timeline timeline;
  private final DocumentManager documentManager;
  private Workspace workspace;

  FileHistoryApi(AppContext appContext, EditorDiffContainer editorDiffContainer,
      Timeline timeline, DocumentManager documentManager) {
    this.appContext = appContext;
    this.editorDiffContainer = editorDiffContainer;
    this.timeline = timeline;
    this.documentManager = documentManager;
  }

  void setWorkspace(Workspace workspace) {
    // Save workspace info. Time line asks server for revisions and workspace info. When revision
    // info comes after workspace info, the saved workspace info is used to properly set revision
    // tooltips.
    this.workspace = workspace;
    this.timeline.updateNodeTooltips();
  }

  Workspace getWorkspace() {
    return workspace;
  }

  void getFileRevisions(PathUtil path, String pathRootId) {
    Preconditions.checkNotNull(pathRootId);

    GetFileRevisionsImpl message = GetFileRevisionsImpl.make()
        .setClientId(BootstrapSession.getBootstrapSession().getActiveClientId())
        .setPathRootId(pathRootId)
        .setPath(path.getPathString())
        .setNumOfRevisions(timeline.maxNumberOfNodes())
        .setFiltering(true)
        .setIncludeBranchRevision(true)
        .setIncludeMostRecentRevision(true);
    getFileRevisions(message);
  }

  void clearDiffEditors() {
    editorDiffContainer.clearDiffEditors();
  }

  /**
   * Fetch a list of revisions for the given file, and call Timeline's drawNodes
   */
  private void getFileRevisions(final GetFileRevisions message) {
//    appContext.getStatusManager(). ("Getting file revisions");
    appContext.getFrontendApi().GET_FILE_REVISIONS
    .send(message, 
        new FrontendApi.ApiCallback<GetFileRevisionsResponse>() {
      @Override
      public void onFail(FailureReason reason) {
        Log.warn(getClass(), "Call to get revisions for file failed.");
      }

      @Override
      public void onMessageReceived(GetFileRevisionsResponse message) {
        // Render timeline with newly fetched revisions
        timeline.drawNodes((JsoArray<Revision>) message.getRevisions());
      }
    });
  }

  /**
   * Fetch the diff of the files for the given revisions and set
   * editorDiffContainer split-pane contents to be the current file before and
   * after snapshots
   */
  void setFile(final PathUtil path, final Revision beforeRevision, final Revision afterRevision) {
    if (beforeRevision == null) {
      throw new IllegalArgumentException("before revision can not be null");
    }

    if (editorDiffContainer.hasRevisions(beforeRevision, afterRevision)) {
      // We already had the diff.
      return;
    }

    editorDiffContainer.setExpectedRevisions(beforeRevision, afterRevision);
    final int scrollTop = editorDiffContainer.getScrollTop();

    // TODO: This use to call out to an API to receive the file diff,
    // we can revist this stuff later, it just called set diff chunks
  }

  /**
   * Gets the file from document manager and sets the left and right panels to the same file.
   */
  void setUnchangedFile(final PathUtil path) {
    timeline.setDiffFilePaths(path.getPathString(), path.getPathString());
    editorDiffContainer.setExpectedRevisions(
        EditorDiffContainer.UNKNOWN_REVISION, EditorDiffContainer.UNKNOWN_REVISION);

    documentManager.getDocument(path, new DocumentManager.GetDocumentCallback() {
        @Override
      public void onUneditableFileContentsReceived(FileContents contents) {
        // TODO handle images here.
      }

        @Override
      public void onFileNotFoundReceived() {
        Log.warn(getClass(), "Call to get file " + path.getPathString() + " failed.");
      }

        @Override
      public void onDocumentReceived(Document document) {
        // editable file. construct unchanged diff chunk here.
        String text = document.asText();
        DiffChunkResponseImpl unchangedDiffChunk = DiffChunkResponseImpl.make()
            .setBeforeData(text).setAfterData(text).setDiffType(DiffType.UNCHANGED);
        JsonArray<DiffChunkResponse> diffChunks = JsoArray.create();
        diffChunks.add(unchangedDiffChunk);
        editorDiffContainer.setDiffChunks(path, diffChunks, EditorDiffContainer.UNKNOWN_REVISION,
            EditorDiffContainer.UNKNOWN_REVISION);
      }
    });
  }

}
