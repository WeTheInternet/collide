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

package com.google.collide.client.ui;

import com.google.collide.client.util.Elements;
import com.google.common.base.Preconditions;

import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.TextAreaElement;

/**
 * Creates a GrowableTextArea, it isn't perfect but it does a fairly decent job
 * of growing as the user types in new text.
 *
 */
/*
 * How it works:
 *
 * This class creates two text areas, one which is returned to the user and a
 * second which is hidden and placed off-screen. As users type into the main
 * text area, the text is also put into the hidden text area which has an
 * explicit row height of 1. We use the scrollHeight of the hidden text area to
 * determine the number of row necessary to display the text in the main area
 * without scrolling. It is off a little bit due to the scrollbar width changing
 * the width of the hidden text area a little bit, but generally it is good
 * enough.
 */
public class GrowableTextArea {

  public static GrowableTextArea create(String className) {
    return createWithMinimumRows(className, 1);
  }

  /**
   * @param minRows The minimum number of rows for this {@link GrowableTextArea}
   *        to be.
   */
  public static GrowableTextArea createWithMinimumRows(String className, int minRows) {
    Preconditions.checkArgument(minRows >= 1, "Minimum rows must be >= 1");
    
    // Create the base text element and pass it in
    TextAreaElement element = Elements.createTextAreaElement();
    element.setClassName(className);
    element.setRows(minRows);
    element.getStyle().setOverflowY(CSSStyleDeclaration.OverflowY.HIDDEN);

    return new GrowableTextArea(element, minRows);
  }

  private final TextAreaElement element;
  private final TextAreaElement clone;
  private final int minimumRows;

  public GrowableTextArea(TextAreaElement element, int minimumRows) {
    this.element = element;
    this.minimumRows = minimumRows;
    this.clone = Elements.createTextAreaElement();
    
    setupClone();
    attachEvents();
  }
  
  private void setupClone() {
    clone.setClassName(element.getClassName());
    CSSStyleDeclaration style = clone.getStyle();
    style.setVisibility(CSSStyleDeclaration.Visibility.HIDDEN);
    style.setPosition(CSSStyleDeclaration.Position.ABSOLUTE);
    style.setOverflow(CSSStyleDeclaration.Overflow.AUTO);
    style.setLeft("-5000px");
    style.setTop("-5000px");
    
    clone.setRows(1);
  }

  private void attachEvents() {
    element.addEventListener(Event.INPUT, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (clone.getParentElement() == null) {
          element.getParentElement().appendChild(clone);
        }
        
        clone.setValue(element.getValue());
        int requiredRows =
            (int) Math.ceil((double) clone.getScrollHeight() / (double) clone.getClientHeight());
        int rows = Math.max(requiredRows, minimumRows);
        
        element.setRows(rows);
      }
    }, false);
  }

  public TextAreaElement asTextArea() {
    return element;
  }
}
