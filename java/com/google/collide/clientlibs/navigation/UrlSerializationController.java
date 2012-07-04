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
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;

import elemental.client.Browser;

/**
 * A controller which can serialize and deserialize pieces of history to a url.
 */
public class UrlSerializationController {

  /**
   * A {@link UrlComponentEncoder} which uses the browser's decode and encode component
   * methods.
   */
  private static final UrlComponentEncoder urlEncoder = new UrlComponentEncoder() {
    @Override
    public String decode(String text) {
      return Browser.decodeURIComponent(text);
    }

    @Override
    public String encode(String text) {
      return Browser.encodeURIComponent(text);
    }
  };

  /** The default serializer to use */
  private final UrlSerializer defaultSerializer = new DefaultUrlSerializer(urlEncoder);

  /**
   * Deserializes a URL into a list of NavigationTokens.
   *
   * @param serializedUrl should not be null and must start with a
   *        {@link UrlSerializer#PATH_SEPARATOR} to distinguish it from other
   *        hash values.
   */
  public JsonArray<NavigationToken> deserializeFromUrl(String serializedUrl) {
    // We attempted to parse an invalid url
    if (StringUtils.isNullOrEmpty(serializedUrl)
        || serializedUrl.indexOf(UrlSerializer.PATH_SEPARATOR) != 0) {
      return JsonCollections.createArray();
    }
    return defaultSerializer.deserialize(serializedUrl);
  }

  /**
   * Serializes a list of history tokens identifying a unique application state
   * to a url.
   *
   * @return a url identifying this list of history tokens. It will always start
   *         with {@link UrlSerializer#PATH_SEPARATOR}.
   */
  public String serializeToUrl(JsonArray<? extends NavigationToken> historyTokens) {
    String url = defaultSerializer.serialize(historyTokens);
    return StringUtils.ensureStartsWith(url, "/");
  }
}
