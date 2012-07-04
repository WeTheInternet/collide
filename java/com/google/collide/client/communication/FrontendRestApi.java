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

package com.google.collide.client.communication;

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.communication.MessageFilter.MessageRecipient;
import com.google.collide.client.status.StatusManager;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.InvalidXsrfTokenServerError;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.ServerError;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls;
import com.google.collide.dto.client.DtoClientImpls.EmptyMessageImpl;
import com.google.collide.dto.client.DtoUtils;
import com.google.collide.dtogen.client.RoutableDtoClientImpl;
import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.client.Jso;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.collide.shared.FrontendConstants;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

/**
 * The Servlet APIs for the Collide server.
 *
 * See {@package com.google.collide.dto} for data objects.
 *
 */
public class FrontendRestApi {

  private static FailureReason getFailureReason(Response response, ServerError responseData) {
    switch (response.getStatusCode()) {
      case Response.SC_OK:
        return null;
      case Response.SC_UNAUTHORIZED:
        if (responseData != null) {
          return responseData.getFailureReason();
        }
        return FailureReason.UNAUTHORIZED;
      // TODO: Make this a server dto error.
      case Response.SC_CONFLICT:
        return FailureReason.STALE_CLIENT;
      case Response.SC_NOT_FOUND:
        if (responseData != null) {
          return responseData.getFailureReason();
        }
        return FailureReason.UNKNOWN;
      case Response.SC_NOT_IMPLEMENTED:
        if (responseData != null) {
          return responseData.getFailureReason();
        }
        return FailureReason.UNKNOWN;
      default:
        return FailureReason.SERVER_ERROR;
    }
  }

  /**
   * Encapsulates a servlet API that documents the message types sent to the
   * frontend, and the optional message types returned as a response.
   *
   * @param <REQ> The outgoing message type.
   * @param <RESP> The incoming message type.
   */
  public static interface Api<REQ extends ClientToServerDto, RESP extends ServerToClientDto> {
    public void send(REQ msg);

    public void send(REQ msg, final ApiCallback<RESP> callback);

    public void send(REQ msg, int retries, final RetryCallback<RESP> callback);
  }

  /**
   * Callback interface for making requests to a frontend API.
   */
  public interface ApiCallback<T extends ServerToClientDto> extends MessageRecipient<T> {
    /**
     * Message didn't come back OK.
     *
     * @param reason the reason for the failure, should not be null
     */
    void onFail(FailureReason reason);
  }

  /**
   * Production implementation of the Api. Sends XmlHttpRequests.
   *
   * Visible so that the {@code MockFrontendApi.MockApi} can inherit it, which
   * in turn is so that {@link #send(ClientToServerDto, int, RetryCallback)} can
   * be inherited and correctly be mocked as the constituent individual sends.
   */
  @VisibleForTesting
  public class ApiImpl<REQ extends ClientToServerDto, RESP extends ServerToClientDto>
      implements Api<REQ, RESP> {
    private final String url;

    @VisibleForTesting
    protected ApiImpl(String url) {
      this.url = url;
    }

    /**
     * @return the url
     */
    public String getUrl() {
      return url;
    }

    /**
     * Calls this Api passing in the specified message. If there is a response
     * that comes back, it will be dispatched on the MessageFilter.
     */
    @Override
    public void send(REQ msg) {
      send(msg, null);
    }

    /**
     * Calls this Api passing in the specified message.
     *
     *  NOTE: Responses will be dispatch on the supplied callback and NOT on the
     * MessageFilter (unless the callback is null).
     */
    @Override
    public void send(REQ msg, final ApiCallback<RESP> callback) {
      doRequest(msg, new RequestCallback() {
        @Override
        public void onError(Request request, Throwable exception) {
          Log.error(FrontendRestApi.class, "Failed: " + exception);
          if (callback != null) {
            callback.onFail(FailureReason.COMMUNICATION_ERROR);
          }
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
          if (response.getStatusCode() == Response.SC_OK) {
            try {

              // If the frontend doesn't write something back to the stream,
              // invoke the callback with an empty message.
              if (response.getText() == null || response.getText().equals("")) {
                if (callback != null) {
                  @SuppressWarnings("unchecked")
                  RESP emptyMessage = (RESP) EmptyMessageImpl.make();
                  callback.onMessageReceived(emptyMessage);
                }
                return;
              }
              ServerToClientDto responseData =
                  (ServerToClientDto) Jso.deserialize(response.getText());
              String action = "?";
              try {
                if (callback != null) {
                  action = "invoking callback";
                  @SuppressWarnings("unchecked")
                  RESP message = (RESP) responseData;
                  callback.onMessageReceived(message);
                } else {
                  action = "dispatching message on MessageFilter";
                  Log.info(FrontendRestApi.class, "dispatching: " + response.getText());
                  getMessageFilter().dispatchMessage(responseData);
                }
              } catch (Exception e) {
                Log.error(FrontendRestApi.class,
                    "Exception when " + action + ": " + response.getText(), e);
              }
            } catch (Exception e) {
              Log.warn(
                  FrontendRestApi.class, "Failed to deserialize JSON response: " + response.getText());
            }
          } else {
            if (callback != null) {
              ServerError responseData = null;
              if (!StringUtils.isNullOrEmpty(response.getText())) {
                try {
                  responseData = DtoUtils.parseAsDto(response.getText(), RoutingTypes.SERVERERROR,
                      RoutingTypes.INVALIDXSRFTOKENSERVERERROR);
                } catch (Exception e) {
                  Log.error(
                      FrontendRestApi.class, "Exception when deserializing " + response.getText(), e);
                }
              }
              FailureReason error = getFailureReason(response, responseData);
              if (recoverer != null) {
                // TODO: Instead of just terminating retry here.
                // We should instead refactor the callback's onFail semantics to also take in
                // "what attempts at failure handling have already been attempted" and make the
                // leaves do something intelligent wrt to handling the final failure. 
                boolean tryAgain = recoverer.handleFailure(FrontendRestApi.this, error, responseData);
                if (tryAgain) {
                  // For auto-retry handlers, this will issue another XHR retry.
                  callback.onFail(error);
                }
              } else {
                callback.onFail(error);
              }
            } else {
              Log.warn(FrontendRestApi.class, "Failed: " + response.getStatusText());
            }
          }
        }
      });
    }

    @Override
    public void send(final REQ msg, int retries, final RetryCallback<RESP> callback) {
      final Countdown countdown = new Countdown(retries);
      send(msg, new ApiCallback<RESP>() {

        @Override
        public void onFail(FailureReason reason) {
          if (FailureReason.UNAUTHORIZED != reason && countdown.canTryAgain()) {
            /*
             * If the failure is due to an authorization issue, there is no
             * reason to retry the request.
             */
            final ApiCallback<RESP> apiCallback = this;
            final RepeatingCommand cmd = new RepeatingCommand() {
              @Override
              public boolean execute() {
                send(msg, apiCallback);
                return false;
              }
            };
            callback.onRetry(countdown.getRetryCount(), countdown.delayToTryAgain(), cmd);
            Scheduler.get().scheduleFixedDelay(cmd, countdown.delayToTryAgain());
          } else {
            callback.onFail(reason);
          }
        }

        @Override
        public void onMessageReceived(RESP message) {
          callback.onMessageReceived(message);
        }
      });
    }

    private void doRequest(REQ msg, RequestCallback internalCallback) {
      final RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, getUrl());
      customHeaders.iterate(new IterationCallback<String>() {
        @Override
        public void onIteration(String header, String value) {
          requestBuilder.setHeader(header, value);
        }
      });

      try {
        RoutableDtoClientImpl messageImpl = (RoutableDtoClientImpl) msg;
        requestBuilder.sendRequest(messageImpl.serialize(), internalCallback);
      } catch (RequestException e1) {
        Log.error(FrontendRestApi.class, e1.getMessage());
      }
    }
  }

  /**
   * Callback with built-in retry notification, so it can put up a "trying again
   * in N seconds" notice if it wants to, or try again early.
   */
  public abstract static class RetryCallback<T extends ServerToClientDto> implements ApiCallback<
      T> {
    /**
     * Called when a given fails, with information about the next time it will
     * be tried and a handle to early if desired.
     *
     * @param count a count of the number of retries so far
     * @param milliseconds time between "now" and the next execution
     * @param retryCmd an already-scheduled {@link RepeatingCommand}, whose
     *        {@link RepeatingCommand#execute()} method could be called early if
     *        desired.
     */
    protected void onRetry(int count, int milliseconds, RepeatingCommand retryCmd) {
      // by default, do nothing. Subclasses may opt to provide status feedback,
      // or trigger retryCmd to try again before the regularly scheduled time.
    }
  }

  private static class Countdown {
    int retriesLeft;
    int retriesDone;

    private Countdown(int retries) {
      this.retriesLeft = retries;
      this.retriesDone = 0;
    }

    /**
     * Decrements the retry counter, and returns {@code true} only if more
     * retries are allowed.
     */
    private boolean canTryAgain() {
      retriesLeft--;
      retriesDone++;
      return retriesLeft > 0;
    }

    /**
     * Returns milliseconds of delay before next .
     */
    private int delayToTryAgain() {
      return 2000 * retriesDone * retriesDone;
    }

    private int getRetryCount() {
      return retriesDone;
    }
  }

  /////////////////////////////////
  // BEGIN AVAILABLE FRONTEND APIS
  /////////////////////////////////

  /*
   * If one were to consider running this as a hosted service, with affordances for branch switching
   * and project management. You would probably need APIs that looked like the following ;) ->
   */

//  /**
//   * Send a keep-alive for the client in a workspace.
//   */
//  public final Api<KeepAlive, EmptyMessage> KEEP_ALIVE = makeApi("/workspace/act/KeepAlive");
//
//  public final Api<ClientToServerDocOp, ServerToClientDocOps> MUTATE_FILE =
//      makeApi("/workspace/act/MutateFile");
//
//  /**
//   * Lets a client re-synchronize with the server's version of a file after
//   * being offline or missing a doc op broadcast.
//   */
//  public final Api<RecoverFromMissedDocOps, RecoverFromMissedDocOpsResponse>
//      RECOVER_FROM_MISSED_DOC_OPS = makeApi("/workspace/act/RecoverFromMissedDocOps");
//
//  /**
//   * Leave a workspace.
//   */
//  public final Api<LeaveWorkspace, EmptyMessage> LEAVE_WORKSPACE =
//      makeApi("/workspace/act/LeaveWorkspace");
//
//  /**
//   * Enter a workspace.
//   */
//  public final Api<EnterWorkspace, EnterWorkspaceResponse> ENTER_WORKSPACE =
//      makeApi("/workspace/act/EnterWorkspace");
//
//  /**
//   * Get the workspace file tree and associated meta data like conflicts and the
//   * tango version number for the file tree.
//   */
//  public final Api<GetFileTree, GetFileTreeResponse>
//      GET_FILE_TREE = makeApi("/workspace/act/GetFileTree");
//
//  /**
//   * Get a subdirectory. Just the subtree rooted at that path. No associated
//   * meta data.
//   */
//  public final Api<GetDirectory, GetDirectoryResponse>
//      GET_DIRECTORY = makeApi("/workspace/act/GetDirectory");
//
//  /**
//   * Get the directory listing and any conflicts.
//   */
//  public final Api<GetFileContents, GetFileContentsResponse>
//      GET_FILE_CONTENTS = makeApi("/workspace/mgmt/GetFileContents");
//
//  /**
//   * Get the sync state.
//   */
//  public final Api<GetSyncState, GetSyncStateResponse>
//      GET_SYNC_STATE = makeApi("/workspace/mgmt/GetSyncState");
//
//  /**
//   * Sync from the parent workspace.
//   */
//  public final Api<Sync, EmptyMessage> SYNC = makeApi("/workspace/mgmt/Sync");
//
//  /**
//   * Undo the most recent sync.
//   */
//  public final Api<UndoLastSync, EmptyMessage> UNDO_LAST_SYNC =
//      makeApi("/workspace/mgmt/UndoLastSync");
//
//  /**
//   * Submit to the parent workspace.
//   */
//  public final Api<Submit, SubmitResponse> SUBMIT = makeApi("/workspace/mgmt/Submit");
//
//  /**
//   * Archives a workspace.
//   */
//  public final Api<SetWorkspaceArchiveState, SetWorkspaceArchiveStateResponse> ARCHIVE_WORKSPACE =
//      makeApi("/workspace/mgmt/setWorkspaceArchiveState");
//
//  /**
//   * Creates a project.
//   */
//  public final Api<CreateProject, CreateProjectResponse> CREATE_PROJECT =
//      makeApi("/project/create");
//
//  /**
//   * Creates a workspace.
//   */
//  public final Api<CreateWorkspace, CreateWorkspaceResponse> CREATE_WORKSPACE =
//      makeApi("/workspace/mgmt/create");
//
//  /**
//   * Gets a list of revisions for a particular file
//   */
//  public final Api<GetFileRevisions, GetFileRevisionsResponse> GET_FILE_REVISIONS =
//      makeApi("/workspace/mgmt/getFileRevisions");
//
//  /**
//   * Gets a diff of a particular file.
//   */
//  public final Api<GetFileDiff, GetFileDiffResponse> GET_FILE_DIFF =
//      makeApi("/workspace/mgmt/getFileDiff");
//
//  /**
//   * Notify the frontend that a tree conflict has been resolved.
//   */
//  public final Api<ResolveTreeConflict, ResolveTreeConflictResponse> RESOLVE_TREE_CONFLICT =
//      makeApi("/workspace/mgmt/resolveTreeConflict");
//
//  /**
//   * Notify the frontend that a conflict chunk has been resolved.
//   */
//  public final Api<ResolveConflictChunk, ConflictChunkResolved> RESOLVE_CONFLICT_CHUNK =
//      makeApi("/workspace/mgmt/resolveConflictChunk");
//  
//  /**
//   * Retrieves code errors for a file.
//   */
//  public final Api<CodeErrorsRequest, CodeErrors> GET_CODE_ERRORS =
//      makeApi("/workspace/code/CodeErrorsRequest");
//
//  /**
//   * Retrieves code parsing results.
//   */
//  public final Api<CodeGraphRequest, CodeGraphResponse> GET_CODE_GRAPH =
//      makeApi("/workspace/code/CodeGraphRequest");
//
//  /**
//   * Gets a list of the templates that might seed new projects
//   */
//  public final Api<GetTemplates, GetTemplatesResponse> GET_TEMPLATES =
//      makeApi("/project/getTemplates");
//
//  /**
//   * Loads a template into a workspace
//   */
//  public final Api<LoadTemplate, LoadTemplateResponse> LOAD_TEMPLATE =
//      makeApi("/workspace/mgmt/loadTemplate");
//
//  /**
//   * Gets a list of the files that have changes in the workspace.
//   */
//  public final Api<GetWorkspaceChangeSummary, GetWorkspaceChangeSummaryResponse>
//      GET_WORKSPACE_CHANGE_SUMMARY = makeApi("/workspace/mgmt/getWorkspaceChangeSummary");
//
//  /**
//   * Gets info about a specific set of workspaces.
//   */
//  public final Api<GetWorkspace, GetWorkspaceResponse> GET_WORKSPACES =
//      makeApi("/workspace/mgmt/get");
//
//  /**
//   * Gets information about a project.
//   */
//  public final Api<GetProjectById, GetProjectByIdResponse> GET_PROJECT_BY_ID =
//      makeApi("/project/getById");
//
//  /**
//   * Gets information about a user's projects.
//   */
//  public final Api<EmptyMessage, GetProjectsResponse> GET_PROJECTS_FOR_USER =
//      makeApi("/project/getForUser");
//
//  public final Api<SetActiveProject, EmptyMessage> SET_ACTIVE_PROJECT =
//      makeApi("/settings/setActiveProject");
//
//  public final Api<SetProjectHidden, EmptyMessage> SET_PROJECT_HIDDEN =
//      makeApi("/settings/setProjectHidden");
//
//  /**
//   * Gets the information about the project that owns a particular workspace.
//   */
//  public final Api<GetOwningProject, GetOwningProjectResponse> GET_OWNING_PROJECT =
//      makeApi("/project/getFromWsId");
//
//  /**
//   * Request to be a project member.
//   */
//  public final Api<RequestProjectMembership, EmptyMessage> REQUEST_PROJECT_MEMBERSHIP =
//      makeApi("/project/requestProjectMembership");
//
//  /**
//   * Gets the list of project members, and users who requested project
//   * membership.
//   */
//  public final Api<GetProjectMembers, GetProjectMembersResponse> GET_PROJECT_MEMBERS =
//      makeApi("/project/getMembers");
//
//  /**
//   * Gets the list of workspace members.
//   */
//  public final Api<GetWorkspaceMembers, GetWorkspaceMembersResponse> GET_WORKSPACE_MEMBERS =
//      makeApi("/workspace/mgmt/getMembers");
//
//  /**
//   * Gets the list of workspace participants.
//   */
//  public final Api<GetWorkspaceParticipants, GetWorkspaceParticipantsResponse>
//      GET_WORKSPACE_PARTICIPANTS = makeApi("/workspace/act/getParticipants");
//
//  /**
//   * Add members to a project.
//   */
//  public final Api<AddProjectMembers, AddMembersResponse> ADD_PROJECT_MEMBERS =
//      makeApi("/project/addProjectMembers");
//
//  /**
//   * Add members to a workspace.
//   */
//  public final Api<AddWorkspaceMembers, AddMembersResponse> ADD_WORKSPACE_MEMBERS =
//      makeApi("/workspace/mgmt/addWorkspaceMembers");
//
//  /**
//   * Set the project role for a single user.
//   */
//  public final Api<SetProjectRole, SetRoleResponse> SET_PROJECT_ROLE =
//      makeApi("/project/setProjectRole");
//
//  /**
//   * Set the workspace role for a single user.
//   */
//  public final Api<SetWorkspaceRole, SetRoleResponse> SET_WORKSPACE_ROLE =
//      makeApi("/workspace/mgmt/setWorkspaceRole");
//
//  /**
//   * Log an exception to the server and potentially receive an unobfuscated
//   * response.
//   */
//  public final Api<LogFatalRecord, LogFatalRecordResponse> LOG_REMOTE =
//      makeApi("/logging/logFatal");
//
//  /** Sends an ADD_FILE, ADD_DIR, COPY, MOVE, or DELETE tree mutation. */
//  public final Api<WorkspaceTreeUpdate, WorkspaceTreeUpdateResponse> MUTATE_WORKSPACE_TREE =
//      makeApi("/workspace/mgmt/mutateTree");
//
//  // TODO: this may want to move to browser channel instead, for
//  // search-as-you-type streaming. No sense to it yet until we have a real
//  // backend, though.
//  public final Api<Search, SearchResponse> SEARCH = makeApi("/workspace/mgmt/search");
//
//  /** Updates the name and summary of a project. */
//  public final Api<UpdateProject, EmptyMessage> UPDATE_PROJECT = makeApi("/project/updateProject");
//
//  /** Requests that we get updated information about a workspace. */
//  public final Api<UpdateWorkspace, EmptyMessage> UPDATE_WORKSPACE =
//      makeApi("/workspace/mgmt/updateWorkspace");
//
//  /** Requests that we get updated information about a workspace's run targets. */
//  public final Api<UpdateWorkspaceRunTargets, EmptyMessage> UPDATE_WORKSPACE_RUN_TARGETS =
//      makeApi("/workspace/mgmt/updateWorkspaceRunTargets");
//
//  public final Api<GetUserAppEngineAppIds, GetUserAppEngineAppIdsResponse>
//      GET_USER_APP_ENGINE_APP_IDS = makeApi("/settings/getUserAppEngineAppIds");
//
//  public final Api<BeginUploadSession, EmptyMessage> BEGIN_UPLOAD_SESSION =
//      makeApi("/uploadcontrol/beginUploadSession");
//
//  public final Api<EndUploadSession, EmptyMessage> END_UPLOAD_SESSION =
//      makeApi("/uploadcontrol/endUploadSession");
//  
//  public final Api<RetryAlreadyTransferredUpload, EmptyMessage> RETRY_ALREADY_TRANSFERRED_UPLOAD =
//      makeApi("/uploadcontrol/retryAlreadyTransferredUpload");
//
//  public final Api<EmptyMessage, GetStagingServerInfoResponse> GET_MIMIC_INFO =
//      makeApi("/settings/getStagingServerInfo");
//
//  public final Api<SetStagingServerAppId, EmptyMessage> SET_MIMIC_APP_ID =
//      makeApi("/settings/setStagingServerAppId");
//
//  public final Api<GetDeployInformation, GetDeployInformationResponse> GET_DEPLOY_INFORMATION =
//      makeApi("/workspace/mgmt/getDeployInformation");
//
//  public final Api<UpdateUserWorkspaceMetadata, EmptyMessage> UPDATE_USER_WORKSPACE_METADATA =
//      makeApi("/settings/updateUserWorkspaceMetadata");
//
//  public final Api<RecoverFromDroppedTangoInvalidation, RecoverFromDroppedTangoInvalidationResponse>
//      RECOVER_FROM_DROPPED_INVALIDATION = makeApi("/payload/recover");
//
//  /**
//   * Deploy a workspace.
//   */
//  public final Api<DeployWorkspace, EmptyMessage> DEPLOY_WORKSPACE =
//      makeApi("/workspace/act/DeployWorkspace");

  ///////////////////////////////
  // END AVAILABLE FRONTEND APIS
  ///////////////////////////////

  /**
   * Generic mechanism for dealing with failed XHRs and responding to them in
   * some sensible fashion.
   */
  public interface XhrFailureHandler {
    /**
     * Returns whether or not the Client should continue retrying RPCs.
     */
    boolean handleFailure(FrontendRestApi api, FailureReason failure, ServerError responseData);
  }

  /**
   * Creates a FrontendApi and initializes it.
   */
  public static FrontendRestApi create(MessageFilter messageFilter, final StatusManager statusManager) {

    // Make a new FrontendApi with a failure recoverer that knows how to deal
    // with XSRF token recovery.
    FrontendRestApi frontendApi = new FrontendRestApi(messageFilter, new XhrFailureHandler() {
      @Override
      public boolean handleFailure(FrontendRestApi api, FailureReason failure, ServerError errorDto) {
        switch (failure) {
          case INVALID_XSRF_TOKEN:
            // Update our XSRF token.
            InvalidXsrfTokenServerError xsrfError = (InvalidXsrfTokenServerError) errorDto;
            BootstrapSession.getBootstrapSession().setXsrfToken(xsrfError.getNewXsrfToken());
            api.initCustomHeaders();
            return true;
          case CLIENT_FRONTEND_VERSION_SKEW:

            // Display a message to the user that he needs to reload the client.
            StatusMessage skewMsg = new StatusMessage(statusManager, MessageType.LOADING,
                "A new version of Collide is available. Please Reload.");
            skewMsg.setDismissable(false);
            skewMsg.addAction(StatusMessage.RELOAD_ACTION);
            skewMsg.fireDelayed(500);
            return false;
          case NOT_LOGGED_IN:

            // Display a message to the user that he needs to reload the client.
            StatusMessage loginMsg = new StatusMessage(statusManager, MessageType.LOADING,
                "You have been signed out. Please reload to sign in.");
            loginMsg.setDismissable(true);
            loginMsg.addAction(StatusMessage.RELOAD_ACTION);
            loginMsg.fireDelayed(500);
            return false;
          default:
            // Allow the RPC retry logic to proceed.
            return true;
        }
      }
    });
    frontendApi.initCustomHeaders();
    return frontendApi;
  }

  private final MessageFilter messageFilter;
  private final JsonStringMap<String> customHeaders = JsonCollections.createMap();
  private final XhrFailureHandler recoverer;

  public FrontendRestApi(MessageFilter messageFilter) {
    this(messageFilter, null);
  }

  public FrontendRestApi(MessageFilter messageFilter, XhrFailureHandler recoverer) {
    this.messageFilter = messageFilter;
    this.recoverer = recoverer;
  }

  private void initCustomHeaders() {
    addCustomHeader(FrontendConstants.CLIENT_BOOTSTRAP_ID_HEADER,
        BootstrapSession.getBootstrapSession().getActiveClientId());
    addCustomHeader(
        FrontendConstants.XSRF_TOKEN_HEADER, BootstrapSession.getBootstrapSession().getXsrfToken());
    addCustomHeader(FrontendConstants.CLIENT_VERSION_HASH_HEADER,
        DtoClientImpls.CLIENT_SERVER_PROTOCOL_HASH);
  }

  /**
   * Adds a custom header which is appended to every api request.
   */
  public void addCustomHeader(String header, String value) {
    customHeaders.put(header, value);
  }

  protected MessageFilter getMessageFilter() {
    return messageFilter;
  }

  /**
   * Makes an API given the URL.
   *
   * @param <REQ> the request object
   * @param <RESP> the response object
   */
  protected <REQ extends ClientToServerDto, RESP extends ServerToClientDto> Api<REQ, RESP> makeApi(
      String url) {
    return new ApiImpl<REQ, RESP>(url);
  }
}
