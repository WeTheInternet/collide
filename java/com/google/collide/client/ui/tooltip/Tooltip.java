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

package com.google.collide.client.ui.tooltip;

import com.google.collide.client.common.Constants;
import com.google.collide.client.ui.menu.AutoHideComponent;
import com.google.collide.client.ui.menu.AutoHideView;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Position;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.menu.PositionController.PositionerBuilder;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.util.AnimationController;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.HoverController.HoverListener;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;
import elemental.events.EventTarget;
import elemental.events.MouseEvent;
import elemental.html.Element;
import elemental.util.Timer;

/**
 * Represents a single tooltip instance attached to any element, activated by
 * hovering.
 */
/*
 * TODO: oh, my god this thing has become a monster. Might be nice to
 * get a list of requirements and start from the top... especially if we need
 * some coach marks as well for the landing page.
 */
public class Tooltip extends AutoHideComponent<AutoHideView<Void>,
                                               AutoHideComponent.AutoHideModel> {

  /**
   * A builder used to construct a new Tooltip.
   */
  public static class Builder {

    private final Resources res;
    private final JsonArray<Element> targetElements;
    private final Positioner positioner;
    private boolean shouldShowOnHover = true;
    private TooltipRenderer renderer;

    /**
     * @see TooltipPositionerBuilder
     */
    public Builder(Resources res, Element targetElement, Positioner positioner) {
      this.res = res;
      this.positioner = positioner;
      this.targetElements = JsonCollections.createArray(targetElement);
    }

    /**
     * Adds additional target elements. If the user hovers over any of the target elements, the
     * tooltip will appear.
     */
    public Builder addTargetElements(Element... additionalTargets) {
      for (int i = 0; i < additionalTargets.length; i++) {
        targetElements.add(additionalTargets[i]);
      }
      return this;
    }

    /**
     * Sets the tooltip text. Each item in the array appears on a new line. This
     * method overwrites the tooltip renderer.
     */
    public Builder setTooltipText(String... tooltipText) {
      return setTooltipRenderer(new SimpleStringRenderer(tooltipText));
    }

    public Builder setTooltipRenderer(TooltipRenderer renderer) {
      this.renderer = renderer;
      return this;
    }
    
    /**
     * If false, will prevent the tooltip from automatically showing on hover.
     */
    public Builder setShouldListenToHover(boolean shouldShowOnHover) {
      this.shouldShowOnHover = shouldShowOnHover;
      return this;
    }

    public Tooltip build() {
      return new Tooltip(getViewInstance(res.tooltipCss()),
          res,
          targetElements,
          positioner,
          renderer,
          shouldShowOnHover);
    }
  }
  
  /**
   * A {@link PositionerBuilder} which uses some more convenient defaults for tooltips. This builder
   * defaults to {@link VerticalAlign#BOTTOM} {@link HorizontalAlign#MIDDLE} and
   * {@link Position#NO_OVERLAP}.
   */
  public static class TooltipPositionerBuilder extends PositionerBuilder {
    public TooltipPositionerBuilder() {
      setVerticalAlign(PositionController.VerticalAlign.BOTTOM);
      setHorizontalAlign(PositionController.HorizontalAlign.MIDDLE);
      setPosition(PositionController.Position.NO_OVERLAP);
    }
  }

  /**
   * Static factory method for creating a simple tooltip.
   */
  public static Tooltip create(Resources res, Element targetElement, VerticalAlign vAlign,
      HorizontalAlign hAlign, String... tooltipText) {
    return new Builder(res, targetElement, new TooltipPositionerBuilder().setVerticalAlign(vAlign)
        .setHorizontalAlign(hAlign).buildAnchorPositioner(targetElement)).setTooltipRenderer(
        new SimpleStringRenderer(tooltipText)).build();
  }

  /**
   * Interface for specifying an arbitrary renderer for tooltips.
   */
  public interface TooltipRenderer {
    Element renderDom();
  }

  /**
   * Default renderer that simply renders the tooltip text with no other DOM.
   */
  private static class SimpleStringRenderer implements TooltipRenderer {
    private final String[] tooltipText;

    SimpleStringRenderer(String... tooltipText) {
      this.tooltipText = tooltipText;
    }

    @Override
    public Element renderDom() {
      Element content = Elements.createDivElement();
      int i = 0;
      for (String p : tooltipText) {
        content.appendChild(Elements.createTextNode(p));
        if (i < tooltipText.length - 1) {
          content.appendChild(Elements.createBRElement());
          content.appendChild(Elements.createBRElement());
        }
        i++;
      }
      return content;
    }
  }

  /** The singleton view instance that all tooltips use. */
  private static AutoHideView<Void> tooltipViewInstance;

  /**
   * The currently active tooltip that is bound to the view.
   */
  private static Tooltip activeTooltip;

  /**
   * The Tooltip is a flyweight that uses a singleton View base element.
   */
  private static AutoHideView<Void> getViewInstance(Css css) {
    if (tooltipViewInstance == null) {
      tooltipViewInstance = new AutoHideView<Void>(Elements.createDivElement());
      tooltipViewInstance.getElement().addClassName(css.tooltipPosition());
    }
    return tooltipViewInstance;
  }

  public interface Css extends CssResource {
    String tooltipPosition();

    String tooltip();

    String triangle();

    String tooltipAbove();

    String tooltipRight();

    String tooltipBelow();

    String tooltipLeft();
    
    String tooltipBelowRightAligned();
  }

  public interface Resources extends ClientBundle {
    @Source({"com/google/collide/client/common/constants.css", "Tooltip.css"})
    Css tooltipCss();
    
    @Source({"com/google/collide/client/common/constants.css", "Coachmark.css"})
    Coachmark.Css coachmarkCss();
  }

  private static final int SHOW_DELAY = Constants.MOUSE_HOVER_DELAY;
  private static final int HIDE_DELAY = Constants.MOUSE_HOVER_DELAY;

  /**
   * Holds a reference to the css.
   */
  private final Css css;
  private Element contentElement;
  private final JsonArray<Element> targetElements;
  private final Timer showTimer;
  private final TooltipRenderer renderer;
  private final PositionController positionController;
  private final JsonArray<EventRemover> eventRemovers;
  private final Positioner positioner;
  private String title;
  private String maxWidth;
  private boolean isEnabled = true;
  private boolean isShowDelayDisabled;

  private Tooltip(AutoHideView<Void> view,
      Resources res,
      JsonArray<Element> targetElements,
      Positioner positioner,
      TooltipRenderer renderer,
      boolean shouldShowOnHover) {
    super(view, new AutoHideModel());
    this.positioner = positioner;
    this.renderer = renderer;
    this.css = res.tooltipCss();
    this.targetElements = targetElements;

    this.eventRemovers =
        shouldShowOnHover ? attachToTargetElement() : JsonCollections.<EventRemover>createArray();

    getView().setAnimationController(AnimationController.FADE_ANIMATION_CONTROLLER);

    positionController = new PositionController(positioner, getView().getElement());

    showTimer = new Timer() {
      @Override
      public void run() {
        show();
      }
    };
    setDelay(HIDE_DELAY);
    setCaptureOutsideClickOnClose(false);

    getHoverController().setHoverListener(new HoverListener() {
      @Override
      public void onHover() {
        if (isEnabled && !isShowing()) {
          deferredShow();
        }
      }
    });
  }

  @Override
  public void show() {

    // Nothing to do if it is showing.
    if (isShowing()) {
      return;
    }

    /*
     * Hide the old Tooltip. This will not actually hide the View because we set
     * activeTooltip to null.
     */
    Tooltip oldTooltip = activeTooltip;
    activeTooltip = null;
    if (oldTooltip != null) {
      oldTooltip.hide();
    }

    ensureContent();

    // Bind to the singleton view.
    getView().getElement().setInnerHTML("");
    getView().getElement().appendChild(contentElement);
    positionController.updateElementPosition();
    activeTooltip = this;

    super.show();
  }

  @Override
  public void forceHide() {
    super.forceHide();
    activeTooltip = null;
  }

  @Override
  protected void hideView() {
    // If another tooltip is being shown, do not hide the shared view.
    if (activeTooltip == this) {
      super.hideView();
    }
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setMaxWidth(String maxWidth) {
    this.maxWidth = maxWidth;

    // Update the content element if it is already created.
    if (contentElement != null) {
      if (maxWidth == null) {
        contentElement.getStyle().removeProperty("max-width");
      } else {
        contentElement.getStyle().setProperty("max-width", maxWidth);
      }
    }
  }

  /**
   * Enables or disables the show delay. If disabled, the tooltip will appear
   * instantly on hover. Defaults to enabled.
   * 
   * @param isDisabled true to disable the show delay
   */
  public void setShowDelayDisabled(boolean isDisabled) {
    this.isShowDelayDisabled = isDisabled;
  }

  /**
   * Enable or disable this tooltip
   */
  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  private void setPositionStyle() {
    VerticalAlign vAlign = positioner.getVerticalAlignment();
    HorizontalAlign hAlign = positioner.getHorizontalAlignment();
    switch (positioner.getVerticalAlignment()) {
      case TOP:
        contentElement.addClassName(css.tooltipAbove());
        break;
      case BOTTOM:
        if (hAlign == HorizontalAlign.RIGHT) {
          contentElement.addClassName(css.tooltipBelowRightAligned());
        } else {
          contentElement.addClassName(css.tooltipBelow());
        }
        break;
      case MIDDLE:
        if (hAlign == HorizontalAlign.LEFT) {
          contentElement.addClassName(css.tooltipLeft());
        } else if (hAlign == HorizontalAlign.RIGHT) {
          contentElement.addClassName(css.tooltipRight());
        }
        break;
    }
  }

  /**
   * Adds event handlers to the target element for the tooltip to show it on
   * hover, and update position on mouse move.
   */
  private JsonArray<EventRemover> attachToTargetElement() {
    JsonArray<EventRemover> removers = JsonCollections.createArray();
    for (int i = 0; i < targetElements.size(); i++) {
      final Element targetElement = targetElements.get(i);
      addPartner(targetElement);

      removers.add(targetElement.addEventListener(Event.MOUSEOUT, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          MouseEvent mouseEvt = (MouseEvent) evt;
          EventTarget relatedTarget = mouseEvt.getRelatedTarget();
          // Ignore the event unless we mouse completely out of the target element.
          if (relatedTarget == null || !targetElement.contains((Node) relatedTarget)) {
            cancelPendingShow();
          }
        }
      }, false));
  
      removers.add(targetElement.addEventListener(Event.MOUSEDOWN, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          cancelPendingShow();
          hide();
        }
      }, false));
    }

    return removers;
  }

  /**
   * Removes event handlers from the target element for the tooltip.
   */
  private void detachFromTargetElement() {
    for (int i = 0; i < targetElements.size(); i++) {
      removePartner(targetElements.get(i));
    }
    for (int i = 0, n = eventRemovers.size(); i < n; ++i) {
      eventRemovers.get(i).remove();
    }
    eventRemovers.clear();
  }

  /**
   * Creates the dom for this tooltip's content.
   * 
   * <code>
   *   <div class="tooltipPosition">
   *     <div class="tooltip tooltipAbove/Below/Left/Right">
   *       tooltipText
   *       <div class="tooltipTriangle"></div>
   *     </div>
   *   </div>
   * </code>
   */
  private void ensureContent() {
    if (contentElement == null) {
      contentElement = renderer.renderDom();

      if (contentElement == null) {

        // Guard against malformed renderers.
        Log.warn(getClass(), "Renderer for tooltip returned a null content element");
        contentElement = Elements.createDivElement();
        contentElement.setTextContent("An empty Tooltip!");
      }

      if (title != null) {

        // Insert a title if one is set.
        Element titleElem = Elements.createElement("b");
        titleElem.setTextContent(title);
        Element breakElem = Elements.createBRElement();
        contentElement.insertBefore(breakElem, contentElement.getFirstChild());
        contentElement.insertBefore(titleElem, contentElement.getFirstChild());
      }

      // Set the maximum width.
      setMaxWidth(maxWidth);

      contentElement.addClassName(css.tooltip());
      Element triangle = Elements.createDivElement(css.triangle());
      contentElement.appendChild(triangle);
      setPositionStyle();
    }
  }

  public void destroy() {
    showTimer.cancel();
    forceHide();
    detachFromTargetElement();
  }

  private void deferredShow() {
    if (isShowDelayDisabled || activeTooltip != null) {
      /*
       * If there is already a tooltip showing and the user mouses over an item
       * that has it's own tooltip, move the tooltip immediately. We don't want
       * to leave a lingering tooltip on the old item.
       */
      showTimer.cancel();
      showTimer.run();
    } else {
      showTimer.schedule(SHOW_DELAY);
    }
  }

  private void cancelPendingShow() {
    showTimer.cancel();
  }
}
