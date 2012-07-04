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

package com.google.collide.client.common;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;

/**
 * ClientBundle resources that are shared across multiple top level presenters.
 *
 */
public class BaseResources {

  public interface Css extends CssResource {

    /**
     * Returns the left and right side padding on buttons.
     */
    int buttonHorizontalPadding();

    /**
     * Applies to internally linking anchor elements.
     */
    // TODO: see if this style can be removed after project membership
    // flow is reworked (Only place used is the membership requests link).
    String anchor();

    /**
     * The class name to apply to buttons.
     *
     * Recommended to use an anchor element. Adding <code>
     *href = &quot;javascript:;&quot;</code> to the anchor element ensures that
     * it triggers a click event when the ENTER key is pressed. This is useful for keyboard
     * navigation through a form.
     */
    String button();
    
    /**
     * A smaller button variant.
     */
    String buttonSmall();

    /**
     * Append this class to the button class to make a blue button.
     */
    String buttonBlue();

    /**
     * Append this class to the button class to make a red button.
     */
    String buttonRed();
    
    /**
     * Append this class to the button class to make a green button.
     */
    String buttonGreen();

    /**
     * Append this class to an inner button div that is only a
     * background image rendered using the @sprite command. This will style it
     * like a native img tag inside a button.
     */
    String buttonImage();

    /**
     * Append this class to make the element appear hovered.
     */
    String buttonHover();

    /**
     * Append this class to make the element appear active.
     */
    String buttonActive();

    /**
     * Returns the style to apply to drawer icon buttons, which are used to show sub content.
     */
    String drawerIconButton();

    /**
     * Returns the style to apply to drawer icon buttons that are active.
     */
    String drawerIconButtonActive();

    /**
     * Returns the style to apply to drawer icon buttons that are active, using
     * a slightly lighter background if the default active style blends with the
     * surrounding contents too much.
     */
    String drawerIconButtonActiveLight();

    String documentScrollable();
    
    String checkbox();
    
    String radio();

    String textArea();

    String textInput();
    
    String textInputSmall();

    String closeX();

    String fileLabelErrorIcon();

    String headerBg();

    String modalDialogTitle();

    String modalDialogMessage();

    String searchBox();

    String searchBoxInUse();

    String downArrow();

    /* Tabs */
    String tabOuterContainer();

    String tabContainer();

    String tab();

    String activeTab();
  }

  public interface Resources extends ClientBundle {

    @Source({"base.css", "com/google/collide/client/common/constants.css"})
    Css baseCss();

    @Source("check_no_box.png")
    DataResource checkNoBoxData();

    @Source("close_x_icon.png")
    ImageResource closeXIcon();

    @Source("default_proj_icon.png")
    ImageResource defaultProjectIcon();

    @Source("disclosure_arrow_dk_grey_down.png")
    ImageResource disclosureArrowDkGreyDown();
    
    @Source("disclosure_arrow_lt_grey_down.png")
    ImageResource disclosureArrowLtGreyDown();

    @Source("file_label_error_icon.png")
    ImageResource fileLabelErrorIcon();

    @Source("fork_lg_white.png")
    ImageResource forkIconLgWhite();

    @Source("fork_sm_off.png")
    ImageResource forkIconOff();

    @Source("fork_sm_on.png")
    ImageResource forkIconOn();

    @Source("logo.png")
    ImageResource logo();

    @Source("add_plus.png")
    ImageResource addPlus();

    @Source("cancel.png")
    ImageResource cancel();

    @Source("search.png")
    ImageResource search();

    @Source("file.png")
    ImageResource file();

    @Source("folder.png")
    ImageResource folder();

    @Source("folder_open.png")
    ImageResource folderOpen();

    @Source("folder_loading.gif")
    ImageResource folderLoading();

    @Source("pageDown.png")
    ImageResource pageDownArrow();

    @Source("pageUp.png")
    ImageResource pageUpArrow();

    @Source("gear_flower.png")
    ImageResource settingsIcon();
    
    @Source("snapshot_camera.png")
    ImageResource snapshotCamera();

    @Source("wrench.png")
    ImageResource wrench();
  }
}
