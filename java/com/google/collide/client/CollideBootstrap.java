package com.google.collide.client;

import xapi.inject.impl.LazyPojo;
import xapi.util.api.SuccessHandler;
import collide.client.util.Elements;

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.util.ClientImplementationsInjector;
import com.google.collide.client.xhrmonitor.XhrWarden;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.Window;

import elemental.html.DivElement;

public class CollideBootstrap extends LazyPojo<AppContext>{

  private CollideBootstrap() {}
  
  private static final CollideBootstrap bootstrap = new CollideBootstrap();

  public static void start(SuccessHandler<AppContext> successHandler) {
    
    // If we do not have a valid Client ID, we need to popup a login page.
    if (BootstrapSession.getBootstrapSession().getActiveClientId() == null) {
      if (Window.Location.getPath().startsWith("/collide"))
        Window.Location.assign("/collide/static/login.html");
      else
        Window.Location.assign("/static/login.html");
      return;
    }
    successHandler.onSuccess(bootstrap.get());
  }

  
  protected AppContext initialValue() {

    if (Elements.getElementById("gwt_root")==null) {
      DivElement el = Elements.createDivElement();
      el.setId("gwt_root");
      Elements.getBody().appendChild(el);
    }

    ClientImplementationsInjector.inject();

    final AppContext appContext = AppContext.create();
    
    GWT.setUncaughtExceptionHandler(appContext.getUncaughtExceptionHandler());
    XhrWarden.watch();

    Resources resources = appContext.getResources();

    // TODO: Figure out why when we use the + operator to concat,
    // these Strings don't at compile time converge to a single String literal.
    // In theory they should. For now we use a StringBuilder.

    // Make sure you call getText() on your CssResource!
    StringBuilder styleBuilder = new StringBuilder();
    styleBuilder.append(resources.appCss().getText());
    styleBuilder.append(resources.baseCss().getText());
    styleBuilder.append(resources.workspaceHeaderCss().getText());
    styleBuilder.append(resources.editorToolBarCss().getText());
    styleBuilder.append(resources.defaultSimpleListCss().getText());
    styleBuilder.append(resources.workspaceShellCss().getText());
    styleBuilder.append(resources.workspaceEditorCss().getText());
    styleBuilder.append(resources.workspaceEditorBufferCss().getText());
    styleBuilder.append(resources.workspaceEditorCursorCss().getText());
    styleBuilder.append(resources.workspaceEditorConsoleViewCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingModelCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarBreakpointsPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarCallStackPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarControlsPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarHeaderCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarNoApiPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarScopeVariablesPaneCss().getText());
    styleBuilder.append(resources.workspaceEditorDomInspectorCss().getText());
    styleBuilder.append(resources.workspaceEditorDebuggingSidebarWatchExpressionsPaneCss().getText());
    styleBuilder.append(resources.remoteObjectTreeCss().getText());
    styleBuilder.append(resources.remoteObjectNodeRendererCss().getText());
    styleBuilder.append(resources.editorDiffContainerCss().getText());
    styleBuilder.append(resources.evaluationPopupControllerCss().getText());
    styleBuilder.append(resources.goToDefinitionCss().getText());
    styleBuilder.append(resources.treeCss().getText());
    styleBuilder.append(resources.workspaceNavigationCss().getText());
    styleBuilder.append(resources.workspaceNavigationFileTreeSectionCss().getText());
    styleBuilder.append(resources.workspaceNavigationShareWorkspacePaneCss().getText());
    styleBuilder.append(resources.workspaceNavigationToolBarCss().getText());
    styleBuilder.append(resources.workspaceNavigationFileTreeNodeRendererCss().getText());
    styleBuilder.append(resources.workspaceNavigationOutlineNodeRendererCss().getText());
    styleBuilder.append(resources.workspaceNavigationParticipantListCss().getText());
    styleBuilder.append(resources.searchContainerCss().getText());
    styleBuilder.append(resources.statusPresenterCss().getText());
    styleBuilder.append(resources.noFileSelectedPanelCss().getText());
    styleBuilder.append(resources.diffRendererCss().getText());
    styleBuilder.append(resources.deltaInfoBarCss().getText());
    styleBuilder.append(resources.codePerspectiveCss().getText());
    styleBuilder.append(resources.unauthorizedUserCss().getText());
    styleBuilder.append(resources.syntaxHighlighterRendererCss().getText());
    styleBuilder.append(resources.lineNumberRendererCss().getText());
    styleBuilder.append(resources.uneditableDisplayCss().getText());
    styleBuilder.append(resources.editorSelectionLineRendererCss().getText());
    styleBuilder.append(resources.fileHistoryCss().getText());
    styleBuilder.append(resources.timelineCss().getText());
    styleBuilder.append(resources.timelineNodeCss().getText());
    styleBuilder.append(resources.panelCss().getText());
    styleBuilder.append(resources.popupCss().getText());
    styleBuilder.append(resources.tooltipCss().getText());
    styleBuilder.append(resources.sliderCss().getText());
    styleBuilder.append(resources.editableContentAreaCss().getText());
    styleBuilder.append(resources.workspaceLocationBreadcrumbsCss().getText());
    styleBuilder.append(resources.awesomeBoxCss().getText());
    styleBuilder.append(resources.awesomeBoxSectionCss().getText());
    styleBuilder.append(resources.centerPanelCss().getText());
    styleBuilder.append(resources.autocompleteComponentCss().getText());
    styleBuilder.append(resources.runButtonTargetPopupCss().getText());
    styleBuilder.append(resources.popupBlockedInstructionalPopupCss().getText());
    styleBuilder.append(resources.dropdownWidgetsCss().getText());
    styleBuilder.append(resources.parenMatchHighlighterCss().getText());
    styleBuilder.append(resources.awesomeBoxHostCss().getText());
    styleBuilder.append(resources.awesomeBoxComponentCss().getText());
    styleBuilder.append(resources.coachmarkCss().getText());
    styleBuilder.append(resources.sidebarListCss().getText());

    /* workspaceNavigationSectionCss, animationController, and resizeControllerCss must come last because they
     * overwrite the CSS properties from previous CSS rules. */
    styleBuilder.append(resources.workspaceNavigationSectionCss().getText());
    styleBuilder.append(resources.resizeControllerCss().getText());

    StyleInjector.inject(styleBuilder.toString());
    Elements.injectJs(CodeMirror2.getJs(resources));
    
    return appContext;
  }

}
