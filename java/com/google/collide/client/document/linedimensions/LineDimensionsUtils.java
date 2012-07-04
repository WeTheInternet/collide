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

package com.google.collide.client.document.linedimensions;

import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.util.RegExpUtils;
import com.google.collide.shared.util.SharedLogUtils;
import com.google.collide.shared.util.StringUtils;
import com.google.collide.shared.util.UnicodeUtils;

/**
 * Various utility functions used by the {@link LineDimensionsCalculator}.
 *
 */
public class LineDimensionsUtils {
  /**
   * Tag to identify if a line has an offset cache.
   */
  final static String NEEDS_OFFSET_LINE_TAG =
      LineDimensionsCalculator.class.getName() + "NEEDS_OFFSET";

  /**
   * The number of spaces a tab is treated as.
   */
  // TODO: Delegate to EditorSettings once it is available.
  private static int tabSpaceEquivalence = 2;

  /**
   * Enables or disables timeline marking.
   */
  private static boolean enableLogging = false;

  /**
   * Checks if the line needs any offset other than indentation tabs and
   * line-ending carriage-return.
   */
  static boolean needsOffset(Line line) {
    Boolean needsOffset = line.getTag(NEEDS_OFFSET_LINE_TAG);
    // lets do a quick test if we haven't visited this line before
    if (needsOffset == null) {
      return forceUpdateNeedsOffset(line);
    }
    return needsOffset;
  }

  static boolean forceUpdateNeedsOffset(Line line) {
    boolean needsOffset = hasSpecialCharactersWithExclusions(line);
    line.putTag(NEEDS_OFFSET_LINE_TAG, needsOffset);
    return needsOffset;
  }

  /**
   * Uses the special character regex to determine if the line will need to be
   * fully scanned.
   */
  static boolean hasSpecialCharactersWithExclusions(Line line) {
    return hasSpecialCharactersMaybeWithExclusions(line.getText(), true);
  }

  static boolean hasSpecialCharactersMaybeWithExclusions(String lineText, boolean stripTab) {
    int length = lineText.length();
    int index = stripTab ? getLastIndentationTabCount(lineText, length) : 0;
    int endIndex = lineText.endsWith("\r\n") ? length - 2 : length;
    return RegExpUtils.resetAndTest(
        UnicodeUtils.regexpNonAsciiTabOrCarriageReturn, lineText.substring(index, endIndex));
  }

  /**
   * Marks the lines internal cache of offset information dirty starting at the
   * specified column. A column of 0 will clear the cache completely marking the
   * line for re-measuring as needed.
   */
  public static void isOffsetNeededAndCache(Line line, int column, TextChange.Type type) {
    // TODO: Inspect the text instead of re-inspecting the entire line
    markTimeline(LineDimensionsCalculator.class, "Begin maybe mark cache dirty");
    Boolean hadNeedsOffset = line.getTag(NEEDS_OFFSET_LINE_TAG);
    /*
     * if you backspace the first character in a line, the line will be the
     * previous line and type will be TextChange.Type.DELETE but that line could
     * not have been visited yet. So we force a needOffset update in that case.
     */
    if (hadNeedsOffset == null || (!hadNeedsOffset && type == TextChange.Type.INSERT)) {
      forceUpdateNeedsOffset(line);
    } else if (hadNeedsOffset) {
      /*
       * we don't know the zoom level here and am only going to mark the cache
       * dirty so we can safely use getUnsafe.
       */
      ColumnOffsetCache cache = ColumnOffsetCache.getUnsafe(line);
      if (cache != null) {
        // if the text was deleted, perhaps we removed the special characters?
        if (type == TextChange.Type.DELETE && !forceUpdateNeedsOffset(line)) {
          ColumnOffsetCache.removeFrom(line);
        } else {
          // if not we should mark our cache dirty
          cache.markDirty(column);
        }
      }
    }
    markTimeline(LineDimensionsCalculator.class, "End maybe mark cache dirty");
  }

  public static void preTextIsOffsetNeededAndCache(
      Line line, int column, TextChange.Type type, String text) {
    // For delete we delegate through since the behavior is the same.
    if (type == TextChange.Type.DELETE) {
      isOffsetNeededAndCache(line, column, type);
    } else {
      Boolean hadNeedsOffset = line.getTag(NEEDS_OFFSET_LINE_TAG);
      // If we are non-special case, scan the new text to determine if that's
      // still the case.
      if (hadNeedsOffset == null || !hadNeedsOffset) {
        int newlineIndex = text.indexOf('\n');
        String newText;
        String oldText = line.getText();
        if (newlineIndex == -1) {
          // Inline insert.
          newText = oldText.substring(0, column) + text + oldText.substring(column);
        } else {
          // Multi-line insert. We do not care about the following lines,
          // as they will be processed later.
          newText = oldText.substring(0, column) + text.substring(0, newlineIndex);
        }
        line.putTag(
            NEEDS_OFFSET_LINE_TAG, hasSpecialCharactersMaybeWithExclusions(newText, true));
      } else {
        // We don't know the zoom level here and am only going to mark the cache
        // dirty so we can safely use getUnsafe.
        ColumnOffsetCache cache = ColumnOffsetCache.getUnsafe(line);
        if (cache != null) {
          cache.markDirty(column);
        }
      }
    }
  }

  /**
   * Returns the index of the last indentation tab before endColumn.
   */
  public static int getLastIndentationTabCount(String lineText, int endColumn) {
    int tabs = 0;
    int length = lineText.length();
    for (; tabs < length && lineText.charAt(tabs) == '\t' && tabs < endColumn; tabs++) {
      // we do it all in the for loop :o
    }
    return tabs;
  }

  /**
   * Sets the number of spaces a tab is rendered as.
   */
  public static void setTabSpaceEquivalence(int spaces) {
    tabSpaceEquivalence = spaces;
  }

  /**
   * Retrieves the number of spaces a tab is rendered as.
   */
  public static int getTabWidth() {
    return tabSpaceEquivalence;
  }

  /**
   * Returns a tab represented as a space.
   */
  public static String getTabAsSpaces() {
    return StringUtils.repeatString(" ", tabSpaceEquivalence);
  }

  /**
   * Emits a markTimeline message if logging is enabled via
   * {@link #enableLogging}.
   */
  public static void markTimeline(Class<?> c, String message) {
    if (enableLogging) {
      SharedLogUtils.markTimeline(c, message);
    }
  }
}
