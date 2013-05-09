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

package com.google.collide.client.code.debugging;

import com.google.collide.client.testing.DebugAttributeSetter;
import com.google.collide.client.testing.DebugId;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.slider.Slider;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;

/**
 * Debugging sidebar header.
 *
 * TODO: i18n for the UI strings?
 *
 */
public class DebuggingSidebarHeader extends UiComponent<DebuggingSidebarHeader.View> {

  public interface Css extends CssResource {
    String root();
    String headerLabel();
    String slider();
  }

  interface Resources extends ClientBundle, Slider.Resources, Tooltip.Resources {
    @Source("DebuggingSidebarHeader.css")
    Css workspaceEditorDebuggingSidebarHeaderCss();
  }

  /**
   * Listener for the user clicks on the sidebar header.
   */
  interface Listener {
    void onActivateBreakpoints();
    void onDeactivateBreakpoints();
  }

  /**
   * The view for the sidebar header.
   */
  static class View extends CompositeView<Void> {
    private final Resources resources;
    private final Css css;
    private final Slider.View sliderView;
    private Tooltip tooltip;

    View(Resources resources) {
      this.resources = resources;
      css = resources.workspaceEditorDebuggingSidebarHeaderCss();
      sliderView = new Slider.View(resources);

      new DebugAttributeSetter().setId(DebugId.DEBUG_BREAKPOINT_SLIDER).on(sliderView.getElement());

      Element rootElement = createRootElement(sliderView.getElement());
      setElement(rootElement);
    }

    Slider.View getSliderView() {
      return sliderView;
    }

    private Element createRootElement(Element sliderContent) {
      Element element = Elements.createDivElement(css.root());
      DomUtils.appendDivWithTextContent(element, css.headerLabel(), "Breakpoints");

      Element slider = Elements.createDivElement(css.slider());
      slider.appendChild(sliderContent);

      element.appendChild(slider);
      return element;
    }

    private void setTooltip(boolean active) {
      if (tooltip != null) {
        tooltip.destroy();
      }

      String tooltipText = active ? "Deactivate all breakpoints" : "Activate all breakpoints";
      tooltip = Tooltip.create(resources, sliderView.getElement(),
          PositionController.VerticalAlign.BOTTOM, PositionController.HorizontalAlign.MIDDLE,
          tooltipText);
    }
  }

  static DebuggingSidebarHeader create(View view) {
    Slider slider = Slider.create(view.getSliderView());
    slider.setSliderStrings("ACTIVATED", "DEACTIVATED");
    return new DebuggingSidebarHeader(view, slider);
  }

  private final Slider slider;
  private Listener delegateListener;

  @VisibleForTesting
  DebuggingSidebarHeader(View view, Slider slider) {
    super(view);

    this.slider = slider;
    getView().setTooltip(true);

    slider.setListener(new Slider.Listener() {
      @Override
      public void onStateChanged(boolean active) {
        if (delegateListener != null) {
          if (active) {
            delegateListener.onActivateBreakpoints();
          } else {
            delegateListener.onDeactivateBreakpoints();
          }
        }
        getView().setTooltip(active);
      }
    });
  }

  void setListener(Listener listener) {
    delegateListener = listener;
  }

  void setAllBreakpointsActive(boolean active) {
    slider.setActive(active);
    getView().setTooltip(active);
  }
}
