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

package com.google.collide.client.code.debugging;

import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.SortedList;
import com.google.collide.shared.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;

/**
 * UI component that displays all breakpoints set in the project.
 *
 * TODO: Text changes of the lines that have breakpoints should be
 * reflected in this UI pane.
 */
public class DebuggingSidebarBreakpointsPane extends UiComponent<
    DebuggingSidebarBreakpointsPane.View> {

  public interface Css extends CssResource {
    String root();
    String section();
    String sectionHeader();
    String sectionFileName();
    String sectionFilePath();
    String breakpoint();
    String breakpointIcon();
    String breakpointInactive();
    String breakpointLine();
    String breakpointLineNumber();
  }

  interface Resources extends ClientBundle {
    @Source("DebuggingSidebarBreakpointsPane.css")
    Css workspaceEditorDebuggingSidebarBreakpointsPaneCss();

    @Source("file.png")
    DataResource fileImageResource();

    @Source("breakpointActive.png")
    ImageResource breakpointActiveResource();

    @Source("breakpointInactive.png")
    ImageResource breakpointInactiveResource();
  }

  /**
   * Listener for the user clicks on the breakpoints.
   */
  interface Listener {
    void onBreakpointIconClick(Breakpoint breakpoint);
    void onBreakpointLineClick(Breakpoint breakpoint);
  }

  static class View extends CompositeView<ViewEvents> {
    private final Css css;

    private final EventListener breakpointClickListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        Element target = (Element) evt.getTarget();

        Element breakpoint = CssUtils.getAncestorOrSelfWithClassName(target, css.breakpoint());
        Element section = CssUtils.getAncestorOrSelfWithClassName(breakpoint, css.section());

        int breakpointIndex = DomUtils.getSiblingIndexWithClassName(breakpoint, css.breakpoint());
        int sectionIndex = DomUtils.getSiblingIndexWithClassName(section, css.section());

        if (target.hasClassName(css.breakpointIcon())) {
          getDelegate().onBreakpointIconClick(sectionIndex, breakpointIndex);
        } else {
          getDelegate().onBreakpointLineClick(sectionIndex, breakpointIndex);
        }
      }
    };

    View(Resources resources) {
      css = resources.workspaceEditorDebuggingSidebarBreakpointsPaneCss();

      Element rootElement = Elements.createDivElement(css.root());
      setElement(rootElement);
    }

    @VisibleForTesting
    void addBreakpointSection(int sectionIndex) {
      getElement().insertBefore(createSection(), getSectionElement(sectionIndex));
    }

    @VisibleForTesting
    void removeBreakpointSection(int sectionIndex) {
      getSectionElement(sectionIndex).removeFromParent();
    }

    @VisibleForTesting
    void addBreakpoint(int sectionIndex, int breakpointIndex) {
      getSectionElement(sectionIndex).insertBefore(createBreakpoint(),
          getBreakpointElement(sectionIndex, breakpointIndex));
    }

    @VisibleForTesting
    void removeBreakpoint(int sectionIndex, int breakpointIndex) {
      getBreakpointElement(sectionIndex, breakpointIndex).removeFromParent();
    }

    private void updateBreakpointSection(int sectionIndex, String fileName, String path) {
      Element section = getSectionElement(sectionIndex);
      DomUtils.getFirstElementByClassName(section, css.sectionFileName()).setTextContent(fileName);
      DomUtils.getFirstElementByClassName(section, css.sectionFilePath()).setTextContent(path);
    }

    private void updateBreakpoint(int sectionIndex, int breakpointIndex, boolean active,
        String line, int lineNumber) {
      line = StringUtils.nullToEmpty(line).trim();

      Element breakpoint = getBreakpointElement(sectionIndex, breakpointIndex);
      CssUtils.setClassNameEnabled(breakpoint, css.breakpointInactive(), !active);
      DomUtils.getFirstElementByClassName(breakpoint, css.breakpointLine()).setTextContent(line);
      // TODO: i18n?
      DomUtils.getFirstElementByClassName(breakpoint, css.breakpointLineNumber())
          .setTextContent("line " + (lineNumber + 1));

      // Set a tooltip.
      // TODO: Do we actually need this?
      String tooltip = line + " line " + (lineNumber + 1);
      breakpoint.setTitle(tooltip);
    }

    private String getBreakpointLineText(int sectionIndex, int breakpointIndex) {
      Element breakpoint = getBreakpointElement(sectionIndex, breakpointIndex);
      Element line = DomUtils.getFirstElementByClassName(breakpoint, css.breakpointLine());
      return line.getTextContent();
    }

    private Element createSection() {
      Element section = Elements.createDivElement(css.section());

      // TODO: i18n?
      Element separator = Elements.createSpanElement();
      separator.setInnerHTML("&mdash;");

      Element sectionHeader = Elements.createDivElement(css.sectionHeader());
      sectionHeader.appendChild(Elements.createSpanElement(css.sectionFileName()));
      sectionHeader.appendChild(separator);
      sectionHeader.appendChild(Elements.createSpanElement(css.sectionFilePath()));

      section.appendChild(sectionHeader);
      return section;
    }

    private Element createBreakpoint() {
      Element breakpoint = Elements.createDivElement(css.breakpoint());
      breakpoint.appendChild(Elements.createDivElement(css.breakpointIcon()));
      breakpoint.appendChild(Elements.createSpanElement(css.breakpointLine()));
      breakpoint.appendChild(Elements.createSpanElement(css.breakpointLineNumber()));
      breakpoint.addEventListener(Event.CLICK, breakpointClickListener, false);
      return breakpoint;
    }

    private Element getSectionElement(int sectionIndex) {
      return DomUtils.getNthChildWithClassName(getElement(), sectionIndex, css.section());
    }

    private Element getBreakpointElement(int sectionIndex, int breakpointIndex) {
      Element section = getSectionElement(sectionIndex);
      return DomUtils.getNthChildWithClassName(section, breakpointIndex, css.breakpoint());
    }
  }

  /**
   * The view events.
   */
  private interface ViewEvents {
    void onBreakpointIconClick(int sectionIndex, int breakpointIndex);
    void onBreakpointLineClick(int sectionIndex, int breakpointIndex);
  }

  static DebuggingSidebarBreakpointsPane create(View view) {
    return new DebuggingSidebarBreakpointsPane(view);
  }

  private static final SortedList.Comparator<Breakpoint> SORTING_FUNCTION =
      new SortedList.Comparator<Breakpoint>() {
        @Override
        public int compare(Breakpoint a, Breakpoint b) {
          int result = a.getPath().compareTo(b.getPath());
          if (result != 0) {
            return result;
          }
          return a.getLineNumber() - b.getLineNumber();
        }
      };

  private Listener delegateListener;
  private final SortedList<Breakpoint> breakpoints =
      new SortedList<Breakpoint>(SORTING_FUNCTION);
  private final JsonArray<Integer> breakpointCountBySections = JsonCollections.createArray();

  private final class ViewEventsImpl implements ViewEvents {

    @Override
    public void onBreakpointIconClick(int sectionIndex, int breakpointIndex) {
      Breakpoint breakpoint = getBreakpoint(sectionIndex, breakpointIndex);
      if (breakpoint == null) {
        Log.error(getClass(), "Failed to find a breakpoint at " + sectionIndex + ":"
            + breakpointIndex);
        return;
      }
      if (delegateListener != null) {
        delegateListener.onBreakpointIconClick(breakpoint);
      }
    }

    @Override
    public void onBreakpointLineClick(int sectionIndex, int breakpointIndex) {
      Breakpoint breakpoint = getBreakpoint(sectionIndex, breakpointIndex);
      if (breakpoint == null) {
        Log.error(getClass(), "Failed to find a breakpoint at " + sectionIndex + ":"
            + breakpointIndex);
        return;
      }
      if (delegateListener != null) {
        delegateListener.onBreakpointLineClick(breakpoint);
      }
    }
  }

  @VisibleForTesting
  DebuggingSidebarBreakpointsPane(View view) {
    super(view);

    view.setDelegate(new ViewEventsImpl());
  }

  void setListener(Listener listener) {
    delegateListener = listener;
  }

  void addBreakpoint(Breakpoint breakpoint) {
    int index = breakpoints.add(breakpoint);

    int section = -1;
    int breakpointCount = 0;
    while (breakpointCount < index && section + 1 < breakpointCountBySections.size()) {
      ++section;
      breakpointCount += breakpointCountBySections.get(section);
    }

    // Now, the breakpoint can be inserted into the following sections:
    // 1) {@code section}, if it exists and should contain the new breakpoint
    // 2) {@code section + 1}, if it exists and should contain the new breakpoint
    // 3) A new section

    int breakpointIndexInSection;

    if (index > 0
        && breakpoint.getPath().compareTo(breakpoints.get(index - 1).getPath()) == 0) {
      int num = breakpointCountBySections.get(section);
      breakpointCountBySections.set(section, num + 1);
      breakpointIndexInSection = index - breakpointCount + num;
    } else if (index + 1 < breakpoints.size()
        && breakpoint.getPath().compareTo(breakpoints.get(index + 1).getPath()) == 0) {
      ++section;
      int num = breakpointCountBySections.get(section);
      breakpointCountBySections.set(section, num + 1);
      breakpointIndexInSection = index - breakpointCount;
    } else {
      ++section;
      breakpointCountBySections.splice(section, 0, 1);
      breakpointIndexInSection = 0;
      getView().addBreakpointSection(section);
      getView().updateBreakpointSection(section, breakpoint.getPath().getBaseName(),
          PathUtil.createExcludingLastN(breakpoint.getPath(), 1).getPathString());
    }

    getView().addBreakpoint(section, breakpointIndexInSection);
    getView().updateBreakpoint(section, breakpointIndexInSection, breakpoint.isActive(), "",
        breakpoint.getLineNumber());
  }

  void removeBreakpoint(Breakpoint breakpoint) {
    int index = breakpoints.findIndex(breakpoint);
    if (index < 0) {
      Log.error(getClass(), "Failed to remove a breakpoint: " + breakpoint);
      return;
    }

    breakpoints.remove(index);

    Position position = getBreakpointPosition(index);
    int section = position.sectionIndex;
    int breakpointsInSection = breakpointCountBySections.get(section);

    getView().removeBreakpoint(section, position.breakpointIndex);

    if (breakpointsInSection > 1) {
      breakpointCountBySections.set(section, breakpointsInSection - 1);
    } else {
      breakpointCountBySections.splice(section, 1);
      getView().removeBreakpointSection(section);
    }
  }

  void updateBreakpoint(Breakpoint breakpoint, String line) {
    int index = breakpoints.findIndex(breakpoint);
    if (index < 0) {
      Log.error(getClass(), "Failed to update a breakpoint: " + breakpoint);
      return;
    }

    Position position = getBreakpointPosition(index);
    getView().updateBreakpoint(position.sectionIndex, position.breakpointIndex,
        breakpoint.isActive(), line, breakpoint.getLineNumber());
  }

  boolean hasBreakpoint(Breakpoint breakpoint) {
    return breakpoints.findIndex(breakpoint) >= 0;
  }

  String getBreakpointLineText(Breakpoint breakpoint) {
    int index = breakpoints.findIndex(breakpoint);
    if (index < 0) {
      Log.error(getClass(), "Failed to find a breakpoint: " + breakpoint);
      return "";
    }

    Position position = getBreakpointPosition(index);
    return getView().getBreakpointLineText(position.sectionIndex, position.breakpointIndex);
  }

  int getBreakpointCount() {
    return breakpoints.size();
  }

  private Position getBreakpointPosition(int index) {
    int section = -1;
    int breakpointCount = 0;
    while (breakpointCount <= index && section + 1 < breakpointCountBySections.size()) {
      ++section;
      breakpointCount += breakpointCountBySections.get(section);
    }

    int breakpointsInSection = breakpointCountBySections.get(section);
    int breakpointIndexInSection = index - breakpointCount + breakpointsInSection;

    return new Position(section, breakpointIndexInSection);
  }

  private Breakpoint getBreakpoint(int sectionIndex, int breakpointIndex) {
    int index = breakpointIndex;
    for (int i = 0; i < sectionIndex; ++i) {
      index += breakpointCountBySections.get(i);
    }
    return breakpoints.get(index);
  }

  private static class Position {
    private final int sectionIndex;
    private final int breakpointIndex;

    private Position(int sectionIndex, int breakpointIndex) {
      this.sectionIndex = sectionIndex;
      this.breakpointIndex = breakpointIndex;
    }
  }
}
