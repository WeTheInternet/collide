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

package com.google.collide.clientlibs.navigation;

import com.google.collide.json.shared.JsonStringMap;

/**
 * A token used to identify a unique segment of history.
 */
public interface NavigationToken {
  /**
   * @return the unique name identifying the place of this token.
   */
  public String getPlaceName();

  /**
   * @return The serialized data contained in this token.
   */
  public JsonStringMap<String> getBookmarkableState();
}
