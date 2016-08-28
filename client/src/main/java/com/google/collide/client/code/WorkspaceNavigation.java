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

import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.util.ResizeController;
import com.google.collide.client.util.ResizeController.ResizeProperty;
import com.google.collide.client.workspace.outline.OutlineSection;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

/**
 * The navigation area for the workspace Shell.
 */
public class WorkspaceNavigation extends UiComponent<WorkspaceNavigation.View> {

  /**
   * Static factory method for obtaining an instance of the WorkspaceNavigation.
   */
  public static WorkspaceNavigation create(View view, WorkspaceNavigationSection<?>[] topSections,
      WorkspaceNavigationSection<?>[] bottomSections,
      WorkspaceNavigationToolBar navigationToolBar) {
    WorkspaceNavigation workspaceNavigation = new WorkspaceNavigation(view, navigationToolBar);

    for (WorkspaceNavigationSection<?> topSection : topSections) {
      workspaceNavigation.addTopSection(topSection);
    }

    for (WorkspaceNavigationSection<?> bottomSection : bottomSections) {
      bottomSection.setVisible(false);
      workspaceNavigation.addBottomSection(bottomSection);
    }

    return workspaceNavigation;
  }

  /**
   * Style names for this presenter's view.
   */
  public interface Css extends CssResource {

    /**
     * Returns the height of the bottom section.
     */
    String bottomSectionsHeight();

    String root();

    String topSections();

    String bottomSections();

    String bottomSectionsClosed();

    String bottomSectionsAnimator();

    String bottomSectionsContent();

    String splitter();
  }

  /**
   * CSS and images used by this presenter.
   */
  interface Resources
      extends
      FileTreeSection.Resources,
      CollaborationSection.Resources,
      OutlineSection.Resources,
      WorkspaceNavigationToolBar.Resources,
      ResizeController.Resources {

    @Source({"collide/client/common/constants.css", "WorkspaceNavigation.css"})
    Css workspaceNavigationCss();
  }

  /**
   * View for this presenter that happens to be a simple overlay type.
   */
  public static class View extends CompositeView<Void> {

    @UiTemplate("WorkspaceNavigation.ui.xml")
    interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, View> {
    }

    private static MyBinder binder = GWT.create(MyBinder.class);

    @UiField(provided = true)
    final Css css;

    @UiField
    DivElement topSections;

    @UiField
    DivElement splitter;

    @UiField
    DivElement bottomSections;

    @UiField
    DivElement bottomSectionsAnimator;

    @UiField
    DivElement bottomSectionsContent;

    @UiField
    DivElement toolbarHolder;

    WorkspaceNavigationToolBar.View navigationToolBarView;

    protected View(Resources res) {
      this.css = res.workspaceNavigationCss();
      this.navigationToolBarView = new WorkspaceNavigationToolBar.View(res);

      setElement(Elements.asJsElement(binder.createAndBindUi(this)));
      setBottomSectionsVisible(false);
      Elements.asJsElement(toolbarHolder).appendChild(navigationToolBarView.getElement());

      /*
       * Add a resize controller. We set the default value programatically
       * because we use padding, which skews the height calculations in
       * ResizeController.
       */
      // TODO: Move this to the presenter.
      ResizeController splitterController =
          new ResizeController(res, Elements.asJsElement(splitter),
              new ResizeController.ElementInfo(Elements.asJsElement(bottomSections),
                  ResizeProperty.HEIGHT, css.bottomSectionsHeight()),
              new ResizeController.ElementInfo(Elements.asJsElement(bottomSectionsAnimator),
                  ResizeProperty.HEIGHT, css.bottomSectionsHeight()),
              new ResizeController.ElementInfo(Elements.asJsElement(bottomSectionsContent),
                  ResizeProperty.HEIGHT, css.bottomSectionsHeight()));
      splitterController.setNegativeDeltaH(true);
      splitterController.start();
    }

    public WorkspaceNavigationToolBar.View getNavigationToolBarView() {
      return navigationToolBarView;
    }

    private void setBottomSectionsVisible(boolean isVisible) {
      // Use a CSS class to set the height to 0px so we don't override the
      // height attribute set by the resize controller.
      CssUtils.setClassNameEnabled(Elements.asJsElement(bottomSections), css.bottomSectionsClosed(),
          !isVisible);
      CssUtils.setClassNameEnabled(Elements.asJsElement(bottomSectionsAnimator),
          css.bottomSectionsClosed(), !isVisible);
      CssUtils.setDisplayVisibility2(Elements.asJsElement(splitter), isVisible);
    }
  }

  /**
   * The bottom section that is currently visible.
   */
  private WorkspaceNavigationSection<?> shownBottomSection;

  /**
   * The bottom section that was previously visible, and may need to be hidden
   * when a new section is shown. We don't hide the old section while it is
   * animating out of view, so we need to keep track of it even if it isn't
   * logically selected.
   */
  private WorkspaceNavigationSection<?> previouslyShownBottomSection;

  private final WorkspaceNavigationToolBar navigationToolBar;

  WorkspaceNavigation(View view, WorkspaceNavigationToolBar navigationToolBar) {
    super(view);
    this.navigationToolBar = navigationToolBar;

    // Hide the bottom sections by default.
    showBottomSection(null);
  }

  /**
   * Takes in a {@link WorkspaceNavigationSection} and adds it to the top
   * sections.
   *
   * @param section section to add
   */
  void addTopSection(WorkspaceNavigationSection<?> section) {
    Elements.asJsElement(getView().topSections).appendChild(section.getView().getElement());
  }

  /**
   * Takes in a {@link WorkspaceNavigationSection} and adds it to the bottom
   * sections.
   *
   * @param section section to add
   */
  void addBottomSection(WorkspaceNavigationSection<?> section) {
    Elements.asJsElement(getView().bottomSectionsContent)
        .appendChild(section.getView().getElement());
  }

  /**
   * Hide currently shown navigation section and show the given one.
   *
   * @param section section to show, or {@code null} to hide shown section
   */
  void showBottomSection(WorkspaceNavigationSection<?> section) {
    if (section == shownBottomSection) {
      return;
    }

    /*
     * Hide the old section, but only if something else if going to be shown.
     * Otherwise, we want to animate the old section out of view.
     */
    if (previouslyShownBottomSection != null && section != null) {
      previouslyShownBottomSection.setVisible(false);
      previouslyShownBottomSection = null;
    }

    if (section != null) {
      section.setVisible(true);
      previouslyShownBottomSection = section;
    }

    getView().setBottomSectionsVisible(section != null);

    shownBottomSection = section;
  }

  /**
   * Shows the specified section if it is not visible, or hides the section if
   * it is visible.
   *
   * @return true if the section is now showing, false if not
   */
  boolean toggleBottomSection(WorkspaceNavigationSection<?> section) {
    if (section == shownBottomSection) {
      showBottomSection(null);
      return false;
    } else {
      showBottomSection(section);
      return true;
    }
  }
}
