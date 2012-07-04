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

import static com.google.collide.dto.DocOpComponent.Type.DELETE;
import static com.google.collide.dto.DocOpComponent.Type.INSERT;
import static com.google.collide.dto.DocOpComponent.Type.RETAIN;
import static com.google.collide.dto.DocOpComponent.Type.RETAIN_LINE;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocOpComponent;
import com.google.collide.dto.DocOpComponent.Delete;
import com.google.collide.dto.DocOpComponent.Insert;
import com.google.collide.dto.DocOpComponent.Retain;
import com.google.collide.dto.DocOpComponent.RetainLine;
import com.google.collide.dto.server.ServerDocOpFactory;
import com.google.collide.dto.shared.DocOpFactory;
import com.google.collide.shared.Pair;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.ot.Composer.ComposeException;

import org.junit.Assert;

/**
 * Utility methods for testing document operations.
 *
 */
public class DocOpTestUtils extends Assert {

  public static void assertDocOpEquals(DocOp a, DocOp b) {
    try {
      assertSize(a.getComponents().size(), b);
      for (int i = 0; i < a.getComponents().size(); i++) {
        assertDocOpComponentEquals(a.getComponents().get(i), b.getComponents().get(i));
      }
    } catch (AssertionError e) {
      AssertionError newE =
          new AssertionError("DocOps not equal:\n" + DocOpUtils.toString(a, false) + "\n"
              + DocOpUtils.toString(b, false));
      newE.initCause(e);
      throw newE;
    }
  }

  public static void assertDocOpComponentEquals(DocOpComponent a, DocOpComponent b) {
    assertEquals("DocOpComponents are not equal", a.getType(), b.getType());

    switch (a.getType()) {
      case INSERT:
        assertEquals(((Insert) a).getText(), ((Insert) b).getText());
        break;

      case DELETE:
        assertEquals(((Delete) a).getText(), ((Delete) b).getText());
        break;

      case RETAIN:
        assertEquals(((Retain) a).getCount(), ((Retain) b).getCount());
        assertEquals(((Retain) a).hasTrailingNewline(), ((Retain) b).hasTrailingNewline());
        break;

      case RETAIN_LINE:
        assertEquals(((RetainLine) a).getLineCount(), ((RetainLine) b).getLineCount());
        break;

      default:
        assert false : "Fix test";
    }
  }

  public static void assertDelete(String expectedDeleteText, DocOp op, int index) {
    DocOpComponent component = op.getComponents().get(index);
    assertEquals(DELETE, component.getType());
    assertEquals(expectedDeleteText, ((Delete) component).getText());
  }

  public static void assertInsert(String expectedInsertText, DocOp op, int index) {
    DocOpComponent component = op.getComponents().get(index);
    assertEquals(INSERT, component.getType());
    assertEquals(expectedInsertText, ((Insert) component).getText());
  }

  public static void assertRetain(int expectedRetainCount, boolean expectedHasTrailingNewline,
      DocOp op, int index) {
    DocOpComponent component = op.getComponents().get(index);
    assertEquals(RETAIN, component.getType());
    Retain retain = (Retain) component;
    assertEquals(expectedRetainCount, retain.getCount());
    assertEquals(expectedHasTrailingNewline, retain.hasTrailingNewline());
  }

  public static void assertRetainLine(int expectedRetainLineCount, DocOp op, int index) {
    DocOpComponent component = op.getComponents().get(index);
    assertEquals(RETAIN_LINE, component.getType());
    RetainLine retainLine = (RetainLine) component;
    assertEquals(expectedRetainLineCount, retainLine.getLineCount());
  }

  public static void assertSize(int expectedComponentsSize, DocOp op) {
    assertEquals("DocOp sizes aren't equal", expectedComponentsSize, op.getComponents().size());
  }
  
  public static void assertCompose(DocOp expected, DocOp a, DocOp b) {
    try {
      assertDocOpEquals(expected, compose(a, b));
    } catch (ComposeException e) {
      throw new AssertionError(e);
    }
  }
  
  public static void assertComposeFails(DocOp a, DocOp b) {
    try {
      compose(a, b);
      throw new AssertionError("Compose should have failed");
    } catch (Composer.ComposeException e) {
    }
  }
  
  /**
   * @see #assertCompose(DocOp, DocOp, DocOp)
   */
  public static DocOp compose(DocOp a, DocOp b) throws ComposeException {
    Pair<DocOp, DocOp> composedDocOps =
        composeWithBothStartingStates(ServerDocOpFactory.INSTANCE, a, b);
    assertDocOpEquals(composedDocOps.first, composedDocOps.second);
    return composedDocOps.first; 
  }

  public static DocOp asDocOp(TextChange textChange) {
    return DocOpUtils.createFromTextChange(ServerDocOpFactory.INSTANCE, textChange);
  }
  
  public static Pair<DocOp, DocOp> composeWithBothStartingStates(DocOpFactory factory, DocOp a,
      DocOp b) throws ComposeException {
    
    ComposeException e1 = null;
    DocOp composedDocOp1 = null;
    try {
      composedDocOp1 = Composer.composeWithStartState(factory, a, b, false);
    } catch (ComposeException e) {
      e1 = e;
    }
    
    ComposeException e2 = null;
    DocOp composedDocOp2 = null;
    try {
      composedDocOp2 = Composer.composeWithStartState(factory, a, b, true);
    } catch (ComposeException e) {
      e2 = e;
    }
    
    if ((e1 == null) != (e2 == null)) {
      throw new IllegalArgumentException(
          "One way of composition had an exception, the other didn't", e1 != null ? e1 : e2);
    } else if (e1 != null /* which means e2 != null too */) {
      throw e1;
    }
    
    return Pair.of(composedDocOp1, composedDocOp2);
  }
}
