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

package com.google.collide.client.xhrmonitor;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.collide.client.xhrmonitor.XhrWarden.WardenXhrRequest;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import elemental.xml.XMLHttpRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the warden.
 */
public class XhrWardenTests extends TestCase {

  private static final int TEST_WARNING_LIMIT = 7;
  private static final int TEST_ERROR_LIMIT = 8;

  /**
   * Creates a mock request but does not automatically replay it!
   */
  private static WardenXhrRequest createRequest(int id, double timestamp) {
    WardenXhrRequest request = EasyMock.createNiceMock(WardenXhrRequest.class);
    expect(request.getId()).andReturn(String.valueOf(id)).anyTimes();
    expect(request.getTime()).andReturn(timestamp).anyTimes();
    return request;
  }

  /**
   * Creates a number of warden http requests starting with start id and at
   * timestamp. For each created request start id and timestamp will be
   * incremented by 1.
   *
   * Request mocks are automatically replayed.
   */
  private static List<WardenXhrRequest> createMultipleRequests(
      int number, int startid, int timestamp) {
    List<WardenXhrRequest> requests = new ArrayList<WardenXhrRequest>();
    for (int i = 0; i < number; i++) {
      WardenXhrRequest request = createRequest(startid++, timestamp++);
      replay(request);
      requests.add(request);
    }
    return requests;
  }

  /**
   * Creates multiple WardenHttpRequests starting at id 1 and timestamp 1.
   *
   * Request mocks are automatically replayed.
   */
  private static List<WardenXhrRequest> createMultipleRequests(int number) {
    return createMultipleRequests(number, 1, 1);
  }

  private XhrWarden.WardenListener listener;
  private XhrWarden.WardenImpl warden;

  @Override
  public void setUp() {
    listener = EasyMock.createMock(XhrWarden.WardenListener.class);
    warden = new XhrWarden.WardenImpl(TEST_WARNING_LIMIT, TEST_ERROR_LIMIT, listener);
  }


  public void testDontKillRequestsWhenUnderLimit() {
    List<WardenXhrRequest> requests =
        createMultipleRequests(XhrWarden.WARDEN_WARNING_THRESHOLD - 1);
    replay(listener);

    for (WardenXhrRequest request : requests) {
      warden.onRequestOpening(request);
      warden.onRequestOpen(request);
    }

    assertEquals(requests.size(), warden.getRequestCount());

    // Effectively nothing special should have occurred
    for (WardenXhrRequest request : requests) {
      verify(request);
    }
    verify(listener);
  }

  public void testLongestIdleIsCorrect() {
    List<WardenXhrRequest> requests =
        createMultipleRequests(XhrWarden.WARDEN_WARNING_THRESHOLD - 1);
    replay(listener);

    for (WardenXhrRequest request : requests) {
      warden.onRequestOpening(request);
      warden.onRequestOpen(request);
    }

    assertSame(requests.get(0), warden.getLongestIdleRequest());
    verify(listener);
  }

  public void testRequestsAreRemovedWhenFinished() {
    List<WardenXhrRequest> requests =
        createMultipleRequests(XhrWarden.WARDEN_WARNING_THRESHOLD - 1);
    replay(listener);

    for (WardenXhrRequest request : requests) {
      warden.onRequestOpening(request);
      warden.onRequestOpen(request);
      warden.onRequestDone(request);
    }

    assertEquals(0, warden.getRequestCount());
    verify(listener);
  }

  public void testWarningAreIssuesWhenRequestLimitIsHit() {
    List<WardenXhrRequest> requests = createMultipleRequests(TEST_WARNING_LIMIT);

    // Mock up listener behavior
    listener.onWarning(warden);
    expectLastCall().times(2);
    replay(listener);

    // Add 7 Requests which will trigger the limit
    for (WardenXhrRequest request : requests) {
      warden.onRequestOpening(request);
      warden.onRequestOpen(request);
    }
    warden.onRequestDone(requests.get(TEST_WARNING_LIMIT - 1));

    // This will cause another log to the listener
    warden.onRequestOpening(requests.get(TEST_WARNING_LIMIT - 1));
    warden.onRequestOpen(requests.get(TEST_WARNING_LIMIT - 1));

    // This should not trigger another warning since we are still over the limit
    WardenXhrRequest lastRequest = createRequest(TEST_WARNING_LIMIT, TEST_WARNING_LIMIT);
    replay(lastRequest);
    warden.onRequestOpen(lastRequest);

    assertEquals(TEST_WARNING_LIMIT, warden.getRequestCount());
    verify(listener);
  }

  public void testErrorsAreIssuedWhenRequestHitRequestLimit() {
    List<WardenXhrRequest> requests = createMultipleRequests(TEST_ERROR_LIMIT, 2, 2);
    WardenXhrRequest killedRequest = createRequest(1, 1);
    killedRequest.kill();
    replay(killedRequest);

    // Mock up listener behavior
    listener.onWarning(warden);
    listener.onEmergency(warden, killedRequest);
    replay(listener);

    // Add 9 Requests which will trigger the over-limit
    warden.onRequestOpening(killedRequest);
    warden.onRequestOpen(killedRequest);

    for (WardenXhrRequest request : requests) {
      warden.onRequestOpening(request);
      warden.onRequestOpen(request);
    }

    assertEquals(8, warden.getRequestCount());
    verify(listener, killedRequest);
  }

  public void testCustomHeadersInserted() {
    XhrWarden.WardenListener listener = EasyMock.createMock(XhrWarden.WardenListener.class);
    replay(listener);

    XMLHttpRequest mockXHR = EasyMock.createMock(XMLHttpRequest.class);
    mockXHR.setRequestHeader("X-Test", "test");
    replay(mockXHR);

    WardenXhrRequest request = createRequest(1, 1);
    expect(request.getRequest()).andReturn(mockXHR).anyTimes();
    replay(request);

    warden.addCustomHeader("X-Test", "test");
    warden.onRequestOpen(request);

    verify(mockXHR);
  }
}
