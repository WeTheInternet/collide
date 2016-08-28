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
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;

/**
 * A serializer which serializes all data in each {@link NavigationToken}'s map to the url. The
 * resulting url is not really that pretty.
 *
 */
class DefaultUrlSerializer implements UrlSerializer {

  static final char KEY_VALUE_SEPARATOR = '=';
  private final UrlComponentEncoder encoder;

  public DefaultUrlSerializer(UrlComponentEncoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public JsonArray<NavigationToken> deserialize(String url) {
    JsonArray<NavigationToken> tokens = JsonCollections.createArray();
    // Simple parser, we pass the first PATH_SEPARATOR then split the string and
    // look for a place name followed by any mapped contents it may have.
    JsonArray<String> components = StringUtils.split(url.substring(1), PATH_SEPARATOR);
    for (int i = 0; i < components.size();) {
      String encodedPlace = components.get(i);
      String decodedPlace = encoder.decode(encodedPlace);

      // Create a token for the decoded place
      NavigationToken token = new NavigationTokenImpl(decodedPlace);
      tokens.add(token);

      // Loop through adding map values until we encounter the next place
      for (i++; i < components.size(); i++) {
        String encodedKeyValue = components.get(i);
        int separatorIndex = encodedKeyValue.indexOf(KEY_VALUE_SEPARATOR);
        if (separatorIndex == -1) {
          // we are back to a place and should let the outer loop restart
          break;
        }
        // Grab the encoded values
        String encodedKey = encodedKeyValue.substring(0, separatorIndex);
        String encodedValue = encodedKeyValue.substring(separatorIndex + 1);

        // decode and place in the map
        String decodedKey = encoder.decode(encodedKey);
        String decodedValue = encoder.decode(encodedValue);
        token.getBookmarkableState().put(decodedKey, decodedValue);
      }
    }

    return tokens;
  }

  /**
   * This serializes a URL into a simple format consisting of /place/key=value/key=value/place/...
   *
   * @return This serializer will never return null and can always serialize a list of
   *         {@link NavigationToken}s.
   */
  @Override
  public String serialize(JsonArray<? extends NavigationToken> tokens) {
    final StringBuilder urlBuilder = new StringBuilder();

    for (int i = 0; i < tokens.size(); i++) {
      NavigationToken token = tokens.get(i);
      String encodedName = encoder.encode(token.getPlaceName());
      urlBuilder.append(PATH_SEPARATOR).append(encodedName);

      JsonStringMap<String> tokenData = token.getBookmarkableState();
      tokenData.iterate(new IterationCallback<String>() {
        @Override
        public void onIteration(String key, String value) {
          // omit explicit nulls
          if (value == null) {
            return;
          }

          String encodedKey = encoder.encode(key);
          String encodedValue = encoder.encode(value);

          urlBuilder.append(PATH_SEPARATOR)
              .append(encodedKey).append(KEY_VALUE_SEPARATOR).append(encodedValue);
        }
      });
    }

    return urlBuilder.toString();
  }
}
