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

package com.google.collide.client.util.dom.eventcapture;

import com.google.collide.client.util.Elements;
import com.google.collide.client.util.JsIntegerMap;
import com.google.collide.client.util.SignalEventUtils;
import com.google.collide.client.util.input.CharCodeWithModifiers;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;

/**
 * Provides a mean for registering global hot key bindings, particularly
 * spring-loaded hot keys.
 *
 * <p>All hot keys depend on CTRL being pressed, while other modifiers are not,
 * so as not to interfere with regular typing.
 */
public class GlobalHotKey {

  /**
   * Container for one entry in the HotKey database.
   */
  public static class Data {
    private final String description;
    private final Handler handler;
    private final CharCodeWithModifiers key;

    private Data(CharCodeWithModifiers key, Handler handler, String description) {
      this.key = key;
      this.handler = handler;
      this.description = description;
    }

    public String getDescription() {
      return description;
    }

    public Handler getHandler() {
      return handler;
    }

    public CharCodeWithModifiers getKey() {
      return key;
    }
  }

  /**
   * A handler interface to receive event callbacks.
   */
  public interface Handler {
    /**
     * Called when a hot key is initially pressed.
     *
     * @param event the underlying event
     */
    void onKeyDown(SignalEvent event);
  }

  private static int handlerCount = 0;
  private static JsIntegerMap<Data> handlers;

  // Human readable descriptions for key codes.
  private static CaptureReleaser remover;

  /**
   * Registers a handler to receive notification when the key corresponding to
   * {@code keyCode} is used.
   *
   * <p>Only one handler can be tied to a particular key code. Attempting to
   * register a previously registered code will result in an assertion being
   * raised.
   *
   * @param key the key code with modifiers
   * @param handler a callback handler
   * @param description short human readable description for the action.
   */
  public static void register(CharCodeWithModifiers key, Handler handler, String description) {
    if (handlers == null) {
      remover = addEventListeners();
      handlers = JsIntegerMap.create();
    }
    Data handle = new Data(key, handler, description);
    int keyDigest = key.getKeyDigest();
    Preconditions.checkState(
        handlers.get(keyDigest) == null, "Only one handler can be registered per a key");
    handlers.put(keyDigest, handle);
    ++handlerCount;
  }

  /**
   * Unregisters a previously registered handler for a particular keyCode.
   */
  public static void unregister(CharCodeWithModifiers key) {
    int keyDigest = key.getKeyDigest();
    Preconditions.checkState(
        handlers.get(keyDigest) != null, "No handler is register for this key");
    handlers.erase(keyDigest);
    if (--handlerCount == 0) {
      remover.release();
      remover = null;
      handlers = null;
    }
  }

  private static CaptureReleaser addEventListeners() {

    final EventListener downListener = new EventListener() {
      @Override
      public void handleEvent(Event event) {
        SignalEvent signalEvent = SignalEventUtils.create(event, false);
        if (signalEvent == null) {
          return;
        }
        int keyDigest = CharCodeWithModifiers.computeKeyDigest(signalEvent);
        final Data data = handlers.get(keyDigest);
        if (data == null) {
          return;
        }

        Handler handler = data.getHandler();
        handler.onKeyDown(signalEvent);
        event.preventDefault();
      }
    };

    // Attach the listeners.
    final Element documentElement = Elements.getDocument().getDocumentElement();
    documentElement.addEventListener(Event.KEYDOWN, downListener, true);

    final CaptureReleaser downRemover = new CaptureReleaser() {
      @Override
      public void release() {
        documentElement.removeEventListener(Event.KEYDOWN, downListener, true);
      }
    };

    return new CaptureReleaser() {
      @Override
      public void release() {
        downRemover.release();
      }
    };
  }

  /**
   * This class is automatically instantiated as a singleton through the
   * {@link #register} method.
   */
  private GlobalHotKey() {
    // Do nothing
  }
}
