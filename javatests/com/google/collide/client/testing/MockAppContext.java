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

import com.google.collide.client.AppContext;
import com.google.collide.client.Resources;
import com.google.collide.client.editor.EditorContext;

/**
 * A variant of {@link AppContext} which provides mock stubs for server communication, to enable
 * standalone unit testing.
 */
public class MockAppContext extends AppContext implements EditorContext<Resources> {
  
  private MockFrontendApi mockFrontendApi = new MockFrontendApi();

  public MockAppContext() {
  }

  /**
   * Checks that all the mock APIs are drained. Throws an exception if not.
   */
  public void assertIsDrained() {
    mockFrontendApi.assertIsDrained();
  }

  /**
   * Returns the frontend API, cast as a mock
   */
  @Override
  public MockFrontendApi getFrontendApi() {
    return mockFrontendApi;
  }
}
