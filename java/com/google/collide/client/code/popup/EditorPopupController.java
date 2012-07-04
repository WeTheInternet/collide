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

package com.google.collide.client.code.popup;

import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.renderer.SingleChunkLineRenderer;
import com.google.collide.client.ui.menu.AutoHideComponent;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Position;
import com.google.collide.client.ui.menu.PositionController.PositionerBuilder;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.popup.Popup;
import com.google.collide.client.util.Elements;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorType;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;
import elemental.util.Timer;

import javax.annotation.Nullable;

/**
 * Controller for the editor-wide popup.
 */
public class EditorPopupController {

  private static final AnchorType START_ANCHOR_TYPE = AnchorType.create(
      EditorPopupController.class, "startAnchor");

  private static final AnchorType END_ANCHOR_TYPE = AnchorType.create(
      EditorPopupController.class, "endAnchor");

  /**
   * Interface for specifying an arbitrary renderer for the popup.
   */
  public interface PopupRenderer {

    /**
     * @return rendered content of the popup
     */
    Element renderDom();
  }

  /**
   * Interface for controlling the popup after it has been shown
   * or scheduled to be shown.
   */
  public interface Remover {

    /**
     * @return true if this popup is currently visible or timer is running
     *         to make it visible
     */
    public boolean isVisibleOrPending();

    /**
     * Hides this popup, if it is currently shown or cancels pending show.
     */
    public void remove();
  }

  public static EditorPopupController create(Popup.Resources resources, Editor editor) {
    return new EditorPopupController(Popup.create(resources), editor);
  }

  private final Editor editor;
  private final Popup popup;
  /** Used to change the position of the popup each time it is shown */
  private final PositionerBuilder positionerBuilder;
  private Remover currentPopupRemover;

  /**
   * A DIV that floats on top of the editor area. We attach the popup to this
   * element.
   */
  private final Element popupDummyElement;

  /**
   * The {@link #popupDummyElement} is anchored between {@code #startAnchor} and
   * {@link #endAnchor} in the {@link #document} document. These variables are
   * tracked to properly detach the anchors from the original document.
   */
  private Anchor startAnchor;
  private Anchor endAnchor;
  private Document document;

  private final Anchor.ShiftListener anchorShiftListener = new Anchor.ShiftListener() {
    @Override
    public void onAnchorShifted(Anchor anchor) {
      updatePopupDummyElementWidth();
    }
  };

  private final Buffer.ScrollListener scrollListener = new Buffer.ScrollListener() {
    @Override
    public void onScroll(Buffer buffer, int scrollTop) {
      hide();
    }
  };

  private EditorPopupController(Popup popup, Editor editor) {
    this.popup = popup;
    this.editor = editor;
    this.popupDummyElement = createPopupDummyElement(editor.getBuffer().getEditorLineHeight());
    this.positionerBuilder = new PositionerBuilder().setPosition(Position.NO_OVERLAP)
        .setHorizontalAlign(HorizontalAlign.MIDDLE);

    popup.setAutoHideHandler(new AutoHideComponent.AutoHideHandler() {
      @Override
      public void onHide() {
        hide();
      }

      public void onShow() {
        // do nothing
      }
    });
  }

  public void cleanup() {
    hide();
  }
  
  private static Element createPopupDummyElement(int lineHeight) {
    Element element = Elements.createDivElement();

    CSSStyleDeclaration style = element.getStyle();
    style.setDisplay(CSSStyleDeclaration.Display.INLINE_BLOCK);
    style.setPosition(CSSStyleDeclaration.Position.ABSOLUTE);
    style.setWidth(0, CSSStyleDeclaration.Unit.PX);
    style.setHeight(lineHeight, CSSStyleDeclaration.Unit.PX);
    style.setZIndex(1);

    // We do this so that custom CSS class (provided by textCssClassName in the #showPopup)
    // with cursor:pointer should work correctly.
    style.setProperty("pointer-events", "none");

    return element;
  }

  private void attachPopupDummyElement(LineInfo lineInfo, int startColumn, int endColumn) {
    // Detach from the old document first, just in case.
    detachPopupDummyElement();

    document = editor.getDocument();
    startAnchor = document.getAnchorManager().createAnchor(START_ANCHOR_TYPE,
        lineInfo.line(), lineInfo.number(), startColumn);
    startAnchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    startAnchor.getShiftListenerRegistrar().add(anchorShiftListener);

    endAnchor = document.getAnchorManager().createAnchor(END_ANCHOR_TYPE,
        lineInfo.line(), lineInfo.number(), endColumn);
    endAnchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    endAnchor.getShiftListenerRegistrar().add(anchorShiftListener);

    editor.getBuffer().addAnchoredElement(startAnchor, popupDummyElement);

    updatePopupDummyElementWidth();
  }

  private void updatePopupDummyElementWidth() {
    if (startAnchor != null && endAnchor != null) {
      Buffer buffer = editor.getBuffer();
      int left = buffer.calculateColumnLeft(startAnchor.getLine(), startAnchor.getColumn());
      popupDummyElement.getStyle().setWidth(
          buffer.calculateColumnLeft(endAnchor.getLine(), endAnchor.getColumn() + 1) - left,
          CSSStyleDeclaration.Unit.PX);
    }
  }

  private void detachPopupDummyElement() {
    if (startAnchor != null) {
      startAnchor.getShiftListenerRegistrar().remove(anchorShiftListener);
      editor.getBuffer().removeAnchoredElement(startAnchor, popupDummyElement);
      document.getAnchorManager().removeAnchor(startAnchor);
      startAnchor = null;
    }
    if (endAnchor != null) {
      endAnchor.getShiftListenerRegistrar().remove(anchorShiftListener);
      document.getAnchorManager().removeAnchor(endAnchor);
      endAnchor = null;
    }
    document = null;
  }

  /**
   * Shows the popup anchored to a given position in the line.
   *
   * @param lineInfo the line to anchor the popup to
   * @param startColumn start column in the line, inclusive
   * @param endColumn end column in the line, inclusive
   * @param textCssClassName class name to highlight the anchor, or {@code null}
   *        if this is not needed
   * @param renderer the popup renderer
   * @param popupPartnerElements array of partner element of the popup
   *        (i.e. those DOM elements where mouse hover will not trigger closing
   *        the popup), or {@code null} if no additional partners should be
   *        considered. Also see {@link AutoHideComponent#addPartner}
   * @param verticalAlign vertical align of the popup related to the line
   * @param shouldCaptureOutsideClickOnClose whether the popup should capture
   *        and prevent clicks outside of it when it closes itself
   * @return an instance of {@link Remover} to control the popup
   */
  public Remover showPopup(LineInfo lineInfo, int startColumn, int endColumn,
      @Nullable String textCssClassName, PopupRenderer renderer,
      final @Nullable JsonArray<Element> popupPartnerElements,
      final VerticalAlign verticalAlign, boolean shouldCaptureOutsideClickOnClose,
      int delayMs) {
    hide();

    attachPopupDummyElement(lineInfo, startColumn, endColumn);

    final SingleChunkLineRenderer lineRenderer = textCssClassName == null ? null :
        SingleChunkLineRenderer.create(startAnchor, endAnchor, textCssClassName);

    popup.setContentElement(renderer.renderDom());
    popup.setCaptureOutsideClickOnClose(shouldCaptureOutsideClickOnClose);
    setPopupPartnersEnabled(popupPartnerElements, true);

    final Timer showTimer = new Timer() {
      @Override
      public void run() {
        positionerBuilder.setVerticalAlign(verticalAlign);
        popup.show(positionerBuilder.buildAnchorPositioner(popupDummyElement));
        if (lineRenderer != null) {
          editor.addLineRenderer(lineRenderer);
          requestRenderLines(lineRenderer);
        }
      }
    };
    if (delayMs <= 0) {
      showTimer.run();
    } else {
      showTimer.schedule(delayMs);
    }

    final com.google.collide.shared.util.ListenerRegistrar.Remover scrollListenerRemover =
        editor.getBuffer().getScrollListenerRegistrar().add(scrollListener);

    return (currentPopupRemover = new Remover() {
      @Override
      public boolean isVisibleOrPending() {
        return this == currentPopupRemover;
      }

      @Override
      public void remove() {
        showTimer.cancel();
        if (isVisibleOrPending()) {
          currentPopupRemover = null;
          detachPopupDummyElement();

          setPopupPartnersEnabled(popupPartnerElements, false);
          popup.destroy();

          if (lineRenderer != null) {
            editor.removeLineRenderer(lineRenderer);
            requestRenderLines(lineRenderer);
          }

          scrollListenerRemover.remove();
        }
      }
    });
  }

  public void cancelPendingHide() {
    popup.cancelPendingHide();
  }

  private void requestRenderLines(SingleChunkLineRenderer lineRenderer) {
    for (int i = lineRenderer.startLine(); i <= lineRenderer.endLine(); ++i) {
      editor.getRenderer().requestRenderLine(
          editor.getDocument().getLineFinder().findLine(i).line());
    }
  }

  private void setPopupPartnersEnabled(@Nullable JsonArray<Element> partnerElements,
      boolean enable) {
    if (partnerElements != null) {
      for (int i = 0, n = partnerElements.size(); i < n; ++i) {
        Element element = partnerElements.get(i);
        if (enable) {
          popup.addPartner(element);
          popup.addPartnerClickTargets(element);
        } else {
          popup.removePartner(element);
          popup.removePartnerClickTargets(element);
        }
      }
    }
  }

  /**
   * Hides the popup if visible.
   */
  public void hide() {
    if (currentPopupRemover != null) {
      currentPopupRemover.remove();
      currentPopupRemover = null;
    }
  }
}
