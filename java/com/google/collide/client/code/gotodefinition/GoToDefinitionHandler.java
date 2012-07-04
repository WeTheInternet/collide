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

package com.google.collide.client.code.gotodefinition;

import com.google.collide.client.code.popup.EditorPopupController;
import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.input.CommonActions;
import com.google.collide.client.editor.input.DefaultActionExecutor;
import com.google.collide.client.editor.input.InputScheme;
import com.google.collide.client.editor.input.RootActionExecutor;
import com.google.collide.client.editor.input.Shortcut;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.history.Place;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.FileTreeModel;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;
import com.google.gwt.event.dom.client.KeyCodes;

import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeyModifier;
import org.waveprotocol.wave.client.common.util.UserAgent;

import elemental.events.Event;

/**
 * Implementation of the "go to definition" feature.
 *
 */
public class GoToDefinitionHandler extends DefaultActionExecutor {

  private static final int COMMAND_KEY_CODE = 91;

  private final Editor editor;
  private final FileTreeModel fileTreeModel;
  private final GoToDefinitionRenderer referenceRenderer;

  private final Shortcut gotoDefinitionAction = new Shortcut() {
    @Override
    public boolean event(InputScheme scheme, SignalEvent event) {
      goToDefinitionAtCurrentCaretPosition();
      return true;
    }
  };

  private LineFinder lineFinder;

  /**
   * Flag which specifies that object is "activated".
   *
   * <p>Activated object plugs renderer and listens to mouse to highlight
   * hovered items.
   */
  private boolean activated;
  private ListenerRegistrar.Remover mouseMoveListenerRemover;
  private ListenerRegistrar.Remover mouseClickListenerRemover;
  private ListenerRegistrar.Remover nativeKeyUpListenerRemover;

  /**
   * Flag which specifies that object is "attached" to editor.
   *
   * <p>Registered object listens for keypeseesd (to become activated) and
   * provides actions.
   */
  private boolean registered;
  private ListenerRegistrar.Remover keyListenerRemover;
  private RootActionExecutor.Remover actionRemover;

  private ReferenceStore referenceStore;
  private ReferenceNavigator referenceNavigator;
  private AnchorTagParser anchorParser;

  private static boolean isActionOnlyKey(SignalEvent signal) {
    // When modifier key is pressed on Mac it is instantly applied to modifiers.
    // So we check on Mac that only "CMD" is pressed.
    // On non-Mac when "CTRL" is the first key in sequence, there are no
    // modifiers applied yet.
    KeyModifier modifier = UserAgent.isMac() ? KeyModifier.META : KeyModifier.NONE;
    return modifier.check(signal) && (signal.getKeyCode() == getActionKeyCode());
  }

  private static int getActionKeyCode() {
    return UserAgent.isMac() ? COMMAND_KEY_CODE : KeyCodes.KEY_CTRL;
  }

  private final Buffer.MouseMoveListener mouseMoveListener = new Buffer.MouseMoveListener() {
    @Override
    public void onMouseMove(int x, int y) {
      NavigableReference reference = findReferenceAtMousePosition(x, y, false);
      referenceRenderer.highlightReference(reference, editor.getRenderer(), lineFinder);
    }
  };

  private final Buffer.MouseClickListener mouseClickListener = new Buffer.MouseClickListener() {
    @Override
    public void onMouseClick(int x, int y) {
      NavigableReference reference = findReferenceAtMousePosition(x, y, true);
      if (reference != null) {
        navigateReference(reference);
      }
    }
  };

  private final Editor.NativeKeyUpListener keyUpListener = new Editor.NativeKeyUpListener() {
    @Override
    public boolean onNativeKeyUp(Event event) {
      com.google.gwt.user.client.Event gwtEvent = (com.google.gwt.user.client.Event) event;
      if (gwtEvent.getKeyCode() == getActionKeyCode()) {
        setActivated(false);
        return true;
      } else {
        return false;
      }
    }
  };

  private final Editor.KeyListener keyListener = new Editor.KeyListener() {

    @Override
    public boolean onKeyPress(SignalEvent signal) {
      boolean shouldActivate = isActionOnlyKey(signal);
      setActivated(shouldActivate);
      return shouldActivate;
    }
  };

  public GoToDefinitionHandler(Place currentPlace, Editor editor, FileTreeModel fileTreeModel,
      GoToDefinitionRenderer.Resources resources, CubeClient cubeClient,
      EditorPopupController popupController) {
    Preconditions.checkNotNull(editor);
    Preconditions.checkNotNull(cubeClient);

    this.editor = editor;
    this.fileTreeModel = fileTreeModel;
    this.referenceRenderer = new GoToDefinitionRenderer(resources, editor, popupController);
    this.referenceNavigator = new ReferenceNavigator(currentPlace, editor);

    addAction(CommonActions.GOTO_DEFINITION, gotoDefinitionAction);
    addAction(CommonActions.GOTO_SOURCE, gotoDefinitionAction);

    referenceStore = new ReferenceStore(cubeClient);
  }

  public void editorContentsReplaced(PathUtil filePath, DocumentParser parser) {
    setActivated(false);
    if (anchorParser != null) {
      anchorParser.cleanup();
      anchorParser = null;
    }
    DynamicReferenceProvider dynamicReferenceProvider = null;
    if (isGoToDefinitionSupported(filePath)) {
      if (StringUtils.endsWithIgnoreCase(filePath.getPathString(), ".html")) {
        anchorParser = new AnchorTagParser(parser);
      }
      DeferringLineParser deferringParser = new DeferringLineParser(parser);
      dynamicReferenceProvider = new DynamicReferenceProvider(
          filePath.getPathString(), deferringParser, fileTreeModel, anchorParser);
      this.referenceNavigator.setCurrentFilePath(filePath.getPathString());
      this.lineFinder = editor.getDocument().getLineFinder();
      setRegistered(true);
    } else {
      setRegistered(false);
    }
    this.referenceStore.onDocumentChanged(editor.getDocument(), dynamicReferenceProvider);
  }

  /**
   * Registers or unregisters keyListener, actions, etc.
   */
  private void setRegistered(boolean registered) {
    if (registered == this.registered) {
      return;
    }
    if (registered) {
      keyListenerRemover = editor.getKeyListenerRegistrar().add(keyListener);
      actionRemover = editor.getInput().getActionExecutor().addDelegate(this);
    } else {
      keyListenerRemover.remove();
      keyListenerRemover = null;
      actionRemover.remove();
      actionRemover = null;
    }

    this.registered = registered;
  }

  private void setActivated(boolean activated) {
    if (activated == this.activated) {
      return;
    }

    if (activated) {
      mouseMoveListenerRemover =
          editor.getBuffer().getMouseMoveListenerRegistrar().add(mouseMoveListener);
      mouseClickListenerRemover =
          editor.getBuffer().getMouseClickListenerRegistrar().add(mouseClickListener);
      nativeKeyUpListenerRemover =
          editor.getNativeKeyUpListenerRegistrar().add(keyUpListener);
      editor.addLineRenderer(referenceRenderer);
    } else {
      mouseMoveListenerRemover.remove();
      mouseMoveListenerRemover = null;
      mouseClickListenerRemover.remove();
      mouseClickListenerRemover = null;
      nativeKeyUpListenerRemover.remove();
      nativeKeyUpListenerRemover = null;
      referenceRenderer.resetReferences(editor.getRenderer(), lineFinder);
      editor.removeLineRenderer(referenceRenderer);
    }
    this.activated = activated;
  }

  private static boolean isGoToDefinitionSupported(PathUtil filePath) {
    String pathString = filePath.getPathString();
    return StringUtils.endsWithIgnoreCase(pathString, ".js")
        || StringUtils.endsWithIgnoreCase(pathString, ".html")
        || StringUtils.endsWithIgnoreCase(pathString, ".py");
  }

  private void goToDefinitionAtCurrentCaretPosition() {
    // TODO: Check here that our code model is fresh enough.
    // int caretOffset = getCaretOffset(editor.getWidget().getElement());
    SelectionModel selection = editor.getSelection();
    LineInfo lineInfo = new LineInfo(selection.getCursorLine(), selection.getCursorLineNumber());
    NavigableReference reference =
        referenceStore.findReference(lineInfo, selection.getCursorColumn(), true);
    navigateReference(reference);
  }

  private void navigateReference(NavigableReference reference) {
    if (reference == null) {
      return;
    }
    setActivated(false);
    reference.navigate(referenceNavigator);
  }

  private NavigableReference findReferenceAtMousePosition(int x, int y, boolean blocking) {
    int lineNumber = editor.getBuffer().convertYToLineNumber(y, true);
    LineInfo lineInfo = lineFinder.findLine(lineNumber);
    int column = editor.getBuffer().convertXToRoundedVisibleColumn(x, lineInfo.line());
    return referenceStore.findReference(lineInfo, column, blocking);
  }

  public void cleanup() {
    referenceStore.cleanup();
    if (anchorParser != null) {
      anchorParser.cleanup();
    }
    setActivated(false);
    setRegistered(false);
  }
}
