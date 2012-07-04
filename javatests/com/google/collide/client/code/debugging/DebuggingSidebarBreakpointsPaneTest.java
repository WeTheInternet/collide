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

import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link DebuggingSidebarBreakpointsPane}.
 */
public class DebuggingSidebarBreakpointsPaneTest extends GWTTestCase {

  private JsonArray<String> viewMessages;
  private int breakpointCount;
  private DebuggingSidebarBreakpointsPane debuggingSidebarBreakpointsPane;

  @Override
  protected void gwtSetUp() {
    viewMessages = JsonCollections.createArray();
    breakpointCount = 0;
    DebuggingSidebarBreakpointsPane.View viewDecorator =
        new DebuggingSidebarBreakpointsPane.View(new MockAppContext().getResources()) {

          @Override
          void addBreakpointSection(int sectionIndex) {
            viewMessages.add("add section " + sectionIndex);
            super.addBreakpointSection(sectionIndex);
          }

          @Override
          void removeBreakpointSection(int sectionIndex) {
            viewMessages.add("remove section " + sectionIndex);
            super.removeBreakpointSection(sectionIndex);
          }

          @Override
          void addBreakpoint(int sectionIndex, int breakpointIndex) {
            ++breakpointCount;
            viewMessages.add("add breakpoint " + sectionIndex + ":" + breakpointIndex);
            super.addBreakpoint(sectionIndex, breakpointIndex);
          }

          @Override
          void removeBreakpoint(int sectionIndex, int breakpointIndex) {
            --breakpointCount;
            viewMessages.add("remove breakpoint " + sectionIndex + ":" + breakpointIndex);
            super.removeBreakpoint(sectionIndex, breakpointIndex);
          }
        };
    debuggingSidebarBreakpointsPane = new DebuggingSidebarBreakpointsPane(viewDecorator);
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testAddAndRemoveBreakpoint() {
    assertAddBreakpoint("/js/foo.js", 10, "add section 0,add breakpoint 0:0");
    assertEquals(1, breakpointCount);
    assertRemoveBreakpoint("/js/foo.js", 10, "remove breakpoint 0:0,remove section 0");
    assertEquals(0, breakpointCount);
  }

  public void testAddBreakpointsForOneFile() {
    assertAddBreakpoint("/js/foo.js", 10, "add section 0,add breakpoint 0:0");
    assertAddBreakpoint("/js/foo.js", 20, "add breakpoint 0:1");
    assertAddBreakpoint("/js/foo.js", 30, "add breakpoint 0:2");
    assertAddBreakpoint("/js/foo.js", 5, "add breakpoint 0:0");
    assertAddBreakpoint("/js/foo.js", 0, "add breakpoint 0:0");
    assertAddBreakpoint("/js/foo.js", 15, "add breakpoint 0:3");

    assertEquals(6, breakpointCount);

    assertRemoveBreakpoint("/js/foo.js", 15, "remove breakpoint 0:3");
    assertRemoveBreakpoint("/js/foo.js", 30, "remove breakpoint 0:4");

    assertAddBreakpoint("/js/foo.js", 35, "add breakpoint 0:4");

    assertRemoveBreakpoint("/js/foo.js", 5, "remove breakpoint 0:1");
    assertRemoveBreakpoint("/js/foo.js", 0, "remove breakpoint 0:0");
    assertRemoveBreakpoint("/js/foo.js", 35, "remove breakpoint 0:2");
    assertRemoveBreakpoint("/js/foo.js", 10, "remove breakpoint 0:0");
    assertRemoveBreakpoint("/js/foo.js", 20, "remove breakpoint 0:0,remove section 0");

    assertEquals(0, breakpointCount);
  }

  public void testAddBreakpointsForUniqueFiles() {
    assertAddBreakpoint("/a/c/foo4.js", 10, "add section 0,add breakpoint 0:0");
    assertAddBreakpoint("/a/c/foo6.js", 10, "add section 1,add breakpoint 1:0");
    assertAddBreakpoint("/a/c/foo1.js", 10, "add section 0,add breakpoint 0:0");
    assertAddBreakpoint("/a/b/zzz4.js", 10, "add section 0,add breakpoint 0:0");
    assertAddBreakpoint("/a/d/aaa4.js", 10, "add section 4,add breakpoint 4:0");

    assertEquals(5, breakpointCount);

    assertRemoveBreakpoint("/a/c/foo6.js", 10, "remove breakpoint 3:0,remove section 3");
    assertRemoveBreakpoint("/a/b/zzz4.js", 10, "remove breakpoint 0:0,remove section 0");
    assertRemoveBreakpoint("/a/d/aaa4.js", 10, "remove breakpoint 2:0,remove section 2");
    assertRemoveBreakpoint("/a/c/foo4.js", 10, "remove breakpoint 1:0,remove section 1");
    assertRemoveBreakpoint("/a/c/foo1.js", 10, "remove breakpoint 0:0,remove section 0");

    assertEquals(0, breakpointCount);
  }

  public void testAddBreakpointsForMixedFiles() {
    assertAddBreakpoint("/js/foo.js", 10, "add section 0,add breakpoint 0:0");
    assertAddBreakpoint("/js/bar.js", 10, "add section 0,add breakpoint 0:0");
    assertAddBreakpoint("/js/foo.js", 20, "add breakpoint 1:1");
    assertAddBreakpoint("/js/bar.js", 20, "add breakpoint 0:1");
    assertAddBreakpoint("/js/foo.js", 30, "add breakpoint 1:2");
    assertAddBreakpoint("/js/bar.js", 30, "add breakpoint 0:2");

    assertEquals(6, breakpointCount);

    assertRemoveBreakpoint("/js/bar.js", 20, "remove breakpoint 0:1");
    assertRemoveBreakpoint("/js/foo.js", 10, "remove breakpoint 1:0");
    assertRemoveBreakpoint("/js/bar.js", 10, "remove breakpoint 0:0");
    assertRemoveBreakpoint("/js/bar.js", 30, "remove breakpoint 0:0,remove section 0");
    assertRemoveBreakpoint("/js/foo.js", 20, "remove breakpoint 0:0");
    assertRemoveBreakpoint("/js/foo.js", 30, "remove breakpoint 0:0,remove section 0");

    assertEquals(0, breakpointCount);
  }

  public void testAddBreakpointsBetweenSections() {
    assertAddBreakpoint("/js/bar.js", 10, "add section 0,add breakpoint 0:0");
    assertAddBreakpoint("/js/bar.js", 20, "add breakpoint 0:1");
    assertAddBreakpoint("/js/bar.js", 30, "add breakpoint 0:2");
    assertAddBreakpoint("/js/foo.js", 10, "add section 1,add breakpoint 1:0");
    assertAddBreakpoint("/js/foo.js", 20, "add breakpoint 1:1");
    assertAddBreakpoint("/js/foo.js", 30, "add breakpoint 1:2");

    assertAddBreakpoint("/js/baz.js", 10, "add section 1,add breakpoint 1:0");
    assertRemoveBreakpoint("/js/baz.js", 10, "remove breakpoint 1:0,remove section 1");

    assertAddBreakpoint("/js/baa.js", 10, "add section 0,add breakpoint 0:0");
    assertRemoveBreakpoint("/js/baa.js", 10, "remove breakpoint 0:0,remove section 0");

    assertAddBreakpoint("/js/zoo.js", 10, "add section 2,add breakpoint 2:0");
    assertRemoveBreakpoint("/js/zoo.js", 10, "remove breakpoint 2:0,remove section 2");

    assertRemoveBreakpoint("/js/foo.js", 30, "remove breakpoint 1:2");
    assertRemoveBreakpoint("/js/foo.js", 20, "remove breakpoint 1:1");
    assertRemoveBreakpoint("/js/foo.js", 10, "remove breakpoint 1:0,remove section 1");
    assertRemoveBreakpoint("/js/bar.js", 30, "remove breakpoint 0:2");
    assertRemoveBreakpoint("/js/bar.js", 20, "remove breakpoint 0:1");
    assertRemoveBreakpoint("/js/bar.js", 10, "remove breakpoint 0:0,remove section 0");

    assertEquals(0, breakpointCount);
  }

  public void testAddBreakpointsInSubfolders() {
    assertAddBreakpoint("/bar.js", 10, "add section 0,add breakpoint 0:0");
    assertAddBreakpoint("/test.html", 10, "add section 1,add breakpoint 1:0");
    assertAddBreakpoint("/test.js", 10, "add section 2,add breakpoint 2:0");
    assertAddBreakpoint("/folder/foo.js", 10, "add section 3,add breakpoint 3:0");
    assertAddBreakpoint("/folder/sub/zoo.js", 10, "add section 4,add breakpoint 4:0");

    assertRemoveBreakpoint("/bar.js", 10, "remove breakpoint 0:0,remove section 0");
    assertRemoveBreakpoint("/test.html", 10, "remove breakpoint 0:0,remove section 0");
    assertRemoveBreakpoint("/test.js", 10, "remove breakpoint 0:0,remove section 0");
    assertRemoveBreakpoint("/folder/foo.js", 10, "remove breakpoint 0:0,remove section 0");
    assertRemoveBreakpoint("/folder/sub/zoo.js", 10, "remove breakpoint 0:0,remove section 0");

    assertEquals(0, breakpointCount);
  }

  private void assertAddBreakpoint(String path, int lineNumber, String message) {
    Breakpoint breakpoint = new Breakpoint.Builder(new PathUtil(path), lineNumber).build();
    debuggingSidebarBreakpointsPane.addBreakpoint(breakpoint);
    assertEquals(path + ":" + lineNumber, message, viewMessages.join(","));
    assertEquals(path + ":" + lineNumber, breakpointCount,
        debuggingSidebarBreakpointsPane.getBreakpointCount());
    viewMessages.clear();
  }

  private void assertRemoveBreakpoint(String path, int lineNumber, String message) {
    Breakpoint breakpoint = new Breakpoint.Builder(new PathUtil(path), lineNumber).build();
    debuggingSidebarBreakpointsPane.removeBreakpoint(breakpoint);
    assertEquals(path + ":" + lineNumber, message, viewMessages.join(","));
    assertEquals(path + ":" + lineNumber, breakpointCount,
        debuggingSidebarBreakpointsPane.getBreakpointCount());
    viewMessages.clear();
  }
}
