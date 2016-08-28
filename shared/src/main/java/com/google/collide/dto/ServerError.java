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

package com.google.collide.dto;

import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;

/**
 * Notifies the client of an error on the frontend.
 *
 */
@RoutingType(type = RoutingTypes.SERVERERROR)
public interface ServerError extends ServerToClientDto {

  /**
   * @return the error code
   */
  FailureReason getFailureReason();

  /** @return the error details (probably not suitable for end-user consumption) */
  String getDetails();

  
  /**
   * A list of errors shared by the server and the client.
   */
  public static enum FailureReason {
    /**
     * Broadly covers any communication error between the client and server in
     * which a response is not received.
     */
    COMMUNICATION_ERROR,

    /**
     * The server returned an error.
     */
    SERVER_ERROR,

    /**
     * The server indicated that the current user is not authorized for the
     * requested service, most likely due to an ACL failure.
     */
    UNAUTHORIZED,

    /**
     * The server indicated that the current user is not logged in to GAIA.
     */
    NOT_LOGGED_IN,

    /**
     * The server indicated that the supplied XSRF token is invalid or out of
     * date. If the user is validly logged in, we deliver a new token with the
     * response XHR.
     */
    INVALID_XSRF_TOKEN,

    /**
     * The server indicated that the request was unable to complete because the
     * client request was out of sync with the current state of some server
     * resource. This can include a missing workspace session.
     */
    STALE_CLIENT,

    /**
     * The failure does not fit into any of the other categories.
     */
    UNKNOWN,

    /**
     * No workspace session present when one is expected.
     */
    MISSING_WORKSPACE_SESSION,
    
    /**
     * No file edit session where one is expected.
     */
    MISSING_FILE_SESSION,
    
    /**
     * Client doc ops failed to apply on the server's document.
     */
    DOC_OPS_FAILED,

    /**
     * The Client is talking to a Frontend that might speak different DTOs.
     */
    CLIENT_FRONTEND_VERSION_SKEW
  }
}
