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

package com.google.collide.shared.ot;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocOpComponent;
import com.google.collide.dto.shared.DocOpFactory;
import com.google.collide.json.shared.JsonArray;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.util.Iterator;

/*
 * Derived from Wave's Composer class. We forked it because we have new doc op
 * components, and removed some of Wave's that aren't applicable.
 *
 * The operations being composed are A and B. A occurs before B, so B must
 * account for A's changes.
 *
 * Each of the State subclasses model a possible state during the composing of
 * the two doc ops. This structure assumes that one of the doc op's current
 * components is longer lived (for example, spans more characters) than the
 * other doc op's current component. Given this, each State subclass is just
 * modeling all the possible combinations. For example, the subclass
 * ProcessingAForBRetain's responsibility is to keep the current state of the
 * retain component from the B doc op, and process components from A. As soon as
 * all of the characters being retained by the componenet from B is finished,
 * the state will likely flip-flop to ProcessingBForAXxx.
 */
/**
 * Composes document operations for the code editor.
 */
public class Composer {
  /**
   * Exception thrown when a composition fails.
   */
  public static class ComposeException extends Exception {
    private ComposeException(String message, Exception e) {
      super(message, e);
    }
  }

  /**
   * Runtime exception used internally by this class. The processing states
   * implement {@link DocOpCursor} which does not throw these exceptions, so we
   * model them as runtime exceptions and have an outer catch around the entire
   * transformation that converts these to the public exception.
   */
  private static class InternalComposeException extends RuntimeException {
    private InternalComposeException(String message) {
      super(message);
    }
    
    private InternalComposeException(String message, Throwable t) {
      super(message, t);
    }
  }

  /**
   * Base class for any state that is processing A's components.
   */
  private abstract class ProcessingA extends State {

    /**
     * Since A occurs before B, B won't have any components that align with A's
     * delete (B doesn't even know about the text that A deleted.) So, we pass
     * through the delete without ever touching any of B's components.
     */
    @Override
    public void delete(String aDeleteText) {
      output.delete(aDeleteText);
    }

    @Override
    boolean isProcessingB() {
      return false;
    }
  }

  /**
   * State that models an outstanding delete component from B.
   */
  private class ProcessingAForBDelete extends ProcessingA {
    private String bDeleteText;

    ProcessingAForBDelete(String bDeleteText) {
      this.bDeleteText = bDeleteText;
    }

    @Override
    public void insert(String aInsertText) {
      if (aInsertText.length() <= bDeleteText.length()) {
        cancel(aInsertText.length());
      } else {
        curState = new ProcessingBForAInsert(aInsertText.substring(bDeleteText.length()));
      }
    }

    @Override
    public void retain(int aRetainCount, boolean aRetainHasTrailingNewline) {
      if (aRetainCount <= bDeleteText.length()) {
        output.delete(bDeleteText.substring(0, aRetainCount));
        cancel(aRetainCount);
      } else {
        output.delete(bDeleteText);
        curState =
            new ProcessingBForARetain(aRetainCount - bDeleteText.length(),
                aRetainHasTrailingNewline);
      }
    }

    @Override
    public void retainLine(int aRetainLineCount) {
      // B is modifying a previously retained line
      output.delete(bDeleteText);

      if (bDeleteText.endsWith("\n") || isLastComponentOfB) {
        // B's deletion finishes a line, so A's retain line is affected
        if (aRetainLineCount == 1) {
          curState = defaultState;
        } else {
          curState = new ProcessingBForARetainLine(aRetainLineCount - 1);
        }
      } else {
        /*
         * B's deletion is part of a line without finishing it, so A's retain
         * line is unaffected, we just have to set the state to processing A's
         * retain line (and so will iterate through B's components)
         */
        curState = new ProcessingBForARetainLine(aRetainLineCount);
      }
    }

    private void cancel(int count) {
      Preconditions.checkArgument(count <= bDeleteText.length(),
          "Cannot cancel if A's component is longer than B's");
      
      if (count < bDeleteText.length()) {
        bDeleteText = bDeleteText.substring(count);
      } else {
        curState = defaultState;
      }
    }
  }

  private class ProcessingAForBRetain extends ProcessingA {
    private int bRetainCount;
    private final boolean bRetainHasTrailingNewline;

    ProcessingAForBRetain(int bRetainCount, boolean bRetainHasTrailingNewline) {
      this.bRetainCount = bRetainCount;
      this.bRetainHasTrailingNewline = bRetainHasTrailingNewline;
    }

    @Override
    public void insert(String aInsertText) {
      if (aInsertText.length() <= bRetainCount) {
        output.insert(aInsertText);
        cancel(aInsertText.length());
      } else {
        output.insert(aInsertText.substring(0, bRetainCount));
        curState = new ProcessingBForAInsert(aInsertText.substring(bRetainCount));
      }
    }

    @Override
    public void retain(int aRetainCount, boolean aRetainHasTrailingNewline) {
      if (aRetainCount <= bRetainCount) {
        output.retain(aRetainCount, aRetainHasTrailingNewline);
        cancel(aRetainCount);
      } else {
        output.retain(bRetainCount, bRetainHasTrailingNewline);
        curState =
            new ProcessingBForARetain(aRetainCount - bRetainCount, aRetainHasTrailingNewline);
      }
    }

    @Override
    public void retainLine(int aRetainLineCount) {
      // B is modifying a previously retained line
      output.retain(bRetainCount, bRetainHasTrailingNewline);

      if (bRetainHasTrailingNewline || isLastComponentOfB) {
        if (aRetainLineCount == 1) {
          curState = defaultState;
        } else {
          curState = new ProcessingBForARetainLine(aRetainLineCount - 1);
        }
      } else {
        curState = new ProcessingBForARetainLine(aRetainLineCount);
      }
    }

    private void cancel(int count) {
      Preconditions.checkArgument(count <= bRetainCount,
          "Cannot cancel if A's component is longer than B's");

      if (count < bRetainCount) {
        bRetainCount -= count;
      } else {
        curState = defaultState;
      }
    }
  }

  private class ProcessingAForBRetainLine extends ProcessingA {
    private int bRetainLineCount;

    ProcessingAForBRetainLine(int bRetainLineCount) {
      this.bRetainLineCount = bRetainLineCount;
    }

    @Override
    public void insert(String aInsertText) {
      // B is retaining the line that A modified
      output.insert(aInsertText);

      boolean aInsertTextHasNewline = aInsertText.endsWith("\n");
      if (aInsertTextHasNewline || isLastComponentOfA) {
        cancelLines(1, aInsertTextHasNewline);
      }
    }

    @Override
    public void retain(int aRetainCount, boolean aRetainHasTrailingNewline) {
      // B is retaining the line that A modified
      output.retain(aRetainCount, aRetainHasTrailingNewline);

      if (aRetainHasTrailingNewline || isLastComponentOfA) {
        cancelLines(1, aRetainHasTrailingNewline);
      }
    }

    @Override
    public void retainLine(int aRetainLineCount) {
      // A and B are retaining some lines
      int minRetainLineCount = Math.min(aRetainLineCount, bRetainLineCount);
      output.retainLine(minRetainLineCount);

      if (aRetainLineCount == bRetainLineCount) {
        curState = defaultState;
      } else if (aRetainLineCount == minRetainLineCount) {
        cancelLines(minRetainLineCount, true);
      } else if (bRetainLineCount == minRetainLineCount) {
        curState = new ProcessingBForARetainLine(aRetainLineCount - minRetainLineCount);
      }
    }

    private void cancelLines(int cancelLineCount, boolean hasNewline) {
      if (hasNewline) {
        bRetainLineCount -= cancelLineCount;
      }

      if (isLastComponentOfA) {
        transitionForLastComponentOfAAndBRetainLine(bRetainLineCount);
      } else if (bRetainLineCount == 0) {
        curState = defaultState;
      }
    }
  }

  private abstract class ProcessingB extends State {
    @Override
    public void insert(String text) {
      output.insert(text);
    }

    @Override
    boolean isProcessingB() {
      return true;
    }
  }

  private class ProcessingBForAFinished extends ProcessingB {
    /**
     * Tracks whether B has used a retain line component to match any
     * potentially leftover (unmatched) text on the last line of A.
     * 
     * A few examples:
     * <ul>
     * <li>A is R(2, true), R(5) and B is RL(1), D(2), RL(1). The use of B's
     * second RL(1) to match the last three characters in A's R(5) would lead to
     * this variable being set to true.</li>
     * <li>There is also a potential for this to be true when B's RL is matching
     * empty text from A. For example, the document text is "Z\n",
     * A is R(2, true) and B is RL(2). A does not have a component for the
     * empty-texted last line, but B does (the second line of the RL(2)).</li>
     * </ul>
     */
    private boolean hasBUsedRlToMatchLeftoverTextOnLastLineOfA;
    
    ProcessingBForAFinished(boolean hasBUsedRlToMatchLeftoverTextOnLastLineOfA) {
      this.hasBUsedRlToMatchLeftoverTextOnLastLineOfA = hasBUsedRlToMatchLeftoverTextOnLastLineOfA;
    }

    @Override
    public void delete(String text) {
      throw new InternalComposeException("A finished, B cannot have a delete");
    }

    @Override
    public void retain(int count, boolean hasTrailingNewline) {
      throw new InternalComposeException("A finished, B cannot have a retain");
    }

    @Override
    public void retainLine(int lineCount) {
      if (lineCount == 1 && !hasBUsedRlToMatchLeftoverTextOnLastLineOfA) {
        output.retainLine(1);
        hasBUsedRlToMatchLeftoverTextOnLastLineOfA = true;
      } else {
        throw new InternalComposeException("A finished, B cannot have a retain line");
      }
    }
  }

  private class ProcessingBForAInsert extends ProcessingB {
    private String aInsertText;

    ProcessingBForAInsert(String aInsertText) {
      this.aInsertText = aInsertText;
    }

    @Override
    public void delete(String bDeleteText) {
      if (bDeleteText.length() <= aInsertText.length()) {
        cancel(bDeleteText.length());
      } else {
        curState = new ProcessingAForBDelete(bDeleteText.substring(aInsertText.length()));
      }
    }

    @Override
    public void retain(int bRetainCount, boolean bRetainHasTrailingNewline) {
      if (bRetainCount <= aInsertText.length()) {
        output.insert(aInsertText.substring(0, bRetainCount));
        cancel(bRetainCount);
      } else {
        output.insert(aInsertText);
        curState =
            new ProcessingAForBRetain(bRetainCount - aInsertText.length(),
                bRetainHasTrailingNewline);
      }
    }

    @Override
    public void retainLine(int bRetainLineCount) {
      assert bRetainLineCount > 0;

      // B is retaining the line where A modified
      output.insert(aInsertText);

      if (aInsertText.endsWith("\n")) {
        bRetainLineCount--;
      }

      transitionForAInsertOrRetainAndBRetainLine(bRetainLineCount);
    }

    private void cancel(int bCount) {
      if (bCount < aInsertText.length()) {
        aInsertText = aInsertText.substring(bCount);
      } else {
        curState = defaultState;
      }
    }
  }

  private class ProcessingBForARetain extends ProcessingB {
    private int aRetainCount;
    private final boolean aRetainHasTrailingNewline;

    ProcessingBForARetain(int aRetainCount, boolean aRetainHasTrailingNewline) {
      this.aRetainCount = aRetainCount;
      this.aRetainHasTrailingNewline = aRetainHasTrailingNewline;
    }

    @Override
    public void delete(String bDeleteText) {
      if (bDeleteText.length() <= aRetainCount) {
        output.delete(bDeleteText);
        cancel(bDeleteText.length());
      } else {
        output.delete(bDeleteText.substring(0, aRetainCount));
        curState = new ProcessingAForBDelete(bDeleteText.substring(aRetainCount));
      }
    }

    @Override
    public void retain(int bRetainCount, boolean bRetainHasTrailingNewline) {
      if (bRetainCount <= this.aRetainCount) {
        output.retain(bRetainCount, bRetainHasTrailingNewline);
        cancel(bRetainCount);
      } else {
        output.retain(aRetainCount, aRetainHasTrailingNewline);
        curState =
            new ProcessingAForBRetain(bRetainCount - aRetainCount, bRetainHasTrailingNewline);
      }
    }

    @Override
    public void retainLine(int bRetainLineCount) {
      Preconditions.checkArgument(bRetainLineCount > 0, "Must retain more than one line");
      
      output.retain(aRetainCount, aRetainHasTrailingNewline);

      if (aRetainHasTrailingNewline) {
        bRetainLineCount--;
      }
      
      transitionForAInsertOrRetainAndBRetainLine(bRetainLineCount);
    }

    private void cancel(int count) {
      if (count < aRetainCount) {
        aRetainCount -= count;
      } else {
        curState = defaultState;
      }
    }
  }

  private class ProcessingBForARetainLine extends ProcessingB {
    private int aRetainLineCount;

    ProcessingBForARetainLine(int aRetainLineCount) {
      this.aRetainLineCount = aRetainLineCount;
    }

    @Override
    public void insert(String bInsertText) {
      super.insert(bInsertText);

      if (isLastComponentOfB) {
        cancelLines(1);
      }
    }

    @Override
    public void delete(String bDeleteText) {
      // A is retaining the line that B modified
      output.delete(bDeleteText);

      if (bDeleteText.endsWith("\n") || isLastComponentOfB) {
        cancelLines(1);
      }
    }

    @Override
    public void retain(int bRetainCount, boolean bRetainHasTrailingNewline) {
      // A is retaining the line that B modified
      output.retain(bRetainCount, bRetainHasTrailingNewline);

      if (bRetainHasTrailingNewline || isLastComponentOfB) {
        cancelLines(1);
      }
    }

    @Override
    public void retainLine(int bRetainLineCount) {
      // A and B are retaining some lines
      int minRetainLineCount = Math.min(aRetainLineCount, bRetainLineCount);
      output.retainLine(minRetainLineCount);

      if (aRetainLineCount == bRetainLineCount) {
        curState = defaultState;
      } else if (bRetainLineCount == minRetainLineCount) {
        cancelLines(minRetainLineCount);
      } else if (aRetainLineCount == minRetainLineCount) {
        curState = new ProcessingAForBRetainLine(bRetainLineCount - minRetainLineCount);
      }
    }

    private void cancelLines(int cancelLineCount) {
      aRetainLineCount -= cancelLineCount;

      if (aRetainLineCount == 0) {
        curState = defaultState;
      }
    }
  }

  private static abstract class State implements DocOpCursor {
    abstract boolean isProcessingB();
  }

  public static DocOp compose(DocOpFactory factory, DocOp a, DocOp b)
      throws ComposeException {
    try {
      return new Composer(factory, a, b).composeImpl(false);
    } catch (InternalComposeException e) {
      throw new ComposeException("Could not compose operations:\na: "
          + DocOpUtils.toString(a, true) + "\nb: " + DocOpUtils.toString(b, true) + "\n", e);
    }
  }

  @VisibleForTesting
  public static DocOp composeWithStartState(DocOpFactory factory, DocOp a, DocOp b,
      boolean startWithSpecificProcessingAState) throws ComposeException {
    try {
      return new Composer(factory, a, b).composeImpl(startWithSpecificProcessingAState);
    } catch (InternalComposeException e) {
      throw new ComposeException("Could not compose operations:\na: "
          + DocOpUtils.toString(a, true) + "\nb: " + DocOpUtils.toString(b, true) + "\n", e);
    }
  }

  public static DocOp compose(DocOpFactory factory, Iterable<DocOp> docOps)
      throws ComposeException {
    Iterator<DocOp> iterator = docOps.iterator();
    DocOp prevDocOp = iterator.next();
    while (iterator.hasNext()) {
      prevDocOp = compose(factory, prevDocOp, iterator.next());
    }

    return prevDocOp;
  }

  private final DocOp a;

  private final DocOp b;

  private final DocOpCapturer output;

  private final ProcessingA defaultState = new ProcessingA() {
    @Override
    public void insert(String aInsertText) {
      curState = new ProcessingBForAInsert(aInsertText);
    }

    @Override
    public void retain(int aRetainCount, boolean aRetainHasTrailingNewline) {
      curState = new ProcessingBForARetain(aRetainCount, aRetainHasTrailingNewline);
    }

    @Override
    public void retainLine(int aRetainLineCount) {
      if (isLastComponentOfB && aRetainLineCount == 1 && isLastComponentOfA) {
        // This catches the RL(1) that matches nothing
        // Essentially curState = defaultState;
      } else {
        curState = new ProcessingBForARetainLine(aRetainLineCount);
      }
    }
  };

  private State curState = defaultState;

  /**
   * State for use by processors that is true if A is on its last component. The
   * last component of A can cancel B's retain line even if A's last component
   * does not end with a newline or is not a retain line.
   */
  private boolean isLastComponentOfA;
  /** Similar to {@link #isLastComponentOfA} but for B */
  private boolean isLastComponentOfB;

  private Composer(DocOpFactory factory, DocOp a, DocOp b) {
    this.a = a;
    this.b = b;

    output = new DocOpCapturer(factory, true);
  }

  /**
   * @param startWithSpecificProcessingAState the allows the caller to begin the
   *        compose with an alternate start state. Normally, the first state is
   *        a trivial ProcessingA that just creates a ProcessingBForAXxx.
   *        However, we could also start the compose with a ProcessingAForBXxx.
   *        If true, we will attempt to do the latter. The two paths should
   *        eventually lead to the same solution.
   */
  private DocOp composeImpl(boolean startWithSpecificProcessingAState) {
    int aIndex = 0;
    JsonArray<DocOpComponent> aComponents = a.getComponents();

    int bIndex = 0;
    JsonArray<DocOpComponent> bComponents = b.getComponents();

    /*
     * Note the "!= INSERT": There isn't a ProcessingAForBInsert. What that
     * implementation would like is emit B's insertion, and then flip to
     * ProcessingBForAXxx, which is what the defaultState will do.
     */
    if (!bComponents.isEmpty() && startWithSpecificProcessingAState
        && bComponents.get(0).getType() != DocOpComponent.Type.INSERT) {
      curState = createSpecificProcessingAState(aComponents.get(0), bComponents.get(0));
      bIndex++;
    } else {
      curState = defaultState;
    }
    
    isLastComponentOfB = bIndex == bComponents.size();

    while (aIndex < aComponents.size()) {
      /*
       * The state from the previous iteration could be a "processing B for A
       * finished" which is of type "processing B", but in that case, we would
       * not have continued to this iteration since the invariant above would
       * not have passed.
       */
      assert !curState.isProcessingB();
      
      isLastComponentOfA = aIndex == aComponents.size() - 1;
      DocOpUtils.acceptComponent(aComponents.get(aIndex++), curState);
      
      // Notice the different invariant compared to the outer while-loop
      while (curState.isProcessingB() && !isProcessingBForAFinished()) {
        if (bIndex >= bComponents.size()) {
          throw new InternalComposeException("Mismatch in doc ops");
        }

        isLastComponentOfB = bIndex == bComponents.size() - 1;
        DocOpUtils.acceptComponent(bComponents.get(bIndex++), curState);
      }
      
      /*
       * At this point, curState must either be processing A, or processing B
       * after A is finished
       */
    }

    if (curState != defaultState && !isProcessingBForAFinished() && !isBRetainingRestOfLastLine()) {
      throw new InternalComposeException("Invalid state");
    }
    
    if (bIndex < bComponents.size()) {
      
      if (curState == defaultState) {
        curState = new ProcessingBForAFinished(false);
      }
      
      while (bIndex < bComponents.size()) {
        isLastComponentOfB = bIndex == bComponents.size() - 1;
        DocOpUtils.acceptComponent(bComponents.get(bIndex++), curState);
      }
    }

    return output.getDocOp();
  }

  private ProcessingA createSpecificProcessingAState(DocOpComponent a, DocOpComponent b) {
    switch (b.getType()) {
      case DocOpComponent.Type.DELETE:
        return new ProcessingAForBDelete(((DocOpComponent.Delete) b).getText());

      case DocOpComponent.Type.INSERT:
        throw new IllegalArgumentException(
            "Cannot create a specific ProcessingA state for B insertion");

      case DocOpComponent.Type.RETAIN:
        return new ProcessingAForBRetain(((DocOpComponent.Retain) b).getCount(),
            ((DocOpComponent.Retain) b).hasTrailingNewline());

      case DocOpComponent.Type.RETAIN_LINE:
        return new ProcessingAForBRetainLine(((DocOpComponent.RetainLine) b).getLineCount());
        
      default:
        throw new IllegalArgumentException("Unknown component type with ordinal: " + b.getType());
    }
  }

  /**
   * Trivial method for cleaner syntax at the call sites (no instanceof there)
   */
  private boolean isProcessingBForAFinished() {
    return curState instanceof ProcessingBForAFinished;
  }
  
  /**
   * Trivial method for clear syntax at the call sites.
   */
  private boolean isBRetainingRestOfLastLine() {
    return curState instanceof ProcessingAForBRetainLine && isLastComponentOfA && isLastComponentOfB
        && ((ProcessingAForBRetainLine) curState).bRetainLineCount == 1;
  }

  private void transitionForAInsertOrRetainAndBRetainLine(int remainingBRetainLineCount) {
    if (isLastComponentOfA) {
      transitionForLastComponentOfAAndBRetainLine(remainingBRetainLineCount);
    } else {
      if (remainingBRetainLineCount == 0) {
        curState = defaultState;
      } else {
        curState = new ProcessingAForBRetainLine(remainingBRetainLineCount);
      }
    }
  }
  
  /**
   * @param remainingBRetainLineCount the remaining retain line count of B
   *        (after any newline that may exist in A)
   */
  private void transitionForLastComponentOfAAndBRetainLine(int remainingBRetainLineCount) {
    switch (remainingBRetainLineCount) {
      case 0:
        curState = new ProcessingBForAFinished(false);
        break;
      case 1:
        curState = new ProcessingBForAFinished(true);
        break;
      default:
        // This is an invalid state
        curState = new ProcessingAForBRetainLine(remainingBRetainLineCount);
        break;
    }
  }
}
