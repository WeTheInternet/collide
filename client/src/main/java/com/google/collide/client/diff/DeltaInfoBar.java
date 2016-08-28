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

package com.google.collide.client.diff;

import collide.client.util.Elements;

import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.dto.DiffStatsDto;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;
import elemental.html.DivElement;

/**
 * Presenter for the graphical delta statistics of a change. This presenter
 * shows a set of bars that depict the change in percentage of
 * added/deleted/changed/unmodified lines.
 *
 */
public class DeltaInfoBar extends UiComponent<DeltaInfoBar.View> {

  /**
   * Static factory method for obtaining an instance of the DeltaInfoBar.
   */
  public static DeltaInfoBar create(Resources resources, DiffStatsDto diffStats,
      boolean includeTooltip) {
    View view = new View(resources);
    DeltaInfoBar deltaInfoBar = new DeltaInfoBar(resources, view, includeTooltip);
    deltaInfoBar.setStats(diffStats);
    return deltaInfoBar;
  }

  public interface Css extends CssResource {
    String added();

    String bar();

    String count();

    String inline();

    String modified();

    String deleted();

    String unmodified();
  }

  public interface Resources extends Tooltip.Resources {
    @Source("DeltaInfoBar.css")
    Css deltaInfoBarCss();
  }

  public static class View extends CompositeView<Void> {
    @VisibleForTesting
    DivElement barsDiv;
    private DivElement container;
    private DivElement countDiv;
    private final Resources res;
    private final Css css;

    private View(Resources res) {
      this.res = res;
      this.css = res.deltaInfoBarCss();
      setElement(createDom());
    }

    /**
     * Create several bars of the given style and append them to the element.
     */
    private void createBars(Element element, int bars, String style) {
      for (int i = 0; i < bars; i++) {
        DivElement bar = Elements.createDivElement(css.bar());
        bar.addClassName(style);
        element.appendChild(bar);
      }
    }

    private Element createDom() {
      container = Elements.createDivElement(css.inline());
      barsDiv = Elements.createDivElement(css.inline());
      countDiv = Elements.createDivElement(css.inline(), css.count());
      container.appendChild(barsDiv);
      container.appendChild(countDiv);
      return container;
    }
  }

  @VisibleForTesting
  static final int BAR_COUNT = 12;

  /**
   * The maximum number of affected lines to display. If the maximum number of
   * affected lines exceeds this value, we display >##### instead of the full
   * number. Used to improve formatting.
   */
  private static final int MAX_AFFECTED = 10000;

  /**
   * For a given component, calculate the number of bars to display relative to
   * the total. For a non-zero component, at least one bar will always be shown.
   * Bar counts are always rounded down.
   */
  @VisibleForTesting
  static int calculateBars(int component, int total) {
    if (total == 0) {
      // Prevent divide-by-zero.
      return 0;
    }
    double bars = (((double) component / (double) total) * BAR_COUNT);
    // Force at least one bar if we have any of that type
    if (component > 0 && bars < 1) {
      bars = 1;
    }
    return (int) bars;
  }

  private final Css css;
  private Tooltip tooltip;
  private final boolean includeTooltip;

  private DeltaInfoBar(Resources resources, View view, boolean includeTooltip) {
    super(view);
    css = resources.deltaInfoBarCss();
    this.includeTooltip = includeTooltip;
  }

  /**
   * Cleans up the tooltip used by this info bar.
   */
  public void destroy() {
    if (tooltip != null) {
      tooltip.destroy();
      tooltip = null;
    }
  }

  /**
   * Calculates and display the components for the given {@link DiffStatsDto}.
   */
  public void setStats(DiffStatsDto diffStats) {
    // Cleanup any old state.
    destroy();

    DivElement barsDiv = getView().barsDiv;
    barsDiv.setInnerHTML("");
    int total = getTotal(diffStats);
    int addedBars = calculateBars(diffStats.getAdded(), total);
    int deletedBars = calculateBars(diffStats.getDeleted(), total);
    int modifiedBars = calculateBars(diffStats.getChanged(), total);
    int unmodifiedBars = Math.max(0, BAR_COUNT - (addedBars + deletedBars + modifiedBars));

    getView().createBars(barsDiv, addedBars, css.added());
    getView().createBars(barsDiv, deletedBars, css.deleted());
    getView().createBars(barsDiv, modifiedBars, css.modified());
    getView().createBars(barsDiv, unmodifiedBars, css.unmodified());

    int affected = getAffected(diffStats);
    if (affected > MAX_AFFECTED) {
      getView().countDiv.setTextContent(">" + String.valueOf(MAX_AFFECTED));
    } else {
      getView().countDiv.setTextContent(String.valueOf(affected));
    }

    // Create a tooltip for the bar.
    if (includeTooltip) {
      tooltip = Tooltip.create(getView().res, getView().container, VerticalAlign.MIDDLE,
          HorizontalAlign.RIGHT, getDescription(diffStats));
    }
  }

  /**
   * Get the total number of lines affected by the change.
   */
  private static int getAffected(DiffStatsDto diffStats) {
    return diffStats.getAdded() + diffStats.getChanged() + diffStats.getDeleted();
  }

  /**
   * Get the textual description of this line count suitable for the UI.
   */
  private static String getDescription(DiffStatsDto diffStats) {
    return "" + diffStats.getAdded() + " added, " + diffStats.getDeleted() + " deleted, "
        + diffStats.getChanged() + " changed (" + diffStats.getUnchanged() + " unchanged)";
  }

  /**
   * Get the total count for each component of the change.
   */
  private static int getTotal(DiffStatsDto diffStats) {
    return getAffected(diffStats) + diffStats.getUnchanged();
  }
}
