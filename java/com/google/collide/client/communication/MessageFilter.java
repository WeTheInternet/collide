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

import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonIntegerMap;
import com.google.collide.shared.util.JsonCollections;

/**
 * Class responsible for routing JsonMessages based on the message type that get
 * sent to the client from the server.
 *
 */
public class MessageFilter {
  /**
   * Interface for receiving JSON messages.
   */
  public interface MessageRecipient<T extends ServerToClientDto> {
    void onMessageReceived(T message);
  }

  private final JsonIntegerMap<MessageRecipient<? extends ServerToClientDto>> messageRecipients =
      JsonCollections.createIntegerMap();

  /**
   * Dispatches an incoming DTO message to a registered recipient.
   *
   * @param message
   */
  public <T extends ServerToClientDto> void dispatchMessage(T message) {
    @SuppressWarnings("unchecked")
    MessageRecipient<T> recipient = (MessageRecipient<T>) messageRecipients.get(message.getType());
    if (recipient != null) {
      recipient.onMessageReceived(message);
    }
  }

  /**
   * Adds a MessageRecipient for a given message type.
   *
   * @param messageType
   * @param recipient
   */
  public void registerMessageRecipient(
      int messageType, MessageRecipient<? extends ServerToClientDto> recipient) {
    messageRecipients.put(messageType, recipient);
  }

  /**
   * Removes any MessageRecipient registered for a given type.
   *
   * @param messageType
   */
  public void removeMessageRecipient(int messageType) {
    messageRecipients.erase(messageType);
  }
}
