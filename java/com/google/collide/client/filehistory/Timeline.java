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

import com.google.collide.client.AppContext;
import com.google.collide.client.common.BaseResources;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.dom.MouseMovePauseDetector;
import com.google.collide.client.util.dom.eventcapture.MouseCaptureListener;
import com.google.collide.dto.Revision;
import com.google.collide.dto.Revision.RevisionType;
import com.google.collide.json.client.JsoArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.css.CSSStyleDeclaration;
import elemental.events.Event;
import elemental.events.MouseEvent;
import elemental.html.DivElement;
import elemental.html.StyleElement;

/**
 * Representation for the FileHistory timeline widget
 *
 */
public class Timeline extends UiComponent<Timeline.View> {

  /**
   * Static factory method for obtaining an instance of the Timeline.
   */
  public static Timeline create(FileHistory fileHistory, AppContext context) {
    return new Timeline(fileHistory, fileHistory.getView().timelineView, context);
  }

  /**
   * Style names used by the Timeline.
   */
  public interface Css extends CssResource {
    String base();

    String rangeLine();

    String rangeLineWrapper();

    String baseLine();

    String nodeContainer();

    String notice();
  }

  /**
   * CSS and images used by the Timeline.
   */
  public interface Resources extends BaseResources.Resources {
    @Source("Timeline.css")
    Css timelineCss();

    @Source("rangeLeft.png")
    ImageResource rangeLeft();

    @Source("rangeRight.png")
    ImageResource rangeRight();
  }

  /**
   * The View for the Timeline.
   */
  public static class View extends CompositeView<Void> {

    private final Resources res;
    private final Css css;

    private DivElement baseLine;
    private DivElement rangeLine;
    private DivElement rangeLineWrapper;
    private DivElement nodeContainer;

    // Keep range line width and left because in CSS they're stored as Strings
    // with unit PCT
    private double rangeLineWidth = 100.0;
    private double rangeLineLeft = 0.0;

    // Base line width for maxNode calculations
    private int baseLineWidth = 0;

    View(Timeline.Resources res) {
      super(Elements.createDivElement(res.timelineCss().base()));
      this.res = res;
      this.css = res.timelineCss();

      // Create DOM and initialize View.
      createDom();
    }

    /**
     * Get revision history and create the DOM for the timeline widget.
     */
    private void createDom() {
      // Instantiate DOM elems.
      baseLine = Elements.createDivElement(css.baseLine());
      rangeLine = Elements.createDivElement(css.rangeLine());
      rangeLineWrapper = Elements.createDivElement(css.rangeLineWrapper());
      nodeContainer = Elements.createDivElement(css.nodeContainer());

      rangeLineWrapper.appendChild(rangeLine);

      getElement().appendChild(baseLine);
      getElement().appendChild(rangeLineWrapper);
      getElement().appendChild(nodeContainer);
    }

    /**
     * Empty the node container of any previous nodes before we create
     * fresh nodes from the getRevisions call
     */
    public void emptyNodeContainer() {
      nodeContainer.setInnerHTML("");
    }

    public void setLoading() {
      emptyNodeContainer();

      toggleTimeline(true);
      // Capture length of baseLine for later use before hiding
      baseLineWidth = baseLine.getOffsetWidth();

      toggleTimeline(false);
    }

    public void setNotice(String text) {
      DivElement notice = Elements.createDivElement(css.notice());
      notice.setTextContent(text);
      nodeContainer.appendChild(notice);
    }

    public int getBaseLineWidth() {
      return baseLineWidth;
    }

    public void toggleTimeline(boolean visible) {
      CssUtils.setDisplayVisibility(rangeLineWrapper, visible);
      CssUtils.setDisplayVisibility(baseLine, visible);
    }

    /**
     * Adjust range line to be between the currentLeftRange and
     * currentRightRange. Used for snapping between a specific range of nodes.
     *
     * @param leftIndex index of the left edge node
     * @param rightIndex index of the right edge node
     * @param numNodes total number of nodes, used for percentage width calculations
     */
    public void adjustRangeLine(int leftIndex, int rightIndex, int numNodes) {
      rangeLineWidth = ((rightIndex - leftIndex) * 100.0) / (numNodes - 1);
      rangeLineLeft = (leftIndex * 100.0 / (numNodes - 1));

      rangeLineWrapper.getStyle().setWidth(rangeLineWidth, CSSStyleDeclaration.Unit.PCT);
      rangeLineWrapper.getStyle().setLeft(rangeLineLeft, CSSStyleDeclaration.Unit.PCT);
    }

    /**
     * Adjust the range line during the middle of a drag (NOT strictly between
     * two different nodes)
     *
     * @param widthDelta increase in range line width, in percentage
     * @param leftDelta increase in range line left offset, in percentage
     */
    public void adjustRangeLineBetween(double widthDelta, double leftDelta) {
      rangeLineWidth += widthDelta;
      rangeLineLeft += leftDelta;

      rangeLineWrapper.getStyle().setWidth(rangeLineWidth, CSSStyleDeclaration.Unit.PCT);
      rangeLineWrapper.getStyle().setLeft(rangeLineLeft, CSSStyleDeclaration.Unit.PCT);
    }

    public void attachDragHandler(MouseCaptureListener mouseCaptureListener) {
      rangeLineWrapper.addEventListener(Event.MOUSEDOWN, mouseCaptureListener, false);
    }

  }

  /* Mouse dragging event listener for range Line */

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
          if (closeEnoughToDot()) {
            adjustRangeLine();
            setDiffForRevisions();
          }
        }
      });
  
  private boolean dragging = false;
  

  private void onNodeDragStart() {
    // Record original x-coordinate
    setCurrentDragX(0);
    forceCursor("-webkit-grabbing");
    setDrag(true);
    mouseMovePauseDetector.start();
  }

  private void onNodeDragMove(int delta) {
    if (getDrag()) {
      moveRange(delta);
    }
  }

  public void onNodeDragEnd() {
    mouseMovePauseDetector.stop();
    setDrag(false);
    removeCursor();
    resetCatchUp();

    // Update current range = temp range
    resetLeftRange();
    resetRightRange();

    adjustRangeLine();

    setDiffForRevisions();
  }

  final AppContext context;
  FileHistoryApi api;
  final FileHistory fileHistory;
  PathUtil path;
  JsoArray<TimelineNode> nodes;
  int numNodes;

  // Minimum interval between (used to calculate max number of nodes) in pixels
  private final int MIN_NODE_INTERNAL = 70;

  // Nodes representing the current left and right sides of the rangeLine
  TimelineNode currentLeftRange;
  TimelineNode currentRightRange;

  // Temporary nodes representing the current left and right sides of the
  // range line during the current action (ex. during a drag). These values
  // are converted into the currentLeftRange and currentRightRange at the
  // end of the drag (see resetLeftRange() and resetRightRange()). We need
  // these because the drag offset is calculated from the original rangeLine
  // position before a drag.
  TimelineNode tempLeftRange;
  TimelineNode tempRightRange;

  // Current dx dragged so far, needed for snapping calculations
  private int currentDragX;

  // Amount the mouse needs to catch up to the range line due to snapping
  private int catchUp;
  private boolean catchUpLeft;

  // Whether the current node is draggable (must be an edge node)
  private boolean drag;

  // Snap-to constants
  private static final double SNAP_THRESHOLD = 2.0/3.0;

  // Cursor style to force when doing a drag action
  private final StyleElement forceDragCursor;

  protected Timeline(FileHistory fileHistory, View view, AppContext context) {
    super(view);
    this.context = context;
    this.fileHistory = fileHistory;
    this.forceDragCursor = Elements.getDocument().createStyleElement();

    this.nodes = JsoArray.create();

    view.attachDragHandler(mouseCaptureListener);
  }

  public void setApi(FileHistoryApi api) {
    this.api = api;
  }

  public void setPath(PathUtil path) {
    this.path = path;
  }

  public void setLoading() {
    // Set loading state for timeline
    getView().setLoading();
  }

  void updateNodeTooltips() {
    for (int i = 0; i < nodes.size(); i++) {
      nodes.get(i).updateTooltipTitle();
    }
  }

  /**
   * Return the number of nodes allowed with a minimum node spacing
   */
  public int maxNumberOfNodes() {
    return (getView().getBaseLineWidth() / MIN_NODE_INTERNAL) + 1;
  }

  private JsoArray<Revision> removeSyncSource(JsoArray<Revision> revisions) {
    JsoArray<Revision> result = JsoArray.create();
    for (int i = 0; i < revisions.size(); i++) {
      if (revisions.get(i).getRevisionType() != RevisionType.SYNC_SOURCE) {
        // For now, we hide SYNC_SOURCE.
        result.add(revisions.get(i));
      }
    }
    return result;
  }

  public void drawNodes(JsoArray<Revision> revisions) {
    // Remove any existing nodes
    getView().emptyNodeContainer();
    nodes.clear();

    if (revisions.size() > 1) {
      getView().toggleTimeline(true);
      // Make file history view default the left side of the diff to the last
      // sync point if any.
      int leftNodeIndex = -1;
      // Draw nodes based on data
      numNodes = revisions.size();
      for (int i = 0; i < revisions.size(); i++) {
        TimelineNode currNode = new TimelineNode(
          new TimelineNode.View(context.getResources()), i, revisions.get(i), this);
        nodes.add(currNode);
        getView().nodeContainer.appendChild(currNode.getView().getElement());

        if (revisions.get(i).getRevisionType() == RevisionType.SYNC_SOURCE) {
          leftNodeIndex = i;
        }
      }

      // By default, the range goes from the first to last node
      TimelineNode leftNode;
      if (leftNodeIndex < 0) {
        leftNode = nodes.get(0);
      } else if (leftNodeIndex == nodes.size() - 1) {
        // When leftNode is the same as the right node, move leftNode left.
        // This can happen when users sync and this file has NO conflict.
        // We have a SYNC_SOURCE and SYNC_MERGED; leftNodeIndex is at
        // SYNC_MERGED, which is the last node.
        // Since revisions.size() > 1, nodes.size() - 2 is valid.
        // To avoid to have left and right diff points to the same revision.
        leftNode = nodes.get(nodes.size() - 2);
      } else {
        leftNode = nodes.get(leftNodeIndex);
      }
      setActiveRange(leftNode, nodes.get(nodes.size() - 1));
      adjustRangeLine();
    } else {
      api.setUnchangedFile(path);
      getView().setNotice("File unchanged.");
    }
  }

  /* Set cursor style */

  public void forceCursor(String type) {
    forceDragCursor.setTextContent("* { cursor: " + type + " !important; }");
    Elements.getBody().appendChild(forceDragCursor);
  }

  public void removeCursor() {
    forceDragCursor.removeFromParent();
  }

  /* Getter and setter methods for private Timeline fields */

  public void setCurrentDragX(int previousDragX) {
    this.currentDragX = previousDragX;
  }

  public int getCurrentDragX() {
    return currentDragX;
  }

  public void setDrag(boolean drag) {
    this.drag = drag;
  }

  public boolean getDrag() {
    return drag;
  }

  public void resetCatchUp() {
    catchUp = 0;
  }

  /**
   * If not valid move for left or right, add distance mouse moved to
   * catchup to close the gap
   * @param dx
   */
  public void incrementCatchUp(int dx) {
    catchUp += dx;
    catchUpLeft = dx < 0;
  }

  /**
   * Update the current drag and catchUp variables to reflect the post autosnap
   * state
   * @param snap snap threshold in pixels
   * @param offset distance we auto-snapped over, need compensate in mouse movements
   */
  public void updateSnapVariables(int snap, int offset) {
    // Subtract the distance we just "snapped" to
    currentDragX += ((currentDragX > 0) ? -snap : snap);
    catchUp += ((currentDragX > 0) ? -offset : offset);

    // Save which direction you're currently going in. Let changing directions
    // be OK.
    catchUpLeft = currentDragX > 0;
  }

  /* Utility methods for snap-to/dragging calculations */

  /**
   * Return the distance (in pixels) of the distance between two nodes on
   * the timeline.
   */
  public int intervalInPx() {
    return getView().baseLine.getOffsetWidth() / (numNodes - 1);
  }

  /**
   * Return the horizontal delta dragged, as a percent of the length
   * of the timeline (because the width of the timeline is recorded
   * as a percentage).
   *
   * @param dx
   * @return
   */
  public double percentageMoved(int dx) {
    return (dx * 100.0) / getView().baseLine.getOffsetWidth();
  }

  /*
   * Reset range methods - encompasses resetting nodes, code, and labels
   */

  public void resetLeftRange() {
    currentLeftRange = tempLeftRange;
  }

  public void resetRightRange() {
    currentRightRange = tempRightRange;
  }


  /**
   * Adjust rangeline to be between the temp left and right edge nodes.
   */
  public void adjustRangeLine() {
    getView().adjustRangeLine(tempLeftRange.index, tempRightRange.index, numNodes);
  }

  // TODO: If moving back and forth really fast, mouse gets out of
  // sync with the range line (there's a gap). Maybe this is due to arithmetic
  // rounding errors? Investigate and fix.

  /**
   * Controls the edge of the range line currently being dragged. If it is a
   * valid drag, calculates how the rangeline should be moved in terms of width
   * changed (percentage) and left offset. Includes auto-snap while dragging if
   * dragged more than 2/3 the way to the next node. Also, "catchup" is allocated
   * to compensate for the gap between the mouse and the range line after auto-snapping
   *
   * @param currentNode the node currently being dragged
   * @param dx the current drag dx recorded (+ is to the right, - is to the left)
   */
  public void moveRangeEdge(TimelineNode currentNode, int dx) {

    // Continue recording and calculating snapping as usual if the mouse
    // doesn't need to catch up
    if (!catchUp(dx)) {
      double percentMoved = percentageMoved(dx);

      currentDragX += dx;
      // Check if dragging left or right edge node
      if (currentNode == currentLeftRange && validLeftEdgeMove(percentMoved < 0)) {
        // Left: need to set new left and extend width

        // Add to left and subtract from width
        getView().adjustRangeLineBetween(-percentMoved, percentMoved);

      } else if (currentNode == currentRightRange && validRightEdgeMove(percentMoved < 0)) {
        // Right: only need to extend width

        // Add to width
        getView().adjustRangeLineBetween(percentMoved, 0);
      } else {
        currentDragX -= dx;
        incrementCatchUp(dx);
      }

      int snap = (int) (SNAP_THRESHOLD * intervalInPx());
      int offset = (int) ((1 - SNAP_THRESHOLD) * intervalInPx());

      // If > snapThreshold away, snapTo the next one
      if (currentDragX > snap || -currentDragX > snap) {
        snapToDot(currentNode);
        adjustRangeLine();

        updateSnapVariables(snap, offset);
      }
    }
  }

  public void moveRange(int dx) {
    if(!catchUp(dx)) {
      double percentMoved = percentageMoved(dx);
      boolean left = percentMoved < 0;

      currentDragX += dx;
      if ((left && validLeftMove()) || (!left && validRightMove())) {
        // Add percent moved to the left offset, width stays the same
        getView().adjustRangeLineBetween(0, percentMoved);
      } else {
        currentDragX -= dx;
        incrementCatchUp(dx);
      }

      int snap = (int) (SNAP_THRESHOLD * intervalInPx());
      int offset = (int) ((1 - SNAP_THRESHOLD) * intervalInPx());

      // If > snapThreshold away, snapTo the next one
      if (currentDragX > snap || -currentDragX > snap) {
        snapToRange();
        adjustRangeLine();

        updateSnapVariables(snap, offset);
      }
    }
  }

  boolean closeEnoughToDot() {
    return Math.abs(currentDragX) < Math.min(30, (1 - SNAP_THRESHOLD) * intervalInPx());
  }

  /**
   * Because of auto-snapping, there's an awkward gap between the mouse and
   * the edge of the range line. Let the mouse catch up to the next node before
   * moving the range line with mouse movements again.
   *
   * @param dx number of pixels dragged
   * @return if the mouse needs to catch up
   */
  public boolean catchUp(int dx) {
    int error = 1;
    boolean goingLeft = dx > 0;

    if ((goingLeft && catchUp < -error) || (!goingLeft && catchUp > error)) {

      // Reset catchup if decide to move mouse in the opposite direction before
      // reaching the next node
      if (goingLeft == catchUpLeft) {
        catchUp += dx;
      } else {
        resetCatchUp();
      }

      // Skip moving the range line, let the mouse catch up to the snapping
      return true;
    }
    return false;
  }

  /*
   * Snap-to methods for dragging the rangeLine
   */

  /**
   * Snap dragging to the next left or right (depending on which direction you
   * are currently dragging) node, if it is a valid drag direction.
   *
   * @param currentNode node currently being dragged
   */
  public void snapToDot(TimelineNode currentNode) {
    boolean left = currentDragX < 0;
    int passed = !left ? 1 : -1;

    // Check if dragging the left or right edge node
    if (currentNode == currentLeftRange && validLeftEdgeMove(left)) {

      // Set the node we "snapped to" by dragging as the new left
      nodes.get(tempLeftRange.index + passed).setTempLeftRange(false);
    } else if (currentNode == currentRightRange && validRightEdgeMove(left)) {

      // Set the node we "snapped to" by dragging as the new right
      nodes.get(tempRightRange.index + passed).setTempRightRange(false);
    }
  }

  public void snapToRange() {
    boolean left = currentDragX < 0;
    int passed = !left ? 1 : -1;

    // Check that dragging is valid depending on the drag direction
    if ((left && validLeftMove()) || (!left && validRightMove())) {

      // Set new rangeline range
      setTempRange(nodes.get(tempLeftRange.index + passed),
        nodes.get(tempRightRange.index + passed), false);
    }
  }

  /**
   * Set the temporary range left and right edges at the same time. Used
   * in snapToRange().
   *
   * @param nextLeft the next left edge to be set
   * @param nextRight the next right edge to be set
   */
  private void setTempRange(TimelineNode nextLeft, TimelineNode nextRight, boolean updateDiff) {
    TimelineNode oldLeft = tempLeftRange;
    TimelineNode oldRight = tempRightRange;

    if (oldRight != null && oldLeft != null) {
      oldLeft.getView().clearRangeStyles(oldLeft.nodeType);
      oldRight.getView().clearRangeStyles(oldRight.nodeType);
    }

    tempLeftRange = nextLeft;
    tempRightRange = nextRight;

    nextLeft.getView().addRangeStyles(nextLeft.nodeType, true);
    nextRight.getView().addRangeStyles(nextRight.nodeType, false);

    if (updateDiff) {
      setDiffForRevisions();
    }
  }

  public void setActiveRange(TimelineNode nextLeft, TimelineNode nextRight) {
    setTempRange(nextLeft, nextRight, true);

    currentLeftRange = nextLeft;
    currentRightRange = nextRight;
  }

  public void setDiffForRevisions() {
    fileHistory.changeLeftRevisionTitle(tempLeftRange.getRevisionTitle());
    fileHistory.changeRightRevisionTitle(tempRightRange.getRevisionTitle());
    api.setFile(path, tempLeftRange.getRevision(), tempRightRange.getRevision());
  }

  void setDiffFilePaths(String leftFilePath, String rightFilePath) {
    if (tempLeftRange != null) {
      tempLeftRange.setFilePath(leftFilePath);
      fileHistory.changeLeftRevisionTitle(tempLeftRange.getRevisionTitle());
    } else {
      fileHistory.changeLeftRevisionTitle(leftFilePath);
    }

    if (tempRightRange != null) {
      tempRightRange.setFilePath(rightFilePath);
      fileHistory.changeRightRevisionTitle(tempRightRange.getRevisionTitle());
    } else {
      fileHistory.changeRightRevisionTitle(rightFilePath);
    }
  }
  /**
   * Don't allow dragging to the left past the first node
   *
   * @return if dragging to the left is valid
   */
  public boolean validLeftMove() {
    return !(currentDragX < 0 && tempLeftRange.index == 0);
  }

  /**
   * Don't allow dragging to the right past the last node
   *
   * @return if dragging to the right is valid
   */
  public boolean validRightMove() {
    return !(currentDragX > 0 && tempRightRange.index == numNodes - 1);
  }

  /**
   * Don't allow dragging the left edge right past the first node or
   * dragging the left edge to the right if length = 1
   *
   * @param dragLeft drag direction (true for left, false for right)
   * @return if the current direction is a valid direction for the left edge node
   */
  public boolean validLeftEdgeMove(boolean dragLeft) {
    return (!dragLeft || validLeftMove())
        && !(!dragLeft && currentDragX > 0 && tempRightRange.index - tempLeftRange.index == 1);
  }

  /**
   * Don't allow dragging the right edge right past the last node or
   * dragging the right edge to the left if length = 1
   *
   * @param dragLeft drag direction (true for left, false for right)
   * @return if the current direction is a valid direction for the right edge node
   */
  public boolean validRightEdgeMove(boolean dragLeft) {
    return (dragLeft || validRightMove())
        && !(dragLeft && currentDragX < 0 && tempRightRange.index - tempLeftRange.index == 1);
  }

  FileHistoryApi getFileHistoryApi() {
    return api;
  }
}
