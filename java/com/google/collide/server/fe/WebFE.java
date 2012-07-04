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

package com.google.collide.server.fe;

import com.google.collide.dto.shared.JsonFieldConstants;

import org.apache.commons.httpclient.HttpStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * A simple web server module that can serve static files bundled with the webserver, as well as
 * serve files from the directory that the webserver was launched in via simple URL path prefixes.
 * 
 *  This web server can also bridge event bus messages to/from client side JavaScript and the server
 * side event bus.
 * 
 * (Implementation based on the stock WebServer module bundled with the Vert.x distribution)
 */
public class WebFE extends BusModBase implements Handler<HttpServerRequest> {

  private static final String WEBROOT_PATH = "/res/";
  private static final String BUNDLED_STATIC_FILES_PATH = "/static/";
  private static final String AUTH_PATH = "/_auth";
  private static final String AUTH_COOKIE_NAME = "_COLLIDE_SESSIONID";

  /**
   * The directory that we will be serving our bundled web application client form. We serve content
   * from here when the URL matches {@link #WEBROOT_PATH}
   */
  private String bundledStaticFilesPrefix;

  /**
   * The directory that we are serving files from as the "web root". We serve content from here when
   * the URL matches {@link #BUNDLED_STATIC_FILES_PATH}
   */
  private String webRootPrefix;

  @Override
  public void start() {
    super.start();

    HttpServer server = vertx.createHttpServer();
    server.requestHandler(this);

    // Configure SSL.
    if (getOptionalBooleanConfig("ssl", false)) {
      server.setSSL(true)
          .setKeyStorePassword(getOptionalStringConfig("keyStorePassword", "password"))
          .setKeyStorePath(getOptionalStringConfig("keyStorePath", "server-keystore.jks"));
    }

    // Configure the event bus bridge.
    boolean bridge = getOptionalBooleanConfig("bridge", false);
    if (bridge) {
      SockJSServer sjsServer = vertx.createSockJSServer(server);
      JsonArray inboundPermitted = getOptionalArrayConfig("in_permitted", new JsonArray());
      JsonArray outboundPermitted = getOptionalArrayConfig("out_permitted", new JsonArray());

      sjsServer.bridge(
          getOptionalObjectConfig("sjs_config", new JsonObject().putString("prefix", "/eventbus")),
          inboundPermitted, outboundPermitted, getOptionalLongConfig("auth_timeout", 5 * 60 * 1000),
          getOptionalStringConfig("auth_address", "participants.authorise"));
    }

    String bundledStaticFiles = getMandatoryStringConfig("staticFiles");
    String webRoot = getMandatoryStringConfig("webRoot");

    bundledStaticFilesPrefix = bundledStaticFiles + File.separator;
    webRootPrefix = webRoot + File.separator;

    server.listen(getOptionalIntConfig("port", 8080), getOptionalStringConfig("host", "127.0.0.1"));
  }

  @Override
  public void handle(HttpServerRequest req) {
    String path = req.path;
    if (path.equals("/")) {
      authAndWriteHostPage(req);
    } else if (path.contains("..")) {
      sendStatusCode(req, 404);
    } else if (path.startsWith(WEBROOT_PATH) && (webRootPrefix != null)) {
      req.response.sendFile(webRootPrefix + path.substring(WEBROOT_PATH.length()));
    } else if (path.startsWith(BUNDLED_STATIC_FILES_PATH) && (bundledStaticFilesPrefix != null)) {
      req.response.sendFile(bundledStaticFilesPrefix + path.substring(
          BUNDLED_STATIC_FILES_PATH.length()));
    } else if (path.startsWith(AUTH_PATH)) {
      writeSessionCookie(req);
    } else {
      sendStatusCode(req, HttpStatus.SC_NOT_FOUND);
    }
  }

  private void authAndWriteHostPage(HttpServerRequest req) {
    Cookie cookie = Cookie.getCookie(AUTH_COOKIE_NAME, req);
    if (cookie != null) {
      // We found a session ID. Lets go ahead and serve up the host page.
      doAuthAndWriteHostPage(req, cookie.value);
      return;
    }

    // We did not find the session ID. Lets go ahead and serve up the login page that should take
    // care of installing a cookie and reloading the page.
    sendRedirect(req, "/static/login.html");
  }

  // TODO: If we want to make this secure, setup SSL and set this as a Secure cookie.
  // Also probably want to resurrect XsrfTokens and the whole nine yards. But for now, this is
  // purely a tracking cookie.
  /**
   * Writes cookies for the session ID that is posted to it.
   */
  private void writeSessionCookie(final HttpServerRequest req) {
    if (!"POST".equals(req.method)) {
      sendStatusCode(req, HttpStatus.SC_METHOD_NOT_ALLOWED);
      return;
    }

    // Extract the post data.
    req.bodyHandler(new Handler<Buffer>() {
        @Override
      public void handle(Buffer buff) {
        String contentType = req.headers().get("Content-Type");
        if ("application/x-www-form-urlencoded".equals(contentType)) {
          QueryStringDecoder qsd = new QueryStringDecoder(buff.toString(), false);
          Map<String, List<String>> params = qsd.getParameters();

          List<String> loginSessionIdList = params.get(JsonFieldConstants.SESSION_USER_ID);
          List<String> usernameList = params.get(JsonFieldConstants.SESSION_USERNAME);
          if (loginSessionIdList == null || loginSessionIdList.size() == 0 || 
              usernameList == null || usernameList.size() == 0) {
            sendStatusCode(req, 400);
            return;
          }

          final String sessionId = loginSessionIdList.get(0);
          final String username = usernameList.get(0);
          vertx.eventBus().send("participants.authorise",
              new JsonObject().putString("sessionID", sessionId).putString("username", username),
              new Handler<Message<JsonObject>>() {
                  @Override
                public void handle(Message<JsonObject> event) {
                  if ("ok".equals(event.body.getString("status"))) {
                    req.response.headers().put("Set-Cookie",
                        AUTH_COOKIE_NAME + "=" + sessionId + "__" + username + "; HttpOnly");
                    sendStatusCode(req, HttpStatus.SC_OK);
                  } else {
                    sendStatusCode(req, HttpStatus.SC_FORBIDDEN);
                  }
                }
              });
        } else {
          sendRedirect(req, "/static/login.html");          
        }
      }
    });
  }

  private void doAuthAndWriteHostPage(final HttpServerRequest req, String authCookie) {
    String[] cookieParts = authCookie.split("__");
    if (cookieParts.length != 2) {
      sendRedirect(req, "/static/login.html");
      return;
    }

    final String sessionId = cookieParts[0];
    String username = cookieParts[1];   
    final HttpServerResponse response = req.response;
    vertx.eventBus().send("participants.authorise", new JsonObject().putString(
        "sessionID", sessionId).putString("username", username).putBoolean("createClient", true),
        new Handler<Message<JsonObject>>() {
            @Override
          public void handle(Message<JsonObject> event) {
            if ("ok".equals(event.body.getString("status"))) {
              File hostPageBodyFile = new File(bundledStaticFilesPrefix + "HostPage.html.body");
              String activeClientId = event.body.getString("activeClient");
              String username = event.body.getString("username");
              if (activeClientId == null || username == null) {
                sendStatusCode(req, HttpStatus.SC_INTERNAL_SERVER_ERROR);
                return;
              }

              String responseText = getHostPage(sessionId, username, activeClientId);
              response.statusCode = HttpStatus.SC_OK;
              byte[] page = responseText.getBytes(Charset.forName("UTF-8"));
              response.putHeader("Content-Length", page.length);
              response.putHeader("Content-Type", "text/html");
              response.end(new Buffer(page));          
            } else {
              sendRedirect(req, "/static/login.html");
            }
          }
        });
  }

  /**
   * Generate the header for the host page that includes the client bootstrap information as well as
   * relevant script includes.
   */
  private String getHostPage(String userId, String username, String activeClientId) {
    StringBuilder sb = new StringBuilder();
    sb.append("<html>\n");
    sb.append("  <head>\n");

    // Include Javascript dependencies.
    sb.append("<script src=\"/static/sockjs-0.2.1.min.js\"></script>\n");
    sb.append("<script src=\"/static/vertxbus.js\"></script>\n");
    sb.append("<script src=\"/static/com.google.collide.client.Collide/com.google.collide.client.Collide." +
    		"nocache.js\"></script>\n");

    // Embed the bootstrap session object.
    emitBootstrapJson(sb, userId, username, activeClientId);
    emitDefaultStyles(sb);
    sb.append("  </head>\n<body><div id='gwt_root'></div></body>\n</html>");
    return sb.toString();
  }

  private void emitDefaultStyles(StringBuilder sb) {
    sb.append("<style>\n#gwt_root {\n")
      .append("position: absolute;\n")
      .append("top: 0;\n")
      .append("left: 0;\n")
      .append("bottom: 0;\n")
      .append("right: 0;\n")
      .append("}\n</style>");
  }

  private void emitBootstrapJson(
      StringBuilder sb, String userId, String username, String activeClientId) {
    sb.append("<script>\n").append("window['__session'] = {\n")
        .append(JsonFieldConstants.SESSION_USER_ID).append(": \"").append(userId).append("\",\n")
        .append(JsonFieldConstants.SESSION_ACTIVE_ID).append(": \"").append(activeClientId)
        .append("\",\n").append(JsonFieldConstants.SESSION_USERNAME).append(": \"")
        .append(username).append("\"\n}\n").append("</script>");
  }

  private void sendRedirect(HttpServerRequest req, String url) {    
    req.response.putHeader("Location", url);
    sendStatusCode(req, HttpStatus.SC_MOVED_TEMPORARILY);
  }

  private void sendStatusCode(HttpServerRequest req, int statusCode) {
    req.response.statusCode = statusCode;
    req.response.end();
  }
}
