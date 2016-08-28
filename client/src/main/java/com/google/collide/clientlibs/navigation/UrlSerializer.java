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

import com.google.collide.json.shared.JsonArray;

/**
 * An object which can serialize a list of HistoryToken's to a url.
 */
public interface UrlSerializer {
  
  /** The separator to be used in separating url paths */
  public static final String PATH_SEPARATOR = "/";

  /**
   * Deserializes a url into a list of history tokens.
   *
   * @param url the url is guaranteed to start with a {@link #PATH_SEPARATOR}
   */
  public JsonArray<NavigationToken> deserialize(String url);

  /**
   * @return a serialized url or null if unable to serialize.
   */
  public String serialize(JsonArray<? extends NavigationToken> tokens);
}
