package com.google.collide.server.plugin.gwt;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.junit.Test;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.deploy.Container;
import org.vertx.java.deploy.impl.VerticleManager;
import org.vertx.java.deploy.impl.VertxLocator;

import com.google.collide.plugin.server.PluginManager;
import com.google.collide.plugin.server.gwt.GwtServerPlugin;
import com.google.collide.server.shared.util.Dto;
import collide.shared.manifest.CollideManifest;

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
    map.put("includes", new JsonArray().addString("gwt"));
    map.put("preserve-cwd", true);
    map.put("webRoot", new File(".").getCanonicalPath());
    File location;
    location = new File (
        GwtPluginTest.class
          .getProtectionDomain().getCodeSource().getLocation()
          .toExternalForm().replace("file:", "")
    );
    while (location != null && !"classes".equals(location.getName())) {
      location = location.getParentFile();
    }
    location = new File(location, "lib");
    String libFolder = location.getCanonicalPath();
    map.put("staticFiles", libFolder);


    JsonObject jsonConfig = new JsonObject(map );

    ClassLoader cl = getClass().getClassLoader();
    ArrayList<URL> urlList = new ArrayList<>();
    while (cl != null) {
      if (cl instanceof URLClassLoader) {
        URLClassLoader urls = (URLClassLoader)cl;
        for (URL url : urls.getURLs()){
          urlList.add(url);
        }
      }
    }

    libFolder = "file:"+libFolder;
    System.out.println(urlList);
    URL[] urls = urlList.toArray(new URL[urlList.size()]);
//    verticleManager.deployVerticle(true,
//        "com.google.collide.plugin.server.gwt.GwtPluginTest", jsonConfig,
//        urls, 1, new File("."),"*", new Handler<String>() {
//      @Override
//      public void handle(String deployId) {
//
//      }
//        });
//    Container container = new Container(verticleManager );
//
//    pluginManager.setContainer(container);
  }

  @Test(timeout = 300000)
  public void testCompiler() {
    GwtServerPlugin plugin = new GwtServerPlugin();
    plugin.initialize(vertx);
    vertx.eventBus().registerHandler("gwt.log", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> arg0) {
        System.out.println("gwtlog: " + arg0.body);
      }
    });
    final JsonObject req =
        Dto.wrap("{"
            + "module : 'collide.demo.Foreign'"
            + ",src : ['demo/src/main/java','demo/src/main/resources','xapi-gwt.jar']"
            + ",deps : ['elemental.jar', 'gwt-dev.jar', 'gwt-user.jar']" +
            "}");
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
