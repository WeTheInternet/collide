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

import com.google.collide.client.collaboration.DocOpsSavedNotifier;

/**
 * Mock class for {@link DocOpsSavedNotifier}.
 * 
 * <p>This will callback the client synchronously.
 */
public class MockDocOpsSavedNotifier extends DocOpsSavedNotifier {

  public MockDocOpsSavedNotifier() {
    super(null, null);
  }

  @Override
  public boolean notifyForWorkspace(Callback callback) {
    callback.onAllDocOpsSaved();
    return false;
  }

  @Override
  public boolean notifyForFiles(Callback callback, String... fileEditSessionKeys) {
    callback.onAllDocOpsSaved();
    return false;
  }

  @Override
  public boolean notifyForDocuments(Callback callback, int... documentIds) {
    callback.onAllDocOpsSaved();
    return false;
  }

}
