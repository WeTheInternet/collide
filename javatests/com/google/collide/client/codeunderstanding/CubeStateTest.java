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

import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.MockApi;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createCodeBlock;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createCodeGraph;
import static com.google.collide.client.codeunderstanding.CodeGraphTestUtils.createFreshness;

import com.google.collide.client.codeunderstanding.CodeGraphTestUtils.MockCubeClientDistributor;
import com.google.collide.client.communication.FrontendApi;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.CodeGraphResponse;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphResponseImpl;
import com.google.collide.dto.client.DtoClientImpls.MockCodeGraphResponseImpl;
import com.google.collide.json.client.Jso;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * A test case for {@link CubeState}.
 */
public class CubeStateTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  /**
   * Tests that {@link CubeState} fires update notification when it receives
   * data which is more fresh than stored one.
   */
  public void testFreshDataMakesUpdate() {
    CodeGraphResponseImpl response = MockCodeGraphResponseImpl.make();
    response.setFreshness(createFreshness("1", "0", "0"));
    response.setLibsSubgraphJson(Jso.serialize(createCodeGraph(
        createCodeBlock("0", "/foo.js", CodeBlock.Type.FILE, 0, 0, 1, 0))));

    final MockApi api = new MockApi();
    MockCubeClientDistributor distributor = new MockCubeClientDistributor();
    CubeState state = new CubeState(api, distributor);
    state.setFilePath("");

    state.refresh();

    assertEquals("no notifications after request", 0, distributor.collectedNotifications.size());
    assertEquals("refresh causes one api request", 1, api.collectedCallbacks.size());
    FrontendApi.ApiCallback<CodeGraphResponse> callback = api.collectedCallbacks.get(0);
    assertNotNull("state pushes non null callback", callback);

    callback.onMessageReceived(response);

    assertEquals("just one notification on response", 1, distributor.collectedNotifications.size());
    CubeDataUpdates freshness = distributor.collectedNotifications.get(0);
    assertTrue("updated libs subgraph", freshness.isLibsSubgraph());
    assertNotNull("has libs subgraph", state.getData().getLibsSubgraph());
    assertFalse("old file tree", freshness.isFileTree());
    assertFalse("old full graph", freshness.isFullGraph());
    assertFalse("old workspace tree", freshness.isWorkspaceTree());
  }

  /**
   * Tests that consequent refresh requests for the same data are collapsed to
   * just one frontend request.
   */
  public void testRefreshCollapsing() {
    final MockApi api = new MockApi();
    MockCubeClientDistributor distributor = new MockCubeClientDistributor();
    CubeState state = new CubeState(api, distributor);
    state.setFilePath("a.js");

    state.refresh();
    state.refresh();
    assertEquals(1, api.collectedCallbacks.size());
  }

  /**
   * Tests that consequent refresh requests for different data are served
   * sequentially.
   */
  public void testRefreshSequencing() {
    CodeGraphResponseImpl response = MockCodeGraphResponseImpl.make();
    response.setFreshness(createFreshness("1", "1", "1"));
    final MockApi api = new MockApi();
    MockCubeClientDistributor distributor = new MockCubeClientDistributor();
    CubeState state = new CubeState(api, distributor);

    state.setFilePath("a.js");
    state.refresh();
    state.setFilePath("b.js");
    state.refresh();

    assertEquals("one request after two refreshes", 1, api.collectedCallbacks.size());

    FrontendApi.ApiCallback<CodeGraphResponse> callback = api.collectedCallbacks.get(0);
    callback.onMessageReceived(response);

    assertEquals("second request comes after first response", 2, api.collectedCallbacks.size());
  }

  /**
   * Tests that consequent refresh requests for different data do not cause
   * frontend request for data that is not required anymore.
   */
  public void testRefreshSequencingAndCollapsing() {
    CodeGraphResponseImpl response = MockCodeGraphResponseImpl.make();
    response.setFreshness(createFreshness("1", "1", "1"));
    final MockApi api = new MockApi();
    MockCubeClientDistributor distributor = new MockCubeClientDistributor();
    CubeState state = new CubeState(api, distributor);

    state.setFilePath("a.js");
    state.refresh();
    state.setFilePath("b.js");
    state.refresh();
    state.setFilePath("c.js");
    state.refresh();

    assertEquals("first request is served immediately", 1, api.collectedCallbacks.size());

    FrontendApi.ApiCallback<CodeGraphResponse> callback = api.collectedCallbacks.get(0);
    callback.onMessageReceived(response);

    assertEquals("second request is served after first response", 2, api.collectedCallbacks.size());

    callback = api.collectedCallbacks.get(1);
    callback.onMessageReceived(response);

    assertEquals("no more requests after second response", 2, api.collectedCallbacks.size());
  }

  /**
   * Tests that update notifications bring {@code false} for updates that
   * doesn't bring fresh data.
   */
  public void testStaleDataMakesNoUpdates() {
    CodeGraphResponseImpl response = MockCodeGraphResponseImpl.make();
    response.setFreshness(createFreshness("0", "0", "0"));
    response.setLibsSubgraphJson(Jso.serialize(createCodeGraph(
        createCodeBlock("0", "/foo.js", CodeBlock.Type.FILE, 0, 0, 1, 0))));

    final MockApi api = new MockApi();
    MockCubeClientDistributor distributor = new MockCubeClientDistributor();
    CubeState state = new CubeState(api, distributor);
    state.setFilePath("");

    state.refresh();

    assertEquals("no updates before response", 0, distributor.collectedNotifications.size());
    assertEquals("one request for processing", 1, api.collectedCallbacks.size());
    FrontendApi.ApiCallback<CodeGraphResponse> callback = api.collectedCallbacks.get(0);
    assertNotNull("non null callback", callback);

    callback.onMessageReceived(response);

    assertEquals("one notification after response", 1, distributor.collectedNotifications.size());
    CubeDataUpdates freshness = distributor.collectedNotifications.get(0);
    assertFalse("old libs subgraph", freshness.isLibsSubgraph());
    assertFalse("old file tree", freshness.isFileTree());
    assertFalse("old full graph", freshness.isFullGraph());
    assertFalse("old workspace tree", freshness.isWorkspaceTree());
    assertNull("no libs subgraph", state.getData().getLibsSubgraph());
  }
}
