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

import junit.framework.TestCase;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Equivalence;

/**
 */
public class DefaultUrlSerializerTests extends TestCase {

  /**
   * A class which simplifies building/asserting a url.
   */
  private class UrlBuilder {
    private final StringBuilder url = new StringBuilder();

    public UrlBuilder place(String placeName) {
      url.append(UrlSerializer.PATH_SEPARATOR);
      url.append(urlEncoder.encode(placeName));
      return this;
    }

    public UrlBuilder value(String key, String value) {
      if (value == null) {
        return this;
      }

      String encodedKey = urlEncoder.encode(key);
      String encodedValue = urlEncoder.encode(value);

      url.append(UrlSerializer.PATH_SEPARATOR);
      url.append(encodedKey).append(DefaultUrlSerializer.KEY_VALUE_SEPARATOR).append(encodedValue);
      return this;
    }

    public UrlBuilder token(NavigationToken token) {
      place(token.getPlaceName());
      token.getBookmarkableState().iterate(new IterationCallback<String>() {
        @Override
        public void onIteration(String key, String value) {
          value(key, value);
        }
      });
      return this;
    }

    public UrlBuilder tokens(JsonArray<NavigationToken> navTokens) {
      for (int i = 0; i < navTokens.size(); i++) {
        token(navTokens.get(i));
      }
      return this;
    }

    public void assertMatch(String resultUrl) {
      TestCase.assertEquals("Url did not match", url.toString(), resultUrl);
    }
  }

  private static final String HOME_PLACE = "HOME";
  private static final String LEAF_PLACE = "LEAF";
  private static final String DETAIL_PLACE = "DETAIL";
  private static final String EMPTY_PLACE = "EMPTY";

  /** Identifies the home place, has no properties */
  private static final NavigationToken HOME_TOKEN = new NavigationTokenImpl(HOME_PLACE);
  /** Identifies a leaf place, has only a single property */
  private static final NavigationToken LEAF_TOKEN = new NavigationTokenImpl(LEAF_PLACE);
  /** Identifies a detail place, has three properties */
  private static final NavigationToken DETAIL_TOKEN = new NavigationTokenImpl(DETAIL_PLACE);
  /** Identifies the EMPTY place, has some weird properties null/empty string */
  private static final NavigationToken EMPTY_TOKEN = new NavigationTokenImpl(EMPTY_PLACE);


  private static final UrlComponentEncoder urlEncoder = new StubUrlEncoder();

  private static final Equivalence<NavigationToken> NAVIGATION_EQUIVALENCE =
      new Equivalence<NavigationToken>() {
        @Override
        public boolean doEquivalent(NavigationToken a, NavigationToken b) {
          return a.getPlaceName().equals(b.getPlaceName()) && JsonCollections.equals(
              a.getBookmarkableState(), b.getBookmarkableState());
        }

        @Override
        public int doHash(NavigationToken t) {
          return t.getPlaceName().hashCode() ^ t.getBookmarkableState().hashCode();
        }
      };

  static {
    LEAF_TOKEN.getBookmarkableState().put("one", "one");

    DETAIL_TOKEN.getBookmarkableState().put("one", "one");
    DETAIL_TOKEN.getBookmarkableState().put("two", "two");
    DETAIL_TOKEN.getBookmarkableState().put("three", "three");

    EMPTY_TOKEN.getBookmarkableState().put("one", null);
    EMPTY_TOKEN.getBookmarkableState().put("two", "");
    // This really tests the encoder more than anything...
    EMPTY_TOKEN.getBookmarkableState().put("three", DefaultUrlSerializer.PATH_SEPARATOR);
    EMPTY_TOKEN.getBookmarkableState()
        .put("four", String.valueOf(DefaultUrlSerializer.KEY_VALUE_SEPARATOR));
  }

  private UrlSerializer serializer;

  @Override
  public void setUp() {
    serializer = new DefaultUrlSerializer(urlEncoder);
  }

  public void testHome() {
    assertTokens(JsonCollections.createArray(HOME_TOKEN));
    assertTokens(JsonCollections.createArray(HOME_TOKEN, HOME_TOKEN, HOME_TOKEN));
  }

  public void testLeaf() {
    assertTokens(JsonCollections.createArray(LEAF_TOKEN));
    assertTokens(JsonCollections.createArray(HOME_TOKEN, LEAF_TOKEN, LEAF_TOKEN));
    assertTokens(JsonCollections.createArray(HOME_TOKEN, LEAF_TOKEN, HOME_TOKEN));
  }

  public void testDetail() {
    assertTokens(JsonCollections.createArray(DETAIL_TOKEN));
    assertTokens(JsonCollections.createArray(HOME_TOKEN, LEAF_TOKEN, DETAIL_TOKEN));
    assertTokens(JsonCollections.createArray(DETAIL_TOKEN, HOME_TOKEN, LEAF_TOKEN, DETAIL_TOKEN));
  }

  public void testWeird() {
    NavigationToken modifiedEmptyToken = new NavigationTokenImpl(EMPTY_PLACE);
    modifiedEmptyToken.getBookmarkableState().putAll(EMPTY_TOKEN.getBookmarkableState());
    modifiedEmptyToken.getBookmarkableState().remove("one");

    // We have to be explit here since we are ignoring null
    JsonArray<NavigationToken> tokens = JsonCollections.createArray(EMPTY_TOKEN);
    String url = serializer.serialize(tokens);
    new UrlBuilder().token(modifiedEmptyToken).assertMatch(url);
    assertEquals(JsonCollections.createArray(modifiedEmptyToken), serializer.deserialize(url));

    tokens = JsonCollections.createArray(DETAIL_TOKEN, EMPTY_TOKEN, LEAF_TOKEN);
    url = serializer.serialize(tokens);
    new UrlBuilder().token(DETAIL_TOKEN)
        .token(modifiedEmptyToken).token(LEAF_TOKEN).assertMatch(url);
    assertEquals(JsonCollections.createArray(DETAIL_TOKEN, modifiedEmptyToken, LEAF_TOKEN),
        serializer.deserialize(url));
  }

  private void assertTokens(JsonArray<NavigationToken> tokens) {
    String url = serializer.serialize(tokens);
    new UrlBuilder().tokens(tokens).assertMatch(url);
    assertEquals(tokens, serializer.deserialize(url));
  }

  private void assertEquals(
      JsonArray<NavigationToken> expected, JsonArray<NavigationToken> actual) {
    boolean isEqual = JsonCollections.equals(expected, actual, NAVIGATION_EQUIVALENCE);
    if (!isEqual) {
      failNotEquals("Deserialized URL was not the same", expected, actual);
    }
  }
}
