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

import com.google.collide.client.code.debugging.DebuggerApiTypes.BreakpointInfo;
import com.google.collide.client.code.debugging.DebuggerApiTypes.CallFrame;
import com.google.collide.client.code.debugging.DebuggerApiTypes.PauseOnExceptionsState;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectId;

/**
 * Implements {@link DebuggerApi} when no API is available in the browser.
 *
 */
public class NoDebuggerApi implements DebuggerApi {

  @Override
  public boolean isDebuggerAvailable() {
    return false;
  }

  @Override
  public String getDebuggingExtensionUrl() {
    return null;
  }

  @Override
  public void runDebugger(String sessionId, String url) {
  }

  @Override
  public void shutdownDebugger(String sessionId) {
  }

  @Override
  public void setBreakpointByUrl(String sessionId, BreakpointInfo breakpointInfo) {
  }

  @Override
  public void removeBreakpoint(String sessionId, String breakpointId) {
  }

  @Override
  public void setBreakpointsActive(String sessionId, boolean active) {
  }

  @Override
  public void setPauseOnExceptions(String sessionId, PauseOnExceptionsState state) {
  }

  @Override
  public void pause(String sessionId) {
  }

  @Override
  public void resume(String sessionId) {
  }

  @Override
  public void stepInto(String sessionId) {
  }

  @Override
  public void stepOut(String sessionId) {
  }

  @Override
  public void stepOver(String sessionId) {
  }

  @Override
  public void requestRemoteObjectProperties(String sessionId, RemoteObjectId remoteObjectId) {
  }

  @Override
  public void setRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String propertyName, String propertyValueExpression) {
  }

  @Override
  public void setRemoteObjectPropertyEvaluatedOnCallFrame(String sessionId, CallFrame callFrame,
      RemoteObjectId remoteObjectId, String propertyName, String propertyValueExpression) {
  }

  @Override
  public void removeRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String propertyName) {
  }

  @Override
  public void renameRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String oldName, String newName) {
  }

  @Override
  public void evaluateExpression(String sessionId, String expression) {
  }

  @Override
  public void evaluateExpressionOnCallFrame(String sessionId, CallFrame callFrame,
      String expression) {
  }

  @Override
  public void requestAllCssStyleSheets(String sessionId) {
  }

  @Override
  public void setStyleSheetText(String sessionId, String styleSheetId, String text) {
  }

  @Override
  public void sendCustomMessage(String sessionId, String message) {
  }

  @Override
  public void addDebuggerResponseListener(DebuggerResponseListener debuggerResponseListener) {
  }

  @Override
  public void removeDebuggerResponseListener(DebuggerResponseListener debuggerResponseListener) {
  }
}
