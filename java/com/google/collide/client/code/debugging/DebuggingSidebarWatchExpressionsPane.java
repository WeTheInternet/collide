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


import collide.client.util.Elements;

import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;

/**
 * Watch expressions pane in the debugging sidebar.
 *
 */
public class DebuggingSidebarWatchExpressionsPane extends UiComponent<
    DebuggingSidebarWatchExpressionsPane.View> {

  public interface Css extends CssResource {
    String root();
    String button();
    String plusButton();
    String refreshButton();
  }

  interface Resources extends ClientBundle, RemoteObjectTree.Resources {
    @Source("DebuggingSidebarWatchExpressionsPane.css")
    Css workspaceEditorDebuggingSidebarWatchExpressionsPaneCss();

    @Source("plusButton.png")
    ImageResource plusButton();

    @Source("refreshButton.png")
    ImageResource refreshButton();
  }

  /**
   * Listener of this pane's events.
   */
  interface Listener {
    void onBeforeAddWatchExpression();
    void onWatchExpressionsCountChange();
  }

  /**
   * The view for the pane.
   */
  static class View extends CompositeView<ViewEvents> {
    private final Resources resources;
    private final Css css;
    private final RemoteObjectTree.View treeView;
    private final Element plusButton;
    private final Element refreshButton;

    View(Resources resources) {
      this.resources = resources;
      css = resources.workspaceEditorDebuggingSidebarWatchExpressionsPaneCss();
      treeView = new RemoteObjectTree.View(resources);

      plusButton = Elements.createDivElement(css.button(), css.plusButton());
      refreshButton = Elements.createDivElement(css.button(), css.refreshButton());

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(treeView.getElement());
      setElement(rootElement);

      attachButtonListeners();
    }

    private void attachButtonListeners() {
      plusButton.addEventListener(Event.CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          getDelegate().onAddNewExpression();
          evt.stopPropagation();
        }
      }, false);

      refreshButton.addEventListener(Event.CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          getDelegate().onWatchRefresh();
          evt.stopPropagation();
        }
      }, false);
    }

    private void attachControlButtons(Element controlButtonsRoot) {
      controlButtonsRoot.appendChild(refreshButton);
      controlButtonsRoot.appendChild(plusButton);
    }
  }

  /**
   * The view events.
   */
  private interface ViewEvents {
    void onAddNewExpression();
    void onWatchRefresh();
  }

  static DebuggingSidebarWatchExpressionsPane create(View view, DebuggerState debuggerState) {
    RemoteObjectTree tree = RemoteObjectTree.create(view.treeView, view.resources, debuggerState);
    return new DebuggingSidebarWatchExpressionsPane(view, tree);
  }

  private final RemoteObjectTree tree;
  private Listener delegateListener;

  private final RemoteObjectTree.Listener remoteObjectTreeListener =
      new RemoteObjectTree.Listener() {
        @Override
        public void onRootChildrenChanged() {
          if (delegateListener != null) {
            delegateListener.onWatchExpressionsCountChange();
          }
        }
      };

  private final class ViewEventsImpl implements ViewEvents {

    @Override
    public void onAddNewExpression() {
      if (delegateListener != null) {
        delegateListener.onBeforeAddWatchExpression();
      }
      tree.collapseRootChildren();
      tree.addMutableRootChild();
    }

    @Override
    public void onWatchRefresh() {
      refreshWatchExpressions();
    }
  }
  
  @VisibleForTesting
  DebuggingSidebarWatchExpressionsPane(View view, RemoteObjectTree tree) {
    super(view);

    this.tree = tree;
    tree.setListener(remoteObjectTreeListener);

    view.setDelegate(new ViewEventsImpl());
  }

  void setListener(Listener listener) {
    delegateListener = listener;
  }

  void attachControlButtons(Element controlButtonsRoot) {
    getView().attachControlButtons(controlButtonsRoot);
  }

  void refreshWatchExpressions() {
    tree.reevaluateRootChildren();
  }

  int getExpressionsCount() {
    return tree.getRootChildrenCount();
  }
}
