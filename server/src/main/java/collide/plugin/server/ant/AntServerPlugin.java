package collide.plugin.server.ant;

import collide.plugin.server.AbstractPluginServer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import xapi.log.X_Log;

import com.google.gwt.dev.codeserver.GwtCompilerThread;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class AntServerPlugin extends AbstractPluginServer<GwtCompilerThread>{

  public class AntRunner implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> event) {
      logger.info(event);
      logger.info(event.body());
      X_Log.info(event);
    }
  }

  @Override
  protected Class<GwtCompilerThread> compilerClass() {
    return GwtCompilerThread.class;
  }

  @Override
  public String getAddressBase() {
    return "ant";
  }

  @Override
  public void initialize(Vertx vertx) {
  }

  @Override
  public Map<String,Handler<Message<JsonObject>>> getHandlers() {
    Map<String,Handler<Message<JsonObject>>> map = new HashMap<String,Handler<Message<JsonObject>>>();

    //fill map from build file w/ targets......
    map.put("ant", new AntRunner());

    return map;
  }

}
