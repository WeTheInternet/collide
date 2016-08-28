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

package com.google.collide.client.workspace;

import collide.client.common.CommonResources;
import collide.client.util.Elements;

import com.google.collide.client.ui.popup.CenterPanel;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.events.Event;
import elemental.events.EventListener;

/**
 * Instructional popup that alert users that window with started application
 * has been blocked by browser.
 */
public class PopupBlockedInstructionalPopup
    extends UiComponent<PopupBlockedInstructionalPopup.View> {

  /**
   * Static factory method for obtaining an instance of the
   * {@link PopupBlockedInstructionalPopup}.
   */
  public static PopupBlockedInstructionalPopup create(Resources res) {
    View view = new View(res);
    CenterPanel centerPanel = CenterPanel.create(res, view.getElement());
    centerPanel.setHideOnEscapeEnabled(true);
    return new PopupBlockedInstructionalPopup(view, centerPanel);
  }

  /**
   * CSS resources interface.
   */
  public interface Css extends CssResource {
    String root();

    String popupBlockedIcon();
  }

  /**
   * BaseResources interface.
   */
  public interface Resources extends CommonResources.BaseResources, CenterPanel.Resources {
    @Source("PopupBlockedInstructionalPopup.css")
    Css popupBlockedInstructionalPopupCss();

    @Source("blocked_popups.png")
    ImageResource popupBlockedIcon();
  }

  interface ViewEvents {
    void onDoneClicked();
  }

  static class View extends CompositeView<ViewEvents> {
    @UiTemplate("PopupBlockedInstructionalPopup.ui.xml")
    interface MyBinder extends UiBinder<Element, View> {
    }

    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    final Resources res;

    @UiField(provided = true)
    final CommonResources.BaseCss baseCss;

    @UiField(provided = true)
    final Css css;

    @UiField
    AnchorElement doneButton;

    View(Resources res) {
      this.res = res;
      this.baseCss = res.baseCss();
      this.css = res.popupBlockedInstructionalPopupCss();
      setElement(Elements.asJsElement(uiBinder.createAndBindUi(this)));
      attachEventListeners();
    }

    private void attachEventListeners() {
      Elements.asJsElement(doneButton).setOnclick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() == null) {
            return;
          }

          getDelegate().onDoneClicked();
        }
      });
    }
  }

  class ViewEventsImpl implements ViewEvents {
    @Override
    public void onDoneClicked() {
      centerPanel.hide();
    }
  }

  private final CenterPanel centerPanel;

  PopupBlockedInstructionalPopup(View view, CenterPanel centerPanel) {
    super(view);
    this.centerPanel = centerPanel;
    view.setDelegate(new ViewEventsImpl());
  }

  public void show() {
    centerPanel.show();
  }
}
