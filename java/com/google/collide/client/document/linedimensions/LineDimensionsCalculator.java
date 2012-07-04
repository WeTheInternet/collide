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

import com.google.collide.client.document.linedimensions.ColumnOffsetCache.ColumnOffset;
import com.google.collide.client.util.dom.FontDimensionsCalculator;
import com.google.collide.client.util.dom.FontDimensionsCalculator.FontDimensions;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.Pair;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.Document.PreTextListener;
import com.google.collide.shared.document.Document.TextListener;
import com.google.collide.shared.document.TextChange.Type;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.collide.shared.util.UnicodeUtils;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * An object which can accurately measure a {@link Line} and map X coordinates
 * to columns and vice versa.
 */
/*
 * TL;DR;
 *
 * We have a fast regex to tell whether a line can use the naive calculations
 * for column/x. If it doesn't we have to put it in a span and measure special
 * characters then store how much they effect the width of our columns. Tabs
 * which are prefixing indentation and suffix carraige returns are special cased
 * and don't require measurements.
 */
/*
 * Implementation Details
 *
 * There are three states a line can be in:
 *
 * 1. Unknown, this is represented by the lack of a NEEDS_OFFSET tag on the
 * Line. This is the default line state and means that a user (or this class)
 * has yet to visit this line in the document.
 *
 * 2. No offset needed (false NEEDS_OFFSET value), this state indicates that
 * we've visited the line and decided it has no characters which warrant special
 * attention. Prefix tabs and suffix carriage returns are included in this state
 * since they are a common case. If they exist they will be handled in some very
 * simple offset code.
 *
 * 3. Offset needed (true NEEDS_OFFSET value), this state indicates that there
 * are special characters within this line. This includes tabs which appear in
 * the middle of a line or carriage-returns which aren't at the end followed by
 * a \n. In this state a ColumnOffsetCache is built and put onto the line. It is
 * lazily constructed as columns are requested.
 *
 * ColumnOffsetCache internals:
 *
 * Once built and attached to a line, the column offset cache maintains a cache
 * entry for each special character on a line. The entry will be made for the
 * column immediately following the special character and the x value of that
 * entry will represent the left edge of that column (the right edge of the
 * column with the special character).
 *
 * All examples assume double-wide characters are 10px and normal characters are
 * 5px wide, combining marks are 0px.
 *
 * For example, if the line is "烏烏龍茶\n" the cache will contain:
 *
 * [{column: 1, x: 10px }, {column: 2, x: 20px }, {column: 3, x: 30px },
 * {column: 4, x: 40px }]
 *
 * Note that this last entry exists regardless of the \n existing. So the last
 * entry may be a column which does not exist in the string.
 *
 * Using the example, when column 2's x is requested (that is the third
 * character in the string), entry [1] will be pulled from the cache and the
 * returned left edge will be 20px.
 *
 * A cache entry is only created for special width characters, any interesting
 * character that turns out to be our same width is ignored as well.
 *
 * For example in the case of "烏aaa烏" the cache will contain:
 *
 * [{column: 1, x: 10px }, {column: 5, x: 25px }]
 *
 * In this example if we were interested in column 2 (that is the 3rd character
 * in the string or 2nd 'a' character), entry [1] will be pulled from cache,
 * then 10px + 5px will be added together and a left edge of 15px will be
 * returned.
 *
 * An important note on combining marks:
 *
 * Combining marks and some similar characters, show up in the cache as 0 width
 * characters. That is when they are measured they add no additional width to
 * the string and function as a zero width column. They will still contain an
 * entry in cache but it will be marked as isZeroWidth and the x value will
 * correspond to the same x value of the previous column.
 *
 * For example, if the line is "à=à" (note the first a has a combining mark so
 * the string is really more like "a`=à" the cache will contain:
 *
 * [{column: 2, x: 5px, isZeroWidth: true }]
 *
 * That means if we look up say character 4 (the last à). We will take 5px and
 * add 5px to get 10px which would put us to the right of the '=' when rendered.
 *
 * How the cache is built:
 *
 * We scan the string for any characters of interest (we vaguely define that as
 * any character not part of the original latin alphabet so character code >
 * 255). We then measure the entire string up to and including that character.
 * If the string matches the length we'd expect, then we cache that the
 * character is normal but do not create an offset cache entry. Otherwise we
 * store the width of the character so we don't have to measure for it again,
 * then create an offset cache entry to denote that this character affects
 * columns past it since it has an odd width.
 *
 * Current Limitations:
 *
 * Some combining marks combine in both directions (this is mostly script type
 * languages). This means they can affect the width of columns before themselves
 * (aka make the character the combine width smaller). We have a hack to
 * mitigate this but really there's only so much we can do.
 *
 * Measuring can be expensive in long strings containing a large number of
 * different special characters. So if you paste a 500 character string of
 * Katakana, then click at the very end, prepare for a small wait while we catch
 * up. The good news is that will be mitigated the second time since each of
 * those character's width is cached and we won't have to layout again.
 *
 * Zoom makes us wipe the entire cache (sucks), Unfortunately this can't be
 * avoided as different character's scale at different factors (double sucks).
 * So we just clear our cache and go about rebuilding.
 *
 * Further comments:
 *
 * I'm not sure how docs does it but they seem pretty quick, they bite it hard
 * on combining marks (the cursor just moves over the letter), but they do
 * handle wider characters fine. The one thing they do have is Arabic makes the
 * cursor go right-to-left but still combining mark's don't quite work and in
 * fact some don't render correctly at all (which do otherwise).
 */
public class LineDimensionsCalculator {
  /**
   * Creates a new {@link LineDimensionsCalculator} from a
   * {@link FontDimensionsCalculator}.
   */
  public static LineDimensionsCalculator create(FontDimensionsCalculator fontCalculator) {
    final LineDimensionsCalculator calculator =
        new LineDimensionsCalculator(new BrowserMeasurementProvider(fontCalculator));
    // add a listener so that we can clear our cache if the dimensions change.
    fontCalculator.addCallback(new FontDimensionsCalculator.Callback() {
      @Override
      public void onFontDimensionsChanged(FontDimensions fontDimensions) {
        LineDimensionsCalculator.clearCharacterCacheDueToZoomChange();
      }
    });
    return calculator;
  }

  /**
   * Creates a new {@link LineDimensionsCalculator} with a custom
   * {@link MeasurementProvider}.
   */
  static LineDimensionsCalculator createWithCustomProvider(MeasurementProvider provider) {
    return new LineDimensionsCalculator(provider);
  }

  /**
   * Specifies how a X-to-column conversion determines the column if the X isn't on the exact column
   * boundary.
   */
  public enum RoundingStrategy {
    ROUND, FLOOR, CEIL;
    
    public int apply(double value) {
      switch (this) {
        case ROUND:
          return (int) Math.round(value);
      
        case FLOOR:
          return (int) Math.floor(value);
      
        case CEIL:
          return (int) Math.ceil(value);
          
        default:
          throw new IllegalStateException("Unexpected value for RoundingStrategy");
      }
    }
  }
  
  /**
   * A cache used to cache the width of special characters. Would be final
   * except there isn't a fast way to clear a map.
   */
  private static JsonStringMap<Double> characterWidthCache = JsonCollections.createMap();

  /**
   * A listener which notifies us of dirty lines. We only have to handle the
   * case where the endLine != startLine since the startLine is handled in the
   * preTextListener.
   */
  private static TextListener textListener = new TextListener() {
    @Override
    public void onTextChange(com.google.collide.shared.document.Document document,
        JsonArray<TextChange> textChanges) {
      for (int i = 0; i < textChanges.size(); i++) {
        TextChange change = textChanges.get(i);
        if (change.getEndLine() != change.getLine()) {
          LineDimensionsUtils.isOffsetNeededAndCache(
              change.getEndLine(), change.getEndColumn(), change.getType());
        }
      }
    }
  };

  /**
   * A listener which allows us to mark the cache dirty before a text change
   * actually takes place.
   */
  private static PreTextListener preTextListener = new PreTextListener() {
    @Override
    public void onPreTextChange(Document document,
        Type type,
        Line line,
        int lineNumber,
        int column,
        String text) {

      /*
       * In the case where text is deleted, we only need to mark ourselves dirty
       * if there is already an OffsetCache. The insert case though requires
       * looking at the newly typed text for special characters.
       */
      LineDimensionsUtils.preTextIsOffsetNeededAndCache(line, column, type, text);
    }
  };

  private final RemoverManager listenerManager = new RemoverManager();
  private final MeasurementProvider measurementProvider;

  private LineDimensionsCalculator(MeasurementProvider measurementProvider) {
    this.measurementProvider = measurementProvider;
  }

  /**
   * Sets the currently opened document so we can listen for mutations.
   */
  public void handleDocumentChange(Document newDocument) {
    // Remove old document listener
    listenerManager.remove();
    // add the new ones
    listenerManager.track(newDocument.getPreTextListenerRegistrar().add(preTextListener));
    listenerManager.track(newDocument.getTextListenerRegistrar().add(textListener));
  }

  /**
   * Converts a column to its x coordinate.
   */
  public double convertColumnToX(Line line, int column) {
    // Simple case we early out
    if (column == 0) {
      return 0;
    }

    if (!LineDimensionsUtils.needsOffset(line)) {
      return simpleConvertColumnToX(line, column);
    }
    return convertColumnToXMeasuringIfNeeded(line, column);
  }

  /**
   * Converts an x coordinate to the Editor column.
   */
  public int convertXToColumn(Line line, double x, RoundingStrategy roundingStrategy) {
    // Easy out (< can happen when selection dragging).
    if (x <= 0) {
      return 0;
    }

    if (!LineDimensionsUtils.needsOffset(line)) {
      return simpleConvertXToColumn(line, x, roundingStrategy);
    }
    return roundingStrategy.apply(convertXToColumnMeasuringIfNeeded(line, x));
  }

  /**
   * Converts column to x using the {@link ColumnOffsetCache} stored on the
   * line, measuring if required.
   */
  private double convertColumnToXMeasuringIfNeeded(Line line, int column) {
    LineDimensionsUtils.markTimeline(getClass(), "Begin converting Column To X via offset cache.");

    ColumnOffsetCache cache = ColumnOffsetCache.getOrCreate(line, getColumnWidth());
    checkColumnInCacheAndMeasureIfNeeded(cache, line, column);
    ColumnOffset offset = cache.getColumnOffsetForColumn(column);

    LineDimensionsUtils.markTimeline(getClass(), "End converting Column To X via offset cache.");
    return smartColumnToX(offset, column);
  }

  /**
   * Converts x to a column using the {@link ColumnOffsetCache} stored on the
   * line, measuring if needed.
   */
  private double convertXToColumnMeasuringIfNeeded(Line line, double x) {
    LineDimensionsUtils.markTimeline(getClass(), "Begin converting X To column via offset cache.");

    ColumnOffsetCache cache = ColumnOffsetCache.getOrCreate(line, getColumnWidth());
    checkXInCacheAndMeasureIfNeeded(cache, line, x);
    Pair<ColumnOffset, Double> offsetAndWidth = cache.getColumnOffsetForX(x, getColumnWidth());

    LineDimensionsUtils.markTimeline(getClass(), "End converting X To column via offset cache.");

    return smartXToColumn(offsetAndWidth.first, offsetAndWidth.second, x);
  }

  /**
   * Smart column to x conversion which converts a column to an x position based
   * on a {@link ColumnOffset}.
   */
  private double smartColumnToX(ColumnOffset offset, int column) {
    if (offset.column == column) {
      return offset.x;
    }

    return offset.x + naiveColumnToX(column - offset.column);
  }

  /**
   * Smart x to column conversion which an x pixel position to a column based on
   * a {@link ColumnOffset}.
   */
  private double smartXToColumn(ColumnOffset offset, double width, double x) {
    double column = offset.column;
    if (x == offset.x) {
      return column;
    } else if (x < offset.x + width) {
      /*
       * We are converting this exact column so lets taken into account this
       * columns length which may be special.
       */
      column += (x - offset.x) / width;
    } else {
      // Figure out the offset in pixels and subtract then convert.
      column += naiveXToColumn(x - offset.x);
    }

    return column;
  }

  /**
   * Naively converts a column to its expected x value not taking into account
   * any special characters.
   */
  private double naiveColumnToX(double column) {
    return column * getColumnWidth();
  }

  /**
   * Naively converts a x pixel value to its expected column not taking into
   * account any special characters.
   */
  private double naiveXToColumn(double x) {
    return x / getColumnWidth();
  }

  /**
   * Finds the adjusted column number due to tab indentation and carriage
   * returns. This is used in the simple case to handle prefixing tabs and the
   * '\r\n' windows line format. Complex cases are handled in the
   * {@link ColumnOffsetCache}.
   */
  private double simpleConvertColumnToX(Line line, int column) {
    // early out when we are at the start of the line
    if (column == 0) {
      return 0;
    }

    LineDimensionsUtils.markTimeline(getClass(), "Calculating simple offset");
    // get any indentation tabs that are affecting us
    int offsetTabColumns = LineDimensionsUtils.getLastIndentationTabCount(line.getText(), column)
        * (LineDimensionsUtils.getTabWidth() - 1);
    int offsetCarriageReturn = 0;
    if (isColumnAffectedByCarriageReturn(line, column)) {
      offsetCarriageReturn = -1;
    }
    LineDimensionsUtils.markTimeline(getClass(), "End calculating simple offset");
    return naiveColumnToX(offsetTabColumns + offsetCarriageReturn + column);
  }

  private int simpleConvertXToColumn(Line line, double x, RoundingStrategy roundingStrategy) {
    if (x == 0) {
      return 0;
    }

    LineDimensionsUtils.markTimeline(getClass(), "Calculating simple offset from x");
    /*
     * we just have to be conscious here of prefix tabs which may be a different
     * width and suffix \r which is 0 width. We deal accordingly.
     */

    /*
     * we divide x by the width of a tab in pixels to overshoot the number of
     * indentation tabs
     */
    int columnIfAllTabs = (int) Math.floor(x / naiveColumnToX(LineDimensionsUtils.getTabWidth()));
    int offsetTabColumns =
        LineDimensionsUtils.getLastIndentationTabCount(line.getText(), columnIfAllTabs);
    assert columnIfAllTabs >= offsetTabColumns : "You appear to be less tabs then you say you are";

    double lineWidthPxWithoutTabs =
        x - (offsetTabColumns * LineDimensionsUtils.getTabWidth() * getColumnWidth());
    int column =
        roundingStrategy.apply(naiveXToColumn(lineWidthPxWithoutTabs) + offsetTabColumns);
    // if we landed on the carriage return column++
    if (column < line.length() && line.getText().charAt(column) == '\r') {
      column++;
    }
    LineDimensionsUtils.markTimeline(getClass(), "End calculating simple offset from x");
    return column;
  }

  /**
   * @return true if a measurement was performed.
   */
  private boolean checkColumnInCacheAndMeasureIfNeeded(
      ColumnOffsetCache cache, Line line, int column) {
    if (cache.isColumnMeasurementNeeded(column)) {
      measureLineStoppingAtColumn(cache, line, column);
      return true;
    }
    return false;
  }

  /**
   * @return true if a measurement was performed.
   */
  private boolean checkXInCacheAndMeasureIfNeeded(ColumnOffsetCache cache, Line line, double x) {
    if (cache.isXMeasurementNeeded(x)) {
      measureLineStoppingAtX(cache, line, x);
      return true;
    }
    return false;
  }

  /**
   * Builds the cache for a line up to or beyond the given endColumn value.
   *
   * @see #measureLine(ColumnOffsetCache, Line, int, double)
   */
  private void measureLineStoppingAtColumn(ColumnOffsetCache cache, Line line, int endColumn) {
    measureLine(cache, line, endColumn, Double.MAX_VALUE);
  }

  /**
   * Builds the cache for a line up to or beyond the given endX value.
   *
   * @see #measureLine(ColumnOffsetCache, Line, int, double)
   */
  private void measureLineStoppingAtX(ColumnOffsetCache cache, Line line, double endX) {
    measureLine(cache, line, Integer.MAX_VALUE, endX);
  }

  /**
   * Builds the cache for a line up to a particular column. Should not be called
   * if the line has already been {@link ColumnOffsetCache#FULLY_MEASURED}.
   *
   * <p>
   * You should only rely on either endColumn or endX, one or the other should
   * be the max value for its data type.
   *
   * @see #measureLineStoppingAtColumn(ColumnOffsetCache, Line, int)
   * @see #measureLineStoppingAtX(ColumnOffsetCache, Line, double)
   *
   * @param endColumn inclusive end column (we will end on or after end)
   * @param endX inclusive end x pixel width (we will end on or after endX)
   */
  private void measureLine(ColumnOffsetCache cache, Line line, int endColumn, double endX) {
    /*
     * Starting at cache.measuredColumn we will use the regex to scan forward to
     * see if we hit an interesting character other than prefixed tab. if we do
     * we'll measure that to that point and append a {@link ColumnOffset} if it
     * is a special size. Rinse and repeat.
     */
    LineDimensionsUtils.markTimeline(getClass(), "Beginning measure line");
    RegExp regexp = UnicodeUtils.regexpNonAsciiTabOrCarriageReturn;
    regexp.setLastIndex(cache.measuredOffset.column);
    MatchResult result = regexp.exec(line.getText());

    if (result != null) {
      double x = 0;
      do {
        // Calculate any x offset up to this point in the line
        ColumnOffset offset = cache.getLastColumnOffsetInCache();
        double baseXOffset = smartColumnToX(offset, result.getIndex());

        /*
         * TODO: we can be smarter here, if i > 1, then this character
         * is a mark. We could separate out the RegExp into non-spacing,
         * enclosing-marks v. spacing-marks and already know which are supposed
         * to be zero-width based on which groups are null.
         */
        String match = result.getGroup(0);
        for (int i = 0; i < match.length(); i++) {
          x = addOffsetForResult(cache, match.charAt(i), result.getIndex() + i, line, baseXOffset);
          baseXOffset = x;
        }
        result = regexp.exec(line.getText());
        // we have to ensure we measure through the last zero-width character.
      } while (result != null && result.getIndex() < endColumn && x < endX);
    }

    if (result == null) {
      cache.measuredOffset = ColumnOffsetCache.FULLY_MEASURED;
      return;
    }

    LineDimensionsUtils.markTimeline(getClass(), "Ending measure line");
  }

  private double addOffsetForResult(
      ColumnOffsetCache cache, char matchedCharacter, int index, Line line, double baseXOffset) {
    /*
     * Get the string up to the current character, special casing tabs since
     * they must render as the correct number of spaces (we replace them when
     * the appropriate number of hard-spaces so the browser doesn't trim them).
     */
    String partialLineText = line.getText().substring(0, index + 1).replace(
        "\t", StringUtils.repeatString("\u00A0", LineDimensionsUtils.getTabWidth()));

    /*
     * Get the width of the string including our special character and if needed
     * append an offset to the cache.
     */
    double expectedWidth = baseXOffset + getColumnWidth();
    double stringWidth = getStringWidth(matchedCharacter, baseXOffset, partialLineText);
    if (stringWidth < baseXOffset) {
      /*
       * This is a annoying condition where certain combining characters can
       * actually change how the previous character is rendered. In some cases
       * actually making it smaller than before. This is fairly annoying. It
       * only happens when some scripts and languages like Arabic with heavy
       * combining marks. This is also possible due to measurement
       * inconsistencies when measuring combining characters.
       *
       * Honestly there's not much we can do, but we make our best attempt to at
       * least provide a consistent cursor experience even if it isn't
       * navigating the characters correctly (not that I would even know,
       * considering I can't speak/read Arabic).
       */
      stringWidth = baseXOffset;
    }
    if (stringWidth != expectedWidth) {
      cache.appendOffset(index + 1, stringWidth, stringWidth - baseXOffset);
    }
    return stringWidth;
  }

  /**
   * Returns the width of a column within the current zoom level.
   */
  private double getColumnWidth() {
    return measurementProvider.getCharacterWidth();
  }

  /**
   * Determines the width of a string using either the cached width of a
   * character of interest or by measuring it using a
   * {@link MeasurementProvider}
   *
   * @param characterOfInterest The character we are interested in which should
   *        also be the last character of the textToMeasure string.
   * @param baseXOffset The base x offset of the column before the character of
   *        interest. The returned result will be this offset + the width of the
   *        characterOfInterest.
   * @param textToMeasureIncludingCharacterOfInterest The string of text to
   *        measure including the character of interest.
   *
   * @return The width of the string which is baseXOffset +
   *         characterOfInterestWidth
   */
  private double getStringWidth(char characterOfInterest, double baseXOffset,
      String textToMeasureIncludingCharacterOfInterest) {
    switch (characterOfInterest) {
      case '\t':
        // base + columnWidth * tab_size_in_columns
        return baseXOffset + LineDimensionsUtils.getTabWidth() * getColumnWidth();
      case '\r':
        // zero-width just return the baseXOffset
        return baseXOffset;
      default:
        Double characterWidth = characterWidthCache.get(String.valueOf(characterOfInterest));
        // if we know the width already return it
        if (characterWidth != null) {
          return baseXOffset + characterWidth;
        }
        // Measure and store the width of the character
        double expectedWidth = baseXOffset + getColumnWidth();
        double width =
            measurementProvider.measureStringWidth(textToMeasureIncludingCharacterOfInterest);

        // cache the width of this character
        characterWidthCache.put(String.valueOf(characterOfInterest), width - baseXOffset);
        return width;
    }
  }

  /**
   * Returns true if the column is past a carriage return at the end of a line.
   */
  private static boolean isColumnAffectedByCarriageReturn(Line line, int column) {
    return line.length() >= 2 && column > line.length() - 2
        && line.getText().charAt(line.length() - 2) == '\r';
  }

  /**
   * Due to differences in how characters measure at different zoom levels (it's
   * not a constant factor for all character types!!!), we just clear the world
   * and rebuild.
   */
  private static void clearCharacterCacheDueToZoomChange() {
    LineDimensionsUtils.markTimeline(LineDimensionsCalculator.class, "Cleared cache due to zoom");
    characterWidthCache = JsonCollections.createMap();
  }
}
