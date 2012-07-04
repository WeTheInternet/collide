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

import com.google.collide.client.code.debugging.DebuggerApiTypes.Location;
import com.google.collide.client.code.debugging.DebuggerState.BreakpointInfoImpl;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Tests for {@link DebuggerState}.
 */
public class DebuggerStateTest extends GWTTestCase {

  private static final String SESSION_ID = "123456";
  private static final String BASE_URI = "http://www.example.com:2020";

  private static final int BREAKPOINT_LINE_NUMBER = 123;
  private static final String BREAKPOINT_CONDITION = "foo==12 || bar<5";

  private PathUtil applicationPath;
  private DebuggerApiStub debuggerApiStub;
  private DebuggerState debuggerState;
  private SourceMapping sourceMapping;

  @Override
  protected void gwtSetUp() {
    applicationPath = new PathUtil("/my_path/index.html");
    debuggerApiStub = new DebuggerApiStub();
    debuggerState = DebuggerState.createForTest(SESSION_ID, debuggerApiStub);
    sourceMapping = StaticSourceMapping.create(BASE_URI);
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testRunDebugger() {
    assertTrue(debuggerState.isDebuggerAvailable());
    assertFlags(false, false);
    doRunDebugger();
  }

  public void testRunDebuggerAfterPaused() {
    doRunDebugger();
    doPauseDebugger();
    doRunDebugger();
    assertFlags(true, false);
  }

  public void testPauseAndResume() {
    doRunDebugger();
    doPauseDebugger();
    doResumeDebugger();

    // Pause debugger on another session.
    debuggerApiStub.pause(SESSION_ID + "-FOO");
    assertFlags(true, false);
  }

  public void testOnAttachAndOnDetach() {
    doRunDebugger();
    doAttachDebugger();
    doDetachDebugger();

    // Run, pause, attach
    doRunDebugger();
    doPauseDebugger();
    doAttachDebugger();

    // Run, pause, detach
    doRunDebugger();
    doPauseDebugger();
    doDetachDebugger();
  }

  public void testSetBreakpointWhenNotActive() {
    Breakpoint breakpoint = createBreakpoint();
    debuggerState.setBreakpoint(breakpoint);

    BreakpointInfoImpl breakpointInfo = debuggerState.findBreakpointInfo(breakpoint);
    assertNull(breakpointInfo);

    debuggerState.removeBreakpoint(breakpoint);
    assertFlags(false, false);
  }

  public void testSetBreakpoint() {
    doRunDebugger();
    doSetBreakpoint();
  }

  public void testRemoveBreakpoint() {
    doRunDebugger();
    BreakpointInfoImpl breakpointInfo = doSetBreakpoint();
    doRemoveBreakpoint(breakpointInfo.getBreakpoint());
  }

  public void testUpdateBreakpoint() {
    doRunDebugger();
    BreakpointInfoImpl breakpointInfo = doSetBreakpoint();

    JsonArray<Location> locations = JsonCollections.createArray();
    final Location location1 = createLocation(1, 10, "source_id_1");
    final Location location2 = createLocation(2, 20, "source_id_2");
    locations.add(location1);
    locations.add(location2);
    debuggerApiStub.dispatchOnBreakpointResolvedEvent(SESSION_ID, breakpointInfo, locations);

    assertEquals(2, breakpointInfo.getLocations().size());
    assertEquals(location1, breakpointInfo.getLocations().get(0));
    assertEquals(location2, breakpointInfo.getLocations().get(1));

    // Update with no breakpointInfo.
    locations = JsonCollections.createArray();
    final Location location3 = createLocation(3, 30, "source_id_3");
    locations.add(location3);
    debuggerApiStub.dispatchOnBreakpointResolvedEvent(SESSION_ID, null,
        breakpointInfo.getBreakpointId(), locations);

    assertEquals(3, breakpointInfo.getLocations().size());
    assertEquals(location3, breakpointInfo.getLocations().get(2));
  }

  public void testSetInactiveBreakpoint() {
    doRunDebugger();
    Breakpoint breakpoint = new Breakpoint.Builder(createBreakpoint())
        .setActive(false)
        .build();
    debuggerState.setBreakpoint(breakpoint);
    assertNull(debuggerState.findBreakpointInfo(breakpoint));
  }

  private void doRunDebugger() {
    debuggerState.runDebugger(sourceMapping, sourceMapping.getRemoteSourceUri(applicationPath));
    assertFlags(true, false);
  }

  private void doPauseDebugger() {
    debuggerApiStub.pause(SESSION_ID);
    assertFlags(true, true);
  }

  private void doResumeDebugger() {
    debuggerApiStub.resume(SESSION_ID);
    assertFlags(true, false);
  }

  private void doAttachDebugger() {
    boolean paused = debuggerState.isPaused();
    debuggerApiStub.dispatchOnDebuggerAttachedEvent(SESSION_ID);
    assertFlags(true, paused);
  }

  private void doDetachDebugger() {
    debuggerApiStub.dispatchOnDebuggerDetachedEvent(SESSION_ID);
    assertFlags(false, false);
  }

  private BreakpointInfoImpl doSetBreakpoint() {
    Breakpoint breakpoint = createBreakpoint();
    debuggerState.setBreakpoint(breakpoint);

    BreakpointInfoImpl breakpointInfo = debuggerState.findBreakpointInfo(breakpoint);
    assertNotNull(breakpointInfo);
    assertEquals(breakpoint, breakpointInfo.getBreakpoint());
    assertNotNull(breakpointInfo.getBreakpointId());
    assertEquals(BREAKPOINT_LINE_NUMBER, breakpointInfo.getLineNumber());
    assertEquals(BREAKPOINT_CONDITION, breakpointInfo.getCondition());
    assertEquals(0, breakpointInfo.getLocations().size());

    String url = breakpointInfo.getUrl();
    String urlRegex = breakpointInfo.getUrlRegex();

    if (url != null) {
      assertNull(urlRegex);
      assertEquals(BASE_URI + applicationPath.getPathString(), breakpointInfo.getUrl());
    } else {
      assertNotNull(urlRegex);
      assertTrue(RegExp.compile(urlRegex).test(BASE_URI + applicationPath.getPathString()));
    }

    return breakpointInfo;
  }

  private void doRemoveBreakpoint(Breakpoint breakpoint) {
    debuggerState.removeBreakpoint(breakpoint);
    assertNull(debuggerState.findBreakpointInfo(breakpoint));
  }

  private Breakpoint createBreakpoint() {
    return new Breakpoint.Builder(applicationPath, BREAKPOINT_LINE_NUMBER)
        .setCondition(BREAKPOINT_CONDITION)
        .build();
  }

  private Location createLocation(final int lineNumber, final int columnNumber,
      final String scriptId) {
    return new Location() {

      @Override
      public int getColumnNumber() {
        return columnNumber;
      }

      @Override
      public int getLineNumber() {
        return lineNumber;
      }

      @Override
      public String getScriptId() {
        return scriptId;
      }
    };
  }

  private void assertFlags(boolean active, boolean paused) {
    assertEquals(active, debuggerState.isActive());
    assertEquals(paused, debuggerState.isPaused());
  }
}
