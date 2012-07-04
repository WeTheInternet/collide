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
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.dto.Revision.RevisionType;
import com.google.collide.dto.client.DtoClientImpls.RevisionImpl;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

/**
 *
 */
public class TimelineTest extends GWTTestCase{
  
  Timeline timeline;
  AppContext context;
  
  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  @Override
  public void gwtSetUp() throws Exception{
    super.gwtSetUp();
    context = new MockAppContext();
    timeline = new MockTimeline(new MockFileHistory(context), 
      new FileHistory.View(context.getResources()).timelineView, context);
    FileHistoryApi api = new FileHistoryApi(context, null, timeline, null);
    timeline.setApi(api);

    RevisionImpl revision = RevisionImpl.make();
    revision.setNodeId("12345");
    revision.setTimestamp(Long.toString(new Date().getTime()));
    revision.setRevisionType(RevisionType.AUTO_SAVE);
    revision.setHasUnresolvedConflicts(false);
    revision.setIsFinalResolution(false);
    revision.setPreviousNodesSkipped(0);

    // Add 4 fake timeline nodes
    TimelineNode node = new TimelineNode(
      new TimelineNode.View(context.getResources()), 0, revision, timeline);
    timeline.nodes.add(node);
    node = new TimelineNode(
      new TimelineNode.View(context.getResources()), 1, revision, timeline);
    timeline.nodes.add(node);
    node = new TimelineNode(
      new TimelineNode.View(context.getResources()), 2, revision, timeline);
    timeline.nodes.add(node);
    node = new TimelineNode(
      new TimelineNode.View(context.getResources()), 3, revision, timeline);
    timeline.nodes.add(node);
    node = new TimelineNode(
      new TimelineNode.View(context.getResources()), 4, revision, timeline);
    timeline.nodes.add(node);
    
    timeline.numNodes = 5;
    
    timeline.setActiveRange(timeline.nodes.get(0), timeline.nodes.get(4));
  }
  
  /**
   * Test that you can't drag a range line past the left edge
   */
  public void testDragLeftPastEdge() {
    timeline.moveRange(-MockTimeline.TIMELINE_INTERVAL);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
  }
  
  /**
   * Test that you can't drag a range line past the right edge
   */
  public void testDragRightPastEdge() {
    timeline.moveRange(MockTimeline.TIMELINE_INTERVAL);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
  }
  
  /**
   * Test that you can't drag the left side of the timeline past the left edge
   */
  public void testResizeLeftPastEdges() {
    timeline.moveRangeEdge(timeline.nodes.get(0), -MockTimeline.TIMELINE_INTERVAL);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
  }
  
  /**
   * Test that you can't drag the right side of the timeline past the right edge
   */
  public void testResizeRightPastEdges() {
    timeline.moveRangeEdge(timeline.nodes.get(3), MockTimeline.TIMELINE_INTERVAL);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
  }
  
  /**
   * Test that you can't resize the range line smaller than length of 1
   */
  public void testResize() {
    // Don't allow dragging the left edge to the right or the right
    // edge to the left if length = 1
    
    timeline.setActiveRange(timeline.nodes.get(1), timeline.nodes.get(2));
    timeline.moveRangeEdge(timeline.nodes.get(1), MockTimeline.TIMELINE_INTERVAL);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2));
    
    mouseUp();
    
    timeline.setActiveRange(timeline.nodes.get(1), timeline.nodes.get(2));
    timeline.moveRangeEdge(timeline.nodes.get(2), -MockTimeline.TIMELINE_INTERVAL);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2)); 
  }
  
  /**
   * Test snap-to resizing features
   */
  public void testSnapToResizeLeft() {
    // Move more than 2/3 the way to the next node, should increment
    timeline.moveRangeEdge(timeline.nodes.get(0), 3 * MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
    
    // Move less than 2/3 the way to the next node, should not increment
    timeline.moveRangeEdge(timeline.nodes.get(0), 1);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
    
    // Move less than 2/3 the way to the next node, should not increment
    timeline.moveRangeEdge(timeline.nodes.get(0), MockTimeline.TIMELINE_INTERVAL/3);
    timeline.moveRangeEdge(timeline.nodes.get(0), MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
    
    resetSnapRange(3, 4);
    
    // Move more than 2/3 the way to the next node, should decrement
    timeline.moveRangeEdge(timeline.nodes.get(3), -( 3 * MockTimeline.TIMELINE_INTERVAL/4));
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(2));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
    
    resetSnapRange(2, 4);
    
    // Move less than 2/3 the way to the next node, should not decrement
    timeline.moveRangeEdge(timeline.nodes.get(2), -1);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(2));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
    
    mouseUp();
    
    // Move less than 2/3 the way to the next node, should not decrement
    timeline.moveRangeEdge(timeline.nodes.get(1), -MockTimeline.TIMELINE_INTERVAL/3);
    timeline.moveRangeEdge(timeline.nodes.get(1), -MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(2));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(4));
  }
  
  /**
   * Test snap-to resizing features
   */
  public void testSnapToResizeRight() {
    // Move more than 2/3 the way to the next node, should decrement
    timeline.moveRangeEdge(timeline.nodes.get(4), -3 * MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(3));
    
    // Move less than 2/3 the way to the next node, should not decrement
    timeline.moveRangeEdge(timeline.nodes.get(4), -1);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(3));
    
    // Move less than 2/3 the way to the next node, should not decrement
    timeline.moveRangeEdge(timeline.nodes.get(4), -MockTimeline.TIMELINE_INTERVAL/3);
    timeline.moveRangeEdge(timeline.nodes.get(4), -MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(3));

    resetSnapRange(0, 1);
    
    // Move more than 2/3 the way to the next node, should increment
    timeline.moveRangeEdge(timeline.nodes.get(1), 3 * MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2));
    
    resetSnapRange(0, 2);
    
    // Move less than 2/3 the way to the next node, should not increment
    timeline.moveRangeEdge(timeline.nodes.get(1), 1);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2));
    
    mouseUp();
    
    // Move less than 2/3 the way to the next node, should not increment
    timeline.moveRangeEdge(timeline.nodes.get(3), MockTimeline.TIMELINE_INTERVAL/3);
    timeline.moveRangeEdge(timeline.nodes.get(3), MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2));
  }
   
  /**
   * Test snap-to dragging features
   */
  public void testSnapToDrag() {
    timeline.setActiveRange(timeline.nodes.get(1), timeline.nodes.get(2));
    
    // Move more than 2/3 the way to the next node, should increment
    timeline.moveRange(3 * MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(2));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(3));
    
    resetSnapRange(1, 2);
    
    // Move more than 2/3 the way to the next node, should increment
    timeline.moveRange(((int) ((2.0/3.0) * MockTimeline.TIMELINE_INTERVAL)) + 1);

    assertEquals(timeline.tempLeftRange, timeline.nodes.get(2));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(3));
    
    resetSnapRange(1, 2);
    
    // Move less than 2/3 the way to the next node, should not increment
    timeline.moveRange(1);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2));
    
    resetSnapRange(1, 2);
    
    // Move less than 2/3 the way to the next node, should not increment
    timeline.moveRange(MockTimeline.TIMELINE_INTERVAL/3);
    timeline.moveRange(MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2));
    
    resetSnapRange(1, 2);
    
    // Move more than 2/3 the way to the next node, should increment
    timeline.moveRange(-(3 * MockTimeline.TIMELINE_INTERVAL/4));
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(0));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(1));
    
    resetSnapRange(1, 2);
    
    // Move less than 2/3 the way to the next node, should not increment
    timeline.moveRange(-1);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2));
    
    resetSnapRange(1, 2);
    
    // Move less than 2/3 the way to the next node, should not increment
    timeline.moveRange(-MockTimeline.TIMELINE_INTERVAL/3);
    timeline.moveRange(-MockTimeline.TIMELINE_INTERVAL/4);
    assertEquals(timeline.tempLeftRange, timeline.nodes.get(1));
    assertEquals(timeline.tempRightRange, timeline.nodes.get(2));
  }
  
  public void resetSnapRange(int left, int right) {
    timeline.setActiveRange(timeline.nodes.get(left), timeline.nodes.get(right));
    mouseUp();
  }
  
  public void mouseUp() {
    timeline.setCurrentDragX(0);
    timeline.resetCatchUp();
    timeline.resetLeftRange();
    timeline.resetRightRange();
  }
}
