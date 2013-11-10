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

import com.google.collide.client.search.awesomebox.AwesomeBox.Resources;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.StringUtils;

/**
 * Renders actions which are filtered via query. Meant to allow easy
 * implementation of a large list of actions which can quickly be filtered.
 *
 *  NOTE: This section adds all items to the DOM and hides/shows them, it will
 * not handle dynamic lists nor is it particularly DOM efficient.
 *
 */
public abstract class AbstractActionSection<T extends AbstractActionSection.FilteredActionItem>
    extends AbstractAwesomeBoxSection<T> {

  protected final int maxResults;
  private final JsonArray<T> allActions;

  public static abstract class FilteredActionItem extends ActionItem {
    private final String text;

    public FilteredActionItem(Resources res, String text) {
      super(res, text);
      this.text = text.toLowerCase();
    }

    public FilteredActionItem(Resources res, String text, int modifiers, String shortcutKey) {
      super(res, text);

      this.text = text.toLowerCase();
      getElement().insertBefore(AwesomeBoxUtils.createSectionShortcut(res, modifiers, shortcutKey),
          getElement().getFirstChild());
    }

    /**
     * @return true to show as soon as the AwesomeBox is focused.
     */
    public abstract boolean onShowing();

    /**
     * Return true for this item to be visible.
     */
    public boolean onQueryChanged(String query) {
      return !StringUtils.isNullOrWhitespace(query) && text.contains(query.toLowerCase());
    }
  }

  public AbstractActionSection(AwesomeBox.Resources res, int maxResults) {
    super(res);
    this.maxResults = maxResults;

    sectionElement = AwesomeBoxUtils.createSectionContainer(res);
    allActions = getAllActions();
    initializeDom();
  }

  /**
   * Returns the header title for this section.
   */
  protected abstract String getTitle();

  /**
   * Creates all DOM for all items in getAllActions hiding them by default.
   */
  protected void initializeDom() {
    for (int i = 0; i < allActions.size(); i++) {
      CssUtils.setDisplayVisibility2(allActions.get(i).getElement(), false);
      sectionElement.appendChild(allActions.get(i).getElement());
    }
  }

  @Override
  public boolean onQueryChanged(final String query) {
    listItems.clear();
    updateItemVisibility(new ItemConditionCallback<T>() {
      @Override
      public boolean isCondition(T item) {
        return item.onQueryChanged(query);
      }
    });
    return listItems.size() > 0;
  }

  @Override
  public boolean onShowing(AwesomeBox awesomeBox) {
    listItems.clear();
    updateItemVisibility(new ItemConditionCallback<T>() {
      @Override
      public boolean isCondition(T item) {
        return item.onShowing();
      }
    });
    return listItems.size() > 0;
  }

  /**
   * Override to provide a list of actions to be added to the DOM. This will be
   * cached at construction time within the AbstractActionSection.
   */
  protected abstract JsonArray<T> getAllActions();

  private interface ItemConditionCallback<T extends FilteredActionItem> {
    public boolean isCondition(T item);
  }

  private void updateItemVisibility(ItemConditionCallback<T> callback) {
    for (int i = 0, r = 0; i < allActions.size(); i++) {
      if (r < maxResults && callback.isCondition(allActions.get(i))) {
        CssUtils.setDisplayVisibility2(allActions.get(i).getElement(), true);
        listItems.add(allActions.get(i));
        r++;
      } else {
        CssUtils.setDisplayVisibility2(allActions.get(i).getElement(), false);
      }
    }
  }
}
