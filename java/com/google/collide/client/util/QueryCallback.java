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

package com.google.collide.client.util;

import com.google.collide.dto.ServerError.FailureReason;

/**
 * Generic callback for any requests that return a single value.
 */
public interface QueryCallback<E> {

  /**
   * Failure callback.
   *
   * @param reason the reason for the failure, should not be null
   */
  void onFail(FailureReason reason);

  /**
   * Callback for queries to the ProjectModel.
   */
  void onQuerySuccess(E result);
}
