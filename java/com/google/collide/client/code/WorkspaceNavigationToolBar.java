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

import com.google.collide.client.common.BaseResources;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.workspace.outline.OutlineSection;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;

/**
 * Footer toolbar in the WorkspaceNavigation on the left of the CodePerspective below the file tree.
 */
public class WorkspaceNavigationToolBar extends UiComponent<WorkspaceNavigationToolBar.View> {

  /**
   * Creates the default version of the footer toolbar in the WorkspaceNavigation.
   */
  public static WorkspaceNavigationToolBar create(
      View view, CollaborationSection collabSection, OutlineSection outlineSection) {
    return new WorkspaceNavigationToolBar(view, collabSection, outlineSection);
  }

  /**
   * Style names associated with elements in the toolbar.
   */
  public interface Css extends CssResource {
    String outerBorder();

    String toolBar();

    String collaborateIcon();

    String deltaIcon();

    String navigatorIcon();
  }

  /**
   * Images and CssResources consumed by the WorkspaceNavigationToolBar.
   */
  public interface Resources extends Tooltip.Resources, BaseResources.Resources {
    @Source("collaborate_icon.png")
    ImageResource collaborateIcon();

    /**
     * Returns the image used for the workspace delta icon.
     *
     * The method deltaIcon is taken by
     * {@link com.google.collide.client.diff.DiffCommon.Resources#deltaIcon()}.
     */
    @Source("delta_icon.png")
    ImageResource workspaceDeltaIcon();

    @Source("navigator_icon.png")
    ImageResource navigatorIcon();

    @Source({"WorkspaceNavigationToolBar.css", "constants.css",
        "com/google/collide/client/common/constants.css"})
    Css workspaceNavigationToolBarCss();
  }

  /**
   * The View for the WorkspaceNavigationToolBar.
   */
  public static class View extends CompositeView<ViewEvents> {
    @UiTemplate("WorkspaceNavigationToolBar.ui.xml")
    interface MyBinder extends UiBinder<DivElement, View> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);

    @UiField
    DivElement toolBar;

    @UiField
    DivElement collaborateButton;

    @UiField
    DivElement collaborateIcon;

    @UiField
    DivElement deltaButton;

    @UiField
    DivElement deltaIcon;

    @UiField
    DivElement navigatorButton;

    @UiField
    DivElement navigatorIcon;

    @UiField(provided = true)
    final Resources res;

    private Element activeButton;

    public View(Resources res) {
      this.res = res;
      setElement(Elements.asJsElement(binder.createAndBindUi(this)));
      attachHandlers();

      // Tooltips
      Tooltip.create(res, Elements.asJsElement(collaborateButton),
          PositionController.VerticalAlign.TOP, PositionController.HorizontalAlign.MIDDLE,
          "Work with collaborators");
      Tooltip.create(res, Elements.asJsElement(deltaButton), PositionController.VerticalAlign.TOP,
          PositionController.HorizontalAlign.MIDDLE, "See what's changed in this branch");
      Tooltip.create(res, Elements.asJsElement(navigatorButton),
          PositionController.VerticalAlign.TOP, PositionController.HorizontalAlign.MIDDLE,
          "View the code navigator");
    }

    protected void attachHandlers() {
      getElement().setOnClick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          ViewEvents delegate = getDelegate();
          if (delegate == null) {
            return;
          }

          Node target = (Node) evt.getTarget();
          if (collaborateButton.isOrHasChild(target)) {
            delegate.onCollaborateButtonClicked();
          } else if (deltaButton.isOrHasChild(target)) {
            delegate.onDeltaButtonClicked();
          } else if (navigatorButton.isOrHasChild(target)) {
            delegate.onNavigatorButtonClicked();
          }
        }
      });
    }

    private void setActiveButton(Element button) {
      if (this.activeButton != null) {
        this.activeButton.removeClassName(res.baseCss().drawerIconButtonActiveLight());
      }

      this.activeButton = button;
      if (button != null) {
        button.addClassName(res.baseCss().drawerIconButtonActiveLight());
      }
    }
  }

  /**
   * Events reported by the EditorToolBar's View.
   */
  interface ViewEvents {
    void onCollaborateButtonClicked();

    void onDeltaButtonClicked();

    void onNavigatorButtonClicked();
  }

  private class ToolbarViewEvents implements WorkspaceNavigationToolBar.ViewEvents {

    @Override
    public void onCollaborateButtonClicked() {
      toggleSection(collabSection, Elements.asJsElement(getView().collaborateButton));
    }

    @Override
    public void onDeltaButtonClicked() {
      // used to show delta section
    }

    @Override
    public void onNavigatorButtonClicked() {
      toggleSection(outlineSection, Elements.asJsElement(getView().navigatorButton));
    }
  }

  private final OutlineSection outlineSection;
  private final CollaborationSection collabSection;
  private WorkspaceNavigation navigation;

  WorkspaceNavigationToolBar(
      View view, CollaborationSection collabSection, OutlineSection outlineSection) {
    super(view);
    this.collabSection = collabSection;
    this.outlineSection = outlineSection;

    getView().setDelegate(new ToolbarViewEvents());
  }

  public void setWorkspaceNavigation(WorkspaceNavigation navigation) {
    this.navigation = navigation;
  }

  void hideDeltaButton() {
    CssUtils.setDisplayVisibility2((Element) (getView().deltaButton), false);
  }

  private boolean toggleSection(WorkspaceNavigationSection<?> section, Element button) {
    if (navigation == null) {
      return false;
    }

    boolean showing = navigation.toggleBottomSection(section);
    if (showing) {
      getView().setActiveButton(button);
    } else {
      getView().setActiveButton(null);
    }
    return showing;
  }
}
