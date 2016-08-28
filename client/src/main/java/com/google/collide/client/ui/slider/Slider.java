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

package com.google.collide.client.ui.slider;

import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.testing.DebugAttributeSetter;
import com.google.collide.client.util.AnimationUtils;
import com.google.collide.client.util.ResizeController;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;

/**
 * Slider UI component.
 */
public class Slider extends UiComponent<Slider.View> {

  private static final String SLIDER_MODE = "slidermode";

  /**
   * BaseCss selectors applied to DOM elements in the slider.
   */
  public interface Css extends CssResource {
    String sliderRoot();
    String sliderLeft();
    String sliderSplitter();
    String sliderRight();
    String sliderFlex();
    String paddingForBorderRadius();
  }

  /**
   * BaseResources used by the Slider.
   *
   * In order to theme the Slider, you extend this interface and override
   * {@link Slider.Resources#sliderCss()}.
   */
  public interface Resources extends ClientBundle, ResizeController.Resources {
    // Default Stylesheet.
    @Source("Slider.css")
    Css sliderCss();
  }

  /**
   * Listener interface for being notified about slider events.
   */
  public interface Listener {
    public void onStateChanged(boolean active);
  }

  /**
   * The view for a Slider.
   */
  public static class View extends CompositeView<ViewEvents> {
    private static final double DURATION = AnimationUtils.SHORT_TRANSITION_DURATION;

    private final Resources resources;
    private final Css css;
    private final Element sliderLeft;
    private final Element sliderSplitter;
    private final Element sliderRight;
    private final int paddingForBorderRadius;

    private boolean animating;

    private final EventListener toggleSliderListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (!animating) {
          animateSlider();
        }
      }
    };

    private class SplitterController extends ResizeController {
      private boolean active;
      private int value;
      private int lastDelta;

      private SplitterController() {
        super(resources, sliderSplitter, new ElementInfo(sliderLeft, ResizeProperty.RIGHT),
            new ElementInfo(sliderSplitter, ResizeProperty.RIGHT),
            new ElementInfo(sliderRight, ResizeProperty.RIGHT));
        setNegativeDeltaW(true);
        showResizingCursor(false);
      }

      @Override
      protected boolean canStartResizing() {
        return !animating;
      }

      @Override
      protected void resizeStarted() {
        animating = true;
        active = CssUtils.isVisible(sliderLeft);
        value = prepareForAnimation(active);
        showResizingCursor(true);
        setRestrictions();
        super.resizeStarted();
      }

      @Override
      protected void resizeEnded() {
        super.resizeEnded();
        if (lastDelta > 0) {
          active = true;
        } else if (lastDelta < 0) {
          active = false;
        }
        runPreparedAnimation(active, value);
        showResizingCursor(false);
        lastDelta = 0;
      }

      @Override
      protected void applyDelta(int deltaW, int deltaH) {
        if (deltaW != 0) {
          lastDelta = deltaW;
        }
        super.applyDelta(deltaW, deltaH);
      }

      private void setRestrictions() {
        for (ElementInfo elementInfo : getElementInfos()) {
          elementInfo.setPropertyMinValue(0);
          elementInfo.setPropertyMaxValue(value - paddingForBorderRadius);
        }
      }

      private void showResizingCursor(boolean show) {
        CssUtils.setClassNameEnabled(sliderSplitter, getCss().hSplitter(), show);
      }
    }

    public View(Resources resources) {
      this.resources = resources;
      css = resources.sliderCss();

      sliderLeft = Elements.createDivElement(css.sliderLeft());
      sliderSplitter = Elements.createDivElement(css.sliderSplitter());
      sliderRight = Elements.createDivElement(css.sliderRight());
      paddingForBorderRadius = CssUtils.parsePixels(css.paddingForBorderRadius());

      Element rootElement = Elements.createDivElement(css.sliderRoot());
      rootElement.appendChild(sliderLeft);
      rootElement.appendChild(sliderSplitter);
      rootElement.appendChild(sliderRight);
      setElement(rootElement);

      rootElement.addEventListener(Event.CLICK, toggleSliderListener, false);

      new SplitterController().start();
    }

    private void setActive(boolean active) {
      CssUtils.setDisplayVisibility(sliderLeft, active);
      CssUtils.setDisplayVisibility(sliderRight, !active);

      sliderLeft.getStyle().removeProperty("width");
      sliderRight.getStyle().removeProperty("width");

      sliderLeft.getStyle().removeProperty("right");
      sliderSplitter.getStyle().removeProperty("right");
      sliderRight.getStyle().removeProperty("right");

      CssUtils.setClassNameEnabled(sliderLeft, css.sliderFlex(), active);
      CssUtils.setClassNameEnabled(sliderRight, css.sliderFlex(), !active);

      new DebugAttributeSetter().add(SLIDER_MODE, String.valueOf(active)).on(getElement());
    }

    private void animateSlider() {
      final boolean active = CssUtils.isVisible(sliderLeft);
      final int value = prepareForAnimation(active);

      // Need to defer the animation, until both elements are visible.
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          runPreparedAnimation(active, value);
        }
      });

      animating = true;
    }

    private int prepareForAnimation(boolean active) {
      Element visibleButton = active ? sliderLeft : sliderRight;
      int value = visibleButton.getOffsetWidth();
      String rightStart = (active ? 0 : value - paddingForBorderRadius)
          + CSSStyleDeclaration.Unit.PX;

      sliderLeft.getStyle().setWidth(value + CSSStyleDeclaration.Unit.PX);
      sliderRight.getStyle().setWidth(value + CSSStyleDeclaration.Unit.PX);

      sliderLeft.removeClassName(css.sliderFlex());
      sliderRight.removeClassName(css.sliderFlex());

      CssUtils.setDisplayVisibility(sliderLeft, true);
      CssUtils.setDisplayVisibility(sliderRight, true);

      sliderLeft.getStyle().setRight(rightStart);
      sliderSplitter.getStyle().setRight(rightStart);
      sliderRight.getStyle().setRight(rightStart);

      return value;
    }

    private void runPreparedAnimation(final boolean active, int value) {
      String rightEnd = (active ? value - paddingForBorderRadius : 0) + CSSStyleDeclaration.Unit.PX;

      if (value <= 0 || rightEnd.equals(sliderRight.getStyle().getRight())) {
        setActive(!active);
        getDelegate().onStateChanged(!active);
        // We should be "animating" until the event queue is processed, so that,
        // for example, a mouse CLICK event should be skipped after a resizing.
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            animating = false;
          }
        });
        return;
      }

      AnimationUtils.animatePropertySet(sliderLeft, "right", rightEnd, DURATION);
      AnimationUtils.animatePropertySet(sliderSplitter, "right", rightEnd, DURATION);
      AnimationUtils.animatePropertySet(sliderRight, "right", rightEnd, DURATION,
          new EventListener() {
            @Override
            public void handleEvent(Event evt) {
              setActive(!active);
              getDelegate().onStateChanged(!active);
              animating = false;
            }
          });
      // In case the previous commands call an old event listener that would
      // set the animating flag to false.
      animating = true;
    }

    private void setSliderStrings(String activatedSlider, String deactivatedSlider) {
      sliderLeft.setTextContent(activatedSlider);
      sliderRight.setTextContent(deactivatedSlider);
    }
  }

  private interface ViewEvents {
    void onStateChanged(boolean active);
  }

  /**
   * Creates an instance of the Slider with its default View.
   *
   * @return a {@link Slider} instance with default View
   */
  public static Slider create(View view) {
    return new Slider(view);
  }

  private Listener listener;

  private Slider(View view) {
    super(view);

    getView().setDelegate(new ViewEvents() {
      @Override
      public void onStateChanged(boolean active) {
        if (Slider.this.listener != null) {
          Slider.this.listener.onStateChanged(active);
        }
      }
    });

    setActive(true);
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setActive(boolean active) {
    getView().setActive(active);
  }

  /**
   * Sets UI strings to display on the slider when it is activated and
   * deactivated correspondingly.
   */
  public void setSliderStrings(String activatedSlider, String deactivatedSlider) {
    getView().setSliderStrings(activatedSlider, deactivatedSlider);
  }
}
