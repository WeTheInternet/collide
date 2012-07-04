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

package com.google.collide.client.search.awesomebox;

import com.google.collide.client.search.awesomebox.AwesomeBox.AwesomeBoxSection;
import com.google.collide.client.search.awesomebox.AwesomeBox.SectionIterationCallback;
import com.google.collide.client.search.awesomebox.AwesomeBoxModel.ContextChangeListener;

import junit.framework.TestCase;

/**
 * Test the context and model of the AwesomeBox.
 */
public class AwesomeBoxModelAndContextTest extends TestCase {

  private AwesomeBoxModel model;
  private AwesomeBoxContext testContext;
  
  private class StubContextChangeListener implements ContextChangeListener {
    private int calledCount = 0;

    @Override
    public void onContextChanged(boolean contextAlreadyActive) {
      if (!contextAlreadyActive) {
        calledCount++;
      }
    }

    public int getCalledCount() {
      return calledCount;
    }
  }

  @Override
  public void setUp() {
    model = new AwesomeBoxModel();
    testContext = new AwesomeBoxContext(new AwesomeBoxContext.Builder());
  }
  
  @Override
  public void tearDown() {
    AwesomeBoxContext.DEFAULT.clearSections();
  }

  public void testDefaultContextEmpty() {
    assertEquals(0, AwesomeBoxContext.DEFAULT.size());
  }

  public void testSectionsCanBeAdded() {
    AwesomeBoxContext.DEFAULT.addSection(new StubAwesomeBoxSection());
    AwesomeBoxContext.DEFAULT.addSection(new StubAwesomeBoxSection());
    AwesomeBoxContext.DEFAULT.addSection(new StubAwesomeBoxSection());

    assertEquals(3, AwesomeBoxContext.DEFAULT.size());
  }

  public void testChangeContextsReturnsRightSections() {
    StubAwesomeBoxSection defaultSection = new StubAwesomeBoxSection();
    StubAwesomeBoxSection testSection1 = new StubAwesomeBoxSection();
    StubAwesomeBoxSection testSection2 = new StubAwesomeBoxSection();
    
    AwesomeBoxContext.DEFAULT.addSection(defaultSection);
    testContext.addSection(testSection1);
    testContext.addSection(testSection2);

    assertSame(defaultSection, model.getContext().getSections().get(0));

    model.changeContext(testContext);
    assertSame(testSection1, model.getContext().getSections().get(0));
    assertSame(testSection2, model.getContext().getSections().get(1));
  }

  public void testTrySetSelection() {
    StubAwesomeBoxSection section1 = new StubAwesomeBoxSection(true);
    StubAwesomeBoxSection section2 = new StubAwesomeBoxSection(false);

    AwesomeBoxContext.DEFAULT.addSection(section1);
    AwesomeBoxContext.DEFAULT.addSection(section2);

    assertTrue(model.trySetSelection(section1, true));
    assertFalse(model.trySetSelection(section2, true));
  }

  public void testGetSetSelection() {
    StubAwesomeBoxSection section1 = new StubAwesomeBoxSection();
    StubAwesomeBoxSection section2 = new StubAwesomeBoxSection(false);

    AwesomeBoxContext.DEFAULT.addSection(section1);
    AwesomeBoxContext.DEFAULT.addSection(section2);
    model.setSelection(section1);
    assertSame(section1, model.getSelection(AwesomeBoxModel.SelectMode.DEFAULT));
  }
  
  public void testSelectFirstItem() {
    StubAwesomeBoxSection section1 = new StubAwesomeBoxSection(false);
    StubAwesomeBoxSection section2 = new StubAwesomeBoxSection(false);
    StubAwesomeBoxSection section3 = new StubAwesomeBoxSection(true);

    AwesomeBoxContext.DEFAULT.addSection(section1);
    AwesomeBoxContext.DEFAULT.addSection(section2);
    AwesomeBoxContext.DEFAULT.addSection(section3);

    model.selectFirstItem();
    assertSame(section3, model.getSelection(AwesomeBoxModel.SelectMode.DEFAULT));
    assertFalse(section1.getHasSelection());
    assertFalse(section2.getHasSelection());
    assertTrue(section3.getHasSelection());
  }
  
  public void testIteration() {
    final StubAwesomeBoxSection section1 = new StubAwesomeBoxSection(false);
    final StubAwesomeBoxSection section2 = new StubAwesomeBoxSection(false);
    final StubAwesomeBoxSection section3 = new StubAwesomeBoxSection(true);

    AwesomeBoxContext.DEFAULT.addSection(section1);
    AwesomeBoxContext.DEFAULT.addSection(section2);
    AwesomeBoxContext.DEFAULT.addSection(section3);

    // backward not quiting iteration
    model.iterateFrom(section3, false, new SectionIterationCallback() {
      @Override
      public boolean onIteration(AwesomeBoxSection section) {
        StubAwesomeBoxSection stub = (StubAwesomeBoxSection) section;
        stub.wasIterated();
        return true;
      }
    });
    assertEquals(1, section1.getAndResetWasIterated());
    assertEquals(1, section2.getAndResetWasIterated());
    assertEquals(0, section3.getAndResetWasIterated());
    
    // forward quiting the iteration
    model.iterateFrom(section1, true, new SectionIterationCallback() {
      @Override
      public boolean onIteration(AwesomeBoxSection section) {
        StubAwesomeBoxSection stub = (StubAwesomeBoxSection) section;
        stub.wasIterated();
        return false;
      }
    });
    assertEquals(0, section1.getAndResetWasIterated());
    assertEquals(1, section2.getAndResetWasIterated());
    assertEquals(0, section3.getAndResetWasIterated());

    // forward not-quiting the iteration
    model.iterateFrom(section1, true, new SectionIterationCallback() {
      @Override
      public boolean onIteration(AwesomeBoxSection section) {
        StubAwesomeBoxSection stub = (StubAwesomeBoxSection) section;
        stub.wasIterated();
        return true;
      }
    });
    assertEquals(0, section1.getAndResetWasIterated());
    assertEquals(1, section2.getAndResetWasIterated());
    assertEquals(1, section3.getAndResetWasIterated());
  }
  
  public void testContextListenerCallback() {
    StubContextChangeListener listener = new StubContextChangeListener();
    model.getContextChangeListener().add(listener);
    
    model.changeContext(testContext);
    model.changeContext(AwesomeBoxContext.DEFAULT);
    
    assertEquals(2, listener.getCalledCount());
  }
}
