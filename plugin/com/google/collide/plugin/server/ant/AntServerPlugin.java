package com.google.collide.plugin.server.ant;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import xapi.log.X_Log;

import com.google.collide.plugin.server.AbstractPluginServer;
import com.google.gwt.dev.codeserver.AbstractCompileThread;
import com.google.gwt.dev.codeserver.GwtCompilerThread;

@SuppressWarnings("rawtypes")
public class AntServerPlugin extends AbstractPluginServer<GwtCompilerThread>{

  public class AntRunner implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> event) {
      logger.info(event);
      logger.info(event.body);
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
