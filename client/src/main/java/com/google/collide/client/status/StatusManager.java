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

package com.google.collide.client.status;

import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.client.JsoArray;


/**
 * The Status Manager is responsible for maintaining all status state. It
 * coordinates priorities between different types of messages and allows the
 * status view to be stateless.
 * 
 * There can be many outstanding messages at any given time, but only one
 * message is active. When a message becomes active, the {@link StatusHandler}
 * is notified of this change.
 * 
 * A fatal message is unrecoverable and cannot be dismissed or canceled once
 * fired.
 */
public class StatusManager {
  /**
   * The currently active message, or null if there are none
   */
  private StatusMessage activeMessage = null;

  // TODO: Too bad we don't have real lightweight collections. Might be
  // worth making an Ordered Set implementation for wider client use, but for
  // now these collections are always very small.

  /**
   * The set of outstanding confirmation messages.
   */
  private final JsoArray<StatusMessage> confirmationMessages = JsoArray.create();

  /**
   * The set of outstanding error messages.
   */
  private final JsoArray<StatusMessage> errorMessages = JsoArray.create();

  /**
   * If a fatal message fires, it is recorded here.
   */
  private StatusMessage firedFatal = null;

  /**
   * The set of outstanding loading messages.
   */
  private final JsoArray<StatusMessage> loadingMessages = JsoArray.create();

  /**
   * The handler responsible for updating the view when there is a new active
   * message.
   */
  private StatusHandler statusHandler;

  public StatusManager() {
    /*
     * Create a dummy statusHandler to safely handle events that occur before
     * the UI handler is hooked up.
     */
    this.statusHandler = new StatusHandler() {
      @Override
      public void clear() {
      }

      @Override
      public void onStatusMessage(StatusMessage msg) {
      }
    };
  }

  StatusManager(StatusHandler statusHandler) {
    this.statusHandler = statusHandler;
  }

  /**
   * Cancel a message. Once a message is canceled, all subsequent fires are
   * no-ops.
   * 
   * @param msg the message to cancel
   */
  void cancel(StatusMessage msg) {
    switch (msg.getType()) {
      case LOADING:
        loadingMessages.remove(msg);
        break;
      case CONFIRMATION:
        confirmationMessages.remove(msg);
        break;
      case ERROR:
        errorMessages.remove(msg);
        break;
      case FATAL:
        // cannot cancel a fatal;
        return;
      default:
        Log.error(getClass(), "Got a status message of unknown type " + msg.getType());
        return;
    }
    possiblyFireHandler();
  }

  /**
   * Clear all outstanding and active messages.
   */
  public void clear() {
    activeMessage = null;
    loadingMessages.clear();
    errorMessages.clear();
    statusHandler.clear();
  }

  /**
   * A message firing to the StatusManager puts it on the queue of pending
   * messages. If this message is the active message, then it fires to the
   * handler as well.
   * 
   * @param msg
   */
  void fire(StatusMessage msg) {
    if ((msg.getType() == MessageType.FATAL) && (firedFatal == null)) {
      statusHandler.onStatusMessage(msg);
      firedFatal = msg;
    } else {
      possiblyAddMessage(msg);
      possiblyFireHandler();
    }
  }

  /**
   * Convenience method for giving the message lists ordered-set like semantics
   * where an update will not add another element but will re-fire the handler
   * if the given message is the active message.
   * 
   * @param msg
   */
  private void possiblyAddMessage(StatusMessage msg) {
    JsoArray<StatusMessage> list = null;
    switch (msg.getType()) {
      case LOADING:
        list = loadingMessages;
        break;
      case CONFIRMATION:
        list = confirmationMessages;
        break;
      case ERROR:
        list = errorMessages;
        break;
      case FATAL:
        return;
      default:
        Log.error(getClass(), "Got a status message of unknown type " + msg.getType());
        return;
    }

    boolean found = false;
    for (int i = 0; !found && i < list.size(); i++) {
      if (list.get(i) == msg) {
        found = true;
      }
    }
    if (found && (msg == activeMessage)) {
      // Trigger a re-fire to the handler if the active message fires again.
      activeMessage = null;
    } else if (!found) {
      list.add(msg);
    }
  }

  /**
   * If the top message has changed, then notify the handler.
   */
  private void possiblyFireHandler() {
    if (firedFatal != null) {
      // Drop all other messages ones a fatal message has fired.
      return;
    }

    // Normal message handling.
    StatusMessage top = null;
    if (!errorMessages.isEmpty()) {
      top = errorMessages.peek();
    } else if (!confirmationMessages.isEmpty()) {
      top = confirmationMessages.peek();
    } else if (!loadingMessages.isEmpty()) {
      top = loadingMessages.peek();
    }

    if (top == null) {
      statusHandler.clear();
    } else if (top != activeMessage) {
      statusHandler.onStatusMessage(top);
    }

    activeMessage = top;
  }

  /**
   * Set a new status handler. This will clear the view of the old handler and
   * immediately fire the top status message to the new handler.
   * 
   * @param statusHandler the new status handler.
   */
  public void setHandler(StatusHandler statusHandler) {
    this.statusHandler.clear();
    this.statusHandler = statusHandler;

    if (firedFatal != null) {
      // On a handoff, persist the fatal message
      statusHandler.onStatusMessage(firedFatal);
    } else if (activeMessage != null) {
      activeMessage = null;
      possiblyFireHandler();
    }
  }
}
