package com.google.collide.server.plugin.gwt;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.junit.Test;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.deploy.Container;
import org.vertx.java.deploy.impl.VerticleManager;
import org.vertx.java.deploy.impl.rhino.RhinoVerticleFactory;

import com.google.collide.server.plugin.PluginManager;
import com.google.collide.server.shared.util.Dto;

public class GwtPluginTest extends TestCase {

  private VertxInternal vertx;
  private BusModBase pluginManager;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    pluginManager = new PluginManager();
    pluginManager.setVertx(vertx);
    vertx = new DefaultVertx(18080, "0.0.0.0");
    VerticleManager verticleManager = new VerticleManager(vertx);
    Map<String, Object> map = new HashMap<String, Object>();
    
    //for now, let's just setup default collide
    map.put("plugins", new JsonArray().addString("gwt"));
    
    JsonObject jsonConfig = new JsonObject(map );
    URL[] urls = new URL[]{
        //classpath?
    };
    verticleManager.deploy(true, "Plugin Test", 
        "com.google.collide.server.plugin.gwt.GwtPluginTest", jsonConfig,
        urls, 1, new File("."), new Handler<Void>() {
      @Override
      public void handle(Void arg0) {
        
      }
        });
    Container container = new Container(verticleManager );
    
    pluginManager.setContainer(container);
  }

  @Test(timeout = 300000)
  public void testCompiler() {
    GwtPlugin plugin = new GwtPlugin();
    plugin.initialize(vertx);
    vertx.eventBus().registerHandler("gwt.log", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> arg0) {
        System.out.println("gwtlog: " + arg0.body);
      }
    });
    final JsonObject req =
        Dto.wrap("{"
            + "module : 'com.google.collide.client.CollideSuperDev'"
            + ",src : ['/shared/collide/deps/wave-r1342740/','/shared/collide/java','/shared/collide/bin/gen']"
            + "}");
    for (Map.Entry<String, Handler<Message<JsonObject>>> handles : plugin.getHandlers().entrySet()) {
      vertx.eventBus().registerHandler("gwt." + handles.getKey(), handles.getValue());
    }
    
    final AtomicBoolean bool = new AtomicBoolean(true);
    vertx.eventBus().send("gwt.compile", req, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> arg0) {
        System.out.println("received " + arg0.body);
        bool.set(false);
      }
    });

    // now we must block for a while to keep the thread alive for compile
    int i = 500;// we're going to block for 100 seconds
    while (i-- > 0 && bool.get())
      // or until the compile finishes
      try {
        Thread.sleep(200);
      } catch (Exception e) {
        return;
      }
  }
}
