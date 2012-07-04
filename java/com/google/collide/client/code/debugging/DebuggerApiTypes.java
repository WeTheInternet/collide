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

import com.google.collide.json.shared.JsonArray;

/**
 * Defines data types used in the browser debugger API {@link DebuggerApi}.
 *
 * <p>The API design of these data types was inspired by the
 * <a href="http://code.google.com/chrome/devtools/docs/remote-debugging.html">
 * Chrome DevTools Remote Debugging API</a>.
 */
class DebuggerApiTypes {

  public static final RemoteObject UNDEFINED_REMOTE_OBJECT = new RemoteObject() {

    @Override
    public String getDescription() {
      return "undefined";
    }

    @Override
    public boolean hasChildren() {
      return false;
    }

    @Override
    public RemoteObjectId getObjectId() {
      return null;
    }

    @Override
    public RemoteObjectType getType() {
      return RemoteObjectType.UNDEFINED;
    }

    @Override
    public RemoteObjectSubType getSubType() {
      return null;
    }
  };

  /**
   * Represents a breakpoint.
   */
  public interface BreakpointInfo {

    /**
     * @return breakpoint's URL. Either {@link #getUrl} or {@link #getUrlRegex}
     *         will be specified
     */
    public String getUrl();

    /**
     * @return a Regex pattern for the URLs of the resources to set breakpoints
     *         on. Either {@link #getUrl} or {@link #getUrlRegex} will be
     *         specified
     */
    public String getUrlRegex();

    /**
     * @return breakpoint's line number
     */
    public int getLineNumber();

    /**
     * @return breakpoint's column number
     */
    public int getColumnNumber();

    /**
     * @return breakpoint's condition
     */
    public String getCondition();
  }

  /**
   * Debugger response that is fired when a breakpoint is resolved.
   */
  public interface OnBreakpointResolvedResponse {

    /**
     * @return breakpoint info
     */
    public BreakpointInfo getBreakpointInfo();

    /**
     * @return breakpoint unique identifier
     */
    public String getBreakpointId();

    /**
     * @return actual breakpoint locations. These may be many if, for example,
     *         the script that contains the breakpoint is included more than
     *         once into an application
     */
    public JsonArray<Location> getLocations();
  }

  /**
   * Debugger response that is fired when debugger stops execution.
   */
  public interface OnPausedResponse {

    /**
     * @return call stack the debugger stopped on, so that the topmost call
     *         frame is at index {@code 0}
     */
    public JsonArray<CallFrame> getCallFrames();
  }

  /**
   * Debugger response that is fired when debugger parses a script.
   */
  public interface OnScriptParsedResponse {

    /**
     * @return line offset of the script within the resource with a given URL
     *         (for script tags)
     */
    public int getStartLine();

    /**
     * @return column offset of the script within the resource with a given URL
     */
    public int getStartColumn();

    /**
     * @return last line of the script
     */
    public int getEndLine();

    /**
     * @return length of the last line of the script
     */
    public int getEndColumn();

    /**
     * @return URL of the script parsed (if any)
     */
    public String getUrl();

    /**
     * @return identifier of the parsed script
     */
    public String getScriptId();

    /**
     * @return whether this script is a user extension script (browser
     *         extension's script, Grease Monkey script and etc.)
     */
    public boolean isContentScript();
  }

  /**
   * Debugger response that gives an array of properties of a remote object.
   */
  public interface OnRemoteObjectPropertiesResponse {

    /**
     * @return unique object identifier of the {@link RemoteObject}
     */
    public RemoteObjectId getObjectId();

    /**
     * @return array of properties of the remote object
     */
    public JsonArray<PropertyDescriptor> getProperties();
  }

  /**
   * Debugger response that is fired when a property of a {@link RemoteObject}
   * was edited, renamed or deleted.
   */
  public interface OnRemoteObjectPropertyChanged {

    /**
     * @return unique object identifier of the {@link RemoteObject}
     */
    public RemoteObjectId getObjectId();

    /**
     * @return old property name
     */
    public String getOldName();

    /**
     * @return new property name, or {@code null} if the property was deleted
     */
    public String getNewName();

    /**
     * @return true if property value has changed
     */
    public boolean isValueChanged();

    /**
     * @return property value if available, or {@code null} otherwise (in this
     *         case it should be explicitly requested via the corresponding
     *         Debugger API call, if needed)
     */
    public RemoteObject getValue();

    /**
     * @return true if an exception was thrown during the evaluation. In this
     *         case the result of the corresponding remote call is undefined,
     *         and it is recommended to request object properties again
     */
    public boolean wasThrown();
  }

  /**
   * Debugger response with the result of an evaluated expression.
   */
  public interface OnEvaluateExpressionResponse {
    
    /**
     * @return the expression that was evaluated
     */
    public String getExpression();

    /**
     * @return ID of the {@link CallFrame} the expression was evaluated on, or
     *         {@code null} if expression was evaluated on the global object
     */
    public String getCallFrameId();

    /**
     * @return evaluation result
     */
    public RemoteObject getResult();

    /**
     * @return true if an exception was thrown during the evaluation. In this
     *         case the evaluation result will contain the thrown value
     */
    public boolean wasThrown();
  }

  /**
   * Debugger response with the metainfo about all known CSS stylesheets.
   */
  public interface OnAllCssStyleSheetsResponse {

    /**
     * @return Descriptor headers for all available CSS stylesheets
     */
    public JsonArray<CssStyleSheetHeader> getHeaders();
  }

  /**
   * Represents a remote object in the context of debuggee application.
   * Inspired by:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/runtime.html#type-RemoteObject
   *
   * <p>WARNING: If you modify this interface, also check out the
   * {@link DebuggerApiUtils#equal} method!
   */
  public interface RemoteObject {

    /**
     * @return string representation of the object
     */
    public String getDescription();

    /**
     * @return true when this object can be queried for children
     */
    public boolean hasChildren();

    /**
     * @return unique object identifier for non-primitive values,
     *         or {@code null} for primitive values
     */
    public RemoteObjectId getObjectId();

    /**
     * @return object type
     */
    public RemoteObjectType getType();

    /**
     * @return object subtype
     */
    public RemoteObjectSubType getSubType();
  }

  /**
   * Abstraction for the {@link RemoteObject} ID.
   */
  public interface RemoteObjectId {

    /**
     * Returns a string representation of the {@link RemoteObject} ID.
     */
    @Override
    public String toString();
  }

  /**
   * Remote object's type.
   */
  public enum RemoteObjectType {
    BOOLEAN, FUNCTION, NUMBER, OBJECT, STRING, UNDEFINED
  }

  /**
   * Remote object's subtype. Specified for {@link RemoteObjectType#OBJECT}
   * objects only.
   */
  public enum RemoteObjectSubType {
    ARRAY, DATE, NODE, NULL, REGEXP
  }

  /**
   * Represents a property of a {@link RemoteObject}.
   * Inspired by:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/runtime.html#type-PropertyDescriptor
   */
  public interface PropertyDescriptor {

    /**
     * @return property name
     */
    public String getName();

    /**
     * @return property value, or the value that was thrown by an exception
     *         if {@link #wasThrown} returns {@code true}, or {@code null} if
     *         no value was associated with the property
     */
    public RemoteObject getValue();

    /**
     * @return true if an exception was thrown on the attempt to get the
     *         property value. In this case the value property will contain
     *         the thrown value
     */
    public boolean wasThrown();

    /**
     * @return true if the type of this property descriptor may be changed and
     *         if the property may be deleted from the corresponding object
     */
    public boolean isConfigurable();

    /**
     * @return true if this property shows up during enumeration of the
     *         properties on the corresponding object
     */
    public boolean isEnumerable();

    /**
     * @return true if the value associated with the property may be changed
     */
    public boolean isWritable();

    /**
     * @return a function which serves as a getter for the property, or
     *         {@code null} if there is no getter
     */
    public RemoteObject getGetterFunction();

    /**
     * @return a function which serves as a setter for the property, or
     *         {@code null} if there is no setter
     */
    public RemoteObject getSetterFunction();
  }

  /**
   * A call frame. Inspired by:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/debugger.html#type-CallFrame
   */
  public interface CallFrame {

    /**
     * @return name of the function called on this frame
     */
    public String getFunctionName();

    /**
     * @return call frame identifier
     */
    public String getId();

    /**
     * @return location in the source code
     */
    public Location getLocation();

    /**
     * @return scope chain for given call frame
     */
    public JsonArray<Scope> getScopeChain();

    /**
     * @return {@code this} object for this call frame
     */
    public RemoteObject getThis();
  }

  /**
   * An arbitrary location in a script. Inspired by:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/debugger.html#type-Location
   */
  public interface Location {

    /**
     * @return column number in the script
     */
    public int getColumnNumber();

    /**
     * @return line number in the script
     */
    public int getLineNumber();

    /**
     * @return script identifier as reported by the {@code scriptParsed} event
     */
    public String getScriptId();
  }

  /**
   * Represents JavaScript's scope. Inspired by:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/debugger.html#type-Scope
   */
  public interface Scope {

    /**
     * @return object representing the scope
     */
    public RemoteObject getObject();

    /**
     * @return true if the object representing the scope is an artificial
     *         transient object used to enumerate the scope variables as its
     *         properties, or false if it represents the actual object in the
     *         Debugger VM
     */
    public boolean isTransient();

    /**
     * @return scope type
     */
    public ScopeType getType();
  }

  /**
   * JavaScript's scope type.
   */
  public enum ScopeType {
    CATCH, CLOSURE, GLOBAL, LOCAL, WITH
  }

  /**
   * Pause on exceptions state.
   */
  public enum PauseOnExceptionsState {
    ALL, NONE, UNCAUGHT
  }

  /**
   * Descriptor of a CSS stylesheet. Inspired by:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/css.html#type-CSSStyleSheetHeader
   */
  public interface CssStyleSheetHeader {

    /**
     * @return whether the stylesheet is disabled
     */
    public boolean isDisabled();

    /**
     * @return stylesheet ID
     */
    public String getId();

    /**
     * @return stylesheet resource URL
     */
    public String getUrl();

    /**
     * @return stylesheet title
     */
    public String getTitle();
  }

  /**
   * Represents a remote Console message. Inspired by:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/console.html#type-ConsoleMessage
   */
  public interface ConsoleMessage {

    /**
     * @return message severity level
     */
    public ConsoleMessageLevel getLevel();

    /**
     * @return Console API message type, or {@code null} if not appropriate
     *         or not supported
     */
    public ConsoleMessageType getType();

    /**
     * @return line number in the resource that generated this message,
     *         or {@code -1} if not appropriate
     */
    public int getLineNumber();

    /**
     * @return array of message parameters in case of a formatted message, or
     *         empty array otherwise
     */
    public JsonArray<RemoteObject> getParameters();

    /**
     * @return repeat count for repeated messages, or {@code 1} for a single
     *         message
     */
    public int getRepeatCount();

    /**
     * @return JavaScript stack trace for assertions and error messages, or
     *         empty array if not appropriate
     */
    public JsonArray<StackTraceItem> getStackTrace();

    /**
     * @return message text
     */
    public String getText();

    /**
     * @return URL of the message origin, or {@code null} if not appropriate
     */
    public String getUrl();
  }

  /**
   * Console message severity level.
   */
  public enum ConsoleMessageLevel {
    DEBUG, ERROR, LOG, TIP, WARNING
  }

  /**
   * Console message type.
   */
  public enum ConsoleMessageType {
    ASSERT, DIR, DIRXML, ENDGROUP, LOG, STARTGROUP, STARTGROUPCOLLAPSED, TRACE
  }

  /**
   * Represents a remote JavaScript stack trace. Inspired by:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/console.html#type-StackTrace
   */
  public interface StackTraceItem {

    /**
     * @return JavaScript script column number
     */
    public int getColumnNumber();

    /**
     * @return JavaScript function name
     */
    public String getFunctionName();

    /**
     * @return JavaScript script line number
     */
    public int getLineNumber();

    /**
     * @return JavaScript script name or URL
     */
    public String getUrl();

  }

  private DebuggerApiTypes() {} // COV_NF_LINE
}
