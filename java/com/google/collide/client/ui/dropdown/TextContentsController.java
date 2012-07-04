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

package com.google.collide.client.ui.dropdown;

import elemental.html.Element;

/**
 * A controller to set the text on an element, optionally supporting a hint.
 */
public class TextContentsController {
  
  private static final String ATTR_VALUE = "value";

  public enum Setter {
    INNER_TEXT, VALUE
  }
  
  private final Element element;
  private final Setter setter;
  private boolean hasText;
  private String hint;
  
  public TextContentsController(Element element, Setter setter) {
    this.element = element;
    this.setter = setter;
    
  }
  
  public void setHint(String hint) {
    this.hint = hint;

    if (!hasText) {
      set(hint);
    }
  }
  
  /**
   * Clears the text of the element and reverts it to the hint specified in
   * #setHint(String)
   */
  public void clearText() {
    set(hint);
  }
  
  public void setText(String text) {
    hasText = !text.isEmpty();
    if (hasText) {
      set(text);
    } else {
      set(hint);
    }
  }
  
  public String getText() {
    switch (setter) {
      case INNER_TEXT:
        return element.getInnerText();
      
      case VALUE:
        return element.getAttribute(ATTR_VALUE);
        
      default:
        throw new IllegalStateException();
    }
  }

  private void set(String text) {
    switch (setter) {
      case INNER_TEXT:
        element.setInnerText(text);
        break;
        
      case VALUE:
        element.setAttribute(ATTR_VALUE, text);
        break;
    }
  }
}
