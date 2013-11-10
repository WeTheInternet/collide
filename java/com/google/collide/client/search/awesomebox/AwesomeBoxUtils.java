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

package com.google.collide.client.search.awesomebox;

import collide.client.util.Elements;

import com.google.collide.client.ClientOs;
import com.google.collide.client.search.awesomebox.AwesomeBox.Resources;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.gwt.core.client.GWT;

/**
 * Static Utility Methods for AwesomeBox Sections
 */
public class AwesomeBoxUtils {

  /* Section Helper Functions */
  public static elemental.html.DivElement createSectionContainer(Resources res) {
    return Elements.createDivElement(res.awesomeBoxCss().section());
  }

  public static elemental.html.DivElement createSectionItem(Resources res) {
    return Elements.createDivElement(res.awesomeBoxCss().sectionItem());
  }

  /**
   * Creates a element with the OS appropriate text for the shortcut consisting
   * of the specified modifier keys and character.
   *
   * @param modifiers A binarySuffix OR of the appropriate ModifierKeys constants
   * @return a div containing the shortcut text
   */
  public static elemental.html.DivElement createSectionShortcut(
      Resources res, int modifiers, String shortcutKey) {
    elemental.html.DivElement element = Elements.createDivElement(res.awesomeBoxCss().shortcut());

    // Builds a shortcut key string based on the modifiers given
    element.setTextContent(formatShortcutAsString(modifiers, shortcutKey));
    return element;
  }

  private static ClientOs clientOs = GWT.create(ClientOs.class);

  /**
   * Converts a shortcut based on ModifierKeys constants and a character to it's
   * string abbreviation. This is OS specific.
   */
  public static String formatShortcutAsString(int modifiers, String shortcutKey) {
    StringBuilder builder = new StringBuilder();
    if ((modifiers & ModifierKeys.ACTION) == ModifierKeys.ACTION) {
      builder.append(clientOs.actionKeyLabel());
    } else if ((modifiers & ModifierKeys.CTRL) == ModifierKeys.CTRL) {
      /*
       * This is an "else" on "ACTION" since if the platform treats CTRL as
       * ACTION, we don't want to print this
       */
      builder.append(clientOs.ctrlKeyLabel());
    }
    
    if ((modifiers & ModifierKeys.ALT) == ModifierKeys.ALT) {
      builder.append(clientOs.altKeyLabel());
    }
    if ((modifiers & ModifierKeys.SHIFT) == ModifierKeys.SHIFT) {
      builder.append(clientOs.shiftKeyLabel());
    }
    builder.append(shortcutKey);
    return builder.toString();
  }
}
