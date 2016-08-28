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

import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;

/**
 * Debugging sidebar call stack pane.
 *
 */
public class DebuggingSidebarCallStackPane extends UiComponent<DebuggingSidebarCallStackPane.View> {

  public interface Css extends CssResource {
    String root();
    String callFrame();
    String callFrameTitle();
    String callFrameSubTitle();
    String callFrameSelected();
  }

  interface Resources extends ClientBundle {
    @Source("DebuggingSidebarCallStackPane.css")
    Css workspaceEditorDebuggingSidebarCallStackPaneCss();
  }

  /**
   * Listener of this pane's events.
   */
  interface Listener {
    void onCallFrameSelect(int depth);
  }

  /**
   * The view for the sidebar call stack pane.
   */
  static class View extends CompositeView<ViewEvents> {
    private final Css css;

    private final EventListener clickListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        Element callFrame = CssUtils.getAncestorOrSelfWithClassName((Element) evt.getTarget(),
            css.callFrame());
        if (callFrame != null) {
          onCallFrameClick(callFrame);
        }
      }
    };

    View(Resources resources) {
      css = resources.workspaceEditorDebuggingSidebarCallStackPaneCss();

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.addEventListener(Event.CLICK, clickListener, false);
      setElement(rootElement);
    }

    private void addCallFrame(String title, String subtitle) {
      boolean addingFirstElement = !getElement().hasChildNodes();

      Element callFrame = Elements.createDivElement(css.callFrame());
      DomUtils.appendDivWithTextContent(callFrame, css.callFrameSubTitle(), subtitle);
      DomUtils.appendDivWithTextContent(callFrame, css.callFrameTitle(), title);
      getElement().appendChild(callFrame);

      if (addingFirstElement) {
        callFrame.addClassName(css.callFrameSelected());
      }
    }

    private void clearCallStack() {
      getElement().setInnerHTML("");
    }

    private void onCallFrameClick(Element callFrame) {
      Element selected = DomUtils.getFirstElementByClassName(getElement(), css.callFrameSelected());
      if (selected != null) {
        selected.removeClassName(css.callFrameSelected());
      }

      callFrame.addClassName(css.callFrameSelected());

      int index = DomUtils.getSiblingIndexWithClassName(callFrame, css.callFrame());
      getDelegate().onCallFrameSelect(index);
    }
  }

  /**
   * The view events.
   */
  private interface ViewEvents {
    void onCallFrameSelect(int depth);
  }

  static DebuggingSidebarCallStackPane create(View view) {
    return new DebuggingSidebarCallStackPane(view);
  }

  private Listener delegateListener;

  @VisibleForTesting
  DebuggingSidebarCallStackPane(View view) {
    super(view);

    view.setDelegate(new ViewEvents() {
      @Override
      public void onCallFrameSelect(int depth) {
        if (delegateListener != null) {
          delegateListener.onCallFrameSelect(depth);
        }
      }
    });
  }

  void setListener(Listener listener) {
    delegateListener = listener;
  }

  void addCallFrame(String title, String subtitle) {
    getView().addCallFrame(title, subtitle);
  }

  void clearCallStack() {
    getView().clearCallStack();
  }
}
