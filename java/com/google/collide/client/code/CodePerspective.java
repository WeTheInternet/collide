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

package com.google.collide.client.code;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.autocomplete.integration.AutocompleteUiController;
import com.google.collide.client.code.debugging.DebuggingModelRenderer;
import com.google.collide.client.code.debugging.DebuggingSidebar;
import com.google.collide.client.code.debugging.EvaluationPopupController;
import com.google.collide.client.code.parenmatch.ParenMatchHighlighter;
import com.google.collide.client.diff.DeltaInfoBar;
import com.google.collide.client.diff.EditorDiffContainer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.renderer.LineNumberRenderer;
import com.google.collide.client.filehistory.FileHistory;
import com.google.collide.client.filehistory.TimelineNode;
import com.google.collide.client.history.Place;
import com.google.collide.client.syntaxhighlighter.SyntaxHighlighterRenderer;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.ResizeController;
import com.google.collide.client.workspace.Header;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.CssResource;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.html.DivElement;

// TODO: Rename the editor package to the code package since it should
// encapsulate the CodePerspective. Then move code editor code to editor.core etc...
/**
 * Presenter for the code perspective.
 *
 *  Contains:
 *
 *  1. Project navigation tree for current workspace.
 *
 *  2. Our working content area (where will attach the editor).
 *
 *  3. Right sidebar (where will attach debugging state panel).
 *
 *  4. PanelContent area header (where will be shown path to the file, and other
 * controls).
 *
 */
public class CodePerspective extends UiComponent<CodePerspective.View> {

  /**
   * Static factory method for obtaining an instance of the CodePerspective.
   */
  public static CodePerspective create(View view,
      Place currentPlace,
      WorkspaceNavigation nav,
      EditableContentArea contentArea,
      AppContext context,
      boolean detached) {
    CodePerspective codePerspective =
        new CodePerspective(view, currentPlace, nav, contentArea, context);
    codePerspective.initResizeControllers(detached);
    return codePerspective;
  }

  /**
   * CSS and images used by the CodePerspective.
   */
  public interface Resources
      extends
      Header.Resources,
      FileHistory.Resources,
      TimelineNode.Resources,
      WorkspaceNavigation.Resources,
      EditorDiffContainer.Resources,
      Editor.Resources,
      SyntaxHighlighterRenderer.Resources,
      LineNumberRenderer.Resources,
      ParenMatchHighlighter.Resources,
      DebuggingModelRenderer.Resources,
      DebuggingSidebar.Resources,
      EvaluationPopupController.Resources,
      DeltaInfoBar.Resources,
      CodeMirror2.Resources,
      EditableContentArea.Resources,
      AutocompleteUiController.Resources {

    @Source({"CodePerspective.css", "com/google/collide/client/common/constants.css"})
    Css codePerspectiveCss();
  }

  /**
   * Style names.
   */
  public interface Css extends CssResource {

    String base();

    int collapsedRightSplitterRight();

    String navArea();

    int navWidth();

    String resizing();

    String splitter();

    String rightSplitter();

    int splitterWidth();

    int splitterOverlap();

    String rightSidebarContentContainer();
  }

  /**
   * The View for the CodePerspective.
   */
  public static class View extends CompositeView<Void> {
    private final Resources res;
    private final Css css;

    private final WorkspaceNavigation.View navView;
    private final EditableContentArea.View contentView;

    /**
     * Wrapper element for the WorkspaceNavigation.View. We need a wrapper
     * because of the behavior of flex box.
     */
    private DivElement navArea;

    /** Splitter between the navigator and content area. */
    private DivElement splitter;

    /** Splitter between the content area and right sidebar. */
    private DivElement rightSplitter;

    /** Container for the right sidebar. */
    private DivElement rightSidebarContentContainer;

    public View(Resources res, boolean detached) {
      this.res = res;
      this.css = res.codePerspectiveCss();

      this.navView = new WorkspaceNavigation.View(res);
      this.contentView = initView(res, detached);

      // Create the DOM and connect the elements together.
      setElement(Elements.createDivElement(css.base()));
      createDom();
    }
    

    public EditableContentArea.View initView(
        Resources res, boolean detached) {
      return new EditableContentArea.View(res, detached);
    }

    public Element getSidebarElement() {
      return rightSidebarContentContainer;
    }

    public Element getNavigationElement() {
      return navArea;
    }

    public WorkspaceNavigation.View getNavigationView() {
      return navView;
    }

    public EditableContentArea.View getContentView() {
      return contentView;
    }

    public Element getSplitter() {
      return splitter;
    }

    public Element getRightSplitter() {
      return rightSplitter;
    }

    public Element getRightSidebarElement() {
      return rightSidebarContentContainer;
    }

    /**
     * Create the initial DOM structure for the workspace shell.
     */
    private void createDom() {

      // Instantiate DOM elems.
      navArea = Elements.createDivElement(css.navArea());
      splitter = Elements.createDivElement(css.splitter());

      rightSplitter = Elements.createDivElement(css.rightSplitter());
      rightSidebarContentContainer = Elements.createDivElement(css.rightSidebarContentContainer());

      Element elem = getElement();
      navArea.appendChild(navView.getElement());
      elem.appendChild(navArea);
      elem.appendChild(splitter);
      elem.appendChild(contentView.getElement());

      // Attach the right sidebar and splitter to the base element of the
      // WorkspaceContentArea.View.
      contentView.getElement().appendChild(rightSplitter);
      contentView.getElement().appendChild(rightSidebarContentContainer);

      rightSidebarContentContainer.getStyle().setVisibility(CSSStyleDeclaration.Visibility.HIDDEN);
    }

    public Element detach() {
      contentView.getElement().removeChild(rightSplitter);
      contentView.getElement().removeChild(rightSidebarContentContainer);
      getElement().removeChild(contentView.getElement());
      getElement().removeChild(splitter);
      navArea.getStyle().setWidth(100, "%");
      contentView.getElement().getStyle().setLeft(0, "px");
      contentView.getElement().getStyle().setRight(5, "px");
      contentView.getElement().getStyle().setTop(0, "px");
      getElement().getStyle().setTop(0, "em");
//      contentView.getContentElement().addClassName("");
      return contentView.getElement();
    }
  }


  final WorkspaceNavigation nav;
  final EditableContentArea contentArea;
  private ResizeController leftResizeController;
  private ResizeController rightResizeController;
  private final Place currentPlace;

  CodePerspective(View view,
      Place currentPlace,
      WorkspaceNavigation nav,
      EditableContentArea contentArea,
      AppContext context) {
    super(view);
    this.currentPlace = currentPlace;
    this.nav = nav;
    this.contentArea = contentArea;
  }


  public EditableContentArea getContentArea() {
    return contentArea;
  }

  private void initResizeControllers(boolean detached) {
    View view = getView();
    leftResizeController = new NavigatorAreaResizeController(currentPlace,
        view.res,
        view.splitter,
        view.navArea,
        view.contentView.getElement(),
        view.css.splitterWidth(),
        view.css.splitterOverlap());
    leftResizeController.start();

    if (detached) {
      CssUtils.setDisplayVisibility2(view.rightSplitter, false);
      CssUtils.setDisplayVisibility2(view.rightSidebarContentContainer, false);
    }
    rightResizeController = new RightSidebarResizeController(currentPlace,
        view.res,
        view.rightSplitter,
        view.rightSidebarContentContainer,
        view.contentView.getContentElement(),
        view.css.splitterWidth(),
        view.css.collapsedRightSplitterRight(),
        contentArea.getView().getDefaultEditableContentAreaRight());
    rightResizeController.setNegativeDeltaW(true);
    rightResizeController.start();
  }
}
