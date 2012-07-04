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

package com.google.collide.client.testutil;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test case that disables scheduling during test execution.
 *
 * <p>Actually this test case make test execution more safe, by isolating
 * disallowing distinct cases to put deferred tasks to one queue.
 *
 * <p>Use this test case for all tests that do not rely on or test deferred or
 * scheduled execution (the most common case).
 *
 * <p>After case execution scheduling functionality is restored to make other
 * test cases and JUnit framework work properly.
 *
 * <p>If you extend this class and override {@link #gwtSetUp()} and / or
 * {@link #gwtTearDown()} - make sure you invoke super methods (before /
 * after other statements correspondingly).
 *
 */
public abstract class SynchronousTestCase extends GWTTestCase {

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    TestSchedulerImpl.setNoOp(true);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    super.gwtTearDown();
    TestSchedulerImpl.setNoOp(false);
  }
}
