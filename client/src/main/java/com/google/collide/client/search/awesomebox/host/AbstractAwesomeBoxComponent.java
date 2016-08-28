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

package com.google.collide.client.search.awesomebox.host;


/**
 * Defines a component which is hosted by the {@link AwesomeBoxComponentHost}.
 *
 */
public abstract class AbstractAwesomeBoxComponent implements AwesomeBoxComponent {

  private final HideMode hideMode;
  private final String placeHolderText;
  private final HiddenBehavior hiddenBehavior;
  private ComponentHost host;

  /**
   * Creates a new {@link AbstractAwesomeBoxComponent} with
   * {@link AwesomeBoxComponent.HideMode#AUTOHIDE},
   * {@link AwesomeBoxComponent.HiddenBehavior#STAY_ACTIVE} and a default
   * placeholder text of 'Actions...'.
   */
  public AbstractAwesomeBoxComponent() {
    this(HideMode.AUTOHIDE, HiddenBehavior.REVERT_TO_DEFAULT, "Actions...");
  }

  public AbstractAwesomeBoxComponent(
      HideMode hideMode, HiddenBehavior hideBehavior, String placeHolderText) {
    this.hideMode = hideMode;
    this.hiddenBehavior = hideBehavior;
    this.placeHolderText = placeHolderText;
  }

  @Override
  public HideMode getHideMode() {
    return hideMode;
  }

  @Override
  public HiddenBehavior getHiddenBehavior() {
    return hiddenBehavior;
  }

  @Override
  public String getPlaceHolderText() {
    return placeHolderText;
  }
  
  @Override
  public String getTooltipText() {
    // no tooltip by default
    return null;
  }

  public void hide() {
    // Component is already hidden
    if (host == null) {
      return;
    }
    // request that our host hide us
    host.requestHide();
    host = null;
  }
  
  /**
   * @return true if this component is active.
   */
  public boolean isActive() {
    return host != null;
  }

  /**
   * Notifies the component that the component has been hidden and its base
   * element has been removed from the DOM.
   */
  @Override
  public void onHide() {
    host = null;
  }

  /**
   * Notifies the component that it has been added to the DOM and is visible.
   */
  @Override
  public void onShow(ComponentHost host, ShowReason reason) {
    this.host = host;
  }
}
