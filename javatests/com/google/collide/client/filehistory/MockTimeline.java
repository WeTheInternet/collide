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

/**
 * Mock Timeline used for testing, with a set interval for testing timeline math
 * dragging logic and stubbing out several methods related to updating the diff 
 * editor.
 * 
 *
 */
public class MockTimeline extends Timeline {
  
  final static int TIMELINE_INTERVAL = 100;
  
  public MockTimeline(FileHistory fileHistory, Timeline.View view, AppContext context) {
    super(fileHistory, view, context);
  }
  
  @Override
  public int intervalInPx() {
    return TIMELINE_INTERVAL;
  }
  
  @Override
  public void setDiffForRevisions() {
    // Do nothing
  }
}
