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

import com.google.collide.client.code.debugging.DebuggerApi.DebuggerResponseListener;
import com.google.collide.client.code.debugging.DebuggerApiTypes.BreakpointInfo;
import com.google.collide.client.code.debugging.DebuggerApiTypes.CallFrame;
import com.google.collide.client.code.debugging.DebuggerApiTypes.ConsoleMessage;
import com.google.collide.client.code.debugging.DebuggerApiTypes.Location;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnAllCssStyleSheetsResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnBreakpointResolvedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnEvaluateExpressionResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnPausedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertiesResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertyChanged;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnScriptParsedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectId;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.ScheduledCommandExecutor;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.StringUtils;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import javax.annotation.Nullable;

/**
 * Represents debugger state on a given debugger session: whether debugger is
 * active, running or paused, and etc.
 *
 * <p>This class also contains cached data responses from the debugger for the
 * life time of a debugger session.
 */
class DebuggerState {

  /**
   * Listener of the "debugger is available" state changes.
   */
  interface DebuggerAvailableListener {
    void onDebuggerAvailableChange();
  }

  /**
   * Listener of the debugger state changes.
   */
  interface DebuggerStateListener {
    void onDebuggerStateChange();
  }

  /**
   * Listener of the {@link DebuggerApiTypes.RemoteObject} related changes.
   */
  interface RemoteObjectListener {
    void onRemoteObjectPropertiesResponse(OnRemoteObjectPropertiesResponse response);
    void onRemoteObjectPropertyChanged(OnRemoteObjectPropertyChanged response);
  }

  /**
   * Listener of the expression evaluation responses.
   */
  interface EvaluateExpressionListener {
    void onEvaluateExpressionResponse(OnEvaluateExpressionResponse response);
    void onGlobalObjectChanged();
  }

  /**
   * Listener of the CSS related responses.
   */
  interface CssListener {
    void onAllCssStyleSheetsResponse(OnAllCssStyleSheetsResponse response);
  }

  /**
   * Listener of the Console related events.
   */
  interface ConsoleListener {
    void onConsoleMessage(ConsoleMessage message);
    void onConsoleMessageRepeatCountUpdated(ConsoleMessage message, int repeatCount);
    void onConsoleMessagesCleared();
  }

  /**
   * Listener of the custom message responses.
   */
  interface CustomMessageListener {
    void onCustomMessageResponse(String response);
  }

  private final String sessionId;
  private final DebuggerApi debuggerApi;
  private final JsonArray<BreakpointInfoImpl> breakpointInfos = JsonCollections.createArray();
  private JsonStringMap<OnScriptParsedResponse> scriptParsedResponses = JsonCollections.createMap();
  private final ListenerManager<DebuggerAvailableListener> debuggerAvailableListenerManager;
  private final ListenerManager<DebuggerStateListener> debuggerStateListenerManager;
  private final ListenerManager<RemoteObjectListener> remoteObjectListenerManager;
  private final ListenerManager<EvaluateExpressionListener> evaluateExpressionListenerManager;
  private final ListenerManager<CssListener> cssListenerManager;
  private final ListenerManager<ConsoleListener> consoleListenerManager;
  private final ListenerManager<CustomMessageListener> customMessageListenerManager;
  private final JsonStringSet expressionsToEvaluate = JsonCollections.createStringSet();

  private boolean active;
  private SourceMapping sourceMapping;

  @Nullable
  private OnPausedResponse lastOnPausedResponse;

  private final ScheduledCommandExecutor expressionsEvaluateCommand =
      new ScheduledCommandExecutor() {
        @Override
        protected void execute() {
          JsonArray<String> expressions = expressionsToEvaluate.getKeys();
          expressionsToEvaluate.clear();

          for (int i = 0, n = expressions.size(); i < n; ++i) {
            sendEvaluateExpressionRequest(expressions.get(i));
          }
        }
      };

  /**
   * Index of the active {@link CallFrame} in the call stack.
   *
   * <p>Default value is {@code 0} that corresponds to the topmost
   * {@link CallFrame} where debugger has stopped. If debugger is not paused,
   * this value has no meaning.
   *
   * <p>This is controlled by the user from the Debugger Sidebar UI.
   */
  private int activeCallFrameIndex = 0;

  /**
   * Last console message received from the debugger.
   */
  private ConsoleMessage lastConsoleMessage;

  private final DebuggerResponseListener debuggerResponseListener = new DebuggerResponseListener() {
    @Override
    public void onDebuggerAvailableChanged() {
      debuggerAvailableListenerManager.dispatch(DEBUGGER_AVAILABLE_DISPATCHER);
    }

    @Override
    public void onDebuggerAttached(String eventSessionId) {
      if (sessionId.equals(eventSessionId)) {
        setActive(true);
      }
    }

    @Override
    public void onDebuggerDetached(String eventSessionId) {
      if (sessionId.equals(eventSessionId)) {
        setActive(false);
      }
    }

    @Override
    public void onBreakpointResolved(String eventSessionId, OnBreakpointResolvedResponse response) {
      if (sessionId.equals(eventSessionId)) {
        updateBreakpointInfo(response);
      }
    }

    @Override
    public void onBreakpointRemoved(String eventSessionId, String breakpointId) {
      if (sessionId.equals(eventSessionId)) {
        removeBreakpointById(breakpointId);
      }
    }

    @Override
    public void onPaused(String eventSessionId, OnPausedResponse response) {
      if (sessionId.equals(eventSessionId)) {
        setOnPausedResponse(response);
      }
    }

    @Override
    public void onResumed(String eventSessionId) {
      if (sessionId.equals(eventSessionId)) {
        setOnPausedResponse(null);
      }
    }

    @Override
    public void onScriptParsed(String eventSessionId, OnScriptParsedResponse response) {
      if (sessionId.equals(eventSessionId)) {
        scriptParsedResponses.put(response.getScriptId(), response);
      }
    }

    @Override
    public void onRemoteObjectPropertiesResponse(String eventSessionId,
        final OnRemoteObjectPropertiesResponse response) {
      if (sessionId.equals(eventSessionId)) {
        remoteObjectListenerManager.dispatch(new Dispatcher<RemoteObjectListener>() {
          @Override
          public void dispatch(RemoteObjectListener listener) {
            listener.onRemoteObjectPropertiesResponse(response);
          }
        });
      }
    }

    @Override
    public void onRemoteObjectPropertyChanged(String eventSessionId,
        final OnRemoteObjectPropertyChanged response) {
      if (sessionId.equals(eventSessionId)) {
        remoteObjectListenerManager.dispatch(new Dispatcher<RemoteObjectListener>() {
          @Override
          public void dispatch(RemoteObjectListener listener) {
            listener.onRemoteObjectPropertyChanged(response);
          }
        });
      }
    }

    @Override
    public void onEvaluateExpressionResponse(String eventSessionId,
        final OnEvaluateExpressionResponse response) {
      if (sessionId.equals(eventSessionId)) {
        CallFrame callFrame = getActiveCallFrame();
        String callFrameId = (callFrame == null ? null : callFrame.getId());
        if (!StringUtils.equalStringsOrEmpty(callFrameId, response.getCallFrameId())) {
          // Maybe a late response from a previous evaluation call. The corresponding evaluation
          // call for the active call frame should have been already sent to the debugger, so just
          // ignore this old response and wait for the actual one.
          return;
        }

        evaluateExpressionListenerManager.dispatch(new Dispatcher<EvaluateExpressionListener>() {
          @Override
          public void dispatch(EvaluateExpressionListener listener) {
            listener.onEvaluateExpressionResponse(response);
          }
        });
      }
    }

    @Override
    public void onGlobalObjectChanged(String eventSessionId) {
      if (sessionId.equals(eventSessionId)) {
        evaluateExpressionListenerManager.dispatch(new Dispatcher<EvaluateExpressionListener>() {
          @Override
          public void dispatch(EvaluateExpressionListener listener) {
            listener.onGlobalObjectChanged();
          }
        });
      }
    }

    @Override
    public void onAllCssStyleSheetsResponse(String eventSessionId,
        final OnAllCssStyleSheetsResponse response) {
      if (sessionId.equals(eventSessionId)) {
        cssListenerManager.dispatch(new Dispatcher<CssListener>() {
          @Override
          public void dispatch(CssListener listener) {
            listener.onAllCssStyleSheetsResponse(response);
          }
        });
      }
    }

    @Override
    public void onConsoleMessage(String eventSessionId, ConsoleMessage message) {
      if (sessionId.equals(eventSessionId)) {
        lastConsoleMessage = message;
        consoleListenerManager.dispatch(new Dispatcher<ConsoleListener>() {
          @Override
          public void dispatch(ConsoleListener listener) {
            listener.onConsoleMessage(lastConsoleMessage);
          }
        });
      }
    }

    @Override
    public void onConsoleMessageRepeatCountUpdated(String eventSessionId, final int repeatCount) {
      if (sessionId.equals(eventSessionId) && lastConsoleMessage != null) {
        consoleListenerManager.dispatch(new Dispatcher<ConsoleListener>() {
          @Override
          public void dispatch(ConsoleListener listener) {
            listener.onConsoleMessageRepeatCountUpdated(lastConsoleMessage, repeatCount);
          }
        });
      }
    }

    @Override
    public void onConsoleMessagesCleared(String eventSessionId) {
      if (sessionId.equals(eventSessionId)) {
        lastConsoleMessage = null;
        consoleListenerManager.dispatch(CONSOLE_MESSAGES_CLEARED_DISPATCHER);
      }
    }

    @Override
    public void onCustomMessageResponse(String eventSessionId, final String response) {
      if (sessionId.equals(eventSessionId)) {
        customMessageListenerManager.dispatch(new Dispatcher<CustomMessageListener>() {
          @Override
          public void dispatch(CustomMessageListener listener) {
            listener.onCustomMessageResponse(response);
          }
        });
      }
    }
  };

  private static final Dispatcher<DebuggerStateListener> DEBUGGER_STATE_DISPATCHER =
      new Dispatcher<DebuggerStateListener>() {
        @Override
        public void dispatch(DebuggerStateListener listener) {
          listener.onDebuggerStateChange();
        }
      };

  private static final Dispatcher<DebuggerAvailableListener> DEBUGGER_AVAILABLE_DISPATCHER =
      new Dispatcher<DebuggerAvailableListener>() {
        @Override
        public void dispatch(DebuggerAvailableListener listener) {
          listener.onDebuggerAvailableChange();
        }
      };

  private static final Dispatcher<ConsoleListener> CONSOLE_MESSAGES_CLEARED_DISPATCHER =
      new Dispatcher<ConsoleListener>() {
        @Override
        public void dispatch(ConsoleListener listener) {
          listener.onConsoleMessagesCleared();
        }
      };

  static DebuggerState create(String sessionId) {
    return new DebuggerState(sessionId, GWT.<DebuggerApi>create(DebuggerApi.class));
  }

  @VisibleForTesting
  static DebuggerState createForTest(String sessionId, DebuggerApi debuggerApi) {
    return new DebuggerState(sessionId, debuggerApi);
  }

  private DebuggerState(String sessionId, DebuggerApi debuggerApi) {
    this.sessionId = sessionId;
    this.debuggerApi = debuggerApi;
    this.debuggerAvailableListenerManager = ListenerManager.create();
    this.debuggerStateListenerManager = ListenerManager.create();
    this.remoteObjectListenerManager = ListenerManager.create();
    this.evaluateExpressionListenerManager = ListenerManager.create();
    this.cssListenerManager = ListenerManager.create();
    this.consoleListenerManager = ListenerManager.create();
    this.customMessageListenerManager = ListenerManager.create();
    this.debuggerApi.addDebuggerResponseListener(debuggerResponseListener);
  }

  ListenerRegistrar<DebuggerAvailableListener> getDebuggerAvailableListenerRegistrar() {
    return debuggerAvailableListenerManager;
  }

  ListenerRegistrar<DebuggerStateListener> getDebuggerStateListenerRegistrar() {
    return debuggerStateListenerManager;
  }

  ListenerRegistrar<RemoteObjectListener> getRemoteObjectListenerRegistrar() {
    return remoteObjectListenerManager;
  }

  ListenerRegistrar<EvaluateExpressionListener> getEvaluateExpressionListenerRegistrar() {
    return evaluateExpressionListenerManager;
  }

  ListenerRegistrar<CssListener> getCssListenerRegistrar() {
    return cssListenerManager;
  }

  ListenerRegistrar<ConsoleListener> getConsoleListenerRegistrar() {
    return consoleListenerManager;
  }

  ListenerRegistrar<CustomMessageListener> getCustomMessageListenerRegistrar() {
    return customMessageListenerManager;
  }

  /**
   * @return whether debugger is available to use
   */
  public boolean isDebuggerAvailable() {
    return debuggerApi.isDebuggerAvailable();
  }

  /**
   * @return URL of the browser extension that provides the debugging API,
   *         or {@code null} if no such extension is available
   */
  public String getDebuggingExtensionUrl() {
    return debuggerApi.getDebuggingExtensionUrl();
  }

  /**
   * @return whether debugger is currently in use
   */
  public boolean isActive() {
    return active;
  }

  /**
   * @return {@code true} if debugger is currently paused, otherwise debugger
   *         is either not active or running
   */
  public boolean isPaused() {
    return lastOnPausedResponse != null;
  }

  /**
   * Sets the index of the active {@link CallFrame} (i.e. selected by the user
   * in the UI).
   *
   * @param index index of the active call frame
   */
  public void setActiveCallFrameIndex(int index) {
    activeCallFrameIndex = index;
  }

  /**
   * @return current {@link SourceMapping} object used for debugging, or
   *         {@code null} if debugger is not active
   */
  public SourceMapping getSourceMapping() {
    return sourceMapping;
  }

  /**
   * @return last {@link OnPausedResponse} from the debugger, or {@code null}
   *         if debugger is not currently paused
   */
  @Nullable
  public OnPausedResponse getOnPausedResponse() {
    return lastOnPausedResponse;
  }

  /**
   * @return {@link OnScriptParsedResponse} for a given source ID, or
   *         {@code null} if undefined
   */
  public OnScriptParsedResponse getOnScriptParsedResponse(String scriptId) {
    return scriptParsedResponses.get(scriptId);
  }

  public CallFrame getActiveCallFrame() {
    if (lastOnPausedResponse == null) {
      return null;
    }
    return lastOnPausedResponse.getCallFrames().get(activeCallFrameIndex);
  }

  /**
   * Calculates {@link PathUtil} for the active call frame of the current
   * debugger call stack.
   *
   * @return a new instance of {@code PathUtil} if the call frame points to a
   *         script in a resource, served by the Collide server, or
   *         {@code null} if this script is served elsewhere, or is anonymous
   *         (result of an {@code eval()} call and etc.), or for other reasons
   */
  public PathUtil getActiveCallFramePath() {
    CallFrame callFrame = getActiveCallFrame();
    if (callFrame == null || callFrame.getLocation() == null) {
      return null;
    }

    Preconditions.checkNotNull(sourceMapping, "No source mapping!");
    Preconditions.checkNotNull(scriptParsedResponses, "No parsed scripts!");

    String scriptId = callFrame.getLocation().getScriptId();
    return sourceMapping.getLocalScriptPath(scriptParsedResponses.get(scriptId));
  }

  public int getActiveCallFrameExecutionLineNumber() {
    CallFrame callFrame = getActiveCallFrame();
    if (callFrame == null || callFrame.getLocation() == null) {
      return -1;
    }

    Preconditions.checkNotNull(sourceMapping, "No source mapping!");
    Preconditions.checkNotNull(scriptParsedResponses, "No parsed scripts!");

    String scriptId = callFrame.getLocation().getScriptId();
    return sourceMapping.getLocalSourceLineNumber(scriptParsedResponses.get(scriptId),
        callFrame.getLocation());
  }

  void runDebugger(SourceMapping sourceMapping, String absoluteResourceUri) {
    Preconditions.checkNotNull(sourceMapping, "Source mapping is NULL!");

    if (active) {
      // We will be reusing current debuggee session, so do a soft reset here.
      softReset();
    } else {
      reset();
    }

    active = true;
    this.sourceMapping = sourceMapping;
    lastOnPausedResponse = null;
    activeCallFrameIndex = 0;
    debuggerApi.runDebugger(sessionId, absoluteResourceUri);
    debuggerStateListenerManager.dispatch(DEBUGGER_STATE_DISPATCHER);
  }

  void shutdown() {
    debuggerApi.shutdownDebugger(sessionId);
  }

  void pause() {
    if (active) {
      debuggerApi.pause(sessionId);
    }
  }

  void resume() {
    if (active) {
      debuggerApi.resume(sessionId);
    }
  }

  void stepInto() {
    if (active) {
      debuggerApi.stepInto(sessionId);
    }
  }

  void stepOut() {
    if (active) {
      debuggerApi.stepOut(sessionId);
    }
  }

  void stepOver() {
    if (active) {
      debuggerApi.stepOver(sessionId);
    }
  }

  void requestRemoteObjectProperties(RemoteObjectId remoteObjectId) {
    if (active) {
      debuggerApi.requestRemoteObjectProperties(sessionId, remoteObjectId);
    }
  }

  void setRemoteObjectProperty(RemoteObjectId remoteObjectId, String propertyName,
      String propertyValueExpression) {
    if (active) {
      CallFrame callFrame = getActiveCallFrame();
      if (callFrame != null) {
        debuggerApi.setRemoteObjectPropertyEvaluatedOnCallFrame(
            sessionId, callFrame, remoteObjectId, propertyName, propertyValueExpression);
      } else {
        debuggerApi.setRemoteObjectProperty(
            sessionId, remoteObjectId, propertyName, propertyValueExpression);
      }
    }
  }

  void removeRemoteObjectProperty(RemoteObjectId remoteObjectId, String propertyName) {
    if (active) {
      debuggerApi.removeRemoteObjectProperty(sessionId, remoteObjectId, propertyName);
    }
  }

  void renameRemoteObjectProperty(RemoteObjectId remoteObjectId, String oldName, String newName) {
    if (active) {
      debuggerApi.renameRemoteObjectProperty(sessionId, remoteObjectId, oldName, newName);
    }
  }

  /**
   * Evaluates a given expression either on the active {@link CallFrame} if the
   * debugger is currently paused, or on the global object if it is running.
   *
   * @param expression expression to evaluate
   */
  void evaluateExpression(String expression) {
    if (active) {
      // Schedule-finally the evaluations to remove duplicates.
      expressionsToEvaluate.add(expression);
      expressionsEvaluateCommand.scheduleFinally();
    }
  }

  private void sendEvaluateExpressionRequest(String expression) {
    if (active) {
      CallFrame callFrame = getActiveCallFrame();
      if (callFrame != null) {
        debuggerApi.evaluateExpressionOnCallFrame(sessionId, callFrame, expression);
      } else {
        debuggerApi.evaluateExpression(sessionId, expression);
      }
    }
  }

  void requestAllCssStyleSheets() {
    if (active) {
      debuggerApi.requestAllCssStyleSheets(sessionId);
    }
  }

  void setStyleSheetText(String styleSheetId, String text) {
    if (active) {
      debuggerApi.setStyleSheetText(sessionId, styleSheetId, text);
    }
  }

  void setBreakpoint(Breakpoint breakpoint) {
    if (active && breakpoint.isActive()) {
      BreakpointInfoImpl breakpointInfo = findBreakpointInfo(breakpoint);
      if (breakpointInfo == null) {
        breakpointInfo = new BreakpointInfoImpl(breakpoint,
            sourceMapping.getRemoteBreakpoint(breakpoint));
        breakpointInfos.add(breakpointInfo);
      }
      if (StringUtils.isNullOrEmpty(breakpointInfo.breakpointId)) {
        // Send to the debugger if it's not yet resolved.
        debuggerApi.setBreakpointByUrl(sessionId, breakpointInfo);
      }
    }
  }

  void removeBreakpoint(Breakpoint breakpoint) {
    if (active && breakpoint.isActive()) {
      BreakpointInfoImpl breakpointInfo = findBreakpointInfo(breakpoint);
      if (breakpointInfo != null && !StringUtils.isNullOrEmpty(breakpointInfo.breakpointId)) {
        debuggerApi.removeBreakpoint(sessionId, breakpointInfo.breakpointId);
      } else {
        Log.error(getClass(), "Breakpoint to remove not found: " + breakpoint);
      }
    }
  }

  void setBreakpointsEnabled(boolean enabled) {
    if (active) {
      debuggerApi.setBreakpointsActive(sessionId, enabled);
    }
  }

  void sendCustomMessage(String message) {
    if (active) {
      debuggerApi.sendCustomMessage(sessionId, message);
    }
  }

  @VisibleForTesting
  BreakpointInfoImpl findBreakpointInfo(Breakpoint breakpoint) {
    for (int i = 0, n = breakpointInfos.size(); i < n; ++i) {
      BreakpointInfoImpl breakpointInfo = breakpointInfos.get(i);
      if (breakpoint.equals(breakpointInfo.breakpoint)) {
        return breakpointInfo;
      }
    }
    return null;
  }

  private void setActive(boolean value) {
    if (active != value) {
      Preconditions.checkState(!value, "Reactivation of debugger is not supported");
      reset();
      active = value;
      debuggerStateListenerManager.dispatch(DEBUGGER_STATE_DISPATCHER);
    }
  }

  private void setOnPausedResponse(@Nullable OnPausedResponse response) {
    if (lastOnPausedResponse != response) {
      lastOnPausedResponse = response;
      activeCallFrameIndex = 0;
      debuggerStateListenerManager.dispatch(DEBUGGER_STATE_DISPATCHER);
    }
  }

  private void reset() {
    softReset();
    active = false;
    sourceMapping = null;
    lastOnPausedResponse = null;
    activeCallFrameIndex = 0;
    breakpointInfos.clear();
  }

  /**
   * Performs a "soft" reset to clear the data that does not survive a restart
   * of an active debugger session. This happens when we choose to debug another
   * application within an already open debugger session (and debuggee window).
   *
   * TODO: We should catch the corresponding event from the extension.
   * The closest seems to be onGlobalObjectChanged, but it does not work.
   */
  private void softReset() {
    scriptParsedResponses = JsonCollections.createMap();
  }

  private void updateBreakpointInfo(OnBreakpointResolvedResponse response) {
    for (int i = 0, n = breakpointInfos.size(); i < n; ++i) {
      BreakpointInfoImpl breakpointInfo = breakpointInfos.get(i);
      if (StringUtils.equalNonEmptyStrings(response.getBreakpointId(), breakpointInfo.breakpointId)
          || breakpointInfo.equalsTo(response.getBreakpointInfo())) {
        if (!StringUtils.isNullOrEmpty(response.getBreakpointId())) {
          breakpointInfo.breakpointId = response.getBreakpointId();
        } else {
          Log.error(getClass(), "Empty breakpointId in the response!");
        }
        breakpointInfo.locations.addAll(response.getLocations());
        break;
      }
    }
  }

  private void removeBreakpointById(String breakpointId) {
    for (int i = 0, n = breakpointInfos.size(); i < n; ++i) {
      BreakpointInfoImpl breakpointInfo = breakpointInfos.get(i);
      if (breakpointId.equals(breakpointInfo.breakpointId)) {
        breakpointInfos.remove(i);
        break;
      }
    }
  }

  /**
   * Implementation of {@link BreakpointInfo} that also contains information
   * received from the debugger.
   */
  @VisibleForTesting
  static class BreakpointInfoImpl implements BreakpointInfo {

    private final Breakpoint breakpoint;
    private final BreakpointInfo delegate;

    // Populated from the debugger responses.
    private String breakpointId;
    private final JsonArray<Location> locations = JsonCollections.createArray();

    private BreakpointInfoImpl(Breakpoint breakpoint, BreakpointInfo delegate) {
      this.breakpoint = breakpoint;
      this.delegate = delegate;
    }

    @Override
    public String getUrl() {
      return delegate.getUrl();
    }

    @Override
    public String getUrlRegex() {
      return delegate.getUrlRegex();
    }

    @Override
    public int getLineNumber() {
      return delegate.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
      return delegate.getColumnNumber();
    }

    @Override
    public String getCondition() {
      return delegate.getCondition();
    }

    public Breakpoint getBreakpoint() {
      return breakpoint;
    }

    public String getBreakpointId() {
      return breakpointId;
    }

    public JsonArray<Location> getLocations() {
      return locations.copy();
    }

    private boolean equalsTo(BreakpointInfo breakpointInfo) {
      return breakpointInfo != null
          && StringUtils.equalStringsOrEmpty(getUrl(), breakpointInfo.getUrl())
          && getLineNumber() == breakpointInfo.getLineNumber()
          && getColumnNumber() == breakpointInfo.getColumnNumber()
          && StringUtils.equalStringsOrEmpty(getCondition(), breakpointInfo.getCondition());
    }
  }
}
