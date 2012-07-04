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
import com.google.collide.client.filehistory.FileHistoryPlace;
import com.google.collide.client.history.Place;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Position;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.menu.PositionController.PositionerBuilder;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;

/**
 * Editor toolbar for the workspace (contains buttons for file history,
 * annotate, and debugging)
 *
 *
 */
public class EditorToolBar extends UiComponent<EditorToolBar.View> {

  /**
   * Creates the default version of the toolbar to be used in the editor shell.
   */
  public static EditorToolBar create(
      View view, Place currentPlace, final AppContext appContext, final EditorBundle editorBundle) {
    return new EditorToolBar(view, currentPlace, appContext, editorBundle);
  }

  /**
   * Style names associated with elements in the toolbar.
   */
  public interface Css extends CssResource {
    String toolButtons();

    String button();

    String historyButton();

    String historyIcon();

    String debugButton();

    String debugIcon();

    String hspace();

    String hspaceIcon();
  }

  /**
   * Images and CssResources consumed by the EditorToolBar.
   */
  public interface Resources extends ClientBundle, Tooltip.Resources {
    @Source("history_icon.png")
    ImageResource historyIcon();

    @Source("debug_icon.png")
    ImageResource debugIcon();

    @Source("hspace.png")
    ImageResource hspaceIcon();

    @Source("EditorToolBar.css")
    Css editorToolBarCss();
  }

  /**
   * The View for the EditorToolBar.
   */
  public static class View extends CompositeView<ViewEvents> {
    @UiTemplate("EditorToolBar.ui.xml")
    interface MyBinder extends UiBinder<DivElement, View> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);

    @UiField
    DivElement toolButtons;

    @UiField
    DivElement historyButton;

    @UiField
    DivElement historyIcon;
    
    @UiField
    DivElement debugButton;

    @UiField
    DivElement debugIcon;

    @UiField(provided = true)
    final Resources res;

    public View(EditorToolBar.Resources res) {
      this.res = res;
      setElement(Elements.asJsElement(binder.createAndBindUi(this)));
      attachHandlers();

      // Make these tooltips right aligned since they're so close to the edge of the screen.
      PositionerBuilder positioner = new Tooltip.TooltipPositionerBuilder().setHorizontalAlign(
          HorizontalAlign.RIGHT).setPosition(Position.OVERLAP);
      Positioner historyTooltipPositioner =
          positioner.buildAnchorPositioner(Elements.asJsElement(historyIcon));
      new Tooltip.Builder(
          res, Elements.asJsElement(historyIcon), historyTooltipPositioner).setTooltipText(
          "Explore this file's changes over time.").build().setTitle("History");

      Positioner debugTooltipPositioner =
          positioner.buildAnchorPositioner(Elements.asJsElement(debugIcon));
      new Tooltip.Builder(
          res, Elements.asJsElement(debugIcon), debugTooltipPositioner).setTooltipText(
          "Opens the debug panel where you can set breakpoints and watch expressions.")
          .build().setTitle("Debugging Controls");
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
          if (historyButton.isOrHasChild(target)) {
            delegate.onHistoryButtonClicked();
          } else if (debugButton.isOrHasChild(target)) {
            delegate.onDebugButtonClicked();
          }
        }
      });
    }

    /**
     * Hide or display the history button.
     */
    public void setHistoryButtonVisible(boolean visible) {
      CssUtils.setDisplayVisibility2(
          Elements.asJsElement(historyButton), visible, false, "inline-block");
    }
  }

  /**
   * Events reported by the EditorToolBar's View.
   */
  private interface ViewEvents {
    void onDebugButtonClicked();
    void onHistoryButtonClicked();
  }

  /**
   * The delegate implementation for handling events reported by the View.
   */
  private class ViewEventsImpl implements ViewEvents {

    @Override
    public void onHistoryButtonClicked() {
      if (currentPath != null && pathRootId != null) {
        currentPlace.fireChildPlaceNavigation(
            FileHistoryPlace.PLACE.createNavigationEvent(currentPath, pathRootId));
      }
    }

    @Override
    public void onDebugButtonClicked() {
      /*
       * TODO: Make the RightSidebarToggleEvent live on
       * CodePerspective's place scope
       */
      WorkspacePlace.PLACE.fireEvent(new RightSidebarToggleEvent());
    }
  }

  private final AppContext appContext;
  private final EditorBundle editorBundle;
  private final Place currentPlace;
  private PathUtil currentPath;
  /**
   * currentPath is correct with respect to pathRootId.
   */
  private String pathRootId;

  EditorToolBar(View view, Place currentPlace, AppContext appContext, EditorBundle editorBundle) {
    super(view);
    this.currentPlace = currentPlace;
    this.appContext = appContext;
    this.editorBundle = editorBundle;
    view.setDelegate(new ViewEventsImpl());
  }

  public void setCurrentPath(PathUtil currentPath, String pathRootId) {
    this.currentPath = Preconditions.checkNotNull(currentPath);
    this.pathRootId = Preconditions.checkNotNull(pathRootId);
  }

  /* Methods for toggling toolbar visibility */

  public void show() {
    Element toolBar = Elements.asJsElement(getView().toolButtons);
    CssUtils.setDisplayVisibility(toolBar, true);
  }

  public void hide() {
    Element toolBar = Elements.asJsElement(getView().toolButtons);
    CssUtils.setDisplayVisibility(toolBar, false);
  }
}
