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

package com.google.collide.client.search.awesomebox;

import collide.client.util.CssUtils;

import com.google.collide.client.search.awesomebox.AbstractAwesomeBoxSection.ActionItem;
import com.google.collide.client.search.awesomebox.AwesomeBox.Resources;
import com.google.collide.client.search.awesomebox.ManagedSelectionList.SelectableElement;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.events.MouseEvent;
import elemental.html.DivElement;

/**
 * Represents a generic AwesomeBoxSection which contains a selectable item list.
 */
public abstract class AbstractAwesomeBoxSection<T extends ActionItem>
    implements AwesomeBox.AwesomeBoxSection {

  /**
   * Generic wrapper around an element so it can be selected in our selection
   * list.
   */
  public static abstract class ActionItem implements SelectableElement {

    public enum ActionSource {
      CLICK, SELECTION
    }

    protected final Element element;
    protected final Resources res;

    protected ActionItem(Resources res, Element element) {
      this.element = element;
      this.res = res;

      initialize();
    }

    public ActionItem(Resources res, String text) {
      this(res, AwesomeBoxUtils.createSectionItem(res));
      element.setTextContent(text);
    }

    /**
     * Initialize's the element after element creation.
     */
    public void initialize() {
    }

    /**
     * Perform this elements action.
     */
    public abstract ActionResult doAction(ActionSource source);

    public void remove() {
      getElement().removeFromParent();
    }

    /**
     * Called to complete a query using tab completion. By default null is
     * returned indicating this item does not support completion.
     */
    public String completeQuery() {
      return null;
    }

    @Override
    public Element getElement() {
      return element;
    }

    @Override
    public boolean onSelected() {
      element.addClassName(res.awesomeBoxCss().selected());
      return true;
    }

    @Override
    public void onSelectionCleared() {
      element.removeClassName(res.awesomeBoxCss().selected());
    }
  }

  protected final Resources res;
  protected final ManagedSelectionList<T> listItems;
  protected DivElement sectionElement;

  protected AbstractAwesomeBoxSection(Resources res) {
    this.res = res;
    listItems = ManagedSelectionList.create();
  }

  @Override
  public DivElement getElement() {
    return sectionElement;
  }

  @Override
  public ActionResult onActionRequested() {
    if (listItems.hasSelection()) {
      return listItems.getSelectedElement().doAction(ActionItem.ActionSource.SELECTION);
    }
    return ActionResult.DO_NOTHING;
  }

  @Override
  public void onClearSelection() {
    listItems.clearSelection();
  }

  @Override
  public String onCompleteSelection() {
    return listItems.hasSelection() ? listItems.getSelectedElement().completeQuery() : null;
  }

  @Override
  public void onAddedToContext(AwesomeBoxContext context) {
    // no-op by default
  }

  @Override
  public void onContextChanged(AwesomeBoxContext context) {
    // no-op by default
  }

  @Override
  public void onHiding(AwesomeBox awesomeBox) {
    // no-op by default
  }

  @Override
  public boolean onMoveSelection(boolean moveDown) {
    if (!CssUtils.isVisible(getElement())) {
      return false;
    }

    return listItems.moveSelection(moveDown);
  }

  @Override
  public ActionResult onSectionClicked(MouseEvent mouseEvent) {
    mouseEvent.preventDefault();
    for (int i = 0; i < listItems.size(); i++) {
      if (listItems.get(i).getElement().contains((Node) mouseEvent.getTarget())) {
        return listItems.get(i).doAction(ActionItem.ActionSource.CLICK);
      }
    }
    return ActionResult.DO_NOTHING;
  }

  /**
   * Shows the section by default.
   */
  @Override
  public boolean onShowing(AwesomeBox awesomeBox) {
    return true;
  }
}
