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

import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonIntegerMap;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorUtils;
import com.google.collide.shared.document.anchor.ReadOnlyAnchor;
import com.google.collide.shared.document.anchor.AnchorManager.AnchorVisitor;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;

// TODO: support RangeAnchoredElements
/**
 * A manager that allows for adding and removing elements to some given
 * container element. This manager is capable of adding elements that are
 * anchored to a point or to a range.
 *
 * Some restrictions of anchored elements:
 * <ul>
 * <li>An anchor cannot be used to anchor multiple elements</li>
 * <li>Anchors must be assigned a line number (though this can be loosened if a
 * use case arises)</li>
 * </ul>
 */
public class ElementManager {

  private final ReadOnlyAnchor.ShiftListener anchorShiftedListener =
      new ReadOnlyAnchor.ShiftListener() {
        @Override
        public void onAnchorShifted(ReadOnlyAnchor anchor) {
          updateAnchoredElements(anchor);
        }
      };

  private final ReadOnlyAnchor.MoveListener anchorMovedListener =
      new ReadOnlyAnchor.MoveListener() {
        @Override
        public void onAnchorMoved(ReadOnlyAnchor anchor) {
          updateAnchoredElements(anchor);
        }
      };

  private final ReadOnlyAnchor.RemoveListener anchorRemovalListener =
      new ReadOnlyAnchor.RemoveListener() {
        @Override
        public void onAnchorRemoved(ReadOnlyAnchor anchor) {
          JsonArray<Element> elements = anchoredElements.get(anchor.getId());
          for (int i = 0, n = elements.size(); i < n; i++) {
            removeAnchoredElement(anchor, elements.get(i));
          }
        }
      };

  private final Buffer buffer;
  private final Element container;
  private final JsonIntegerMap<JsonArray<Element>> anchoredElements =
      JsonCollections.createIntegerMap();
  private final JsonArray<ReadOnlyAnchor> anchoredElementAnchors = JsonCollections.createArray();

  private final Renderer.LineLifecycleListener renderedLineLifecycleListener =
      new Renderer.LineLifecycleListener() {
        private final AnchorVisitor lineCreatedAnchorVisitor = new AnchorVisitor() {
          @Override
          public void visitAnchor(Anchor anchor) {
            JsonArray<Element> elements = anchoredElements.get(anchor.getId());
            if (elements != null) {
              for (int i = 0, n = elements.size(); i < n; i++) {
                Element element = elements.get(i);
                attachElement(element);
                positionElementToAnchorTopLeft(anchor, element);
              }
            }
          }
        };

        private final AnchorVisitor lineShiftedAnchorVisitor = new AnchorVisitor() {
          @Override
          public void visitAnchor(Anchor anchor) {
            JsonArray<Element> elements = anchoredElements.get(anchor.getId());
            if (elements != null) {
              for (int i = 0, n = elements.size(); i < n; i++) {
                updateAnchoredElement(anchor, elements.get(i));
              }
            }
          }
        };

        private final AnchorVisitor lineGarbageCollectedAnchorVisitor = new AnchorVisitor() {
          @Override
          public void visitAnchor(Anchor anchor) {
            JsonArray<Element> elements = anchoredElements.get(anchor.getId());
            if (elements != null) {
              for (int i = 0, n = elements.size(); i < n; i++) {
                detachElement(elements.get(i));
              }
            }
          }
        };

        @Override
        public void onRenderedLineGarbageCollected(Line line) {
          AnchorUtils.visitAnchorsOnLine(line, lineGarbageCollectedAnchorVisitor);
        }

        @Override
        public void onRenderedLineCreated(Line line, int lineNumber) {
          AnchorUtils.visitAnchorsOnLine(line, lineCreatedAnchorVisitor);
        }

        @Override
        public void onRenderedLineShifted(Line line, int lineNumber) {
          /*
           * TODO: Given this callback exists now, do we really
           * need to require anchors with line numbers?
           */
          AnchorUtils.visitAnchorsOnLine(line, lineShiftedAnchorVisitor);
        }
      };

  private ListenerRegistrar.Remover rendererListenerRemover;
  private final JsonArray<Element> unmanagedElements = JsonCollections.createArray();
  private ViewportModel viewport;

  public ElementManager(Element container, Buffer buffer) {
    this.container = container;
    this.buffer = buffer;
  }

  public void handleDocumentChanged(ViewportModel viewport, Renderer renderer) {
    if (rendererListenerRemover != null) {
      rendererListenerRemover.remove();
    }

    removeAnchoredElements();
    detachElements(unmanagedElements);
    unmanagedElements.clear();

    this.viewport = viewport;

    rendererListenerRemover =
        renderer.getLineLifecycleListenerRegistrar().add(renderedLineLifecycleListener);
  }

  public void addAnchoredElement(ReadOnlyAnchor anchor, Element element) {
    if (!anchor.hasLineNumber()) {
      throw new IllegalArgumentException(
          "The given anchor does not have a line number; create it with line numbers");
    }

    JsonArray<Element> elements = anchoredElements.get(anchor.getId());
    if (elements == null) {
      elements = JsonCollections.createArray();
      anchoredElements.put(anchor.getId(), elements);

      anchoredElementAnchors.add(anchor);

      anchor.getReadOnlyShiftListenerRegistrar().add(anchorShiftedListener);
      anchor.getReadOnlyMoveListenerRegistrar().add(anchorMovedListener);
      anchor.getReadOnlyRemoveListenerRegistrar().add(anchorRemovalListener);
      
    } else if (elements.contains(element)) {
      // Already anchored, do nothing
      return;
    }

    elements.add(element);
    initializeElementForBeingManaged(element);

    updateAnchoredElement(anchor, element);
  }

  public void removeAnchoredElement(ReadOnlyAnchor anchor, Element element) {
    JsonArray<Element> elements = anchoredElements.get(anchor.getId());
    if (elements == null || !elements.remove(element)) {
      return;
    }
    
    if (elements.size() == 0) {
      anchor.getReadOnlyShiftListenerRegistrar().remove(anchorShiftedListener);
      anchor.getReadOnlyMoveListenerRegistrar().remove(anchorMovedListener);
      anchor.getReadOnlyRemoveListenerRegistrar().remove(anchorRemovalListener);

      anchoredElements.erase(anchor.getId());
      anchoredElementAnchors.remove(anchor);
    }

    detachElement(element);
  }

  private void removeAnchoredElements() {
    while (anchoredElementAnchors.size() > 0) {
      ReadOnlyAnchor anchor = anchoredElementAnchors.get(0);
      JsonArray<Element> elements = anchoredElements.get(anchor.getId());
      for (int i = 0, n = elements.size(); i < n; i++) {
        removeAnchoredElement(anchor, elements.get(i));
      }
    }
  }

  public void addUnmanagedElement(Element element) {
    unmanagedElements.add(element);
    attachElement(element);
  }

  public void removeUnmanagedElement(Element element) {
    unmanagedElements.remove(element);
    detachElement(element);
  }

  private void initializeElementForBeingManaged(Element element) {
    element.getStyle().setPosition(CSSStyleDeclaration.Position.ABSOLUTE);
  }

  private void updateAnchoredElements(ReadOnlyAnchor anchor) {
    JsonArray<Element> elements = anchoredElements.get(anchor.getId());
    if (elements != null) {
      for (int i = 0, n = elements.size(); i < n; i++) {
        updateAnchoredElement(anchor, elements.get(i));
      }
    }
  }
  
  /**
   * Renders an anchored element intelligently; adds it to the DOM when it is in
   * the viewport and removes it once it leaves the viewport.
   */
  private void updateAnchoredElement(ReadOnlyAnchor anchor, Element element) {
    /*
     * We only want the line number if the anchor is in the viewport, and this
     * is a quick way of achieving that
     */
    int lineNumberGuess = LineUtils.getCachedLineNumber(anchor.getLine());
    boolean isInViewport =
        lineNumberGuess != -1 && lineNumberGuess >= viewport.getTopLineNumber()
            && lineNumberGuess <= viewport.getBottomLineNumber();
    boolean isRendered = element.getParentElement() != null;

    if (isInViewport && !isRendered) {
      // Anchor moved into the viewport
      attachElement(element);
      positionElementToAnchorTopLeft(anchor, element);

    } else if (isRendered && !isInViewport) {
      // Anchor moved out of the viewport
      detachElement(element);

    } else if (isInViewport) {
      // Anchor was and is in viewport, reposition
      positionElementToAnchorTopLeft(anchor, element);
    }
  }

  private void positionElementToAnchorTopLeft(ReadOnlyAnchor anchor, Element element) {
    CSSStyleDeclaration style = element.getStyle();

    style.setTop(buffer.convertLineNumberToY(anchor.getLineNumber()), CSSStyleDeclaration.Unit.PX);

    int column = anchor.getColumn();
    if (column != AnchorManager.IGNORE_COLUMN) {
      style.setLeft(buffer.convertColumnToX(anchor.getLine(), column), CSSStyleDeclaration.Unit.PX);
    }
  }

  private void attachElement(Element element) {
    container.appendChild(element);
  }

  private void detachElements(JsonArray<Element> elements) {
    for (int i = 0, n = elements.size(); i < n; i++) {
      detachElement(elements.get(i));
    }
  }

  private void detachElement(Element element) {
    if (container.contains(element)) {
      container.removeChild(element);
    }
  }

  public void repositionAnchoredElementsWithColumn() {
    for (int i = 0, n = anchoredElementAnchors.size(); i < n; i++) {
      ReadOnlyAnchor anchor = anchoredElementAnchors.get(i);
      if (!anchor.hasColumn()) {
        continue;
      }

      JsonArray<Element> elements = anchoredElements.get(anchor.getId());
      for (int elementsPos = 0, elementsSize = elements.size(); elementsPos < elementsSize;
          elementsPos++) {
        positionElementToAnchorTopLeft(anchor, elements.get(elementsPos));
      }
    }
  }
}
