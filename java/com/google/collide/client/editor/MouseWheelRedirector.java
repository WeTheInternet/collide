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

import com.google.collide.client.editor.Buffer.ScrollListener;
import com.google.collide.client.util.BrowserUtils;
import com.google.collide.json.client.Jso;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MouseWheelEvent;
import elemental.html.Element;

import org.waveprotocol.wave.client.common.util.UserAgent;

/*
 * We want to behave as close to native scrolling as possible, but still prevent
 * flickering (without expanding the viewport/prerendering lines). The simplest
 * approach is to capture the amount of scroll per mousewheel, and manually
 * scroll the buffer.
 * 
 * There is a known issue with ChromeOS where it sends a deltaWheel of +/-120
 * regardless of how much it will actually scroll. See
 * http://code.google.com/p/chromium-os/issues/detail?id=23607 . Because of
 * this, we cannot properly redirect mousewheels on ChromeOS. We disable this
 * behavior which allows ChromeOS to have native-feeling scrolling (albeit
 * with flicker.)
 */
/**
 * An object that intercepts mousewheel events that would have otherwise gone to
 * the scrollable layer in the buffer. Instead, we manually scroll the buffer
 * 
 * <p>The purpose is to prevent flickering of the newly scrolled region. If the
 * scrollable element takes the scroll, that element will be scrolled before we
 * can fill it with contents. Therefore, there will be white displayed for a
 * split-second, and then text.
 */
class MouseWheelRedirector {

  public static void redirect(Buffer buffer, Element scrollableElement) {
    // ChromeOS early exit (see class implementation comment)
    if (BrowserUtils.isChromeOs()) {
      return;
    }
    
    new MouseWheelRedirector(buffer).attachEventHandlers(scrollableElement);
  }

  /**
   * Default value for {@link #mouseWheelToScrollDelta} indicating it has not
   * been defined yet.
   */
  private static final int UNDEFINED = 0;
  
  private final Buffer buffer;
  
  /**
   * The magnitude to scroll per wheelDelta unit. Even though mousewheel events
   * have wheelDelta in multiples of 120, this is the magnitude of a scroll
   * corresponding to 1.
   */
  private double mouseWheelToScrollDelta = UNDEFINED;

  private MouseWheelRedirector(Buffer buffer) {
    this.buffer = buffer;
  }

  private static native int getWheelDeltaX(Event event) /*-{
    // if using webkit (such as in Chrome) we can detect horizontal scroll
    if (event.wheelDeltaX) {
      return event.wheelDeltaX;
    } else {
      return 0;
    }
  }-*/;

  private void attachEventHandlers(Element scrollableElement) {
    /*
     * The MOUSEWHEEL does not exist on FF (it has DOMMouseScroll which we don't
     * bother to support). This means FF mousewheel scrolling will flicker.
     */
    scrollableElement.addEventListener(Event.MOUSEWHEEL, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        MouseWheelEvent mouseWheelEvent = (MouseWheelEvent) evt;
        
        /*
         * The negative is so the deltaX,Y are positive when the scroll delta
         * is. That is, a positive "deltaY" will scroll down.
         */
        int deltaY = -((Jso) mouseWheelEvent).getIntField("wheelDeltaY");
        int deltaX = -((Jso) mouseWheelEvent).getIntField("wheelDeltaX");
        
        /*
         * If the deltaY is 0, this is probably a horizontal-only scroll, in
         * which case we let it proceed as normal (no preventDefault, no manual
         * scrolling, etc.)
         */
        if (deltaY != 0) {
          if (mouseWheelToScrollDelta == UNDEFINED) {
            captureFirstMouseWheelToScrollDelta(deltaY);
          } else {
            /*
             * There is a chance that we have both a horizontal and vertical
             * scroll here. For vertical scroll, we must manually scroll to
             * prevent flickering. Since we'll need to preventDefault, we
             * must scroll the event's horizontal component too (otherwise
             * the intended horizontal scroll would be lost.)
             */
            buffer.setScrollTop(buffer.getScrollTop() + (int) (mouseWheelToScrollDelta * deltaY));
            buffer.setScrollLeft(buffer.getScrollLeft() + (int) (mouseWheelToScrollDelta * deltaX));
            evt.preventDefault();
          }
        }
      }

    }, false);
  }

  private void captureFirstMouseWheelToScrollDelta(final int wheelDelta) {
    if (UserAgent.debugUserAgentString().contains("Chrome/17") && UserAgent.isMac()
        && (wheelDelta < 120 || (wheelDelta % 120) != 0)) {
      /*
       * This is a workaround for Mac trackpads that typically send the actual pixel scroll amount
       * instead of a factor of 120. It seems like without this special check, we would still get a
       * sane mapping for mouseWheelToScrollDelta, but if the initial touchpad scroll is really
       * fast, we get a mouseWheelToScrollDelta that is way too big.
       *
       * Chrome 18 and above fix this with a fixed constant of 1 to 3, so we don't need special
       * casing. (17 is stable at the time of this writing.)
       */
      mouseWheelToScrollDelta = 1;
      
    } else {
      final int initialScrollTop = buffer.getScrollTop();

      buffer.getScrollListenerRegistrar().add(new ScrollListener() {
        @Override
        public void onScroll(Buffer buffer, int scrollTop) {
          mouseWheelToScrollDelta = Math.abs(((float) scrollTop - initialScrollTop) / wheelDelta);
          buffer.getScrollListenerRegistrar().remove(this);
        }
      });
    }
  }
}
