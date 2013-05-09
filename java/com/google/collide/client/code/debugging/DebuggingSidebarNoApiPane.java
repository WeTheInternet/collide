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

import javax.annotation.Nullable;

import com.google.collide.client.util.AnimationUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.AnchorElement;
import elemental.html.DivElement;

/**
 * Debugging sidebar pane that is shown when no {@link DebuggerApi} is
 * available.
 */
public class DebuggingSidebarNoApiPane extends UiComponent<DebuggingSidebarNoApiPane.View> {

  public interface Css extends CssResource {
    String root();
    String headerLogo();
    String headerText();
    String button();
    String footerText();
  }

  interface Resources extends ClientBundle {
    @Source("DebuggingSidebarNoApiPane.css")
    Css workspaceEditorDebuggingSidebarNoApiPaneCss();

    @Source("extensionPackage.png")
    ImageResource extensionPackage();
  }

  /**
   * Listener of this pane's events.
   */
  interface Listener {
    void onShouldDisplayNoApiPaneChange();
  }

  private static class Panel {
    private final Element root;
    private final AnchorElement button;
    private final DivElement footer;

    private Panel(Element root, AnchorElement button, DivElement footer) {
      this.root = root;
      this.button = button;
      this.footer = footer;
    }
  }

  /**
   * The view for the sidebar call stack pane.
   */
  static class View extends CompositeView<Void> {
    private final Css css;
    private final Panel downloadExtension;
    private final Panel browserNotSupported;

    View(Resources resources) {
      css = resources.workspaceEditorDebuggingSidebarNoApiPaneCss();

      downloadExtension = createLayout("",
          "Download Debugging Extension",
          "Install the Debugging Extension to support debugging in Collide.");
      browserNotSupported = createLayout(
          "Your browser does not support breakpoint debugging in Collide.",
          "Download Google Chrome",
          "It's free and installs in seconds.");

      setElement(Elements.createDivElement());
    }

    private Panel createLayout(String headerText, String buttonText, String footerText) {
      Element root = Elements.createDivElement(css.root());

      if (StringUtils.isNullOrEmpty(headerText)) {
        root.appendChild(Elements.createDivElement(css.headerLogo()));
      } else {
        DomUtils.appendDivWithTextContent(root, css.headerText(), headerText);
      }

      AnchorElement button = Elements.createAnchorElement();
      button.setClassName(css.button());
      button.setTextContent(buttonText);
      root.appendChild(button);

      DivElement footer = DomUtils.appendDivWithTextContent(root, css.footerText(), footerText);
      return new Panel(root, button, footer);
    }

    private void showLayout(@Nullable String extensionUrl) {
      hideLayout();

      boolean isExtensionSupported = (extensionUrl != null);
      final Panel panel = isExtensionSupported ? downloadExtension : browserNotSupported;
      getElement().appendChild(panel.root);

      panel.button.setTarget(StringUtils.isNullOrEmpty(extensionUrl) ? "_blank" : "");
      // TODO: What's the right answer?
      panel.button.setHref(StringUtils.ensureNotEmpty(extensionUrl,
          "http://www.google.com/url?sa=D&q=http://www.google.com/chrome/"));

      if (isExtensionSupported) {
        panel.button.addEventListener(Event.CLICK, new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            panel.footer.getStyle().setColor("#000");
            panel.footer.getStyle().setOpacity(0.0);
            panel.footer.setTextContent("Please accept extension in download bar,"
                + " confirm installation, and then reload the page.");
            AnimationUtils.animatePropertySet(
                panel.footer, "opacity", "1.0", AnimationUtils.LONG_TRANSITION_DURATION);
          }
        }, false);
      }
    }

    private void hideLayout() {
      downloadExtension.root.removeFromParent();
      browserNotSupported.root.removeFromParent();
    }
  }

  static DebuggingSidebarNoApiPane create(View view, DebuggerState debuggerState) {
    return new DebuggingSidebarNoApiPane(view, debuggerState);
  }

  private final DebuggerState debuggerState;
  private Listener delegateListener;

  private final DebuggerState.DebuggerAvailableListener debuggerAvailableListener =
      new DebuggerState.DebuggerAvailableListener() {
        @Override
        public void onDebuggerAvailableChange() {
          displayOrHide();

          if (delegateListener != null) {
            delegateListener.onShouldDisplayNoApiPaneChange();
          }
        }
      };

  @VisibleForTesting
  DebuggingSidebarNoApiPane(View view, DebuggerState debuggerState) {
    super(view);

    this.debuggerState = debuggerState;
    debuggerState.getDebuggerAvailableListenerRegistrar().add(debuggerAvailableListener);

    displayOrHide();
  }

  void setListener(Listener listener) {
    delegateListener = listener;
  }

  boolean shouldDisplay() {
    return !debuggerState.isDebuggerAvailable();
  }

  private void displayOrHide() {
    if (shouldDisplay()) {
      getView().showLayout(debuggerState.getDebuggingExtensionUrl());
    } else {
      getView().hideLayout();
    }
  }
}
