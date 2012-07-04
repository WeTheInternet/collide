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

import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.Spacer;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.editor.selection.SelectionModel.MoveAction;
import com.google.collide.client.util.input.CharCodeWithModifiers;
import com.google.collide.client.util.input.KeyCodeMap;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.SortedList;

import org.waveprotocol.wave.client.common.util.JsoIntMap;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.UserAgent;

import elemental.events.KeyboardEvent;

import java.util.Random;

/**
 * The default InputScheme implementation for common keybindings. {@link ModifierKeys#ACTION}
 * for action key abstraction details.
 * <ul>
 * <li>ACTION+S - prevent default save behavior from browser.
 * <li>ACTION+A - select all text in the current document.
 * <li>TAB - if there is a selection region then insert a tab at the beginning
 * of each line the selection passes through, else insert a tab at the current
 * position.
 * <li>SHIFT+TAB - remove a tab character if possible from the beginning of each
 * line the current selection passes through.
 * <li>BACKSPACE - delete one character to the left of the cursor. DELETE -
 * delete one character to the right of the cursor.
 * <li>CUT (ACTION+X)/COPY (ACTION+C)/PASTE (ACTION+V) - same as native
 * cut/copy/paste functionality.
 * <li>UNDO (ACTION+Z)/REDO (ACTION+Y) - undo or redo the most recent change
 * to the document.
 * <li>Cursor movement (ARROW_*, PAGE_*) - change the position of the cursor
 * based upon the directional key pressed and the unit of movement associated
 * with that key. For arrow keys this is one character in the direction of the
 * arrow, for page up/down this is an entire page.
 * </ul>
 *
 *
 */
public class DefaultScheme extends InputScheme {

  private static final boolean ENABLE_DEBUG_SPACER_KEYS = false;
  private static final boolean ENABLE_ANIMATION_CONTROL_KEYS = false;
  static final JsoIntMap<MoveAction> MOVEMENT_KEYS_MAPPING = JsoIntMap.create();

  InputMode defaultMode;

  static {
    //Initialize key mappings.
    registerMovementKey(ModifierKeys.NONE, KeyCodeMap.ARROW_LEFT, MoveAction.LEFT);
    registerMovementKey(ModifierKeys.NONE, KeyCodeMap.ARROW_RIGHT, MoveAction.RIGHT);
    registerMovementKey(ModifierKeys.NONE, KeyCodeMap.ARROW_UP, MoveAction.UP);
    registerMovementKey(ModifierKeys.NONE, KeyCodeMap.ARROW_DOWN, MoveAction.DOWN);
    registerMovementKey(ModifierKeys.NONE, KeyCodeMap.PAGE_UP, MoveAction.PAGE_UP);
    registerMovementKey(ModifierKeys.NONE, KeyCodeMap.PAGE_DOWN, MoveAction.PAGE_DOWN);

    if (UserAgent.isMac()) {
      registerMovementKey(ModifierKeys.ALT, KeyCodeMap.ARROW_LEFT, MoveAction.WORD_LEFT);
      registerMovementKey(ModifierKeys.ALT, KeyCodeMap.ARROW_RIGHT, MoveAction.WORD_RIGHT);
      registerMovementKey(ModifierKeys.ACTION, KeyCodeMap.ARROW_LEFT, MoveAction.LINE_START);
      registerMovementKey(ModifierKeys.ACTION, KeyCodeMap.ARROW_RIGHT, MoveAction.LINE_END);
      registerMovementKey(ModifierKeys.NONE, KeyCodeMap.HOME, MoveAction.TEXT_START);
      registerMovementKey(ModifierKeys.NONE, KeyCodeMap.END, MoveAction.TEXT_END);
      
      /*
       * Add Emacs-style bindings on Mac (note that these will not conflict 
       * with any Collide shortcuts since these chord with CTRL, and Collide
       * shortcuts chord with CMD)
       */
      registerMovementKey(ModifierKeys.CTRL, 'a', MoveAction.LINE_START);
      registerMovementKey(ModifierKeys.CTRL, 'e', MoveAction.LINE_END);
      registerMovementKey(ModifierKeys.CTRL, 'p', MoveAction.UP);
      registerMovementKey(ModifierKeys.CTRL, 'n', MoveAction.DOWN);
      registerMovementKey(ModifierKeys.CTRL, 'f', MoveAction.RIGHT);
      registerMovementKey(ModifierKeys.CTRL, 'b', MoveAction.LEFT);
      
    } else {
      registerMovementKey(ModifierKeys.ACTION, KeyCodeMap.ARROW_LEFT, MoveAction.WORD_LEFT);
      registerMovementKey(ModifierKeys.ACTION, KeyCodeMap.ARROW_RIGHT, MoveAction.WORD_RIGHT);
      registerMovementKey(ModifierKeys.NONE, KeyCodeMap.HOME, MoveAction.LINE_START);
      registerMovementKey(ModifierKeys.NONE, KeyCodeMap.END, MoveAction.LINE_END);
      registerMovementKey(ModifierKeys.ACTION, KeyCodeMap.HOME, MoveAction.TEXT_START);
      registerMovementKey(ModifierKeys.ACTION, KeyCodeMap.END, MoveAction.TEXT_END);
    }
  }

  private static void registerMovementKey(int modifiers, int charCode, MoveAction action) {
    MOVEMENT_KEYS_MAPPING.put(CharCodeWithModifiers.computeKeyDigest(modifiers, charCode), action);
  }

  /**
   * Setup and add single InputMode
   */
  public DefaultScheme(InputController inputController) {
    super(inputController);

    defaultMode = new InputMode() {
      @Override
      public void setup() {
      }

      @Override
      public void teardown() {
      }

      /**
       * By default, check for cursor movement, then add this as text to the
       * current document.
       *
       * Only add printable text, not control characters.
       */
      @Override
      public boolean onDefaultInput(SignalEvent event, char character) {
        // Check for movement here.
        int letter = KeyCodeMap.getKeyFromEvent(event);
        int modifiers = ModifierKeys.computeModifiers(event);
        boolean withShift = (modifiers & ModifierKeys.SHIFT) != 0;
        modifiers &= ~ModifierKeys.SHIFT;
        int strippedKeyDigest = CharCodeWithModifiers.computeKeyDigest(modifiers, letter);
        if (MOVEMENT_KEYS_MAPPING.containsKey(strippedKeyDigest)) {
          MoveAction action = MOVEMENT_KEYS_MAPPING.get(strippedKeyDigest);
          getScheme().getInputController().getSelection().move(action, withShift);
          return true;
        }

        if (event.getAltKey()) {
          // Don't process Alt+* combinations.
          return false;
        }

        if (event.getCommandKey() || event.getKeySignalType() != SignalEvent.KeySignalType.INPUT) {
          // Don't insert any Action+* / non-input combinations as text.
          return false;
        }

        InputController input = this.getScheme().getInputController();
        SelectionModel selection = input.getSelection();
        int column = selection.getCursorColumn();

        if (KeyCodeMap.isPrintable(letter)) {
          String text = Character.toString((char) letter);
          // insert a single character
          input.getEditorDocumentMutator().insertText(selection.getCursorLine(),
              selection.getCursorLineNumber(), column, text);
          return true;
        }
        // let it fall through
        return false;
      }

      /**
       * Insert more than one character directly into the document
       */
      @Override
      public boolean onDefaultPaste(SignalEvent event, String text) {
        SelectionModel selection = this.getScheme().getInputController().getSelection();
        int column = selection.getCursorColumn();

        getScheme().getInputController().getEditorDocumentMutator()
            .insertText(selection.getCursorLine(), selection.getCursorLineNumber(), column, text);
        return true;
      }
    };

    /*
     * ACTION+S - prevent the default browser behavior because the document is
     * saved automatically.
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 's') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        // prevent ACTION+S
        return true;
      }
    });

    /**
     * ACTION+A - select all text in the current document.
     *
     * @see SelectionModel#selectAll()
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'a') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().getSelection().selectAll();
        return true;
      }
    });

    defaultMode.bindAction(CommonActions.GOTO_DEFINITION, ModifierKeys.ACTION, 'b');
    defaultMode.bindAction(CommonActions.GOTO_SOURCE, ModifierKeys.NONE, KeyCodeMap.F4);
    defaultMode.bindAction(CommonActions.SPLIT_LINE, ModifierKeys.ACTION, KeyCodeMap.ENTER);
    defaultMode.bindAction(CommonActions.START_NEW_LINE, ModifierKeys.SHIFT, KeyCodeMap.ENTER);

    // Single / multi-line comment / uncomment.
    defaultMode.bindAction(CommonActions.TOGGLE_COMMENT,
        ModifierKeys.ACTION, KeyboardEvent.KeyCode.SLASH);

    // Multi-line indenting and dedenting.

    /**
     * TAB - add a tab at the current position, or at the beginning of each line
     * if there is a selection.
     *
     * @see InputController#indentSelection()
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.TAB) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().handleTab();
        return true;
      }
    });

    /**
     * SHIFT+TAB - remove a tab from the beginning of each line in the
     * selection.
     *
     * @see InputController#dedentSelection()
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.SHIFT, KeyCodeMap.TAB) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().dedentSelection();
        return true;
      }
    });

    /**
     * BACKSPACE - native behavior
     *
     * @see InputController#deleteCharacter(boolean)
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.BACKSPACE) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().deleteCharacter(false);
        return true;
      }
    });

    /**
     * SHIFT+BACKSPACE - native behavior
     *
     * @see InputController#deleteCharacter(boolean)
     */
    // This is common due to people backspacing while typing uppercase chars
    defaultMode.addShortcut(
        new EventShortcut(ModifierKeys.SHIFT, KeyCodeMap.BACKSPACE) {
          @Override
          public boolean event(InputScheme scheme, SignalEvent event) {
            scheme.getInputController().deleteCharacter(false);
            return true;
          }
        });

    /**
     * ACTION+BACKSPACE - delete previous word
     */
    defaultMode.addShortcut(new EventShortcut(wordGrainModifier(), KeyCodeMap.BACKSPACE) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().deleteWord(false);
        return true;
      }
    });

    /**
     * DELETE - native behavior
     *
     * @see InputController#deleteCharacter(boolean)
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.DELETE) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().deleteCharacter(true);
        return true;
      }
    });

    /**
     * ESC - Broadcasts clear message
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.ESC) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        // TODO: Make this broadcast event.
        return true;
      }
    });

    /**
     * ACTION+DELETE - delete next word
     */
    defaultMode.addShortcut(new EventShortcut(wordGrainModifier(), KeyCodeMap.DELETE) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().deleteWord(true);
        return true;
      }
    });

    /**
     * UNDO (ACTION+Z) - undo the most recent document action
     *
     * @see Editor#undo()
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'z') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().getEditor().undo();
        return true;
      }
    });

    /**
     * REDO (ACTION+Y) - redo the last undone document action
     *
     * @see Editor#redo()
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'y') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().getEditor().redo();
        return true;
      }
    });

    /**
     * Find Next (ACTION+G) - Goto next match
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'g') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme
            .getInputController()
            .getEditor()
            .getSearchModel()
            .getMatchManager()
            .selectNextMatch();
        return true;
      }
    });

    /**
     * Find Previous (ACTION+SHIFT+G) - Goto previous match
     */
    defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'G') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme
            .getInputController()
            .getEditor()
            .getSearchModel()
            .getMatchManager()
            .selectPreviousMatch();
        return true;
      }
    });

    /**
     * ACTION+ALT+V - Switch from Default to Vim Scheme
     *
     * TODO: Removed VIM keybinding access until we're ready to launch
     * it
     */
    if (false) {
      // Disabled due to prioritization
      defaultMode
          .addShortcut(new EventShortcut(ModifierKeys.ACTION | ModifierKeys.ALT, 'v') {
            @Override
            public boolean event(InputScheme scheme, SignalEvent event) {
              getInputController().setActiveInputScheme(getInputController().vimScheme);
              return true;
            }
          });
    }


    if (ENABLE_DEBUG_SPACER_KEYS) {
      final SortedList<Spacer> spacers =
          new SortedList<Spacer>(new Spacer.Comparator());
      final Spacer.OneWaySpacerComparator spacerFinder = new Spacer.OneWaySpacerComparator();

      defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'i') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          final Editor editor = scheme.getInputController().getEditor();
          spacers.add(editor.getBuffer().addSpacer(
              new LineInfo(editor.getSelection().getCursorLine(), editor.getSelection()
                  .getCursorLineNumber()), new Random().nextInt(500) + 1));
          return true;
        }
      });

      defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'd') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          final Editor editor = scheme.getInputController().getEditor();
          spacerFinder.setValue(editor.getSelection().getCursorLineNumber());
          int spacerIndex = spacers.findInsertionIndex(spacerFinder, false);
          if (spacerIndex >= 0) {
            editor.getBuffer().removeSpacer(spacers.get(spacerIndex));
            spacers.remove(spacerIndex);
          }
          return true;
        }
      });

      defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'u') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          final Editor editor = scheme.getInputController().getEditor();
          spacerFinder.setValue(editor.getSelection().getCursorLineNumber());
          int spacerIndex = spacers.findInsertionIndex(spacerFinder, false);
          if (spacerIndex >= 0) {
            // spacers.get(spacerIndex).setHeight(new Random().nextInt(500)+1);
          }
          return true;
        }
      });
    }

    if (ENABLE_ANIMATION_CONTROL_KEYS) {
      defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'e') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          final Editor editor = scheme.getInputController().getEditor();
          editor.setAnimationEnabled(true);
          return true;
        }
      });

      defaultMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'd') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          final Editor editor = scheme.getInputController().getEditor();
          editor.setAnimationEnabled(false);
          return true;
        }
      });
    }

    addMode(1, defaultMode);
  }

  private static int wordGrainModifier() {
    return UserAgent.isMac() ? ModifierKeys.ALT : ModifierKeys.ACTION;
  }

  @Override
  public void setup() {
    switchMode(1);
  }
}
