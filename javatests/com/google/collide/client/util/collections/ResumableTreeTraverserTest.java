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

package com.google.collide.client.util.collections;

import com.google.collide.client.ui.tree.TreeTestUtils;
import com.google.collide.client.util.collections.ResumableTreeTraverser.FinishedCallback;
import com.google.collide.client.util.collections.ResumableTreeTraverser.VisitorApi;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.FileInfo;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * The tests use a file tree since we already have a util method to create
 * a nice tree.  The actual data doesn't affect the traversal.
 */
/**
 * Tests for {@link ResumableTreeTraverser}.
 * 
 */
public class ResumableTreeTraverserTest extends TestCase {

  private static class NodeProvider implements ResumableTreeTraverser.NodeProvider<TreeNodeInfo> {
    @Override
    public Iterator<TreeNodeInfo> getChildrenIterator(TreeNodeInfo node) {
      if (node instanceof FileInfo) {
        return null;
      }
      
      DirInfo dirInfo = (DirInfo) node;
      JsonArray<TreeNodeInfo> children = JsonCollections.createArray();
      children.addAll(dirInfo.getFiles());
      children.addAll(dirInfo.getSubDirectories());
      
      return children.asIterable().iterator();
    }
  }
  
  private static class Visitor implements ResumableTreeTraverser.Visitor<TreeNodeInfo> {
    List<TreeNodeInfo> visitedItems = new ArrayList<TreeNodeInfo>(); 
    
    @Override
    public void visit(TreeNodeInfo item, VisitorApi visitorApi) {
      visitedItems.add(item);
    }
  }
  
  private static class AlwaysPauseVisitor extends Visitor {
    @Override
    public void visit(TreeNodeInfo item, VisitorApi visitorApi) {
      super.visit(item, visitorApi);
      visitorApi.pause();
    }
  }
  
  private static class AlwaysPauseAndResumeVisitor extends Visitor {
    @Override
    public void visit(TreeNodeInfo item, VisitorApi visitorApi) {
      super.visit(item, visitorApi);
      visitorApi.pause();
      visitorApi.resume();
    }
  }
  
  private static class AlwaysPauseAndRemoveVisitor extends Visitor {
    @Override
    public void visit(TreeNodeInfo item, VisitorApi visitorApi) {
      super.visit(item, visitorApi);
      visitorApi.pause();
      visitorApi.remove();
    }
  }
  
  private static class AlwaysRemoveVisitor extends Visitor {
    @Override
    public void visit(TreeNodeInfo item, VisitorApi visitorApi) {
      super.visit(item, visitorApi);
      visitorApi.remove();
    }
  }
  
  private final NodeProvider provider = new NodeProvider(); 

  private DirInfo root;
  private Visitor trivialVisitor;
  
  public ResumableTreeTraverserTest(String name) {
    super(name);
  }

  public void testAlwaysPauseVisitors() {
    Visitor alwaysPauseVisitor = new AlwaysPauseVisitor();
    Visitor alwaysPauseVisitor2 = new AlwaysPauseVisitor();
    Visitor alwaysPauseVisitor3 = new AlwaysPauseVisitor();
    
    ResumableTreeTraverser<TreeNodeInfo> traverser =
        new ResumableTreeTraverser<TreeNodeInfo>(provider, provider.getChildrenIterator(root),
            JsonCollections.createArray(alwaysPauseVisitor, alwaysPauseVisitor2,
                alwaysPauseVisitor3), null);
    // -1 to offset the first call to resume
    int numPauses = -1;
    while (traverser.hasMore()) {
      traverser.resume();
      numPauses++;
      
      // Assert that these visitors are being called in order
      assertEquals(Math.min(numPauses / 3 + 1, trivialVisitor.visitedItems.size()),
          alwaysPauseVisitor.visitedItems.size());
      assertEquals(
          Math.min(numPauses / 3 + ((numPauses % 3 >= 1) ? 1 : 0),
              trivialVisitor.visitedItems.size()), alwaysPauseVisitor2.visitedItems.size());
      assertEquals(
          Math.min(numPauses / 3 + ((numPauses % 3 == 2) ? 1 : 0),
              trivialVisitor.visitedItems.size()), alwaysPauseVisitor3.visitedItems.size());
    }
    
    assertEquals(trivialVisitor.visitedItems, alwaysPauseVisitor.visitedItems);
    assertEquals(trivialVisitor.visitedItems, alwaysPauseVisitor2.visitedItems);
    assertEquals(trivialVisitor.visitedItems, alwaysPauseVisitor3.visitedItems);
    assertEquals(trivialVisitor.visitedItems.size() * 3, numPauses);
  }
  
  public void testAlwaysPauseAndRemoveBlocksSubsequentVisitor() {
    testVisitorWhoseRemovalBlocksSubsequentVisitor(new AlwaysPauseAndRemoveVisitor());
  }
  
  public void testAlwaysRemoveBlocksSubsequentVisitor() {
    testVisitorWhoseRemovalBlocksSubsequentVisitor(new AlwaysRemoveVisitor());
  }
  
  private void testVisitorWhoseRemovalBlocksSubsequentVisitor(Visitor visitor1) {
    Visitor visitor2 = new Visitor();
    
    ResumableTreeTraverser<TreeNodeInfo> traverser =
        new ResumableTreeTraverser<TreeNodeInfo>(provider, provider.getChildrenIterator(root),
            JsonCollections.createArray(visitor1, visitor2), null);
    while (traverser.hasMore()) {
      traverser.resume();
    }

    // Visitor1 deletes immediately, so no children should have been visited
    List<TreeNodeInfo> expectedItems = Lists.newArrayList();
    Iterators.addAll(expectedItems, provider.getChildrenIterator(root));
    assertEquals(expectedItems, visitor1.visitedItems);

    // Since visitor1 always deletes, visitor2 should never be called
    assertEquals(0, visitor2.visitedItems.size());
  }

  public void testVisitorBeforeAlwaysRemoveVisitor2() {
    testVisitorBeforeVisitor2ThatRemoves(new Visitor(), new AlwaysRemoveVisitor());
  }
  
  public void testVisitorBeforeAlwaysPauseAndRemoveVisitor2() {
    testVisitorBeforeVisitor2ThatRemoves(new Visitor(), new AlwaysPauseAndRemoveVisitor());
  }
  
  public void testAlwaysPauseVisitorBeforeAlwaysRemoveVisitor2() {
    testVisitorBeforeVisitor2ThatRemoves(new AlwaysPauseVisitor(), new AlwaysRemoveVisitor());
  }
  
  public void testAlwaysPauseVisitorBeforeAlwaysPauseAndRemoveVisitor2() {
    testVisitorBeforeVisitor2ThatRemoves(new AlwaysPauseVisitor(),
        new AlwaysPauseAndRemoveVisitor());
  }
  
  private void testVisitorBeforeVisitor2ThatRemoves(Visitor visitor1, Visitor visitor2) {
    ResumableTreeTraverser<TreeNodeInfo> traverser =
        new ResumableTreeTraverser<TreeNodeInfo>(provider, provider.getChildrenIterator(root),
            JsonCollections.createArray(visitor1, visitor2), null);
    while (traverser.hasMore()) {
      traverser.resume();
    }

    // Visitor2 deletes immediately, so no children should have been visited
    List<TreeNodeInfo> expectedItems = Lists.newArrayList();
    Iterators.addAll(expectedItems, provider.getChildrenIterator(root));
    assertEquals(expectedItems, visitor1.visitedItems);
    assertEquals(expectedItems, visitor2.visitedItems);
  }
  
  public void testPauseAndResumeSynchronouslyCalled() {
    FinishedCallback finishedCallback =
        EasyMock.createStrictMock(ResumableTreeTraverser.FinishedCallback.class);
    finishedCallback.onTraversalFinished();
    EasyMock.replay(finishedCallback);
    
    ResumableTreeTraverser<TreeNodeInfo> traverser = new ResumableTreeTraverser<TreeNodeInfo>(
        provider, provider.getChildrenIterator(root),
        JsonCollections.createArray(new AlwaysPauseAndResumeVisitor()), finishedCallback);
    while (traverser.hasMore()) {
      traverser.resume();
    }
    
    EasyMock.verify(finishedCallback);
  }
  
  @Override
  protected void setUp() throws Exception {
    root = TreeTestUtils.createMockTree(TreeTestUtils.SERVER_NODE_INFO_FACTORY);
    trivialVisitor = new Visitor(); 

    new ResumableTreeTraverser<TreeNodeInfo>(provider, provider.getChildrenIterator(root),
        JsonCollections.createArray(trivialVisitor), null).resume();
  }
}
