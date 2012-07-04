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

import elemental.events.MouseEvent;
import elemental.html.DivElement;

/**
 * Stub AwesomeBoxSection used for testing.
 *
 */
public class StubAwesomeBoxSection implements AwesomeBox.AwesomeBoxSection {

  private boolean acceptsSelection = false;
  private boolean hasSelection = false;
  private int iterationCount = 0;

  public StubAwesomeBoxSection() {
  }

  public StubAwesomeBoxSection(boolean acceptsSelection) {
    this.acceptsSelection = acceptsSelection;
  }

  public boolean getHasSelection() {
    return hasSelection;
  }

  public int getAndResetWasIterated() {
    int count = iterationCount;
    iterationCount = 0;
    return count;
  }

  public void wasIterated() {
    iterationCount++;
  }

  @Override
  public DivElement getElement() {
    return null;
  }

  @Override
  public void onClearSelection() {
  }

  @Override
  public String onCompleteSelection() {
    return null;
  }

  @Override
  public void onHiding(AwesomeBox section) {
  }

  @Override
  public boolean onMoveSelection(boolean moveDown) {
    hasSelection = acceptsSelection; // if we can accept selection, we did.
    return acceptsSelection;
  }

  @Override
  public boolean onQueryChanged(String query) {
    return false;
  }

  @Override
  public ActionResult onSectionClicked(MouseEvent mouseEvent) {
    return null;
  }

  @Override
  public ActionResult onActionRequested() {
    return ActionResult.DO_NOTHING;
  }

  @Override
  public void onContextChanged(AwesomeBoxContext context) {
  }

  @Override
  public boolean onShowing(AwesomeBox section) {
    return false;
  }

  @Override
  public void onAddedToContext(AwesomeBoxContext context) {
  }
}
