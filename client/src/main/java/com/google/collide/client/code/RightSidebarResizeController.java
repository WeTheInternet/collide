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

import com.google.collide.client.history.Place;
import com.google.collide.client.util.AnimationUtils;
import com.google.collide.client.util.ResizeController;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;

/**
 * Class responsible for managing resizing of the right sidebar.
 *
 */
class RightSidebarResizeController extends ResizeController
    implements
      RightSidebarExpansionEvent.Handler,
      RightSidebarToggleEvent.Handler {

  private static enum ExpansionState {
   COLLAPSED, EXPANDED, COLLAPSING, EXPANDING
  }

  private static final double DURATION = 0.2;
  private static final int DEFAULT_SIDEBAR_WIDTH = 300;

  private final int collapsedSplitterRight;
  private final int defaultEditableContentAreaRight;
  private final Element splitter;
  private final Element sidebarArea;
  private final Element contentArea;
  private final int splitterWidth;
  private final Place currentPlace;

  int oldSidebarWidth;
  int oldSplitterRight;
  ExpansionState expansionState = ExpansionState.COLLAPSED;

  public RightSidebarResizeController(Place currentPlace,
      CodePerspective.Resources resources,
      Element splitter,
      Element sidebarArea,
      Element contentArea,
      int splitterWidth,
      int collapsedSplitterRight, int defaultEditableContentAreaRight) {
    super(resources, splitter, new ElementInfo(sidebarArea, ResizeProperty.WIDTH), new ElementInfo(
        splitter, ResizeProperty.RIGHT), new ElementInfo(contentArea, ResizeProperty.RIGHT));
    this.currentPlace = currentPlace;
    this.splitter = splitter;
    this.sidebarArea = sidebarArea;
    this.contentArea = contentArea;
    this.splitterWidth = splitterWidth;
    this.defaultEditableContentAreaRight = defaultEditableContentAreaRight;
    oldSplitterRight = this.collapsedSplitterRight = collapsedSplitterRight;
  }

  @Override
  public void onRightSidebarExpansion(RightSidebarExpansionEvent evt) {
    showSidebar(evt.shouldExpand());
  }

  private void showSidebar(boolean show) {
    int targetSidebarWidth = oldSidebarWidth;
    int targetSplitterRight = oldSplitterRight;
    int targetContentAreaRight = oldSplitterRight + splitterWidth;

    if (show) {
      // If we ask to expand, but we are already expanded, do nothing.
      if (!isAnimating() && !isCollapsed()) {
        return;
      }
      if (targetSidebarWidth <= 0) {
        targetSidebarWidth = DEFAULT_SIDEBAR_WIDTH;
        targetSplitterRight = targetSidebarWidth - splitterWidth;
        targetContentAreaRight = targetSidebarWidth;
      }
    } else {
      // Remember the old sizes if we happen to be expanded.
      if (!isAnimating() && !isCollapsed()) {
        oldSidebarWidth = sidebarArea.getClientWidth();
        oldSplitterRight = getSplitterOffsetRight();
      }

      // We want to collapse.
      targetSidebarWidth = splitterWidth;
      targetSplitterRight = collapsedSplitterRight;
      targetContentAreaRight = defaultEditableContentAreaRight;
    }

    splitter.getStyle().setRight(targetSplitterRight, CSSStyleDeclaration.Unit.PX);
    splitter.getStyle().setDisplay("none");

    final String targetSidebarVisibility =
        targetSplitterRight <= 0 ? CSSStyleDeclaration.Visibility.HIDDEN : "";

    AnimationUtils.backupOverflow(sidebarArea.getStyle());
    AnimationUtils.animatePropertySet(sidebarArea, "width",
        targetSidebarWidth + CSSStyleDeclaration.Unit.PX, DURATION, new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            splitter.getStyle().setDisplay("");
            AnimationUtils.restoreOverflow(sidebarArea.getStyle());
            sidebarArea.getStyle().setVisibility(targetSidebarVisibility);
            expansionState = expansionState == ExpansionState.EXPANDING ? ExpansionState.EXPANDED:
                ExpansionState.COLLAPSED;
          }
        });
    AnimationUtils.animatePropertySet(
        contentArea, "right", targetContentAreaRight + CSSStyleDeclaration.Unit.PX, DURATION);

    sidebarArea.getStyle().setVisibility("");
    expansionState = show ? ExpansionState.EXPANDING : ExpansionState.COLLAPSING;
  }

  @Override
  public void onRightSidebarToggled(RightSidebarToggleEvent evt) {
    showSidebar(isCollapsed());
  }

  @Override
  public void start() {
    super.start();
    attachDblClickListener();
    currentPlace.registerSimpleEventHandler(RightSidebarExpansionEvent.TYPE, this);
    currentPlace.registerSimpleEventHandler(RightSidebarToggleEvent.TYPE, this);
  }

  @Override
  protected void resizeStarted() {
    sidebarArea.getStyle().setVisibility("");
    super.resizeStarted();
  }

  private void attachDblClickListener() {

    // Double clicking animates the splitter to hide and show the nav area.
    // Equivalent to an automated resize.
    splitter.setOndblclick(new EventListener() {

      @Override
      public void handleEvent(Event evt) {

        // We just want to toggle. If it is collapsed, we want to expand.
        currentPlace.fireEvent(new RightSidebarExpansionEvent(isCollapsed()));
      }
    });
  }

  private boolean isAnimating() {
    return expansionState == ExpansionState.EXPANDING
        || expansionState == ExpansionState.COLLAPSING;
  }

  private boolean isCollapsed() {
    if (isAnimating()) {
      // Splitter is hidden at this point.
      return expansionState == ExpansionState.COLLAPSING;
    }
    return getSplitterOffsetRight() == collapsedSplitterRight;
  }

  private int getSplitterOffsetRight() {
    return splitter.getOffsetParent().getClientWidth() - splitter.getOffsetLeft()
        - splitter.getOffsetWidth();
  }
}
