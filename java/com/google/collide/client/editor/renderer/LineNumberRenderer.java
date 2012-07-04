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

package com.google.collide.client.editor.renderer;

import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.gutter.Gutter;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.JsIntegerMap;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.gwt.resources.client.ClientBundle;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;

/**
 * A renderer for the line numbers in the left gutter.
 */
public class LineNumberRenderer {

  private static final int NONE = -1;

  private final Buffer buffer;
  private final Gutter leftGutter;

  /**
   * Current editor instance.
   *
   * Used to track if current fie can be edited (i.e. is not readonly).
   *
   * TODO: add new abstraction to avoid editor passing.
   */
  private final Editor editor;

  private int previousBottomLineNumber = -1;
  private int previousTopLineNumber = -1;
  private JsIntegerMap<Element> lineNumberToElementCache;
  private final ViewportModel viewport;
  private final Css css;
  private int activeLineNumber = NONE;
  private int renderedActiveLineNumber = NONE;
  private final JsonArray<ListenerRegistrar.Remover> listenerRemovers =
      JsonCollections.createArray();

  private final SelectionModel.CursorListener cursorListener = new SelectionModel.CursorListener() {
    @Override
    public void onCursorChange(LineInfo lineInfo, int column, boolean isExplicitChange) {
      activeLineNumber = lineInfo.number();
      updateActiveLine();
    }
  };

  private Editor.ReadOnlyListener readonlyListener = new Editor.ReadOnlyListener() {
    @Override
    public void onReadOnlyChanged(boolean isReadOnly) {
      updateActiveLine();
    }
  };

  private void updateActiveLine() {
    int lineNumber = this.activeLineNumber;
    if (editor.isReadOnly()) {
      lineNumber = NONE;
    }
    if (lineNumber == renderedActiveLineNumber) {
      return;
    }

    if (renderedActiveLineNumber != NONE) {
      Element renderedActiveLine = lineNumberToElementCache.get(renderedActiveLineNumber);
      if (renderedActiveLine != null) {
        renderedActiveLine.removeClassName(css.activeLineNumber());
        renderedActiveLineNumber = NONE;
      }
    }
    Element newActiveLine = lineNumberToElementCache.get(lineNumber);
    // Add class if it's in the viewport.
    if (newActiveLine != null) {
      newActiveLine.addClassName(css.activeLineNumber());
      renderedActiveLineNumber = lineNumber;
    }
  }

  public void teardown() {
    for (int i = 0, n = listenerRemovers.size(); i < n; i++) {
      listenerRemovers.get(i).remove();
    }
  }

  /**
   * Line number CSS.
   */
  public interface Css extends Editor.EditorSharedCss {
    String lineNumber();

    String activeLineNumber();
  }

  /**
   * Line number resources.
   */
  public interface Resources extends ClientBundle {
    @Source({"com/google/collide/client/common/constants.css",
        "LineNumberRenderer.css"})
    Css lineNumberRendererCss();
  }

  LineNumberRenderer(Buffer buffer, Resources res, Gutter leftGutter, ViewportModel viewport,
      SelectionModel selection, Editor editor) {
    this.buffer = buffer;
    this.leftGutter = leftGutter;
    this.editor = editor;
    this.lineNumberToElementCache = JsIntegerMap.create();
    this.viewport = viewport;
    this.css = res.lineNumberRendererCss();
    listenerRemovers.add(selection.getCursorListenerRegistrar().add(cursorListener));
    listenerRemovers.add(editor.getReadOnlyListenerRegistrar().add(readonlyListener));
  }

  void renderImpl(int updateBeginLineNumber) {
    int topLineNumber = viewport.getTopLineNumber();
    int bottomLineNumber = viewport.getBottomLineNumber();

    if (previousBottomLineNumber == -1 || topLineNumber > previousBottomLineNumber
        || bottomLineNumber < previousTopLineNumber) {

      if (previousBottomLineNumber > -1) {
        garbageCollectLines(previousTopLineNumber, previousBottomLineNumber);
      }

      fillOrUpdateLines(topLineNumber, bottomLineNumber);

    } else {
      /*
       * The viewport was shifted and part of the old viewport will be in the
       * new viewport.
       */
      // first garbage collect any lines that have gone off the screen
      if (previousTopLineNumber < topLineNumber) {
        // off the top
        garbageCollectLines(previousTopLineNumber, topLineNumber - 1);
      }

      if (previousBottomLineNumber > bottomLineNumber) {
        // off the bottom
        garbageCollectLines(bottomLineNumber + 1, previousBottomLineNumber);
      }

      /*
       * Re-create any line numbers that are now visible or have had their
       * positions shifted.
       */
      if (previousTopLineNumber > topLineNumber) {
        // new lines at the top
        fillOrUpdateLines(topLineNumber, previousTopLineNumber - 1);
      }

      if (updateBeginLineNumber >= 0 && updateBeginLineNumber <= bottomLineNumber) {
        // lines updated in the middle; redraw everything below
        fillOrUpdateLines(updateBeginLineNumber, bottomLineNumber);
      } else {
        // only check new lines scrolled in from the bottom
        if (previousBottomLineNumber < bottomLineNumber) {
          fillOrUpdateLines(previousBottomLineNumber, bottomLineNumber);
        }
      }
    }

    previousTopLineNumber = viewport.getTopLineNumber();
    previousBottomLineNumber = viewport.getBottomLineNumber();
  }

  void render() {
    renderImpl(-1);
  }

  /**
   * Re-render all line numbers including and after lineNumber to account for
   * spacer movement.
   */
  void renderLineAndFollowing(int lineNumber) {
    renderImpl(lineNumber);
  }

  private void fillOrUpdateLines(int beginLineNumber, int endLineNumber) {
    for (int i = beginLineNumber; i <= endLineNumber; i++) {
      Element lineElement = lineNumberToElementCache.get(i);
      if (lineElement != null) {
        updateElementPosition(lineElement, i);
      } else {
        Element element = createElement(i);
        lineNumberToElementCache.put(i, element);
        leftGutter.addUnmanagedElement(element);
      }
    }
  }

  private void updateElementPosition(Element lineNumberElement, int lineNumber) {
    lineNumberElement.getStyle().setTop(
        buffer.calculateLineTop(lineNumber), CSSStyleDeclaration.Unit.PX);
  }

  private Element createElement(int lineNumber) {
    Element element = Elements.createDivElement(css.lineNumber());
    // Line 0 will be rendered as Line 1
    element.setTextContent(String.valueOf(lineNumber + 1));
    element.getStyle().setTop(buffer.calculateLineTop(lineNumber), CSSStyleDeclaration.Unit.PX);
    if (lineNumber == activeLineNumber) {
      element.addClassName(css.activeLineNumber());
      renderedActiveLineNumber = activeLineNumber;
    }
    return element;
  }

  private void garbageCollectLines(int beginLineNumber, int endLineNumber) {
    for (int i = beginLineNumber; i <= endLineNumber; i++) {
      Element lineElement = lineNumberToElementCache.get(i);
      if (lineElement != null) {
        leftGutter.removeUnmanagedElement(lineElement);
        lineNumberToElementCache.erase(i);
      } else {
        throw new IndexOutOfBoundsException(
            "Tried to garbage collect line number " + i + " when it does not exist.");
      }
    }
    if (beginLineNumber <= renderedActiveLineNumber && renderedActiveLineNumber <= endLineNumber) {
      renderedActiveLineNumber = NONE;
    }
  }
}
