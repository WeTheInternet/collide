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

package com.google.collide.client.search.awesomebox.host;

import com.google.collide.client.search.awesomebox.host.AwesomeBoxComponent.HiddenBehavior;
import com.google.collide.client.search.awesomebox.host.AwesomeBoxComponent.HideMode;
import com.google.collide.client.search.awesomebox.host.AwesomeBoxComponent.ShowReason;
import com.google.collide.client.util.Elements;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;
import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;
import elemental.html.DivElement;
import elemental.html.Element;

/**
 * The host control of the {@link AwesomeBoxComponent}s and related components.
 * It performs very little other than management of {@link AwesomeBoxComponent}s
 * and focus/cancel actions.
 *
 */
public class AwesomeBoxComponentHost extends UiComponent<AwesomeBoxComponentHost.View> {

  public interface Css extends CssResource {
    String container();

    String base();
  }

  /**
   * A small class which wraps the currently visible
   * {@link AwesomeBoxComponent}.
   */
  public class HostedComponent implements ComponentHost {
    public final AwesomeBoxComponent component;

    public HostedComponent(AwesomeBoxComponent component) {
      this.component = component;
    }

    @Override
    public void requestHide() {
      if (current != this) {
        // This is stale, the component is already hidden.
        return;
      }

      hideImpl(AwesomeBoxComponentHiddenListener.Reason.OTHER);
    }
  }

  /**
   * Allows an object that is not a section to listen in when the AwesomeBox is
   * hiding/showing.
   */
  public interface AwesomeBoxComponentHiddenListener {
    public enum Reason {
      /**
       * An event occurred which canceled the user's interaction with the
       * AwesomeBox such as pressing the ESC button.
       */
      CANCEL_EVENT,
      /**
       * An external click occurred triggering an autohide.
       */
      EXTERNAL_CLICK,
      /**
       * The component was hidden programatically, or by the component.
       */
      OTHER
    }

    public void onHidden(Reason reason);
  }

  public interface ViewEvents {
    public void onExternalClick();

    public void onClick();

    public void onEscapePressed();
  }

  public static class View extends CompositeView<ViewEvents> {
    private final EventListener bodyListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (getDelegate() != null && !getElement().contains((Node) evt.getTarget())) {
          getDelegate().onExternalClick();
        }
      }
    };

    private final DivElement baseElement;
    private EventRemover bodyRemover;

    public View(Element container, Css css) {
      super(container);

      container.addClassName(css.container());
      baseElement = Elements.createDivElement(css.base());
      baseElement.setTextContent("Actions");
      container.appendChild(baseElement);

      attachEvents();
    }

    void attachEvents() {
      baseElement.addEventListener(Event.CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() != null) {
            getDelegate().onClick();
          }
        }
      }, false);

      getElement().addEventListener(Event.KEYUP, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          KeyboardEvent event = (KeyboardEvent) evt;
          if (event.getKeyCode() == KeyCode.ESC && getDelegate() != null) {
            getDelegate().onEscapePressed();
          }
        }
      }, false);
    }

    public void setBaseActive(boolean active) {
      Preconditions.checkState(!isBaseActive() == active, "Invalid base element state!");

      if (!active) {
        baseElement.removeFromParent();
      } else {
        getElement().appendChild(baseElement);
      }
    }

    public boolean isBaseActive() {
      return baseElement.getParentElement() != null;
    }

    public void attachComponentElement(Element component) {
      Preconditions.checkState(!isBaseActive(), "Base cannot be attached");

      getElement().appendChild(component);
    }

    public void setBodyListenerAttached(boolean shouldAttach) {
      boolean isListenerAttached = bodyRemover != null;
      Preconditions.checkState(
          isListenerAttached != shouldAttach, "Invalid listener attachment state");
      if (shouldAttach) {
        bodyRemover = Elements.getBody().addEventListener(Event.MOUSEDOWN, bodyListener, false);
      } else {
        bodyRemover.remove();
        bodyRemover = null;
      }
    }
  }

  public class ViewEventsImpl implements ViewEvents {
    @Override
    public void onClick() {
      // only do a show if we're not showing anything already
      if (getView().isBaseActive()) {
        showImpl(ShowReason.CLICK);
      }
    }

    @Override
    public void onExternalClick() {
      if (model.getActiveComponent().getHideMode() == HideMode.AUTOHIDE) {
        hideImpl(AwesomeBoxComponentHiddenListener.Reason.EXTERNAL_CLICK);
      }
    }

    @Override
    public void onEscapePressed() {
      hideImpl(AwesomeBoxComponentHiddenListener.Reason.CANCEL_EVENT);
    }
  }

  private final HostedComponent NONE_SHOWING = new HostedComponent(null);

  private final ListenerManager<AwesomeBoxComponentHiddenListener> componentHiddenListener =
      ListenerManager.create();
  private final AwesomeBoxComponentHostModel model;
  private HostedComponent current = NONE_SHOWING;

  public AwesomeBoxComponentHost(View view, AwesomeBoxComponentHostModel model) {
    super(view);
    this.model = model;
    view.setDelegate(new ViewEventsImpl());
  }

  /**
   * Returns a {@link ListenerRegistrar} which can be used to listen for the
   * active component being hidden.
   */
  public ListenerRegistrar<AwesomeBoxComponentHiddenListener> getComponentHiddenListener() {
    return componentHiddenListener;
  }

  /**
   * Hides the currently active component.
   */
  public void hide() {
    if (isComponentActive()) {
      hideImpl(AwesomeBoxComponentHiddenListener.Reason.OTHER);
    }
  }

  /**
   * Displays the current component set in the
   * {@link AwesomeBoxComponentHostModel}. Any currently displayed component
   * will be removed from the DOM.
   */
  public void show() {
    showImpl(ShowReason.OTHER);
  }

  private void showImpl(ShowReason reason) {
    if (current.component == model.getActiveComponent()) {
      current.component.focus();
      return;
    } else if (isComponentActive()) {
      hide();
    }

    current = new HostedComponent(model.getActiveComponent());
    Preconditions.checkState(current.component != null, "There is no active component to host");
    getView().setBaseActive(false);
    getView().setBodyListenerAttached(true);
    getView().attachComponentElement(current.component.getElement());
    current.component.onShow(current, reason);
    current.component.focus();
  }

  /**
   * @return true if a component is currently active.
   */
  public boolean isComponentActive() {
    return current != NONE_SHOWING;
  }

  private void hideImpl(AwesomeBoxComponentHiddenListener.Reason reason) {
    Preconditions.checkNotNull(
        current != NONE_SHOWING, "There must be an active component to hide.");

    // Extract the current component and mark us as none showing
    AwesomeBoxComponent component = current.component;
    current = NONE_SHOWING;

    // remove the current component and reattach our base
    /*
     * NOTE: Removing parent seems to cause any queued DOM events for this
     * element to freak out, make sure current is already NONE_SHOWING to block
     * other hide attempts. If you don't you'll probably see things like
     * NOT_FOUND_ERR. This is especially true of the blur event used by the
     * AwesomeBox to hide.
     */
    component.getElement().removeFromParent();
    getView().setBaseActive(true);
    getView().setBodyListenerAttached(false);

    // Hide component, potentially revert, then dispatch the hidden listener
    component.onHide();
    maybeRevertToDefaultComponent(component);
    dispatchHiddenListener(reason);
  }

  private void dispatchHiddenListener(final AwesomeBoxComponentHiddenListener.Reason reason) {
    componentHiddenListener.dispatch(new Dispatcher<AwesomeBoxComponentHiddenListener>() {
      @Override
      public void dispatch(AwesomeBoxComponentHiddenListener listener) {
        listener.onHidden(reason);
      }
    });
  }

  private void maybeRevertToDefaultComponent(AwesomeBoxComponent component) {
    boolean isComponentTheModelsActiveComponent = component == model.getActiveComponent();
    boolean isHiddenBehaviorRevert =
        component.getHiddenBehavior() == HiddenBehavior.REVERT_TO_DEFAULT;

    if (isComponentTheModelsActiveComponent && isHiddenBehaviorRevert) {
      model.revertToDefaultComponent();
    }
  }
}
