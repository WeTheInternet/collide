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

import com.google.collide.client.code.debugging.DebuggerApiTypes.CssStyleSheetHeader;
import com.google.collide.client.code.debugging.DebuggerApiTypes.Location;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnAllCssStyleSheetsResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnBreakpointResolvedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnEvaluateExpressionResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnRemoteObjectPropertiesResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnScriptParsedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.PropertyDescriptor;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectSubType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectType;
import com.google.collide.json.client.Jso;
import com.google.collide.json.shared.JsonArray;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link DebuggerChromeApiUtils}.
 *
 */
public class DebuggerChromeApiUtilsTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

/*
  // TODO: Update the test when the protocol is stable.
  public void testParseOnPausedResponse() {
    String serializedResponse = "{\"id\":null,\"method\":\"Debugger.paused\","
        + "\"target\":\"505600000\",\"result\":{\"details\":{\"callFrames\":[{\"functionName\":"
        + "\"\",\"id\":\"{\\\"ordinal\\\":0,\\\"injectedScriptId\\\":1}\",\"location\":"
        + "{\"columnNumber\":8,\"lineNumber\":9,\"scriptId\":\"32\"},\"scopeChain\":[{\"object\":"
        + "{\"className\":\"DOMWindow\",\"description\":\"DOMWindow\",\"objectId\":"
        + "\"{\\\"injectedScriptId\\\":1,\\\"id\\\":1}\",\"type\":\"object\"},\"type\":"
        + "\"global\"}],\"this\":{\"className\":\"DOMWindow\",\"description\":\"DOMWindow\","
        + "\"objectId\":\"{\\\"injectedScriptId\\\":1,\\\"id\\\":2}\",\"type\":\"object\"}}],"
        + "\"eventData\":{},\"eventType\":0}}}";
    
    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    OnPausedResponse parsedResponse = DebuggerChromeApiUtils.parseOnPausedResponse(result);
    assertNotNull(parsedResponse);

    JsonArray<CallFrame> callFrames = parsedResponse.getCallFrames();
    assertNotNull(callFrames);
    assertEquals(1, callFrames.size());

    // First call frame.
    CallFrame callFrame = callFrames.get(0);
    assertEquals("", callFrame.getFunctionName());
    assertEquals("{\"ordinal\":0,\"injectedScriptId\":1}", callFrame.getId());
    assertLocation(9, 8, "32", callFrame.getLocation());
    assertRemoteObject("DOMWindow", true, "{\"injectedScriptId\":1,\"id\":2}",
        RemoteObjectType.OBJECT, null, callFrame.getThis());

    JsonArray<Scope> scopeChain = callFrame.getScopeChain();
    assertNotNull(scopeChain);
    assertEquals(1, scopeChain.size());

    assertEquals(ScopeType.GLOBAL, scopeChain.get(0).getType());
    assertRemoteObject("DOMWindow", true, "{\"injectedScriptId\":1,\"id\":1}",
        RemoteObjectType.OBJECT, null, scopeChain.get(0).getObject());
  }
*/

  public void testParseOnBreakpointResolvedResponse() {
    String serializedResponse = "{\"id\":null,\"method\":\"Debugger.breakpointResolved\","
        + "\"target\":\"48715528\",\"result\":{\"breakpointId\":"
        + "\"http://closure-library.googlecode.com/svn/trunk/closure/goog/base.js:317:0\","
        + "\"location\":{\"columnNumber\":4,\"lineNumber\":317,\"scriptId\":\"33\"}}}";

    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    OnBreakpointResolvedResponse parsedResponse =
        DebuggerChromeApiUtils.parseOnBreakpointResolvedResponse(request, result);
    assertNotNull(parsedResponse);

    assertEquals("http://closure-library.googlecode.com/svn/trunk/closure/goog/base.js:317:0",
        parsedResponse.getBreakpointId());
    assertNull(parsedResponse.getBreakpointInfo());

    JsonArray<Location> locations = parsedResponse.getLocations();
    assertNotNull(locations);
    assertEquals(1, locations.size());
    assertLocation(317, 4, "33", locations.get(0));
  }

  public void testParseOnRemoveBreakpoint() {
    String serializedResponse = "{\"id\":4,\"method\":\"Debugger.removeBreakpoint\","
        + "\"target\":\"48715528\",\"request\":{\"breakpointId\":"
        + "\"http://closure-library.googlecode.com/svn/trunk/closure/goog/base.js:317:0\"},"
        + "\"result\":{}}";

    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    String breakpointId = DebuggerChromeApiUtils.parseOnRemoveBreakpointResponse(request);
    assertEquals("http://closure-library.googlecode.com/svn/trunk/closure/goog/base.js:317:0",
        breakpointId);
  }

  public void testParseOnScriptParsedResponse() {
    String serializedResponse = "{\"id\":null,\"method\":\"Debugger.scriptParsed\","
        + "\"target\":\"48715528\",\"result\":{\"endColumn\":1,\"endLine\":1502,"
        + "\"scriptId\":\"33\",\"startColumn\":0,\"startLine\":0,\"url\":"
        + "\"http://closure-library.googlecode.com/svn/trunk/closure/goog/base.js\"}}";

    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    OnScriptParsedResponse parsedResponse = DebuggerChromeApiUtils.parseOnScriptParsedResponse(
        result);
    assertNotNull(parsedResponse);

    assertEquals(0, parsedResponse.getStartLine());
    assertEquals(0, parsedResponse.getStartColumn());
    assertEquals(1502, parsedResponse.getEndLine());
    assertEquals(1, parsedResponse.getEndColumn());
    assertEquals("33", parsedResponse.getScriptId());
    assertEquals("http://closure-library.googlecode.com/svn/trunk/closure/goog/base.js",
        parsedResponse.getUrl());
    assertFalse(parsedResponse.isContentScript());
  }

  public void testParseOnScriptParsedResponseForContentScript() {
    String serializedResponse = "{\"id\":null,\"method\":\"Debugger.scriptParsed\","
        + "\"target\":\"48715528\",\"result\":{\"endColumn\":511,\"endLine\":18,"
        + "\"isContentScript\":true,\"scriptId\":\"32\",\"startColumn\":0,\"startLine\":0,"
        + "\"url\":\"chrome-extension://plcnnpdmhobdfbponjpedobekiogmbco/content/main.js\"}}";

    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    OnScriptParsedResponse parsedResponse = DebuggerChromeApiUtils.parseOnScriptParsedResponse(
        result);
    assertNotNull(parsedResponse);

    assertEquals(0, parsedResponse.getStartLine());
    assertEquals(0, parsedResponse.getStartColumn());
    assertEquals(18, parsedResponse.getEndLine());
    assertEquals(511, parsedResponse.getEndColumn());
    assertEquals("32", parsedResponse.getScriptId());
    assertEquals("chrome-extension://plcnnpdmhobdfbponjpedobekiogmbco/content/main.js",
        parsedResponse.getUrl());
    assertTrue(parsedResponse.isContentScript());
  }

  public void testParseOnRemoteObjectPropertiesResponse() {
    String serializedResponse = "{\"id\":5,\"method\":\"Runtime.getProperties\","
        + "\"target\":\"292044344\",\"request\":{\"objectId\":\"{\\\"injectedScriptId\\\":1,"
        + "\\\"id\\\":13}\",\"ignoreHasOwnProperty\":false},\"result\":{\"result\":"
        + "[{\"name\":\"msg\",\"value\":{\"description\":\"onTimer callback\","
        + "\"type\":\"string\"}},{\"name\":\"logs\",\"value\":{\"className\":"
        + "\"HTMLDivElement\",\"description\":\"HTMLDivElement\",\"hasChildren\":true,"
        + "\"objectId\":\"{\\\"injectedScriptId\\\":1,\\\"id\\\":19}\",\"type\":\"object\","
        + "\"subtype\":\"node\"}}]}}";

    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    OnRemoteObjectPropertiesResponse parsedResponse =
        DebuggerChromeApiUtils.parseOnRemoteObjectPropertiesResponse(request, result);
    assertNotNull(parsedResponse);

    assertEquals("{\"injectedScriptId\":1,\"id\":13}", parsedResponse.getObjectId().toString());

    JsonArray<PropertyDescriptor> properties = parsedResponse.getProperties();
    assertNotNull(properties);
    assertEquals(2, properties.size());

    PropertyDescriptor propertyDescriptor = properties.get(0);
    assertNull(propertyDescriptor.getGetterFunction());
    assertNull(propertyDescriptor.getSetterFunction());
    assertEquals("msg", propertyDescriptor.getName());
    assertRemoteObject("onTimer callback", false, null, RemoteObjectType.STRING, null,
        propertyDescriptor.getValue());
    assertFalse(propertyDescriptor.wasThrown());

    propertyDescriptor = properties.get(1);
    assertNull(propertyDescriptor.getGetterFunction());
    assertNull(propertyDescriptor.getSetterFunction());
    assertEquals("logs", propertyDescriptor.getName());
    assertRemoteObject("HTMLDivElement", true, "{\"injectedScriptId\":1,\"id\":19}",
        RemoteObjectType.OBJECT, RemoteObjectSubType.NODE, propertyDescriptor.getValue());
    assertFalse(propertyDescriptor.wasThrown());
  }

  public void testParseOnEvaluateExpressionResponse() {
    String serializedResponse = "{\"id\":8,\"method\":\"Debugger.evaluateOnCallFrame\","
        + "\"target\":\"400060083\",\"request\":{\"callFrameId\":\"{\\\"ordinal\\\":0,"
        + "\\\"injectedScriptId\\\":1}\",\"expression\":\"myArray\"},\"result\":{\"result\":{"
        + "\"className\":\"Array\",\"description\":\"Array[4]\",\"hasChildren\":true,\"objectId\":"
        + "\"{\\\"injectedScriptId\\\":1,\\\"id\\\":12}\",\"type\":\"object\","
        + "\"subtype\":\"array\"}}}";

    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    OnEvaluateExpressionResponse parsedResponse =
        DebuggerChromeApiUtils.parseOnEvaluateExpressionResponse(request, result);
    assertNotNull(parsedResponse);

    assertEquals("{\"ordinal\":0,\"injectedScriptId\":1}", parsedResponse.getCallFrameId());
    assertEquals("myArray", parsedResponse.getExpression());
    assertRemoteObject("Array[4]", true, "{\"injectedScriptId\":1,\"id\":12}",
        RemoteObjectType.OBJECT, RemoteObjectSubType.ARRAY, parsedResponse.getResult());
    assertFalse(parsedResponse.wasThrown());
  }

  public void testParseOnEvaluateExpressionResponseWasThrown() {
    String serializedResponse = "{\"id\":9,\"method\":\"Debugger.evaluateOnCallFrame\","
        + "\"target\":\"400060083\",\"request\":{\"callFrameId\":\"{\\\"ordinal\\\":0,"
        + "\\\"injectedScriptId\\\":1}\",\"expression\":\"myArray1\"},\"result\":{\"result\":{"
        + "\"className\":\"ReferenceError\",\"description\":\"ReferenceError\","
        + "\"hasChildren\":true,\"objectId\":\"{\\\"injectedScriptId\\\":1,\\\"id\\\":13}\","
        + "\"type\":\"object\"},\"wasThrown\":true}}";

    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    OnEvaluateExpressionResponse parsedResponse =
        DebuggerChromeApiUtils.parseOnEvaluateExpressionResponse(request, result);
    assertNotNull(parsedResponse);

    assertEquals("{\"ordinal\":0,\"injectedScriptId\":1}", parsedResponse.getCallFrameId());
    assertEquals("myArray1", parsedResponse.getExpression());
    assertRemoteObject("ReferenceError", true, "{\"injectedScriptId\":1,\"id\":13}",
        RemoteObjectType.OBJECT, null, parsedResponse.getResult());
    assertTrue(parsedResponse.wasThrown());
  }

  public void testParseOnAllCssStyleSheetsResponse() {
    String serializedResponse = "{\"id\":9,\"method\":\"CSS.getAllStyleSheets\","
        + "\"target\":\"400060083\",\"result\":{\"headers\":[{\"disabled\":false,"
        + "\"sourceURL\":\"http://localhost/test.css\",\"styleSheetId\":\"2\",\"title\":\"\"}]}}";

    Jso response = Jso.deserialize(serializedResponse);
    Jso request = response.getJsObjectField("request").cast();
    Jso result = response.getJsObjectField("result").cast();

    OnAllCssStyleSheetsResponse parsedResponse =
        DebuggerChromeApiUtils.parseOnAllCssStyleSheetsResponse(result);
    assertNotNull(parsedResponse);

    JsonArray<CssStyleSheetHeader> headers = parsedResponse.getHeaders();
    assertNotNull(headers);
    assertEquals(1, headers.size());

    CssStyleSheetHeader header = headers.get(0);
    assertFalse(header.isDisabled());
    assertEquals("2", header.getId());
    assertEquals("", header.getTitle());
    assertEquals("http://localhost/test.css", header.getUrl());
  }

  private void assertLocation(int lineNumber, int columnNumber, String scriptId, Location loc) {
    assertEquals(lineNumber, loc.getLineNumber());
    assertEquals(columnNumber, loc.getColumnNumber());
    assertEquals(scriptId, loc.getScriptId());
  }

  private void assertRemoteObject(String description, boolean hasChildren, String objectId,
      RemoteObjectType type, RemoteObjectSubType subtype, RemoteObject object) {
    assertEquals(description, object.getDescription());
    assertEquals(hasChildren, object.hasChildren());
    if (objectId == null) {
      assertNull(object.getObjectId());
    } else {
      assertNotNull(object.getObjectId());
      assertEquals(objectId, object.getObjectId().toString());
    }
    assertEquals(type, object.getType());
    assertEquals(subtype, object.getSubType());
  }
}
