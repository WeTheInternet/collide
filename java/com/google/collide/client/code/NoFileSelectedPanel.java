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

import collide.client.util.Elements;

import com.google.collide.client.history.Place;
import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.util.PathUtil;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;

/**
 * The screen you first see when opening a workspace that has no file selected.
 *
 */
public class NoFileSelectedPanel extends UiComponent<NoFileSelectedPanel.View>
 implements FileContent, PanelContent.HiddenContent {

  private static final String REGULAR_MESSAGE = "Choose a file to begin editing.";

  public static NoFileSelectedPanel create(Place place, Resources res) {
    View view = new View(res);
    view.setMessage(REGULAR_MESSAGE);
    NoFileSelectedPanel panel = new NoFileSelectedPanel(view);
    return panel;
  }

  /**
   * Images and CSS.
   */
  public interface Resources extends ClientBundle {
    @Source({"collide/client/common/constants.css", "NoFileSelectedPanel.css"})

    Css noFileSelectedPanelCss();

    @Source("editor.png")
    ImageResource editorLogo();
  }

  /**
   * Styles names.
   */
  public interface Css extends CssResource {
    String base();

    String bigger();

    String center();

    String logo();

    String text();
  }

  /**
   * Events sourced by this View.
   */
  public interface ViewEvents {

    // TODO: Add desktop Drag n' drop event.
    void onClicked();
  }

  public static class View extends CompositeView<ViewEvents> {
    @UiTemplate("NoFileSelectedPanel.ui.xml")
    interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, View> {
    }

    private static MyBinder binder = GWT.create(MyBinder.class);

    final Resources res;

    @UiField(provided = true)
    final Css css;

    @UiField
    ParagraphElement message;

    private EventRemover remover;

    public View(Resources res) {
      this.res = res;
      this.css = res.noFileSelectedPanelCss();
      setElement(Elements.asJsElement(binder.createAndBindUi(this)));
      handleEvents();
    }

    public void detach() {
      // Remove ourselves, we have served our purpose.
      getElement().removeFromParent();

      // Unhook the eventlistener just case.
      if (remover != null) {
        remover.remove();
      }
    }

    /*
     * Set the message (the last line) of the panel.
     */
    public void setMessage(String msg) {
      message.setInnerText(msg);
    }

    private void handleEvents() {
      remover = getElement().addEventListener(Event.CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() != null) {
            getDelegate().onClicked();
          }
        }
      }, false);
    }

  }

  @Override
  public PathUtil filePath() {
    return null;
  }
  
  public NoFileSelectedPanel(View view) {
    super(view);

    getView().setMessage(REGULAR_MESSAGE);
  }

  public void detach() {
    getView().detach();
  }

  @Override
  public Element getContentElement() {
    return getView().getElement();
  }

  @Override
  public void onContentDisplayed() {}

  @Override
  public void onContentDestroyed() {

  }
}
