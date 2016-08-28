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

import collide.client.filetree.FileTreeUiController;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.gotodefinition.GoToDefinitionRenderer;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.ui.panel.PanelModel;
import com.google.collide.client.ui.panel.PanelModel.Builder;
import com.google.collide.mvp.ShowableUiComponent;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;
import elemental.html.DivElement;

/**
 * The main content area on the CodePerspective.
 *
 */
public class EditableContentArea extends MultiPanel<PanelModel, EditableContentArea.View> {

  /**
   * Static factory method for obtaining an instance of the EditableContentArea.
   */
  public static EditableContentArea create(
      View view, AppContext appContext, EditorBundle editorBundle, FileTreeUiController controller) {

    final EditorToolBar toolBar = EditorToolBar.create(
        view.getEditorToolBarView(), FileSelectedPlace.PLACE, appContext, editorBundle);
    // Hook presenter in the editor bundle to the view in the header
    editorBundle.getBreadcrumbs().setView(view.getBreadcrumbsView());
    return new EditableContentArea(view, toolBar, controller);
  }


  /**
   * Style names.
   */
  public interface Css extends CssResource {
    String base();

    String contentHeader();

    String contentContainer();

    int editableContentAreaRight();
  }

  public interface Resources
      extends
      GoToDefinitionRenderer.Resources,
      EditorToolBar.Resources,
      WorkspaceLocationBreadcrumbs.Resources,
      NoFileSelectedPanel.Resources,
      UneditableDisplay.Resources {

    @Source({"collide/client/common/constants.css", "EditableContentArea.css"})
    Css editableContentAreaCss();
  }

  /**
   * The View for the EditableContentArea.
   */
  public static class View extends MultiPanel.View<PanelModel> {
    private DivElement header;
    private DivElement content;
    private final WorkspaceLocationBreadcrumbs.View breadcrumbsView;
    private final EditorToolBar.View editorToolBarView;
    private final Css css;

    public View(Resources res, boolean detached) {
      super(Elements.createDivElement(res.editableContentAreaCss().base()), detached);
      this.css = res.editableContentAreaCss();

      // Instantiate sub-views.
      this.breadcrumbsView = new WorkspaceLocationBreadcrumbs.View(res);
      this.editorToolBarView = new EditorToolBar.View(res, detached);

      createDom();
    }

    @Override
    public Element getContentElement() {
      return content;
    }

    @Override
    public Element getHeaderElement() {
      return header;
    }

    public EditorToolBar.View getEditorToolBarView() {
      return editorToolBarView;
    }

    public WorkspaceLocationBreadcrumbs.View getBreadcrumbsView() {
      return breadcrumbsView;
    }

    public int getDefaultEditableContentAreaRight() {
      return css.editableContentAreaRight();
    }

    private void createDom() {
      Element elem = getElement();
      header = Elements.createDivElement(css.contentHeader());
      content = Elements.createDivElement(css.contentContainer());

      elem.appendChild(header);
      elem.appendChild(content);
      header.appendChild(breadcrumbsView.getElement());
      header.appendChild(editorToolBarView.getElement());
    }
  }

  private final EditorToolBar toolBar;
  private FileTreeUiController fileController;

  @Override
  public void clearNavigator() {
    fileController.clearSelectedNodes();
  }

  @Override
  public void setContent(PanelContent panelContent, PanelModel settings) {
    if (panelContent instanceof FileContent) {
      super.setContent(panelContent, settings);
    }else {
      //use an inner multipanel to add, instead of replace, content panels.
      super.setContent(panelContent, settings);
    }
  };

  public EditableContentArea(View view, EditorToolBar toolBar, FileTreeUiController controller) {
    super(view);
    this.toolBar = toolBar;
    this.fileController = controller;
  }

  @Override
  public ShowableUiComponent<?> getToolBar() {
    return getEditorToolBar();
  }

  public EditorToolBar getEditorToolBar() {
    return toolBar;
  }

  @Override
  public Builder<PanelModel> newBuilder() {
    return defaultBuilder();
  }
  
}
