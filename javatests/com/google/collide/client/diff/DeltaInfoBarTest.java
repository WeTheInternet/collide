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

package com.google.collide.client.diff;

import com.google.collide.client.diff.DeltaInfoBar.Css;
import com.google.collide.dto.client.DtoClientImpls.DiffStatsDtoImpl;
import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

import elemental.html.DivElement;

/**
 * Tests the {@link DeltaInfoBar}
 *
 */
public class DeltaInfoBarTest extends GWTTestCase {

  private static final int ADDED_LINES = 20;
  private static final int LARGE_TOTAL = 1000;
  private static final int CHANGED_LINES = 50;
  private static final int DELETED_LINES = 49;
  private static final int UNCHANGED_LINES = 1;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testAtLeastOne() {
    assertEquals(1, DeltaInfoBar.calculateBars(1, LARGE_TOTAL));
  }

  public void testBarCount() {
    DiffStatsDtoImpl diffStats = (DiffStatsDtoImpl) DiffStatsDtoImpl.create();
    diffStats
        .setAdded(ADDED_LINES)
        .setChanged(CHANGED_LINES)
        .setDeleted(DELETED_LINES)
        .setUnchanged(UNCHANGED_LINES);
    DeltaInfoBar.Resources res = (DeltaInfoBar.Resources) GWT.create(DeltaInfoBar.Resources.class);
    DeltaInfoBar deltaInfoBar = DeltaInfoBar.create(res, diffStats, false);
    DivElement barsDiv = deltaInfoBar.getView().barsDiv;

    Css css = res.deltaInfoBarCss();

    int addedBars = barsDiv.getElementsByClassName(css.added()).getLength();
    int removedBars = barsDiv.getElementsByClassName(css.deleted()).getLength();
    int modifiedBars = barsDiv.getElementsByClassName(css.modified()).getLength();
    int unmodifiedBars = barsDiv.getElementsByClassName(css.unmodified()).getLength();

    // verify the total count
    assertEquals(DeltaInfoBar.BAR_COUNT, addedBars + removedBars + modifiedBars + unmodifiedBars);

    // Because BAR_COUNT can change, and testing the exact amounts involves
    // re-implementing calculateBars here, we verify that the ordering of
    // amounts is sane.
    assertTrue(unmodifiedBars <= addedBars);
    assertTrue(addedBars <= removedBars);
    assertTrue(removedBars <= modifiedBars);
    assertEquals(DeltaInfoBar.BAR_COUNT - (addedBars + removedBars + modifiedBars), unmodifiedBars);
  }

  public void testZero() {
    assertEquals(0, DeltaInfoBar.calculateBars(0, LARGE_TOTAL));
  }
}
