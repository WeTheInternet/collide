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

package com.google.collide.client.editor.input;

import com.google.collide.client.util.SignalEventUtils;
import com.google.collide.client.util.input.CharCodeWithModifiers;
import com.google.collide.client.util.input.KeyCodeMap;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import elemental.events.Event;
import elemental.js.util.JsArrayOfInt;
import elemental.js.util.JsMapFromIntTo;

/**
 * Class that represents an editor mode for handling input, such as vi's insert
 * mode.
 *
 * <p>Each mode controls a collection of {@link Shortcut}s and fires them
 * based upon any input via {@link Shortcut#event}.
 */
public abstract class InputMode {

  /**
   * Effect of a SignalEvent.
   */
  public enum EventResult {
    /**
     * Event would directly trigger a StreamShortcut callback.
     */
    STREAM_CALLBACK,

    /**
     * Event is part of one or more StreamShortcuts, but has not been completely
     * typed.
     */
    STREAM_PART,

    /**
     * Event would directly trigger an EventShortcut callback.
     */
    EVENT_CALLBACK,

    /**
     * Event would not trigger an Event, or was part/all of StreamShortcut
     */
    NONE
  }

  /**
   * Manager of collection of shortcuts for this class and the buffer of any
   * ongoing shortcut stream of keys.
   */
  public class ShortcutController {
    /**
     * Generic Node class.
     *
     * Cannot be nested inside PartialTrie
     * (<a href="http://code.google.com/p/google-web-toolkit/issues/detail?id=5483">link</a>).
     */
    private class Node<T> {
      T value;
      JsMapFromIntTo<Node<T>> next;

      Node() {
        next = JsMapFromIntTo.create();
      }
    }

    //TODO: Move this functionality to AbstractTrie.
    /**
     * Basic trie class, supporting only put and get, plus a feedback function
     * to check if a string is along the path to a valid trie entry.
     *
     * @see #alongPath
     */
    private class PartialTrie<T> {
      private Node<T> root;

      PartialTrie() {
        root = new Node<T>();
      }

      /**
       * Inserts a new value T into the trie.
       *
       * @return T previous value at prefix, or null if there was no old entry
       */
      T put(JsArrayOfInt prefix, T value) {
        Node<T> current = root;
        for (int i = 0, n = prefix.length(); i < n; i++) {
          int index = prefix.get(i);
          Node<T> next = current.next.get(index);
          if (next == null) {
            // this branch doesn't exist yet
            next = new Node<T>();
            current.next.put(index, next);
            current = next;
          }
        }
        T old = current.value;
        current.value = value;
        return old;
      }

      /**
       * Returns 0 if seq is along the path to one or more entries.
       *
       * <p>For example, nearestValue("app") would return 0 for a trie with
       * values "apples", "apple", "orange". "apple" has 0 characters to get
       * to "apple".
       *
       * @return {@code 1} for direct match, {@code 0} for path match,
       *         {@code -1} for no match
       */
      int alongPath(JsArrayOfInt seq) {
        Node<T> current = root;

        for (int i = 0, n = seq.length(); i < n; i++) {
          int index = seq.get(i);
          current = current.next.get(index);

          if (current == null) {
            return -1; // off the end of the trie, no match
          }
        }

        // If we get here, current is along the path to one or more valid
        // entries
        if (current.value != null) {
          return 1;
        } else {
          return 0;
        }
      }

      /**
       * Returns the value T stored at exactly this location in the trie.
       *
       * @return T
       */
      T get(JsArrayOfInt seq) {
        Node<T> current = root;
        for (int i = 0, n = seq.length(); i < n; i++) {
          int index = seq.get(i);
          current = current.next.get(index);

          if (current == null) {
            return null;
          }
        }

        return current.value;
      }
    }

    /**
     * Buffer of the current input stream building to a shortcut.
     *
     * <p>String is represented by an array of UTF-16 integers.
     *
     * <p>This will be appended to when the input matches a prefix of one
     * or more {@link StreamShortcut}s. Used to match against a PrefixTrie.
     */
    JsArrayOfInt streamBuffer;

    PartialTrie<StreamShortcut> streamTrie;

    /**
     * A map of {@link EventShortcut}s from event hash to shortcut object.
     */
    JsMapFromIntTo<EventShortcut> eventShortcuts;


    public ShortcutController() {
      eventShortcuts = JsMapFromIntTo.create();
      streamTrie = new PartialTrie<StreamShortcut>();
      streamBuffer = JsArrayOfInt.create();
    }

    /**
     * Adds an event shortcut to the event shortcut map.
     */
    public void addShortcut(EventShortcut event) {
      eventShortcuts.put(event.getKeyDigest(), event);
    }

    /**
     * Adds a stream shortcut to the stream shortcut trie.
     */
    public void addShortcut(StreamShortcut stream) {
      streamTrie.put(stream.getActivationStream(), stream);
    }

    /**
     * Clears any internal state (streamBuffer value),
     * after shortcut was triggered.
     */
    public void reset() {
      streamBuffer.setLength(0);
    }

    /**
     * Returns the shortcut associated with this event.
     */
    private Shortcut findEventShortcut(SignalEvent event) {
      int keyDigest = CharCodeWithModifiers.computeKeyDigest(event);
      if (eventShortcuts.hasKey(keyDigest)) {
        return eventShortcuts.get(keyDigest);
      } else {
        return null;
      }
    }

    /**
     * Searches the trie using streamBuffer for an exact
     * {@link StreamShortcut} match.
     *
     * @return {@link StreamShortcut} if found, else {@code null}
     */
    private Shortcut findStreamShortcut() {
      return streamTrie.get(streamBuffer);
    }

    /**
     * Adds the keycode of the event to the end of the stream buffer.
     */
    private void addToStreamBuffer(SignalEvent event) {
      streamBuffer.push(KeyCodeMap.getKeyFromEvent(event));
    }

    /**
     * Deletes the last character from the stream buffer (backspace).
     */
    public void deleteLastCharFromStreamBuffer() {
      streamBuffer.setLength(streamBuffer.length() - 1);
    }

    /**
     * Tests if the event should be "captured".
     *
     * <p>Event should be captured if it either:<ul>
     * <li>directly fires a callback or
     * <li>is part of a StreamCallback that hasn't been fully typed yet
     * </ul>
     *
     * @return {@link EventResult} that this event would cause
     */
    public EventResult testEventEffect(SignalEvent event) {
      // Letters above U+FFFF will wrap around, so they aren't supported in
      // shortcuts.
      if (event.getKeyCode() > 0xFFFF) {
        return EventResult.NONE;
      }

      // Try EventShortcut.
      int keyDigest = CharCodeWithModifiers.computeKeyDigest(event);
      if (eventShortcuts.hasKey(keyDigest)) {
        return EventResult.EVENT_CALLBACK;
      }

      // Then try StreamShortcut.
      addToStreamBuffer(event);
      int streamResult = streamTrie.alongPath(streamBuffer);
      // Take off event keycode - it was added only to search trie.
      deleteLastCharFromStreamBuffer();

      if (streamResult == 1) {
        // Exact match.
        return EventResult.STREAM_CALLBACK;
      } else if (streamResult == 0) {
        // Partial match.
        return EventResult.STREAM_PART;
      }

      // No effect.
      return EventResult.NONE;
    }
  }

  private ShortcutController shortcutController;

  private InputScheme scheme = null;

  public InputMode() {
    shortcutController = new ShortcutController();
  }

  /**
   * Preforms mode-specific setup (such as adding a new overlay to
   * display the current search term as the user types).
   */
  public abstract void setup();

  /**
   * Removes document changes made in {@link InputMode#setup()}.
   */
  public abstract void teardown();

  /**
   * Implements default behavior when no shortcut matches the input event.
   *
   * <p>Include the text captured from the hidden input field.
   *
   * @param character - 0 for no printable character
   * @return {@code true} to prevent default action in browser
   */
  public abstract boolean onDefaultInput(SignalEvent signal, char character);

  /**
   * Takes action after user has inserted more than one character of text.
   *
   * @param text - more than one character
   * @return boolean True to prevent default action in browser
   */
  public abstract boolean onDefaultPaste(SignalEvent signal, String text);

  void setScheme(InputScheme scheme) {
    this.scheme = scheme;
  }

  public InputScheme getScheme() {
    return this.scheme;
  }

  /**
   * Binds specified key to named action.
   */
  public void bindAction(String actionName, int modifiers, int charCode) {
    shortcutController.addShortcut(new ActionShortcut(modifiers, charCode, actionName));
  }

  /**
   * Adds this event shortcut to the shortcut controller.
   */
  public void addShortcut(EventShortcut shortcut) {
    shortcutController.addShortcut(shortcut);
  }

  public void addShortcut(StreamShortcut shortcut) {
    shortcutController.addShortcut(shortcut);
  }

  /**
   * Checks if this event should fire any shortcuts.
   *
   * <p>There is not matching events, fires the defaultInput function.
   *
   * @return {@code true} if default browser behavior should be prevented
   */
  public boolean handleEvent(SignalEvent event, String text) {
    if (event.isPasteEvent()) {
      String pasteContents = SignalEventUtils.getPasteContents((Event) event.asEvent());
      if (pasteContents != null) {
        return onDefaultPaste(event, pasteContents);
      }
    }

    // If one character was entered, send it through the shortcut system, else
    // think of it as a paste.
    if (text.length() > 1) {
      return onDefaultPaste(event, text);
    } else {
      Shortcut eventShortcut = null;
      EventResult result = shortcutController.testEventEffect(event);
      if (result == EventResult.EVENT_CALLBACK) {
        eventShortcut = shortcutController.findEventShortcut(event);
      }

      if (result == EventResult.NONE) {
        shortcutController.reset();
        char character = 0;
        if (text.length() == 1) {
          character = text.charAt(0);
        }
        return onDefaultInput(event, character);
      }

      if (result == EventResult.STREAM_CALLBACK || result == EventResult.STREAM_PART) {
        // Always add to the buffer for either of these.
        shortcutController.addToStreamBuffer(event);

        if (result == EventResult.STREAM_CALLBACK) {
          eventShortcut = shortcutController.findStreamShortcut();
        } else {
          // STREAM_PART
          return true; // Always prevent default when adding to stream buffer.
        }
      }

      // Tell the shortcut controller that a shortcut was fired so it can reset
      // state.
      shortcutController.reset();
      boolean returnValue = eventShortcut.event(this.scheme, event);

      // Only fire if the event is blocked.
      if (returnValue) {
        this.scheme.handleShortcutCalled();
      }
      return returnValue;
    }
  }
}
