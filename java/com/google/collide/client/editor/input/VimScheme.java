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

import com.google.collide.client.editor.search.SearchModel;
import com.google.collide.client.editor.selection.LocalCursorController;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.editor.selection.SelectionModel.MoveAction;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.input.CharCodeWithModifiers;
import com.google.collide.client.util.input.KeyCodeMap;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.document.util.PositionUtils;
import com.google.collide.shared.util.ScopeMatcher;
import com.google.collide.shared.util.StringUtils;
import com.google.collide.shared.util.TextUtils;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.util.JsoIntMap;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEvent.MoveUnit;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;

/**
 * Basic Vi(m) keybinding support. This is limited to single file operations.
 * This includes the main Vim modes for Command, Visual, Insert, and single-line
 * search/command entry.
 */
public class VimScheme extends InputScheme {

  private static final JsoIntMap<MoveAction> CHAR_MOVEMENT_KEYS_MAPPING = JsoIntMap.create();
  private static final JsoIntMap<MoveAction> LINE_MOVEMENT_KEYS_MAPPING = JsoIntMap.create();

  /**
   * Command mode mirrors Vim's command mode for performing commands from single
   * keypresses.
   */
  InputMode commandMode;
  /**
   * Insert mode can only insert text or escape back to command mode.
   */
  InputMode insertMode;
  /**
   * Command capture mode gets switched to from command mode when a ":" is typed
   * until the enter key is pressed, and the resulting text between : and
   * <enter> is used as the argument to {@link #doColonCommand(StringBuilder)}.
   */
  InputMode commandCaptureMode;
  /**
   * Search capture mode gets switched to from command mode when a "/" is typed
   * and will highlight whatever text is typed in the current document until
   * escape or enter are pressed.
   */
  InputMode searchCaptureMode;
  Element statusHeader;
  boolean inVisualMode;
  MoveUnit visualMoveUnit;

  /** Any numbers typed before a command shortcut. */
  private StringBuilder numericPrefixText = new StringBuilder();
  private StringBuilder command;
  private StringBuilder searchTerm;
  private String clipboard;
  private boolean isLineCopy;

  private static enum Modes {
    COMMAND, INSERT, COMMAND_CAPTURE, SEARCH_CAPTURE
  }

  // The order of characters here needs to match
  private static final String OPENING_GROUPS = "{[(";
  private static final String CLOSING_GROUPS = "}])";

  static {
    CHAR_MOVEMENT_KEYS_MAPPING.put(
        CharCodeWithModifiers.computeKeyDigest(ModifierKeys.NONE, 'h'), MoveAction.LEFT);
    CHAR_MOVEMENT_KEYS_MAPPING.put(
        CharCodeWithModifiers.computeKeyDigest(ModifierKeys.NONE, 'j'), MoveAction.DOWN);
    CHAR_MOVEMENT_KEYS_MAPPING.put(
        CharCodeWithModifiers.computeKeyDigest(ModifierKeys.NONE, 'k'), MoveAction.UP);
    CHAR_MOVEMENT_KEYS_MAPPING.put(
        CharCodeWithModifiers.computeKeyDigest(ModifierKeys.NONE, 'l'), MoveAction.RIGHT);

    LINE_MOVEMENT_KEYS_MAPPING.put(
        CharCodeWithModifiers.computeKeyDigest(ModifierKeys.NONE, 'h'), MoveAction.LINE_START);
    LINE_MOVEMENT_KEYS_MAPPING.put(
        CharCodeWithModifiers.computeKeyDigest(ModifierKeys.NONE, 'j'), MoveAction.DOWN);
    LINE_MOVEMENT_KEYS_MAPPING.put(
        CharCodeWithModifiers.computeKeyDigest(ModifierKeys.NONE, 'k'), MoveAction.UP);
    LINE_MOVEMENT_KEYS_MAPPING.put(
        CharCodeWithModifiers.computeKeyDigest(ModifierKeys.NONE, 'l'), MoveAction.LINE_END);
  }

  /**
   * TODO: Refactor shortcut system to separate activation key
   * definition (the "Shortcut") from the actual shortcut function (the
   * "Action") as some shortcuts have duplicate actions.
   */
  public VimScheme(InputController inputController) {
    super(inputController);

    commandMode = new InputMode() {
      @Override
      public void setup() {
        setStatus("-- COMMAND --");
        inVisualMode = false;
        numericPrefixText.setLength(0);
        LocalCursorController cursor = getInputController().getEditor().getCursorController();
        cursor.setBlockMode(true);
        cursor.setColor("blue");
        getInputController().getEditor().getSelection().deselect();
      }

      @Override
      public void teardown() {
        getInputController().getEditor().getCursorController().resetCursorView();
      }

      @Override
      public boolean onDefaultInput(SignalEvent signal, char character) {
        // navigate here
        int letter = KeyCodeMap.getKeyFromEvent(signal);
        int modifiers = ModifierKeys.computeModifiers(signal);
        modifiers = modifiers & ~ModifierKeys.SHIFT;
        int strippedKeyDigest = CharCodeWithModifiers.computeKeyDigest(modifiers, letter);
        JsoIntMap<MoveAction> movementKeysMapping = getMovementKeysMapping();
        if (movementKeysMapping.containsKey(strippedKeyDigest)) {
          MoveAction action = movementKeysMapping.get(strippedKeyDigest);
          getScheme().getInputController().getSelection().move(action, inVisualMode);
          return true;
        }

        if (tryAddNumericPrefix((char) letter)) {
          return true;
        }

        numericPrefixText.setLength(0);
        return false;
      }

      /**
       * Paste events. These can come in from native Command+V or forced via
       * holding a local clipboard/buffer of any text copied within the editor.
       */
      @Override
      public boolean onDefaultPaste(SignalEvent signal, String text) {
        handlePaste(text, true);
        return true;
      }
    };

    addMode(Modes.COMMAND, commandMode);

    /*
     * Command+Alt+V - Switch from Vim to Default Scheme
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.ACTION | ModifierKeys.ALT, 'v') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        getInputController().setActiveInputScheme(getInputController().nativeScheme);
        return true;
      }
    });

    /*
     * ESC, ACTION+[ - reset state in command mode
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.ESC) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.COMMAND);
        return true;
      }
    });
    commandMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, '[') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.COMMAND);
        return true;
      }
    });

    /*
     * TODO: extract common visual mode switching code.
     */
    /*
     * v - Switch to visual mode (character)
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'v') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        setStatus("-- VISUAL (char) --");
        inVisualMode = true;
        visualMoveUnit = MoveUnit.CHARACTER;
        // select the character the cursor is over now
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        int cursorColumn = selectionModel.getCursorColumn();
        selectionModel.setSelection(cursorLineInfo, cursorColumn, cursorLineInfo, cursorColumn + 1);
        return true;
      }
    });

    /*
     * V - Switch to visual mode (line)
     */
    /*
     * TODO: Doesn't exactly match vim's visual-line mode, force
     * selections of entire lines.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'V') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        setStatus("-- VISUAL (line) --");
        inVisualMode = true;
        visualMoveUnit = MoveUnit.LINE;
        // move cursor to beginning of current line, select to column 0 of next
        // line
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        LineInfo nextLineInfo =
            new LineInfo(cursorLineInfo.line().getNextLine(), cursorLineInfo.number() + 1);
        selectionModel.setSelection(cursorLineInfo, 0, nextLineInfo, 0);
        return true;
      }
    });

    /*
     * i - Switch to insert mode
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'i') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.INSERT);
        return true;
      }
    });

    /*
     * A - Jump to end of line, enter insert mode.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'A') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        int lastColumn = LineUtils.getLastCursorColumn(cursorLineInfo.line());
        selectionModel.setCursorPosition(cursorLineInfo, lastColumn);
        switchMode(Modes.INSERT);
        return true;
      }
    });

    /*
     * O - Insert line above, enter insert mode.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'O') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        Document document = getInputController().getEditor().getDocument();
        Line cursorLine = selectionModel.getCursorLine();
        int cursorLineNumber = selectionModel.getCursorLineNumber();
        document.insertText(cursorLine, 0, "\n");
        selectionModel.setCursorPosition(new LineInfo(cursorLine, cursorLineNumber), 0);
        switchMode(Modes.INSERT);
        return true;
      }
    });

    /*
     * o - Insert line below, enter insert mode.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'o') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        Document document = getInputController().getEditor().getDocument();
        Line cursorLine = selectionModel.getCursorLine();
        int cursorLineNumber = selectionModel.getCursorLineNumber();
        document.insertText(cursorLine, LineUtils.getLastCursorColumn(cursorLine), "\n");
        selectionModel.setCursorPosition(new LineInfo(cursorLine.getNextLine(),
            cursorLineNumber + 1), 0);
        switchMode(Modes.INSERT);
        return true;
      }
    });

    /*
     * : - Switch to colon capture mode for commands.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, ':') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.COMMAND_CAPTURE);
        return true;
      }
    });

    /*
     * "/" - Switch to search mode.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, '/') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.SEARCH_CAPTURE);
        return true;
      }
    });

    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, '*') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        String word =
            TextUtils.getWordAtColumn(selectionModel.getCursorLine().getText(),
                selectionModel.getCursorColumn());
        if (word == null) {
          return true;
        }
        switchMode(Modes.SEARCH_CAPTURE);
        searchTerm.append(word);
        doPartialSearch();
        drawSearchTerm();
        return true;
      }
    });

    /*
     * Movement
     */
    /*
     * ^,0 - Move to first character in line.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, '^') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        selectionModel.setCursorPosition(cursorLineInfo, 0);
        return true;
      }
    });
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, '0') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        if (tryAddNumericPrefix('0')) {
          return true;
        }
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        selectionModel.setCursorPosition(cursorLineInfo, 0);
        return true;
      }
    });

    /*
     * $ - Move to end of line.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, '$') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        Line cursorLine = selectionModel.getCursorLine();
        LineInfo cursorLineInfo = new LineInfo(cursorLine, selectionModel.getCursorLineNumber());
        selectionModel.setCursorPosition(cursorLineInfo, LineUtils.getLastCursorColumn(cursorLine));
        return true;
      }
    });

    /*
     * w - move the cursor to the first character of the next word.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'w') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        String text = selectionModel.getCursorLine().getText();
        int column = selectionModel.getCursorColumn();
        column = TextUtils.moveByWord(text, column, true, false);
        if (column == -1) {
          Line cursorLine = cursorLineInfo.line().getNextLine();
          if (cursorLine != null) {
            cursorLineInfo = new LineInfo(cursorLine, cursorLineInfo.number() + 1);
            column = 0;
          } else {
            column = LineUtils.getLastCursorColumn(cursorLine); // at last character
                                                          // in document
          }
        }

        selectionModel.setCursorPosition(cursorLineInfo, column);
        return true;
      }
    });

    /*
     * b - move the cursor to the first character of the previous word.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'b') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        String text = selectionModel.getCursorLine().getText();
        int column = selectionModel.getCursorColumn();
        column = TextUtils.moveByWord(text, column, false, false);
        if (column == -1) {
          Line cursorLine = cursorLineInfo.line().getPreviousLine();
          if (cursorLine != null) {
            cursorLineInfo = new LineInfo(cursorLine, cursorLineInfo.number() - 1);
            column = LineUtils.getLastCursorColumn(cursorLine);
          } else {
            column = 0; // at first character in document
          }
        }

        selectionModel.setCursorPosition(cursorLineInfo, column);
        return true;
      }
    });

    /*
     * e - move the cursor to the last character of the next word.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'e') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        String text = selectionModel.getCursorLine().getText();
        int column = selectionModel.getCursorColumn();
        column = TextUtils.moveByWord(text, column, true, true);
        if (column == -1) {
          Line cursorLine = cursorLineInfo.line().getNextLine();
          if (cursorLine != null) {
            cursorLineInfo = new LineInfo(cursorLine, cursorLineInfo.number() + 1);
            column = 0;
          } else {
            // at the last character in the document
            column = LineUtils.getLastCursorColumn(cursorLine);
          }
        }

        selectionModel.setCursorPosition(cursorLineInfo, column);
        return true;
      }
    });

    /*
     * % - jump to the next matching {}, [] or () character if the cursor is
     * over one of the two.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, '%') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        final SelectionModel selectionModel = getInputController().getSelection();
        Document document = getInputController().getEditor().getDocument();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        String text = selectionModel.getCursorLine().getText();
        final char cursorChar = text.charAt(selectionModel.getCursorColumn());
        final char searchChar;
        final boolean searchingForward = OPENING_GROUPS.indexOf(cursorChar) >= 0;
        final Position searchingTo;
        if (searchingForward) {
          searchChar = CLOSING_GROUPS.charAt(OPENING_GROUPS.indexOf(cursorChar));
          searchingTo =
              new Position(new LineInfo(document.getLastLine(), document.getLastLineNumber()),
                  document.getLastLine().length());
        } else if (CLOSING_GROUPS.indexOf(cursorChar) >= 0) {
          searchChar = OPENING_GROUPS.charAt(CLOSING_GROUPS.indexOf(cursorChar));
          searchingTo = new Position(new LineInfo(document.getFirstLine(), 0), 0);
        } else {
          return true; // not on a valid starting character
        }


        Position startingPosition = new Position(cursorLineInfo, selectionModel.getCursorColumn()
            + (searchingForward ? 0 : 1));
        PositionUtils.visit(new LineUtils.LineVisitor() {
          // keep a stack to match the correct corresponding bracket
          ScopeMatcher scopeMatcher = new ScopeMatcher(searchingForward, cursorChar, searchChar);
          @Override
          public boolean accept(Line line, int lineNumber, int beginColumn, int endColumn) {
            int column;
            String text = line.getText().substring(beginColumn, endColumn);
            column = scopeMatcher.searchNextLine(text);
            if (column >= 0) {
              selectionModel
                  .setCursorPosition(new LineInfo(line, lineNumber), column + beginColumn);
              return false;
            }
            return true;
          }
        }, startingPosition, searchingTo);
        return true;
      }
    });

    /*
     * } - next paragraph.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, '}') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        int lineNumber = cursorLineInfo.number();
        boolean skippingEmptyLines = true;
        Line line;
        for (line = cursorLineInfo.line(); line.getNextLine() != null; line = line.getNextLine(),
            lineNumber++) {
          String text = line.getText();
          text = text.substring(0, text.length() - (text.endsWith("\n") ? 1 : 0));
          boolean isEmptyLine = text.trim().length() > 0;
          if (skippingEmptyLines) {
            // check if this line is empty
            if (isEmptyLine) {
              skippingEmptyLines = false; // non-empty line
            }
          } else {
            // check if this line is not empty
            if (!isEmptyLine) {
              break;
            }
          }
        }
        selectionModel.setCursorPosition(new LineInfo(line, lineNumber), 0);
        return true;
      }
    });

    /*
     * TODO: merge both paragraph searching blocks together.
     */
    /*
     * { - previous paragraph.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, '{') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getSelection();
        LineInfo cursorLineInfo =
            new LineInfo(selectionModel.getCursorLine(), selectionModel.getCursorLineNumber());
        int lineNumber = cursorLineInfo.number();
        boolean skippingEmptyLines = true;
        Line line;
        for (line = cursorLineInfo.line(); line.getPreviousLine() != null; line =
            line.getPreviousLine(), lineNumber--) {
          String text = line.getText();
          text = text.substring(0, text.length() - (text.endsWith("\n") ? 1 : 0));
          if (skippingEmptyLines) {
            // check if this line is empty
            if (text.trim().length() > 0) {
              skippingEmptyLines = false; // non-empty line
            }
          } else {
            // check if this line is not empty
            if (text.trim().length() > 0) {
              // not empty, continue
            } else {
              break;
            }
          }
        }
        selectionModel.setCursorPosition(new LineInfo(line, lineNumber), 0);
        return true;
      }
    });

    /*
     * Cmd+u - page up.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'u') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        getInputController().getSelection().move(MoveAction.PAGE_UP, inVisualMode);
        return true;
      }
    });

    /*
     * Cmd+d - page down.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'd') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        getInputController().getSelection().move(MoveAction.PAGE_DOWN, inVisualMode);
        return true;
      }
    });

    /*
     * Ngg - move cursor to line N, or first line by default.
     */
    commandMode.addShortcut(new StreamShortcut("gg") {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        moveCursorToLine(getPrefixValue(), true);
        return true;
      }
    });

    /*
     * NG - move cursor to line N, or last line by default.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'G') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        moveCursorToLine(getPrefixValue(), false);
        return true;
      }
    });

    /*
     * Text manipulation
     */
    /*
     * x - Delete one character to right of cursor, or the current selection.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'x') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().deleteCharacter(true);
        return true;
      }
    });

    /*
     * X - Delete one character to left of cursor.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'X') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().deleteCharacter(false);
        return true;
      }
    });

    /*
     * p - Paste after cursor.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'p') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        if (clipboard != null && clipboard.length() > 0) {
          handlePaste(clipboard, true);
        }
        return true;
      }
    });

    /*
     * P - Paste before cursor.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'P') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        if (clipboard != null && clipboard.length() > 0) {
          handlePaste(clipboard, false);
        }
        return true;
      }
    });

    /*
     * Nyy - Copy N lines. If there is already a selection, copy that instead.
     */
    commandMode.addShortcut(new StreamShortcut("yy") {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        if (selectionModel.hasSelection()) {
          isLineCopy = (visualMoveUnit == MoveUnit.LINE);
        } else {
          int numLines = getPrefixValue();
          if (numLines <= 0) {
            numLines = 1;
          }
          selectNextNLines(numLines);
          isLineCopy = false;
        }

        Preconditions.checkState(selectionModel.hasSelection());
        getInputController().prepareForCopy();
        Position[] selectionRange = selectionModel.getSelectionRange(true);
        clipboard =
            LineUtils.getText(selectionRange[0].getLine(), selectionRange[0].getColumn(),
                selectionRange[1].getLine(), selectionRange[1].getColumn());
        selectionModel.deselect();
        switchMode(Modes.COMMAND);
        return false;
      }
    });

    /*
     * Ndd - Cut N lines.
     */
    commandMode.addShortcut(new StreamShortcut("dd") {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        int numLines = getPrefixValue();
        if (numLines <= 0) {
          numLines = 1;
        }
        SelectionModel selectionModel = getInputController().getEditor().getSelection();
        selectNextNLines(numLines);

        Preconditions.checkState(selectionModel.hasSelection());
        getInputController().prepareForCopy();
        Position[] selectionRange = selectionModel.getSelectionRange(true);
        clipboard =
            LineUtils.getText(selectionRange[0].getLine(), selectionRange[0].getColumn(),
                selectionRange[1].getLine(), selectionRange[1].getColumn());
        selectionModel.deleteSelection(getInputController().getEditorDocumentMutator());
        return false;
      }
    });

    /*
     * >> - indent line.
     */
    commandMode.addShortcut(new StreamShortcut(">>") {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().indentSelection();
        return true;
      }
    });

    /*
     * << - dedent line.
     */
    commandMode.addShortcut(new StreamShortcut("<<") {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().dedentSelection();
        return true;
      }
    });

    /*
     * u - Undo.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'u') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().getEditor().undo();
        return true;
      }
    });

    /*
     * ACTION+r - Redo.
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, 'r') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().getEditor().redo();
        return true;
      }
    });

    /**
     * n - next search match
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'n') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        doSearch(true);
        return true;
      }
    });

    /**
     * N - previous search match
     */
    commandMode.addShortcut(new EventShortcut(ModifierKeys.NONE, 'N') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        doSearch(false);
        return true;
      }
    });

    insertMode = new InputMode() {
      @Override
      public void setup() {
        setStatus("-- INSERT --");
      }

      @Override
      public void teardown() {
      }

      @Override
      public boolean onDefaultInput(SignalEvent signal, char character) {
        int letter = KeyCodeMap.getKeyFromEvent(signal);

        int modifiers = ModifierKeys.computeModifiers(signal);
        modifiers = modifiers & ~ModifierKeys.SHIFT;
        int strippedKeyDigest = CharCodeWithModifiers.computeKeyDigest(modifiers, letter);
        JsoIntMap<MoveAction> movementKeysMapping = DefaultScheme.MOVEMENT_KEYS_MAPPING;
        if (movementKeysMapping.containsKey(strippedKeyDigest)) {
          MoveAction action = movementKeysMapping.get(strippedKeyDigest);
          getScheme().getInputController().getSelection().move(action, false);
          return true;
        }

        InputController input = getInputController();
        SelectionModel selection = input.getSelection();
        int column = selection.getCursorColumn();

        if (!signal.getCommandKey() && KeyCodeMap.isPrintable(letter)) {
          String text = Character.toString((char) letter);

          // insert a single character
          input.getEditorDocumentMutator().insertText(selection.getCursorLine(),
              selection.getCursorLineNumber(), column, text);
          return true;
        }
        return false; // let it fall through
      }

      @Override
      public boolean onDefaultPaste(SignalEvent signal, String text) {
        return false;
      }
    };
    addMode(Modes.INSERT, insertMode);

    /*
     * ESC, ACTION+[ - Switch to command mode.
     */
    insertMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.ESC) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.COMMAND);
        return true;
      }
    });
    insertMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, '[') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.COMMAND);
        return true;
      }
    });

    /*
     * BACKSPACE - Native behavior.
     */
    insertMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.BACKSPACE) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        getInputController().deleteCharacter(false);
        return true;
      }
    });

    /*
     * DELETE - Native behavior.
     */
    insertMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.DELETE) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        getInputController().deleteCharacter(true);
        return true;
      }
    });

    /*
     * TAB - Insert a tab.
     */
    insertMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.TAB) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        getInputController().handleTab();
        return true;
      }
    });

    commandCaptureMode = new InputMode() {
      @Override
      public void setup() {
        command = new StringBuilder();
        setStatus("-- COMMAND CAPTURE --");
      }

      @Override
      public void teardown() {
      }

      @Override
      public boolean onDefaultInput(SignalEvent signal, char character) {
        int letter = KeyCodeMap.getKeyFromEvent(signal);
        if (KeyCodeMap.isPrintable(letter)) {
          command.append(Character.toString((char) letter));
          drawCommandTerm();
        }
        return false;
      }

      @Override
      public boolean onDefaultPaste(SignalEvent signal, String text) {
        return false;
      }
    };

    addMode(Modes.COMMAND_CAPTURE, commandCaptureMode);

    /*
     * ESC, ACTION+[ - Exit on escape back to command mode.
     */
    commandCaptureMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.ESC) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.COMMAND);
        return true;
      }
    });

    commandCaptureMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, '[') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        switchMode(Modes.COMMAND);
        return true;
      }
    });

    /*
     * ENTER - Do the command, then exit to command mode.
     */
    commandCaptureMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.ENTER) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        doColonCommand(command);
        switchMode(Modes.COMMAND);
        return true;
      }
    });

    searchCaptureMode = new InputMode() {
      @Override
      public void setup() {
        searchTerm = new StringBuilder();
        setStatus("-- SEARCH --");
      }

      @Override
      public void teardown() {
      }

      @Override
      public boolean onDefaultInput(SignalEvent signal, char character) {
        int letter = KeyCodeMap.getKeyFromEvent(signal);
        if (KeyCodeMap.isPrintable(letter)) {
          searchTerm.append(Character.toString((char) letter));
          doPartialSearch();
          drawSearchTerm();
        }
        return false;
      }

      @Override
      public boolean onDefaultPaste(SignalEvent signal, String text) {
        return false;
      }
    };

    addMode(Modes.SEARCH_CAPTURE, searchCaptureMode);

    /*
     * ESC, ACTION+[ - Exit on escape back to command mode, and clear any
     * highlights.
     */
    searchCaptureMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.ESC) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().getEditor().getSearchModel().setQuery("");
        switchMode(Modes.COMMAND);
        return true;
      }
    });

    searchCaptureMode.addShortcut(new EventShortcut(ModifierKeys.ACTION, '[') {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        scheme.getInputController().getEditor().getSearchModel().setQuery("");
        switchMode(Modes.COMMAND);
        return true;
      }
    });

    /*
     * ENTER - Do the command, then exit to command mode.
     */
    searchCaptureMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.ENTER) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        /*
         * TODO: There is a bug when switching modes that erases the
         * current selection, when we get to actually working on vim support I
         * should track this down.
         */
        switchMode(Modes.COMMAND);
        return true;
      }
    });

    /*
     * BACKSPACE - Remove the last character from the searchTerm.
     */
    searchCaptureMode.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.BACKSPACE) {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        if (searchTerm.length() > 0) {
          searchTerm.deleteCharAt(searchTerm.length() - 1);
          doPartialSearch();
          drawSearchTerm();
        }
        return true;
      }
    });
  }

  private JsoIntMap<MoveAction> getMovementKeysMapping() {
    if (!inVisualMode) {
      return DefaultScheme.MOVEMENT_KEYS_MAPPING;
    }
    if (visualMoveUnit == MoveUnit.LINE) {
      return LINE_MOVEMENT_KEYS_MAPPING;
    } else {
      return CHAR_MOVEMENT_KEYS_MAPPING;
    }
  }

  private void setStatus(String text) {
    statusHeader.setTextContent(text);
  }

  private void drawSearchTerm() {
    setStatus("Search: '" + searchTerm + "'");
  }

  private void drawCommandTerm() {
    setStatus("Command: '" + command + "'");
  }

  private void doColonCommand(StringBuilder command) {
    if (command.equals("q")) {

    }
  }

  /**
   * Display search matches for the currently entered searchTerm, but don't move
   * the cursor.
   */
  private void doPartialSearch() {
    getInputController().getEditor().getSearchModel().setQuery(searchTerm.toString());
  }

  private void doSearch(boolean searchNext) {
    SearchModel model = getInputController().getEditor().getSearchModel();

    if (StringUtils.isNullOrEmpty(model.getQuery())) {
      return;
    }
    
    if (searchNext) {
      model.getMatchManager().selectNextMatch();
    } else {
      model.getMatchManager().selectPreviousMatch();
    }
  }

  private void selectNextNLines(int numLines) {
    SelectionModel selectionModel = getInputController().getEditor().getSelection();
    Document document = getInputController().getEditor().getDocument();
    Line cursorLine = selectionModel.getCursorLine();
    int cursorLineNumber = selectionModel.getCursorLineNumber();
    LineInfo cursorLineInfo = new LineInfo(cursorLine, cursorLineNumber);
    LineInfo endLineInfo;
    if (cursorLineNumber + numLines > document.getLastLineNumber()) {
      endLineInfo = new LineInfo(document.getLastLine(), document.getLastLineNumber());
    } else {
      endLineInfo =
          cursorLine.getDocument().getLineFinder()
              .findLine(cursorLineInfo, cursorLineNumber + numLines);
    }

    selectionModel.setSelection(cursorLineInfo, 0, endLineInfo, 0);
  }

  /**
   * Jump to lineNumber (1-based). If requested number is invalid, either go to
   * the first line or the last line, depending upon defaultToFirstLine.
   */
  private void moveCursorToLine(int lineNumber, boolean defaultToFirstLine) {
    Document document = getInputController().getEditor().getDocument();
    SelectionModel selectionModel = getInputController().getEditor().getSelection();
    LineInfo targetLineInfo;
    if (lineNumber > document.getLastLineNumber() + 1 || lineNumber <= 0) {
      if (defaultToFirstLine) {
        targetLineInfo = new LineInfo(document.getFirstLine(), 0);
      } else {
        targetLineInfo = new LineInfo(document.getLastLine(), document.getLastLineNumber());
      }
    } else {
      Line cursorLine = selectionModel.getCursorLine();
      int cursorLineNumber = selectionModel.getCursorLineNumber();
      LineInfo cursorLineInfo = new LineInfo(cursorLine, cursorLineNumber);
      targetLineInfo =
          cursorLine.getDocument().getLineFinder().findLine(cursorLineInfo, lineNumber - 1);
    }
    selectionModel.setCursorPosition(targetLineInfo, 0);
  }

  /**
   * Return the prefix value, or -1 if there is no valid prefix.
   */
  private int getPrefixValue() {
    if (numericPrefixText.length() == 0) {
      return -1;
    }
    return Integer.parseInt(numericPrefixText.toString());
  }

  /**
   * Try to append character to the current numeric prefix if it is a number and
   * not a leading 0.
   *
   * @param character
   * @return True if the character was added.
   */
  private boolean tryAddNumericPrefix(char character) {
    // if this is a number, keep track of it to do Ndd-type commands
    if (('0' < character && character <= '9') || (character == '0'
        && numericPrefixText.length() > 0)) {
      numericPrefixText.append(character);
      return true;
    }
    return false;
  }

  private void switchMode(Modes newMode) {
    super.switchMode(newMode.ordinal());
  }

  private void addMode(Modes modeId, InputMode mode) {
    super.addMode(modeId.ordinal(), mode);
  }

  private void handlePaste(String text, boolean isPasteAfter) {
    SelectionModel selection = getInputController().getSelection();
    int lineNumber = selection.getCursorLineNumber();
    Line line = selection.getCursorLine();
    if (isLineCopy) {
      // multi-line paste, insert on new line above or below
      if (!isPasteAfter) {
        // insert text before the cursor line
        getInputController().getEditorDocumentMutator().insertText(line, lineNumber, 0, text);
      } else {
        // insert at end of current line (before \n)
        text = "\n" + text.substring(0, text.length() - (text.endsWith("\n") ? 1 : 0));
        getInputController().getEditorDocumentMutator().insertText(line, lineNumber,
            LineUtils.getLastCursorColumn(line), text);
      }
    } else {
      // not a full-line paste, act normally
      getInputController().getEditorDocumentMutator().insertText(line, lineNumber,
          selection.getCursorColumn(), text);
    }
  }

  @Override
  public void setup() {
    /*
     * TODO: create some sort of mode status display area for
     * current mode and stream input.
     *
     * TODO: Long term move this display area into the awesome bar.
     */
    statusHeader = Elements.createDivElement();
    statusHeader.getStyle().setPosition(CSSStyleDeclaration.Position.ABSOLUTE);
    statusHeader.getStyle().setBottom(0, CSSStyleDeclaration.Unit.PX);
    statusHeader.getStyle().setLeft(50, CSSStyleDeclaration.Unit.PCT);
    statusHeader.getStyle().setFontWeight(CSSStyleDeclaration.FontWeight.BOLD);
    Elements.getBody().appendChild(statusHeader);
    switchMode(Modes.COMMAND);
  }

  @Override
  public void teardown() {
    super.teardown();
    statusHeader.removeFromParent();
  }

  /**
   * Undo any shortcut-specific state (prefix values, etc).
   */
  @Override
  public void handleShortcutCalled() {
    numericPrefixText.setLength(0);
  }
}
