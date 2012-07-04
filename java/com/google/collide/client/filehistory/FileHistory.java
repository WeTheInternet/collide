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

package com.google.collide.client.filehistory;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.FileSelectedPlace;
import com.google.collide.client.code.EditableContentArea.Content;
import com.google.collide.client.history.Place;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;


/**
 * The File History diff and bar view, which uses a Javascript timeline widget
 * to compare past revisions. Also allows for reverting back to previous
 * revisions.
 *
 */
public class FileHistory extends UiComponent<FileHistory.View> implements Content {

  /**
   * Static factory method for obtaining an instance of FileHistory.
   */
  public static FileHistory create(
      Place currentPlace, AppContext appContext, FileHistory.View view) {
    return new FileHistory(view, currentPlace, appContext);
  }

  public interface Css extends CssResource {
    String base();

    String diff();

    String timelineBar();

    String timelineWrapper();

    String timelineTitle();

    String filters();

    String filter();

    String currentFilter();

    String closeButton();

    String closeIcon();

    String button();

    String disabledButton();

    String title();

    String rightRevisionTitle();

    String leftRevisionTitle();

    String titleBar();
  }

  public interface Resources extends Timeline.Resources {
    @Source("close.png")
    ImageResource closeIcon();

    @Source("FileHistory.css")
    Css fileHistoryCss();
  }

  public static class View extends CompositeView<ViewEvents> {
    @UiTemplate("FileHistory.ui.xml")
    interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, View> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);

    @UiField
    DivElement diff;

    @UiField
    DivElement timelineBar;

    @UiField
    DivElement timelineWrapper;

    @UiField
    DivElement timelineTitle;

    Element leftRevisionTitle;

    Element rightRevisionTitle;

    Element titleBar;

    Element closeButton;

    Element closeIcon;

    Timeline.View timelineView;

    @UiField(provided = true)
    final Resources res;

    @UiField(provided = true)
    final Css css;

    View(Resources res) {
      this.res = res;
      this.css = res.fileHistoryCss();
      this.timelineView = new Timeline.View(res);

      setElement(Elements.asJsElement(binder.createAndBindUi(this)));

      Elements.asJsElement(timelineWrapper).appendChild(timelineView.getElement());
      createDom();

      attachEventHandlers();
    }

    protected void attachEventHandlers() {
      closeButton.setOnClick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          ViewEvents delegate = getDelegate();
          if (delegate == null) {
            return;
          }
          delegate.onCloseButtonClicked();
        }
      });

    }

    /**
     * Initialize diff revision titles on the toolbar (default to base version
     * and current version.
     */
    private void createDom() {
      closeButton = Elements.createDivElement(css.closeButton());
      closeIcon = Elements.createDivElement(css.closeIcon());

      closeButton.appendChild(closeIcon);

      titleBar = Elements.createDivElement(css.titleBar());
      leftRevisionTitle = Elements.createDivElement(css.leftRevisionTitle());
      rightRevisionTitle = Elements.createDivElement(css.rightRevisionTitle());

      leftRevisionTitle.addClassName(css.title());
      rightRevisionTitle.addClassName(css.title());

      titleBar.appendChild(leftRevisionTitle);
      titleBar.appendChild(rightRevisionTitle);

    }

  }

  /**
   * Events reported by the FileHistory's View.
   */
  private interface ViewEvents {
    void onCloseButtonClicked();
  }

  /**
   * The delegate implementation for handling events reported by the View.
   */
  private class ViewEventsImpl implements ViewEvents {

    @Override
    public void onCloseButtonClicked() {
      // Clear editor content
      api.clearDiffEditors();

      // unfortunately this must be hard coded since we can be either a child of
      // the file selected place or the workspace place.
      WorkspacePlace.PLACE.fireChildPlaceNavigation(
          FileSelectedPlace.PLACE.createNavigationEvent(path));
    }
  }

  private final AppContext appContext;
  private final Place currentPlace;
  private PathUtil path;
  private FileHistoryApi api;

  @VisibleForTesting
  protected FileHistory(View view, Place currentPlace, AppContext appContext) {
    super(view);
    this.currentPlace = currentPlace;
    this.appContext = appContext;
    this.path = PathUtil.WORKSPACE_ROOT;

    view.setDelegate(new ViewEventsImpl());
  }

  public void setPath(PathUtil path) {
    this.path = path;
  }

  public void setApi(FileHistoryApi api) {
    this.api = api;
  }

  @Override
  public Element getContentElement() {
    return getView().getElement();
  }

  @Override
  public void onContentDisplayed() {
  }

  /* Setup/teardown for the FileHistory place */
  public void setup(Element contentHeader) {
    contentHeader.appendChild(getView().closeButton);
    contentHeader.appendChild(getView().titleBar);

    changeLeftRevisionTitle("Workspace Branched");
    changeRightRevisionTitle("Current Version");
  }

  public void teardown() {
    getView().closeButton.removeFromParent();
    getView().titleBar.removeFromParent();
  }

  /* Change revision titles */

  public void changeLeftRevisionTitle(String title) {
    getView().leftRevisionTitle.setTextContent(title);
  }

  public void changeRightRevisionTitle(String title) {
    getView().rightRevisionTitle.setTextContent(title);
  }
}
