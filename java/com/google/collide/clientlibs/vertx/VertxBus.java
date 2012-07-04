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

package com.google.collide.clientlibs.vertx;

import com.google.gwt.core.client.JavaScriptObject;



/**
 * An interface exposing the operations which can be performed over the vertx event bus.
 */
public interface VertxBus {

  /**
   * Handler for receiving replies to messages you sent on the event bus.
   */
  public interface ReplyHandler {
    void onReply(String message);
  }

  /**
   * Client for sending a reply in response to a message you received on the event bus.
   */
  public static class ReplySender extends JavaScriptObject {
    protected ReplySender() {
    }

    public final native void sendReply(String message) /*-{
      this(message);
    }-*/;
  }

  /**
   * Handler messages sent to you on the event bus.
   */
  public interface MessageHandler {
    void onMessage(String message, ReplySender replySender);
  }

  public interface ConnectionListener {
    void onOpen();

    void onClose();
  }

  public static final short CONNECTING = 0;
  public static final short OPEN = 1;
  public static final short CLOSING = 2;
  public static final short CLOSED = 3;

  /**
   * Sets a callback which is called when the eventbus is open. The eventbus is opened automatically
   * upon instantiation so this should be the first thing that is set after instantiation.
   */
  public void setOnOpenCallback(ConnectionListener callback);

  /** Sets a callback which is called when the eventbus is closed */
  public void setOnCloseCallback(ConnectionListener callback);

  /**
   * Sends a message to an address, providing an replyHandler.
   */
  public void send(String address, String message, ReplyHandler replyHandler);

  /**
   * Sends a message to an address.
   */
  public void send(String address, String message);

  /** Closes the event bus */
  public void close();

  /**
   * @return the ready state of the event bus
   */
  public short getReadyState();

  /**
   * Registers a new handler which will listener for messages sent to the specified address.
   */
  public void register(String address, MessageHandler handler);

  /**
   * Unregistered a previously registered handler listening on the specified address.
   */
  public void unregister(String address, MessageHandler handler);
}
