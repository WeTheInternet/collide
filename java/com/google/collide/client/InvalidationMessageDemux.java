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

package com.google.collide.client;

import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.communication.MessageFilter.MessageRecipient;
import com.google.collide.clientlibs.invalidation.InvalidationManager;
import com.google.collide.dto.InvalidationMessage;
import com.google.collide.dto.RoutingTypes;

/**
 * A demultiplexer that receives a single {@link InvalidationMessage} message type from the push
 * channel and sends it to the appropriate listener.
 *
 */
public class InvalidationMessageDemux {

  public static InvalidationMessageDemux attach(
      InvalidationManager invalidationManager, MessageFilter messageFilter) {

    InvalidationMessageDemux demux = new InvalidationMessageDemux(invalidationManager);
    messageFilter.registerMessageRecipient(RoutingTypes.INVALIDATIONMESSAGE, demux.dtoRecipient);
    return demux;
  }

  private final InvalidationManager tangoInvalidationManager;

  private final MessageRecipient<InvalidationMessage> dtoRecipient =
      new MessageRecipient<InvalidationMessage>() {
        @Override
        public void onMessageReceived(InvalidationMessage invalidation) {
          tangoInvalidationManager.handleInvalidation(invalidation.getObjectName(),
              Long.parseLong(invalidation.getVersion()), invalidation.getPayload());
        }
      };

  private InvalidationMessageDemux(InvalidationManager tangoInvalidationManager) {
    this.tangoInvalidationManager = tangoInvalidationManager;
  }
}
