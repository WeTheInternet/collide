package com.google.collide.client.code;

import com.google.collide.client.history.Place;
import com.google.collide.client.util.AnimationUtils;
import com.google.collide.client.util.ResizeController;

import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;

/**
 * Class responsible for managing resizing of the Navigation Section.
 */
class NavigatorAreaResizeController extends ResizeController
    implements NavigationAreaExpansionEvent.Handler {

  private static final double DURATION = 0.2;

  int oldNavWidth;
  int oldSplitterLeft;

  private final Element splitter;
  private final Element navigatorArea;
  private final Element contentArea;
  private final int splitterWidth;
  private final int splitterOverlap;
  private final Place currentPlace;

  public NavigatorAreaResizeController(Place currentPlace,
      CodePerspective.Resources resources,
      Element splitter,
      Element navigatorArea,
      Element contentArea,
      int splitterWidth,
      int splitterOverlap) {
    super(resources, splitter, new ElementInfo(navigatorArea, ResizeProperty.WIDTH),
        new ElementInfo(splitter, ResizeProperty.LEFT),
        new ElementInfo(contentArea, ResizeProperty.LEFT));
    this.currentPlace = currentPlace;
    this.splitter = splitter;
    this.navigatorArea = navigatorArea;
    this.contentArea = contentArea;
    this.splitterWidth = splitterWidth;
    this.splitterOverlap = splitterOverlap;
  }

  @Override
  public void onNavAreaExpansion(NavigationAreaExpansionEvent evt) {
    int targetNavWidth = oldNavWidth;
    int targetSplitterLeft = oldSplitterLeft;
    int targetContentAreaLeft = oldSplitterLeft + splitterWidth;

    // If we are asked to collapse.
    if (!evt.shouldExpand()) {

      // Remember the old sizes if we happen to be expanded.
      if (!isCollapsed(splitter)) {
        oldNavWidth = navigatorArea.getClientWidth();
        oldSplitterLeft = splitter.getOffsetLeft();
      }

      // We want to collapse.
      targetNavWidth = 0;
      targetSplitterLeft = 0;
      targetContentAreaLeft = 0;
    }

    // If we ask to expand, but we are already expanded, do nothing.
    if (evt.shouldExpand() && !isCollapsed(splitter)) {
      return;
    }

    splitter.getStyle().setLeft(targetSplitterLeft, CSSStyleDeclaration.Unit.PX);
    splitter.getStyle().setDisplay("none");

    AnimationUtils.backupOverflow(navigatorArea.getStyle());
    AnimationUtils.animatePropertySet(navigatorArea, "width",
        targetNavWidth + CSSStyleDeclaration.Unit.PX, DURATION, new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            splitter.getStyle().setDisplay("");
            AnimationUtils.restoreOverflow(navigatorArea.getStyle());
          }
        });
    AnimationUtils.animatePropertySet(
        contentArea, "left", targetContentAreaLeft + CSSStyleDeclaration.Unit.PX, DURATION);
  }

  @Override
  public void start() {
    super.start();
    attachDblClickListener();
    currentPlace.registerSimpleEventHandler(NavigationAreaExpansionEvent.TYPE, this);
  }

  @Override
  protected void resizeStarted() {
    // Make sure the content area is to the right of the splitter in the
    // case where we drag after collapsing.
    String contentAreaOffset =
        (splitterOverlap + getSplitter().getOffsetLeft()) + CSSStyleDeclaration.Unit.PX;
    contentArea.getStyle().setLeft(contentAreaOffset);
    super.resizeStarted();
  }

  private void attachDblClickListener() {

    // Double clicking animates the splitter to hide and show the nav area.
    // Equivalent to an automated resize.
    splitter.setOnDblClick(new EventListener() {

      @Override
      public void handleEvent(Event evt) {

        // We just want to toggle. If it is collapsed, we want to expand.
        currentPlace.fireEvent(new NavigationAreaExpansionEvent(isCollapsed(splitter)));
      }
    });
  }

  private boolean isCollapsed(Element splitter) {
    return splitter.getOffsetLeft() == 0;
  }
}
