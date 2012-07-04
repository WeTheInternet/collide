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

import com.google.collide.client.AppContext;
import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.code.FileSelectedPlace;
import com.google.collide.client.code.RightSidebarExpansionEvent;
import com.google.collide.client.code.popup.EditorPopupController;
import com.google.collide.client.communication.ResourceUriUtils;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.gutter.Gutter;
import com.google.collide.client.history.Place;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.PopupBlockedInstructionalPopup;
import com.google.collide.client.workspace.RunApplicationEvent;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.common.base.Preconditions;

import elemental.client.Browser;
import elemental.html.Element;
import elemental.html.Window;

/**
 * A controller for the debugging model state.
 */
public class DebuggingModelController {

  public static DebuggingModelController create(Place currentPlace, AppContext appContext,
      DebuggingModel debuggingModel, Editor editor, EditorPopupController editorPopupController,
      DocumentManager documentManager) {
    DebuggingModelController dmc = new DebuggingModelController(currentPlace, appContext,
        debuggingModel, editor, editorPopupController, documentManager);
    dmc.populateDebuggingSidebar();

    // Register the DebuggingModelController as a handler for clicks on the
    // Header's run button.
    currentPlace.registerSimpleEventHandler(RunApplicationEvent.TYPE, dmc.runApplicationHandler);
    return dmc;
  }

  /**
   * Flag indicating that "popup blocked" alert was shown once to user, so we
   * should not annoy user with it again.
   *
   * <p>This flag is used by and changed by {@link #createOrOpenPopup}.
   */
  private static boolean doNotShowPopupBlockedInstruction;

  /**
   * Creates or reuses launchpad window.
   *
   * <p>This method should be called from user initiated event handler.
   *
   * <p>If popup window already exists, it is cleared. Launchpad is filled
   * with disclaimer that informs user that deployment is in progress.
   *
   * <p>Actually, popup is never blocked, because it is initiated by user.
   * But in case it is not truth, we show alert to user that instructs how to
   * enable popups.
   */
  public static Window createOrOpenPopup(AppContext appContext) {
    Window popup =
        Browser.getWindow().open("", BootstrapSession.getBootstrapSession().getActiveClientId());
    if (popup == null) {
      if (!doNotShowPopupBlockedInstruction) {
        doNotShowPopupBlockedInstruction = true;
        PopupBlockedInstructionalPopup.create(appContext.getResources()).show();
      }
    }
    return popup;
  }

  /**
   * Handler for clicks on the run button on the workspace header.
   */
  private final RunApplicationEvent.Handler runApplicationHandler =
      new RunApplicationEvent.Handler() {
        @Override
        public void onRunButtonClicked(RunApplicationEvent evt) {
          runApplication(evt.getUrl());
        }
      };

  private final DebuggingModel.DebuggingModelChangeListener debuggingModelChangeListener =
      new DebuggingModel.DebuggingModelChangeListener() {
        @Override
        public void onBreakpointAdded(Breakpoint newBreakpoint) {
          if (shouldProcessBreakpoint(newBreakpoint) && !breakpoints.contains(newBreakpoint)) {
            anchorBreakpointAndUpdateSidebar(newBreakpoint);
            debuggingModelRenderer.renderBreakpointOnGutter(
                newBreakpoint.getLineNumber(), newBreakpoint.isActive());
          }
          debuggerState.setBreakpoint(newBreakpoint);
          debuggingSidebar.addBreakpoint(newBreakpoint);
        }

        @Override
        public void onBreakpointRemoved(Breakpoint oldBreakpoint) {
          if (shouldProcessBreakpoint(oldBreakpoint) && breakpoints.contains(oldBreakpoint)) {
            breakpoints.removeBreakpoint(oldBreakpoint);
            debuggingModelRenderer.removeBreakpointOnGutter(oldBreakpoint.getLineNumber());
          }
          debuggerState.removeBreakpoint(oldBreakpoint);
          debuggingSidebar.removeBreakpoint(oldBreakpoint);
        }

        @Override
        public void onBreakpointReplaced(Breakpoint oldBreakpoint, Breakpoint newBreakpoint) {
          /*
           * If a breakpoint is not in the document being displayed, we do not
           * know the code line that it was attached to, thus we save the old
           * breakpoint's line, if any.
           */
          String breakpointLine = debuggingSidebar.getBreakpointLineText(oldBreakpoint);

          onBreakpointRemoved(oldBreakpoint);
          onBreakpointAdded(newBreakpoint);

          if (!shouldProcessBreakpoint(newBreakpoint)) {
            debuggingSidebar.updateBreakpoint(newBreakpoint, breakpointLine);
          }
        }

        @Override
        public void onPauseOnExceptionsModeUpdated(DebuggingModel.PauseOnExceptionsMode oldMode,
            DebuggingModel.PauseOnExceptionsMode newMode) {
          // TODO: Implement this in the UI.
          // TODO: Update DebuggerState.
        }

        @Override
        public void onBreakpointsEnabledUpdated(boolean newValue) {
          debuggerState.setBreakpointsEnabled(newValue);
          debuggingSidebar.setAllBreakpointsActive(newValue);
        }
      };

  private final Gutter.ClickListener leftGutterClickListener = new Gutter.ClickListener() {
    @Override
    public void onClick(int y) {
      int lineNumber = editor.getBuffer().convertYToLineNumber(y, true);
      for (int i = 0; i < breakpoints.size(); ++i) {
        Breakpoint breakpoint = breakpoints.get(i);
        if (breakpoint.getLineNumber() == lineNumber) {
          debuggingModel.removeBreakpoint(breakpoint);
          return;
        }
      }

      if (!isCurrentDocumentEligibleForDebugging()) {
        return;
      }

      Breakpoint breakpoint = new Breakpoint.Builder(path, lineNumber).build();
      debuggingModel.addBreakpoint(breakpoint);

      // Show the sidebar if the very first breakpoint has just been set.
      maybeShowSidebar();
    }
  };

  private boolean isCurrentDocumentEligibleForDebugging() {
    Preconditions.checkNotNull(path);
    // TODO: Be more smart here. Also what about source mappings?
    String baseName = path.getBaseName().toLowerCase();
    return baseName.endsWith(".js") || baseName.endsWith(".htm") || baseName.endsWith(".html");
  }

  /**
   * Handler that receives events from the remote debugger about changes of it's
   * state (e.g. a {@code running} debugger changed to {@code paused}, etc.).
   */
  private final DebuggerState.DebuggerStateListener debuggerStateListener =
      new DebuggerState.DebuggerStateListener() {
        @Override
        public void onDebuggerStateChange() {
          if (debuggerState.isPaused()) {
            showSidebar();
          }

          debuggingModelRenderer.renderDebuggerState();
          handleOnCallFrameSelect(0);
        }
      };

  /**
   * Handler that receives user commands, such as clicks on the debugger control
   * buttons in the sidebar.
   */
  private final DebuggingSidebar.DebuggerCommandListener userCommandListener =
      new DebuggingSidebar.DebuggerCommandListener() {
        @Override
        public void onPause() {
          debuggerState.pause();
        }

        @Override
        public void onResume() {
          debuggerState.resume();
        }

        @Override
        public void onStepOver() {
          debuggerState.stepOver();
        }

        @Override
        public void onStepInto() {
          debuggerState.stepInto();
        }

        @Override
        public void onStepOut() {
          debuggerState.stepOut();
        }

        @Override
        public void onCallFrameSelect(int depth) {
          handleOnCallFrameSelect(depth);
        }

        @Override
        public void onBreakpointIconClick(Breakpoint breakpoint) {
          Breakpoint newBreakpoint = new Breakpoint.Builder(breakpoint)
              .setActive(!breakpoint.isActive())
              .build();
          debuggingModel.updateBreakpoint(breakpoint, newBreakpoint);
        }

        @Override
        public void onBreakpointLineClick(Breakpoint breakpoint) {
          maybeNavigateToDocument(breakpoint.getPath(), breakpoint.getLineNumber());
        }

        @Override
        public void onActivateBreakpoints() {
          debuggingModel.setBreakpointsEnabled(true);
        }

        @Override
        public void onDeactivateBreakpoints() {
          debuggingModel.setBreakpointsEnabled(false);
        }

        @Override
        public void onLocationLinkClick(String url, int lineNumber) {
          SourceMapping sourceMapping = debuggerState.getSourceMapping();
          if (sourceMapping == null) {
            // Ignore. Maybe the debugger has just been shutdown.
            return;
          }

          PathUtil path = sourceMapping.getLocalSourcePath(url);
          if (path != null) {
            maybeNavigateToDocument(path, lineNumber);
          }
        }
      };

  /**
   * Handler that receives notifications when a breakpoint description changes.
   */
  private final AnchoredBreakpoints.BreakpointDescriptionListener breakpointDescriptionListener =
      new AnchoredBreakpoints.BreakpointDescriptionListener() {
        @Override
        public void onBreakpointDescriptionChange(Breakpoint breakpoint, String newText) {
          debuggingSidebar.updateBreakpoint(breakpoint, newText);
        }
      };

  private final AppContext appContext;
  private final Editor editor;
  private final DebuggingModel debuggingModel;
  private final ListenerRegistrar.Remover leftGutterClickListenerRemover;
  private final DebuggerState debuggerState;
  private final DebuggingSidebar debuggingSidebar;
  private final DebuggingModelRenderer debuggingModelRenderer;
  private final Place currentPlace;
  private final CssLiveEditController cssLiveEditController;
  private final EvaluationPopupController evaluationPopupController;

  private PathUtil path;
  private AnchoredBreakpoints breakpoints;

  /**
   * Indicator that sidebar has been discovered by user.
   */
  private boolean sidebarDiscovered;

  private DebuggingModelController(Place currentPlace, AppContext appContext,
      DebuggingModel debuggingModel, Editor editor, EditorPopupController editorPopupController,
      DocumentManager documentManager) {
    this.appContext = appContext;
    this.editor = editor;
    this.currentPlace = currentPlace;
    this.debuggingModel = debuggingModel;
    this.leftGutterClickListenerRemover =
        editor.getLeftGutter().getClickListenerRegistrar().add(leftGutterClickListener);

    // Every time we enter workspace, we get a new debugging session id.
    String sessionId = BootstrapSession.getBootstrapSession().getActiveClientId() + ":"
        + System.currentTimeMillis();
    this.debuggerState = DebuggerState.create(sessionId);

    this.debuggingSidebar = DebuggingSidebar.create(appContext.getResources(), debuggerState);
    this.debuggingModelRenderer =
        DebuggingModelRenderer.create(appContext, editor, debuggingSidebar, debuggerState);
    this.cssLiveEditController = new CssLiveEditController(debuggerState, documentManager);
    this.evaluationPopupController = EvaluationPopupController.create(
        appContext.getResources(), editor, editorPopupController, debuggerState);

    this.debuggingModel.addModelChangeListener(debuggingModelChangeListener);
    this.debuggerState.getDebuggerStateListenerRegistrar().add(debuggerStateListener);
    this.debuggingSidebar.getDebuggerCommandListenerRegistrar().add(userCommandListener);
  }

  public void setDocument(Document document, PathUtil path, DocumentParser parser) {
    if (breakpoints != null) {
      breakpoints.teardown();
      debuggingModelRenderer.handleDocumentChanged();
    }
    this.path = path;
    breakpoints = new AnchoredBreakpoints(debuggingModel, document);
    anchorBreakpoints();
    maybeAnchorExecutionLine();

    evaluationPopupController.setDocument(document, path, parser);
    breakpoints.setBreakpointDescriptionListener(breakpointDescriptionListener);
  }

  public Element getDebuggingSidebarElement() {
    return debuggingSidebar.getView().getElement();
  }

  /**
   * @see #runApplication(String)
   */
  public boolean runApplication(PathUtil applicationPath) {
    String baseUri = ResourceUriUtils.getAbsoluteResourceBaseUri();
    SourceMapping sourceMapping = StaticSourceMapping.create(baseUri);
    return runApplication(sourceMapping, sourceMapping.getRemoteSourceUri(applicationPath));
  }

  /**
   * Runs a given resource via debugger API if it is available, or just opens
   * the resource in a new window.
   *
   * @param absoluteResourceUri absolute resource URI
   * @return {@code true} if the application was started successfully via
   *         debugger API, {@code false} if it was opened in a new window
   */
  public boolean runApplication(String absoluteResourceUri) {
    final String baseUri;

    // Check if the URI points to a local path.
    String workspaceBaseUri = ResourceUriUtils.getAbsoluteResourceBaseUri();
    if (absoluteResourceUri.startsWith(workspaceBaseUri + "/")) {
      baseUri = workspaceBaseUri;
    } else {
      baseUri = ResourceUriUtils.extractBaseUri(absoluteResourceUri);
    }

    SourceMapping sourceMapping = StaticSourceMapping.create(baseUri);
    return runApplication(sourceMapping, absoluteResourceUri);
  }

  private boolean runApplication(SourceMapping sourceMapping, String absoluteResourceUri) {
    if (debuggerState.isDebuggerAvailable()) {
      debuggerState.runDebugger(sourceMapping, absoluteResourceUri);

      JsonArray<Breakpoint> allBreakpoints = debuggingModel.getBreakpoints();
      for (int i = 0; i < allBreakpoints.size(); ++i) {
        debuggerState.setBreakpoint(allBreakpoints.get(i));
      }

      debuggerState.setBreakpointsEnabled(debuggingModel.isBreakpointsEnabled());
      return true;
    } else {
      Window popup = createOrOpenPopup(appContext);
      if (popup != null) {
        popup.getLocation().assign(absoluteResourceUri);
        // Show the sidebar once to promote the Debugger Extension.
        maybeShowSidebar();
      }
      return false;
    }
  }

  private void anchorBreakpoints() {
    JsonArray<Breakpoint> allBreakpoints = debuggingModel.getBreakpoints();
    for (int i = 0; i < allBreakpoints.size(); ++i) {
      Breakpoint breakpoint = allBreakpoints.get(i);
      if (path.equals(breakpoint.getPath())) {
        anchorBreakpointAndUpdateSidebar(breakpoint);
        debuggingModelRenderer.renderBreakpointOnGutter(
            breakpoint.getLineNumber(), breakpoint.isActive());
      }
    }
  }

  private void populateDebuggingSidebar() {
    JsonArray<Breakpoint> allBreakpoints = debuggingModel.getBreakpoints();
    for (int i = 0; i < allBreakpoints.size(); ++i) {
      debuggingSidebar.addBreakpoint(allBreakpoints.get(i));
    }
  }

  private void anchorBreakpointAndUpdateSidebar(Breakpoint breakpoint) {
    Anchor anchor = breakpoints.anchorBreakpoint(breakpoint);
    debuggingSidebar.updateBreakpoint(breakpoint, anchor.getLine().getText());
  }

  private void maybeAnchorExecutionLine() {
    PathUtil callFramePath = debuggerState.getActiveCallFramePath();
    if (callFramePath != null && callFramePath.equals(path)) {
      int lineNumber = debuggerState.getActiveCallFrameExecutionLineNumber();
      if (lineNumber >= 0) {
        debuggingModelRenderer.renderExecutionLine(lineNumber);
      }
    }
  }

  private void showSidebar() {
    sidebarDiscovered = true;
    currentPlace.fireEvent(new RightSidebarExpansionEvent(true));
  }

  /**
   * Shows sidebar in case user hasn't discovered yet, and at least one
   * breakpoint is set.
   */
  private void maybeShowSidebar() {
    if (!sidebarDiscovered && debuggingModel.getBreakpointCount() != 0) {
      showSidebar();
    }
  }

  private boolean shouldProcessBreakpoint(Breakpoint breakpoint) {
    return path != null && path.equals(breakpoint.getPath());
  }

  private void handleOnCallFrameSelect(int callFrameDepth) {
    debuggerState.setActiveCallFrameIndex(callFrameDepth);
    debuggingModelRenderer.renderDebuggerCallFrame();

    PathUtil callFramePath = debuggerState.getActiveCallFramePath();
    if (callFramePath == null) {
      // Not paused, remove the execution line (if any).
      debuggingModelRenderer.removeExecutionLine();
      return;
    }

    maybeAnchorExecutionLine();

    int lineNumber = debuggerState.getActiveCallFrameExecutionLineNumber();
    maybeNavigateToDocument(callFramePath, lineNumber);
  }

  private void maybeNavigateToDocument(PathUtil documentPath, int lineNumber) {
    if (documentPath.equals(this.path)) {
      editor.getFocusManager().focus();
      editor.scrollTo(lineNumber, 0);
    } else {
      currentPlace.fireChildPlaceNavigation(
          FileSelectedPlace.PLACE.createNavigationEvent(documentPath, lineNumber));
    }
  }

  public void cleanup() {
    debuggerState.shutdown();
    leftGutterClickListenerRemover.remove();
  }
}
