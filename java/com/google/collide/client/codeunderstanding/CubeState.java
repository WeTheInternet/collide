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

package com.google.collide.client.codeunderstanding;

import static com.google.collide.shared.util.StringUtils.isNullOrEmpty;

import com.google.collide.client.communication.FrontendApi;
import com.google.collide.client.util.DeferredCommandExecutor;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.CodeGraph;
import com.google.collide.dto.CodeGraphFreshness;
import com.google.collide.dto.CodeGraphRequest;
import com.google.collide.dto.CodeGraphResponse;
import com.google.collide.dto.CodeReferences;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls.CodeBlockImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphFreshnessImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphRequestImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeReferencesImpl;
import com.google.collide.json.client.Jso;
import com.google.common.base.Preconditions;

/**
 * An object that holds the current state of communication with Cube-service.
 *
 * <p>Request to service are sequenced and "collapsed".
 * It means that newer request is hold until the previous response comes,
 * and that if there are several new requests, only the last of them
 * survives.
 *
 * <p>Also this class is responsible for merging updates coming from the server.
 *
 */
public class CubeState implements FrontendApi.ApiCallback<CodeGraphResponse> {

  /**
   * An interface of a callback that is called when appropriate update
   * is received.
   */
  interface CubeResponseDistributor {

    /**
     * Notifies instances interested in Cube data.
     *
     * @param updates indicates which data has been changed
     */
    void notifyListeners(CubeDataUpdates updates);
  }

  /**
   * A command that gently asks outer object to perform "retry" action
   * after specified timeout.
   *
   * <p>The command itself is executed once a second to be able to be
   * dismissed.
   *
   * <p>When time is elapsed command calls outer object
   * {@link CubeState#retryIfReady()} each iteration until it
   * responds that action is taken.
   */
  private class RetryExecutor extends DeferredCommandExecutor {

    protected RetryExecutor() {
      super(1000);
    }

    @Override
    protected boolean execute() {
      return !retryIfReady();
    }
  }

  /**
   * Cube API.
   */
  private final FrontendApi.RequestResponseApi<CodeGraphRequest, CodeGraphResponse> api;

  /**
   * Last (merged) Cube data.
   */
  private CubeData data;

  /**
   * Actual merged freshness of data.
   */
  private CodeGraphFreshness freshness;

  /**
   * File path to be requested on next request.
   */
  private String activeFilePath;

  /**
   * File path in last response.
   *
   * <p>If filePath to be requested is not the same as in last response -
   * we reset fileTree and it's freshness.
   */
  private String lastResponseFilePath;

  /**
   * File path in last request for which he hasn't got response yet.
   */
  private String requestedFilePath;

  /**
   * Flag indicating that we should make a new request after
   * we receive response.
   */
  private boolean deferredRefresh;

  /**
   * Indicates that we should not further do more requests / process responses.
   */
  private boolean isDismissed;

  /**
   * Instance that distributes data to consumers.
   */
  private final CubeResponseDistributor distributor;

  /**
   * Number of times the retry command has been consequently (re)scheduled.
   */
  private int retryRound;

  /**
   * Retry command executor.
   */
  private final RetryExecutor retryExecutor = new RetryExecutor();

  public CubeState(FrontendApi.RequestResponseApi<CodeGraphRequest, CodeGraphResponse> api,
      CubeResponseDistributor distributor) {
    this.api = api;
    this.distributor = distributor;
    data = CubeData.EMPTY_DATA;

    freshness = CodeGraphFreshnessImpl.make()
        .setFullGraph("0")
        .setLibsSubgraph("0")
        .setWorkspaceTree("0")
        .setFileTree("0")
        .setFileReferences("0");
  }

  /**
   * Prevents further instance activity.
   */
  public void dismiss() {
    isDismissed = true;
    retryExecutor.cancel();
  }

  public CubeData getData() {
    return data;
  }

  /**
   * Makes a next request if there is a deferred one or
   * schedule retry if required.
   *
   * <p>This method must be called after network response or
   * failure is processed to schedule next network activity.   
   */
  private void processDeferredActions() {
    requestedFilePath = null;
    if (deferredRefresh) {
      deferredRefresh = false;
      refresh();
    } else if (retryRound > 0) {
      // We should schedule retry if hasn't done it yet.
      if (!retryExecutor.isScheduled()) {
        retryRound++;
        retryExecutor.schedule(2 + retryRound);
      }
    }
  }

  /**
   * Enqueues or collapses request to Cube-service.
   */
  public void refresh() {
    if (isDismissed) {
      return;
    }

    if (activeFilePath == null) {
      return;
    }

    // Refresh is already deferred.
    if (deferredRefresh) {
      return;
    }

    // Waiting for response of equal request.
    if (activeFilePath.equals(requestedFilePath)) {
      return;
    }

    boolean activeIsCodeFilePath = checkFilePathIsCodeFile(activeFilePath);

    // Will send request after response come.
    if (requestedFilePath != null) {
      // do not defer if we do not need fileTree
      if (activeIsCodeFilePath) {
        deferredRefresh = true;
      }
      return;
    }


    // Else reset fileTree, if needed.
    if (!activeFilePath.equals(lastResponseFilePath) || !activeIsCodeFilePath) {
      resetContextFileData();
    }

    String filePathToRequest = activeIsCodeFilePath ? activeFilePath : null;
    // And send request.
    CodeGraphRequest request = CodeGraphRequestImpl.make()
        .setFreshness(freshness)
        .setFilePath(filePathToRequest);
    api.send(request, this);
    requestedFilePath = activeFilePath;
  }

  /**
   * Forgets stale fileTree data and freshness.
   */
  private void resetContextFileData() {
    freshness = CodeGraphFreshnessImpl.make()
        .setLibsSubgraph(freshness.getLibsSubgraph())
        .setFileTree("0")
        .setFullGraph(freshness.getFullGraph())
        .setWorkspaceTree(freshness.getWorkspaceTree())
        .setFileReferences("0");
    data = new CubeData(activeFilePath, null, data.getFullGraph(), data.getLibsSubgraph(),
        data.getWorkspaceTree(), null);
  }

  /**
   * Merges fresh server data with stored one and notifies consumers.
   *
   * @param message fresh Cube data.
   */
  @Override
  public void onMessageReceived(CodeGraphResponse message) {
    if (isDismissed) {
      Log.debug(getClass(), "Ignored CUBE response");
      return;
    }

    retryRound = 0;

    retryExecutor.cancel();

    boolean requestedIsCode = checkFilePathIsCodeFile(requestedFilePath);

    CodeGraphFreshnessImpl merged = CodeGraphFreshnessImpl.make();
    CodeGraphFreshness serverFreshness = message.getFreshness();

    CodeGraph libsSubgraph = data.getLibsSubgraph();
    merged.setLibsSubgraph(this.freshness.getLibsSubgraph());
    boolean libsSubgraphUpdated = false;

    CodeBlock fileTree = data.getFileTree();
    merged.setFileTree(this.freshness.getFileTree());
    merged.setFileTreeHash(this.freshness.getFileTreeHash());
    boolean fileTreeUpdated = false;

    CodeGraph workspaceTree = data.getWorkspaceTree();
    merged.setWorkspaceTree(this.freshness.getWorkspaceTree());
    boolean workspaceTreeUpdated = false;

    CodeGraph fullGraph = data.getFullGraph();
    merged.setFullGraph(this.freshness.getFullGraph());
    boolean fullGraphUpdated = false;

    CodeReferences fileReferences = data.getFileReferences();
    merged.setFileReferences(this.freshness.getFileReferences());
    boolean fileReferencesUpdated = false;

    if (!isNullOrEmpty(message.getLibsSubgraphJson())
        && compareFreshness(serverFreshness.getLibsSubgraph(), merged.getLibsSubgraph()) > 0) {
      libsSubgraph = Jso.<CodeGraphImpl>deserialize(message.getLibsSubgraphJson());
      merged.setLibsSubgraph(serverFreshness.getLibsSubgraph());
      libsSubgraphUpdated = true;
    }

    if (!isNullOrEmpty(message.getFileTreeJson()) && requestedIsCode
        && isServerFileTreeMoreFresh(merged, serverFreshness)) {
      fileTree = Jso.<CodeBlockImpl>deserialize(message.getFileTreeJson());
      merged.setFileTree(serverFreshness.getFileTree());
      merged.setFileTreeHash(serverFreshness.getFileTreeHash());
      fileTreeUpdated = true;
    }

    if (!isNullOrEmpty(message.getWorkspaceTreeJson())
        && compareFreshness(serverFreshness.getWorkspaceTree(), merged.getWorkspaceTree()) > 0) {
      workspaceTree = Jso.<CodeGraphImpl>deserialize(message.getWorkspaceTreeJson());
      merged.setWorkspaceTree(serverFreshness.getWorkspaceTree());
      workspaceTreeUpdated = true;
    }

    if (!isNullOrEmpty(message.getFullGraphJson())
        && compareFreshness(serverFreshness.getFullGraph(), merged.getFullGraph()) > 0) {
      fullGraph = Jso.<CodeGraphImpl>deserialize(message.getFullGraphJson());
      merged.setFullGraph(serverFreshness.getFullGraph());
      fullGraphUpdated = true;
    }

    if (!isNullOrEmpty(message.getFileReferencesJson())
        && compareFreshness(serverFreshness.getFileReferences(), merged.getFileReferences()) > 0) {
      fileReferences = Jso.<CodeReferencesImpl>deserialize(message.getFileReferencesJson());
      merged.setFileReferences(serverFreshness.getFileReferences());
      fileReferencesUpdated = true;
    }

    if (!requestedFilePath.equals(activeFilePath)) {
      fileTree = null;
      fileTreeUpdated = false;
      fileReferences = null;
      fileReferencesUpdated = false;
    }

    CubeDataUpdates updates = new CubeDataUpdates(fileTreeUpdated, fullGraphUpdated,
        libsSubgraphUpdated, workspaceTreeUpdated, fileReferencesUpdated);

    // At this moment consumers are waiting for *activeFilePath* updates.
    // If update for it is not received yet (requested != active), then
    // fileTree is set to null, and consumers could try to use data from
    // fullGraph using filePath stored in data.
    data = new CubeData(activeFilePath, fileTree, fullGraph, libsSubgraph, workspaceTree,
        fileReferences);
    freshness = merged;

    Log.debug(getClass(), "CUBE data updated", updates, data);

    lastResponseFilePath = requestedFilePath;
    distributor.notifyListeners(updates);

    processDeferredActions();
  }

  @Override
  public void onFail(FailureReason reason) {
    if (isDismissed) {
      return;
    }

    processDeferredActions(true);
  }

  /**
   * Makes a next request if there is a deferred one or
   * schedule retry if required.
   *
   * <p>This method must be called after network response or
   * failure is processed to schedule next network activity.
   *
   * @param afterFail {@code true} indicates that this method
   * is invoked from {@link #onFail(FailureReason)}
   */
  private void processDeferredActions(boolean afterFail) {
    requestedFilePath = null;
    if (deferredRefresh) {
      deferredRefresh = false;
      refresh();
    } else if (afterFail || retryRound > 0) {
      // We should schedule retry if hasn't done it yet.
      if (!retryExecutor.isScheduled()) {
        retryRound++;
        retryExecutor.schedule(2 + retryRound);
      }
    }
  }

  /**
   * Compares server and client freshness.
   *
   * <p>The point is that server can send no freshness, which means that
   * data is not ready.
   *
   * <p>In common case freshness is an integer written as string.
   *
   * @param serverFreshness string that represents server side freshness
   * @param clientFreshness string that represents client side freshness
   * @return {@code 0}, {@code 1}, or {@code -1} according to freshness
   *         relationship
   */
  private static int compareFreshness(String serverFreshness, String clientFreshness) {
    if (isNullOrEmpty(clientFreshness)) {
      throw new IllegalArgumentException("client freshness should never be undefined");
    }
    if (isNullOrEmpty(serverFreshness)) {
      // Assume server don't know better than we do.
      return -1;
    }
    return Long.valueOf(serverFreshness).compareTo(Long.valueOf(clientFreshness));
  }

  /**
   * Sets active file path and requests fresh data from service.
   *
   * @param filePath new active file path
   */
  void setFilePath(String filePath) {
    Preconditions.checkNotNull(filePath);
    activeFilePath = filePath;
    refresh();
  }

  /**
   * Checks if the specified file path is acceptable for Cube.
   *
   * @return {@code false} if we shouldn't send request for the specified file
   */
  private static boolean checkFilePathIsCodeFile(String filePath) {
    Preconditions.checkNotNull(filePath);
    // TODO: should we ignore case?
    return filePath.endsWith(".js") || filePath.endsWith(".py") || filePath.endsWith(".dart");
  }

  private static boolean isServerFileTreeMoreFresh(
      CodeGraphFreshness clientFreshness, CodeGraphFreshness serverFreshness) {
    return compareFreshness(serverFreshness.getFileTree(), clientFreshness.getFileTree()) > 0
        || (compareFreshness(serverFreshness.getFileTree(), clientFreshness.getFileTree()) == 0
            && !serverFreshness.getFileTreeHash().equals(clientFreshness.getFileTreeHash()));
  }

  /**
   * Resends last request.
   *
   * @return {@code false} if object is not ready to perform that action
   *         and should be notified again later
   */
  private boolean retryIfReady() {
    // Waiting for response.
    if (requestedFilePath != null) {
      return false;
    }

    // Force request.
    deferredRefresh = false;
    refresh();
    return true;
  }
}
