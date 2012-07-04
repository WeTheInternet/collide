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

package com.google.collide.client.collaboration;

import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.communication.MessageFilter.MessageRecipient;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.ServerToClientDocOp;
import com.google.collide.dto.client.DtoClientImpls.ServerToClientDocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.ServerToClientDocOpsImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;

/**
 * Receives {@link ServerToClientDocOp} from the {@link MessageFilter} and
 * forwards them.
 */
public class IncomingDocOpDemultiplexer {

  public static IncomingDocOpDemultiplexer create(MessageFilter messageFilter) {
    IncomingDocOpDemultiplexer receiver = new IncomingDocOpDemultiplexer(messageFilter);
    messageFilter.registerMessageRecipient(RoutingTypes.SERVERTOCLIENTDOCOP,
        receiver.messageReceiver);
    messageFilter.registerMessageRecipient(RoutingTypes.SERVERTOCLIENTDOCOPS,
        receiver.bulkMessageReceiver);
    
    return receiver;
  }

  public interface Receiver {
    /*
     * TODO: if a client wants to have in-order doc ops, we should
     * really be passing each DocOpReceiver the list of BulkReceivers so it can
     * callback the same time it would normally call into the OT stack (this
     * ensures ordering and even recovered doc ops).
     */
    /**
     * Called when a doc op is received from the server. These may be
     * out-of-order and during doc op recovery, this will NOT be called.
     */
    void onDocOpReceived(ServerToClientDocOpImpl docOpDto, DocOp docOp);
  }

  private final MessageFilter.MessageRecipient<ServerToClientDocOpImpl> messageReceiver =
      new MessageRecipient<ServerToClientDocOpImpl>() {
        @Override
        public void onMessageReceived(ServerToClientDocOpImpl message) {
          handleServerToClientDocOpMsg(message);
        }
      };

  private final MessageFilter.MessageRecipient<ServerToClientDocOpsImpl> bulkMessageReceiver =
      new MessageRecipient<ServerToClientDocOpsImpl>() {
        @Override
        public void onMessageReceived(ServerToClientDocOpsImpl message) {
          for (int i = 0, n = message.getDocOps().size(); i < n; i++) {
            handleServerToClientDocOpMsg((ServerToClientDocOpImpl) message.getDocOps().get(i));
          }
        }
      };

  /** Map from file edit session key to receiver */
  private final JsonStringMap<Receiver> receivers = JsonCollections.createMap();
  private final JsonArray<Receiver> bulkReceivers = JsonCollections.createArray();
  private final MessageFilter messageFilter;

  private IncomingDocOpDemultiplexer(MessageFilter messageFilter) {
    this.messageFilter = messageFilter;
  }

  public void teardown() {
    messageFilter.removeMessageRecipient(RoutingTypes.SERVERTOCLIENTDOCOP);
  }

  /**
   * Adds a {@link Receiver} that will receive all DocOp messages.
   */
  public void addBulkReceiver(Receiver receiver) {
    bulkReceivers.add(receiver);
  }

  public void removeBulkReceiver(Receiver receiver) {
    bulkReceivers.remove(receiver);
  }

  public void setReceiver(String fileEditSessionKey, Receiver receiver) {
    receivers.put(fileEditSessionKey, receiver);
  }

  public void handleServerToClientDocOpMsg(ServerToClientDocOpImpl message) {    
    
    // Early exit if nobody is listening.
    Receiver receiver = receivers.get(message.getFileEditSessionKey());
    if (receiver == null && bulkReceivers.size() == 0) {
      return;
    }

    DocOp docOp = message.getDocOp2();

    // Send to the registered receiver for the file edit session.
    if (receiver != null) {
      receiver.onDocOpReceived(message, docOp);
    }

    // Send to bulk receivers.
    for (int i = 0; i < bulkReceivers.size(); i++) {
      bulkReceivers.get(i).onDocOpReceived(message, docOp);
    }
  }
}
