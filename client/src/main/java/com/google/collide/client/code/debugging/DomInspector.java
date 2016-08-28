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

import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;
import elemental.events.CustomEvent;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.IFrameElement;
import elemental.html.LinkElement;
import elemental.html.ScriptElement;

/**
 * Dom Inspector UI.
 *
 */
public class DomInspector extends UiComponent<DomInspector.View> {

  private static final String DEBUGGER_CUSTOM_MESSAGE_REQUEST = "DebuggerCustomMessageRequest";
  private static final String DEBUGGER_CUSTOM_MESSAGE_RESPONSE = "DebuggerCustomMessageResponse";

  public interface Css extends CssResource {
    String root();
    String domIframe();
  }

  interface Resources extends ClientBundle, Tooltip.Resources {
    @Source("DomInspector.css")
    Css workspaceEditorDomInspectorCss();
  }

  /**
   * The view of the Dom Inspector.
   */
  static class View extends CompositeView<ViewEvents> {
    private final Css css;
    private final IFrameElement domInspectorIframe;

    private final EventListener customDebuggerMessageRequestListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        Object detail = ((CustomEvent) evt).getDetail();
        if (detail != null) {
          getDelegate().onCustomMessageRequest(detail.toString());
        }
      }
    };

    View(Resources resources) {
      css = resources.workspaceEditorDomInspectorCss();

      domInspectorIframe = Elements.createIFrameElement(css.domIframe());
      domInspectorIframe.setOnload(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          onDomInspectorIframeLoaded();
        }
      });
      CssUtils.setDisplayVisibility(domInspectorIframe, false);

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(domInspectorIframe);
      setElement(rootElement);
    }

    private void show() {
      if (!isVisible()) {
        CssUtils.setDisplayVisibility(domInspectorIframe, true);
        // Ping with an empty message to re-initialize.
        sendCustomMessageResponseToInspector("");
      }
    }

    private void hide() {
      CssUtils.setDisplayVisibility(domInspectorIframe, false);
    }

    private boolean isVisible() {
      return CssUtils.isVisible(domInspectorIframe);
    }

    private void onDomInspectorIframeLoaded() {
      // <link href="test.css" rel="stylesheet" type="text/css">
      LinkElement linkElement = Elements.getDocument().createLinkElement();
      linkElement.setRel("stylesheet");
      linkElement.setType("text/css");
      linkElement.setHref("/static/dominspector_css_compiled.css");
      Elements.getHead(domInspectorIframe.getContentDocument()).appendChild(linkElement);

      ScriptElement scriptElement = Elements.getDocument().createScriptElement();
      scriptElement.setSrc("/static/dominspector_js_compiled.js");
      Elements.getBody(domInspectorIframe.getContentDocument()).appendChild(scriptElement);

      domInspectorIframe.getContentWindow().addEventListener(
          DEBUGGER_CUSTOM_MESSAGE_REQUEST, customDebuggerMessageRequestListener, false);
    }

    private void sendCustomMessageResponseToInspector(String response) {
      if (domInspectorIframe.getContentDocument() != null
          && domInspectorIframe.getContentWindow() != null) {
        CustomEvent evt = (CustomEvent) domInspectorIframe.getContentDocument().createEvent(
            "CustomEvent");
        evt.initCustomEvent(DEBUGGER_CUSTOM_MESSAGE_RESPONSE, true, true, response);
        domInspectorIframe.getContentWindow().dispatchEvent(evt);
      }
    }
  }

  /**
   * The view events.
   */
  private interface ViewEvents {
    void onCustomMessageRequest(String message);
  }

  static DomInspector create(View view, DebuggerState debuggerState) {
    return new DomInspector(view, debuggerState);
  }

  private final DebuggerState debuggerState;

  private final DebuggerState.CustomMessageListener customDebuggerMessageResponseListener =
      new DebuggerState.CustomMessageListener() {
        @Override
        public void onCustomMessageResponse(String response) {
          getView().sendCustomMessageResponseToInspector(response);
        }
      };

  @VisibleForTesting
  DomInspector(View view, DebuggerState debuggerState) {
    super(view);

    this.debuggerState = debuggerState;
    debuggerState.getCustomMessageListenerRegistrar().add(customDebuggerMessageResponseListener);

    view.setDelegate(new ViewEvents() {
      @Override
      public void onCustomMessageRequest(String message) {
        DomInspector.this.debuggerState.sendCustomMessage(message);
      }
    });
  }

  void show() {
    getView().show();
  }

  void hide() {
    getView().hide();
  }
}
