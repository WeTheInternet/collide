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
import com.google.collide.client.code.debugging.DebuggerApiTypes.CssStyleSheetHeader;
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

/**
 * Defines an API to communicate with the browser debugger.
 *
 * <p>This API allows to arrange multiple debugging sessions simultaneously.
 * Each debugging session is identified by its session ID. For example, we may
 * provide a workspace ID as a debugging session ID, so that debugging requests
 * from different IDE instances (browser tabs) should go to the same debuggee
 * instance. Otherwise, debugging session IDs may be unique across different
 * workspaces and/or different IDE instances.
 */
interface DebuggerApi {

  /**
   * @return true if the debugger is available
   */
  public boolean isDebuggerAvailable();

  /**
   * @return URL of the browser extension that provides the debugging API,
   *         or {@code null} if no such extension is available
   */
  public String getDebuggingExtensionUrl();
  
  /**
   * Runs the debugger on a given URL. This may open a new window or tab to run
   * the debugger.
   *
   * @param sessionId ID of the debugging session
   * @param url URL to run the debugger on
   */
  public void runDebugger(String sessionId, String url);

  /**
   * Shuts down a given debugger session. This may close the corresponding
   * debuggee window or tab.
   *
   * <p>To reestablish the debugging session, just call the
   * {@link #runDebugger} again.
   *
   * @param sessionId ID of the debugging session
   */
  public void shutdownDebugger(String sessionId);

  /**
   * Sets a JavaScript breakpoint at a given location.
   *
   * @param sessionId ID of the debugging session
   * @param breakpointInfo breakpoint data
   */
  public void setBreakpointByUrl(String sessionId, BreakpointInfo breakpointInfo);

  /**
   * Removes a JavaScript breakpoint by the resolved breakpoint ID, that comes
   * from the {@link DebuggerResponseListener#onBreakpointResolved} event.
   *
   * @param sessionId ID of the debugging session
   * @param breakpointId ID of the breakpoint to remove
   */
  public void removeBreakpoint(String sessionId, String breakpointId);

  /**
   * Activates/deactivates all breakpoints on the debuggee page.
   *
   * @param sessionId ID of the debugging session
   * @param active new value for breakpoints active state
   */
  public void setBreakpointsActive(String sessionId, boolean active);
  
  /**
   * Defines pause on exceptions state. Can be set to stop on all exceptions,
   * uncaught exceptions or no exceptions.
   *
   * @param sessionId ID of the debugging session
   * @param state pause on exceptions state
   */
  public void setPauseOnExceptions(String sessionId, PauseOnExceptionsState state);

  /**
   * Stops debugger on the next JavaScript statement.
   *
   * @param sessionId ID of the debugging session
   */
  public void pause(String sessionId);

  /**
   * Resumes debugger execution.
   *
   * @param sessionId ID of the debugging session
   */
  public void resume(String sessionId);

  /**
   * Steps debugger into the statement.
   *
   * @param sessionId ID of the debugging session
   */
  public void stepInto(String sessionId);

  /**
   * Steps debugger out of the function.
   *
   * @param sessionId ID of the debugging session
   */
  public void stepOut(String sessionId);

  /**
   * Steps debugger over the statement.
   *
   * @param sessionId ID of the debugging session
   */
  public void stepOver(String sessionId);

  /**
   * Requests properties of a given {@link RemoteObject}.
   *
   * <p>This will request only properties in the object's prototype (in terms
   * of the {@code hasOwnProperty} method).
   *
   * @param sessionId ID of the debugging session
   * @param remoteObjectId ID of the remote object to request the properties for
   */
  public void requestRemoteObjectProperties(String sessionId, RemoteObjectId remoteObjectId);

  /**
   * Sets the property with a given name in a given {@link RemoteObject}
   * equal to a given expression, evaluated on the global object.
   *
   * @param sessionId ID of the debugging session
   * @param remoteObjectId ID of the remote object to set the property on
   * @param propertyName property name to set the value for
   * @param propertyValueExpression expression to evaluate and set as the value
   * @see #evaluateExpression
   */
  public void setRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String propertyName, String propertyValueExpression);

  /**
   * Sets the property with a given name in a given {@link RemoteObject}
   * equal to a given expression, evaluated on a given call frame.
   *
   * @param sessionId ID of the debugging session
   * @param callFrame the call frame to evaluate on. This is a part of the
   *        {@link DebuggerResponseListener#onPaused} response
   * @param remoteObjectId ID of the remote object to set the property on
   * @param propertyName property name to set the value for
   * @param propertyValueExpression expression to evaluate and set as the value
   * @see #evaluateExpressionOnCallFrame
   */
  public void setRemoteObjectPropertyEvaluatedOnCallFrame(String sessionId, CallFrame callFrame,
      RemoteObjectId remoteObjectId, String propertyName, String propertyValueExpression);

  /**
   * Removes a property with the given name from the given {@link RemoteObject}.
   *
   * @param sessionId ID of the debugging session
   * @param remoteObjectId ID of the remote object to set the property on
   * @param propertyName property name to remove
   */
  public void removeRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String propertyName);

  /**
   * Renames a given property within a {@link RemoteObject}.
   *
   * @param sessionId ID of the debugging session
   * @param remoteObjectId ID of the remote object to rename the property for
   * @param oldName old property name
   * @param newName new property name
   */
  public void renameRemoteObjectProperty(String sessionId, RemoteObjectId remoteObjectId,
      String oldName, String newName);

  /**
   * Evaluates a given expression on the global object.
   *
   * <p>Note: The evaluation result is valid until the
   * {@link DebuggerResponseListener#onGlobalObjectChanged} is fired.
   *
   * @param sessionId ID of the debugging session
   * @param expression expression to evaluate
   */
  public void evaluateExpression(String sessionId, String expression);

  /**
   * Evaluates expression on a given call frame.
   *
   * <p>API design notes: a {@link CallFrame} type was chosen as an argument
   * instead of a {@link String} {@code callFrameId}, so that to give extra
   * flexibility to the implementers of this interface, as some APIs may only
   * support evaluation on a given {@link RemoteObject}. In this case, the
   * {@code callFrame} object should provide all the necessary information.
   *
   * @param sessionId ID of the debugging session
   * @param callFrame the call frame to evaluate on. This is a part of the
   *        {@link DebuggerResponseListener#onPaused} response
   * @param expression expression to evaluate
   */
  public void evaluateExpressionOnCallFrame(String sessionId, CallFrame callFrame,
      String expression);

  /**
   * Requests {@link CssStyleSheetHeader} entries for all known stylesheets.
   *
   * @param sessionId ID of the debugging session
   */
  public void requestAllCssStyleSheets(String sessionId);

  /**
   * Sets the new text for a given stylesheet.
   *
   * @param sessionId ID of the debugging session
   * @param styleSheetId ID of the stylesheet to modify
   * @param text the text to set as CSS rules
   */
  public void setStyleSheetText(String sessionId, String styleSheetId, String text);

  /**
   * Sends a custom raw message to the debugger.
   *
   * @param sessionId ID of the debugging session
   * @param message raw message to send to the debugger
   */
  public void sendCustomMessage(String sessionId, String message);

  /**
   * Adds a given {@link DebuggerResponseListener}.
   *
   * @param debuggerResponseListener the listener to add
   */
  public void addDebuggerResponseListener(DebuggerResponseListener debuggerResponseListener);

  /**
   * Removes a given {@link DebuggerResponseListener}.
   *
   * @param debuggerResponseListener the listener to remove
   */
  public void removeDebuggerResponseListener(DebuggerResponseListener debuggerResponseListener);

  /**
   * Defines an API of the debugger responses.
   */
  public interface DebuggerResponseListener {

    /**
     * Fired when debugger becomes available or unavailable.
     *
     * <p>To find out the most recent debugger state call the
     * {@link #isDebuggerAvailable} method.
     */
    public void onDebuggerAvailableChanged();

    /**
     * Fired when debugger is attached to the debuggee application.
     *
     * @param sessionId ID of the debugging session
     */
    public void onDebuggerAttached(String sessionId);

    /**
     * Fired when debugger is detached from the debuggee application.
     *
     * <p>This may happen, for example, if the debuggee application was
     * terminated, or the browser extension providing the debugging API was
     * disabled, or for any other reason.
     *
     * @param sessionId ID of the debugging session
     */
    public void onDebuggerDetached(String sessionId);

    /**
     * Fired when a breakpoint is resolved by the debugger.
     *
     * <p>This event may be fired several times for a single breakpoint as long
     * as any new information becomes available to the debugger.
     *
     * @param sessionId ID of the debugging session
     * @param response debugger response
     */
    public void onBreakpointResolved(String sessionId, OnBreakpointResolvedResponse response);

    /**
     * Fired when a breakpoint is removed by the debugger.
     *
     * @param sessionId ID of the debugging session
     * @param breakpointId ID of the breakpoint that got removed
     */
    public void onBreakpointRemoved(String sessionId, String breakpointId);

    /**
     * Fired when debugger stops on a breakpoint or exception, or because of
     * any other stop criteria.
     *
     * @param sessionId ID of the debugging session
     * @param response debugger response
     */
    public void onPaused(String sessionId, OnPausedResponse response);

    /**
     * Fired when debugger resumes execution.
     *
     * @param sessionId ID of the debugging session
     */
    public void onResumed(String sessionId);

    /**
     * Fired when debugger parses a script. This event is also fired for all
     * known scripts upon enabling debugger.
     *
     * @param sessionId ID of the debugging session
     * @param response debugger response
     */
    public void onScriptParsed(String sessionId, OnScriptParsedResponse response);

    /**
     * Fired as a response to the {@link #requestRemoteObjectProperties} method
     * call.
     *
     * @param sessionId ID of the debugging session
     * @param response debugger response
     */
    public void onRemoteObjectPropertiesResponse(String sessionId,
        OnRemoteObjectPropertiesResponse response);

    /**
     * Fired when a property of a {@link RemoteObject} was was edited, renamed
     * or deleted.
     *
     * @param sessionId ID of the debugging session
     * @param response debugger response
     */
    public void onRemoteObjectPropertyChanged(String sessionId,
        OnRemoteObjectPropertyChanged response);

    /**
     * Fired as a response to the {@link #evaluateExpressionOnCallFrame} method
     * call.
     *
     * @param sessionId ID of the debugging session
     * @param response debugger response
     */
    public void onEvaluateExpressionResponse(String sessionId,
        OnEvaluateExpressionResponse response);

    /**
     * Fired when a remote global object (global scope) has changed. This is to
     * notify that all expressions evaluated on the global object are no longer
     * valid, and should be refreshed.
     *
     * @param sessionId ID of the debugging session
     */
    public void onGlobalObjectChanged(String sessionId);

    /**
     * Fired as a response to the {@link #requestAllCssStyleSheets} method call.
     *
     * @param sessionId ID of the debugging session
     * @param response debugger response
     */
    public void onAllCssStyleSheetsResponse(String sessionId, OnAllCssStyleSheetsResponse response);

    /**
     * Fired when a console message is received from the debugger.
     *
     * @param sessionId ID of the debugging session
     * @param message console message
     */
    public void onConsoleMessage(String sessionId, ConsoleMessage message);

    /**
     * Fired when subsequent console message(s) are equal to the previous
     * one(s).
     *
     * @param sessionId ID of the debugging session
     * @param repeatCount new repeat count value
     */
    public void onConsoleMessageRepeatCountUpdated(String sessionId, int repeatCount);

    /**
     * Fired when all console messages should be cleared. This happens either
     * upon manual {@code clearMessages} command or after a page navigation.
     *
     * @param sessionId ID of the debugging session
     */
    public void onConsoleMessagesCleared(String sessionId);

    /**
     * Response for a custom message sent via the {@link #sendCustomMessage}.
     *
     * @param sessionId ID of the debugging session
     * @param response debugger response
     */
    public void onCustomMessageResponse(String sessionId, String response);
  }
}
