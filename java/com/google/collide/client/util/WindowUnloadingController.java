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

import elemental.events.Event;

import elemental.html.Window.BeforeUnloadEventListener;
import elemental.js.JsBrowser;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller used to manage messages displayed when the user attempts to
 * close the browser or navigate to another page.
 * 
 */
public class WindowUnloadingController {

  /**
   * A message displayed to the user when the user tries to navigate away from
   * the page.
   * 
   */
  public static interface Message {
    /**
     * Returns the message to display to the user in an attempt to prevent the
     * user from navigating away from the application.
     * 
     * @return the message, or null if it is safe to close the app
     */
    String getMessage();
  }

  private final List<Message> messages = new ArrayList<Message>();

  public WindowUnloadingController() {
    JsBrowser.getWindow().setOnBeforeUnload(new BeforeUnloadEventListener() {
      @Override
      public String handleEvent(Event event) {
        return handleBeforeUnloadEvent();
      }
    });
  }

  public void addMessage(Message message) {
    messages.add(message);
  }

  public void removeMessage(Message message) {
    messages.remove(message);
  }

  private String handleBeforeUnloadEvent() {
    // Look for an active message in any of the ClosingMessages.
    String toRet = null;
    for (Message message : messages) {
      String m = message.getMessage();
      if (m != null) {
        if (toRet != null) {
          // Chrome does not support newlines in alert boxes, so use spaces
          // instead.
          toRet += "  ";
        }
        toRet = (toRet == null) ? m : (toRet + m);
      }
    }
    return toRet;
  }
}
