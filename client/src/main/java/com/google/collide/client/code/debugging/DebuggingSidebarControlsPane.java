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

import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;

/**
 * Debugging sidebar controls pane.
 *
 * TODO: i18n for the UI strings?
 *
 */
public class DebuggingSidebarControlsPane extends UiComponent<DebuggingSidebarControlsPane.View> {

  public interface Css extends CssResource {
    String root();
    String buttonEnabled();
    String pauseButton();
    String resumeButton();
    String stepOverButton();
    String stepIntoButton();
    String stepOutButton();
  }

  interface Resources extends ClientBundle, Tooltip.Resources {
    @Source("DebuggingSidebarControlsPane.css")
    Css workspaceEditorDebuggingSidebarControlsPaneCss();

    @Source("pauseButton.png")
    ImageResource pauseButton();

    @Source("resumeButton.png")
    ImageResource resumeButton();

    @Source("stepOverButton.png")
    ImageResource stepOverButton();

    @Source("stepIntoButton.png")
    ImageResource stepIntoButton();

    @Source("stepOutButton.png")
    ImageResource stepOutButton();
  }

  enum DebuggerCommand {
    PAUSE, RESUME, STEP_OVER, STEP_INTO, STEP_OUT
  }

  /**
   * Listener of this pane's events.
   */
  interface Listener {
    void onDebuggerCommand(DebuggerCommand command);
  }

  /**
   * The view for the sidebar controls pane.
   */
  static class View extends CompositeView<ViewEvents> {
    private final Resources resources;
    private final Css css;
    private final Element pauseButton;
    private final Element resumeButton;
    private final Element stepOverButton;
    private final Element stepIntoButton;
    private final Element stepOutButton;

    private class ButtonClickListener implements EventListener {
      private final DebuggerCommand command;

      private ButtonClickListener(DebuggerCommand command) {
        this.command = command;
      }

      @Override
      public void handleEvent(Event evt) {
        Element target = (Element) evt.getTarget();
        if (target.hasClassName(css.buttonEnabled())) {
          getDelegate().onDebuggerCommand(command);
        }
      }
    }

    View(Resources resources) {
      this.resources = resources;
      css = resources.workspaceEditorDebuggingSidebarControlsPaneCss();

      pauseButton = createControlButton(css.pauseButton(), DebuggerCommand.PAUSE, "Pause");
      resumeButton = createControlButton(css.resumeButton(), DebuggerCommand.RESUME, "Resume");
      stepOverButton = createControlButton(
          css.stepOverButton(), DebuggerCommand.STEP_OVER, "Step Over");
      stepIntoButton = createControlButton(
          css.stepIntoButton(), DebuggerCommand.STEP_INTO, "Step Into");
      stepOutButton = createControlButton(
          css.stepOutButton(), DebuggerCommand.STEP_OUT, "Step Out");

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(pauseButton);
      rootElement.appendChild(resumeButton);
      rootElement.appendChild(stepOverButton);
      rootElement.appendChild(stepIntoButton);
      rootElement.appendChild(stepOutButton);
      setElement(rootElement);

      // Hide the resume button (pause button is visible by default).
      CssUtils.setDisplayVisibility(resumeButton, false);
    }

    private Element createControlButton(String className, DebuggerCommand command, String tooltip) {
      Element button = Elements.createDivElement(className, css.buttonEnabled());
      button.addEventListener(Event.CLICK, new ButtonClickListener(command), false);
      Tooltip.create(resources, button, PositionController.VerticalAlign.BOTTOM,
          PositionController.HorizontalAlign.MIDDLE, tooltip);
      return button;
    }

    private void setActive(boolean active) {
      CssUtils.setClassNameEnabled(pauseButton, css.buttonEnabled(), active);
      CssUtils.setClassNameEnabled(resumeButton, css.buttonEnabled(), active);
    }

    private void setPaused(boolean paused) {
      if (paused) {
        CssUtils.setDisplayVisibility(pauseButton, false);
        CssUtils.setDisplayVisibility(resumeButton, true);
        stepOverButton.addClassName(css.buttonEnabled());
        stepIntoButton.addClassName(css.buttonEnabled());
        stepOutButton.addClassName(css.buttonEnabled());
      } else {
        CssUtils.setDisplayVisibility(pauseButton, true);
        CssUtils.setDisplayVisibility(resumeButton, false);
        stepOverButton.removeClassName(css.buttonEnabled());
        stepIntoButton.removeClassName(css.buttonEnabled());
        stepOutButton.removeClassName(css.buttonEnabled());
      }
    }
  }

  /**
   * The view events.
   */
  private interface ViewEvents {
    void onDebuggerCommand(DebuggerCommand command);
  }

  static DebuggingSidebarControlsPane create(View view) {
    return new DebuggingSidebarControlsPane(view);
  }

  private Listener delegateListener;

  @VisibleForTesting
  DebuggingSidebarControlsPane(View view) {
    super(view);

    view.setDelegate(new ViewEvents() {
      @Override
      public void onDebuggerCommand(DebuggerCommand command) {
        if (delegateListener != null) {
          delegateListener.onDebuggerCommand(command);
        }
      }
    });
  }

  void setListener(Listener listener) {
    delegateListener = listener;
  }

  void setActive(boolean active) {
    getView().setActive(active);
  }

  void setPaused(boolean paused) {
    getView().setPaused(paused);
  }
}
