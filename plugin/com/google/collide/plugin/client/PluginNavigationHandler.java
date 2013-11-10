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

package com.google.collide.plugin.client;

import collide.client.util.Elements;

import com.google.collide.client.history.PlaceConstants;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.collide.plugin.client.PluginPlace.NavigationEvent;
import com.google.collide.plugin.client.standalone.StandaloneContext;

import elemental.dom.Element;

/**
 * Handler for the selection of a Workspace.
 */
public class PluginNavigationHandler extends PlaceNavigationHandler<PluginPlace.NavigationEvent>{

  private StandaloneContext standaloneContext;

  private boolean once;

  public PluginNavigationHandler(StandaloneContext standaloneContext) {
    this.standaloneContext = standaloneContext;
    once = true;
  }

  @Override
  protected void enterPlace(NavigationEvent ev) {
    if (ev.isShow()) {
      standaloneContext.getPanel().show();
      Element devMode = Elements.getElementById("developer-mode");
      if (devMode != null)
        devMode.setAttribute("href", devMode.getAttribute("href").replace("show=true", "show=false"));
    } else if (ev.isHide()) {
      standaloneContext.getPanel().hide();
      Element devMode = Elements.getElementById("developer-mode");
      if (devMode != null)
        devMode.setAttribute("href", devMode.getAttribute("href").replace("show=false", "show=true"));
    }
    if (once) {
      once = false;
      Elements.getWindow().getLocation().setHash("/"+PlaceConstants.WORKSPACE_PLACE_NAME);
    }
  }

}
