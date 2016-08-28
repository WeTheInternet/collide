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

package com.google.collide.clientlibs.network.shared;

import com.google.collide.client.status.StatusManager;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;

/**
 * Some utils for displaying common network error type status messages.
 */
public class StatusMessageUtils {

  /**
   * Creates a reloadable and dismissable error {@link StatusMessage}. {@link StatusMessage#fire()}
   * must be called before it is shown to the user.
   */
  public static StatusMessage createReloadableDismissableErrorStatus(
      StatusManager manager, String title, String longText) {
    StatusMessage error = StatusMessageUtils.createDismissableErrorStatus(manager, title, longText);
    error.addAction(StatusMessage.RELOAD_ACTION);
    return error;
  }

  /**
   * Creates a dismissable error {@link StatusMessage}. {@link StatusMessage#fire()} must be called
   * before it is shown to the user.
   */
  public static StatusMessage createDismissableErrorStatus(
      StatusManager manager, String title, String longText) {
    StatusMessage error = new StatusMessage(manager, MessageType.ERROR, title);
    error.setLongText(longText);
    error.setDismissable(true);
    return error;
  }

  private StatusMessageUtils() {
    // Static utility class
  }

}
