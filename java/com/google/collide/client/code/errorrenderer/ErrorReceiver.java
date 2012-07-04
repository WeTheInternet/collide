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

package com.google.collide.client.code.errorrenderer;

import com.google.collide.dto.CodeError;
import com.google.collide.json.shared.JsonArray;

/**
 * A receiver which handles notifying the editor of code errors.
 *
 */
public interface ErrorReceiver {

  /**
   * Gets notified about code errors detected in a source file.
   */
  public static interface ErrorListener {

    /**
     * Signals that the list of errors in a file has changed.
     *
     * @param errors new list of all errors in the file
     */
    void onErrorsChanged(JsonArray<CodeError> errors);
  }

  public void setActiveDocument(String fileEditSessionKey);

  public void addErrorListener(String fileEditSessionKey, ErrorListener listener);

  public void removeErrorListener(String fileEditSessionKey, ErrorListener listener);
}
