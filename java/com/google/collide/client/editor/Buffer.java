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

package com.google.collide.client.editor;

import collide.client.common.CommonResources;
import collide.client.common.Constants;
import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.document.linedimensions.LineDimensionsCalculator;
import com.google.collide.client.document.linedimensions.LineDimensionsCalculator.RoundingStrategy;
import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.client.util.Executor;
import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.client.util.dom.DomUtils.Offset;
import com.google.collide.client.util.dom.FontDimensionsCalculator.FontDimensions;
import com.google.collide.client.util.dom.MouseGestureListener;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Document.LineCountListener;
import com.google.collide.shared.document.Document.LineListener;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.ReadOnlyAnchor;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;
import com.google.collide.shared.util.TextUtils;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;
import elemental.events.MouseEvent;
import elemental.html.ClientRect;
import elemental.html.DivElement;

/*
 * TODO: Buffer has turned into an EditorSurface, but is still
 * called Buffer.
 */
/**
 * The presenter for the text portion of the editor. This class is used to
 * display text to the user, and to accept mouse input from the user.
 *
 * The lifecycle of this class is tied to the {@link Editor} that owns it.
 */
public class Buffer extends UiComponent<Buffer.View>
    implements LineListener, LineCountListener, CoordinateMap.DocumentSizeProvider {

  private static final int MARKER_COLUMN = 100;

  /**
   * Static factory method for obtaining an instance of Buffer.
   */
  public static Buffer create(Buffer.Resources resources, FontDimensions fontDimensions,
      LineDimensionsCalculator lineDimensions, Executor renderTimeExecutor) {
    View view = new View(resources);
    Buffer buffer = new Buffer(view, fontDimensions, lineDimensions, renderTimeExecutor);
    MouseWheelRedirector.redirect(buffer, view.scrollableElement);
    return buffer;
  }

  /**
   * CssResource for the editor.
   */
  public interface Css extends Editor.EditorSharedCss {
    String editorLineHeight();

    String line();

    String scrollbar();

    int scrollableLeftPadding();

    String spacer();

    String textLayer();

    String root();

    String columnMarkerLine();
  }

  /**
   * Listener that is notified of multiple click and drag mouse actions in the
   * text buffer area of the editor.
   */
  public interface MouseDragListener {
    void onMouseClick(Buffer buffer, int clickCount, int x, int y, boolean isShiftHeld);

    void onMouseDrag(Buffer buffer, int x, int y);

    void onMouseDragRelease(Buffer buffer, int x, int y);
  }

  /*
   * TODO: listeners probably also want to be notified when the
   * mouse leaves the buffer
   */
  /**
   * Listener that is notified of mouse movements in the buffer.
   *
   * <p>You probably want to use {@link MouseHoverManager} instead.
   *
   * TODO: Make it package-private.
   */
  public interface MouseMoveListener {
    void onMouseMove(int x, int y);
  }

  /**
   * Listener that is notified of mouse movements out of the buffer.
   */
  interface MouseOutListener {
    void onMouseOut();
  }

  /**
   * Listener that is called when there is a click anywhere in the editor.
   */
  public interface MouseClickListener {
    void onMouseClick(int x, int y);
  }

  /**
   * Listener that is called when the buffer's height changes.
   */
  public interface HeightListener {
    void onHeightChanged(int height);
  }

  /**
   * ClientBundle for the editor.
   */
  public interface Resources extends CommonResources.BaseResources {
    @Source({"Buffer.css", "constants.css", "collide/client/common/constants.css"})

    Css workspaceEditorBufferCss();
  }

  /**
   * Listener that is notified of scroll events.
   */
  public interface ScrollListener {
    void onScroll(Buffer buffer, int scrollTop);
  }

  /**
   * Listener that is notified of window resize events.=
   */
  public interface ResizeListener {
    void onResize(Buffer buffer, int documentHeight, int viewportHeight, int scrollTop);
  }

  /**
   * Listen for spacers being added or removed.
   */
  public interface SpacerListener {
    void onSpacerAdded(Spacer spacer);

    void onSpacerHeightChanged(Spacer spacer, int oldHeight);

    void onSpacerRemoved(Spacer spacer, Line oldLine, int oldLineNumber);
  }

  /**
   * View for the buffer.
   */
  public static class View extends CompositeView<ViewEvents> {
    private final Css css;

    private final EventListener mouseMoveListener = new EventListener() {
      @Override
      public void handleEvent(Event event) {
        int eventOffsetX = DomUtils.getOffsetX((MouseEvent) event);
        int eventOffsetY = DomUtils.getOffsetY((MouseEvent) event);

        Offset targetOffsetInBuffer =
            DomUtils.calculateElementOffset((Element) event.getTarget(), textLayerElement, true);
        getDelegate().onMouseMove(
            targetOffsetInBuffer.left + eventOffsetX, targetOffsetInBuffer.top + eventOffsetY);
      }
    };

    private final EventListener mouseOutListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        /*
         * Check if we really should handle this event:
         * For mouseout, there are two situations:
         * 1. If relatedTarget is defined, then it (the DOM node to which the
         *    mouse moves) should NOT be inside the buffer element itself.
         * 2. User leaves the window using a keyboard command or something else.
         *    In this case, relatedTarget is undefined.
         */
        com.google.gwt.user.client.Event gwtEvent = (com.google.gwt.user.client.Event) evt;
        Element relatedTarget = (Element) gwtEvent.getRelatedEventTarget();
        if (relatedTarget == null || !scrollableElement.contains(relatedTarget)) {
          getDelegate().onMouseOut();
        }
      }
    };

    private EventRemover mouseMoveListenerRemover;
    private EventRemover mouseOutListenerRemover;

    private final Element rootElement;
    private final Element scrollbarElement;
    private final Element scrollableElement;
    private final Element textLayerElement;
    private final Element columnMarkerElement;

    private int scrollTopFromPreviousDispatch;

    private View(Resources res) {
      this.css = res.workspaceEditorBufferCss();

      columnMarkerElement = Elements.createDivElement(css.columnMarkerLine());
      textLayerElement = Elements.createDivElement(css.textLayer());

      scrollableElement = createScrollableElement(res.baseCss());
      if (false) {
        /*
         * TODO: Re-enable post-v1 when we have a settings page to configure the
         * placement of this marker
         */
        // Note: columnMarkerElement is lying under the textLayerElement,
        //       so spacers are not shadowed.
        scrollableElement.appendChild(columnMarkerElement);
      }
      scrollableElement.appendChild(textLayerElement);

      scrollbarElement = createScrollbarElement(res.baseCss());

      rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(scrollableElement);
      rootElement.appendChild(scrollbarElement);
      setElement(rootElement);
    }

    private Element createScrollbarElement(CommonResources.BaseCss baseCss) {
      final DivElement scrollbarElement = Elements.createDivElement(css.scrollbar());
      scrollbarElement.addClassName(baseCss.documentScrollable());

      scrollbarElement.addEventListener(Event.SCROLL, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          setScrollTop(scrollbarElement.getScrollTop(), false);
        }
      }, false);

      // Prevent stealing focus from scrollable.
      scrollbarElement.addEventListener(Event.MOUSEDOWN, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          evt.preventDefault();
        }
      }, false);

      // Empty child will be set to the document height
      scrollbarElement.appendChild(Elements.createDivElement());

      return scrollbarElement;
    }

    private Element createScrollableElement(CommonResources.BaseCss baseCss) {
      final DivElement scrollableElement = Elements.createDivElement(css.scrollable());
      scrollableElement.addClassName(baseCss.documentScrollable());

      scrollableElement.addEventListener(Event.SCROLL, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          setScrollTop(scrollableElement.getScrollTop(), false);
        }
      }, false);

      scrollableElement.addEventListener(Event.CONTEXTMENU, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          /*
           * TODO: eventually have our context menu, but for now
           * disallow browser's since it's confusing that it does not have copy
           * nor paste options
           */
          evt.stopPropagation();
          evt.preventDefault();
        }
      }, false);

      // TODO: Detach listener in appropriate moment.
      MouseGestureListener.createAndAttach(scrollableElement, new MouseGestureListener.Callback() {
        @Override
        public boolean onClick(int clickCount, MouseEvent event) {
          // The buffer area does not include the scrollable's padding
          int bufferClientLeft = css.scrollableLeftPadding();
          int bufferClientTop = 0;
          for (Element element = scrollableElement; element.getOffsetParent() != null;
              element = element.getOffsetParent()) {
            bufferClientLeft += element.getOffsetLeft();
            bufferClientTop += element.getOffsetTop();
          }

          /*
           * This onClick method will get called for horizontal scrollbar interactions. We want to
           * exit early for those. It will not get called for vertical scrollbar interactions since
           * that is a separate element outside of the scrollable element.
           */
          if (scrollableElement == event.getTarget()) {
            // Test if the mouse event is on the horizontal scrollbar.
            int relativeY = event.getClientY() - bufferClientTop;
            if (relativeY > scrollableElement.getClientHeight()) {
              // Prevent editor losing focus
              event.preventDefault();
              return false;
            }
          }

          getDelegate().onMouseClick(clickCount,
              event.getClientX(),
              event.getClientY(),
              bufferClientLeft,
              bufferClientTop,
              event.isShiftKey());

          return true;
        }

        @Override
        public void onDragRelease(MouseEvent event) {
          getDelegate().onMouseDragRelease(event.getClientX(), event.getClientY());
        }

        @Override
        public void onDrag(MouseEvent event) {
          getDelegate().onMouseDrag(event.getClientX(), event.getClientY());
        }
      });

      /*
       * Don't allow tabbing to this -- the input element will be tabbable
       * instead
       */
      scrollableElement.setTabIndex(-1);

      Browser.getWindow().addEventListener(Event.RESIZE, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          // TODO: also listen for the navigation slider
          // this event is being caught multiple times, and sometimes the
          // calculated values are all zero. So only respond if we have positive
          // values.
          int height = (int) textLayerElement.getBoundingClientRect().getHeight();
          int viewportHeight = getHeight();
          if (height > 0 && viewportHeight > 0) {
            getDelegate().onScrollableResize(
                height, viewportHeight, scrollableElement.getScrollTop());
          }
        }
      }, false);

      return scrollableElement;
    }

    public int getScrollLeft() {
      return scrollableElement.getScrollLeft();
    }

    private void addLine(Element lineElement) {
      String className = lineElement.getClassName();
      if (className == null || className.isEmpty()) {
        lineElement.addClassName(css.line());
      }

      textLayerElement.appendChild(lineElement);
    }

    private Element getFirstLine() {
      return textLayerElement.getFirstChildElement();
    }

    private int getHeight() {
      return scrollableElement.getClientHeight();
    }

    private int getScrollTop() {
      return scrollableElement.getScrollTop();
    }

    private int getScrollHeight() {
      return scrollableElement.getScrollHeight();
    }

    private int getWidth() {
      return scrollableElement.getClientWidth();
    }

    private void reset() {
      scrollableElement.setScrollTop(0);
      scrollTopFromPreviousDispatch = 0;

      textLayerElement.setInnerHTML("");
    }

    private void setBufferHeight(int height) {
      textLayerElement.getStyle().setHeight(height, CSSStyleDeclaration.Unit.PX);
      columnMarkerElement.getStyle().setHeight(height, CSSStyleDeclaration.Unit.PX);

      /*
       * For expediency, we deviate from typical scrollbar behavior by having
       * the vertical scrollbar span the entire height, even in the presence of
       * a horizontal scrollbar in the scrollable element. The problem is that
       * if the scrollable element is scrolled all the way to its bottom, its
       * scrolltop will be SCROLLBAR_SIZE greater than what the
       * scrollbarElement's scrolltop is when scrollbarElement's scrolled all
       * the way to the bottom (because scrollTop + clientHeight cannot be
       * greater than scrollHeight). We get around this problem by adding the
       * SCROLLBAR_SIZE to the scrollbar element's height, which allows the
       * scrollTops on both elements to be the same. (There's now a little room
       * at the bottom of the vertical scrollbar that won't scroll the
       * scrollable element, but that's OK.)
       */
      scrollbarElement.getFirstChildElement().getStyle()
          .setHeight(height + Constants.SCROLLBAR_SIZE, CSSStyleDeclaration.Unit.PX);
    }

    public void setWidth(int width) {
      textLayerElement.getStyle().setWidth(width, CSSStyleDeclaration.Unit.PX);
    }

    private void setScrollTop(int scrollTop, boolean forceDispatch) {
      if (scrollTop != scrollableElement.getScrollTop()) {
        scrollableElement.setScrollTop(scrollTop);
      }

      if (scrollTop != scrollbarElement.getScrollTop()) {
        scrollbarElement.setScrollTop(scrollTop);
      }

      if (scrollTop != scrollTopFromPreviousDispatch) {
        // Use getScrollTop in case the desired scrollTop could not be set
        int newScrollTop = scrollableElement.getScrollTop();
        getDelegate().onScroll(newScrollTop);
        scrollTopFromPreviousDispatch = newScrollTop;
      }
    }

    public void setScrollLeft(int scrollLeft) {
      scrollableElement.setScrollLeft(scrollLeft);
    }

    void registerMouseMoveListener() {
      if (mouseMoveListenerRemover == null) {
        mouseMoveListenerRemover =
            scrollableElement.addEventListener(Event.MOUSEMOVE, mouseMoveListener, false);
      }
    }

    void unregisterMouseMoveListener() {
      if (mouseMoveListenerRemover != null) {
        mouseMoveListenerRemover.remove();
        mouseMoveListenerRemover = null;
      }
    }

    void registerMouseOutListener() {
      if (mouseOutListenerRemover == null) {
        mouseOutListenerRemover =
            scrollableElement.addEventListener(Event.MOUSEOUT, mouseOutListener, false);
      }
    }

    void unregisterMouseOutListener() {
      if (mouseOutListenerRemover != null) {
        mouseOutListenerRemover.remove();
        mouseOutListenerRemover = null;
      }
    }
  }

  private interface ViewEvents {
    void onMouseClick(int clickCount,
        int clientX,
        int clientY,
        int bufferClientLeft,
        int bufferClientTop,
        boolean isShiftHeld);

    void onMouseDrag(int clientX, int clientY);

    void onMouseDragRelease(int clientX, int clientY);

    void onMouseMove(int bufferX, int bufferY);

    void onMouseOut();

    void onScroll(int scrollTop);

    void onScrollableResize(int height, int viewportHeight, int scrollTop);
  }

  private final CoordinateMap coordinateMap;
  private final ElementManager elementManager;
  private final ListenerManager<HeightListener> heightListenerManager =
      ListenerManager.create();
  private int maxLineLength;
  private final ListenerManager<MouseClickListener> mouseClickListenerManager =
      ListenerManager.create();
  private final ListenerManager<MouseDragListener> mouseDragListenerManager =
      ListenerManager.create();

  private final ListenerManager<MouseMoveListener> mouseMoveListenerManager =
      ListenerManager.create(new ListenerManager.RegistrationListener<MouseMoveListener>() {
            @Override
            public void onListenerAdded(MouseMoveListener listener) {
              if (mouseMoveListenerManager.getCount() == 1) {
                getView().registerMouseMoveListener();
              }
            }

            @Override
            public void onListenerRemoved(MouseMoveListener listener) {
              if (mouseMoveListenerManager.getCount() == 0) {
                getView().unregisterMouseMoveListener();
              }
            }
          });

  private final ListenerManager<MouseOutListener> mouseOutListenerManager =
      ListenerManager.create(new ListenerManager.RegistrationListener<MouseOutListener>() {
            @Override
            public void onListenerAdded(MouseOutListener listener) {
              if (mouseOutListenerManager.getCount() == 1) {
                getView().registerMouseOutListener();
              }
            }

            @Override
            public void onListenerRemoved(MouseOutListener listener) {
              if (mouseOutListenerManager.getCount() == 0) {
                getView().unregisterMouseOutListener();
              }
            }
          });

  private static final Dispatcher<MouseOutListener> mouseOutListenerDispatcher =
      new Dispatcher<MouseOutListener>() {
        @Override
        public void dispatch(MouseOutListener listener) {
          listener.onMouseOut();
        }
      };

  private final ListenerManager<SpacerListener> spacerListenerManager = ListenerManager.create();
  private Document document;
  private final int editorLineHeight;
  private final ListenerManager<ScrollListener> scrollListenerManager = ListenerManager.create();
  private final ListenerManager<ResizeListener> resizeListenerManager = ListenerManager.create();
  private final FontDimensions fontDimensions;
  private final LineDimensionsCalculator lineDimensions;
  private final RemoverManager documentChangedRemoverManager = new RemoverManager();
  private final Executor renderTimeExecutor;

  private Buffer(View view, FontDimensions fontDimensions, LineDimensionsCalculator lineDimensions,
      Executor renderTimeExecutor) {
    super(view);

    this.fontDimensions = fontDimensions;
    this.lineDimensions = lineDimensions;
    this.renderTimeExecutor = renderTimeExecutor;
    this.editorLineHeight = CssUtils.parsePixels(view.css.editorLineHeight());

    coordinateMap = new CoordinateMap(this);
    elementManager = new ElementManager(getView().textLayerElement, this);

    updateColumnMarkerPosition();

    view.setDelegate(new ViewEvents() {
      private int bufferLeft;
      private int bufferTop;
      private int bufferRelativeX;
      private int bufferRelativeY;

      @Override
      public void onMouseClick(final int clickCount,
          int clientX,
          int clientY,
          int bufferClientLeft,
          int bufferClientTop,
          final boolean isShiftHeld) {

        this.bufferLeft = bufferClientLeft;
        this.bufferTop = bufferClientTop;
        updateBufferRelativeXy(clientX, clientY);

        if (clickCount == 1) {
          // Dispatch to simple click listeners
          mouseClickListenerManager.dispatch(new Dispatcher<MouseClickListener>() {
            @Override
            public void dispatch(MouseClickListener listener) {
              listener.onMouseClick(bufferRelativeX, bufferRelativeY);
            }
          });
        }

        mouseDragListenerManager.dispatch(new Dispatcher<MouseDragListener>() {
          @Override
          public void dispatch(MouseDragListener listener) {
            listener.onMouseClick(
                Buffer.this, clickCount, bufferRelativeX, bufferRelativeY, isShiftHeld);
          }
        });
      }

      @Override
      public void onMouseDrag(final int clientX, final int clientY) {
        updateBufferRelativeXy(clientX, clientY);
        mouseDragListenerManager.dispatch(new Dispatcher<MouseDragListener>() {
          @Override
          public void dispatch(MouseDragListener listener) {
            listener.onMouseDrag(Buffer.this, bufferRelativeX, bufferRelativeY);
          }
        });
      }

      @Override
      public void onMouseDragRelease(final int clientX, final int clientY) {
        updateBufferRelativeXy(clientX, clientY);
        mouseDragListenerManager.dispatch(new Dispatcher<MouseDragListener>() {
          @Override
          public void dispatch(MouseDragListener listener) {
            listener.onMouseDragRelease(Buffer.this, bufferRelativeX, bufferRelativeY);
          }
        });
      }

      @Override
      public void onMouseMove(final int bufferX, final int bufferY) {
        mouseMoveListenerManager.dispatch(new Dispatcher<MouseMoveListener>() {
          @Override
          public void dispatch(MouseMoveListener listener) {
            listener.onMouseMove(bufferX, bufferY);
          }
        });
      }

      @Override
      public void onMouseOut() {
        mouseOutListenerManager.dispatch(mouseOutListenerDispatcher);
      }

      private void updateBufferRelativeXy(int clientX, int clientY) {
        /*
         * TODO: consider moving this element top/left-relative
         * code to MouseGestureListener
         */
        bufferRelativeX = clientX - bufferLeft + getScrollLeft();
        bufferRelativeY = clientY - bufferTop + getScrollTop();
      }

      @Override
      public void onScroll(final int scrollTop) {
        scrollListenerManager.dispatch(new Dispatcher<ScrollListener>() {
          @Override
          public void dispatch(ScrollListener listener) {
            listener.onScroll(Buffer.this, scrollTop);
          }
        });
      }

      @Override
      public void onScrollableResize(
          final int height, final int viewportHeight, final int scrollTop) {
        // TODO: Look into why this is necessary.
        updateTextWidth();
        updateVerticalScrollbarDisplayVisibility();
        updateColumnMarkerHeight();
        resizeListenerManager.dispatch(new Dispatcher<ResizeListener>() {
          @Override
          public void dispatch(ResizeListener listener) {
            listener.onResize(Buffer.this, height, viewportHeight, scrollTop);
          }
        });
      }
    });
  }

  public void addLineElement(Element lineElement) {
    getView().addLine(lineElement);
  }

  public boolean hasLineElement(Element lineElement) {
    return lineElement.getParentElement() != null;
  }

  /*
   * TODO: consider making ElementManager public, and a
   * getElementManager() method instead
   */
  public void addAnchoredElement(ReadOnlyAnchor anchor, Element element) {
    elementManager.addAnchoredElement(anchor, element);
  }

  public void removeAnchoredElement(ReadOnlyAnchor anchor, Element element) {
    elementManager.removeAnchoredElement(anchor, element);
  }

  public void addUnmanagedElement(Element element) {
    elementManager.addUnmanagedElement(element);
  }

  public void removeUnmanagedElement(Element element) {
    elementManager.removeUnmanagedElement(element);
  }

  /**
   * Returns a newly added spacer above the given {@code lineInfo} with the
   * given {@code height}.
   */
  public Spacer addSpacer(LineInfo lineInfo, int height) {
    final Spacer createdSpacer =
        coordinateMap.createSpacer(lineInfo, height, this, getView().css.spacer());
    updateBufferHeightAndMaybeScrollTop(calculateSpacerTop(createdSpacer), height);
    spacerListenerManager.dispatch(new Dispatcher<Buffer.SpacerListener>() {
      @Override
      public void dispatch(SpacerListener listener) {
        listener.onSpacerAdded(createdSpacer);
      }
    });
    return createdSpacer;
  }

  public void removeSpacer(final Spacer spacer) {
    final Line spacerLine = spacer.getLine();
    final int spacerLineNumber = spacer.getLineNumber();
    if (coordinateMap.removeSpacer(spacer)) {
      updateBufferHeightAndMaybeScrollTop(convertLineNumberToY(spacerLineNumber),
          -spacer.getHeight());
      spacerListenerManager.dispatch(new Dispatcher<Buffer.SpacerListener>() {
        @Override
        public void dispatch(SpacerListener listener) {
          listener.onSpacerRemoved(spacer, spacerLine, spacerLineNumber);
        }
      });
    }
  }

  public boolean hasSpacers() {
    return coordinateMap.getTotalSpacerHeight() != 0;
  }

  @Override
  public void handleSpacerHeightChanged(final Spacer spacer, final int oldHeight) {
    int deltaHeight = spacer.getHeight() - oldHeight;
    updateBufferHeightAndMaybeScrollTop(calculateSpacerTop(spacer), deltaHeight);
    spacerListenerManager.dispatch(new Dispatcher<Buffer.SpacerListener>() {
      @Override
      public void dispatch(SpacerListener listener) {
        listener.onSpacerHeightChanged(spacer, oldHeight);
      }
    });
  }

  public int calculateSpacerTop(Spacer spacer) {
    return coordinateMap.convertLineNumberToY(spacer.getLineNumber()) - spacer.getHeight();
  }

  /**
   * @param inDocumentRange whether to do a validation check on the return line
   *        number to ensure it is in the document's range
   */
  public int convertYToLineNumber(int y, boolean inDocumentRange) {
    int lineNumber = coordinateMap.convertYToLineNumber(y);
    return inDocumentRange ? LineUtils.getValidLineNumber(lineNumber, document) : lineNumber;
  }

  public int convertXToRoundedVisibleColumn(int x, Line line) {
    int roundedColumn = convertXToColumn(x, line, RoundingStrategy.ROUND);
    return TextUtils.findNextCharacterInclusive(line.getText(), roundedColumn);
  }

  public int convertXToColumn(int x, Line line, RoundingStrategy roundingStrategy) {
    return LineUtils.rubberbandColumn(
        line, lineDimensions.convertXToColumn(line, x, roundingStrategy));
  }

  public ListenerRegistrar<HeightListener> getHeightListenerRegistrar() {
    return heightListenerManager;
  }

  /**
   * Returns the top for a line, e.g. if {@code lineNumber} is 0 and it is a
   * simple document, 0 will be returned.
   */
  public int convertLineNumberToY(int lineNumber) {
    return coordinateMap.convertLineNumberToY(lineNumber);
  }

  public int convertColumnToX(Line line, int column) {
    return (int) Math.floor(lineDimensions.convertColumnToX(line, column));
  }

  public int calculateColumnLeftRelativeToScrollableIgnoringSpecialCharacters(int column) {
    return calculateColumnLeftIgnoringSpecialCharacters(column)
        + getView().css.scrollableLeftPadding();
  }

  /**
   * Converts a column to an x value assuming all characters in-between are
   * number width.
   * <p>
   * DO NOT USE THIS UNLESS IT IS INTENTIONAL AND YOU UNDERSTAND THE
   * CONSEQUENCES. DO NOT USE IT JUST BECAUSE IT DOES NOT REQUIRE A
   * {@link Line}.
   */
  public int calculateColumnLeftIgnoringSpecialCharacters(int column) {
    return (int) Math.floor(fontDimensions.getCharacterWidth() * column);
  }

  public int calculateColumnLeft(Line line, int column) {
    return Math.max(0, convertColumnToX(line, column));
  }

  public int calculateLineBottom(int lineNumber) {
    return convertLineNumberToY(lineNumber) + editorLineHeight;
  }

  public int calculateLineTop(int lineNumber) {
    return convertLineNumberToY(lineNumber);
  }

  public ListenerRegistrar<MouseClickListener> getMouseClickListenerRegistrar() {
    return mouseClickListenerManager;
  }

  public ListenerRegistrar<SpacerListener> getSpacerListenerRegistrar() {
    return spacerListenerManager;
  }

  public Document getDocument() {
    return document;
  }

  @Override
  public float getEditorCharacterWidth() {
    return fontDimensions.getCharacterWidth();
  }

  /**
   * TODO: So right now this uses a constant, unfortunately it's not
   * accurate when zoomed in/out and sometimes leads to whitespace between
   * selection lines. This should be converted to fontDimensions.getHeight().
   */
  @Override
  public int getEditorLineHeight() {
    return editorLineHeight;
  }

  public Element getFirstLineElement() {
    return getView().getFirstLine();
  }

  public int getFlooredHeightInLines() {
    /*
     * Imagine scrollTop = 0, lineHeight = 10, and height = 20. If we passed
     * "true", convertYToLineNumber(0+20) would bound on the document size and
     * return 1 instead of the 2 that we need.
     */
    return convertYToLineNumber(getScrollTop() + getHeight(), false)
        - convertYToLineNumber(getScrollTop(), false);
  }

  public int getHeight() {
    return getView().getHeight();
  }

  public ListenerRegistrar<MouseDragListener> getMouseDragListenerRegistrar() {
    return mouseDragListenerManager;
  }

  // TODO: Make it package-private.
  public ListenerRegistrar<MouseMoveListener> getMouseMoveListenerRegistrar() {
    return mouseMoveListenerManager;
  }

  ListenerRegistrar<MouseOutListener> getMouseOutListenerRegistrar() {
    return mouseOutListenerManager;
  }

  public int getMaxLineLength() {
    return maxLineLength;
  }

  public int getScrollLeft() {
    return getView().getScrollLeft();
  }

  public ListenerRegistrar<ScrollListener> getScrollListenerRegistrar() {
    return scrollListenerManager;
  }

  public ListenerRegistrar<ResizeListener> getResizeListenerRegistrar() {
    return resizeListenerManager;
  }

  public int getScrollTop() {
    return getView().getScrollTop();
  }

  public int getScrollHeight() {
    return getView().getScrollHeight();
  }

  public int getWidth() {
    return getView().getWidth();
  }

  public void handleDocumentChanged(Document newDocument) {
    documentChangedRemoverManager.remove();

    document = newDocument;
    coordinateMap.handleDocumentChange(newDocument);
    lineDimensions.handleDocumentChange(newDocument);

    getView().reset();

    documentChangedRemoverManager.track(newDocument.getLineListenerRegistrar().add(this));
    updateBufferHeight();
  }

  @Override
  public void onLineAdded(Document document, final int lineNumber,
      final JsonArray<Line> addedLines) {
    renderTimeExecutor.execute(new Runnable() {
      @Override
      public void run() {
        updateBufferHeightAndMaybeScrollTop(convertLineNumberToY(lineNumber), addedLines.size()
            * getEditorLineHeight());
      }
    });
  }

  @Override
  public void onLineRemoved(final Document document, final int lineNumber,
      final JsonArray<Line> removedLines) {
    renderTimeExecutor.execute(new Runnable() {
      @Override
      public void run() {
        /*
         * Since the removed line(s) no longer exist, we need to make sure to
         * clamp them
         */
        int safeLineNumber =
            Math.min(document.getLastLineNumber(), lineNumber);
        updateBufferHeightAndMaybeScrollTop(convertLineNumberToY(safeLineNumber),
            -removedLines.size() * getEditorLineHeight());
      }
    });
  }

  @Override
  public void onLineCountChanged(Document document, int lineCount) {
    updateBufferHeight();
  }

  public void setMaxLineLength(int maxLineLength) {
    this.maxLineLength = maxLineLength;
    updateTextWidth();
  }

  private void updateTextWidth() {
    int longestLineWidth = (int) Math.floor(maxLineLength * getEditorCharacterWidth());
    getView().setWidth(longestLineWidth);
  }

  /**
   * Updates the buffer height and potentially the scroll top depending on
   * whether the change is before the scroll top or not.
   *
   * @param changeTop the top (px) where the change occurred
   * @param changeHeight the potentially negative change in height
   */
  private void updateBufferHeightAndMaybeScrollTop(int changeTop, final int changeHeight) {
    updateBufferHeight();

    /*
     * If the change is on or before the scrolled position and we don't update
     * the scroll position, the content of the viewport will be different. To
     * keep the content of the viewport the same, we adjust the scrolled
     * position.
     */
    final int scrollTop = getScrollTop();
    if (changeTop <= scrollTop) {
      setScrollTop(scrollTop + changeHeight);
    }
  }

  /**
   * Updates the buffer height to the calculated height. Most callers should use
   * {@link #updateBufferHeightAndMaybeScrollTop(int, int)}.
   */
  private void updateBufferHeight() {
    final int totalBufferHeight =
        coordinateMap.getTotalSpacerHeight() + document.getLineCount() * editorLineHeight;
    getView().setBufferHeight(totalBufferHeight);
    updateColumnMarkerHeight();
    updateVerticalScrollbarDisplayVisibility();

    heightListenerManager.dispatch(new Dispatcher<Buffer.HeightListener>() {
      @Override
      public void dispatch(HeightListener listener) {
        listener.onHeightChanged(totalBufferHeight);
      }
    });
  }

  public void setScrollLeft(int scrollLeft) {
    getView().setScrollLeft(scrollLeft);
  }

  /**
   * Sets the scroll top of the buffer. Cannot set scroll top from a scroll
   * listener it will be ignored.
   */
  public void setScrollTop(int scrollTop) {
    if (!scrollListenerManager.isDispatching()) {
      getView().setScrollTop(scrollTop, false);
    }
  }

  void handleComponentsInitialized(ViewportModel viewport, Renderer renderer) {
    elementManager.handleDocumentChanged(viewport, renderer);
  }

  public void repositionAnchoredElementsWithColumn() {
    updateColumnMarkerPosition();
    elementManager.repositionAnchoredElementsWithColumn();
  }

  private void updateColumnMarkerPosition() {
    getView().columnMarkerElement.getStyle().setLeft(
        calculateColumnLeftRelativeToScrollableIgnoringSpecialCharacters(MARKER_COLUMN),
        CSSStyleDeclaration.Unit.PX);
  }

  private void updateColumnMarkerHeight() {
    int height = getView().textLayerElement.getClientHeight();
    int limitHeight = Math.max(height, getHeight());
    getView().columnMarkerElement.getStyle().setHeight(limitHeight, CSSStyleDeclaration.Unit.PX);
  }

  public ClientRect getBoundingClientRect() {
    return getView().scrollableElement.getBoundingClientRect();
  }

  private void updateVerticalScrollbarDisplayVisibility() {
    CssUtils.setDisplayVisibility2(getView().scrollbarElement,
        getView().getScrollHeight() > getView().getHeight());
  }

  public void setColumnMarkerVisibility(boolean visible) {
    CssUtils.setDisplayVisibility2(getView().columnMarkerElement, visible);
  }

  public void setVerticalScrollbarVisibility(boolean visible) {
    if (visible) {
      if (!getView().rootElement.contains(getView().scrollbarElement)) {
        getView().rootElement.appendChild(getView().scrollbarElement);
      }
      getView().scrollableElement.getStyle().setOverflowY("auto");
    } else {
      getView().rootElement.removeChild(getView().scrollbarElement);
      getView().scrollableElement.getStyle().setOverflowY("hidden");
    }
  }

  /**
   * Ensures that the scrollTop is synchronized across all editor components. For example, this
   * should be called after the editor has been removed from the DOM and re-added.
   */
  public void synchronizeScrollTop() {
    getView().setScrollTop(getView().scrollTopFromPreviousDispatch, true);
  }
}
