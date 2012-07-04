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

package com.google.collide.client.ui.popup;

import com.google.collide.client.ui.menu.AutoHideComponent;
import com.google.collide.client.ui.menu.AutoHideView;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.util.Elements;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.html.Element;

import javax.annotation.Nullable;

/**
 * Represents a floating popup, that can be attached to any element.
 *
 */
public class Popup extends AutoHideComponent<Popup.View, AutoHideComponent.AutoHideModel> {

  public interface Css extends CssResource {
    String root();
    String contentHolder();
  }

  public interface Resources extends ClientBundle {
    @Source({"com/google/collide/client/common/constants.css", "Popup.css"})
    Css popupCss();
  }

  /**
   * The View for the Popup component.
   */
  public static class View extends AutoHideView<Void> {
    private final Css css;
    private final Element contentHolder;

    View(Resources resources) {
      this.css = resources.popupCss();

      contentHolder = Elements.createDivElement(css.contentHolder());

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(contentHolder);
      setElement(rootElement);
    }

    void setContentElement(@Nullable Element contentElement) {
      contentHolder.setInnerHTML("");
      if (contentElement != null) {
        contentHolder.appendChild(contentElement);
      }
    }
  }

  public static Popup create(Resources resources) {
    View view = new View(resources);
    return new Popup(view);
  }

  private PositionController positionController;

  private Popup(View view) {
    super(view, new AutoHideModel());
  }

  @Override
  public void show() {
    Preconditions.checkNotNull(
        positionController, "You cannot show this popup without using a position controller");
    positionController.updateElementPosition();

    cancelPendingHide();
    super.show();
  }

  /**
   * Shows the popup anchored to a given element.
   */
  public void show(Positioner positioner) {
    positionController = new PositionController(positioner, getView().getElement());
    show();
  }

  /**
   * Sets the popup's content element.
   *
   * @param contentElement the DOM element to show in the popup, or {@code null}
   *        to clean up the popup's DOM
   */
  public void setContentElement(@Nullable Element contentElement) {
    getView().setContentElement(contentElement);
  }

  public void destroy() {
    forceHide();
    setContentElement(null);
    positionController = null;
  }
}
