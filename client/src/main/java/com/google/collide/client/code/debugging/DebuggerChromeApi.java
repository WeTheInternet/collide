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
import com.google.collide.client.code.debugging.DebuggerApiTypes.ConsoleMessage;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnAllCssStyleSheetsResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnBreakpointResolvedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnEvaluateExpressionResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnPausedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertiesResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertyChanged;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnScriptParsedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.PauseOnExceptionsState;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectId;
import com.google.collide.client.util.JsIntegerMap;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.client.Jso;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;

import elemental.client.Browser;
import elemental.events.CustomEvent;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;

/**
 * Implements communication API with the Chrome browser debugger.
 *
 * <p>On the side of the Chrome browser the "Collide Debugger" extension should
 * be installed to accept the requests from the Collide client.
 */
class DebuggerChromeApi implements DebuggerApi {

  // TODO: Replace the URL when extension is public.
  private static final String EXTENSION_URL =
      "http://www.example.com/collide.crx";

  private static final String DEBUGGER_EXTENSION_REQUEST_EVENT = "DebuggerExtensionRequest";
  private static final String DEBUGGER_EXTENSION_RESPONSE_EVENT = "DebuggerExtensionResponse";

  private static final String METHOD_WINDOW_OPEN = "window.open";
  private static final String METHOD_WINDOW_CLOSE = "window.close";
  private static final String METHOD_ON_ATTACH = "onAttach";
  private static final String METHOD_ON_DETACH = "onDetach";
  private static final String METHOD_ON_GLOBAL_OBJECT_CHANGED = "onGlobalObjectChanged";
  private static final String METHOD_ON_EXTENSION_INSTALLED_CHANGED = "onExtensionInstalledChanged";

  private static final String METHOD_CSS_GET_ALL_STYLE_SHEETS = "CSS.getAllStyleSheets";
  private static final String METHOD_CSS_SET_STYLE_SHEET_TEXT = "CSS.setStyleSheetText";

  private static final String METHOD_CONSOLE_ENABLE = "Console.enable";
  private static final String METHOD_CONSOLE_MESSAGE_ADDED = "Console.messageAdded";
  private static final String METHOD_CONSOLE_MESSAGE_REPEAT_COUNT_UPDATED =
      "Console.messageRepeatCountUpdated";
  private static final String METHOD_CONSOLE_MESSAGES_CLEARED = "Console.messagesCleared";

  private static final String METHOD_DEBUGGER_ENABLE = "Debugger.enable";
  private static final String METHOD_DEBUGGER_SET_BREAKPOINT_BY_URL = "Debugger.setBreakpointByUrl";
  private static final String METHOD_DEBUGGER_REMOVE_BREAKPOINT = "Debugger.removeBreakpoint";
  private static final String METHOD_DEBUGGER_SET_BREAKPOINTS_ACTIVE =
      "Debugger.setBreakpointsActive";
  private static final String METHOD_DEBUGGER_SET_PAUSE_ON_EXCEPTIONS =
      "Debugger.setPauseOnExceptions";
  private static final String METHOD_DEBUGGER_PAUSE = "Debugger.pause";
  private static final String METHOD_DEBUGGER_RESUME = "Debugger.resume";
  private static final String METHOD_DEBUGGER_STEP_INTO = "Debugger.stepInto";
  private static final String METHOD_DEBUGGER_STEP_OUT = "Debugger.stepOut";
  private static final String METHOD_DEBUGGER_STEP_OVER = "Debugger.stepOver";
  private static final String METHOD_DEBUGGER_EVALUATE_ON_CALL_FRAME =
      "Debugger.evaluateOnCallFrame";

  private static final String METHOD_RUNTIME_CALL_FUNCTION_ON = "Runtime.callFunctionOn";
  private static final String METHOD_RUNTIME_EVALUATE = "Runtime.evaluate";
  private static final String METHOD_RUNTIME_GET_PROPERTIES = "Runtime.getProperties";

  private static final String EVENT_DEBUGGER_BREAKPOINT_RESOLVED = "Debugger.breakpointResolved";
  private static final String EVENT_DEBUGGER_SCRIPT_PARSED = "Debugger.scriptParsed";
  private static final String EVENT_DEBUGGER_PAUSED = "Debugger.paused";
  private static final String EVENT_DEBUGGER_RESUMED = "Debugger.resumed";

  private final JsonArray<DebuggerResponseListener> debuggerResponseListeners =
      JsonCollections.createArray();
  private final EventRemover debuggerExtensionListenerRemover;
  private int lastUsedId = 0;
  private JsonStringMap<JsIntegerMap<Integer>> idToCustomMessageIds = JsonCollections.createMap();
  private JsonStringMap<JsIntegerMap<Callback>> callbacks = JsonCollections.createMap();

  private final EventListener debuggerExtensionResponseListener = new EventListener() {
    @Override
    public void handleEvent(Event evt) {
      Object detail = ((CustomEvent) evt).getDetail();
      if (detail != null) {
        handleDebuggerExtensionResponse(new ExtensionResponse(Jso.deserialize(detail.toString())));
      }
    }
  };

  private static class ExtensionResponse {
    private final Jso response;

    ExtensionResponse(Jso response) {
      this.response = response;
    }

    int messageId() {
      return response.getFieldCastedToInteger("id");
    }

    String sessionId() {
      return response.getStringField("target");
    }

    String methodName() {
      return response.getStringField("method");
    }

    Jso request() {
      return response.getJsObjectField("request").cast();
    }

    Jso result() {
      return response.getJsObjectField("result").cast();
    }

    String errorMessage() {
      return response.getStringField("error");
    }

    boolean isError() {
      return !StringUtils.isNullOrEmpty(errorMessage());
    }
  }

  private interface DebuggerResponseDispatcher {
    void dispatch(DebuggerResponseListener responseListener);
  }

  private interface Callback {
    void run(ExtensionResponse response);
  }

  public DebuggerChromeApi() {
    debuggerExtensionListenerRemover = Browser.getWindow().addEventListener(
        DEBUGGER_EXTENSION_RESPONSE_EVENT, debuggerExtensionResponseListener, false);
  }

  public void teardown() {
    debuggerResponseListeners.clear();
    debuggerExtensionListenerRemover.remove();
    idToCustomMessageIds = JsonCollections.createMap();
    callbacks = JsonCollections.createMap();
  }

  @Override
  public final native boolean isDebuggerAvailable() /*-{
    try {
      return !!top["__DebuggerExtensionInstalled"];
    } catch (e) {
    }
    return false;
  }-*/;

  @Override
  public String getDebuggingExtensionUrl() {
    return EXTENSION_URL;
  }

  @Override
  public void runDebugger(String sessionId, String url) {
    Jso params = Jso.create();
    params.addField("url", url);
    sendCustomEvent(sessionId, METHOD_WINDOW_OPEN, params);
    sendCustomEvent(sessionId, METHOD_DEBUGGER_ENABLE, null);
    sendCustomEvent(sessionId, METHOD_CONSOLE_ENABLE, null);
  }

  @Override
  public void shutdownDebugger(String sessionId) {
    sendCustomEvent(sessionId, METHOD_WINDOW_CLOSE, null);
  }

  @Override
  public void setBreakpointByUrl(String sessionId, BreakpointInfo breakpointInfo) {
    String condition = breakpointInfo.getCondition();
    Jso params = Jso.create();
    if (breakpointInfo.getUrl() != null) {
      params.addField("url", breakpointInfo.getUrl());
    } else {
      params.addField("urlRegex", breakpointInfo.getUrlRegex());
    }
    params.addField("lineNumber", breakpointInfo.getLineNumber());
    params.addField("columnNumber", breakpointInfo.getColumnNumber());
    params.addField("condition", condition == null ? "" : condition);
    sendCustomEvent(sessionId, METHOD_DEBUGGER_SET_BREAKPOINT_BY_URL, params);
  }

  @Override
  public void removeBreakpoint(String sessionId, String breakpointId) {
    Jso params = Jso.create();
    params.addField("breakpointId", breakpointId);
    sendCustomEvent(sessionId, METHOD_DEBUGGER_REMOVE_BREAKPOINT, params);
  }

  @Override
  public void setBreakpointsActive(String sessionId, boolean active) {
    Jso params = Jso.create();
    params.addField("active", active);
    sendCustomEvent(sessionId, METHOD_DEBUGGER_SET_BREAKPOINTS_ACTIVE, params);
  }

  @Override
  public void setPauseOnExceptions(String sessionId, PauseOnExceptionsState state) {
    Jso params = Jso.create();
    params.addField("state", state.toString().toLowerCase());
    sendCustomEvent(sessionId, METHOD_DEBUGGER_SET_PAUSE_ON_EXCEPTIONS, params);
  }

  @Override
  public void pause(String sessionId) {
    sendCustomEvent(sessionId, METHOD_DEBUGGER_PAUSE, null);
  }

  @Override
  public void resume(String sessionId) {
    sendCustomEvent(sessionId, METHOD_DEBUGGER_RESUME, null);
  }

  @Override
  public void stepInto(String sessionId) {
    sendCustomEvent(sessionId, METHOD_DEBUGGER_STEP_INTO, null);
  }

  @Override
  public void stepOut(String sessionId) {
    sendCustomEvent(sessionId, METHOD_DEBUGGER_STEP_OUT, null);
  }

  @Override
  public void stepOver(String sessionId) {
    sendCustomEvent(sessionId, METHOD_DEBUGGER_STEP_OVER, null);
  }

  @Override
  public void requestRemoteObjectProperties(String sessionId, RemoteObjectId remoteObjectId) {
    Jso params = Jso.create();
    params.addField("objectId", remoteObjectId.toString());
    params.addField("ownProperties", true);
    sendCustomEvent(sessionId, METHOD_RUNTIME_GET_PROPERTIES, params);
  }

  @Override
  public void setRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String propertyName, String propertyValueExpression) {
    String expression = preparePropertyValueExpression(propertyValueExpression);
    evaluateExpression(sessionId, expression,
        createSetRemoteObjectPropertyCallback(sessionId, remoteObjectId, propertyName));
  }

  @Override
  public void setRemoteObjectPropertyEvaluatedOnCallFrame(String sessionId, CallFrame callFrame,
      RemoteObjectId remoteObjectId, String propertyName, String propertyValueExpression) {
    String expression = preparePropertyValueExpression(propertyValueExpression);
    evaluateExpressionOnCallFrame(sessionId, callFrame, expression,
        createSetRemoteObjectPropertyCallback(sessionId, remoteObjectId, propertyName));
  }

  /**
   * This will wrap the given expression into braces if it looks like a
   * JavaScript object syntax.
   *
   * <p>We do this just for the user's convenience overriding the semantics of
   * the {@code window.eval} method, that evaluates the <i>"{}"</i> into
   * {@code undefined}, and <i>"{foo:123}"</i> into {@code 123}.
   */
  private String preparePropertyValueExpression(String expression) {
    if (StringUtils.firstNonWhitespaceCharacter(expression) == '{'
        && StringUtils.lastNonWhitespaceCharacter(expression) == '}') {
      return "(" + expression + ")";
    }
    return expression;
  }

  private Callback createSetRemoteObjectPropertyCallback(final String sessionId,
      final RemoteObjectId remoteObjectId, final String propertyName) {
    return new Callback() {
      @Override
      public void run(ExtensionResponse evaluationResponse) {
        OnEvaluateExpressionResponse evaluationParsedResponse =
            DebuggerChromeApiUtils.parseOnEvaluateExpressionResponse(
                evaluationResponse.request(), evaluationResponse.result());

        boolean isError = evaluationResponse.isError()
            || evaluationParsedResponse == null
            || evaluationParsedResponse.wasThrown()
            || evaluationParsedResponse.getResult() == null;

        final RemoteObject evaluationResult = isError ? null : evaluationParsedResponse.getResult();

        Jso params = Jso.create();
        if (!isError && DebuggerApiUtils.isNonFiniteNumber(evaluationResult)) {
          params.addField("functionDeclaration", "function(a) {"
              + "  this[a] = " + evaluationResult.getDescription() + ";"
              + "  return this[a];"
              + "}");
          params.addField("objectId", remoteObjectId.toString());

          JsonArray<Jso> arguments = JsonCollections.createArray();
          arguments.add(Jso.create());
          arguments.get(0).addField("value", propertyName);

          params.addField("arguments", arguments);
        } else if (!isError) {
          params.addField("functionDeclaration", "function(a, b) { this[a] = b; return this[a]; }");
          params.addField("objectId", remoteObjectId.toString());

          JsonArray<Jso> arguments = JsonCollections.createArray();
          arguments.add(Jso.create());
          arguments.add(Jso.create());
          arguments.get(0).addField("value", propertyName);

          if (evaluationResult.getObjectId() == null) {
            if (!DebuggerApiUtils.addPrimitiveJsoField(
                arguments.get(1), "value", evaluationResult)) {
              isError = true;
            }
          } else {
            arguments.get(1).addField("objectId", evaluationResult.getObjectId().toString());
          }

          params.addField("arguments", arguments);
        }

        if (isError) {
          // We do not know the property value. Just dispatch the error event.
          OnRemoteObjectPropertyChanged parsedResponse =
              DebuggerChromeApiUtils.createOnEditRemoteObjectPropertyResponse(
                  remoteObjectId, propertyName, null, true);
          dispatchOnRemoteObjectPropertyChanged(sessionId, parsedResponse);
          return;
        }

        sendCustomEvent(sessionId, METHOD_RUNTIME_CALL_FUNCTION_ON, params, new Callback() {
          @Override
          public void run(ExtensionResponse response) {
            RemoteObject newValue =
                DebuggerChromeApiUtils.parseCallFunctionOnResult(response.result());
            boolean isError = response.isError()
                || newValue == null
                || !DebuggerApiUtils.equal(evaluationResult, newValue);
            OnRemoteObjectPropertyChanged parsedResponse =
                DebuggerChromeApiUtils.createOnEditRemoteObjectPropertyResponse(
                    remoteObjectId, propertyName, newValue, isError);
            dispatchOnRemoteObjectPropertyChanged(sessionId, parsedResponse);
          }
        });
      }
    };
  }

  @Override
  public void removeRemoteObjectProperty(final String sessionId,
      final RemoteObjectId remoteObjectId, final String propertyName) {
    Jso params = Jso.create();
    params.addField("functionDeclaration", "function(a) { delete this[a]; return !(a in this); }");
    params.addField("objectId", remoteObjectId.toString());

    JsonArray<Jso> arguments = JsonCollections.createArray();
    arguments.add(Jso.create());
    arguments.get(0).addField("value", propertyName);
    params.addField("arguments", arguments);

    sendCustomEvent(sessionId, METHOD_RUNTIME_CALL_FUNCTION_ON, params, new Callback() {
      @Override
      public void run(ExtensionResponse response) {
        boolean isError = response.isError() || !DebuggerApiUtils.castToBoolean(
            DebuggerChromeApiUtils.parseCallFunctionOnResult(response.result()));
        OnRemoteObjectPropertyChanged parsedResponse =
            DebuggerChromeApiUtils.createOnRemoveRemoteObjectPropertyResponse(
                remoteObjectId, propertyName, isError);
        dispatchOnRemoteObjectPropertyChanged(sessionId, parsedResponse);
      }
    });
  }

  @Override
  public void renameRemoteObjectProperty(final String sessionId,
      final RemoteObjectId remoteObjectId, final String oldName, final String newName) {
    Jso params = Jso.create();
    params.addField("functionDeclaration", "function(a, b) {"
        + "  if (a === b) return true;"
        + "  this[b] = this[a];"
        + "  if (this[b] !== this[a]) return false;"
        + "  delete this[a];"
        + "  return !(a in this);"
        + "}");
    params.addField("objectId", remoteObjectId.toString());

    JsonArray<Jso> arguments = JsonCollections.createArray();
    arguments.add(Jso.create());
    arguments.add(Jso.create());
    arguments.get(0).addField("value", oldName);
    arguments.get(1).addField("value", newName);
    params.addField("arguments", arguments);

    sendCustomEvent(sessionId, METHOD_RUNTIME_CALL_FUNCTION_ON, params, new Callback() {
      @Override
      public void run(ExtensionResponse response) {
        boolean isError = response.isError() || !DebuggerApiUtils.castToBoolean(
            DebuggerChromeApiUtils.parseCallFunctionOnResult(response.result()));
        OnRemoteObjectPropertyChanged parsedResponse =
            DebuggerChromeApiUtils.createOnRenameRemoteObjectPropertyResponse(
                remoteObjectId, oldName, newName, isError);
        dispatchOnRemoteObjectPropertyChanged(sessionId, parsedResponse);
      }
    });
  }

  @Override
  public void evaluateExpression(String sessionId, String expression) {
    evaluateExpression(sessionId, expression, null);
  }

  private void evaluateExpression(String sessionId, String expression, Callback callback) {
    Jso params = Jso.create();
    params.addField("expression", expression);
    params.addField("doNotPauseOnExceptions", true);
    sendCustomEvent(sessionId, METHOD_RUNTIME_EVALUATE, params, callback);
  }

  @Override
  public void evaluateExpressionOnCallFrame(String sessionId, CallFrame callFrame,
      String expression) {
    evaluateExpressionOnCallFrame(sessionId, callFrame, expression, null);
  }

  private void evaluateExpressionOnCallFrame(String sessionId, CallFrame callFrame,
      String expression, Callback callback) {
    Jso params = Jso.create();
    params.addField("expression", expression);
    params.addField("callFrameId", callFrame.getId());
    sendCustomEvent(sessionId, METHOD_DEBUGGER_EVALUATE_ON_CALL_FRAME, params, callback);
  }

  @Override
  public void requestAllCssStyleSheets(String sessionId) {
    sendCustomEvent(sessionId, METHOD_CSS_GET_ALL_STYLE_SHEETS, null);
  }

  @Override
  public void setStyleSheetText(String sessionId, String styleSheetId, String text) {
    Jso params = Jso.create();
    params.addField("styleSheetId", styleSheetId);
    params.addField("text", text);
    sendCustomEvent(sessionId, METHOD_CSS_SET_STYLE_SHEET_TEXT, params);
  }

  @Override
  public void sendCustomMessage(String sessionId, String message) {
    Jso messageObject = Jso.deserialize(message);
    if (messageObject == null) {
      return;
    }

    JsIntegerMap<Integer> map = idToCustomMessageIds.get(sessionId);
    if (map == null) {
      map = JsIntegerMap.create();
      idToCustomMessageIds.put(sessionId, map);
    }

    map.put(lastUsedId + 1, messageObject.getIntField("id"));
    sendCustomEvent(sessionId, messageObject.getStringField("method"),
        (Jso) messageObject.getObjectField("params"));
  }

  @Override
  public void addDebuggerResponseListener(DebuggerResponseListener debuggerResponseListener) {
    debuggerResponseListeners.add(debuggerResponseListener);
  }

  @Override
  public void removeDebuggerResponseListener(DebuggerResponseListener debuggerResponseListener) {
    debuggerResponseListeners.remove(debuggerResponseListener);
  }

  private void sendCustomEvent(String sessionId, String methodName, Jso params) {
    sendCustomEvent(sessionId, methodName, params, null);
  }

  private void sendCustomEvent(String sessionId, String methodName, Jso params, Callback callback) {
    Log.debug(getClass(), "Sending message to the debugger: " + methodName);

    final int id = ++lastUsedId;

    Jso data = Jso.create();
    data.addField("id", id);
    data.addField("target", sessionId);
    data.addField("method", methodName);
    if (params != null) {
      data.addField("params", params);
    }

    if (callback != null) {
      JsIntegerMap<Callback> map = callbacks.get(sessionId);
      if (map == null) {
        map = JsIntegerMap.create();
        callbacks.put(sessionId, map);
      }
      map.put(id, callback);
    }

    CustomEvent evt = (CustomEvent) Browser.getDocument().createEvent("CustomEvent");
    evt.initCustomEvent(DEBUGGER_EXTENSION_REQUEST_EVENT, true, true, Jso.serialize(data));
    Browser.getWindow().dispatchEvent(evt);
  }

  private void handleDebuggerExtensionResponse(ExtensionResponse response) {
    if (maybeInvokeCallbackForResponse(response)) {
      return;
    }
    if (maybeHandleDebuggerCustomMessageResponse(response)) {
      return;
    }

    final String sessionId = response.sessionId();
    final String methodName = response.methodName();
    final Jso request = response.request();
    final Jso result = response.result();

    Log.debug(getClass(), "Received debugger message: " + methodName);

    if (response.isError()) {
      Log.debug(getClass(), "Received debugger error message: " + response.errorMessage());
      handleDebuggerExtensionErrorResponse(sessionId, methodName);
      return;
    }

    if (EVENT_DEBUGGER_SCRIPT_PARSED.equals(methodName)) { // The most frequent event.
      final OnScriptParsedResponse parsedResponse =
          DebuggerChromeApiUtils.parseOnScriptParsedResponse(result);
      if (parsedResponse != null) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onScriptParsed(sessionId, parsedResponse);
          }
        });
      }
    } else if (METHOD_CONSOLE_MESSAGE_ADDED.equals(methodName)) {
      final ConsoleMessage consoleMessage =
          DebuggerChromeApiUtils.parseOnConsoleMessageReceived(result);
      if (consoleMessage != null) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onConsoleMessage(sessionId, consoleMessage);
          }
        });
      }
    } else if (METHOD_CONSOLE_MESSAGE_REPEAT_COUNT_UPDATED.equals(methodName)) {
      final int repeatCount =
          DebuggerChromeApiUtils.parseOnConsoleMessageRepeatCountUpdated(result);
      if (repeatCount != -1) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onConsoleMessageRepeatCountUpdated(sessionId, repeatCount);
          }
        });
      }
    } else if (EVENT_DEBUGGER_PAUSED.equals(methodName)) {
      final OnPausedResponse parsedResponse = DebuggerChromeApiUtils.parseOnPausedResponse(result);
      if (parsedResponse != null) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onPaused(sessionId, parsedResponse);
          }
        });
      }
    } else if (EVENT_DEBUGGER_RESUMED.equals(methodName)) {
      dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
        @Override
        public void dispatch(DebuggerResponseListener responseListener) {
          responseListener.onResumed(sessionId);
        }
      });
    } else if (METHOD_DEBUGGER_SET_BREAKPOINT_BY_URL.equals(methodName)
        || EVENT_DEBUGGER_BREAKPOINT_RESOLVED.equals(methodName)) {
      final OnBreakpointResolvedResponse parsedResponse =
          DebuggerChromeApiUtils.parseOnBreakpointResolvedResponse(request, result);
      if (parsedResponse != null) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onBreakpointResolved(sessionId, parsedResponse);
          }
        });
      }
    } else if (METHOD_DEBUGGER_REMOVE_BREAKPOINT.equals(methodName)) {
      final String breakpointId = DebuggerChromeApiUtils.parseOnRemoveBreakpointResponse(request);
      if (breakpointId != null) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onBreakpointRemoved(sessionId, breakpointId);
          }
        });
      }
    } else if (METHOD_ON_ATTACH.equals(methodName) || METHOD_WINDOW_OPEN.equals(methodName)) {
      dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
        @Override
        public void dispatch(DebuggerResponseListener responseListener) {
          responseListener.onDebuggerAttached(sessionId);
        }
      });
    } else if (METHOD_ON_DETACH.equals(methodName) || METHOD_WINDOW_CLOSE.equals(methodName)) {
      dispatchOnDebuggerDetachedEvent(sessionId);
    } else if (METHOD_ON_GLOBAL_OBJECT_CHANGED.equals(methodName)) {
      dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
        @Override
        public void dispatch(DebuggerResponseListener responseListener) {
          responseListener.onGlobalObjectChanged(sessionId);
        }
      });
    } else if (METHOD_RUNTIME_GET_PROPERTIES.equals(methodName)) {
      final OnRemoteObjectPropertiesResponse parsedResponse =
          DebuggerChromeApiUtils.parseOnRemoteObjectPropertiesResponse(request, result);
      if (parsedResponse != null) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onRemoteObjectPropertiesResponse(sessionId, parsedResponse);
          }
        });
      }
    } else if (METHOD_RUNTIME_EVALUATE.equals(methodName)
        || METHOD_DEBUGGER_EVALUATE_ON_CALL_FRAME.equals(methodName)) {
      final OnEvaluateExpressionResponse parsedResponse =
          DebuggerChromeApiUtils.parseOnEvaluateExpressionResponse(request, result);
      if (parsedResponse != null) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onEvaluateExpressionResponse(sessionId, parsedResponse);
          }
        });
      }
    } else if (METHOD_CSS_GET_ALL_STYLE_SHEETS.equals(methodName)) {
      final OnAllCssStyleSheetsResponse parsedResponse =
          DebuggerChromeApiUtils.parseOnAllCssStyleSheetsResponse(result);
      if (parsedResponse != null) {
        dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
          @Override
          public void dispatch(DebuggerResponseListener responseListener) {
            responseListener.onAllCssStyleSheetsResponse(sessionId, parsedResponse);
          }
        });
      }
    } else if (METHOD_ON_EXTENSION_INSTALLED_CHANGED.equals(methodName)) {
      dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
        @Override
        public void dispatch(DebuggerResponseListener responseListener) {
          responseListener.onDebuggerAvailableChanged();
        }
      });
    } else if (METHOD_CONSOLE_MESSAGES_CLEARED.equals(methodName)) {
      dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
        @Override
        public void dispatch(DebuggerResponseListener responseListener) {
          responseListener.onConsoleMessagesCleared(sessionId);
        }
      });
    } else {
      Log.warn(getClass(), "Ignoring debugger message: " + methodName);
    }
  }

  private boolean maybeInvokeCallbackForResponse(ExtensionResponse response) {
    final int messageId = response.messageId();
    final String sessionId = response.sessionId();

    if (callbacks.get(sessionId) != null && callbacks.get(sessionId).hasKey(messageId)) {
      JsIntegerMap<Callback> map = callbacks.get(sessionId);
      Callback callback = map.get(messageId);
      map.erase(messageId);

      callback.run(response);
      return true;
    }

    return false;
  }

  private boolean maybeHandleDebuggerCustomMessageResponse(ExtensionResponse response) {
    final int messageId = response.messageId();
    final String sessionId = response.sessionId();
    final String methodName = response.methodName();
    final Jso result = response.result();

    if (idToCustomMessageIds.get(sessionId) != null
        && idToCustomMessageIds.get(sessionId).hasKey(messageId)) {
      JsIntegerMap<Integer> map = idToCustomMessageIds.get(sessionId);

      Jso customResponse = Jso.create();
      customResponse.addField("id", map.get(messageId).intValue());
      customResponse.addField("result", result);
      if (response.isError()) {
        customResponse.addField("error", response.errorMessage());
      }
      final String customResponseSerialized = Jso.serialize(customResponse);

      map.erase(messageId);

      dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
        @Override
        public void dispatch(DebuggerResponseListener responseListener) {
          responseListener.onCustomMessageResponse(sessionId, customResponseSerialized);
        }
      });
      return true;
    } else if (methodName.startsWith("DOM.")) {
      Jso customResponse = Jso.create();
      customResponse.addField("method", methodName);
      customResponse.addField("params", result);
      if (response.isError()) {
        customResponse.addField("error", response.errorMessage());
      }
      final String customResponseSerialized = Jso.serialize(customResponse);

      dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
        @Override
        public void dispatch(DebuggerResponseListener responseListener) {
          responseListener.onCustomMessageResponse(sessionId, customResponseSerialized);
        }
      });
      return true;
    }

    return false;
  }

  private void handleDebuggerExtensionErrorResponse(String sessionId, String methodName) {
    // Dispatch the onDebuggerDetached internal event for each failed method
    // that might have actually detached the remote debugger.
    if (METHOD_ON_ATTACH.equals(methodName) || METHOD_WINDOW_OPEN.equals(methodName)
        || METHOD_ON_DETACH.equals(methodName) || METHOD_WINDOW_CLOSE.equals(methodName)) {
      dispatchOnDebuggerDetachedEvent(sessionId);
    }
  }

  private void dispatchOnDebuggerDetachedEvent(final String sessionId) {
    // Clear cached data for the debugger session.
    idToCustomMessageIds.remove(sessionId);
    callbacks.remove(sessionId);

    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onDebuggerDetached(sessionId);
      }
    });
  }

  private void dispatchOnRemoteObjectPropertyChanged(final String sessionId,
      final OnRemoteObjectPropertyChanged parsedResponse) {
    dispatchDebuggerResponse(new DebuggerResponseDispatcher() {
      @Override
      public void dispatch(DebuggerResponseListener responseListener) {
        responseListener.onRemoteObjectPropertyChanged(sessionId, parsedResponse);
      }
    });
  }

  private void dispatchDebuggerResponse(DebuggerResponseDispatcher dispatcher) {
    JsonArray<DebuggerResponseListener> copy = debuggerResponseListeners.copy();
    for (int i = 0, n = copy.size(); i < n; i++) {
      dispatcher.dispatch(copy.get(i));
    }
  }
}
