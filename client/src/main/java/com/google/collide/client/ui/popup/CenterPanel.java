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

import javax.annotation.Nullable;

import collide.client.common.CommonResources.BaseCss;
import collide.client.util.Elements;

import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Timer;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.KeyboardEvent.KeyCode;
import elemental.js.dom.JsElement;
import elemental.js.events.JsKeyboardEvent;

/**
 * A popup that automatically centers its content, even if the dimensions of the content change. The
 * centering is done in CSS, so performance is very good. A semi-transparent "glass" panel appears
 * behind the popup. The glass is not optional due to the way {@link CenterPanel} is implemented.
 *
 * <p>
 * {@link CenterPanel} animates into and out of view using the shrink in/expand out animation.
 * </p>
 */
public class CenterPanel extends UiComponent<CenterPanel.View> {

  /**
   * Constructs a new {@link CenterPanel} that contains the specified content
   * element.
   * 
   * @param resources the resources to apply to the popup
   * @param content the content to display in the center of the popup
   * @return a new {@link CenterPanel} instance
   */
  public static CenterPanel create(Resources resources, elemental.dom.Element content) {
    View view = new View(resources, content);
    return new CenterPanel(view);
  }

  /**
   * The resources used by this UI component.
   */
  public interface Resources extends ClientBundle {
    @Source({"collide/client/common/constants.css", "CenterPanel.css"})
    Css centerPanelCss();
  }

  /**
   * The BaseCss Style names used by this panel.
   */
  public interface Css extends CssResource {
    /**
     * Returns duration of the popup animation in milliseconds.
     */
    int animationDuration();

    String content();

    String contentVisible();

    String glass();

    String glassVisible();

    String popup();

    String positioner();
  }

  /**
   * The events sources by the View.
   */
  private interface ViewEvents {
    void onEscapeKey();
  }

  /**
   * The view that renders the {@link CenterPanel}. The View consists of a glass
   * panel that fades out the background, and a DOM structure that positions the
   * contents in the exact center of the screen.
   */
  public static class View extends CompositeView<ViewEvents> {

    @UiTemplate("CenterPanel.ui.xml")
    interface MyBinder extends UiBinder<Element, View> {
    }

    private static MyBinder uiBinder = GWT.create(MyBinder.class);

    final Resources res;

    @UiField(provided = true)
    final Css css;

    @UiField
    DivElement contentContainer;

    @UiField
    DivElement glass;

    @UiField
    DivElement popup;

    View(Resources res, elemental.dom.Element content) {
      this.res = res;
      this.css = res.centerPanelCss();
      setElement(Elements.asJsElement(uiBinder.createAndBindUi(this)));
      Elements.asJsElement(contentContainer).appendChild(content);
      handleEvents();
    }

    /**
     * Returns the duration of the popup animation in milliseconds. The return
     * value should equal the value of {@link BaseCss#animationDuration()}.
     */
    protected int getAnimationDuration() {
      return css.animationDuration();
    }

    /**
     * Updates the View to reflect the showing state of the popup.
     * 
     * @param showing true if showing, false if not.
     */
    protected void setShowing(boolean showing) {
      if (showing) {
        glass.addClassName(css.glassVisible());
        contentContainer.addClassName(css.contentVisible());
      } else {
        glass.removeClassName(css.glassVisible());
        contentContainer.removeClassName(css.contentVisible());
      }
    }

    private void handleEvents() {
      getElement().addEventListener(Event.KEYDOWN, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          JsKeyboardEvent keyEvt = (JsKeyboardEvent) evt;
          int keyCode = keyEvt.getKeyCode();
          if (KeyCode.ESC == keyCode) {
            if (getDelegate() != null) {
              getDelegate().onEscapeKey();
            }
          }
        }
      }, true);
    }
  }

  private boolean hideOnEscapeEnabled = false;
  private boolean isShowing;

  CenterPanel(View view) {
    super(view);
    handleViewEvents();
  }

  /**
   * Hides the {@link CenterPanel} popup. The popup will animate out of view.
   */
  public void hide() {
    if (!isShowing) {
      return;
    }
    isShowing = false;

    // Animate the popup out of existance.
    getView().setShowing(false);

    // Remove the popup when the animation completes.
    new Timer() {
      @Override
      public void run() {
        // The popup may have been shown before this timer executes.
        if (!isShowing) {
          Elements.asJsElement(getView().popup).removeFromParent();
        }
      }
    }.schedule(getView().getAnimationDuration());
  }

  /**
   * Checks if the {@link CenterPanel} is showing or animating into view.
   * 
   * @return true if showing, false if hidden
   */
  public boolean isShowing() {
    return isShowing;
  }

  /**
   * Sets whether or not the popup should hide when escape is pressed. The
   * default behavior is to ignore the escape key.
   * 
   * @param isEnabled true to close on escape, false not to
   */
  // TODO: This only works if the popup has focus. We need to capture events.
  // TODO: Consider making escaping the default.
  public void setHideOnEscapeEnabled(boolean isEnabled) {
    this.hideOnEscapeEnabled = isEnabled;
  }

  /**
   * See {@link #show(InputElement)}.
   */
  public void show() {
    show(null);
  }

  /**
   * Displays the {@link CenterPanel} popup. The popup will animate into view.
   *
   * @param selectAndFocusElement an {@link InputElement} to select and focus on when the panel is
   *        shown. If null, no element will be given focus
   */
  public void show(@Nullable final InputElement selectAndFocusElement) {
    if (isShowing) {
      return;
    }
    isShowing = true;

    // Attach the popup to the body.
    final JsElement popup = getView().popup.cast();
    if (popup.getParentElement() == null) {
      // Hide the popup so it can enter its initial state without flickering.
      popup.getStyle().setVisibility("hidden");
      Elements.getBody().appendChild(popup);
    }

    // Start the animation after the element is attached.
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        // The popup may have been hidden before this timer executes.
        if (isShowing) {
          popup.getStyle().removeProperty("visibility");
          getView().setShowing(true);
          if (selectAndFocusElement != null) {
            selectAndFocusElement.select();
            selectAndFocusElement.focus();
          }
        }
      }
    });
  }

  private void handleViewEvents() {
    getView().setDelegate(new ViewEvents() {
      @Override
      public void onEscapeKey() {
        if (hideOnEscapeEnabled) {
          hide();
        }
      }
    });
  }
}
