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
import com.google.collide.client.code.debugging.DebuggerApiTypes.ConsoleMessageLevel;
import com.google.collide.client.code.debugging.DebuggerApiTypes.ConsoleMessageType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.CssStyleSheetHeader;
import com.google.collide.client.code.debugging.DebuggerApiTypes.Location;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnAllCssStyleSheetsResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnBreakpointResolvedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnEvaluateExpressionResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnPausedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertiesResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertyChanged;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnScriptParsedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.PropertyDescriptor;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectId;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectSubType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.Scope;
import com.google.collide.client.code.debugging.DebuggerApiTypes.ScopeType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.StackTraceItem;
import com.google.collide.json.client.Jso;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonObject;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;

/**
 * Utility class to deal with the Chrome debugger responses.
 */
class DebuggerChromeApiUtils {

  /**
   * Parses the {@link OnPausedResponse} debugger response.
   *
   * @param result debugger result
   * @return new instance, or {@code null} for invalid {@code result}
   */
  public static OnPausedResponse parseOnPausedResponse(Jso result) {
    if (result == null) {
      return null;
    }

    final JsonArray<CallFrame> callFrames = parseCallFramesArray(result.getArrayField(
        "callFrames"));

    return new OnPausedResponse() {

      @Override
      public JsonArray<CallFrame> getCallFrames() {
        return callFrames;
      }
    };
  }

  /**
   * Parses the {@link OnBreakpointResolvedResponse} debugger response.
   *
   * @param request original request data that was sent to the debugger
   * @param result debugger result
   * @return new instance, or {@code null} for invalid {@code result}
   */
  public static OnBreakpointResolvedResponse parseOnBreakpointResolvedResponse(Jso request,
      Jso result) {
    if (result == null) {
      return null;
    }

    final JsonArray<Location> locations = parseBreakpointLocations(result);
    final BreakpointInfo breakpointInfo = parseBreakpointInfo(request);
    final String breakpointId = result.getStringField("breakpointId");

    return new OnBreakpointResolvedResponse() {

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
    };
  }

  /**
   * Parses the debugger response that is fired when a breakpoint got removed.
   *
   * @param request original request data that was sent to the debugger
   * @return ID of the breakpoint that was removed
   */
  public static String parseOnRemoveBreakpointResponse(Jso request) {
    if (request == null) {
      return null;
    }
    return request.getStringField("breakpointId");
  }

  /**
   * Parses the {@link OnScriptParsedResponse} debugger response.
   *
   * @param result debugger result
   * @return new instance, or {@code null} for invalid {@code result}
   */
  public static OnScriptParsedResponse parseOnScriptParsedResponse(final Jso result) {
    if (result == null) {
      return null;
    }

    return new OnScriptParsedResponse() {

      @Override
      public int getStartLine() {
        return result.getFieldCastedToInteger("startLine");
      }

      @Override
      public int getStartColumn() {
        return result.getFieldCastedToInteger("startColumn");
      }

      @Override
      public int getEndLine() {
        return result.getFieldCastedToInteger("endLine");
      }

      @Override
      public int getEndColumn() {
        return result.getFieldCastedToInteger("endColumn");
      }

      @Override
      public String getUrl() {
        return result.getStringField("url");
      }

      @Override
      public String getScriptId() {
        return result.getStringField("scriptId");
      }

      @Override
      public boolean isContentScript() {
        return result.getFieldCastedToBoolean("isContentScript");
      }
    };
  }

  /**
   * Parses the {@link OnRemoteObjectPropertiesResponse} debugger response.
   *
   * @param request original request data that was sent to the debugger
   * @param result debugger result
   * @return new instance, or {@code null} for invalid {@code request} or
   *         {@code result}
   */
  public static OnRemoteObjectPropertiesResponse parseOnRemoteObjectPropertiesResponse(Jso request,
      Jso result) {
    if (request == null || result == null) {
      return null;
    }

    final RemoteObjectId objectId = parseRemoteObjectId(request.getStringField("objectId"));
    final JsonArray<PropertyDescriptor> properties = parsePropertyDescriptorArray(
        result.getArrayField("result"));

    return new OnRemoteObjectPropertiesResponse() {

      @Override
      public RemoteObjectId getObjectId() {
        return objectId;
      }

      @Override
      public JsonArray<PropertyDescriptor> getProperties() {
        return properties;
      }
    };
  }

  /**
   * Creates a {@link OnRemoteObjectPropertyChanged} debugger response for a
   * deleted remote object property event.
   *
   * @param remoteObjectId ID of the remote object the property was deleted from
   * @param propertyName name of the deleted property
   * @param wasThrown true if an exception was thrown by the debugger
   * @return new instance
   */
  public static OnRemoteObjectPropertyChanged createOnRemoveRemoteObjectPropertyResponse(
      RemoteObjectId remoteObjectId, String propertyName, boolean wasThrown) {
    return createOnRemoteObjectPropertyChangedResponse(
        remoteObjectId, propertyName, null, null, false, wasThrown);
  }

  /**
   * Creates a {@link OnRemoteObjectPropertyChanged} debugger response for a
   * renamed remote object property event.
   *
   * @param remoteObjectId ID of the remote object the property was deleted from
   * @param oldName old property name
   * @param newName new property name
   * @param wasThrown true if an exception was thrown by the debugger
   * @return new instance
   */
  public static OnRemoteObjectPropertyChanged createOnRenameRemoteObjectPropertyResponse(
      RemoteObjectId remoteObjectId, String oldName, String newName, boolean wasThrown) {
    return createOnRemoteObjectPropertyChangedResponse(
        remoteObjectId, oldName, newName, null, false, wasThrown);
  }

  /**
   * Creates a {@link OnRemoteObjectPropertyChanged} debugger response for a
   * edited remote object property event.
   *
   * @param remoteObjectId ID of the remote object the property was deleted from
   * @param propertyName name of the edited property
   * @param propertyValue new property value
   * @param wasThrown true if an exception was thrown by the debugger
   * @return new instance
   */
  public static OnRemoteObjectPropertyChanged createOnEditRemoteObjectPropertyResponse(
      RemoteObjectId remoteObjectId, String propertyName, RemoteObject propertyValue,
      boolean wasThrown) {
    return createOnRemoteObjectPropertyChangedResponse(
        remoteObjectId, propertyName, propertyName, propertyValue, true, wasThrown);
  }

  private static OnRemoteObjectPropertyChanged createOnRemoteObjectPropertyChangedResponse(
      final RemoteObjectId remoteObjectId, final String oldName, final String newName,
      final RemoteObject value, final boolean isValueChanged, final boolean wasThrown) {
    return new OnRemoteObjectPropertyChanged() {

      @Override
      public RemoteObjectId getObjectId() {
        return remoteObjectId;
      }

      @Override
      public String getOldName() {
        return oldName;
      }

      @Override
      public String getNewName() {
        return newName;
      }

      @Override
      public boolean isValueChanged() {
        return isValueChanged;
      }

      @Override
      public RemoteObject getValue() {
        return value;
      }

      @Override
      public boolean wasThrown() {
        return wasThrown;
      }
    };
  }

  /**
   * Parses the {@link OnEvaluateExpressionResponse} debugger response.
   *
   * @param request original request data that was sent to the debugger
   * @param result debugger result
   * @return new instance, or {@code null} for invalid {@code request} or
   *         {@code result}
   */
  public static OnEvaluateExpressionResponse parseOnEvaluateExpressionResponse(Jso request,
      Jso result) {
    if (request == null || result == null) {
      return null;
    }

    final String expression = request.getStringField("expression");
    final String callFrameId = request.getStringField("callFrameId");
    final RemoteObject evaluationResult = parseRemoteObject((Jso) result.getObjectField("result"));
    final boolean wasThrown = result.getFieldCastedToBoolean("wasThrown");

    return new OnEvaluateExpressionResponse() {

      @Override
      public String getExpression() {
        return expression;
      }

      @Override
      public String getCallFrameId() {
        return callFrameId;
      }

      @Override
      public RemoteObject getResult() {
        return evaluationResult;
      }

      @Override
      public boolean wasThrown() {
        return wasThrown;
      }
    };
  }

  /**
   * Parses the {@link OnAllCssStyleSheetsResponse} debugger response.
   *
   * @param result debugger result
   * @return new instance, or {@code null} for invalid {@code result}
   */
  public static OnAllCssStyleSheetsResponse parseOnAllCssStyleSheetsResponse(Jso result) {
    if (result == null) {
      return null;
    }

    final JsonArray<CssStyleSheetHeader> headers = parseCssStyleSheetHeaders(
        result.getArrayField("headers"));

    return new OnAllCssStyleSheetsResponse() {

      @Override
      public JsonArray<CssStyleSheetHeader> getHeaders() {
        return headers;
      }
    };
  }

  /**
   * Parses the {@link DebuggerChromeApi#METHOD_RUNTIME_CALL_FUNCTION_ON}
   * debugger response.
   *
   * @param result debugger result
   * @return new instance, or {@code null} for invalid {@code result}
   */
  public static RemoteObject parseCallFunctionOnResult(Jso result) {
    if (result == null) {
      return null;
    }

    return parseRemoteObject((Jso) result.getObjectField("result"));
  }

  /**
   * Parses the {@link ConsoleMessage} debugger response.
   *
   * @param result debugger result
   * @return new instance, or {@code null} for invalid {@code result}
   */
  public static ConsoleMessage parseOnConsoleMessageReceived(Jso result) {
    if (result == null) {
      return null;
    }

    Jso json = (Jso) result.getObjectField("message");
    final int lineNumber = json.hasOwnProperty("line") ? json.getFieldCastedToInteger("line") : -1;
    final int repeatCount =
        json.hasOwnProperty("repeatCount") ? json.getFieldCastedToInteger("repeatCount") : 1;
    final ConsoleMessageLevel messageLevel = parseConsoleMessageLevel(json.getStringField("level"));
    final ConsoleMessageType messageType = parseConsoleMessageType(json.getStringField("type"));
    final JsonArray<RemoteObject> parameters = parseRemoteObjectArray(
        json.getArrayField("parameters"));
    final JsonArray<StackTraceItem> stackTrace = parseStackTraceItemArray(
        json.getArrayField("stackTrace"));
    final String text = json.getStringField("text");
    final String url = json.getStringField("url");

    return new ConsoleMessage() {

      @Override
      public ConsoleMessageLevel getLevel() {
        return messageLevel;
      }

      @Override
      public ConsoleMessageType getType() {
        return messageType;
      }

      @Override
      public int getLineNumber() {
        return lineNumber;
      }

      @Override
      public JsonArray<RemoteObject> getParameters() {
        return parameters;
      }

      @Override
      public int getRepeatCount() {
        return repeatCount;
      }

      @Override
      public JsonArray<StackTraceItem> getStackTrace() {
        return stackTrace;
      }

      @Override
      public String getText() {
        return text;
      }

      @Override
      public String getUrl() {
        return url;
      }
    };
  }

  /**
   * Parses the debugger response that is fired when subsequent console
   * message(s) are equal to the previous one(s).
   *
   * @param result debugger result
   * @return new repeat count value, or {@code -1} for invalid {@code result}
   */
  public static int parseOnConsoleMessageRepeatCountUpdated(Jso result) {
    if (result == null) {
      return -1;
    }
    return result.getFieldCastedToInteger("count");
  }

  private static JsonArray<PropertyDescriptor> parsePropertyDescriptorArray(
      JsonArray<JsonObject> jsonArray) {
    JsonArray<PropertyDescriptor> result = JsonCollections.createArray();
    if (jsonArray != null) {
      for (int i = 0, n = jsonArray.size(); i < n; ++i) {
        PropertyDescriptor propertyDescriptor = parsePropertyDescriptor((Jso) jsonArray.get(i));
        if (propertyDescriptor != null) {
          result.add(propertyDescriptor);
        }
      }
    }
    return result;
  }

  private static PropertyDescriptor parsePropertyDescriptor(final Jso json) {
    if (json == null) {
      return null;
    }

    final RemoteObject remoteObject = parseRemoteObject((Jso) json.getObjectField("value"));
    final RemoteObject getter = parseRemoteObject((Jso) json.getObjectField("get"));
    final RemoteObject setter = parseRemoteObject((Jso) json.getObjectField("set"));

    return new PropertyDescriptor() {

      @Override
      public String getName() {
        return json.getStringField("name");
      }

      @Override
      public RemoteObject getValue() {
        return remoteObject;
      }

      @Override
      public boolean wasThrown() {
        return json.getFieldCastedToBoolean("wasThrown");
      }

      @Override
      public boolean isConfigurable() {
        return json.getFieldCastedToBoolean("configurable");
      }

      @Override
      public boolean isEnumerable() {
        return json.getFieldCastedToBoolean("enumerable");
      }

      @Override
      public boolean isWritable() {
        return json.getFieldCastedToBoolean("writable");
      }

      @Override
      public RemoteObject getGetterFunction() {
        return getter;
      }

      @Override
      public RemoteObject getSetterFunction() {
        return setter;
      }
    };
  }

  private static JsonArray<CallFrame> parseCallFramesArray(JsonArray<JsonObject> jsonArray) {
    JsonArray<CallFrame> result = JsonCollections.createArray();
    if (jsonArray != null) {
      for (int i = 0, n = jsonArray.size(); i < n; ++i) {
        CallFrame callFrame = parseCallFrame((Jso) jsonArray.get(i));
        if (callFrame != null) {
          result.add(callFrame);
        }
      }
    }
    return result;
  }

  private static CallFrame parseCallFrame(Jso json) {
    if (json == null) {
      return null;
    }

    final String functionName = json.getStringField("functionName");
    final String callFrameId = json.getStringField("callFrameId");
    final Location location = parseLocation((Jso) json.getObjectField("location"));
    final JsonArray<Scope> scopeChain = parseScopeChain(json.getArrayField("scopeChain"));
    final RemoteObject thisObject = parseRemoteObject((Jso) json.getObjectField("this"));

    return new CallFrame() {

      @Override
      public String getFunctionName() {
        return functionName;
      }

      @Override
      public String getId() {
        return callFrameId;
      }

      @Override
      public Location getLocation() {
        return location;
      }

      @Override
      public JsonArray<Scope> getScopeChain() {
        return scopeChain;
      }

      @Override
      public RemoteObject getThis() {
        return thisObject;
      }
    };
  }

  private static Location parseLocation(final Jso json) {
    if (json == null) {
      return null;
    }

    return new Location() {

      @Override
      public int getColumnNumber() {
        return json.getFieldCastedToInteger("columnNumber");
      }

      @Override
      public int getLineNumber() {
        return json.getFieldCastedToInteger("lineNumber");
      }

      @Override
      public String getScriptId() {
        return json.getStringField("scriptId");
      }
    };
  }

  private static JsonArray<Scope> parseScopeChain(JsonArray<JsonObject> jsonArray) {
    JsonArray<Scope> result = JsonCollections.createArray();
    if (jsonArray != null) {
      for (int i = 0, n = jsonArray.size(); i < n; ++i) {
        Scope scope = parseScope(jsonArray.get(i));
        if (scope != null) {
          result.add(scope);
        }
      }
    }
    return result;
  }

  private static Scope parseScope(JsonObject json) {
    if (json == null) {
      return null;
    }

    final RemoteObject object = parseRemoteObject((Jso) json.getObjectField("object"));
    final ScopeType scopeType = parseScopeType(json.getStringField("type"));

    return new Scope() {

      @Override
      public RemoteObject getObject() {
        return object;
      }

      @Override
      public boolean isTransient() {
        // @see http://code.google.com/chrome/devtools/docs/protocol/tot/debugger.html#type-Scope
        // For GLOBAL and WITH scopes it represents the actual object; for the rest of the scopes,
        // it is artificial transient object enumerating scope variables as its properties.
        return scopeType != ScopeType.GLOBAL && scopeType != ScopeType.WITH;
      }

      @Override
      public ScopeType getType() {
        return scopeType;
      }
    };
  }

  private static ScopeType parseScopeType(String type) {
    return type == null ? null : ScopeType.valueOf(type.toUpperCase());
  }

  private static JsonArray<RemoteObject> parseRemoteObjectArray(JsonArray<JsonObject> jsonArray) {
    JsonArray<RemoteObject> result = JsonCollections.createArray();
    if (jsonArray != null) {
      for (int i = 0, n = jsonArray.size(); i < n; ++i) {
        RemoteObject remoteObject = parseRemoteObject((Jso) jsonArray.get(i));
        if (remoteObject != null) {
          result.add(remoteObject);
        }
      }
    }
    return result;
  }

  private static RemoteObject parseRemoteObject(Jso json) {
    if (json == null) {
      return null;
    }

    final RemoteObjectId objectId = parseRemoteObjectId(json.getStringField("objectId"));
    final RemoteObjectType remoteObjectType = parseRemoteObjectType(json.getStringField("type"));
    final RemoteObjectSubType remoteObjectSubType = parseRemoteObjectSubType(
        json.getStringField("subtype"));
    final String description = StringUtils.ensureNotEmpty(json.getStringField("description"),
        json.getFieldCastedToString("value"));

    return new RemoteObject() {

      @Override
      public String getDescription() {
        return description;
      }

      @Override
      public boolean hasChildren() {
        return objectId != null;
      }

      @Override
      public RemoteObjectId getObjectId() {
        return objectId;
      }

      @Override
      public RemoteObjectType getType() {
        return remoteObjectType;
      }

      @Override
      public RemoteObjectSubType getSubType() {
        return remoteObjectSubType;
      }
    };
  }

  private static RemoteObjectType parseRemoteObjectType(String type) {
    return type == null ? null : RemoteObjectType.valueOf(type.toUpperCase());
  }

  private static RemoteObjectSubType parseRemoteObjectSubType(String subtype) {
    return subtype == null ? null : RemoteObjectSubType.valueOf(subtype.toUpperCase());
  }

  private static RemoteObjectId parseRemoteObjectId(final String objectId) {
    if (StringUtils.isNullOrEmpty(objectId)) {
      return null;
    }

    return new RemoteObjectId() {

      @Override
      public String toString() {
        return objectId;
      }
    };
  }

  /**
   * Handles both "Debugger.setBreakpointByUrl" and "Debugger.breakpointResolved" responses.
   *
   * Docs:
   * http://code.google.com/chrome/devtools/docs/protocol/tot/debugger.html#command-setBreakpointByUrl
   * http://code.google.com/chrome/devtools/docs/protocol/tot/debugger.html#event-breakpointResolved
   */
  private static JsonArray<Location> parseBreakpointLocations(Jso json) {
    JsonArray<Location> result = JsonCollections.createArray();

    if (json != null) {
      // Debugger.breakpointResolved response.
      Location location = parseLocation((Jso) json.getObjectField("location"));
      if (location != null) {
        result.add(location);
      }

      // Debugger.setBreakpointByUrl response.
      JsonArray<JsonObject> jsonArray = json.getArrayField("locations");
      if (jsonArray != null) {
        for (int i = 0, n = jsonArray.size(); i < n; ++i) {
          location = parseLocation((Jso) jsonArray.get(i));
          if (location != null) {
            result.add(location);
          }
        }
      }
    }

    return result;
  }

  private static BreakpointInfo parseBreakpointInfo(final Jso json) {
    if (json == null) {
      return null;
    }

    return new BreakpointInfo() {

      @Override
      public String getUrl() {
        return json.getStringField("url");
      }

      @Override
      public String getUrlRegex() {
        return json.getStringField("urlRegex");
      }

      @Override
      public int getLineNumber() {
        return json.getFieldCastedToInteger("lineNumber");
      }

      @Override
      public int getColumnNumber() {
        return json.getFieldCastedToInteger("columnNumber");
      }

      @Override
      public String getCondition() {
        return json.getStringField("condition");
      }
    };
  }

  private static JsonArray<CssStyleSheetHeader> parseCssStyleSheetHeaders(
      JsonArray<JsonObject> jsonArray) {
    JsonArray<CssStyleSheetHeader> result = JsonCollections.createArray();
    if (jsonArray != null) {
      for (int i = 0, n = jsonArray.size(); i < n; ++i) {
        CssStyleSheetHeader header = parseCssStyleSheetHeader((Jso) jsonArray.get(i));
        if (header != null) {
          result.add(header);
        }
      }
    }
    return result;
  }

  private static CssStyleSheetHeader parseCssStyleSheetHeader(final Jso json) {
    if (json == null) {
      return null;
    }

    return new CssStyleSheetHeader() {

      @Override
      public boolean isDisabled() {
        return json.getFieldCastedToBoolean("disabled");
      }

      @Override
      public String getId() {
        return json.getStringField("styleSheetId");
      }

      @Override
      public String getUrl() {
        return json.getStringField("sourceURL");
      }

      @Override
      public String getTitle() {
        return json.getStringField("title");
      }
    };
  }

  private static ConsoleMessageLevel parseConsoleMessageLevel(String level) {
    return level == null ? null : ConsoleMessageLevel.valueOf(level.toUpperCase());
  }

  private static ConsoleMessageType parseConsoleMessageType(String type) {
    return type == null ? null : ConsoleMessageType.valueOf(type.toUpperCase());
  }

  private static JsonArray<StackTraceItem> parseStackTraceItemArray(
      JsonArray<JsonObject> jsonArray) {
    JsonArray<StackTraceItem> result = JsonCollections.createArray();
    if (jsonArray != null) {
      for (int i = 0, n = jsonArray.size(); i < n; ++i) {
        StackTraceItem item = parseStackTraceItem((Jso) jsonArray.get(i));
        if (item != null) {
          result.add(item);
        }
      }
    }
    return result;
  }

  private static StackTraceItem parseStackTraceItem(final Jso json) {
    if (json == null) {
      return null;
    }

    return new StackTraceItem() {

      @Override
      public int getColumnNumber() {
        return json.getFieldCastedToInteger("columnNumber");
      }

      @Override
      public String getFunctionName() {
        return json.getStringField("functionName");
      }

      @Override
      public int getLineNumber() {
        return json.getFieldCastedToInteger("lineNumber");
      }

      @Override
      public String getUrl() {
        return json.getStringField("url");
      }
    };
  }

  private DebuggerChromeApiUtils() {} // COV_NF_LINE
}
