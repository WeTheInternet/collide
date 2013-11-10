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

import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;

/**
 * The presenter for the debugging sidebar. This sidebar shows current debugger
 * state, call stack, watch expressions, breakpoints and etc.
 *
 * TODO: i18n for the UI strings?
 *
 */
public class DebuggingSidebar extends UiComponent<DebuggingSidebar.View> {

  public interface Css extends CssResource {
    String root();
    String unscrollable();
    String scrollable();
    String expandablePane();
    String paneTitle();
    String paneTitleText();
    String paneBody();
    String paneInfo();
    String paneInfoHeader();
    String paneInfoBody();
    String paneData();
    String paneExpanded();
  }

  public interface Resources extends ClientBundle,
      DebuggingSidebarHeader.Resources,
      DebuggingSidebarControlsPane.Resources,
      DebuggingSidebarWatchExpressionsPane.Resources,
      DebuggingSidebarCallStackPane.Resources,
      DebuggingSidebarScopeVariablesPane.Resources,
      DebuggingSidebarBreakpointsPane.Resources,
      DomInspector.Resources,
      ConsoleView.Resources,
      DebuggingSidebarNoApiPane.Resources {

    @Source("DebuggingSidebar.css")
    Css workspaceEditorDebuggingSidebarCss();

    @Source("triangleRight.png")
    DataResource triangleRightResource();

    @Source("triangleDown.png")
    DataResource triangleDownResource();
  }

  /**
   * User actions on the debugger.
   */
  public interface DebuggerCommandListener {
    void onPause();
    void onResume();
    void onStepOver();
    void onStepInto();
    void onStepOut();
    void onCallFrameSelect(int depth);
    void onBreakpointIconClick(Breakpoint breakpoint);
    void onBreakpointLineClick(Breakpoint breakpoint);
    void onActivateBreakpoints();
    void onDeactivateBreakpoints();
    void onLocationLinkClick(String url, int lineNumber);
  }

  /**
   * The view for the sidebar.
   */
  public static class View extends CompositeView<Void> {
    private final Css css;

    private final DebuggingSidebarHeader.View headerView;
    private final DebuggingSidebarControlsPane.View controlsPaneView;
    private final DebuggingSidebarWatchExpressionsPane.View watchExpressionsPaneView;
    private final DebuggingSidebarCallStackPane.View callStackPaneView;
    private final DebuggingSidebarScopeVariablesPane.View scopeVariablesPaneView;
    private final DebuggingSidebarBreakpointsPane.View breakpointsPaneView;
    private final DomInspector.View domInspectorView;
    private final ConsoleView.View consoleView;
    private final DebuggingSidebarNoApiPane.View noApiPaneView;

    private final Element headerPane;
    private final Element controlsPane;
    private final Element watchExpressionsPane;
    private final Element callStackPane;
    private final Element scopeVariablesPane;
    private final Element breakpointsPane;
    private final Element domInspectorPane;
    private final Element consolePane;
    private final Element noApiPane;

    private final EventListener expandCollapsePaneListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        Element pane = CssUtils.getAncestorOrSelfWithClassName((Element) evt.getTarget(),
            css.expandablePane());
        if (pane != null) {
          expandPane(pane, !pane.hasClassName(css.paneExpanded()));
        }
      }
    };

    public View(Resources resources) {
      css = resources.workspaceEditorDebuggingSidebarCss();

      headerView = new DebuggingSidebarHeader.View(resources);
      controlsPaneView = new DebuggingSidebarControlsPane.View(resources);
      watchExpressionsPaneView = new DebuggingSidebarWatchExpressionsPane.View(resources);
      callStackPaneView = new DebuggingSidebarCallStackPane.View(resources);
      scopeVariablesPaneView = new DebuggingSidebarScopeVariablesPane.View(resources);
      breakpointsPaneView = new DebuggingSidebarBreakpointsPane.View(resources);
      domInspectorView = new DomInspector.View(resources);
      consoleView = new ConsoleView.View(resources);
      noApiPaneView = new DebuggingSidebarNoApiPane.View(resources);

      headerPane = headerView.getElement();
      controlsPane = controlsPaneView.getElement();
      watchExpressionsPane = createWatchExpressionsPane();
      callStackPane = createCallStackPane();
      scopeVariablesPane = createScopeVariablesPane();
      breakpointsPane = createBreakpointsPane();
      domInspectorPane = createDomInspectorPane();
      consolePane = createConsolePane();
      noApiPane = noApiPaneView.getElement();

      Element rootElement = Elements.createDivElement(css.root());

      Element unscrollable = Elements.createDivElement(css.unscrollable());
      unscrollable.appendChild(headerPane);
      unscrollable.appendChild(controlsPane);
      unscrollable.appendChild(noApiPane);

      Element scrollable = Elements.createDivElement(css.scrollable());
      scrollable.appendChild(watchExpressionsPane);
      scrollable.appendChild(callStackPane);
      scrollable.appendChild(scopeVariablesPane);
      scrollable.appendChild(breakpointsPane);
      scrollable.appendChild(consolePane);
      scrollable.appendChild(domInspectorPane);

      rootElement.appendChild(unscrollable);
      rootElement.appendChild(scrollable);
      setElement(rootElement);

      CssUtils.setDisplayVisibility(noApiPane, false);
    }

    private Element createWatchExpressionsPane() {
      return createExpandablePane("Watch Expressions", "No watch expressions", "",
          watchExpressionsPaneView.getElement());
    }

    private Element createCallStackPane() {
      return createExpandablePane("Call Stack", "Not paused",
          "The call stack is a representation of how your code was executed.",
          callStackPaneView.getElement());
    }

    private Element createScopeVariablesPane() {
      return createExpandablePane("Scope Variables", "Not paused", "",
          scopeVariablesPaneView.getElement());
    }

    private Element createBreakpointsPane() {
      return createExpandablePane("Breakpoints", "No breakpoints", "",
          breakpointsPaneView.getElement());
    }

    private Element createDomInspectorPane() {
      return createExpandablePane("DOM Inspector", "Not paused", "",
          domInspectorView.getElement());
    }

    private Element createConsolePane() {
      return createExpandablePane("Console", "Not debugging", "", consoleView.getElement());
    }

    private Element createExpandablePane(String titleText, String infoHeader, String infoBody,
        Element dataElement) {
      Element pane = Elements.createDivElement(css.expandablePane());

      Element title = Elements.createDivElement(css.paneTitle());
      DomUtils.appendDivWithTextContent(title, css.paneTitleText(), titleText);
      title.addEventListener(Event.CLICK, expandCollapsePaneListener, false);

      Element info = Elements.createDivElement(css.paneInfo());
      if (!StringUtils.isNullOrEmpty(infoHeader)) {
        DomUtils.appendDivWithTextContent(info, css.paneInfoHeader(), infoHeader);
      }
      if (!StringUtils.isNullOrEmpty(infoBody)) {
        DomUtils.appendDivWithTextContent(info, css.paneInfoBody(), infoBody);
      }

      Element data = Elements.createDivElement(css.paneData());
      if (dataElement != null) {
        data.appendChild(dataElement);
      }

      Element body = Elements.createDivElement(css.paneBody());
      body.appendChild(info);
      body.appendChild(data);

      pane.appendChild(title);
      pane.appendChild(body);
      return pane;
    }

    private Element getPaneTitle(Element pane) {
      return DomUtils.getFirstElementByClassName(pane, css.paneTitle());
    }

    private void showPaneData(Element pane, boolean show) {
      Element info = DomUtils.getFirstElementByClassName(pane, css.paneInfo());
      Element data = DomUtils.getFirstElementByClassName(pane, css.paneData());
      CssUtils.setDisplayVisibility(info, !show);
      CssUtils.setDisplayVisibility(data, show);
    }

    private void expandPane(Element pane, boolean expand) {
      CssUtils.setClassNameEnabled(pane, css.paneExpanded(), expand);
    }

    private void showNoApiPane(boolean show) {
      CssUtils.setDisplayVisibility(headerPane, !show);
      CssUtils.setDisplayVisibility(controlsPane, !show);
      CssUtils.setDisplayVisibility(watchExpressionsPane, !show);
      CssUtils.setDisplayVisibility(callStackPane, !show);
      CssUtils.setDisplayVisibility(scopeVariablesPane, !show);
      // Breakpoints pane stays always visible.
      CssUtils.setDisplayVisibility(noApiPane, show);
    }
  }

  public static DebuggingSidebar create(Resources resources, DebuggerState debuggerState) {
    View view = new View(resources);
    DebuggingSidebarHeader header = DebuggingSidebarHeader.create(view.headerView);
    DebuggingSidebarControlsPane controlsPane =
        DebuggingSidebarControlsPane.create(view.controlsPaneView);
    DebuggingSidebarWatchExpressionsPane watchExpressionsPane =
        DebuggingSidebarWatchExpressionsPane.create(view.watchExpressionsPaneView, debuggerState);
    DebuggingSidebarCallStackPane callStackPane =
        DebuggingSidebarCallStackPane.create(view.callStackPaneView);
    DebuggingSidebarScopeVariablesPane scopeVariablesPane =
        DebuggingSidebarScopeVariablesPane.create(view.scopeVariablesPaneView, debuggerState);
    DebuggingSidebarBreakpointsPane breakpointsPane =
        DebuggingSidebarBreakpointsPane.create(view.breakpointsPaneView);
    DomInspector domInspector = DomInspector.create(view.domInspectorView, debuggerState);
    ConsoleView console = ConsoleView.create(view.consoleView, debuggerState);
    DebuggingSidebarNoApiPane noApiPane =
        DebuggingSidebarNoApiPane.create(view.noApiPaneView, debuggerState);
    return new DebuggingSidebar(view, header, controlsPane, watchExpressionsPane, callStackPane,
        scopeVariablesPane, breakpointsPane, domInspector, console, noApiPane);
  }

  private static final Dispatcher<DebuggerCommandListener> ON_ACTIVATE_BREAKPOINTS_DISPATCHER =
      new Dispatcher<DebuggerCommandListener>() {
        @Override
        public void dispatch(DebuggerCommandListener listener) {
          listener.onActivateBreakpoints();
        }
      };
  private static final Dispatcher<DebuggerCommandListener> ON_DEACTIVATE_BREAKPOINTS_DISPATCHER =
      new Dispatcher<DebuggerCommandListener>() {
        @Override
        public void dispatch(DebuggerCommandListener listener) {
          listener.onDeactivateBreakpoints();
        }
      };

  private final class ViewEventsImpl implements DebuggingSidebarHeader.Listener,
      DebuggingSidebarControlsPane.Listener,
      DebuggingSidebarWatchExpressionsPane.Listener,
      DebuggingSidebarCallStackPane.Listener,
      DebuggingSidebarBreakpointsPane.Listener,
      DebuggingSidebarNoApiPane.Listener,
      ConsoleView.Listener {

    @Override
    public void onDebuggerCommand(final DebuggingSidebarControlsPane.DebuggerCommand command) {
      commandListenerManager.dispatch(new Dispatcher<DebuggerCommandListener>() {
        @Override
        public void dispatch(DebuggerCommandListener listener) {
          switch (command) {
            case PAUSE:
              listener.onPause();
              break;
            case RESUME:
              listener.onResume();
              break;
            case STEP_OVER:
              listener.onStepOver();
              break;
            case STEP_INTO:
              listener.onStepInto();
              break;
            case STEP_OUT:
              listener.onStepOut();
              break;
          }
        }
      });
    }

    @Override
    public void onCallFrameSelect(final int depth) {
      commandListenerManager.dispatch(new Dispatcher<DebuggerCommandListener>() {
        @Override
        public void dispatch(DebuggerCommandListener listener) {
          listener.onCallFrameSelect(depth);
        }
      });
    }

    @Override
    public void onBreakpointIconClick(final Breakpoint breakpoint) {
      commandListenerManager.dispatch(new Dispatcher<DebuggerCommandListener>() {
        @Override
        public void dispatch(DebuggerCommandListener listener) {
          listener.onBreakpointIconClick(breakpoint);
        }
      });
    }

    @Override
    public void onBreakpointLineClick(final Breakpoint breakpoint) {
      commandListenerManager.dispatch(new Dispatcher<DebuggerCommandListener>() {
        @Override
        public void dispatch(DebuggerCommandListener listener) {
          listener.onBreakpointLineClick(breakpoint);
        }
      });
    }

    @Override
    public void onActivateBreakpoints() {
      commandListenerManager.dispatch(ON_ACTIVATE_BREAKPOINTS_DISPATCHER);
    }

    @Override
    public void onDeactivateBreakpoints() {
      commandListenerManager.dispatch(ON_DEACTIVATE_BREAKPOINTS_DISPATCHER);
    }

    @Override
    public void onBeforeAddWatchExpression() {
      // Show the Watch Expressions pane tree.
      getView().expandPane(getView().watchExpressionsPane, true);
      getView().showPaneData(getView().watchExpressionsPane, true);
    }

    @Override
    public void onWatchExpressionsCountChange() {
      updateWatchExpressionsPaneState();
    }

    @Override
    public void onShouldDisplayNoApiPaneChange() {
      getView().showNoApiPane(noApiPane.shouldDisplay());
    }

    @Override
    public void onLocationLinkClick(final String url, final int lineNumber) {
      commandListenerManager.dispatch(new Dispatcher<DebuggerCommandListener>() {
        @Override
        public void dispatch(DebuggerCommandListener listener) {
          listener.onLocationLinkClick(url, lineNumber);
        }
      });
    }
  }

  private final ListenerManager<DebuggerCommandListener> commandListenerManager;
  private final DebuggingSidebarHeader header;
  private final DebuggingSidebarControlsPane controlsPane;
  private final DebuggingSidebarWatchExpressionsPane watchExpressionsPane;
  private final DebuggingSidebarCallStackPane callStackPane;
  private final DebuggingSidebarScopeVariablesPane scopeVariablesPane;
  private final DebuggingSidebarBreakpointsPane breakpointsPane;
  private final DomInspector domInspector;
  private final ConsoleView console;
  private final DebuggingSidebarNoApiPane noApiPane;

  private DebuggingSidebar(final View view, DebuggingSidebarHeader header,
      DebuggingSidebarControlsPane controlsPane,
      DebuggingSidebarWatchExpressionsPane watchExpressionsPane,
      DebuggingSidebarCallStackPane callStackPane,
      DebuggingSidebarScopeVariablesPane scopeVariablesPane,
      DebuggingSidebarBreakpointsPane breakpointsPane,
      DomInspector domInspector,
      ConsoleView console,
      DebuggingSidebarNoApiPane noApiPane) {
    super(view);

    this.commandListenerManager = ListenerManager.create();
    this.header = header;
    this.controlsPane = controlsPane;
    this.watchExpressionsPane = watchExpressionsPane;
    this.callStackPane = callStackPane;
    this.scopeVariablesPane = scopeVariablesPane;
    this.breakpointsPane = breakpointsPane;
    this.domInspector = domInspector;
    this.console = console;
    this.noApiPane = noApiPane;

    watchExpressionsPane.attachControlButtons(
        getView().getPaneTitle(getView().watchExpressionsPane));

    ViewEventsImpl delegate = new ViewEventsImpl();
    header.setListener(delegate);
    controlsPane.setListener(delegate);
    watchExpressionsPane.setListener(delegate);
    callStackPane.setListener(delegate);
    breakpointsPane.setListener(delegate);
    noApiPane.setListener(delegate);
    console.setListener(delegate);

    // Initialize the UI with the defaults.
    setActive(false);
    setPaused(false);
    setAllBreakpointsActive(true);
    setScopeVariablesRootNodes(null);
    refreshWatchExpressions();

    // Expand some panes.
    getView().expandPane(getView().callStackPane, true);
    getView().expandPane(getView().scopeVariablesPane, true);
    getView().expandPane(getView().breakpointsPane, true);
    getView().expandPane(getView().consolePane, true);

    getView().showNoApiPane(noApiPane.shouldDisplay());
  }

  public void setActive(boolean active) {
    controlsPane.setActive(active);
    if (active) {
      domInspector.show();
      console.show();
    } else {
      domInspector.hide();
      console.hide();
    }
    getView().showPaneData(getView().domInspectorPane, active);
    getView().showPaneData(getView().consolePane, active);
  }

  public void setPaused(boolean paused) {
    controlsPane.setPaused(paused);
  }

  public void setAllBreakpointsActive(boolean active) {
    header.setAllBreakpointsActive(active);
  }

  public void clearCallStack() {
    callStackPane.clearCallStack();
    getView().showPaneData(getView().callStackPane, false);
  }

  public void addCallFrame(String title, String subtitle) {
    getView().showPaneData(getView().callStackPane, true);
    callStackPane.addCallFrame(title, subtitle);
  }

  public void addBreakpoint(Breakpoint breakpoint) {
    if (breakpointsPane.hasBreakpoint(breakpoint)) {
      return;
    }

    breakpointsPane.addBreakpoint(breakpoint);
    updateBreakpointsPaneState();

    // If added a first breakpoint, expand the breakpoint pane automatically.
    if (breakpointsPane.getBreakpointCount() == 1) {
      getView().expandPane(getView().breakpointsPane, true);
    }
  }

  public void removeBreakpoint(Breakpoint breakpoint) {
    breakpointsPane.removeBreakpoint(breakpoint);
    updateBreakpointsPaneState();
  }

  public void updateBreakpoint(Breakpoint breakpoint, String line) {
    addBreakpoint(breakpoint); // Adds if absent.
    breakpointsPane.updateBreakpoint(breakpoint, line);
  }

  public String getBreakpointLineText(Breakpoint breakpoint) {
    return breakpointsPane.getBreakpointLineText(breakpoint);
  }

  private void updateBreakpointsPaneState() {
    boolean hasBreakpoints = breakpointsPane.getBreakpointCount() > 0;
    getView().showPaneData(getView().breakpointsPane, hasBreakpoints);
  }

  public void setScopeVariablesRootNodes(@Nullable JsonArray<RemoteObjectNode> rootNodes) {
    scopeVariablesPane.setScopeVariablesRootNodes(rootNodes);
    getView().showPaneData(getView().scopeVariablesPane, rootNodes != null);
  }

  public void refreshWatchExpressions() {
    watchExpressionsPane.refreshWatchExpressions();
    updateWatchExpressionsPaneState();
  }

  private void updateWatchExpressionsPaneState() {
    boolean hasWatchExpressions = watchExpressionsPane.getExpressionsCount() > 0;
    getView().showPaneData(getView().watchExpressionsPane, hasWatchExpressions);
  }

  public ListenerRegistrar<DebuggerCommandListener> getDebuggerCommandListenerRegistrar() {
    return commandListenerManager;
  }
}
