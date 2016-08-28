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

package com.google.collide.client.util;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.HasView;
import com.google.collide.shared.util.JsonCollections;

import elemental.dom.Element;

/**
 * Encapsulates a list of dom elements that are to be reused during rendering. This controller will
 * automatically create new ones when the list grows and remove old ones when it shrinks. It is
 * particularly efficient if rendering happens often.
 *
 * @param <P> A presenter which has a view
 */
public class ViewListController<P extends HasView<?>> {

  public static <P extends HasView<?>> ViewListController<P> create(
      Element container, Factory<P> factory) {
    return new ViewListController<P>(container, JsonCollections.<P>createArray(), factory);
  }

  /**
   * A factory which can return a new presenter with its view.
   *
   * @param <P> The presenter type
   */
  public interface Factory<P extends HasView<?>> {
    /**
     * Creates a new presenter and appends its view to the specified container.
     */
    public P create(Element container);
  }

  private int index = 0;
  private final JsonArray<P> list;
  private final Factory<P> factory;
  private final Element container;

  public ViewListController(Element container, JsonArray<P> list, Factory<P> factory) {
    this.list = list;
    this.container = container;
    this.factory = factory;
  }

  /**
   * Resets the list to element index 0.
   */
  public void reset() {
    index = 0;
  }

  /**
   * Retrieves the next presenter to be used (potentially creating a new one).
   */
  public P next() {
    int elementIndex = index++;
    if (elementIndex >= list.size()) {
      P presenter = factory.create(container);
      list.add(presenter);
    }

    return list.get(elementIndex);
  }

  /**
   * @return the number of elements current in the list.
   */
  public int size() {
    return list.size();
  }

  /**
   * Removes any remaining elements still attached to the DOM.
   */
  public void prune() {
    if (index >= list.size()) {
      return;
    }

    JsonArray<P> elementsToRemove = list.splice(index, list.size() - index);
    for (int i = 0; i < elementsToRemove.size(); i++) {
      elementsToRemove.get(i).getView().getElement().removeFromParent();
    }
  }

}
