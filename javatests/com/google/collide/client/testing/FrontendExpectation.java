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

package com.google.collide.client.testing;

import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.testing.MockFrontendApi.MockApi;
import com.google.collide.dto.ServerError;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutableDto;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;


/**
 * Canned expectations, pairing an expected message with either a simulated
 * response ({@link FrontendExpectation.Response}), simulated server failure
 * ({@link FrontendExpectation.Fail}), or a simulated client-side exception 
 * ({@link FrontendExpectation.Throw}).
 *
 * These are used in the {@link MockApi} class.
 *
 *
 * @param <REQ> request type for the expectation
 * @param <RESP> correct response type for the expectation
 */
abstract class FrontendExpectation<
    REQ extends ClientToServerDto, RESP extends ServerToClientDto> extends Expectation<REQ, RESP> {

  /**
   * Expectation for a server-side error instead of a correct message.
   *
   * @param <REQ> request type
   * @param <RESP> correct response type (of the contract, not the thrown
   *        exception type)
   */
  public static class Fail<REQ extends ClientToServerDto, RESP extends ServerToClientDto>
      extends FrontendExpectation<REQ, RESP> {
    @SuppressWarnings("unused")
    private ServerError error;
  
    public Fail(REQ req, ServerError error) {
      super(req);
      this.error = error;
    }
  
    @Override
    public void doCallback(final ApiCallback<RESP> callback) {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          // did we really mean to lose our thrown info?
          callback.onFail(FailureReason.COMMUNICATION_ERROR);
        }
      });
    }
  }
  
  /**
   * Expectation for a server-side error due to a communication error.
   *
   * @param <REQ> request type
   */
  public static class CommunicationFailure<
      REQ extends ClientToServerDto, RESP extends ServerToClientDto> extends FrontendExpectation<
      REQ, RESP> {

    public CommunicationFailure(REQ request) {
      super(request);
    }

    @Override
    public void doCallback(final ApiCallback<RESP> callback) {
      callback.onFail(FailureReason.COMMUNICATION_ERROR);
    }
  }

  /**
   * Expectation for a correct message response.
   *
   * @param <REQ> request type
   * @param <RESP> response type
   */
  public static class Response<REQ extends ClientToServerDto, RESP extends ServerToClientDto>
      extends FrontendExpectation<REQ, RESP> {
    private RESP response;

    public Response(REQ req, RESP response) {
      super(req);
      this.response = response;
    }

    @Override
    public void doCallback(final ApiCallback<RESP> callback) {
      callback.onMessageReceived(response);
    }
  }

  /**
   * Expectation for a correct message response, delivered asynchronously.
   *
   * @param <REQ> request type
   * @param <RESP> response type
   */
  public static class AsyncResponse<REQ extends ClientToServerDto, RESP extends ServerToClientDto>
      extends FrontendExpectation<REQ, RESP> {
    private RESP response;

    public AsyncResponse(REQ req, RESP response) {
      super(req);
      this.response = response;
    }

    @Override
    public void doCallback(final ApiCallback<RESP> callback) {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          callback.onMessageReceived(response);
        }
      });
    }
  }
  /**
   * Expectation for a thrown exception instead of a correct message.
   *
   * @param <REQ> request type
   * @param <RESP> correct response type (of the contract, not the thrown
   *        exception type)
   */
  public static class Throw<REQ extends ClientToServerDto, RESP extends ServerToClientDto>
      extends FrontendExpectation<REQ, RESP> {
    @SuppressWarnings("unused")
    private Throwable response;

    public Throw(REQ req, Throwable response) {
      super(req);
      this.response = response;
    }

    @Override
    public void doCallback(final ApiCallback<RESP> callback) {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          // did we really mean to lose our thrown info?
          callback.onFail(FailureReason.COMMUNICATION_ERROR);
        }
      });
    }
  }
  
  public FrontendExpectation(REQ request) {
    super(request);
  }

  /**
   * Throws a runtime exception if the given request isn't the expected value.
   *
   * @param request actual request to test against expection
   */
  public void checkExpectation(REQ request) {
    String check = checkMatch(this.request, request);
    if (check.length() > 0) {
      throw new ExpectationViolation(check);
    }
  }

  /**
   * Does whatever this expectation type does with the callback object.
   *
   * @param callback
   */
  public abstract void doCallback(ApiCallback<RESP> callback);

  /**
   * Tests two message objects for "equality," not identity or class-identity,
   * by checking that all the object fields match except Chrome's __gwt_ObjectId
   *
   * To allow some looseness in the matching, the check is not symmetric.  Any
   * properties set in the first, pattern object must match those in the second,
   * target argument, but the reverse is not true.
   *
   * @param pattern the "pattern" objects, the properties of which have to
   *   match those in the {@code target} object.
   * @param target the "actual" object in the comparison.  This may have extra
   *   properties not checked by the {@code pattern}, but where they do overlap,
   *   the {@code target} properties must match the {@code pattern}.
   * @return an empty string if the objects do match, or a text identifying the
   *   mismatch(es) if they do not.
   */
  @VisibleForTesting
  static native String checkMatch(RoutableDto pattern, RoutableDto target) /*-{
    var result = new Array();

    // some special handling for arrays, for which we don't want "extra is okay"
    if (pattern instanceof Array) {
      if (! target instanceof Array) {
        result.push("expected array, got non-array object");
      } else if (pattern.length != target.length) {
        result.push("expected array length " + pattern.length
            + ", got array of length " + target.length);
      }
    }

    for (prop in pattern) {
      if (pattern.hasOwnProperty(prop)) {
        if (typeof(pattern[prop]) == 'object') {
          if (typeof(target[prop]) != 'object') {
            result.push(" field " + prop + " is not an object, but " + typeof(target[prop]));
          } else {
            // using a simple != fails, it gives identity not equivalence.
            // so we recurse checking property equivalence:
            var fail =
                @com.google.collide.client.testing.FrontendExpectation::checkMatch(Lcom/google/collide/dtogen/shared/RoutableDto;Lcom/google/collide/dtogen/shared/RoutableDto;)
                    (pattern[prop], target[prop]);
            if (fail != "") {
              result.push("in field " + prop + ": " + fail);
            }
          }
        } else if (typeof(target[prop]) == 'undefined'
                   || pattern[prop] != target[prop]) {
          result.push(" field " + prop + " does not match: " + pattern[prop]
              + " != " + target[prop]);
        }
      }
    }
    return result.toString();
  }-*/;
}
