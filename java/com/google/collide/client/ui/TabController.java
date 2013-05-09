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

package com.google.collide.client.ui;

import com.google.collide.client.common.BaseResources.Resources;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.json.shared.JsonArray;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MouseEvent;
import elemental.html.HTMLCollection;
import elemental.js.dom.JsElement;

/**
 * An object which manages a list of elements and treats them as tabs.
 */
public class TabController<T> {
  /**
   * Creates a TabController for a list of elements.
   */
  public static <T> TabController<T> create(Resources res, TabClickedListener<T> listener,
      Element tabContainer, JsonArray<TabElement<T>> headers) {
    TabController<T> controller = new TabController<T>(res, listener, tabContainer);

    // Create the tab headers
    for (int i = 0; i < headers.size(); i++) {
      TabElement<T> element = headers.get(i);
      if (controller.activeTab == null) {
        controller.setActiveTab(element);
      }
      tabContainer.appendChild(element);
    }

    return controller;
  }

  public interface TabClickedListener<T> {
    public void onTabClicked(TabElement<T> element);
  }

  /**
   * An javascript overlay which encapsulates the tab identifier associated with
   * each tab header.
   */
  public static class TabElement<T> extends JsElement {
    /**
     * Creates a tab element from an element and data.
     */
    public static <T> TabElement<T> create(Resources res, String label, T data) {
      @SuppressWarnings("unchecked")
      TabElement<T> element = (TabElement<T>) Elements.createDivElement(res.baseCss().tab());
      element.setTextContent(label);
      element.setTabData(data);
      return element;
    }

    protected TabElement() {
      // javascript overlay
    }

    public final native void setTabData(T data) /*-{
      this.__tabData = data;
    }-*/;

    public final native T getTabData() /*-{
      return this.__tabData;
    }-*/;
  }

  private final Resources res;
  private final TabClickedListener<T> listener;
  private final Element container;
  private TabElement<T> activeTab;

  private TabController(Resources res, TabClickedListener<T> listener, Element container) {
    this.container = container;
    this.res = res;
    this.listener = listener;

    attachHandlers();
  }

  private void attachHandlers() {
    container.addEventListener(Event.CLICK, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        MouseEvent event = (MouseEvent) evt;
        // we could really just use the event target but this is for future
        // expandability I guess.
        Element element = CssUtils.getAncestorOrSelfWithClassName(
            (Element) event.getTarget(), res.baseCss().tab());
        if (element != null) {
          @SuppressWarnings("unchecked")
          TabElement<T> tabElement = (TabElement<T>) element;
          selectTab(tabElement);
        }
      }
    }, false);
  }

  /**
   * Selects the supplied tab dispatching the listeners clicked event.
   */
  public void selectTab(TabElement<T> element) {
    if (activeTab == element) {
      return;
    }

    setActiveTab(element);
    listener.onTabClicked(element);
  }

  public T getActiveTab() {
    return activeTab.getTabData();
  }

  /**
   * Sets the active tab based on the provided tab data without dispatching the
   * listeners clicked event.
   */
  public boolean setActiveTab(T tab) {
    if (getActiveTab() == tab) {
      return true;
    }

    HTMLCollection nodes = container.getChildren();
    for (int i = 0; i < nodes.getLength(); i++) {
      @SuppressWarnings("unchecked")
      TabElement<T> element = (TabElement<T>) nodes.item(i);
      if (element.getTabData().equals(tab)) {
        setActiveTab(element);
        return true;
      }
    }

    return false;
  }

  /**
   * Sets the active tab without triggering the {@link TabClickedListener}
   * callback.
   */
  private void setActiveTab(TabElement<T> element) {
    if (activeTab != null) {
      activeTab.removeClassName(res.baseCss().activeTab());
    }
    element.addClassName(res.baseCss().activeTab());
    activeTab = element;
  }
}
