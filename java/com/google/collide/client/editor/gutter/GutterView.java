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

package com.google.collide.client.editor.gutter;

import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.gutter.Gutter.Position;
import com.google.collide.client.editor.gutter.Gutter.ViewDelegate;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.client.util.dom.MouseGestureListener;
import com.google.collide.client.util.dom.ScrollbarSizeCalculator;
import com.google.collide.mvp.CompositeView;

import elemental.css.CSSStyleDeclaration;
import elemental.events.MouseEvent;
import elemental.html.Element;

/**
 * The view component of the MVP stack for a gutter.
 *
 */
class GutterView extends CompositeView<ViewDelegate> {

  Element contentElement;
  private final boolean overviewMode;
  Element scrollableElement;

  GutterView(boolean overviewMode, Position position, String cssClassName, Buffer buffer) {
    this.overviewMode = overviewMode;
    createDom(position, cssClassName);
    attachEventHandlers(buffer);
  }

  private void createDom(Position position, String cssClassName) {
    contentElement = Elements.createDivElement();
    contentElement.getStyle().setPosition(CSSStyleDeclaration.Position.RELATIVE);

    scrollableElement = Elements.createDivElement(cssClassName);
    // TODO: push into elemental
    scrollableElement.getStyle().setProperty("float",
        position == Gutter.Position.LEFT ? "left" : "right");

    scrollableElement.appendChild(contentElement);

    setElement(scrollableElement);
  }

  private void attachEventHandlers(final Buffer buffer) {
    // TODO: Detach listener in appropriate moment.
    MouseGestureListener.createAndAttach(scrollableElement, new MouseGestureListener.Callback() {
      @Override
      public boolean onClick(int clickCount, MouseEvent event) {
        if (clickCount != 1 || event.getButton() != MouseEvent.Button.PRIMARY) {
          return true;
        }
        
        int clickClientY = event.getClientY();
        int scrollableElementClientTop =
            DomUtils.calculateElementClientOffset(scrollableElement).top;
        int gutterY = clickClientY - scrollableElementClientTop + buffer.getScrollTop();
        getDelegate().onClick(gutterY);
        return true;
      }

      @Override
      public void onDrag(MouseEvent event) {
        // Do nothing.
      }

      @Override
      public void onDragRelease(MouseEvent event) {
        // Do nothing.
      }
    });

    if (!overviewMode) {
      buffer.getScrollListenerRegistrar().add(new Buffer.ScrollListener() {
        @Override
        public void onScroll(Buffer buffer, int scrollTop) {
          // no scrollTop on unscrollable elements
          contentElement.getStyle().setMarginTop(-scrollTop, CSSStyleDeclaration.Unit.PX);
        }
      });

      buffer.getHeightListenerRegistrar().add(new Buffer.HeightListener() {
        @Override
        public void onHeightChanged(int height) {
          /*
           * The gutter's height must account for a potential horizontal
           * scrollbar visible in the buffer. One example of this requirement is
           * the line number gutter: If the buffer is showing a horizontal
           * scrollbar, the last line number should be positioned the scrollbar
           * height from the bottom edge. So, the left gutter's scroll height
           * must have at least the scrollbar's height in addition to the
           * regular buffer scroll height.
           */
          contentElement.getStyle().setHeight(
              height + ScrollbarSizeCalculator.INSTANCE.getHeightOfHorizontalScrollbar(),
              CSSStyleDeclaration.Unit.PX);
        }
      });
    }
  }

  void addElement(Element element) {
    contentElement.appendChild(element);
  }

  void reset() {
    contentElement.getStyle().setMarginTop(0, CSSStyleDeclaration.Unit.PX);
    contentElement.setInnerHTML("");
  }

  void setWidth(int width) {
    scrollableElement.getStyle().setWidth(width, CSSStyleDeclaration.Unit.PX);
  }

  public int getWidth() {
    return scrollableElement.getClientWidth();
  }
}
