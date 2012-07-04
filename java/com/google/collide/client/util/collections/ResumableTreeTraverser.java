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

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.util.Iterator;

/*
 * This class uses iterators since they inherently save traversal state and
 * allow for removing the current item. (Less for work for this class to do.)
 */
/**
 * A helper to visit a tree of objects with features like pause/resume.
 * 
 */
public class ResumableTreeTraverser<E> {

  /**
   * Listener that is notified when the traversal finishes.
   */
  public interface FinishedCallback {
    void onTraversalFinished();
  }
  
  /**
   * Provider for the nodes in the tree.
   */
  public interface NodeProvider<E> {
    /**
     * Returns an iterator for the children of {@code node}, or null if
     * {@code node} doesn't have children.
     */
    Iterator<E> getChildrenIterator(E node);
  }

  /**
   * A visitor that is called for each node traversed in the tree.
   */
  public interface Visitor<E> {
    void visit(E item, VisitorApi visitorApi);
  }

  /**
   * An object that encapsulates various API available to a
   * {@link ResumableTreeTraverser.Visitor}.
   */
  public static class VisitorApi {
    private ResumableTreeTraverser<?> treeTraverser;
    private boolean wasRemovedCalled;
    
    private VisitorApi(ResumableTreeTraverser<?> treeTraverser) {
      this.treeTraverser = treeTraverser;
    }
   
    /**
     * Pauses the tree traversal.
     */
    public void pause() {
      treeTraverser.pause();
    }

    /**
     * Resumes a paused tree traversal.
     */
    public void resume() {
      treeTraverser.resume();
    }
    
    /**
     * Removes the node currently being visited.
     */
    public void remove() {
      if (!wasRemovedCalled) {
        treeTraverser.currentNodeIterator.remove();
        wasRemovedCalled = true;
      }
    }
    
    private boolean resetRemoveState() {
      boolean previousWasRemovedCalled = wasRemovedCalled;
      wasRemovedCalled = false;
      return previousWasRemovedCalled;
    }
  }

  /**
   * If paused during a dispatch, this will be the dispatch state so we can
   * resume exactly where we left off.
   */
  private static class DispatchPauseState<E> {
    Iterator<? extends Visitor<E>> dispatchIterator;
    E dispatchNode;
    
    DispatchPauseState(Iterator<? extends Visitor<E>> visitorIterator, E dispatchNode) {
      this.dispatchIterator = visitorIterator;
      this.dispatchNode = dispatchNode;
    }
  }
  
  private final NodeProvider<E> adapter; 
  private final FinishedCallback finishedCallback;
  private final VisitorApi visitorApi;
  private final Iterable<? extends Visitor<E>> visitorIterable;
  
  private boolean isPaused;
  private DispatchPauseState<E> dispatchPauseState;
  
  private JsonArray<Iterator<E>> nodeIteratorStack = JsonCollections.createArray();
  private Iterator<E> currentNodeIterator;
  
  private boolean isDispatching;
  
  public ResumableTreeTraverser(NodeProvider<E> adapter, Iterator<E> nodes,
      JsonArray<? extends Visitor<E>> visitors, FinishedCallback finishedCallback) {
    this.adapter = adapter;
    this.finishedCallback = finishedCallback;
    this.visitorIterable = visitors.asIterable();
    this.visitorApi = new VisitorApi(this);
    
    nodeIteratorStack.add(nodes);
    isPaused = true;
  }
  
  @VisibleForTesting
  boolean hasMore() {
    return nodeIteratorStack.size() > 0;
  }
  
  /**
   * Initially starts the traversal or resumes a previously paused traversal. If this is called from
   * a {@link Visitor#visit(Object, VisitorApi)} dispatch, then the traversal will be resumed after
   * that visit method returns. If this is not called from a visit dispatch, then the traversal will
   * be resumed immediately synchronously.
   */
  public void resume() {
    Preconditions.checkArgument(isPaused);
    isPaused = false;
    
    if (!isDispatching) {
      resumeTraversal();
    }
  }
  
  private void resumeTraversal() {
    if (nodeIteratorStack.size() == 0) {
      // Nothing to do, exit early
      return;
    }
    
    while (!isPaused && (nodeIteratorStack.size() > 0 || dispatchPauseState != null)) {
      currentNodeIterator = nodeIteratorStack.isEmpty() ? null : nodeIteratorStack.peek();
      
      while (!isPaused && ((currentNodeIterator != null && currentNodeIterator.hasNext())
          || dispatchPauseState != null)) {
        
        E node;
        Iterator<? extends Visitor<E>> visitorIterator;
        
        if (dispatchPauseState == null) {
          node = currentNodeIterator.next();
          visitorIterator = visitorIterable.iterator();
        } else {
          node = dispatchPauseState.dispatchNode;
          visitorIterator = dispatchPauseState.dispatchIterator;
          dispatchPauseState = null;
        }
        
        dispatchToVisitors(visitorIterator, node, visitorApi);
        /*
         * A visitor could have paused at this point, if you're doing real work
         * below, make sure to have a !isPaused check
         */
        
        if (!isPaused) {
          boolean wasNodeRemoved = visitorApi.resetRemoveState();
          if (!wasNodeRemoved) {
            Iterator<E> childrenIterator = adapter.getChildrenIterator(node);
            if (childrenIterator != null) {
              nodeIteratorStack.add(childrenIterator);
              currentNodeIterator = childrenIterator;
            }
          }
        }
      }
      
      if (!isPaused) {
        if (!nodeIteratorStack.isEmpty()) {
          nodeIteratorStack.pop();
          
          if (nodeIteratorStack.size() == 0 && finishedCallback != null) {
            finishedCallback.onTraversalFinished();
          }
        }
      }
    }
  }

  public void pause() {
    Preconditions.checkArgument(!isPaused);
    
    isPaused = true;
  }
  
  private void dispatchToVisitors(Iterator<? extends Visitor<E>> visitorIterator, E node,
      VisitorApi visitorApi) {
    
    // !wasRemovedCalled will prevent subsequent visitors from being called
    while (!isPaused && visitorIterator.hasNext() && !visitorApi.wasRemovedCalled) {
      isDispatching = true;
      try {
        visitorIterator.next().visit(node, visitorApi);
      } finally {
        isDispatching = false;
      }
    }
    
    if (isPaused) {
      // The visitor paused
      dispatchPauseState = new DispatchPauseState<E>(visitorIterator, node);
    }
  }
}
