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

package com.google.collide.client.search.awesomebox.shared;

import collide.client.common.CommonResources;

import com.google.collide.client.search.awesomebox.host.AwesomeBoxComponent;
import com.google.collide.client.search.awesomebox.host.AwesomeBoxComponentHost;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.gwt.resources.client.CssResource;

/**
 * The resources shared by the awesomebox related objects.
 */
public interface AwesomeBoxResources extends CommonResources.BaseResources, Tooltip.Resources {

  /**
   * Shared CSS styles by all {@link AwesomeBoxComponent} objects.
   */
  public interface ComponentCss extends CssResource {
    // Generic Component Styles
    String closeButton();

    // Snapshot Styles
    String snapshot();

    String snapshotComponent();

    String snapshotMessageInput();

    String snapshotTextAreaContainer();

    String snapshotLabelContainer();

    String snapshotLabel();

    // Find/Replace Styles
    String findComponent();

    String findContainer();

    String findInput();

    String findRowLabel();

    String replaceInput();

    String findRow();

    String findActions();

    String navActions();

    String actionGroup();

    String replaceActions();

    String totalMatchesContainer();

    String numMatches();
  }

  @Source({"AwesomeBoxComponentHost.css", "collide/client/common/constants.css"})
  public AwesomeBoxComponentHost.Css awesomeBoxHostCss();

  @Source({"AwesomeBoxComponent.css", "collide/client/common/constants.css"})
  public ComponentCss awesomeBoxComponentCss();
}
