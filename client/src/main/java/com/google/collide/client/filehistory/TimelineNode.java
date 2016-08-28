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

package com.google.collide.client.filehistory;

import java.util.Date;
import java.util.List;

import collide.client.util.Elements;

import com.google.collide.client.ClientConfig;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Position;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.dom.MouseMovePauseDetector;
import com.google.collide.client.util.dom.eventcapture.MouseCaptureListener;
import com.google.collide.clientlibs.model.Workspace;
import com.google.collide.dto.Revision;
import com.google.collide.dto.Revision.RevisionType;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.collect.Lists;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MouseEvent;
import elemental.html.DivElement;

/**
 * Representation of a timeline node ("dot") on the timeline widget, and its
 * tooltip with node information
 */
public class TimelineNode extends UiComponent<TimelineNode.View> {

  /**
   * Static factory method for obtaining an instance of the TimelineNode.
   */
  public static TimelineNode create(
      TimelineNode.View view, int index, Revision revision, Timeline timeline) {
    return new TimelineNode(view, index, revision, timeline);
  }

  /**
   * Style names used by the TimelineNode.
   */
  public interface Css extends CssResource {
    String base();

    String currentLeft();

    String currentRight();

    String nodeWrapper();

    String largeNodeWrapper();

    String node();

    String nodeRange();

    String nodeBranch();

    String nodeBranchRange();

    String nodeSync();

    String nodeSyncRange();

    String nodeIndicator();

    String conflictIcon();

    String conflictResolvedIcon();

    // TODO: add deleted icon.

    String label();
  }

  /**
   * CSS and images used by the TimelineNode.
   */
  public interface Resources extends Tooltip.Resources {

    // Regular Node
    @Source("node.png")
    ImageResource node();

    @Source("conflictIcon.png")
    ImageResource conflictIcon();

    @Source("conflictResolvedIcon.png")
    ImageResource conflictResolvedIcon();

    @Source("nodeHover.png")
    ImageResource nodeHover();

    @Source("nodeRange.png")
    ImageResource nodeRange();

    // Branch Node
    @Source("nodeBranch.png")
    ImageResource nodeBranch();

    @Source("nodeBranchHover.png")
    ImageResource nodeBranchHover();

    @Source("nodeBranchRange.png")
    ImageResource nodeBranchRange();

    // Sync Node
    @Source("nodeSync.png")
    ImageResource nodeSync();

    @Source("nodeSyncHover.png")
    ImageResource nodeSyncHover();

    @Source("nodeSyncRange.png")
    ImageResource nodeSyncRange();

    // Current Node
    @Source("nodeCurrent.png")
    ImageResource nodeCurrent();

    @Source("clear.png")
    ImageResource clear();

    @Source("TimelineNode.css")
    Css timelineNodeCss();
  }

  /**
   * The View for the TimelineNode.
   */
  public static class View extends CompositeView<ViewEvents> {

    private final Resources res;
    private final Css css;

    private DivElement nodeIndicator;
    private DivElement node;
    private DivElement nodeWrapper;
    private DivElement label;

    View(TimelineNode.Resources res) {
      super(Elements.createDivElement(res.timelineNodeCss().base()));
      this.res = res;
      this.css = res.timelineNodeCss();

      createDom();
      attachHandlers();
    }

    protected void createDom() {
      getElement().setAttribute("draggable", "true");

      node = Elements.createDivElement(css.node());
      nodeIndicator = Elements.createDivElement(css.nodeIndicator());
      nodeWrapper = Elements.createDivElement(css.nodeWrapper());
      label = Elements.createDivElement(css.label());

      nodeWrapper.appendChild(node);
      nodeWrapper.appendChild(nodeIndicator);

      getElement().appendChild(nodeWrapper);
    }

    protected void attachHandlers() {
      nodeWrapper.setOndblclick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          ViewEvents delegate = getDelegate();
          if (delegate == null) {
            return;
          }
          delegate.onNodeDblClick();
        }
      });

      nodeWrapper.setOnclick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          ViewEvents delegate = getDelegate();
          if (delegate == null) {
            return;
          }
          delegate.onNodeClick(((MouseEvent) evt).isCtrlKey());
        }
      });
    }

    public void attachDragHandler(MouseCaptureListener mouseCaptureListener) {
      nodeWrapper.addEventListener(Event.MOUSEDOWN, mouseCaptureListener, false);
    }

    public void setNodeType(NodeType nodeType) {
      node.addClassName(nodeType.getBaseClassName());
      nodeIndicator.addClassName(nodeType.getIndicatorClassName());
      nodeWrapper.addClassName(nodeType.getWrapperClassName());
    }

    public void addRangeStyles(NodeType nodeType, boolean left) {
      node.addClassName(nodeType.getRangeClassName());
      if (left) {
        getElement().addClassName(css.currentLeft());
      } else {
        getElement().addClassName(css.currentRight());
      }
    }

    public void clearRangeStyles(NodeType nodeType) {
      node.removeClassName(nodeType.getRangeClassName());
      getElement().removeClassName(css.currentLeft());
      getElement().removeClassName(css.currentRight());
    }

    public void setAsCurrentNode() {
      node.setAttribute("current", "true");
      nodeWrapper.removeClassName(css.largeNodeWrapper());
    }
  }

  /**
   * Events reported by the TimelineNode's View.
   */
  private interface ViewEvents {
    void onNodeDblClick();

    void onNodeClick(boolean isCtrlKey);
  }

  /**
   * The delegate implementation for handling events reported by the View.
   */
  private class ViewEventsImpl implements ViewEvents {

    /**
     * On node double click, the range should adjust to be this node -> last
     * node
     */
    @Override
    public void onNodeDblClick() {
      setTempLeftRange(true);
      timeline.nodes.get(timeline.nodes.size() - 1).setTempRightRange(true);

      // Update current range = temp range
      timeline.resetLeftRange();
      timeline.resetRightRange();

      timeline.adjustRangeLine();
    }

    /**
     * On node click, the range should shorten appropriately
     */
    @Override
    public void onNodeClick(boolean isCtrlKey) {
      // Check if dot inside the range line or not
      if (index > timeline.currentLeftRange.index && index < timeline.currentRightRange.index) {

        // If clicked inside the range line,
        if (isCtrlKey) {
          // act as dragging the right side
          setTempRightRange(true);
        } else {
          // act as dragging the left side
          setTempLeftRange(true);
        }
      } else {

        // If clicked outside the range line, find which side it is closest
        // to and update the range line

        if (index < timeline.currentLeftRange.index) {
          setTempLeftRange(true);
        } else if (index > timeline.currentRightRange.index) {
          setTempRightRange(true);
        }
      }

      // Update current range = temp range
      timeline.resetLeftRange();
      timeline.resetRightRange();

      timeline.adjustRangeLine();
    }
  }

  private final MouseCaptureListener mouseCaptureListener = new MouseCaptureListener() {
    @Override
    protected void onMouseMove(MouseEvent evt) {
      mouseMovePauseDetector.handleMouseMove(evt);
      if (!dragging) {
        onNodeDragStart();
        dragging = true;
      }
      onNodeDragMove(getDeltaX());
    }

    @Override
    protected void onMouseUp(MouseEvent evt) {
      onNodeDragEnd();
      dragging = false;
    }
  };

  private final MouseMovePauseDetector mouseMovePauseDetector =
      new MouseMovePauseDetector(new MouseMovePauseDetector.Callback() {
        @Override
        public void onMouseMovePaused() {
          if (timeline.closeEnoughToDot()) {
            timeline.adjustRangeLine();
            timeline.setDiffForRevisions();
          }
        }
      });

  private boolean dragging = false;

  private void onNodeDragStart() {
    // Record original x-coordinate
    timeline.setCurrentDragX(0);

    // Can only drag edge nodes
    TimelineNode that = getNode();
    if (that == timeline.currentLeftRange) {
      timeline.setDrag(true);
      timeline.forceCursor("col-resize");
    } else if (that == timeline.currentRightRange) {
      timeline.setDrag(true);
      timeline.forceCursor("col-resize");
    }
    mouseMovePauseDetector.start();
  }

  private void onNodeDragMove(int delta) {
    if (timeline.getDrag()) {
      timeline.moveRangeEdge(getNode(), delta);
    }
  }

  public void onNodeDragEnd() {
    mouseMovePauseDetector.stop();
    timeline.setDrag(false);
    timeline.removeCursor();
    timeline.resetCatchUp();

    // Update current range = temp range
    timeline.resetLeftRange();
    timeline.resetRightRange();

    timeline.adjustRangeLine();

    timeline.setDiffForRevisions();
  }

  // Timeline Node types (sync, branch)

  static class NodeType {

  /**
   * Static factory method for a NodeType.
   */
    public static NodeType create(Revision revision, Css css) {

      String indicatorClassName = css.nodeIndicator();
      switch (revision.getRevisionType()) {
        case AUTO_SAVE:
          if (revision.getHasUnresolvedConflicts()) {
            indicatorClassName = css.conflictIcon();
          } else if (revision.getIsFinalResolution()) {
            indicatorClassName = css.conflictResolvedIcon();
          }
          return new NodeType(revision.getRevisionType(), css.node(), css.nodeRange(),
              css.nodeWrapper(), indicatorClassName);
        case SYNC_SOURCE:
        case SYNC_MERGED:
          if (revision.getHasUnresolvedConflicts()) {
            indicatorClassName = css.conflictIcon();
          }
          // not possible to be a final conflict resolution node.
          return new NodeType(revision.getRevisionType(), css.nodeSync(), css.nodeSyncRange(),
              css.largeNodeWrapper(), indicatorClassName);
        case BRANCH:
          return new NodeType(revision.getRevisionType(), css.nodeBranch(), css.nodeBranchRange(),
              css.largeNodeWrapper(), indicatorClassName);
        case DELETE:
          // TODO need a DELETE node type or indicator.
          return new NodeType(revision.getRevisionType(), css.node(), css.nodeRange(),
              css.nodeWrapper(), indicatorClassName);
        case MOVE:
          // TODO need a MOVE node type or indicator.
          return new NodeType(revision.getRevisionType(), css.node(), css.nodeRange(),
              css.nodeWrapper(), indicatorClassName);
        case COPY:
          // TODO need a COPY node type or indicator.
          return new NodeType(revision.getRevisionType(), css.node(), css.nodeRange(),
              css.nodeWrapper(), indicatorClassName);
        default:
          throw new IllegalArgumentException("Attempted to create a non-existent NodeType!");
      }
    }

    private final RevisionType type;
    private final String baseClassName;
    private final String rangeClassName;
    private final String wrapperClassName;
    private final String indicatorClassName; //displayed at top-right to indicate node states.

    NodeType(
        RevisionType type, String baseClassName, String rangeClassName, String wrapperClassName,
        String indicatorClassName) {
      this.type = type;
      this.baseClassName = baseClassName;
      this.rangeClassName = rangeClassName;
      this.wrapperClassName = wrapperClassName;
      this.indicatorClassName = indicatorClassName;
    }

    RevisionType getType(){
      return type;
    }

    String getBaseClassName() {
      return baseClassName;
    }

    String getRangeClassName() {
      return rangeClassName;
    }

    String getWrapperClassName() {
      return wrapperClassName;
    }

    String getIndicatorClassName(){
      return indicatorClassName;
    }
  }

  private final Tooltip tooltip;
  private final Timeline timeline;
  private final Revision revision;
  // file path is discovered during file diff.
  private String filePath = "";
  public final int index;
  public final NodeType nodeType;
  public boolean currentNode;

  protected TimelineNode(View view, int index, Revision revision, Timeline timeline) {
    super(view);
    this.timeline = timeline;
    this.revision = revision;
    this.index = index;
    this.nodeType = NodeType.create(revision, getView().css);

    setLabelText();
    setNodeOffset();
    setNodeType();

    Positioner positioner = new Tooltip.TooltipPositionerBuilder().setVerticalAlign(
        VerticalAlign.TOP).setHorizontalAlign(HorizontalAlign.MIDDLE).setPosition(Position.OVERLAP)
        .buildAnchorPositioner(getView().nodeWrapper);
    tooltip = new Tooltip.Builder(getView().res, getView().nodeWrapper, positioner).setTooltipText(
        "").build();
    tooltip.setTitle(getTooltipTitle());

    view.setDelegate(new ViewEventsImpl());
    view.attachDragHandler(mouseCaptureListener);
  }

  private TimelineNode getNode() {
    return this;
  }

  public Revision getRevision() {
    return revision;
  }

  void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  String getFilePath() {
    return filePath;
  }

  public String getRevisionTitle() {
    return filePath + " @ " + getFormattedFullDate();
  }

  void updateTooltipTitle() {
    tooltip.setTitle(getTooltipTitle());
  }

  private String getTooltipTitle() {
    String type = revision.getRevisionType().name();
    if (revision.getRevisionType() == RevisionType.AUTO_SAVE) {
      type = "EDIT";
    }

    Workspace workspaceInfo = timeline.getFileHistoryApi().getWorkspace();
    if (workspaceInfo != null /*&& workspaceInfo.getWorkspaceType() == WorkspaceType.TRUNK*/) {
      type = "SUBMITTED_" + type;
    }
    return type + "  " + getFormattedFullDate();
  }

  private String[] getTooltipText() {
    List<String> text = Lists.newArrayList();

    if (revision.getHasUnresolvedConflicts()) {
      text.add("Has conflicts.");
    } else if (revision.getIsFinalResolution()) {
      text.add("Conflicts resolved.");
    }

    if (ClientConfig.isDebugBuild()) {
      if (revision.getPreviousNodesSkipped() != 0) {
        text.add("Hide " + (revision.getPreviousNodesSkipped() == -1
            ? " unkown # of" : revision.getPreviousNodesSkipped()) + " previous nodes.");
      }
      text.add("Root ID: " + revision.getRootId());
      text.add("ID:" + revision.getNodeId());
    }
    return text.toArray(new String[0]);
  }

  private String getFormattedDate() {
    // If today, only put the time. Else, only put the date.
    // TODO: Figure out what's the best way to display dates like this
    PredefinedFormat format;
    if (dateIsToday(new Date(Long.valueOf(revision.getTimestamp())))) {
      format = PredefinedFormat.TIME_SHORT;
    } else {
      format = PredefinedFormat.DATE_SHORT;
    }

    return getFormattedDate(format);
  }

  private String getFormattedFullDate() {
    return getFormattedDate(PredefinedFormat.DATE_TIME_SHORT);
  }

  private boolean dateIsToday(Date date) {
    Date today = new Date();

    return today.getYear() == date.getYear() && today.getDate() == date.getDate();
  }

  private String getFormattedDate(PredefinedFormat format) {
    String timestamp = revision.getTimestamp();
    Date date = new Date(Long.valueOf(revision.getTimestamp()));
    return DateTimeFormat.getFormat(format).format(date);
  }

  protected void setLabelText() {
    getView().label.setTextContent(getFormattedDate());
  }

  protected void setNodeOffset() {
    getView().getElement().getStyle().setLeft(getNodeOffset(), CSSStyleDeclaration.Unit.PCT);
  }

  public double getNodeOffset() {
    return (index * 100.0 / (timeline.numNodes - 1));
  }

  public void setNodeType() {
    getView().setNodeType(nodeType);
  }

  public void setAsCurrentNode() {
    currentNode = true;
    getView().setAsCurrentNode();
  }

  /*
   * Methods to set current and temp ranges. Temp ranges needed to preserve
   * original (before dragging) range as the "current"
   */

  /**
   * Set the current node to be the new temporary left edge node of the range
   * line, and adjusts range styles. The temporary node becomes the current node
   * upon calling resetLeftRange();
   */
  public void setTempLeftRange(boolean updateDiff) {
    TimelineNode old = timeline.tempLeftRange;
    if (old != null) {
      old.getView().clearRangeStyles(old.nodeType);
    }

    timeline.tempLeftRange = this;
    getView().addRangeStyles(nodeType, true);

    if (updateDiff) {
      timeline.setDiffForRevisions();
    }
  }

  /**
   * Set the current node to be the new temporary right edge node of the range
   * line, and adjusts range styles. The temporary node becomes the current node
   * upon calling resetRightRange();
   */
  public void setTempRightRange(boolean updateDiff) {
    TimelineNode old = timeline.tempRightRange;
    // Clear range styles from the old node
    if (old != null) {
      old.getView().clearRangeStyles(old.nodeType);
    }

    timeline.tempRightRange = this;
    getView().addRangeStyles(nodeType, false);

    if (updateDiff) {
      timeline.setDiffForRevisions();
    }
  }
}
