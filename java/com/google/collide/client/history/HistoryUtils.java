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

package com.google.collide.client.history;

import com.google.collide.clientlibs.navigation.NavigationToken;
import com.google.collide.clientlibs.navigation.UrlSerializationController;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.common.annotations.VisibleForTesting;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;

/*
 * TODO: The prefixing of paths with "/h/" is to allow us to use HTML5
 * pushState(). We currently still use the hash fragment because our testing
 * infrastructure doesn't work with FF4, or Chrome. Rage. But we are setup to
 * easily switch to using it once the testing infrastructure gets with the
 * times.
 */
/**
 * Utility class for extracting String encodings of {@link Place}s from the
 * history string, and for creating entries in History based on
 * {@link PlaceNavigationEvent}s.
 *
 *  Note that because we are using HTML5 pushState() and popState(), that we are
 * not using a hash fragment to encode the history string. We use a simple URL
 * scheme where the path of the URL is the History String.
 *
 *  In order to not collide with some of the reserved URL mappings exposed by
 * the Frontend, we prefix paths used as history with "/h/", since the FE treats
 * "/h/*" URLs like a request for the root servlet.
 *
 */
public class HistoryUtils {

  /**
   * Callback for entities that are interested when HistoryUtils API changes the
   * URL.
   */
  public interface SetHistoryListener {
    void onHistorySet(String historyString);
  }

  /**
   * This gets called back when the user navigates history via back/forward
   * button presses. This does NOT get dispatched when we set the history token
   * ourselves.
   */
  public interface ValueChangeListener {
    void onValueChanged(String historyString);
  }

  private static String lastSetHistoryString = "";
  private static final JsoArray<SetHistoryListener> setHistoryListeners = JsoArray.create();
  private static final JsoArray<ValueChangeListener> valueChangeListeners = JsoArray.create();
  private static final UrlSerializationController urlSerializationController =
      new UrlSerializationController();

  // We want to trap changes to the hash fragment.
  static {
    Browser.getWindow().addEventListener("hashchange", new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        String currentHistoryString = getHistoryString();

        // We dispatch only if the current history string is different from one
        // that we set.
        if (!lastSetHistoryString.equals(currentHistoryString)) {
          for (int i = 0, n = valueChangeListeners.size(); i < n; i++) {
            valueChangeListeners.get(i).onValueChanged(currentHistoryString);
          }
        }
        lastSetHistoryString = currentHistoryString;
      }
    }, false);
  }

  /**
   * Adds a listener that will be called whenever the URL changes. This gets
   * called each time we set a history token. It will also be called immediately
   * from this registration.
   */
  public static void addSetHistoryListener(SetHistoryListener listener) {
    setHistoryListeners.add(listener);
    listener.onHistorySet(getHistoryString());
  }

  /**
   * Adds a listener that will be called whenever the user presses back and
   * forward. This will NOT get called when we set the history string.
   */
  public static void addValueChangeListener(ValueChangeListener listener) {
    valueChangeListeners.add(listener);
  }

  /**
   * Takes in a snapshot of the active Places, and creates a History entry for
   * it.
   */
  public static void createHistoryEntry(JsoArray<? extends NavigationToken> historySnapshot) {
    String historyString = createHistoryString(historySnapshot);
    
    // This will update the URL without refreshing the browser.
    setHistoryString(historyString);

    // Now inform interested parties of the new URL.
    for (int i = 0, n = setHistoryListeners.size(); i < n; i++) {
      setHistoryListeners.get(i).onHistorySet(historyString);
    }
  }
  
  public static String createHistoryString(JsonArray<? extends NavigationToken> historySnapshot) {
    return urlSerializationController.serializeToUrl(historySnapshot);
  }

  /**
   * @return the currently set, entire History String.
   */
  public static String getHistoryString() {

    // TODO: We're currently using hash when we refactor the place
    // framework we will move to pushstate.
    String hashFragment = Browser.getWindow().getLocation().getHash();

    // Remove the hash/
    if (hashFragment.length() > 0) {
      hashFragment = hashFragment.substring(1);
    }
    return Browser.decodeURI(hashFragment);
  }

  /**
   * See: {@link #parseHistoryString(String historyString)}.
   */
  public static JsonArray<NavigationToken> parseHistoryString() {
    return parseHistoryString(getHistoryString());
  }

  /**
   * Parses the history string and returns an array of {@link HistoryPiece}s.
   * These can be used to construct an array of {@link PlaceNavigationEvent}s
   * that can be fired on the {@link RootPlace}.
   *
   * @param historyString the String corresponding to the entire History Token.
   * @return the parsed history String as an array of history pieces, or an
   *         empty {@link JsoArray} if the History String is malformed or not
   *         present.
   */
  public static JsonArray<NavigationToken> parseHistoryString(String historyString) {
    return urlSerializationController.deserializeFromUrl(historyString);
  }

  @VisibleForTesting
  static void setHistoryString(String historyString) {
    lastSetHistoryString = historyString;

    // TODO: When we move to FF4, we can use the pushState() API.
    // Until then, we are stuck with the hash fragment.
    // history.pushState(null, null, historyString);
    Browser.getWindow().getLocation().setHash(Browser.encodeURI(historyString));
  }
}
