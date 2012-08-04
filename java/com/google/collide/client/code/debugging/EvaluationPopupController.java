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

package com.google.collide.client.code.debugging;

import javax.annotation.Nullable;

import com.google.collide.client.code.debugging.DebuggerApiTypes.OnEvaluateExpressionResponse;
import com.google.collide.client.code.popup.EditorPopupController;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.MouseHoverManager;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;

/**
 * Controller for the debugger evaluation expression popup.
 */
public class EvaluationPopupController {

  public interface Css extends CssResource {
    String popupAnchor();
  }

  public interface Resources extends ClientBundle, RemoteObjectTree.Resources {
    @Source("EvaluationPopupController.css")
    Css evaluationPopupControllerCss();
  }

  static EvaluationPopupController create(Resources resources, Editor editor,
      EditorPopupController popupController, DebuggerState debuggerState) {
    return new EvaluationPopupController(resources, editor, popupController, debuggerState);
  }

  private class DebuggerListenerImpl implements DebuggerState.DebuggerStateListener,
      DebuggerState.EvaluateExpressionListener {

    private boolean isAttached;
    private final RemoverManager removerManager = new RemoverManager();

    @Override
    public void onDebuggerStateChange() {
      handleOnDebuggerStateChange();
    }

    @Override
    public void onEvaluateExpressionResponse(OnEvaluateExpressionResponse response) {
      handleOnEvaluateExpressionResponse(response);
    }

    @Override
    public void onGlobalObjectChanged() {
      hidePopup();
    }

    boolean attach() {
      if (!isAttached) {
        isAttached = true;
        removerManager.track(debuggerState.getDebuggerStateListenerRegistrar().add(this));
        removerManager.track(debuggerState.getEvaluateExpressionListenerRegistrar().add(this));
        return true;
      }
      return false;
    }

    void detach() {
      if (isAttached) {
        isAttached = false;
        removerManager.remove();
      }
    }
  }

  private final Css css;
  private final Editor editor;
  private final EditorPopupController popupController;
  private final DebuggerState debuggerState;
  private final DebuggerListenerImpl debuggerListener = new DebuggerListenerImpl();
  private final EvaluableExpressionFinder expressionFinder = new EvaluableExpressionFinder();
  private final RemoteObjectTree remoteObjectTree;
  private final RemoverManager removerManager = new RemoverManager();
  private final Element popupRootElement;

  private DocumentParser documentParser;
  private EditorPopupController.Remover editorPopupControllerRemover;

  private LineInfo lastExpressionLineInfo;
  private EvaluableExpressionFinder.Result lastExpressionResult;
  private boolean awaitingExpressionEvaluation;

  private final EditorPopupController.PopupRenderer popupRenderer =
      new EditorPopupController.PopupRenderer() {
        @Override
        public Element renderDom() {
          RemoteObjectNode root = remoteObjectTree.getRoot();
          RemoteObjectNode rootChild = (root == null ? null : root.getChildren().get(0));

          // The left margin is different for expandable and non-expandable
          boolean expandable = (rootChild != null && rootChild.hasChildren());
          if (expandable) {
            popupRootElement.getStyle().setMarginLeft(-7, CSSStyleDeclaration.Unit.PX);
          } else {
            popupRootElement.getStyle().setMarginLeft(-17, CSSStyleDeclaration.Unit.PX);
          }

          return popupRootElement;
        }
      };

  private final MouseHoverManager.MouseHoverListener mouseHoverListener =
      new MouseHoverManager.MouseHoverListener() {
        @Override
        public void onMouseHover(int x, int y, LineInfo lineInfo, int column) {
          handleOnMouseHover(lineInfo, column);
        }
      };

  private EvaluationPopupController(Resources resources, Editor editor,
      EditorPopupController popupController, DebuggerState debuggerState) {
    this.css = resources.evaluationPopupControllerCss();
    this.editor = editor;
    this.popupController = popupController;
    this.debuggerState = debuggerState;
    this.remoteObjectTree = RemoteObjectTree.create(
        new RemoteObjectTree.View(resources), resources, debuggerState);
    this.popupRootElement = createPopupRootElement();
  }

  private Element createPopupRootElement() {
    Element root = Elements.createDivElement();
    CssUtils.setUserSelect(root, false);
    root.appendChild(remoteObjectTree.getView().getElement());
    return root;
  }

  void setDocument(Document document, PathUtil path, @Nullable DocumentParser documentParser) {
    hidePopup();

    if (path.getBaseName().endsWith(".js")) {
      this.documentParser = documentParser;
      if (debuggerListener.attach()) {
        // Initialize with the current debugger state.
        handleOnDebuggerStateChange();
      }
    } else {
      this.documentParser = null;
      debuggerListener.detach();
      removerManager.remove();
    }
  }

  private void hidePopup() {
    if (editorPopupControllerRemover != null) {
      editorPopupControllerRemover.remove();
      editorPopupControllerRemover = null;
    }
    lastExpressionLineInfo = null;
    lastExpressionResult = null;
    remoteObjectTree.setRoot(null);
    awaitingExpressionEvaluation = false;
  }

  private boolean isLastPopupVisible() {
    return editorPopupControllerRemover != null
        && editorPopupControllerRemover.isVisibleOrPending();
  }

  private boolean registerNewFinderResult(LineInfo lineInfo,
      EvaluableExpressionFinder.Result result) {
    if (isLastPopupVisible()
        && lineInfo.equals(lastExpressionLineInfo)
        && lastExpressionResult != null
        && lastExpressionResult.getStartColumn() == result.getStartColumn()
        && lastExpressionResult.getEndColumn() == result.getEndColumn()
        && lastExpressionResult.getExpression().equals(result.getExpression())) {
      // The same result was found - no need to hide and reopen the same popup.
      popupController.cancelPendingHide();
      return false;
    }

    hidePopup();
    lastExpressionLineInfo = lineInfo;
    lastExpressionResult = result;
    awaitingExpressionEvaluation = true;

    return true;
  }

  private void handleOnDebuggerStateChange() {
    if (debuggerState.isPaused()) {
      removerManager.track(editor.getMouseHoverManager().addMouseHoverListener(mouseHoverListener));
    } else {
      removerManager.remove();
      hidePopup();
    }
  }

  private void handleOnMouseHover(LineInfo lineInfo, int column) {
    EvaluableExpressionFinder.Result result =
        expressionFinder.find(lineInfo, column, documentParser);
    if (result != null) {
      if (registerNewFinderResult(lineInfo, result)) {
        debuggerState.evaluateExpression(result.getExpression());
      }
    } else {
      hidePopup();
    }
  }

  private void handleOnEvaluateExpressionResponse(OnEvaluateExpressionResponse response) {
    if (!awaitingExpressionEvaluation
        || isLastPopupVisible()
        || response.wasThrown()
        || lastExpressionLineInfo == null
        || lastExpressionResult == null
        || !lastExpressionResult.getExpression().equals(response.getExpression())) {
      return;
    }

    awaitingExpressionEvaluation = false;

    RemoteObjectNode newRoot = RemoteObjectNode.createRoot();
    RemoteObjectNode child =
        new RemoteObjectNode.Builder(response.getExpression(), response.getResult())
            .setDeletable(false)
            .build();
    newRoot.addChild(child);
    remoteObjectTree.setRoot(newRoot);

    JsonArray<Element> popupPartnerElements =
        JsonCollections.createArray(remoteObjectTree.getContextMenuElement());

    editorPopupControllerRemover = popupController.showPopup(lastExpressionLineInfo,
        lastExpressionResult.getStartColumn(), lastExpressionResult.getEndColumn(),
        css.popupAnchor(), popupRenderer, popupPartnerElements, VerticalAlign.BOTTOM,
        true /* shouldCaptureOutsideClickOnClose */, 0 /* delayMs */);
  }
}
