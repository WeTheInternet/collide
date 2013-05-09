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

package com.google.collide.client.ui.dropdown;

import com.google.collide.client.ui.dropdown.DropdownWidgets.Resources;
import com.google.collide.client.ui.list.KeyboardSelectionController;
import com.google.collide.client.ui.list.SimpleList;
import com.google.collide.client.ui.list.SimpleList.ListItemRenderer;
import com.google.collide.client.ui.menu.AutoHideComponent.AutoHideHandler;
import com.google.collide.client.ui.menu.AutoHideController;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.menu.PositionController.PositionerBuilder;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.Elements;
import com.google.collide.json.shared.JsonArray;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;

/**
 * A controller that can add a dropdown to any element.
 *
 */
public class DropdownController<M> {

  /**
   * Simplifies construction of a dropdown controller by setting reasonable
   * defaults for most options.
   *
   */
  public static class Builder<M> {

    /** Indicates the width of the dropdown's anchor should be used. */
    public final static int WIDTH_OF_ANCHOR = -1;

    private Element input;
    private Tooltip anchorTooltip;
    private boolean autoFocus = true;
    private boolean enableKeyboardSelection = false;
    private int maxHeight;
    private int maxWidth;

    private final Resources res;
    private final Listener<M> listener;
    private final ListItemRenderer<M> renderer;
    private final Positioner positioner;
    private final Element trigger;
    
    /**
     * @see DropdownPositionerBuilder
     */
    public Builder(Positioner positioner, Element trigger, DropdownWidgets.Resources res,
        Listener<M> listener,
        SimpleList.ListItemRenderer<M> renderer) {
      this.positioner = positioner;
      this.res = res;
      this.listener = listener;
      this.renderer = renderer;
      this.trigger = trigger;
    }

    public Builder<M> setInputTargetElement(Element inputTarget) {
      this.input = inputTarget;
      return this;
    }

    public Builder<M> setAnchorTooltip(Tooltip tooltip) {
      this.anchorTooltip = tooltip;
      return this;
    }

    public Builder<M> setShouldAutoFocusOnOpen(boolean autoFocus) {
      this.autoFocus = autoFocus;
      return this;
    }

    public Builder<M> setKeyboardSelectionEnabled(boolean enabled) {
      this.enableKeyboardSelection = enabled;
      return this;
    }

    public Builder<M> setMaxHeight(int maxHeight) {
      this.maxHeight = maxHeight;
      return this;
    }

    public Builder<M> setMaxWidth(int maxWidth) {
      this.maxWidth = maxWidth;
      return this;
    }

    public DropdownController<M> build() {
      return new DropdownController<M>(res,
          listener,
          renderer,
          positioner,
          trigger,
          input,
          anchorTooltip,
          autoFocus,
          enableKeyboardSelection,
          maxHeight,
          maxWidth);
    }
  }

  /**
   * A {@link PositionerBuilder} which starts with its {@link VerticalAlign} property set to
   * {@link VerticalAlign#BOTTOM} for convenience when building dropdowns.
   */
  public static class DropdownPositionerBuilder extends PositionerBuilder {
    public DropdownPositionerBuilder() {
      setVerticalAlign(VerticalAlign.BOTTOM);
    }
  }

  /**
   * Base listener that provides default implementations (i.e. no-op) for the
   * Listener interface. This allows subclasses to only override the methods it
   * is interested in.
   */
  public static class BaseListener<M> implements Listener<M> {
    @Override
    public void onItemClicked(M item) {
    }

    @Override
    public void onHide() {
    }

    @Override
    public void onShow() {
    }
  }

  public interface Listener<M> {
    void onItemClicked(M item);

    void onHide();

    void onShow();
  }

  private static final int DROPDOWN_ZINDEX = 20005;

  private final DropdownWidgets.Resources res;
  private final Listener<M> listener;
  private final ListItemRenderer<M> renderer;

  private JsonArray<M> itemsForLazyCreation;
  private SimpleList<M> list;
  private AutoHideController listAutoHider;
  // if null, keyboard selection is disabled
  private KeyboardSelectionController keyboardSelectionController = null;
  private final Element inputTarget;
  private final Element trigger;
  private final Tooltip anchorTooltip;
  private final boolean shouldAutoFocusOnOpen;
  private final boolean enableKeyboardSelection;
  private final int maxHeight;
  private final int maxWidth;
  private final Positioner positioner;
  private PositionController positionController;
  private boolean isDisabled;

  /**
   * Creates a DropdownController which appears relative to an anchor.
   *
   * @param res the {@link Resources}
   * @param listener a {@link Listener} for dropdown events.
   * @param renderer the {@link ListItemRenderer} for rendering each list item
   * @param positioner the {@link Positioner} used by {@link PositionController} to position the
   *        dropdown.
   * @param trigger the item (usually a button) for which to show the menu (optional)
   * @param inputTarget the element used to record input from for keyboard navigation
   * @param shouldAutoFocusOnOpen autofocuses the list on open for keyboard navigation
   */
  private DropdownController(DropdownWidgets.Resources res,
      Listener<M> listener,
      SimpleList.ListItemRenderer<M> renderer,
      Positioner positioner,
      Element trigger,
      Element inputTarget,
      Tooltip anchorTooltip,
      boolean shouldAutoFocusOnOpen,
      boolean enableKeyboardSelection,
      int maxHeight,
      int maxWidth) {
    this.res = res;
    this.listener = listener;
    this.renderer = renderer;
    this.positioner = positioner;
    this.inputTarget = inputTarget;
    this.trigger = trigger;
    this.anchorTooltip = anchorTooltip;
    this.shouldAutoFocusOnOpen = shouldAutoFocusOnOpen;
    this.enableKeyboardSelection = enableKeyboardSelection;
    this.maxHeight = maxHeight;
    this.maxWidth = maxWidth;

    if (trigger != null) {
      attachEventHandlers(trigger);
    }
  }

  private void attachEventHandlers(Element trigger) {
    trigger.addEventListener(Event.CLICK, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (listAutoHider == null || !listAutoHider.isShowing()) {
          show();
        } else {
          hide();
        }
      }
    }, false);
  }

  public void setItems(JsonArray<M> items) {
    if (list != null) {
      list.render(items);
    } else {
      itemsForLazyCreation = items;
    }
  }

  /**
   * Sets the tooltip associated with the element so it can be disabled while
   * the dropdown is showing.
   */
  public void setElementTooltip(Tooltip tooltip) {
    listAutoHider.setTooltip(tooltip);
  }

  /**
   * Show the controller relative to the anchor.
   */
  public void show() {
    if (!isDisabled && preShowCheck()) {
      updatePositionAndSize();
      postShow();
    }
  }
  
  public boolean isShowing() {
    return listAutoHider != null && listAutoHider.isShowing();
  }

  /**
   * Hides the dropdown if it is showing. Shows it if it is hidden.
   */
  public void toggle() {
    if (isShowing()) {
      hide();
    } else {
      show();
    }
  }

  /**
   * Will show the controller at the given position (ignoring the anchor).
   */
  public void showAtPosition(int x, int y) {
    if (!isDisabled && preShowCheck()) {
      updatePositionAndSizeAtCoordinates(x, y);
      postShow();
    }
  }
  
  /**
   * Do this before showing the dropdown
   * @return true if we should show, false otherwise
   */
  private boolean preShowCheck() {
    ensureDropdownCreated();
    return list.size() > 0;
  }

  private void postShow() {
    if (keyboardSelectionController != null) {
      keyboardSelectionController.setHandlerEnabled(true);
    }
    listAutoHider.show();
    if (shouldAutoFocusOnOpen) {
      list.getView().focus();
    }
  }

  public void hide() {
    if (keyboardSelectionController != null) {
      keyboardSelectionController.setHandlerEnabled(false);
    }
    if (listAutoHider != null) {
      // in case we can't hide before we've created the dropdown
      listAutoHider.hide();
    }
  }

  /**
   * Returns the simple list container element
   */
  public Element getElement() {
    // if they want the element we must ensure it has been created.
    ensureDropdownCreated();
    return list.getView();
  }

  public SimpleList<M> getList() {
    ensureDropdownCreated();
    return list;
  }

  /**
   * Enables or disables the dropdown. Does not affect the current showing state.
   */
  public void setDisabled(boolean isDisabled) {
    this.isDisabled = isDisabled;
  }

  private void ensureDropdownCreated() {
    if (list == null) {
      createDropdown();
    }
  }
  
  /**
   * Prevents default on all mouse clicks the dropdown receives. There is no way
   * to remove the handler once it is set.
   */
  public void preventDefaultOnMouseDown() {
    // Prevent the dropdown from taking focus on click
    getElement().addEventListener(Event.MOUSEDOWN, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        evt.preventDefault();
      }
    }, false);
  }

  private void createDropdown() {
    SimpleList.View listView = (SimpleList.View) Elements.createDivElement();
    listView.setTabIndex(100);
    listView.getStyle().setZIndex(DROPDOWN_ZINDEX);

    SimpleList.ListEventDelegate<M> listEventDelegate = new SimpleList.ListEventDelegate<M>() {
      @Override
      public void onListItemClicked(Element listItemBase, M itemData) {
        handleItemClicked(itemData);
      }
    };

    list = SimpleList.create(listView, res, renderer, listEventDelegate);

    if (itemsForLazyCreation != null) {
      list.render(itemsForLazyCreation);
      itemsForLazyCreation = null;
    }

    listAutoHider = AutoHideController.create(list.getView());
    listAutoHider.setTooltip(anchorTooltip);
    // Don't actually autohide (we use it for outside click dismissal)
    listAutoHider.setDelay(-1);
    if (trigger != null) {
      listAutoHider.addPartnerClickTargets(trigger);
    }

    listAutoHider.setAutoHideHandler(new AutoHideHandler() {
      @Override
      public void onShow() {
        listener.onShow();
      }

      @Override
      public void onHide() {
        listAutoHider.getView().getElement().removeFromParent();
        listener.onHide();
      }
    });

    if (enableKeyboardSelection) {
      keyboardSelectionController = new KeyboardSelectionController(
          inputTarget == null ? listView : inputTarget, list.getSelectionModel());
    }

    positionController = new PositionController(positioner, listView);
  }

  private void updatePositionAndSize() {
    Element dropdownElement = listAutoHider.getView().getElement();
    
    updateWidthAndHeight(dropdownElement);
    positionController.updateElementPosition(0, 0);
  }

  private void updatePositionAndSizeAtCoordinates(int x, int y) {
    Element dropdownElement = listAutoHider.getView().getElement();
    // ensure we're attached to the DOM
    Elements.getBody().appendChild(dropdownElement);
    updateWidthAndHeight(dropdownElement);

    positionController.updateElementPosition(x, y);
  }

  private void updateWidthAndHeight(Element dropdownElement) {
    /*
     * The 'outline' is drawn to the left of and above where the absolute top
     * and left are, so account for them in the top and right.
     */
    CSSStyleDeclaration style = dropdownElement.getStyle();

    /*
     * This width will either be 0 if we're being positioned by the mouse or the width of our
     * anchor.
     */
    int widthOfAnchorOrZero = positioner.getMinimumWidth();

    // set the minimum width
    int dropdownViewMinWidth =
        widthOfAnchorOrZero - (2 * res.defaultSimpleListCss().menuListBorderPx());
    style.setProperty("min-width", dropdownViewMinWidth + "px");

    // sets the maximum width
    boolean useWidthOfAnchor = maxWidth == Builder.WIDTH_OF_ANCHOR && widthOfAnchorOrZero != 0;
    if (maxWidth != 0 && useWidthOfAnchor) {
      int curMaxWidth = useWidthOfAnchor ? widthOfAnchorOrZero : maxWidth;
      style.setProperty("max-width", curMaxWidth + "px");
    }

    if (maxHeight != 0) {
      style.setProperty("max-height", maxHeight + "px");
    }
  }
  
  private void handleItemClicked(M item) {
    hide();
    listener.onItemClicked(item);
  }
}
