package collide.plugin.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public interface ServerPlugin {

  String getAddressBase();
  /**
   * @return an immutable map of message handlers to hook up to vertx
   */
  Map<String, Handler<Message<JsonObject>>>getHandlers();
  void initialize(Vertx vertx);

}
