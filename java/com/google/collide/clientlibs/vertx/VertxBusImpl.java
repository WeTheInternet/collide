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
 * A GWT overlay object for the Vertx Event Bus.
 */
public class VertxBusImpl extends JavaScriptObject implements VertxBus {

  public static final native VertxBus create() /*-{
    var url = window.location.protocol + '//' + window.location.hostname + ':' + window.location.port;
    url += "/eventbus";
    return new $wnd.vertx.EventBus(url);
  }-*/;

  protected VertxBusImpl() {}

  @Override
  public final native void setOnOpenCallback(ConnectionListener callback) /*-{
    this.onopen = function() { 
      callback.@com.google.collide.clientlibs.vertx.VertxBus.ConnectionListener::onOpen()();
    }
  }-*/;

  @Override
  public final native void setOnCloseCallback(ConnectionListener callback) /*-{
    this.onclose = function() { 
      callback.@com.google.collide.clientlibs.vertx.VertxBus.ConnectionListener::onClose()();
    }
  }-*/;

  @Override
  public final void send(String address, String message) {   
    send(address, message, null);
  }
  
  @Override
  public final native void send(String address, String message, ReplyHandler replyHandler) /*-{    
    var replyHandlerWrapper;

    if(replyHandler) {
      replyHandlerWrapper = function(reply) {
        replyHandler.@com.google.collide.clientlibs.vertx.VertxBus.ReplyHandler::onReply(Ljava/lang/String;)(reply.dto)
      }
    }

    this.send(address, {dto: message}, replyHandlerWrapper);
  }-*/;

  @Override
  public final native void close() /*-{
    this.close();
  }-*/;

  @Override
  public final native short getReadyState() /*-{
    return this.readyState();
  }-*/;

  @Override
  public final native void register(String address, MessageHandler handler) /*-{
    var handlerWrapper = function(message, replier) {
      handler.@com.google.collide.clientlibs.vertx.VertxBus.MessageHandler::onMessage(Ljava/lang/String;Lcom/google/collide/clientlibs/vertx/VertxBus$ReplySender;)
      (message.dto, replier)
    }

    // Ghetto!
    handler.__unregisterRef = handlerWrapper;
    this.registerHandler(address, handlerWrapper);
  }-*/;

  @Override
  public final native void unregister(String address, MessageHandler handler) /*-{
    this.unregisterHandler(address, handler.__unregisterRef);
  }-*/;
}
