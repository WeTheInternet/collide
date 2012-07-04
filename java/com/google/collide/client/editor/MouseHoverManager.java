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

import com.google.collide.client.common.Constants;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerRegistrar.Remover;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Timer;

import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.UserAgent;

import elemental.events.Event;

/**
 * Manages mouse hover events, optionally when a key modifier combination is
 * pressed.
 *
 * <p>This class fires mouse hover events asynchronously, with the delay of
 * {@link Constants#MOUSE_HOVER_DELAY} milliseconds. The reason is that it is
 * quite expensive to calculate LineInfo and column number from a mouse move
 * event's {x,y} pair.
 */
public class MouseHoverManager {

  public enum KeyModifier {
    NONE(0),
    SHIFT(KeyCodes.KEY_SHIFT),
    CTRL(KeyCodes.KEY_CTRL),
    ALT(KeyCodes.KEY_ALT),
    META(91);

    /**
     * @return {@link #META} key modifier for Mac OS, or {@link #CTRL} otherwise
     */
    public static KeyModifier ctrlOrMeta() {
      return UserAgent.isMac() ? KeyModifier.META : KeyModifier.CTRL;
    }

    private final int keyCode;

    private KeyModifier(int keyCode) {
      this.keyCode = keyCode;
    }

    public int getKeyCode() {
      return keyCode;
    }
  }

  public interface MouseHoverListener {
    void onMouseHover(int x, int y, LineInfo lineInfo, int column);
  }

  private final Editor editor;
  private final JsonStringMap<ListenerManager<MouseHoverListener>> listenerManagers =
      JsonCollections.createMap();

  /**
   * Current key combination that we will dispatch the mouse hover events for.
   * If {@code null}, no mouse hover events should be dispatched just yet.
   */
  private KeyModifier lastKeyModifier = KeyModifier.NONE;

  private ListenerRegistrar.Remover keyPressListenerRemover;
  private ListenerRegistrar.Remover mouseMoveListenerRemover;
  private ListenerRegistrar.Remover mouseOutListenerRemover;
  private ListenerRegistrar.Remover nativeKeyUpListenerRemover;

  private final Editor.NativeKeyUpListener keyUpListener = new Editor.NativeKeyUpListener() {
    @Override
    public boolean onNativeKeyUp(Event event) {
      /*
       * Consider any key-up event releases the key modifier combination, to
       * avoid tricky stale states.
       */
      releaseLastKeyModifier();

      // Do not interfere with the editor input.
      return false;
    }
  };

  private final Editor.KeyListener keyPressListener = new Editor.KeyListener() {
    @Override
    public boolean onKeyPress(SignalEvent signal) {
      KeyModifier newKeyModifier = null;

      JsonArray<String> modifierKeys = listenerManagers.getKeys();
      for (int i = 0, n = modifierKeys.size(); i < n; ++i) {
        KeyModifier keyModifier = KeyModifier.valueOf(modifierKeys.get(i));
        if (keyModifier.getKeyCode() == signal.getKeyCode()) {
          newKeyModifier = keyModifier;
          break;
        }
      }

      if (lastKeyModifier != newKeyModifier) {
        lastKeyModifier = newKeyModifier;
        updateEditorListeners();
      }

      // Do not interfere with the editor input.
      return false;
    }
  };

  private class MouseListenersImpl extends Timer implements
      Buffer.MouseMoveListener, Buffer.MouseOutListener {
    private int x;
    private int y;

    @Override
    public void run() {
      handleOnMouseMove(x, y);
    }

    @Override
    public void onMouseMove(int x, int y) {
      this.x = x;
      this.y = y;
      schedule(Constants.MOUSE_HOVER_DELAY);
    }

    @Override
    public void onMouseOut() {
      // We are no longer hovering the editor's buffer.
      cancel();

      // We can not track the keyboard outside the buffer, just reset the state.
      releaseLastKeyModifier();
    }
  }

  private final MouseListenersImpl mouseListener = new MouseListenersImpl();

  MouseHoverManager(Editor editor) {
    this.editor = editor;
  }

  public Remover addMouseHoverListener(MouseHoverListener listener) {
    return addMouseHoverListener(KeyModifier.NONE, listener);
  }

  public Remover addMouseHoverListener(
      final KeyModifier keyModifier, final MouseHoverListener listener) {
    String key = keyModifier.toString();
    ListenerManager<MouseHoverListener> manager = listenerManagers.get(key);
    if (manager == null) {
      manager = ListenerManager.create();
      listenerManagers.put(key, manager);
    }

    final Remover listenerRemover = manager.add(listener);
    updateEditorListeners();

    return new Remover() {
      @Override
      public void remove() {
        removeMouseHoverListener(listenerRemover, keyModifier, listener);
      }
    };
  }

  private void removeMouseHoverListener(
      Remover listenerRemover, KeyModifier keyModifier, MouseHoverListener listener) {
    String key = keyModifier.toString();
    ListenerManager<MouseHoverListener> manager = listenerManagers.get(key);
    if (manager == null) {
      return;
    }

    listenerRemover.remove();
    if (manager.getCount() == 0) {
      listenerManagers.remove(key);
    }

    updateEditorListeners();
  }

  private void releaseLastKeyModifier() {
    if (lastKeyModifier != KeyModifier.NONE) {
      lastKeyModifier = KeyModifier.NONE;
      updateEditorListeners();
    }
  }

  private void updateEditorListeners() {
    if (listenerManagers.isEmpty()) {
      removeAllEditorListeners();
      return;
    }

    // Attach the performance-critical mouse move listener only if we really need it.
    if (lastKeyModifier == null || listenerManagers.get(lastKeyModifier.toString()) == null) {
      if (mouseMoveListenerRemover != null) {
        mouseMoveListenerRemover.remove();
        mouseMoveListenerRemover = null;
      }
    } else {
      if (mouseMoveListenerRemover == null) {
        mouseMoveListenerRemover =
            editor.getBuffer().getMouseMoveListenerRegistrar().add(mouseListener);
      }
    }

    if (keyPressListenerRemover == null) {
      keyPressListenerRemover = editor.getKeyListenerRegistrar().add(keyPressListener);
    }
    if (nativeKeyUpListenerRemover == null) {
      nativeKeyUpListenerRemover = editor.getNativeKeyUpListenerRegistrar().add(keyUpListener);
    }

    /*
     * We should always listen to these events, since we want to release the
     * last key modifier upon receiving it.
     */
    if (mouseOutListenerRemover == null) {
      mouseOutListenerRemover =
          editor.getBuffer().getMouseOutListenerRegistrar().add(mouseListener);
    }
  }

  private void removeAllEditorListeners() {
    if (keyPressListenerRemover != null) {
      keyPressListenerRemover.remove();
      keyPressListenerRemover = null;
    }
    if (mouseMoveListenerRemover != null) {
      mouseMoveListenerRemover.remove();
      mouseMoveListenerRemover = null;
    }
    if (mouseOutListenerRemover != null) {
      mouseOutListenerRemover.remove();
      mouseOutListenerRemover = null;
    }
    if (nativeKeyUpListenerRemover != null) {
      nativeKeyUpListenerRemover.remove();
      nativeKeyUpListenerRemover = null;
    }
  }

  private void handleOnMouseMove(final int x, final int y) {
    if (lastKeyModifier == null || editor.getDocument() == null) {
      return;
    }

    String key = lastKeyModifier.toString();
    ListenerManager<MouseHoverListener> manager = listenerManagers.get(key);
    if (manager == null) {
      return;
    }

    int lineNumber = editor.getBuffer().convertYToLineNumber(y, true);
    final LineInfo lineInfo = editor.getDocument().getLineFinder().findLine(lineNumber);
    final int column = editor.getBuffer().convertXToRoundedVisibleColumn(x, lineInfo.line());

    manager.dispatch(new ListenerManager.Dispatcher<MouseHoverListener>() {
      @Override
      public void dispatch(MouseHoverListener listener) {
        listener.onMouseHover(x, y, lineInfo, column);
      }
    });
  }
}
