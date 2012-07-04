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

import com.google.collide.client.communication.FrontendApi;
import com.google.collide.dto.RecoverFromMissedDocOps;
import com.google.collide.dto.RecoverFromMissedDocOpsResponse;
import com.google.collide.dto.Search;
import com.google.collide.dto.SearchResponse;
import com.google.collide.dto.ServerError;
import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.ServerToClientDto;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A testing mock of our FrontendApi, sporting each API with an expectation
 * queue instead of actual client/server interaction.
 *
 */
public class MockFrontendApi extends FrontendApi {

  public class MockApi<REQ extends ClientToServerDto, RESP extends ServerToClientDto>
      extends ApiImpl<REQ, RESP> {

    protected MockApi() {
      super("mock");
    }

    Queue<FrontendExpectation<REQ, RESP>> expectations =
        new LinkedList<FrontendExpectation<REQ, RESP>>();

    /**
     * Throws a {@code RuntimeException} if there are unmet expectations.
     */
    public void assertIsDrained() {
      if (!expectations.isEmpty()) {
        throw new Expectation.LeftoverExpectations(expectations.size(), expectations.peek());
      }
    }

    public void expectAndFailSynchronouslyWithCommunicationError(REQ expect) {
      expectations.add(new FrontendExpectation.CommunicationFailure<REQ, RESP>(expect));
    }

    public void expectAndFail(REQ expect, ServerError error) {
      expectations.add(new FrontendExpectation.Fail<REQ, RESP>(expect, error));
    }

    public void expectAndReturn(REQ expect, RESP response) {
      expectations.add(new FrontendExpectation.Response<REQ, RESP>(expect, response));
    }

    public void expectAndReturnAsync(REQ expect, RESP response) {
      expectations.add(new FrontendExpectation.AsyncResponse<REQ, RESP>(expect, response));
    }

    public void expectAndThrow(REQ expect, Throwable thrown) {
      expectations.add(new FrontendExpectation.Throw<REQ, RESP>(expect, thrown));
    }

    /**
     * Mostly for debugging the test infrastructure, this says how many unmet
     * expectations are in queue.
     *
     * @return integer count
     */
    public int getExpectationCount() {
      return expectations.size();
    }

    @Override
    public void send(REQ msg) {
      FrontendExpectation<REQ, RESP> expects = expectations.remove();
      expects.checkExpectation(msg);
    }

    @Override
    public void send(REQ msg, ApiCallback<RESP> callback) {
      FrontendExpectation<REQ, RESP> expects = expectations.remove();
      expects.checkExpectation(msg);
      expects.doCallback(callback);
    }
  }

  public MockFrontendApi() {
    super(null, null);
  }

  /**
   * Checks that all the APIs are drained
   */
  public void assertIsDrained() {
    // TODO: implement?
  }

  @Override
  protected <REQ extends ClientToServerDto, RESP extends ServerToClientDto> MockApi<REQ, RESP> makeApi(
      String url) {
    return new MockApi<REQ, RESP>();
  }
}
