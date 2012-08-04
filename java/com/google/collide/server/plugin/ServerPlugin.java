package com.google.collide.server.plugin;

import java.util.Map;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public interface ServerPlugin {

  String getAddressBase();
  /**
   * @return an immutable map of message handlers to hook up to vertx
   */
  Map<String, Handler<Message<JsonObject>>>getHandlers();
  void initialize(Vertx vertx);

}
