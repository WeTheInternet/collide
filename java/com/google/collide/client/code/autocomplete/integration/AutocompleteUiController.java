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

package com.google.collide.client.code.autocomplete.integration;

import com.google.collide.client.code.autocomplete.AutocompleteBox;
import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteProposals;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.FocusManager;
import com.google.collide.client.ui.list.SimpleList;
import com.google.collide.client.ui.list.SimpleList.View;
import com.google.collide.client.ui.menu.AutoHideController;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.anchor.ReadOnlyAnchor;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.resources.client.CssResource;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import elemental.css.CSSStyleDeclaration;
import elemental.html.ClientRect;
import elemental.html.Element;
import elemental.html.TableCellElement;
import elemental.html.TableElement;

/**
 * A controller for managing the UI for showing autocomplete proposals.
 *
 */
public class AutocompleteUiController implements AutocompleteBox {

  public interface Resources extends SimpleList.Resources {
    @Source("AutocompleteComponent.css")
    Css autocompleteComponentCss();
  }

  public interface Css extends CssResource {
    String cappedProposalLabel();
    
    String proposalLabel();

    String proposalGroup();

    String container();

    String items();

    String hint();

    int maxHeight();
  }
  
  private static final int MAX_COMPLETIONS_TO_SHOW = 100;
  private static final AutocompleteProposal CAPPED_INDICATOR = new AutocompleteProposal("");

  private final SimpleList.ListItemRenderer<AutocompleteProposal> listItemRenderer =
      new SimpleList.ListItemRenderer<AutocompleteProposal>() {
        @Override
        public void render(Element itemElement, AutocompleteProposal itemData) {
          TableCellElement label = Elements.createTDElement(css.proposalLabel());
          TableCellElement group = Elements.createTDElement(css.proposalGroup());
          
          if (itemData != CAPPED_INDICATOR) {
            label.setTextContent(itemData.getLabel());
            group.setTextContent(itemData.getPath().getPathString());
          } else {
            label.setTextContent("Type for more results");
            label.addClassName(css.cappedProposalLabel());
          }

          itemElement.appendChild(label);
          itemElement.appendChild(group);
        }

        @Override
        public Element createElement() {
          return Elements.createTRElement();
        }
      };

  private final SimpleList.ListEventDelegate<AutocompleteProposal> listDelegate =
      new SimpleList.ListEventDelegate<AutocompleteProposal>() {
        @Override
        public void onListItemClicked(Element itemElement, AutocompleteProposal itemData) {
          Preconditions.checkNotNull(delegate);
          if (itemData == CAPPED_INDICATOR) {
            return;
          }
          
          delegate.onSelect(autocompleteProposals.select(itemData));
        }
      };

  private final AutoHideController autoHideController;
  private final Css css;
  private final SimpleList<AutocompleteProposal> list;
  private Events delegate;
  private final Editor editor;
  private final Element box;
  private final Element container;
  private final Element hint;

  /** Will be non-null when the popup is showing */
  private ReadOnlyAnchor anchor;

  /**
   * True to force the layout above the anchor, false to layout below. This
   * should be set when showing from the hidden state. It's used to keep
   * the position consistent while the box is visible.
   */
  private boolean positionAbove;

  /**
   * The currently displayed proposals. This may contain more proposals than actually shown since we
   * cap the maximum number of visible proposals. This will be null if the UI is not showing.
   */
  private AutocompleteProposals autocompleteProposals;

  public AutocompleteUiController(Editor editor, Resources res) {
    this.editor = editor;
    this.css = res.autocompleteComponentCss();

    box = Elements.createDivElement();
    // Prevent our mouse events from going to the editor
    DomUtils.stopMousePropagation(box);

    TableElement tableElement = Elements.createTableElement();
    tableElement.setClassName(css.items());

    container = Elements.createDivElement(css.container());
    DomUtils.preventExcessiveScrollingPropagation(container);
    container.appendChild(tableElement);
    box.appendChild(container);

    hint = Elements.createDivElement(css.hint());
    CssUtils.setDisplayVisibility2(hint, false);
    box.appendChild(hint);

    list =
        SimpleList.create((View) box, container, tableElement, res.defaultSimpleListCss(),
            listItemRenderer, listDelegate);

    autoHideController = AutoHideController.create(box);
    autoHideController.setCaptureOutsideClickOnClose(false);
    autoHideController.setDelay(-1);
  }

  @Override
  public boolean isShowing() {
    return autoHideController.isShowing();
  }

  @Override
  public boolean consumeKeySignal(SignalEventEssence signal) {
    Preconditions.checkState(isShowing());
    Preconditions.checkNotNull(delegate);

    if ((signal.keyCode == KeyCodes.KEY_TAB) || (signal.keyCode == KeyCodes.KEY_ENTER)) {
      delegate.onSelect(autocompleteProposals.select(list.getSelectionModel().getSelectedItem()));
      return true;
    }

    if (signal.keyCode == KeyCodes.KEY_ESCAPE) {
      delegate.onCancel();
      return true;
    }

    if (signal.type != SignalEvent.KeySignalType.NAVIGATION) {
      return false;
    }

    if ((signal.keyCode == KeyCodes.KEY_DOWN)) {
      list.getSelectionModel().selectNext();
      return true;
    }

    if (signal.keyCode == KeyCodes.KEY_UP) {
      list.getSelectionModel().selectPrevious();
      return true;
    }

    if ((signal.keyCode == KeyCodes.KEY_LEFT) || (signal.keyCode == KeyCodes.KEY_RIGHT)) {
      delegate.onCancel();
      return true;
    }

    if (signal.keyCode == KeyCodes.KEY_PAGEUP) {
      list.getSelectionModel().selectPreviousPage();
      return true;
    }

    if (signal.keyCode == KeyCodes.KEY_PAGEDOWN) {
      list.getSelectionModel().selectNextPage();
      return true;
    }

    return false;
  }

  @Override
  public void setDelegate(Events delegate) {
    this.delegate = delegate;
  }

  @Override
  public void dismiss() {
    boolean hadFocus = list.hasFocus();
    autoHideController.hide();

    if (anchor != null) {
      editor.getBuffer().removeAnchoredElement(anchor, autoHideController.getView().getElement());
      anchor = null;
    }

    autocompleteProposals = null;

    FocusManager focusManager = editor.getFocusManager();
    if (hadFocus && !focusManager.hasFocus()) {
      focusManager.focus();
    }
  }

  @Override
  public void positionAndShow(AutocompleteProposals items) {
    this.autocompleteProposals = items;
    this.anchor = editor.getSelection().getCursorAnchor();

    boolean showingFromHidden = !autoHideController.isShowing();
    if (showingFromHidden) {
      list.getSelectionModel().clearSelection();
    }
    
    final JsonArray<AutocompleteProposal> itemsToDisplay;
    if (items.size() <= MAX_COMPLETIONS_TO_SHOW) {
      itemsToDisplay = items.getItems();
    } else {
      itemsToDisplay = items.getItems().slice(0, MAX_COMPLETIONS_TO_SHOW);
      itemsToDisplay.add(CAPPED_INDICATOR);
    }
    list.render(itemsToDisplay);
    if (list.getSelectionModel().getSelectedItem() == null) {
      list.getSelectionModel().setSelectedItem(0);
    }

    String hintText = items.getHint();
    if (hintText == null) {
      hint.setTextContent("");
      CssUtils.setDisplayVisibility2(hint, false);
    } else {
      hint.setTextContent(hintText);
      CssUtils.setDisplayVisibility2(hint, true);
    }

    autoHideController.show();

    editor.getBuffer().addAnchoredElement(anchor, box);

    ensureRootElementWillBeOnScreen(showingFromHidden);
  }

  private void ensureRootElementWillBeOnScreen(boolean showingFromHidden) {
    // Remove any max-heights so we can get its desired height
    container.getStyle().removeProperty("max-height");
    ClientRect bounds = box.getBoundingClientRect();
    int height = (int) bounds.getHeight();
    int delta = height - (int) container.getBoundingClientRect().getHeight();

    ClientRect bufferBounds = editor.getBuffer().getBoundingClientRect();
    int lineHeight = editor.getBuffer().getEditorLineHeight();
    int lineTop = (int) bounds.getTop() - CssUtils.parsePixels(box.getStyle().getMarginTop());

    int spaceAbove =  lineTop - (int) bufferBounds.getTop();
    int spaceBelow = (int) bufferBounds.getBottom() - lineTop - lineHeight;

    if (showingFromHidden) {
      // If it was already showing, we don't adjust the positioning.
      positionAbove = spaceAbove >= css.maxHeight() && spaceBelow < css.maxHeight();
    }

    // Get available height.
    int maxHeight = positionAbove ? spaceAbove : spaceBelow;

    // Restrict to specified height.
    maxHeight = Math.min(maxHeight, css.maxHeight());

    // Fit to content size.
    maxHeight = Math.min(maxHeight, height);

    container.getStyle().setProperty(
        "max-height", (maxHeight - delta) + CSSStyleDeclaration.Unit.PX);

    int marginTop = positionAbove ? -maxHeight : lineHeight;
    box.getStyle().setMarginTop(marginTop, CSSStyleDeclaration.Unit.PX);
  }

  @VisibleForTesting
  SimpleList<AutocompleteProposal> getList() {
    return list;
  }
}
