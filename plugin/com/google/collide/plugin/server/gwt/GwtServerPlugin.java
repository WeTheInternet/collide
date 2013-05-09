package com.google.collide.plugin.server.gwt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpStatus;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import xapi.inject.impl.LazyPojo;

import com.google.collide.plugin.server.AbstractPluginServer;
import com.google.collide.server.fe.Cookie;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.dev.codeserver.GwtCompilerThread;

public class GwtServerPlugin extends AbstractPluginServer<GwtCompilerThread>{

  public GwtServerPlugin() {
  }

  @Override
  protected Class<GwtCompilerThread> compilerClass() {
    return GwtCompilerThread.class;
  }
  
  @Override
  public String getAddressBase() {
    return "gwt";
  }

  private final LazyPojo<Map<String, Handler<Message<JsonObject>>>> allModules =
      new LazyPojo<Map<String, Handler<Message<JsonObject>>>>() {
        @Override
        protected java.util.Map<String, Handler<Message<JsonObject>>> initialValue() {
          Map<String, Handler<Message<JsonObject>>> map =
              new HashMap<String, Handler<Message<JsonObject>>>();

          map.put("compile", new GwtCompileHandler());
          map.put("settings", new GwtSettingsHandler());
          map.put("proxy", new GwtProxyHandle());
          map.put("kill", new GwtKillHandle());

          return map;
        };
      };

  @Override
  public Map<String, Handler<Message<JsonObject>>> getHandlers() {
    return ImmutableMap.copyOf(allModules.get());
  }

//  @Override
  public void handle(HttpServerRequest req) {
    Cookie cookie = Cookie.getCookie(AUTH_COOKIE_NAME, req);
    if (cookie != null) {
      // We found a session ID. Lets go ahead and serve up the host page.
      System.out.println(cookie.value);
    }

    // We did not find the session ID. Lets go ahead and serve up the login page that should take
    // care of installing a cookie and reloading the page.
    sendRedirect(req, "/static/login.html");
  }

  private void sendRedirect(HttpServerRequest req, String url) {
    req.response.putHeader("Location", url);
    sendStatusCode(req, HttpStatus.SC_MOVED_TEMPORARILY);
  }

  private void sendStatusCode(HttpServerRequest req, int statusCode) {
    req.response.statusCode = statusCode;
    req.response.end();
  }

  class GwtKillHandle implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> event) {
      
    }
  }
  class GwtProxyHandle implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> event) {
      //Search for a proxy instance that has an active recompiler
      String module = event.body.getString("module");
      if (module == null){
        event.reply(new JsonObject("{\"error\":\"" +
            "No module field in json: "+event.body.toString()+
            "\"}"));
        return;
      }
      Set<String> modules = vertx.sharedData().getSet(module);
      if (modules == null){
        event.reply(new JsonObject("{\"error\":\"" +
            "No active modules found for module: "+module+
            "\"}"));
        return;
      }
      String address = modules.iterator().next();
      if (address == null){
        vertx.sharedData().removeSet(module);
        event.reply(new JsonObject("{\"error\":\"" +
            "No active address for gwt module: "+module+
            "\"}"));
        return;
      }
      //We have the address of the backend with our recompiler.
      //Ask it for the source file we need.
  
      //TODO: use this for handling incoming compile requests;
      //The source file forwarder will be setup using the module as a known address
    }
  }


}
