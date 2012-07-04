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

import com.google.collide.client.util.Elements;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.util.SortedList;

import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;

/*
 * TODO: Knowledge about what lines the spacer is logically linked
 * to which could make the system scroll intelligently, see comments in CL
 * 26294787
 */
/**
 * A spacer allows a client to insert UI inbetween lines in an editor without
 * affecting the backing document.
 *
 */
public class Spacer {

  /**
   * Comparator for sorting spacers.
   */
  public static class Comparator implements SortedList.Comparator<Spacer> {
    @Override
    public int compare(Spacer a, Spacer b) {
      return a.getLineNumber() - b.getLineNumber();
    }
  }

  /**
   * One way comparator for sorting spacers.
   */
  public static class OneWaySpacerComparator extends SortedList.OneWayIntComparator<Spacer> {
    @Override
    public int compareTo(Spacer s) {
      return value - s.getLineNumber();
    }
  }

  /** A spacer's anchor is always attached to the line that follows the spacer */
  private final Anchor anchor;
  private final Buffer buffer;
  private final CoordinateMap coordinateMap;
  /** Always use {@link #getElement()} */
  private Element element;
  private int height;
  private String cssClass;

  Spacer(Anchor anchor, int height, CoordinateMap coordinateMap, Buffer buffer, String cssClass) {
    this.anchor = anchor;
    this.height = height;
    this.coordinateMap = coordinateMap;
    this.buffer = buffer;
    this.cssClass = cssClass;
  }

  /**
   * Return the line number this spacer is attached above.
   */
  public int getLineNumber() {
    return anchor.getLineNumber();
  }

  public Line getLine() {
    return anchor.getLine();
  }

  public LineInfo getLineInfo() {
    return anchor.getLineInfo();
  }

  public Anchor getAnchor() {
    return anchor;
  }

  /**
   * Return the height of this spacer, not including the line it is attached
   * to below.
   */
  public int getHeight() {
    return height;
  }

  public void setHeight(int newHeight) {
    int oldHeight = height;
    height = newHeight;
    coordinateMap.handleSpacerHeightChanged(this, oldHeight);
    element.getStyle().setHeight(height, CSSStyleDeclaration.Unit.PX);
    element.getStyle().setMarginTop(-height, CSSStyleDeclaration.Unit.PX);
  }

  public void addElement(Element addElement) {
    getElement().appendChild(addElement);
  }

  public boolean isAttached() {
    return anchor.isAttached();
  }

  @Override
  public String toString() {
    return "Spacer, height: " + height + ", line number: " + anchor.getLineNumber();
  }

  private Element getElement() {
    if (element != null) {
      return element;
    }

    // Create div area for clients to draw inside
    element = Elements.createDivElement();
    element.addClassName(cssClass);
    element.getStyle().setLeft("0px");
    element.getStyle().setRight("0px");
    element.getStyle().setHeight(height, CSSStyleDeclaration.Unit.PX);
    element.getStyle().setPosition(CSSStyleDeclaration.Position.ABSOLUTE);
    element.getStyle().setMarginTop(-height, CSSStyleDeclaration.Unit.PX);

    EventListener bubblePreventionListener = new EventListener() {
      @Override
      public void handleEvent(Event e) {
        e.stopPropagation();
      }
    };

    element.setOnMouseDown(bubblePreventionListener);
    element.setOnMouseMove(bubblePreventionListener);
    element.setOnMouseUp(bubblePreventionListener);

    buffer.addAnchoredElement(anchor, element);

    return element;
  }
}
