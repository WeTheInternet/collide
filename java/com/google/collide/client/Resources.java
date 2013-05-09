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

package com.google.collide.client;

import com.google.collide.client.common.BaseResources;
import com.google.collide.client.status.StatusPresenter;
import com.google.collide.client.ui.panel.Panel;
import com.google.collide.client.ui.popup.Popup;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.workspace.WorkspaceShell;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;

/**
 * Interface for resources, e.g., css, images, text files, etc. Make sure you
 * add your resource to
 * {@link com.google.collide.client.Collide#onModuleLoad()}.
 */
public interface Resources
    extends
    BaseResources.Resources,
    StatusPresenter.Resources,
    WorkspaceShell.Resources,
    // TODO: Once we have actual consumers of the Tooltip class, we
    // can just have them extend it instead of doing it on the base interface.
    Tooltip.Resources,
    Popup.Resources,
    Panel.Resources{

  /**
   * Interface for css resources.
   */
  public interface AppCss extends CssResource {
  }

  @Source({"app.css", "com/google/collide/client/common/constants.css"})
  @NotStrict
  AppCss appCss();
}
