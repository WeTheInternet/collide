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

import javax.annotation.Nullable;

import com.google.collide.client.util.Elements;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;

/**
 * Scope variables pane in the debugging sidebar.
 */
public class DebuggingSidebarScopeVariablesPane extends UiComponent<
    DebuggingSidebarScopeVariablesPane.View> {

  public interface Css extends CssResource {
    String root();
  }

  interface Resources extends ClientBundle, RemoteObjectTree.Resources {
    @Source("DebuggingSidebarScopeVariablesPane.css")
    Css workspaceEditorDebuggingSidebarScopeVariablesPaneCss();
  }

  /**
   * The view for the scope variables pane.
   */
  static class View extends CompositeView<Void> {
    private final Resources resources;
    private final Css css;
    private final RemoteObjectTree.View treeView;

    View(Resources resources) {
      this.resources = resources;
      css = resources.workspaceEditorDebuggingSidebarScopeVariablesPaneCss();
      treeView = new RemoteObjectTree.View(resources);

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(treeView.getElement());
      setElement(rootElement);
    }
  }

  static DebuggingSidebarScopeVariablesPane create(View view, DebuggerState debuggerState) {
    RemoteObjectTree tree = RemoteObjectTree.create(view.treeView, view.resources, debuggerState);
    return new DebuggingSidebarScopeVariablesPane(view, tree);
  }

  private final RemoteObjectTree tree;

  @VisibleForTesting
  DebuggingSidebarScopeVariablesPane(View view, RemoteObjectTree tree) {
    super(view);

    this.tree = tree;
  }

  void setScopeVariablesRootNodes(@Nullable JsonArray<RemoteObjectNode> rootNodes) {
    if (rootNodes == null) {
      tree.setRoot(null);
      return;
    }

    RemoteObjectNode newRootNode = RemoteObjectNode.createRoot();
    for (int i = 0, n = rootNodes.size(); i < n; ++i) {
      newRootNode.addChild(rootNodes.get(i));
    }
    tree.setRoot(newRootNode);
  }
}
