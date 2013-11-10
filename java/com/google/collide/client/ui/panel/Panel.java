package com.google.collide.client.ui.panel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import collide.client.util.Elements;

import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.util.ResizeBounds;
import com.google.collide.client.util.ResizeController;
import com.google.collide.client.util.ResizeController.ElementInfo;
import com.google.collide.client.util.ResizeController.ResizeEventHandler;
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
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.util.ArrayOf;
import elemental.util.Collections;
public class Panel
<Delegate, V extends Panel.View<Delegate>>
extends UiComponent<V>
implements ContentReceiver
{

  public static interface Css extends CssResource {
    String caliper();

    String content();

    String headerText();

    String hidden();

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

    String refreshIcon();

    String buttonBar();

    String maximizeIcon();

    String closeIcon();
  }

  public interface Resources extends ResizeController.Resources {
    @Source({"collide/client/common/constants.css", "Panel.css"})
    Css panelCss();
  }

  public static interface Interpolator {
    Interpolator NO_OP = new Interpolator() {
      @Override
      public float interpolate(float value) {
        return value;
      }
    };
    float interpolate(float value);
  }

  public static class View<Delegate> extends CompositeView<Delegate> {

    @UiTemplate("Panel.ui.xml")
    interface PanelBinder extends UiBinder<com.google.gwt.dom.client.DivElement,View<?>> {
    }

    static PanelBinder binder = GWT.create(PanelBinder.class);

    @UiField(provided = true)
    protected final Css css;

    @UiField
    protected DivElement root;

    @UiField
    protected DivElement header;

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

    protected View(final Resources resources) {
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

    private Panel<?, ?> panel;

    public SizeLimitingElement(Panel<?, ?> panel, Element element, ResizeProperty property) {
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
//        Log.info(getClass(), "Min value "+min);
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
//          Log.info(getClass(), "Min value "+min);
          return anchor.getClientWidth();
        }
        return (int) min;
      default:
        throw new RuntimeException("Can't get here");
      }
    }
  }

  protected static class DefaultHeaderBuilder implements HeaderBuilder {

    elemental.html.DivElement el, buttons, close, maximize, refresh;
    @Override
    public Element buildHeader(String title, Css css, Panel<?, ?> panel) {
      PanelState state = panel.getPanelState();
      if (el == null) {
        el = Elements.createDivElement(css.headerText());
      }
      el.setInnerHTML(title);
      if (state.isClosable()) {
        makeClosable(panel, el, css);
      } else if (close != null) {
        el.removeChild(close);
        close = null;
      }

      if (state.isMaximizable()) {
        makeMaximizable(panel, el, css);
      } else if (maximize != null) {
        el.removeChild(maximize);
        maximize = null;
      }

      if (state.isRefreshable()) {
        makeRefreshable(panel, el, css);
      } else if (refresh != null) {
        el.removeChild(refresh);
        refresh = null;
      }

      return el;
    }

    protected elemental.html.DivElement getButtonBar(Css css) {
      if (buttons == null) {
        buttons = Elements.createDivElement(css.buttonBar());
        el.appendChild(buttons);
      }
      return buttons;
    }

   protected void makeRefreshable(Panel<? , ?> panel, elemental.html.DivElement el, Css css) {
     if (refresh == null) {
       refresh = Elements.createDivElement(css.refreshIcon());
       getButtonBar(css).appendChild(refresh);
     }
   }

   protected void makeMaximizable(Panel<? , ?> panel, elemental.html.DivElement el, Css css) {
     if (maximize == null) {
       maximize = Elements.createDivElement(css.maximizeIcon());
       getButtonBar(css).appendChild(maximize);
       maximize.setInnerHTML("+");
     }
   }

    protected void makeClosable(Panel<? , ?> panel, elemental.html.DivElement el, Css css) {
      if (close == null) {
        close = Elements.createDivElement(css.closeIcon());
        getButtonBar(css).appendChild(close);
        close.setInnerHTML("X");
      }
    }

  }


  private final JsonArray<Element> clickTargets = JsonCollections.createArray();

  public static <Delegate> Panel<Delegate, Panel.View<Delegate>>
  create(String id, final Resources resources, ResizeBounds bounds) {
    return create(id, resources, bounds, null);
  }
  public static <Delegate> Panel<Delegate, Panel.View<Delegate>>
  create(String id, Resources resources, ResizeBounds bounds, HeaderBuilder header) {
    final View<Delegate> view = new View<Delegate>(resources);
    return new Panel<Delegate, Panel.View<Delegate>>(id, resources, view, bounds, header);
  }

  private class ResizableController extends ResizeController{

    private String enabledClass;

    public ResizableController(Resources resources, String enabledClass, Element splitter, ElementInfo ... elementInfos) {
      super(resources, splitter, elementInfos);
      this.enabledClass = enabledClass;
    }
    @Override
    protected String initCss(ElementInfo horizEl, ElementInfo vertEl) {
      if (enabledClass==null)
        enabledClass = super.initCss(horizEl, vertEl);
      return enabledClass;
    }
    protected void superDelta(int deltaW, int deltaH) {
      super.applyDelta(deltaW, deltaH);
    }
    
    ScheduledCommand cmd;
    
    @Override
    protected void applyDelta(final int deltaW, final int deltaH) {
      if (deltaW != 0 || deltaH != 0) {
        if (cmd == null) {
          Scheduler.get().scheduleFinally((cmd = new ScheduledCommand() {
            @Override
            public void execute() {
              cmd = null;
              resizeHandler.whileDragging(
//                  isNegativeWidth() ?
                      -deltaW, 
//                      : deltaW, 
                  isNegativeHeight() ? -deltaH : deltaH, 
                  0, 0);
            }
          }));
        }
      }
      superDelta(deltaW, deltaH);
    }
    
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
  private final HeaderBuilder header;
  private ResizeController dragController;
  private PanelState panelState;
  private PanelContent panelContent;
  private final String id;
  protected Panel(String id, final Resources resources, final V view, ResizeBounds bounds, HeaderBuilder header) {
    super(view);
    this.id = id;
    this.header = header == null ? new DefaultHeaderBuilder() : header;
    panelState = new PanelState();
    setBounds(bounds);
    controllers = Collections.arrayOf();
    // Use a finally command to let us pick up changes to state before we do a layout.
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

          ElementInfo bodyWidth = new SizeLimitingElement(Panel.this, c, ResizeProperty.WIDTH);
          ElementInfo bodyHeight = new SizeLimitingElement(Panel.this, c, ResizeProperty.HEIGHT);
          bodyWidth.refresh();
          bodyHeight.refresh();
          ElementInfo neg_bodyWidth = new SizeLimitingElement(Panel.this, c, ResizeProperty.NEG_WIDTH);
          ElementInfo neg_bodyHeight = new SizeLimitingElement(Panel.this, c, ResizeProperty.NEG_HEIGHT);

          ElementInfo northInfo = new SizeLimitingElement(Panel.this, c, ResizeProperty.TOP);
          ElementInfo eastInfo = new SizeLimitingElement(Panel.this, c, ResizeProperty.RIGHT);
          ElementInfo neg_eastInfo = new SizeLimitingElement(Panel.this, c, ResizeProperty.NEG_RIGHT);
          ElementInfo westInfo = new SizeLimitingElement(Panel.this, c, ResizeProperty.LEFT);

          controllers.setLength(0);

          ResizeController controller;

          com.google.collide.client.util.ResizeController.Css css = resources.resizeControllerCss();

          controller = new ResizableController(resources, css.hSplitter(), n, northInfo, neg_bodyHeight);
          controller.start();
          controllers.push(controller);

          controller = new ResizableController(resources, css.vSplitter(), w, westInfo, neg_bodyWidth);
          controller.start();
          controllers.push(controller);

          controller = new ResizableController(resources, css.vSplitter(), e, eastInfo, bodyWidth);
          controller.start();
          controllers.push(controller);

          controller = new ResizableController(resources, css.hSplitter(), s, bodyHeight);
          controller.start();
          controllers.push(controller);

          controller = new ResizableController(resources, css.nwSplitter(), nw, northInfo, westInfo, neg_bodyWidth, neg_bodyHeight);
          controller.start();
          controllers.push(controller);

          controller = new ResizableController(resources, css.neSplitter(), ne, northInfo, neg_eastInfo, bodyWidth, neg_bodyHeight);
          controller.start();
          controllers.push(controller);

          controller = new ResizableController(resources, css.swSplitter(), sw, bodyHeight, westInfo, neg_bodyWidth);
          controller.start();
          controllers.push(controller);

          controller = new ResizableController(resources, css.seSplitter(), se,neg_eastInfo, bodyHeight,  bodyWidth);
          controller.start();
          controllers.push(controller);

          controller = new ResizableController(resources, css.moveCursor(), Elements.asJsElement(view.header), northInfo, westInfo) {
            @Override
            protected String getResizeCursor() {
              return resources.resizeControllerCss().moveCursor();
            }
            @Override
            protected String initCss(ElementInfo horizEl, ElementInfo vertEl) {
              return resources.resizeControllerCss().moveCursor();
            }
          };
          setPanelDragController(controller);
          controller.start();
          controllers.push(controller);
          
          

      }
    });
  }

  protected void setPanelDragController(ResizeController controller) {
    this.dragController = controller;
  }
  public String getId() {
    return id;
  }
  public void hide() {
    getView().getElement().addClassName(getView().css.hidden());
//    CssUtils.setDisplayVisibility2(getView().getElement(), false);
//    getView().getElement().setHidden(true);
  }
  public void show() {
    getView().getElement().removeClassName(getView().css.hidden());
//    getView().getElement().setHidden(false);
//    CssUtils.setDisplayVisibility2(getView().getElement(), true);
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
      // grab a new execution loop, in case we are called in a heavyweight task.
      // this will also prevent multiple panels from rendering at the same time.
      Scheduler.get().scheduleFinally(resize);
    }
  }

  protected void doResize() {
    if (isVisible()) {
      // If there is header text, we should redraw it.
      for (int i = 0; i < controllers.length(); i++) {
        for (ElementInfo info : controllers.get(i).getElementInfos()){
          info.refresh();
        }
      }
      if (!getPanelState().isAllHeader()) {
        setHeaderContent(getPanelState().getTitle());
      }
    }
  }

  private boolean isVisible() {
    return !getView().getElement().hasClassName(getView().css.hidden());
  }
  /**
   * Sets the popup's panelContent element.
   *
   * @param panelContent the panelContent to display
   */
  public void setContent(@Nullable PanelContent panelContent) {
    if (panelContent == null) {
      this.panelContent = null;
      return;
    }
    if (this.panelContent != panelContent) {
      onContentClosed(this.panelContent);
    }
    this.panelContent = panelContent;
    getView().setContentElement(panelContent.getContentElement());
    // refresh minimum sizing
    scheduleResize();
  }

  private void onContentClosed(PanelContent panelContent2) {

  }
  public void setHeaderElement(@Nullable Element headerElement) {
    getView().setHeaderElement(headerElement);
    getPanelState().setTitle(null);
    scheduleResize();
  }

  public void setHeaderContent(@Nonnull String headerContent) {
    Preconditions.checkNotNull(headerContent, "You may not send null strings as panel headers. " +
    		"Use new String(0xa) for a non-breaking space.");

    try {
      getView().setHeaderElement(
        renderHeader(getView().css, headerContent)
        )
        ;
    } finally {
      // We want to update the state title, but not before the builder gets a
      // chance to look at the stale data, to avoid unneeded repaints.
      getPanelState().setTitle(headerContent);
    }
  }

  protected Element renderHeader(Css res, String headerContent) {
    return header.buildHeader(headerContent, getView().css, this);
  }

  public void destroy() {
    Log.info(getClass(), "Destroying panel");
    forceHide();
    setContent(null);
    positionController = null;
    resize = null;
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

  public void setPosition(float left, float top) {
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
  public void setSize(float width, float height) {
    CSSStyleDeclaration style = getView().getElement().getStyle();
    if (width < 0) {
      // set right
      style.clearWidth();
    } else {
      style.setWidth(width, "px");
    }
    if (height < 0) {
      style.clearHeight();
    } else {
      style.setHeight(height, "px");
    }
  }

  public void setAllHeader() {
    getPanelState().setAllHeader(true);
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
    this.bounds = bounds;
  }
//  @Override
//  public Element getContentElement() {
//    return getView().getElement();
//  }

  public PanelContent getContent() {
    return panelContent;
  }

//  @Override
//  public void onContentDisplayed() {
//    //hookup listeners
//  }
//  @Override
//  public void onContentDestroyed() {
//    //destroy listeners
//
//  }

  ResizeEventHandler resizeHandler;
  public void addResizeHandler(ResizeEventHandler resizeEventHandler) {
    if (resizeHandler != null) {
      dragController.removeEventHandler(resizeHandler);
    }
    resizeHandler = resizeEventHandler;
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        dragController.addEventHandler(resizeHandler);
      }
    });
    }

  protected PanelState getPanelState() {
    return panelState;
  }
  public void setClosable(boolean b) {
    getPanelState().setClosable(true);
    scheduleResize();
  }
  public boolean isHidden() {
    return getView().getElement().hasClassName(getView().css.hidden());
  }

}
