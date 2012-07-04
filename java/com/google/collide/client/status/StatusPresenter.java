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

package com.google.collide.client.status;

import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.testing.DebugAttributeSetter;
import com.google.collide.client.testing.DebugId;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.client.JsoArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.css.CSSStyleDeclaration;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.css.CSSStyleDeclaration.Visibility;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.DivElement;
import elemental.html.Element;
import elemental.html.PreElement;
import elemental.html.SpanElement;

/**
 * The StatusPresenter handles status events and renders them into the UI. This
 * presenter is meant to be injected into some already constructed DOM space
 * since it does not define its own dimensions.
 *
 */
public class StatusPresenter extends UiComponent<StatusPresenter.View>
    implements StatusHandler {

  public interface Css extends CssResource {
    String actionsContainer();

    String action();

    String expandedFatal();

    String expandedRegular();

    String fatal();

    String fatalImage();
    
    String inline();

    String longText();

    String more();

    String statusArea();

    String statusConfirmation();

    String statusDismiss();

    String statusError();

    String statusLoading();

    String statusText();
  }

  public interface Resources extends ClientBundle {
    @Source("close.png")
    ImageResource close();

    @Source("fatal_border.png")
    ImageResource fatalBorder();

    @Source({"com/google/collide/client/common/constants.css", "StatusPresenter.css"})
    Css statusPresenterCss();
  }

  public static class View extends CompositeView<StatusPresenter.ViewEvents> {
    private static final int PADDING_WIDTH = 60;

    private DivElement actionsContainer;
    private final Css css;
    private DivElement fatalImage;
    private PreElement longText;
    private SpanElement more;
    private DivElement statusDismiss;
    private StatusMessage statusMessage;
    private DivElement statusText;

    public View(Resources resources) {
      this.css = resources.statusPresenterCss();
      setElement(createDom());
    }

    public void clear() {
      getElement().getStyle().setVisibility(Visibility.HIDDEN);
      statusDismiss.getStyle().setVisibility(Visibility.HIDDEN);
      longText.getStyle().setDisplay(Display.NONE);
    }

    public void renderStatus(StatusMessage message) {
      this.statusMessage = message;
      getElement().getStyle().setVisibility(Visibility.VISIBLE);
      longText.getStyle().setDisplay(CSSStyleDeclaration.Display.NONE);

      final JsoArray<StatusAction> statusActions = message.getActions();      
      actionsContainer.getStyle().setDisplay(
          statusActions.size() > 0 ? Display.BLOCK : Display.NONE);
      actionsContainer.setInnerHTML("");
      for (int i = 0, n = statusActions.size(); i < n; i++) {
        final StatusAction statusAction = statusActions.get(i);
        SpanElement action = Elements.createSpanElement(css.action());
        statusAction.renderAction(action);        
        action.setOnClick(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            statusAction.onAction();
          }
        });

        actionsContainer.appendChild(action);
      }

      statusText.setTextContent(message.getText());

      // Render a countdown if the message is going to expire.
      if (message.getTimeToExpiry() > 0) {
        final StatusMessage msg = message;
        Scheduler.get().scheduleFixedPeriod(new RepeatingCommand() {
          @Override
          public boolean execute() {
            if (msg == StatusPresenter.View.this.statusMessage) {
              int seconds = msg.getTimeToExpiry() / 1000;
              statusText.setTextContent(msg.getText() + " ..." + seconds);
              return seconds > 0;
            } else {
              return false;
            }
          }
        }, 1000);
      }

      if (!message.getLongText().isEmpty()) {
        longText.setTextContent(message.getLongText());
        more.getStyle().setDisplay(Display.INLINE_BLOCK);
      } else {
        more.getStyle().setDisplay(Display.NONE);
      }

      if (message.isDismissable()) {
        statusDismiss.getStyle().setVisibility(Visibility.VISIBLE);
      } else {
        statusDismiss.getStyle().setVisibility(Visibility.HIDDEN);
      }

      if (message.getType() != MessageType.FATAL) {
        // Size and center the message
        dynamicallyPositionMessage();
      } else {
        // Fatal messages are 100% width.
        getElement().getStyle().setWidth(100, CSSStyleDeclaration.Unit.PCT);
        getElement().getStyle().setMarginLeft(0, CSSStyleDeclaration.Unit.PX);
      }

      // Render the particular message type's style
      DebugAttributeSetter debugSetter = new DebugAttributeSetter();
      switch (message.getType()) {
        case LOADING:
          debugSetter.add("status", "loading");
          getElement().setClassName(css.statusLoading());
          break;
        case CONFIRMATION:
          debugSetter.add("status", "confirmation");
          getElement().setClassName(css.statusConfirmation());
          break;
        case ERROR:
          debugSetter.add("status", "error");
          getElement().setClassName(css.statusError());
          break;
        case FATAL:
          debugSetter.add("status", "fatal");
          getElement().setClassName(css.fatal());
          getDelegate().onStatusExpanded();
          break;
        default:
          debugSetter.add("status", "unknown");
          Log.error(getClass(), "Got a status message of unknown type " + message.getType());
      }
      debugSetter.on(getElement());
    }

    private Element createDom() {
      DivElement root = Elements.createDivElement(css.statusArea());
      statusText = Elements.createDivElement(css.statusText());
      statusDismiss = Elements.createDivElement(css.statusDismiss());
      statusDismiss.setOnClick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          getDelegate().onStatusDismissed();
        }
      });
      new DebugAttributeSetter().setId(DebugId.STATUS_PRESENTER).on(root);

      longText = Elements.createPreElement();
      longText.setClassName(css.longText());
      longText.getStyle().setDisplay(CSSStyleDeclaration.Display.NONE);

      more = Elements.createSpanElement();
      more.setClassName(css.more());
      more.setTextContent("show more details...");
      more.setOnClick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          getDelegate().onStatusExpanded();
        }
      });

      actionsContainer = Elements.createDivElement();
      actionsContainer.setClassName(css.actionsContainer());

      fatalImage = Elements.createDivElement(css.fatalImage());

      root.appendChild(fatalImage);
      root.appendChild(statusText);
      root.appendChild(more);
      root.appendChild(actionsContainer);
      root.appendChild(statusDismiss);
      root.appendChild(longText);

      return root;
    }

    /**
     * Size the message dynamically to scale with the text but not taking up
     * more than 50% of the screen width.
     */
    private void dynamicallyPositionMessage() {
      int messageWidth =
          statusText.getScrollWidth() + actionsContainer.getScrollWidth() + more.getScrollWidth()
              + statusDismiss.getScrollWidth() + PADDING_WIDTH;
      int maxWidth = Elements.getBody().getClientWidth() / 2;
      int width = CssUtils.isVisible(longText) ? maxWidth : Math.min(messageWidth, maxWidth);
      getElement().getStyle().setWidth(width, CSSStyleDeclaration.Unit.PX);
      getElement().getStyle().setMarginLeft(width / -2, CSSStyleDeclaration.Unit.PX);
    }
  }

  public interface ViewEvents {
    void onStatusDismissed();

    void onStatusExpanded();
  }

  public static StatusPresenter create(Resources resources) {
    View view = new View(resources);
    return new StatusPresenter(view);
  }

  private StatusMessage statusMessage;

  protected StatusPresenter(View view) {
    super(view);
    handleViewEvents();
  }

  @Override
  public void clear() {
    getView().clear();
    statusMessage = null;
  }

  @Override
  public void onStatusMessage(StatusMessage msg) {
    statusMessage = msg;
    getView().renderStatus(msg);
  }

  private void handleViewEvents() {
    getView().setDelegate(new ViewEvents() {
      @Override
      public void onStatusDismissed() {
        if (statusMessage != null) {
          statusMessage.cancel();
        }
      }

      @Override
      public void onStatusExpanded() {
        getView().more.getStyle().setDisplay(CSSStyleDeclaration.Display.NONE);
        getView().longText.getStyle().setDisplay(CSSStyleDeclaration.Display.BLOCK);
        if (statusMessage.getType() != MessageType.FATAL) {
          getView().getElement().addClassName(getView().css.expandedRegular());
          getView().dynamicallyPositionMessage();
        } else {
          getView().getElement().addClassName(getView().css.expandedFatal());
        }
      }
    });
  }
}
