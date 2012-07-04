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

package com.google.collide.client.workspace;

import com.google.collide.client.code.CodePerspective;
import com.google.collide.client.util.Elements;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.CssResource;

import elemental.html.DivElement;
import elemental.html.Element;

/**
 * Presenter for the top-level shell of the workspace. Nothing more than a
 * container for the {@link Header} and simple API for setting the panel element
 * for the currently active workspace Perspective.
 *
 */
public class WorkspaceShell extends UiComponent<WorkspaceShell.View> {

  /**
   * Static factory method for obtaining an instance of the Workspace Shell.
   */
  public static WorkspaceShell create(View view, Header header) {
    return new WorkspaceShell(view, header);
  }

  /**
   * Style names used by the WorkspaceShell.
   */
  public interface Css extends CssResource {
    String base();

    String header();

    String perspectivePanel();
  }

  /**
   * CSS and images used by the WorkspaceShell.
   */
  public interface Resources extends CodePerspective.Resources, UnauthorizedUser.Resources {
    @Source("WorkspaceShell.css")
    Css workspaceShellCss();
  }

  /**
   * The View for the Shell.
   */
  public static class View extends CompositeView<Void> {
    private final Header.View headerView;
    private final DivElement perspectivePanel;

    View(WorkspaceShell.Resources res) {
      super(Elements.createDivElement(res.workspaceShellCss().base()));

      // Create DOM and instantiate sub-views.
      this.headerView = new Header.View(res);
      this.headerView.getElement().addClassName(res.workspaceShellCss().header());
      this.perspectivePanel = Elements.createDivElement(res.workspaceShellCss().perspectivePanel());

      Element elem = getElement();
      elem.appendChild(headerView.getElement());
      elem.appendChild(perspectivePanel);
    }

    public Header.View getHeaderView() {
      return headerView;
    }

    public DivElement getPerspectivePanel() {
      return perspectivePanel;
    }
  }

  final Header header;

  protected WorkspaceShell(View view, Header header) {
    super(view);
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }

  public void setPerspective(Element perspectiveContents) {
    getView().perspectivePanel.setInnerHTML("");
    getView().perspectivePanel.appendChild(perspectiveContents);
  }
}
