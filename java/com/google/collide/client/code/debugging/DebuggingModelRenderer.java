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

import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.debugging.DebuggerApiTypes.CallFrame;
import com.google.collide.client.code.debugging.DebuggerApiTypes.Location;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnPausedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnScriptParsedResponse;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.Scope;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.testing.DebugAttributeSetter;
import com.google.collide.client.util.JsIntegerMap;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;

/**
 * A renderer for the debugging model.
 */
public class DebuggingModelRenderer {

  /**
   * CssResource for the debugging model UI.
   */
  public interface Css extends CssResource {
    String breakpoint();
    String breakpointInactive();
    String executionLine();
    String gutterExecutionLine();
  }

  /**
   * ClientBundle for the debugging model UI.
   */
  public interface Resources extends ClientBundle {
    @Source({"DebuggingModelRenderer.css",
 "com/google/collide/client/editor/constants.css"})

    Css workspaceEditorDebuggingModelCss();

    @Source("gutterExecutionLine.png")
    ImageResource gutterExecutionLine();

    @Source("breakpointGutterActive.png")
    DataResource breakpointGutterActiveResource();

    @Source("breakpointGutterInactive.png")
    DataResource breakpointGutterInactiveResource();
  }

  public static DebuggingModelRenderer create(DebuggingModelRenderer.Resources resources, Editor editor,
      DebuggingSidebar debuggingSidebar, DebuggerState debuggerState) {
    return new DebuggingModelRenderer(resources, editor, debuggingSidebar, debuggerState);
  }

  private final Css css;
  private final Editor editor;
  private final DebuggingSidebar debuggingSidebar;
  private final DebuggerState debuggerState;
  private JsIntegerMap<Element> lineNumberToElementCache = JsIntegerMap.create();
  private AnchoredExecutionLine anchoredExecutionLine;

  private DebuggingModelRenderer(DebuggingModelRenderer.Resources resources, Editor editor,
      DebuggingSidebar debuggingSidebar, DebuggerState debuggerState) {
    this.css = resources.workspaceEditorDebuggingModelCss();
    this.editor = editor;
    this.debuggingSidebar = debuggingSidebar;
    this.debuggerState = debuggerState;
  }

  void renderBreakpointOnGutter(int lineNumber, boolean active) {
    Element element = lineNumberToElementCache.get(lineNumber);
    if (element == null) {
      element = Elements.createDivElement(css.breakpoint());
      element.getStyle().setTop(editor.getBuffer().calculateLineTop(lineNumber),
          CSSStyleDeclaration.Unit.PX);

      new DebugAttributeSetter().add("linenumber", String.valueOf(lineNumber + 1)).on(element);

      editor.getLeftGutter().addUnmanagedElement(element);
      lineNumberToElementCache.put(lineNumber, element);
    }

    CssUtils.setClassNameEnabled(element, css.breakpointInactive(), !active);
  }

  void removeBreakpointOnGutter(int lineNumber) {
    Element element = lineNumberToElementCache.get(lineNumber);
    if (element != null) {
      editor.getLeftGutter().removeUnmanagedElement(element);
      lineNumberToElementCache.erase(lineNumber);
    }
  }

  void renderDebuggerState() {
    debuggingSidebar.setActive(debuggerState.isActive());
    debuggingSidebar.setPaused(debuggerState.isPaused());

    debuggingSidebar.clearCallStack();

    if (debuggerState.isPaused()) {
      OnPausedResponse onPausedResponse = Preconditions.checkNotNull(
          debuggerState.getOnPausedResponse());
      JsonArray<CallFrame> callFrames = onPausedResponse.getCallFrames();
      for (int i = 0, n = callFrames.size(); i < n; ++i) {
        CallFrame callFrame = callFrames.get(i);
        Location location = callFrame.getLocation();
        OnScriptParsedResponse onScriptParsedResponse = debuggerState.getOnScriptParsedResponse(
            location.getScriptId());

        // TODO: What about i18n?
        String title = StringUtils.ensureNotEmpty(callFrame.getFunctionName(),
            "(anonymous function)");
        String subtitle = getShortenedScriptUrl(onScriptParsedResponse) + ":" +
            (location.getLineNumber() + 1);
        debuggingSidebar.addCallFrame(title, subtitle);
      }
    }
  }

  void renderDebuggerCallFrame() {
    CallFrame callFrame = debuggerState.getActiveCallFrame();
    if (callFrame == null) {
      // Debugger is not paused. Remove the previous scope tree UI.
      debuggingSidebar.setScopeVariablesRootNodes(null);
      debuggingSidebar.refreshWatchExpressions();
      return;
    }

    // Render the Scope Variables pane.
    JsonArray<RemoteObjectNode> rootNodes = JsonCollections.createArray();

    JsonArray<Scope> scopeChain = callFrame.getScopeChain();
    for (int i = 0, n = scopeChain.size(); i < n; ++i) {
      Scope scope = scopeChain.get(i);
      String name = StringUtils.capitalizeFirstLetter(scope.getType().toString());
      RemoteObject remoteObject = scope.getObject();

      RemoteObjectNode.Builder scopeNodeBuilder = new RemoteObjectNode.Builder(name, remoteObject)
          .setOrderIndex(i)
          .setWritable(false)
          .setDeletable(false)
          .setTransient(scope.isTransient());

      // Append the call frame "this" object to the top scope.
      if (i == 0 && callFrame.getThis() != null) {
        RemoteObjectNode thisNode = new RemoteObjectNode.Builder("this", callFrame.getThis())
            .setWritable(false)
            .setDeletable(false)
            .build();
        RemoteObjectNode scopeNode = scopeNodeBuilder
            .setHasChildren(true) // At least will contain the "this" child.
            .build();
        scopeNode.addChild(thisNode);
        rootNodes.add(scopeNode);
      } else {
        rootNodes.add(scopeNodeBuilder.build());
      }
    }

    debuggingSidebar.setScopeVariablesRootNodes(rootNodes);
    debuggingSidebar.refreshWatchExpressions();
  }

  void renderExecutionLine(int lineNumber) {
    removeExecutionLine();
    anchoredExecutionLine = AnchoredExecutionLine.create(editor, lineNumber, css.executionLine(),
        css.gutterExecutionLine());
  }

  void removeExecutionLine() {
    if (anchoredExecutionLine != null) {
      anchoredExecutionLine.teardown();
      anchoredExecutionLine = null;
    }
  }

  private static String getShortenedScriptUrl(OnScriptParsedResponse onScriptParsedResponse) {
    String url = onScriptParsedResponse != null ? onScriptParsedResponse.getUrl() : null;
    if (StringUtils.isNullOrEmpty(url)) {
      // TODO: What about i18n?
      return "unknown";
    }
    if (onScriptParsedResponse.isContentScript()) {
      return url; // No shortening.
    }

    while (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }

    int pos = url.lastIndexOf("/");
    if (pos >= 0) {
      return url.substring(pos + 1);
    }
    return url;
  }

  void handleDocumentChanged() {
    lineNumberToElementCache = JsIntegerMap.create();
    removeExecutionLine();
  }
}
