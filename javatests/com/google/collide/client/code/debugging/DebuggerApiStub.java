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
import com.google.collide.client.code.debugging.DebuggerApiTypes.Location;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnBreakpointResolvedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnPausedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.PauseOnExceptionsState;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectId;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Stub implementation of the {@link DebuggerApi} for testing.
 */
public class DebuggerApiStub implements DebuggerApi {

  private final JsonArray<DebuggerResponseListener> debuggerResponseListeners =
      JsonCollections.createArray();

  @Override
  public boolean isDebuggerAvailable() {
    return true;
  }

  @Override
  public String getDebuggingExtensionUrl() {
    return null;
  }

  @Override
  public void runDebugger(final String sessionId, String url) {
    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onDebuggerAttached(sessionId);
      }
    });
  }

  @Override
  public void shutdownDebugger(String sessionId) {
    dispatchOnDebuggerDetachedEvent(sessionId);
  }

  @Override
  public void setBreakpointByUrl(final String sessionId, final BreakpointInfo breakpointInfo) {
    dispatchOnBreakpointResolvedEvent(
        sessionId, breakpointInfo, JsonCollections.<Location>createArray());
  }

  @Override
  public void removeBreakpoint(final String sessionId, final String breakpointId) {
    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onBreakpointRemoved(sessionId, breakpointId);
      }
    });
  }

  @Override
  public void setBreakpointsActive(String sessionId, boolean active) {
    throw new UnsupportedOperationException("setBreakpointsActive");
  }

  @Override
  public void setPauseOnExceptions(String sessionId, PauseOnExceptionsState state) {
    throw new UnsupportedOperationException("setPauseOnExceptions");
  }

  @Override
  public void pause(final String sessionId) {
    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onPaused(sessionId, new OnPausedResponse() {
          @Override
          public JsonArray<CallFrame> getCallFrames() {
            return JsonCollections.createArray();
          }
        });
      }
    });
  }

  @Override
  public void resume(final String sessionId) {
    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onResumed(sessionId);
      }
    });
  }

  @Override
  public void stepInto(String sessionId) {
    throw new UnsupportedOperationException("stepInto");
  }

  @Override
  public void stepOut(String sessionId) {
    throw new UnsupportedOperationException("stepOut");
  }

  @Override
  public void stepOver(String sessionId) {
    throw new UnsupportedOperationException("stepOver");
  }

  @Override
  public void requestRemoteObjectProperties(String sessionId, RemoteObjectId remoteObjectId) {
    throw new UnsupportedOperationException("requestRemoteObjectProperties");
  }

  @Override
  public void setRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String propertyName, String propertyValueExpression) {
    throw new UnsupportedOperationException("setRemoteObjectProperty");
  }

  @Override
  public void setRemoteObjectPropertyEvaluatedOnCallFrame(String sessionId, CallFrame callFrame,
      RemoteObjectId remoteObjectId, String propertyName, String propertyValueExpression) {
    throw new UnsupportedOperationException("setRemoteObjectPropertyEvaluatedOnCallFrame");
  }

  @Override
  public void removeRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String propertyName) {
    throw new UnsupportedOperationException("removeRemoteObjectProperty");
  }

  @Override
  public void renameRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String oldName, String newName) {
    throw new UnsupportedOperationException("renameRemoteObjectProperty");
  }

  @Override
  public void evaluateExpression(String sessionId, String expression) {
    throw new UnsupportedOperationException("evaluateExpression");
  }

  @Override
  public void evaluateExpressionOnCallFrame(String sessionId, CallFrame callFrame,
      String expression) {
    throw new UnsupportedOperationException("evaluateExpressionOnCallFrame");
  }

  @Override
  public void requestAllCssStyleSheets(String sessionId) {
    throw new UnsupportedOperationException("requestAllCssStyleSheets");
  }

  @Override
  public void setStyleSheetText(String sessionId, String styleSheetId, String text) {
    throw new UnsupportedOperationException("setStyleSheetText");
  }

  @Override
  public void sendCustomMessage(String sessionId, String message) {
    throw new UnsupportedOperationException("sendCustomMessage");
  }

  @Override
  public void addDebuggerResponseListener(DebuggerResponseListener debuggerResponseListener) {
    debuggerResponseListeners.add(debuggerResponseListener);
  }

  @Override
  public void removeDebuggerResponseListener(DebuggerResponseListener debuggerResponseListener) {
    debuggerResponseListeners.remove(debuggerResponseListener);
  }

  public void dispatchOnDebuggerAttachedEvent(final String sessionId) {
    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onDebuggerAttached(sessionId);
      }
    });
  }

  public void dispatchOnDebuggerDetachedEvent(final String sessionId) {
    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onDebuggerDetached(sessionId);
      }
    });
  }

  public void dispatchOnBreakpointResolvedEvent(String sessionId, BreakpointInfo breakpointInfo,
      JsonArray<Location> locations) {
    String breakpointId =  String.valueOf(breakpointInfo.hashCode());
    dispatchOnBreakpointResolvedEvent(sessionId, breakpointInfo, breakpointId, locations);
  }

  public void dispatchOnBreakpointResolvedEvent(final String sessionId,
      final BreakpointInfo breakpointInfo, final String breakpointId,
      final JsonArray<Location> locations) {
    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onBreakpointResolved(sessionId, new OnBreakpointResolvedResponse() {
          @Override
          public BreakpointInfo getBreakpointInfo() {
            return breakpointInfo;
          }

          @Override
          public String getBreakpointId() {
            return breakpointId;
          }

          @Override
          public JsonArray<Location> getLocations() {
            return locations;
          }
        });
      }
    });
  }

  private interface DebuggerResponseDispatcher {
    void dispatch(DebuggerResponseListener responseListener);
  }

  private void dispatchDebuggerResponse(DebuggerResponseDispatcher dispatcher) {
    JsonArray<DebuggerResponseListener> copy = debuggerResponseListeners.copy();
    for (int i = 0, n = copy.size(); i < n; i++) {
      dispatcher.dispatch(copy.get(i));
    }
  }
}
