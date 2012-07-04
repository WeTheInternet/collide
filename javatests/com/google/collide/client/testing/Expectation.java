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

package com.google.collide.client.testing;

import junit.framework.AssertionFailedError;

/**
 * An expectation mechanism for any object type. See {@link FrontendExpectation} for
 * DTO-specific utility.
 *
 *
 * @param <REQ>
 * @param <RESP>
 */
public class Expectation<REQ, RESP> {

  public static class ExpectationViolation extends AssertionFailedError {
      public ExpectationViolation(String diff) {
        super("difference detected: " + diff);
      }
    }

  public static class LeftoverExpectations extends AssertionFailedError {
      public LeftoverExpectations(int remaining, Expectation<?, ?> first) {
        super("there are " + remaining + " unmet expectations, starting with one for "
            + first.request.toString());
      }
    }

  protected REQ request;

  public Expectation(REQ request) {
    this.request = request;
  }
}
