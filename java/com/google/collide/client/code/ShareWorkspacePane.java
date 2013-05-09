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

package com.google.collide.client.code;

import com.google.collide.client.AppContext;
import com.google.collide.client.history.HistoryUtils;
import com.google.collide.client.util.AnimationController;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.InputElement;

/**
 * The presenter for the pane that asks the user to share the workspace.
 */
public class ShareWorkspacePane extends UiComponent<ShareWorkspacePane.View>
    implements HistoryUtils.SetHistoryListener {

  /**
   * Static factory method for obtaining an instance of ShareWorkspacePane.
   */
  static ShareWorkspacePane create(View view, AppContext context) {
    AnimationController ruleAnimator = new AnimationController.Builder()
        .setCollapse(true)
        .setFade(true)
        .setFixedHeight(true)
        .build();

    return new ShareWorkspacePane(view, ruleAnimator);
  }

  public interface Css extends CssResource {
    String label();

    String root();

    String rule();

    String url();
  }

  interface Resources extends ClientBundle {
    @Source("ShareWorkspacePane.css")
    Css workspaceNavigationShareWorkspacePaneCss();
  }

  static class View extends CompositeView<Void> {
    private final Css css;

    private Element label;

    private Element rule;

    private InputElement url;

    View(Resources res) {
      this.css = res.workspaceNavigationShareWorkspacePaneCss();

      setElement(createShareContents());
    }

    Element createShareContents() {
      Element root = Elements.createDivElement(css.root());

      rule = Elements.createDivElement(css.rule());

      label = Elements.createElement("p", css.label());
      label.setTextContent("Collaborate with others by pasting this link into an email or IM.");

      url = Elements.createInputTextElement(css.url());
      url.addEventListener(Event.CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          url.setSelectionRange(0, url.getValue().length());
        }
      }, false);

      // TODO: the elements above aren't added because we don't want them -- but I'm
      // not actually removing them because I want ot reduce code change
      
      return root;
    }
  }

  private final AnimationController ruleAnimator;

  private ShareWorkspacePane(View view, AnimationController ruleAnimator) {
    super(view);

    this.ruleAnimator = ruleAnimator;

    HistoryUtils.addSetHistoryListener(this);
  }

  @Override
  public void onHistorySet(String historyString) {
    getView().url.setValue(Browser.getWindow().getLocation().getHref());
  }

  public void setInstructionsVisible(boolean visible, AnimationController animator) {
    View view = getView();
    CssUtils.setDisplayVisibility2(view.label, visible);
    CssUtils.setDisplayVisibility2(view.rule, !visible);
  }
}
