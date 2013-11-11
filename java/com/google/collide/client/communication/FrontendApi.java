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
import com.google.collide.client.util.logging.Log;
import com.google.collide.clientlibs.vertx.VertxBus.ReplyHandler;
import com.google.collide.dto.ClientToServerDocOp;
import com.google.collide.dto.CodeErrors;
import com.google.collide.dto.CodeErrorsRequest;
import com.google.collide.dto.CodeGraphRequest;
import com.google.collide.dto.CodeGraphResponse;
import com.google.collide.dto.CompileResponse;
import com.google.collide.dto.EmptyMessage;
import com.google.collide.dto.GetDirectory;
import com.google.collide.dto.GetDirectoryResponse;
import com.google.collide.dto.GetFileContents;
import com.google.collide.dto.GetFileContentsResponse;
import com.google.collide.dto.GetFileRevisions;
import com.google.collide.dto.GetFileRevisionsResponse;
import com.google.collide.dto.GetRunConfig;
import com.google.collide.dto.GetRunConfigResponse;
import com.google.collide.dto.GetWorkspaceMetaData;
import com.google.collide.dto.GetWorkspaceMetaDataResponse;
import com.google.collide.dto.GetWorkspaceParticipants;
import com.google.collide.dto.GetWorkspaceParticipantsResponse;
import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.GwtSettings;
import com.google.collide.dto.KeepAlive;
import com.google.collide.dto.LogFatalRecord;
import com.google.collide.dto.LogFatalRecordResponse;
import com.google.collide.dto.RecoverFromMissedDocOps;
import com.google.collide.dto.RecoverFromMissedDocOpsResponse;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.Search;
import com.google.collide.dto.SearchResponse;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.ServerToClientDocOps;
import com.google.collide.dto.SetMavenConfig;
import com.google.collide.dto.UpdateWorkspaceRunTargets;
import com.google.collide.dto.WorkspaceTreeUpdate;
import com.google.collide.dto.client.DtoClientImpls.ServerErrorImpl;
import com.google.collide.dto.shared.JsonFieldConstants;
import com.google.collide.dtogen.client.RoutableDtoClientImpl;
import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutableDto;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.client.Jso;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.annotations.VisibleForTesting;

/**
 * The EventBus APIs for the Collide server.
 *
 * See {@package com.google.collide.dto} for data objects.
 *
 */
public class FrontendApi {

  /**
   * EventBus API that documents the message types sent to the frontend. This API is fire and
   * forget, since it does not expect a response.
   *
   * @param <REQ> The outgoing message type.
   */
  public static interface SendApi<REQ extends ClientToServerDto> {
    public void send(REQ msg);
  }

  /**
   * EventBus API that documents the message types sent to the frontend. This API is fire and
   * forget, since it does not expect a response.
   *
   * @param <REQ> The outgoing message type.
   */
  public static interface ReceiveApi<RESP extends ServerToClientDto> {
    public void request(final ApiCallback<RESP> msg);
  }

  /**
   * EventBus API that documents the message types sent to the frontend, and the message type
   * expected to be returned as a response.
   *
   * @param <REQ> The outgoing message type.
   * @param <RESP> The incoming message type.
   */
  public static interface RequestResponseApi<
      REQ extends ClientToServerDto, RESP extends ServerToClientDto> {
    public void send(REQ msg, final ApiCallback<RESP> callback);
  }

  /**
   * Callback interface for receiving a matched response for requests to a frontend API.
   */
  public interface ApiCallback<T extends ServerToClientDto> extends MessageRecipient<T> {
    void onFail(FailureReason reason);
  }

  @VisibleForTesting
  protected class ApiImpl<REQ extends ClientToServerDto, RESP extends ServerToClientDto>
      implements
        RequestResponseApi<REQ, RESP>,
        SendApi<REQ>,
        ReceiveApi<RESP>{
    private final String address;

    protected ApiImpl(String address) {
      this.address = address;
    }

    @Override
    public void send(REQ msg) {
      RoutableDtoClientImpl messageImpl = (RoutableDtoClientImpl) msg;
      addCustomFields(messageImpl);
      pushChannel.send(address, messageImpl.serialize());
    }

    @Override
    public void send(REQ msg, final ApiCallback<RESP> callback) {
      RoutableDtoClientImpl messageImpl = (RoutableDtoClientImpl) msg;
      addCustomFields(messageImpl);
      pushChannel.send(address, messageImpl.serialize(), new ReplyHandler() {
          @Override
        public void onReply(String message) {
          Jso jso = Jso.deserialize(message);

          Log.debug(getClass(), "sent message: "+ message);

          if (RoutingTypes.SERVERERROR == jso.getIntField(RoutableDto.TYPE_FIELD)) {
            ServerErrorImpl serverError = (ServerErrorImpl) jso;
            callback.onFail(serverError.getFailureReason());
            return;
          }

          ServerToClientDto messageDto = (ServerToClientDto) jso;

          @SuppressWarnings("unchecked")
          RESP resp = (RESP) messageDto;
          callback.onMessageReceived(resp);
        }
      });
    }

    @Override
    public void request(final ApiCallback<RESP> callback) {
      Log.debug(getClass(), "Performing request on address "+address);
      pushChannel.request(address, new ReplyHandler() {
        @Override
        public void onReply(String message) {
          Log.debug(getClass(), "received message: "+message);
          Jso jso = Jso.deserialize(message);


          if (RoutingTypes.SERVERERROR == jso.getIntField(RoutableDto.TYPE_FIELD)) {
            ServerErrorImpl serverError = (ServerErrorImpl) jso;
            callback.onFail(serverError.getFailureReason());
            return;
          }

          ServerToClientDto messageDto = (ServerToClientDto) jso;

          @SuppressWarnings("unchecked")
          RESP resp = (RESP) messageDto;
          callback.onMessageReceived(resp);
        }
      });
    }

    private void addCustomFields(final RoutableDtoClientImpl messageImpl) {
      customHeaders.iterate(new IterationCallback<String>() {
          @Override
        public void onIteration(String header, String value) {
          messageImpl.<Jso>cast().addField(header, value);
        }
      });
    }
  }

  

  // ///////////////////////////////
  // BEGIN AVAILABLE FRONTEND APIS
  // ///////////////////////////////

  /*
   * IMPORTANT!
   *
   * By convention (and ignore the entries that ignore this convention :) ) we try to have
   * GetDto/ResponseDto pairs for each unique servlet path. This helps us guard against
   * client/frontend API version skew via a simple hash of all of the DTO messages.
   *
   * So if you add a new Servlet Path, please also add a new Get/Response Dto pair.
   */

  public final RequestResponseApi<ClientToServerDocOp, ServerToClientDocOps> MUTATE_FILE =
      makeApi("documents.mutate");

  /**
   * Lets a client re-synchronize with the server's version of a file after being offline or missing
   * a doc op broadcast.
   */
  public final RequestResponseApi<RecoverFromMissedDocOps, RecoverFromMissedDocOpsResponse>
      RECOVER_FROM_MISSED_DOC_OPS = makeApi("documents.recoverMissedDocop");

  /**
   * Get the contents of a file and provisions an edit session so that it can be edited.
   */
  public final RequestResponseApi<GetFileContents, GetFileContentsResponse> GET_FILE_CONTENTS =
      makeApi("documents.createEditSession");

  /**
   * Get the revisions for a file to enable reversioning.
   */
  public final RequestResponseApi<GetFileRevisions, GetFileRevisionsResponse> GET_FILE_REVISIONS = 
      makeApi("documents.getRevisions");

  /**
   * Get a subdirectory. Just the subtree rooted at that path. No associated meta data.
   */
  public final RequestResponseApi<GetDirectory, GetDirectoryResponse> GET_DIRECTORY =
      makeApi("tree.get");

  /** Sends an ADD_FILE, ADD_DIR, COPY, MOVE, or DELETE tree mutation. */
  public final RequestResponseApi<WorkspaceTreeUpdate, EmptyMessage>
      MUTATE_WORKSPACE_TREE = makeApi("tree.mutate");

  /**
   * Send a keep-alive for the client in a workspace.
   */
  public final SendApi<KeepAlive> KEEP_ALIVE = makeApi("participants.keepAlive");

  /**
   * Gets the list of workspace participants.
   */
  public final RequestResponseApi<GetWorkspaceParticipants, GetWorkspaceParticipantsResponse>
      GET_WORKSPACE_PARTICIPANTS = makeApi("participants.getParticipants");

  /** Requests that we get updated information about a workspace's run targets. */
  public final SendApi<UpdateWorkspaceRunTargets> UPDATE_WORKSPACE_RUN_TARGETS =
      makeApi("workspace.updateRunTarget");

  /** Requests workspace state like the last opened files and run targets. */
  public final RequestResponseApi<GetWorkspaceMetaData, GetWorkspaceMetaDataResponse>
      GET_WORKSPACE_META_DATA = makeApi("workspace.getMetaData");

  /**
   * Retrieves code errors for a file.
   */
  public final RequestResponseApi<CodeErrorsRequest, CodeErrors> GET_CODE_ERRORS =
      makeApi("todo.implementMe");

  /**
   * Updates a maven config
   */
  public final SendApi<SetMavenConfig> SET_MAVEN_CONFIG =
      makeApi("maven.save");
//  public final RequestResponseApi<SetMavenConfig,MavenConfig> SET_MAVEN_CONFIG =
//      makeApi("maven.save");

  public final RequestResponseApi<GetRunConfig, GetRunConfigResponse> GET_RUN_CONFIG =
    makeApi("run.get");

//  public final RequestResponseApi<GetRunConfig,GwtStatus> COMPILE_GWT =
//    makeApi("gwt.compile");

  public final RequestResponseApi<GwtCompile,CompileResponse> COMPILE_GWT =
      makeApi("gwt.compile");
  
  public final RequestResponseApi<GwtCompile,CompileResponse> RE_COMPILE_GWT =
      makeApi("gwt.recompile");

  public final RequestResponseApi<GwtCompile, CompileResponse> KILL_GWT =
      makeApi("gwt.status");

  public final ReceiveApi<GwtSettings> GWT_SETTINGS =
      makeApi("gwt.settings");

  /**
   * Retrieves code parsing results.
   */
  public final RequestResponseApi<CodeGraphRequest, CodeGraphResponse> GET_CODE_GRAPH =
      makeApi("todo.implementMe");
  /**
   * Log an exception to the server and potentially receive an unobfuscated response.
   */
  public final RequestResponseApi<LogFatalRecord, LogFatalRecordResponse> LOG_REMOTE =
      makeApi("todo.implementMe");

  // TODO: this may want to move to browser channel instead, for
  // search-as-you-type streaming. No sense to it yet until we have a real
  // backend, though.
  public final RequestResponseApi<Search, SearchResponse> SEARCH = makeApi("todo.implementMe");

  // /////////////////////////////
  // END AVAILABLE FRONTEND APIS
  // /////////////////////////////

  /**
   * Creates a FrontendApi and initializes it.
   */
  public static FrontendApi create(PushChannel pushChannel, StatusManager statusManager) {
    FrontendApi frontendApi = new FrontendApi(pushChannel, statusManager);
    frontendApi.initCustomFields();
    return frontendApi;
  }

  private final JsonStringMap<String> customHeaders = JsonCollections.createMap();
  private final PushChannel pushChannel;
  private final StatusManager statusManager;

  public FrontendApi(PushChannel pushChannel, StatusManager statusManager) {
    this.pushChannel = pushChannel;
    this.statusManager = statusManager;
  }

  private void initCustomFields() {
    customHeaders.put(
        JsonFieldConstants.SESSION_USER_ID, BootstrapSession.getBootstrapSession().getUserId());
  }

  /**
   * Makes an API given the URL.
   *
   * @param <REQ> the request object
   * @param <RESP> the response object
   */
  protected <
      REQ extends ClientToServerDto, RESP extends ServerToClientDto> ApiImpl<REQ, RESP> makeApi(
      String url) {
    return new ApiImpl<REQ, RESP>(url);
  }
}
