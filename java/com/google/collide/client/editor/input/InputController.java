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

import com.google.collide.client.document.linedimensions.LineDimensionsUtils;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.Editor.KeyListener;
import com.google.collide.client.editor.Editor.NativeKeyUpListener;
import com.google.collide.client.editor.Editor.ReadOnlyListener;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.BrowserUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.SignalEventUtils;
import com.google.collide.client.util.logging.Log;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.DocumentMutator;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.TextUtils;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.TextEvent;
import elemental.html.Element;
import elemental.html.TextAreaElement;

/**
 * Controller for taking input from the user. This manages an offscreen textarea
 * that receives the user's entered text.
 *
 * The lifecycle of this class is tied to the editor that owns it.
 *
 */
public class InputController {

  // TODO: move to elemental
  private static final String EVENT_TEXTINPUT = "textInput";

  final InputScheme nativeScheme;
  final InputScheme vimScheme;

  private Document document;
  private Editor editor;
  private DocumentMutator editorDocumentMutator;
  private final TextAreaElement inputElement;
  private InputScheme activeInputScheme = null;
  private final ListenerManager<KeyListener> keyListenerManager = ListenerManager.create();
  private final ListenerManager<NativeKeyUpListener> nativeKeyUpListenerManager = ListenerManager
      .create();
  private SelectionModel selection;
  private ViewportModel viewport;
  private final RootActionExecutor actionExecutor;

  public InputController() {
    inputElement = createInputElement();
    actionExecutor = new RootActionExecutor();
    nativeScheme = new DefaultScheme(this);
    vimScheme = new VimScheme(this);
  }

  public Document getDocument() {
    return document;
  }

  public Editor getEditor() {
    return editor;
  }

  public DocumentMutator getEditorDocumentMutator() {
    return editorDocumentMutator;
  }

  public Element getInputElement() {
    return inputElement;
  }

  public String getInputText() {
    return inputElement.getValue();
  }

  public ListenerRegistrar<KeyListener> getKeyListenerRegistrar() {
    return keyListenerManager;
  }

  public ListenerRegistrar<NativeKeyUpListener> getNativeKeyUpListenerRegistrar() {
    return nativeKeyUpListenerManager;
  }

  public SelectionModel getSelection() {
    return selection;
  }

  public void handleDocumentChanged(Document document, SelectionModel selection,
      ViewportModel viewport) {
    this.document = document;
    this.selection = selection;
    this.viewport = viewport;
  }

  public void initializeFromEditor(Editor editor, DocumentMutator editorDocumentMutator) {
    this.editor = editor;
    this.editorDocumentMutator = editorDocumentMutator;

    editor.getReadOnlyListenerRegistrar().add(new ReadOnlyListener() {
      @Override
      public void onReadOnlyChanged(boolean isReadOnly) {
        handleReadOnlyChanged(isReadOnly);
      }
    });

    handleReadOnlyChanged(editor.isReadOnly());
  }

  private void handleReadOnlyChanged(boolean isReadOnly) {
    if (isReadOnly) {
      setActiveInputScheme(new ReadOnlyScheme(this));
    } else {
      setActiveInputScheme(nativeScheme);
    }
  }

  public void setActiveInputScheme(InputScheme inputScheme) {
    if (this.activeInputScheme != null) {
      this.activeInputScheme.teardown();
    }
    this.activeInputScheme = inputScheme;
    this.activeInputScheme.setup();
  }

  public void setInputText(String text) {
    inputElement.setValue(text);
  }

  public void setSelection(SelectionModel selection) {
    this.selection = selection;
  }

  boolean dispatchKeyPress(final SignalEvent signalEvent) {
    class KeyDispatcher implements Dispatcher<KeyListener> {
      boolean handled;

      @Override
      public void dispatch(KeyListener listener) {
        handled |= listener.onKeyPress(signalEvent);
      }
    }

    KeyDispatcher keyDispatcher = new KeyDispatcher();
    keyListenerManager.dispatch(keyDispatcher);

    return keyDispatcher.handled;
  }

  boolean dispatchKeyUp(final Event event) {
    class NativeKeyUpDispatcher implements Dispatcher<Editor.NativeKeyUpListener> {
      boolean handled;

      @Override
      public void dispatch(NativeKeyUpListener listener) {
        handled |= listener.onNativeKeyUp(event);
      }
    }

    NativeKeyUpDispatcher nativeKeyUpDispatcher = new NativeKeyUpDispatcher();
    nativeKeyUpListenerManager.dispatch(nativeKeyUpDispatcher);

    return nativeKeyUpDispatcher.handled;
  }

  private TextAreaElement createInputElement() {
    final TextAreaElement inputElement = Elements.createTextAreaElement();

    // Ensure it is offscreen
    inputElement.getStyle().setPosition(CSSStyleDeclaration.Position.ABSOLUTE);
    inputElement.getStyle().setLeft("-100000px");
    inputElement.getStyle().setTop("0");
    inputElement.getStyle().setHeight("1px");
    inputElement.getStyle().setWidth("1px");
    /*
     * Firefox doesn't seem to respect just the NOWRAP value, so we need to set
     * the legacy wrap attribute.
     */
    inputElement.setAttribute("wrap", "off");

    // Attach listeners
    /*
     * For text events, call inputHandler.handleInput(event, text) if the text
     * entered was > 1 character -> from a paste event. This gets fed directly
     * into the document. Single keypresses all get captured by signalEventListener
     * and passed through the shortcut system.
     *
     * TODO: This isn't actually true, there could be paste events
     * of only one character. Change this to check if the event was a clipboard
     * event.
     */
    inputElement.addEventListener(EVENT_TEXTINPUT, new EventListener() {
      @Override
      public void handleEvent(Event event) {
        /*
         * TODO: figure out best event to listen to. Tried "input",
         * but see http://code.google.com/p/chromium/issues/detail?id=76516
         */
        String text = ((TextEvent) event).getData();
        if (text.length() <= 1) {
          return;
        }
        setInputText("");
        activeInputScheme.handleEvent(SignalEventUtils.create(event), text);
      }
    }, false);

    if (BrowserUtils.isFirefox()) {
      inputElement.addEventListener(Event.INPUT, new EventListener() {
        @Override
        public void handleEvent(Event event) {
          /*
           * TODO: FF doesn't support textInput, and Chrome's input
           * is buggy.
           */
          String text = getInputText();
          if (text.length() <= 1) {
            return;
          }
          setInputText("");

          activeInputScheme.handleEvent(SignalEventUtils.create(event), text);

          event.preventDefault();
          event.stopPropagation();
        }
      }, false);
    }

    EventListener signalEventListener = new EventListener() {
      @Override
      public void handleEvent(Event event) {
        SignalEvent signalEvent = SignalEventUtils.create(event);
        if (signalEvent != null) {
          processSignalEvent(signalEvent);
        } else if ("keyup".equals(event.getType())) {
          boolean handled = dispatchKeyUp(event);
          if (handled) {
            // Prevent any browser handling.
            event.preventDefault();
            event.stopPropagation();
          }
        }
      }
    };

    /*
     * Attach to all of key events, and the SignalEvent logic will filter
     * appropriately
     */
    inputElement.addEventListener(Event.KEYDOWN, signalEventListener, false);
    inputElement.addEventListener(Event.KEYPRESS, signalEventListener, false);
    inputElement.addEventListener(Event.KEYUP, signalEventListener, false);
    inputElement.addEventListener(Event.COPY, signalEventListener, false);
    inputElement.addEventListener(Event.PASTE, signalEventListener, false);
    inputElement.addEventListener(Event.CUT, signalEventListener, false);

    return inputElement;
  }

  @VisibleForTesting
  public void processSignalEvent(SignalEvent signalEvent) {
    boolean handled = dispatchKeyPress(signalEvent);

    if (!handled) {
      if (signalEvent.isCopyEvent() || signalEvent.isCutEvent()) {
        prepareForCopy();
        if (signalEvent.isCutEvent() && selection.hasSelection()) {
          selection.deleteSelection(editorDocumentMutator);
        }

        // These events are special cased, nothing else should happen.
        return;
      }

      /*
       * Send all keypresses through here.
       */
      try {
        handled = activeInputScheme.handleEvent(signalEvent, "");
      } catch (Throwable t) {
        Log.error(getClass(), t);
      }
    }

    if (handled) {
      // Prevent any browser handling.
      signalEvent.preventDefault();
      signalEvent.stopPropagation();
      setInputText("");
    }
  }

  public void prepareForCopy() {
    if (!selection.hasSelection()) {
      // TODO: Discuss Ctrl-X feature.
      return;
    }

    Position[] selectionRange = selection.getSelectionRange(true);
    String selectionText = LineUtils.getText(
        selectionRange[0].getLine(), selectionRange[0].getColumn(),
        selectionRange[1].getLine(), selectionRange[1].getColumn());
    setInputText(selectionText);
    inputElement.select();

    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      @Override
      public void execute() {
        /*
         * The text has been copied by now, so clear it (if the text was large,
         * it would cause slow layout)
         */
        setInputText("");
      }
    });
  }

  /**
   * Add a tab character to the beginning of each line in the current selection,
   * or at the current cursor position if no text is selected.
   */
  // TODO: This should probably be a setting, tabs or spaces
  public void handleTab() {
    if (selection.hasMultilineSelection()) {
      indentSelection();
    } else {
      getEditorDocumentMutator().insertText(selection.getCursorLine(),
          selection.getCursorLineNumber(), selection.getCursorColumn(),
          LineDimensionsUtils.getTabAsSpaces());
    }
  }

  public void indentSelection() {
    selection.adjustSelectionIndentation(
        editorDocumentMutator, LineDimensionsUtils.getTabAsSpaces(), true);
  }

  /**
   * Removes the indentation from the beginning of each line of a multiline
   * selection.
   */
  public void dedentSelection() {
    selection.adjustSelectionIndentation(
        editorDocumentMutator, LineDimensionsUtils.getTabAsSpaces(), false);
  }

  /**
   * Delete a character around the current cursor, and take care of joining lines
   * together if the delete removes a newline. This is used to implement backspace
   * and delete, depending upon the afterCursor argument.
   *
   * @param afterCursor if true, delete the character to the right of the cursor
   */
  public void deleteCharacter(boolean afterCursor) {
    if (tryDeleteSelection()) {
      return;
    }

    Line cursorLine = selection.getCursorLine();
    int cursorLineNumber = selection.getCursorLineNumber();
    int deleteColumn = !afterCursor ? selection.getCursorColumn() - 1 : selection.getCursorColumn();
    if (cursorLine.hasColumn(deleteColumn)) {
      getEditorDocumentMutator().deleteText(cursorLine, cursorLineNumber, deleteColumn, 1);
    } else if (deleteColumn < 0 && cursorLine.getPreviousLine() != null) {
      // Join the lines
      Line previousLine = cursorLine.getPreviousLine();
      getEditorDocumentMutator().deleteText(previousLine, cursorLineNumber - 1,
          previousLine.getText().length() - 1, 1);
    }
  }

  public void deleteWord(boolean afterCursor) {
    if (tryDeleteSelection()) {
      return;
    }

    Line cursorLine = selection.getCursorLine();
    int cursorColumn = selection.getCursorColumn();

    boolean mergeWithPreviousLine = cursorColumn == 0 && !afterCursor;
    boolean mergeWithNextLine = cursorColumn == cursorLine.length() - 1 && afterCursor;
    if (mergeWithPreviousLine || mergeWithNextLine) {
      // Re-use delete character logic
      deleteCharacter(afterCursor);
      return;
    }

    int otherColumn =
        afterCursor ? TextUtils.findNextWord(cursorLine.getText(), cursorColumn, true) : TextUtils
            .findPreviousWord(cursorLine.getText(), cursorColumn, false);
    editorDocumentMutator.deleteText(cursorLine, Math.min(otherColumn, cursorColumn),
        Math.abs(otherColumn - cursorColumn));
  }

  private boolean tryDeleteSelection() {
    if (selection.hasSelection()) {
      selection.deleteSelection(editorDocumentMutator);
      return true;
    } else {
      return false;
    }
  }

  ViewportModel getViewportModel() {
    return viewport;
  }

  public RootActionExecutor getActionExecutor() {
    return actionExecutor;
  }
}
