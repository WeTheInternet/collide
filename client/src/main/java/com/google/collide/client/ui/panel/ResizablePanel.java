package com.google.collide.client.ui.panel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import collide.client.util.Elements;

import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.util.ResizeBounds;
import com.google.collide.client.util.ResizeController;
import com.google.collide.client.util.ResizeController.ElementInfo;
import com.google.collide.client.util.ResizeController.ResizeProperty;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.util.ArrayOf;
import elemental.util.Collections;

public class ResizablePanel <Res extends ResizablePanel.Resources & ResizeController.Resources> extends UiComponent<ResizablePanel.View<Res>> {

  public static interface Css extends CssResource {
    String content();

    String headerText();

    String east();

    String handleContainer();

    String header();

    String north();

    String northeast();

    String northwest();

    String positioner();

    String root();

    String south();

    String southeast();

    String southwest();

    String west();

    String draggable();
  }

  public interface Resources extends ClientBundle {
    @Source({"collide/client/common/constants.css", "Panel.css"})
    Css panelCss();
  }

  public static interface Interpolator {
    double interpolate(double value);
  }

  public static class View<Res extends Resources & ResizeController.Resources> extends CompositeView<Void> {

    @UiTemplate("Panel.ui.xml")
    interface PanelBinder extends UiBinder<com.google.gwt.dom.client.DivElement,View<?>> {
    }

    static PanelBinder binder = GWT.create(PanelBinder.class);

    @UiField(provided = true)
    final Css css;

    @UiField
    DivElement root;

    @UiField
    DivElement header;

    @UiField
    DivElement north;
    @UiField
    DivElement east;
    @UiField
    DivElement west;
    @UiField
    DivElement south;

    @UiField
    DivElement northeast;
    @UiField
    DivElement northwest;
    @UiField
    DivElement southeast;
    @UiField
    DivElement southwest;

    @UiField
    DivElement contentContainer;

    View(final Res resources) {
      this.css = resources.panelCss();
      binder.createAndBindUi(this);
      setElement(Elements.asJsElement(root));
    }

    @Override
    public Element getElement() {
      return super.getElement();
    }

    void setContentElement(@Nullable Element contentElement) {
      contentContainer.setInnerHTML("");
      if (contentElement != null) {
        Elements.asJsElement(contentContainer).appendChild(contentElement);
      }
    }

    void setHeaderElement(@Nullable Element contentElement) {
      header.setInnerHTML("");
      if (contentElement != null) {
        Elements.asJsElement(header).appendChild(contentElement);
      }
    }

  }

  public static class SizeLimitingElement extends ElementInfo{

    private ResizablePanel<?> panel;

    public SizeLimitingElement(ResizablePanel<?> panel, Element element, ResizeProperty property) {
      super(element, property);
      this.panel = panel;
    }

    @Override
    public int getPropertyMaxValue() {
      switch (getResizeProperty()) {
      case BOTTOM:
        float min;
      case NEG_BOTTOM:
      case TOP:
      case NEG_TOP:
        return Integer.MAX_VALUE;
      case HEIGHT:
      case NEG_HEIGHT:
        min = panel.getBounds().getMaxHeight();
        if (min <= 0) {
          //when less than zero, use the panel's anchor element as boundary
          Element anchor = panel.getChildAnchor();
          return anchor.getClientHeight();
        }
        return (int) min;
      case LEFT:
      case NEG_LEFT:
      case RIGHT:
      case NEG_RIGHT:
        //need to do positioning instead of sizing
        return Integer.MAX_VALUE;
//        return (int)panel.getBounds().getMinWidth();
      case WIDTH:
      case NEG_WIDTH:
        min = panel.getBounds().getMaxWidth();
        if (min <= 0) {
          Element anchor = panel.getChildAnchor();
          return anchor.getClientWidth();
        }
        return (int) min;
      default:
        throw new RuntimeException("Can't get here");
      }
//      if ( getResizeProperty().isVertical()) {
//        float h = panel.getBounds().getMaxHeight();
//        if (h <= 0) {
//          //when less than zero, use the panel's anchor element as boundary
//          Element anchor = panel.getParentAnchor();
////          Log.info(getClass(), "MAX H "+anchor.getBoundingClientRect().getHeight());
//          Log.info(getClass(), "MAX W "+(int)anchor.getBoundingClientRect().getHeight());
//          return (int)anchor.getBoundingClientRect().getHeight();
//        }
////        Log.info(getClass(), "Max H "+h);
//        return (int) h;
//      }else {
//        float w = panel.getBounds().getMaxWidth();
//        if (w <= 0) {
//          //when less than zero, use the panel's anchor element as boundary
//          Element anchor = panel.getParentAnchor();
//          Log.info(getClass(), "MAX W "+anchor.getClientWidth());
//          return (int)anchor.getBoundingClientRect().getWidth();
////          return anchor.getClientWidth();
//        }
//        Log.info(getClass(), "Max W "+w);
//        return (int) w;
//      }
    }

    @Override
    public int getPropertyMinValue() {
      switch (getResizeProperty()) {
      case BOTTOM:
        float min;
      case NEG_BOTTOM:
      case TOP:
      case NEG_TOP:
        return Integer.MIN_VALUE;
      case HEIGHT:
      case NEG_HEIGHT:
        min = panel.getBounds().getMinHeight();
        if (min <= 0) {
          //when less than zero, use the panel's anchor element as boundary
          Element anchor = panel.getChildAnchor();
          return anchor.getClientHeight();
        }
        Log.info(getClass(), "Min value "+min);
        return (int) min;
      case LEFT:
      case NEG_LEFT:
      case RIGHT:
      case NEG_RIGHT:
        //need to do positioning instead of sizing
        return Integer.MIN_VALUE;
//        return (int)panel.getBounds().getMinWidth();
      case WIDTH:
      case NEG_WIDTH:
        min = panel.getBounds().getMinWidth();
        if (min <= 0) {
          //when less than zero, use the panel's anchor element as boundary
          Element anchor = panel.getChildAnchor();
          Log.info(getClass(), "Min value "+min);
          return anchor.getClientWidth();
        }
        return (int) min;
      default:
        throw new RuntimeException("Can't get here");
      }
    }
  }

  private final JsonArray<Element> clickTargets = JsonCollections.createArray();

  public static <Res extends Resources & ResizeController.Resources> ResizablePanel<Res> create(final Res resources, ResizeBounds bounds) {
    final View<Res> view = new View<Res>(resources);
    final ResizablePanel<Res> panel = new ResizablePanel<Res>(resources, view, bounds);
    return panel;
  }

  public Element getParentAnchor() {
    Element anchor = getView().getElement();
    Log.info(getClass(), anchor);
    return
      anchor.getParentElement() == null
      ?
        getView().getElement()
      : anchor.getParentElement()
      ;
  }
  public Element getChildAnchor() {
    return
//      getView().getElement()
      Elements.asJsElement(getView().contentContainer)
      ;
  }

  private PositionController positionController;
  private ResizeBounds bounds;
  private ScheduledCommand resize;
  private final ArrayOf<ResizeController> controllers;

  private ResizablePanel(final Res resources, final View<Res> view, ResizeBounds bounds) {
    super(view);
    setBounds(bounds);
    controllers = Collections.arrayOf();
    Scheduler.get().scheduleFinally(new ScheduledCommand() {
      @Override
      public void execute() {
          Element n = Elements.asJsElement(view.north);
          Element e = Elements.asJsElement(view.east);
          Element w = Elements.asJsElement(view.west);
          Element s = Elements.asJsElement(view.south);
          Element ne = Elements.asJsElement(view.northeast);
          Element se = Elements.asJsElement(view.southeast);
          Element nw = Elements.asJsElement(view.northwest);
          Element sw = Elements.asJsElement(view.southwest);
          Element c = Elements.asJsElement(view.root);

          ElementInfo bodyWidth = new SizeLimitingElement(ResizablePanel.this, c, ResizeProperty.WIDTH);
          ElementInfo bodyHeight = new SizeLimitingElement(ResizablePanel.this, c, ResizeProperty.HEIGHT);
          ElementInfo neg_bodyWidth = new SizeLimitingElement(ResizablePanel.this, c, ResizeProperty.NEG_WIDTH);
          ElementInfo neg_bodyHeight = new SizeLimitingElement(ResizablePanel.this, c, ResizeProperty.NEG_HEIGHT);

          ElementInfo northInfo = new SizeLimitingElement(ResizablePanel.this, c, ResizeProperty.TOP);
          ElementInfo eastInfo = new SizeLimitingElement(ResizablePanel.this, c, ResizeProperty.RIGHT);
          ElementInfo neg_eastInfo = new SizeLimitingElement(ResizablePanel.this, c, ResizeProperty.NEG_RIGHT);
          ElementInfo westInfo = new SizeLimitingElement(ResizablePanel.this, c, ResizeProperty.LEFT);

          controllers.setLength(0);

          ResizeController controller;

          controller = new ResizeController(resources, n, northInfo, neg_bodyHeight);
          controller.start();
          controllers.push(controller);

          controller = new ResizeController(resources, w, westInfo, neg_bodyWidth);
          controller.start();
          controllers.push(controller);

          controller = new ResizeController(resources, e, eastInfo, bodyWidth);
          controller.start();
          controllers.push(controller);

          controller = new ResizeController(resources, s, bodyHeight);
          controller.start();
          controllers.push(controller);

          controller = new ResizeController(resources, nw, northInfo, westInfo, neg_bodyWidth, neg_bodyHeight);
          controller.start();
          controllers.push(controller);

          controller = new ResizeController(resources, ne, northInfo, neg_eastInfo, bodyWidth, neg_bodyHeight);
          controller.start();
          controllers.push(controller);

          controller = new ResizeController(resources, sw, bodyHeight, westInfo, neg_bodyWidth);
          controller.start();
          controllers.push(controller);

          controller = new ResizeController(resources, se,neg_eastInfo, bodyHeight,  bodyWidth);
          controller.start();
          controllers.push(controller);

          controller = new ResizeController(resources, Elements.asJsElement(view.header), northInfo, westInfo) {
            @Override
            protected String getResizeCursor() {
              return resources.panelCss().draggable();
            }
          };
          controller.start();
          controllers.push(controller);

          // use the controllers array to create a deactivator

      }
    });
  }

  public void show() {
    Preconditions.checkNotNull(positionController,
      "You cannot show this popup without using a position controller");
    positionController.updateElementPosition();
  }

  /**
   * Shows the panel anchored to a given element.
   */
  public void show(Positioner positioner) {
    positionController = new PositionController(positioner, getView().getElement());
    show();
    scheduleResize();
  }

  private void scheduleResize() {
    if (resize == null) {
      resize = new ScheduledCommand() {
        @Override
        public void execute() {
          resize = null;
          doResize();
        }
      };
      Scheduler.get().scheduleFinally(resize);
    }
  }

  protected void doResize() {
    for (int i = 0; i < controllers.length(); i++) {
      for (ElementInfo info : controllers.get(i).getElementInfos()){
        info.refresh();
      }
    }
  }

  /**
   * Sets the popup's content element.
   *
   * @param contentElement the DOM element to show in the popup, or {@code null} to clean up the popup's DOM
   */
  public void setContentElement(@Nullable Element contentElement) {
    getView().setContentElement(contentElement);
    // refresh minimum sizing
    scheduleResize();
  }

  public void setHeaderElement(@Nullable Element headerElement) {
    getView().setHeaderElement(headerElement);
    scheduleResize();
  }

  public void setHeaderContent(@Nonnull String headerContent) {
    Preconditions.checkNotNull(headerContent, "You may not send null strings as panel headers. " +
    		"Use new String(0xa) for a non-breaking space.");
    getView().setHeaderElement(renderHeader(getView().css, headerContent));
  }

  protected Element renderHeader(Css res, String headerContent) {
    elemental.html.DivElement el = Elements.createDivElement(res.headerText());
    el.setInnerHTML(headerContent);
    return el;
  }

  public void destroy() {
    forceHide();
    setContentElement(null);
    positionController = null;
  }

  /**
   * Add one or more partner elements that, when the panel is dragged, are resized and moved relative to the
   * panel's delta.
   */
  public void addPartnerDragElements(Element ... elems) {
    if (elems != null) {
      for (Element e : elems) {
        clickTargets.add(e);
      }
    }
  }

  public void removePartnerDragElements(Element ... elems) {
    if (elems != null) {
      for (Element e : elems) {
        clickTargets.remove(e);
      }
    }
  }

  private void forceHide() {

  }

  public void setPosition(int left, int top) {
    CSSStyleDeclaration style = getView().getElement().getStyle();
    if (left < 0) {
      // set right
      style.clearLeft();
      style.setRight(-left, "px");
    } else {
      style.clearRight();
      style.setLeft(left, "px");
    }
    if (top < 0) {
      style.clearTop();
      style.setBottom(-top, "px");
    } else {
      style.clearBottom();
      style.setTop(top, "px");
    }
  }

  public void setAllHeader() {
    getView().setHeaderElement(Elements.asJsElement(getView().contentContainer));
  }

  /**
   * @return the bounds
   */
  public ResizeBounds getBounds() {
    return bounds;
  }

  /**
   * @param bounds the bounds to set
   */
  public void setBounds(ResizeBounds bounds) {
    //TODO: resize if and only if bounds changed
    this.bounds = bounds;
  }

}
