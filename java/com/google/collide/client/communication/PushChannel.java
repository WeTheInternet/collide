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

import java.util.ArrayList;
import java.util.List;

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.status.StatusManager;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.util.logging.Log;
import com.google.collide.clientlibs.vertx.VertxBus;
import com.google.collide.clientlibs.vertx.VertxBus.MessageHandler;
import com.google.collide.clientlibs.vertx.VertxBus.ReplyHandler;
import com.google.collide.clientlibs.vertx.VertxBus.ReplySender;
import com.google.collide.clientlibs.vertx.VertxBusImpl;
import com.google.collide.dtogen.client.RoutableDtoClientImpl;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.client.Jso;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.gwt.user.client.Timer;

import elemental.js.util.JsArrayOfString;
import elemental.js.util.JsMapFromStringTo;

/**
 * A PushChannel abstraction on top of the {@link VertxBus}.
 * 
 */
public class PushChannel {

  public interface Listener {
    void onReconnectedSuccessfully();
  }

  public static PushChannel create(MessageFilter messageFilter, StatusManager statusManager) {
    // If we do not have a valid client ID... bail.
    if (BootstrapSession.getBootstrapSession().getActiveClientId() == null) {
      StatusMessage fatal =
          new StatusMessage(statusManager, MessageType.FATAL, "You are not logged in!");
      fatal.addAction(StatusMessage.RELOAD_ACTION);
      fatal.setDismissable(false);
      fatal.fire();
      return null;
    }

    VertxBus eventBus = VertxBusImpl.create();
    PushChannel pushChannel = new PushChannel(eventBus, messageFilter, statusManager);
    pushChannel.init();
    return pushChannel;
  }

  private class DisconnectedTooLongTimer extends Timer {
    private static final int DELAY_MS = 60 * 1000;

    @Override
    public void run() {
      // reconnection effort failed.
      StatusMessage fatal = new StatusMessage(
          statusManager, MessageType.FATAL, "Lost communication with the server.");
      fatal.addAction(StatusMessage.RELOAD_ACTION);
      fatal.setDismissable(false);
      fatal.fire();
    }

    void schedule() {
      schedule(DELAY_MS);
    }
  }

  private class QueuedMessage {
    final String address;
    final String msg;
    final ReplyHandler replyHandler;

    QueuedMessage(String address, String msg, ReplyHandler replyHandler) {
      this.address = address;
      this.msg = msg;
      this.replyHandler = replyHandler;
    }
  }

  private final ListenerManager<PushChannel.Listener> listenerManager = ListenerManager.create();

  private final DisconnectedTooLongTimer disconnectedTooLongTimer = new DisconnectedTooLongTimer();

  private final VertxBus.ConnectionListener connectionListener = new VertxBus.ConnectionListener() {
    private boolean hasReceivedOnDisconnected;
    private VertxBus.MessageHandler messageHandler = null;

    @Override
    public void onOpen() {
      // Lazily initialize the messageHandler and register to handle messages.
      if (messageHandler == null) {
        messageHandler = new VertxBus.MessageHandler() {
          @Override
          public void onMessage(String message, ReplySender replySender) {
            ServerToClientDto dto =
                (ServerToClientDto) Jso.deserialize(message).<RoutableDtoClientImpl>cast();
            messageFilter.dispatchMessage(dto);
          }
        };
        eventBus.register(
            "client." + BootstrapSession.getBootstrapSession().getActiveClientId(), messageHandler);
        JsArrayOfString keys = queuedReceivers.keys();
        for (int i = keys.length();i-->0;){
          String key = keys.get(i);
          MessageHandler receiver = queuedReceivers.get(key);
          if (null!=receiver)
            eventBus.register(key, receiver);
        }
      }

      // Notify listeners who handle reconnections.
      if (hasReceivedOnDisconnected) {
        disconnectedTooLongTimer.cancel();

        listenerManager.dispatch(new ListenerManager.Dispatcher<PushChannel.Listener>() {
            @Override
          public void dispatch(PushChannel.Listener listener) {
            listener.onReconnectedSuccessfully();
          }
        });
        hasReceivedOnDisconnected = false;
      }

      // Drain any messages that came in while the channel was not open.
      for (QueuedMessage msg : queuedMessages) {
        eventBus.send(msg.address, msg.msg, msg.replyHandler);
      }
      queuedMessages.clear();
      
    }

    @Override
    public void onClose() {
      hasReceivedOnDisconnected = true;
      disconnectedTooLongTimer.schedule();
    }
  };

  private final MessageFilter messageFilter;
  private final StatusManager statusManager;
  private final VertxBus eventBus;
  private final List<QueuedMessage> queuedMessages = new ArrayList<QueuedMessage>();
  private final JsMapFromStringTo<MessageHandler> queuedReceivers = JsMapFromStringTo.<MessageHandler>create();

  private PushChannel(VertxBus eventBus, MessageFilter messageFilter, StatusManager statusManager) {
    this.eventBus = eventBus;
    this.messageFilter = messageFilter;
    this.statusManager = statusManager;
  }

  private void init() {
    eventBus.setOnOpenCallback(connectionListener);
    eventBus.setOnCloseCallback(connectionListener);
  }

  /**
   * Listens to all messages published to a given address.
   * This is NOT secure, but can be very useful for keeping all collaborators updated.
   */
  public void receive(String address, MessageHandler listener) {
    if (eventBus.getReadyState() != VertxBus.OPEN) {
      Log.debug(PushChannel.class,
          "Tried to add a message receiver on address " +
          address+ " before vertx was initialized");
      queuedReceivers.put(address,listener);
      return;
    }
    eventBus.register(address, listener);
  }
  
  /**
   * Sends a message to an address, providing an replyHandler.
   */
  public void send(String address, String message, ReplyHandler replyHandler) {
    if (eventBus.getReadyState() != VertxBus.OPEN) {
      Log.debug(PushChannel.class,
          "Message sent to '" + address + "' while channel was disconnected: " + message);
      queuedMessages.add(new QueuedMessage(address, message, replyHandler));
      return;
    }
    eventBus.send(address, message, replyHandler);
  }

  /**
   * Sends a message to an address.
   */
  public void send(String address, String message) {
    send(address, message, null);
  }

  public ListenerRegistrar<PushChannel.Listener> getListenerRegistrar() {
    return listenerManager;
  }
}
