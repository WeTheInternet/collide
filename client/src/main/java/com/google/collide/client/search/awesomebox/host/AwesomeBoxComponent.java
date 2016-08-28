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

import elemental.dom.Element;

/**
 * Defines the minimum interface exposed by an {@link AwesomeBoxComponentHost}
 * component. A component is one which is hosted within the AwesomeBox UI.
 * Examples include find/replace, the awesomebox itself, and the checkpoint ui.
 */
public interface AwesomeBoxComponent {
  
  /**
   * Defines how the component host hides this component.
   */
  public enum HideMode {
    /**
     * The component will autohide when the user clicks outside of the
     * {@link AwesomeBoxComponentHost} or the actual input loses focus.
     */
    AUTOHIDE,
    /** The component must be manually closed or programatically closed. */
    NO_AUTOHIDE,
  }

  public enum HiddenBehavior {
    /** The component will stay active when hidden. */
    STAY_ACTIVE,
    /**
     * When hidden, this current component hosted by the
     * {@link AwesomeBoxComponentHost} will revert to the default component.
     */
    REVERT_TO_DEFAULT
  }
  
  public enum ShowReason {
    /** Indicates the component is being shown due to a click event */
    CLICK,
    /** Indicates the component is being shown programatically */
    OTHER
  }
  
  HideMode getHideMode();

  HiddenBehavior getHiddenBehavior();

  String getPlaceHolderText();
  
  String getTooltipText();
  
  Element getElement();

  /**
   * Called when the component should steal focus, guaranteed to be called
   * immediately after onShow.
   */
  void focus();

  void onShow(ComponentHost host, ShowReason reason);

  void onHide();
}
