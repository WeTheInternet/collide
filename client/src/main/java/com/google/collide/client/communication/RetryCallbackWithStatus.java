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

package com.google.collide.client.communication;

import com.google.collide.client.communication.FrontendRestApi.RetryCallback;
import com.google.collide.client.status.StatusAction;
import com.google.collide.client.status.StatusManager;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import elemental.html.SpanElement;

/**
 * A {@link RetryCallback} which also updates {@link StatusMessage}s as it goes.
 * 
 */
public class RetryCallbackWithStatus<T extends ServerToClientDto>
    extends RetryCallback<T> {

  private StatusManager manager;
  private String messageText;
  private StatusMessage pendingMessage;

  /**
   * Constructs a retry-with-status callback for a given trying-to-reload message.
   * 
   * @param message text to display when retrying
   */
  public RetryCallbackWithStatus(StatusManager statusManager, String message) {
    this(statusManager, message, null);
  }

  /**
   * Constructs a retrying callback with a given trying-to-reload message, but
   * also managing a given operation-is-pending message (probably a deferred
   * status for the first call).
   *
   * @param message text to display when retrying
   * @param pending an already-fired message that should be cancelled when
   *        either a new message is needed, or when the operation completes.
   */
  public RetryCallbackWithStatus(StatusManager statusManager, String message,
      StatusMessage pending) {
    manager = statusManager;
    messageText = message;
    pendingMessage = pending;
  }

  public void dismissMessage() {
    if (pendingMessage != null) {
      pendingMessage.cancel();
    }
  }

  @Override
  public void onFail(FailureReason reason) {
    dismissMessage();
  }
  
  @Override
  public void onMessageReceived(T message) {
    dismissMessage();
  }

  @Override
  protected void onRetry(int count, int milliseconds, final RepeatingCommand retryCmd) {
    dismissMessage();
    if (milliseconds > 2000) {
      pendingMessage = new StatusMessage(manager,
          MessageType.LOADING, messageText);
      pendingMessage.addAction(new StatusAction() {
        @Override
        public void renderAction(SpanElement actionContainer) {
          actionContainer.setTextContent("Retry now");
        }
        @Override
        public void onAction() {
          retryCmd.execute();
        }
      });
      pendingMessage.expire(milliseconds);
      pendingMessage.fire();
    }
  }
}
