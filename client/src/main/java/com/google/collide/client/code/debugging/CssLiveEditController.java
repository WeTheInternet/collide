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

package com.google.collide.client.code.debugging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.collide.client.code.debugging.DebuggerApiTypes.CssStyleSheetHeader;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnAllCssStyleSheetsResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnEvaluateExpressionResponse;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.util.DeferredCommandExecutor;
import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.FileContents;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;
import com.google.common.base.Preconditions;

/**
 * Controller for live edits of CSS files during debugging.
 *
 * <p>Schedules propagation of CSS edits to the debugger.
 *
 */
class CssLiveEditController {

  /**
   * Bean containing information about document binding and update status.
   */
  private static class DocumentInfo {
    @Nullable
    private Document document;

    @Nonnull
    private final String styleSheetId;

    @Nonnull
    private final PathUtil path;

    /**
     * Indicator that file has changed, but changes hasn't been sent to debugger.
     */
    private boolean dirty;

    private DocumentInfo(@Nonnull String styleSheetId, @Nonnull PathUtil path) {
      Preconditions.checkNotNull(styleSheetId);
      Preconditions.checkNotNull(path);
      this.path = path;
      this.styleSheetId = styleSheetId;
    }
  }

  private final DocumentManager documentManager;
  private final DebuggerState debuggerState;
  private final EventsListenerImpl eventsListener;

  /**
   * Executor that sends content of marked documents to debugger
   * and then clears marks.
   */
  private final DeferredCommandExecutor updatesApplier = new DeferredCommandExecutor(100) {
    @Override
    protected boolean execute() {
      if (debuggerState.isActive()) {
        for (DocumentInfo documentInfo : trackedDocuments.asIterable()) {
          if (documentInfo.dirty) {
            documentInfo.dirty = false;
            Document document = Preconditions.checkNotNull(documentInfo.document);
            String styleSheetId = Preconditions.checkNotNull(documentInfo.styleSheetId);
            debuggerState.setStyleSheetText(styleSheetId, document.asText());
          }
        }
      }
      return false;
    }
  };

  // TODO: Can Chrome notify us when list changes?
  /**
   * Executor that periodically requests list of used CSS from debugger.
   */
  private final DeferredCommandExecutor trackListUpdater = new DeferredCommandExecutor(100) {
    @Override
    protected boolean execute() {
      if (debuggerState.isActive()) {
        debuggerState.requestAllCssStyleSheets();
      }
      return true;
    }
  };

  /**
   * List of documents that are currently tracked.
   */
  private final JsonArray<DocumentInfo> trackedDocuments = JsonCollections.createArray();

  private final RemoverManager removerManager = new RemoverManager();

  private class EventsListenerImpl implements
      DebuggerState.DebuggerStateListener,
      DebuggerState.CssListener,
      DebuggerState.EvaluateExpressionListener {

    @Override
    public void onAllCssStyleSheetsResponse(OnAllCssStyleSheetsResponse response) {
      handleOnAllCssStyleSheetsResponse(response);
    }

    @Override
    public void onDebuggerStateChange() {
      updateLiveEditListenerState();
    }

    @Override
    public void onEvaluateExpressionResponse(OnEvaluateExpressionResponse response) {
    }

    @Override
    public void onGlobalObjectChanged() {
      // Invalidate the cached stylesheet ID.
      updateLiveEditListenerState();
    }
  }

  /**
   * Marks specified document for update and schedules update is necessary.
   */
  private void scheduleUpdate(@Nonnull DocumentInfo documentInfo) {
    Preconditions.checkNotNull(documentInfo);
    if (!debuggerState.isActive()) {
      return;
    }
    Preconditions.checkNotNull(documentInfo.document);
    if (documentInfo.dirty) {
      return;
    }
    documentInfo.dirty = true;
    if (!updatesApplier.isScheduled()) {
      updatesApplier.schedule(1);
    }
  }

  CssLiveEditController(DebuggerState debuggerState, DocumentManager documentManager) {
    this.debuggerState = debuggerState;
    this.documentManager = documentManager;
    this.eventsListener = new EventsListenerImpl();
    debuggerState.getDebuggerStateListenerRegistrar().add(eventsListener);
    debuggerState.getCssListenerRegistrar().add(eventsListener);
    debuggerState.getEvaluateExpressionListenerRegistrar().add(eventsListener);
  }

  private void updateLiveEditListenerState() {
    untrackAll();
    trackListUpdater.cancel();

    if (debuggerState.isActive()) {
      trackListUpdater.schedule(1);
    }
  }

  /**
   * Registers all CSS paths / styleSheetIds that are in use.
   */
  private void handleOnAllCssStyleSheetsResponse(OnAllCssStyleSheetsResponse response) {
    if (!debuggerState.isActive()) {
      return;
    }

    JsonArray<DocumentInfo> freshList = buildDocumentInfoList(response);
    if (checkForEquality(freshList, trackedDocuments)) {
      return;
    }

    untrackAll();
    for (final DocumentInfo documentInfo : freshList.asIterable()) {
      trackedDocuments.add(documentInfo);
      documentManager.getDocument(documentInfo.path, new DocumentManager.GetDocumentCallback() {
        @Override
        public void onDocumentReceived(Document document) {
          // This method can be called asynchronously. If item is not in list
          // then ignore this notification. "equals" is not overridden, so
          // "contains" reports that list contains given reference.
          if (!trackedDocuments.contains(documentInfo)) {
            return;
          }
          documentInfo.document = document;
          removerManager.track(document.getTextListenerRegistrar().add(new Document.TextListener() {
            @Override
            public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
              scheduleUpdate(documentInfo);
            }
          }));
          // Schedule update, because debugger can have stale version in use.
          scheduleUpdate(documentInfo);
        }

        @Override
        public void onUneditableFileContentsReceived(FileContents contents) {
          // TODO: Well, is it unmodifiable at all, or only for us?
        }

        @Override
        public void onFileNotFoundReceived() {
          // Do nothing.
        }
      });
    }
  }

  /**
   * Checks equality of two track-lists.
   *
   * <p>Track lists are supposed to be equal if they contain the same set
   * of (path, styleSheetId) pairs.
   *
   * <p>Note: currently we suggest, that list is ordered somehow, so pairs have
   * matching positions.
   */
  private boolean checkForEquality(JsonArray<DocumentInfo> list1, JsonArray<DocumentInfo> list2) {
    if (list1.size() != list2.size()) {
      return false;
    }
    for (int i = 0, n = list1.size(); i < n; i++) {
      DocumentInfo item1 = list1.get(i);
      DocumentInfo item2 = list2.get(i);
      if (!item1.styleSheetId.equals(item2.styleSheetId)) {
        return false;
      }
      if (!item1.path.equals(item2.path)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Builds track-list from debugger response.
   *
   * <p>Only ".css" files are accepted.
   *
   * <p>Note: For some items URL looks like "data:...".
   * In that case {@link PathUtil} is {@code null}.
   */
  private JsonArray<DocumentInfo> buildDocumentInfoList(OnAllCssStyleSheetsResponse response) {
    JsonArray<DocumentInfo> result = JsonCollections.createArray();

    JsonArray<CssStyleSheetHeader> headers = response.getHeaders();
    for (CssStyleSheetHeader header : headers.asIterable()) {
      String url = header.getUrl();
      PathUtil path = debuggerState.getSourceMapping().getLocalSourcePath(url);
      if (path == null) {
        continue;
      }
      if (!path.getBaseName().endsWith(".css")) {
        continue;
      }
      String styleSheetId = header.getId();
      DocumentInfo docInfo = new DocumentInfo(styleSheetId, path);
      result.add(docInfo);
    }
    return result;
  }

  /**
   * Unregisters text-change listeners, stops appropriate executors and
   * cleans track list.
   */
  private void untrackAll() {
    removerManager.remove();
    trackedDocuments.clear();
    updatesApplier.cancel();
  }
}
