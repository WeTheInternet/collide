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
import com.google.collide.plugin.shared.CompiledDirectory;
import com.google.collide.server.maven.MavenResources;
import com.google.collide.server.shared.BusModBase;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import org.apache.http.HttpStatus;
import xapi.log.X_Log;
import xapi.util.api.Pointer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
  private static final String CODESERVER_FRAGMENT = "/code/";
  private static final String EVENTBUS_FRAGMENT = "/eventbus";
  private static final String SOURCEMAP_PATH = "/sourcemaps/";

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

  /**
   * The directory in which we are compiling temporary files (for gwt builds),
   * used when invoking the code server /{@link #CODESERVER_FRAGMENT}/your.gwt.module/command
   * where command = compile | clean | kill
   *
   * Defaults to the value from MavenConfig, which will default to /tmp
   */
  private String workDir;

  /**
   * The master production directory for your webapp, to which we will sync all changes.
   * If this value == webRootPrefix, then mvn collide:deploy will sync to your collide work directory.
   * Web invoke: /{@link #CODESERVER_FRAGMENT}/deploy
   *
   * Defaults to the value from MavenConfig, which will default to {@link #WEBROOT_PATH}/war
   */
  private String warDir;

  /**
   * Your project / maven configuration file.
   */
  private MavenResources config;
  private SockJSHandler sjsServer;
  private Router router;

  @Override
  public void start() {
    super.start();

    final HttpServerOptions opts = new HttpServerOptions();

    // Configure SSL.
    if (getOptionalBooleanConfig("ssl", false)) {
      opts.setSsl(true)
          .setLogActivity(true)
          .setKeyStoreOptions(new JksOptions()
            .setPassword(getOptionalStringConfig("keyStorePassword", "password"))
            .setPath(getOptionalStringConfig("keyStorePath", "server-keystore.jks"))
          );
    }


    // Configure the event bus bridge.
    boolean bridge = getOptionalBooleanConfig("bridge", false);
    if (bridge) {

      final SockJSHandlerOptions sockOpts = new SockJSHandlerOptions()
          .setHeartbeatInterval(2_000)
          .setSessionTimeout(15_000)
//            .setLibraryURL(getOptionalStringConfig("suffix", "") + "/eventbus")
          ;
      JsonArray inboundPermitted = getOptionalArrayConfig("in_permitted", new JsonArray());
      JsonArray outboundPermitted = getOptionalArrayConfig("out_permitted", new JsonArray());
      final BridgeOptions bridgeOpts = new BridgeOptions()
          .setReplyTimeout(15_000)
          .setPingTimeout(15_000);
      bridgeOpts.setInboundPermitted(toOpts(inboundPermitted));
      bridgeOpts.setOutboundPermitted(toOpts(outboundPermitted));

      sjsServer = SockJSHandler.create(vertx, sockOpts);
      sjsServer.bridge(bridgeOpts);

      router = Router.router(vertx);
//
      router.route("/eventbus/*").handler(sjsServer);


//          getOptionalStringConfig("auth_address", "participants.authorise"));
    }

    HttpServer server = vertx.createHttpServer(opts);
    server.requestHandler(this);

    String bundledStaticFiles = getMandatoryStringConfig("staticFiles");
    String webRoot = getMandatoryStringConfig("webRoot");
    String workDirectory = getOptionalStringConfig("workDir", "/tmp");
    String warDirectory = getOptionalStringConfig("warDir", webRoot + File.separator + "war");


    bundledStaticFilesPrefix = bundledStaticFiles + File.separator;
    webRootPrefix = webRoot + File.separator;
    workDir = workDirectory + File.separator;
    warDir = warDirectory + File.separator;

    int port = getOptionalIntConfig("port", 8080);
    String host = getOptionalStringConfig("host", "127.0.0.1");
    System.out.println("Connecting to "+host+":"+port);
    server.listen(port, host);

    vertx.eventBus().<JsonObject>consumer("frontend.symlink", event -> {
        //TODO: require these messages to be signed by code private to the server
        try{
        String dto = event.body().getString("dto");
        X_Log.trace("Symlinking",dto);
        CompiledDirectory dir = CompiledDirectory.fromString(dto);
        vertx.sharedData().getLocalMap("symlinks").put(
            dir.getUri()
            , dir);
        }catch (Exception e) {
          e.printStackTrace();
        }
    });
  }

  private List<PermittedOptions> toOpts(JsonArray inboundPermitted) {
    return ((List<?>)inboundPermitted.getList())
        .stream()
        .map(name->new PermittedOptions().setAddressRegex(String.valueOf(name)))
        .collect(Collectors.toList());
  }

  private class SymlinkRequest{
    String urlFragment;
    String defaultTarget;
    String userAgent;
    HttpServerResponse response;
  }
  private class SymlinkResponse{
    boolean handled;
    String resolved;
  }

  @Override
  public void handle(HttpServerRequest req) {
    if (req.path().equals("/") || req.path().equals("/demo/")) {
      //send login page
      authAndWriteHostPage(req);
      return;
    }
    String path = req.path().replace("/demo/", "/");
    if (path.contains("..")) {
      //sanitize hack attempts
      sendStatusCode(req, 404);
    } else if (path.startsWith(EVENTBUS_FRAGMENT)) {
      System.out.println("Sending to event bus: " + router.get(req.path()));
      router.accept(req);
//      req.upgrade()
    } else if (path.startsWith(CODESERVER_FRAGMENT)) {
      sendToCodeServer(req);//listen on http so we can send compile requests without sockets hooked up.
    } else{
      if (path.startsWith(AUTH_PATH)) {
        writeSessionCookie(req);
        return;
      }
      else if (path.startsWith(WEBROOT_PATH) && (webRootPrefix != null)){
        //TODO: sanitize this path
        //check for symlinks
        logger.info(path);
        SymlinkRequest request = new SymlinkRequest();
        request.urlFragment = path.substring(WEBROOT_PATH.length());
        request.defaultTarget = webRootPrefix;
        request.response = req.response();
        SymlinkResponse response = processSymlink(request);
        if (response.handled)
          return;
      }
      else if (path.contains(SOURCEMAP_PATH)){
        //forward sourcemap paths to appropriate internal server
        SymlinkRequest request = new SymlinkRequest();
        request.urlFragment = path;
        // dump headers in case we can pull in the permutation here...
        request.userAgent = req.headers().get("User-Agent");
        request.defaultTarget = webRootPrefix;
        request.response = req.response();
        SymlinkResponse response = processSymlink(request);
        if (response.handled)
          return;

      } else if (path.startsWith(BUNDLED_STATIC_FILES_PATH) && (bundledStaticFilesPrefix != null)) {
        Pointer<String> file = new Pointer<String>(path.substring(BUNDLED_STATIC_FILES_PATH.length()));
        //check for symlinks
        SymlinkRequest request = new SymlinkRequest();
        request.urlFragment = file.get();
        request.defaultTarget = bundledStaticFilesPrefix;
        request.response = req.response();
        SymlinkResponse response = processSymlink(request);
        if (response.handled)
          return;
      }
      //if we didn't return, we're out of options.
      doFail(req);
    }
  }
  private void doFail(HttpServerRequest req) {
    //TODO: add a pluggable proxy here.
    sendStatusCode(req, HttpStatus.SC_NOT_FOUND);
  }
  private SymlinkResponse processSymlink(final SymlinkRequest request) {
    SymlinkResponse response = new SymlinkResponse();


    LocalMap<String, Object> map = vertx.sharedData().getLocalMap("symlinks");
    Set<String> keys= map.keySet();
    String uri = request.urlFragment;
//    System.out.println("Dereferencing request uri: "+uri+" against "+keys);
    if (uri.length() == 0) {
      return response;
    }
    if (uri.charAt(0)=='/')uri = uri.substring(1);
    for (Object key : keys){
      final String symlink = String.valueOf(key);
      if (uri.contains(symlink)){
        Object link =  map.get(symlink);
        //TODO: also serve up _gen, _extra, _source, etc.
        if (("/"+uri).contains(SOURCEMAP_PATH)){
          X_Log.info("Checking against symlink ",uri," .contains( ",symlink," )");
          //serve up file from our sourcemap directory
          try{
            if (uri.endsWith(".java")||uri.endsWith("gwtSourceMap.json")){
              //client is requesting actual source file.

              //try to proxy the request to tcp server in gwt compiler.
              int port =
                  (int) link.getClass().getMethod("getPort").invoke(link);
              final String cls = uri;
              final Pointer<Boolean> success = new Pointer<Boolean>(false);
              vertx.createNetClient().connect(port, "localhost",
                  async -> {
                  final NetSocket event = async.result();
                    if (cls.endsWith("json")) {
                      // requesting sourcemap file
                      request.response.headers().set("Content-Type","application/json");
                      event.write(cls+"\n" + "User-Agent: "+request.userAgent);
                    } else {
                      // requesting source file
                      // chop off the module name from cls
                      event.write(cls.substring(1+cls.indexOf('/',SOURCEMAP_PATH.length())));
                      request.response.headers().set("Content-Type","text/plain");
                    }

                    final Buffer buf = Buffer.buffer(4096);
                    event.handler(buf::appendBuffer);
                    event.closeHandler(ev-> {
                        int len = buf.length();
                        if (len > 0) {
                          success.set(true);
                          request.response.putHeader("Content-Length", Integer.toString(buf.length()));
                          request.response.putHeader("Content-Type",
                            cls.endsWith("json") ? "application/json" : "text/plain");
                          request.response.end(buf);
                          synchronized (success) {
                            success.notify();
                          }
                        }
                    });
              });
              synchronized (success) {
                success.wait(5000);
              }
              response.handled = success.get();
              return response;
            }

            String base = String.valueOf(link.getClass().getMethod("getSourceMapDir").invoke(link));

            //current super dev mode only produces one permutation for chrome,
            //so there >should< only be be one sourcemap file to find.
            //in the future, we will have to resolve the correct permutation to send.

            //for now, we'll just search for the file requested.
            File f = new File(base);
            if (f.exists()&&f.isDirectory()){
              String suffix = uri.substring(1+uri.lastIndexOf('.'));
              for (File child : f.listFiles()){
                if (child.getName().endsWith(suffix)){
                  response.handled=true;
                  response.resolved = child.getAbsolutePath();
                  applySourceMapHeader(request.response, response.resolved);
                  request.response.sendFile(response.resolved);
                  return response;
                }
              }
            }
          }catch (Exception e) {
            e.printStackTrace();
          }
        }

        try{
          response.resolved =
              link.getClass().getMethod("getWarDir").invoke(link)+"/"+symlink
              +uri.substring(symlink.length());
          logger.info("Symlink resolved to "+response.resolved);
          response.handled = true;
          applySourceMapHeader(request.response, response.resolved);
          request.response.sendFile(response.resolved);
          return response;
        }catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    response.handled=true;
    if (request.urlFragment.contains(SOURCEMAP_PATH)) {
      // This is for statically compiled gwt source (built through gradle),
      // which will have a different structure than our dynamic superdev compiles;
      String[] bits = request.urlFragment.split("/");
      String moduleName = bits[2];
      if (request.urlFragment.endsWith(".json")) {
        // we are save to assume _sourceMap0 since there will never be more than 1 iteration on a full compile
        String mapFile = bits[4].replace(".json", "_sourceMap0.json");
        response.resolved = bundledStaticFilesPrefix.replace("out/"+moduleName, "extra") + moduleName+ "/symbolMaps/" + mapFile;
      } else {
        String sourceFolder = bundledStaticFilesPrefix.replace("out/"+moduleName, "sourcemaps") + moduleName+ "/src/";
        response.resolved = sourceFolder + request.urlFragment.split(SOURCEMAP_PATH)[1];

      }
    } else {
      response.resolved = request.defaultTarget+request.urlFragment;
      applySourceMapHeader(request.response, response.resolved);
    }
    request.response.sendFile(response.resolved);
    return response ;
  }

  private void applySourceMapHeader(HttpServerResponse response, String moduleName) {
    if (moduleName.endsWith(".cache.js")){
      int last = moduleName.lastIndexOf('/');
      String strongName = moduleName.substring(last+1).split("[.]cache")[0];
      moduleName = moduleName.substring(moduleName.lastIndexOf('/',last-1)+1,last);
      // TODO: perform reverse-lookup on shortened module name
      // TODO: link strong name to user session for this module
      response.putHeader("X-SourceMap",BUNDLED_STATIC_FILES_PATH+moduleName +SOURCEMAP_PATH+strongName+".json");
      response.putHeader("Access-Control-Allow-Origin", "*");
    }
  }


  private void sendToCodeServer(HttpServerRequest req) {
    Cookie cookie = Cookie.getCookie(AUTH_COOKIE_NAME, req);
    if (cookie == null) {
      sendRedirect(req, "/static/login.html");
      return;
    }
    //TODO: forward commands to message bus, so we can allow throttled guest compiles.
//    QueryStringDecoder qsd = new QueryStringDecoder(req.query, false);
//    Map<String, List<String>> params = qsd.getParameters();
//
//    List<String> loginSessionIdList = params.get(JsonFieldConstants.SESSION_USER_ID);
//    List<String> usernameList = params.get(JsonFieldConstants.SESSION_USERNAME);
//    if (loginSessionIdList == null || loginSessionIdList.size() == 0 ||
//        usernameList == null || usernameList.size() == 0) {
//      sendStatusCode(req, 400);
//      return;
//    }
//
//    final String sessionId = loginSessionIdList.get(0);
//    final String username = usernameList.get(0);
//    List<String> modules = qsd.getParameters().get("module");
//
//    if (modules != null){
//      MavenResources config = getConfig(req);
//      CodeSvr.startOrRefresh(logToVertx(req),config,modules);
//    }
  }

  private MavenResources getConfig(HttpServerRequest req) {
//    req.response.
    MavenResources res = new MavenResources();
    res.setSrcRoot(webRootPrefix);
    res.setWorkDir(workDir);
    res.setWarTargetDir(warDir);
    res.setWarSrcDir(warDir);
    return res;
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
    if (!"POST".equals(req.method().name())) {
      sendStatusCode(req, HttpStatus.SC_METHOD_NOT_ALLOWED);
      return;
    }

    // Extract the post data.
    req.bodyHandler(new Handler<Buffer>() {
        @Override
      public void handle(Buffer buff) {
        String contentType = req.headers().get("Content-Type");
        if (String.valueOf(contentType).startsWith("application/x-www-form-urlencoded")) {
          QueryStringDecoder qsd = new QueryStringDecoder(buff.toString(), false);
          Map<String, List<String>> params = qsd.parameters();

          List<String> loginSessionIdList = params.get(JsonFieldConstants.SESSION_USER_ID);
          List<String> usernameList = params.get(JsonFieldConstants.SESSION_USERNAME);
          if (loginSessionIdList == null || loginSessionIdList.size() == 0 ||
              usernameList == null || usernameList.size() == 0) {
            System.out.println("Failed to write session cookie; "+loginSessionIdList+" / "+usernameList);
            sendStatusCode(req, 400);
            return;
          }

          final String sessionId = loginSessionIdList.get(0);
          final String username = usernameList.get(0);
          X_Log.info("Writing session cookie; ",username," / ",sessionId);
          vertx.eventBus().<JsonObject>send("participants.authorise",
              new JsonObject().put("sessionID", sessionId).put("username", username),
              async -> {
                final Message<JsonObject> event = async.result();
              X_Log.trace("Writing session cookie; ",event.body());
              if ("ok".equals(event.body().getString("status"))) {
                req.response().headers().set("Set-Cookie",
                    AUTH_COOKIE_NAME + "=" + sessionId + "__" + username + "; HttpOnly");
                sendStatusCode(req, HttpStatus.SC_OK);
              } else {
                sendStatusCode(req, HttpStatus.SC_FORBIDDEN);
              }
            }
          );
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
    final HttpServerResponse response = req.response();
    vertx.eventBus().<JsonObject>send("participants.authorise", new JsonObject().put(
        "sessionID", sessionId).put("username", username).put("createClient", true),
        async -> {
          final Message<JsonObject> event = async.result();
        if ("ok".equals(event.body().getString("status"))) {
          String activeClientId = event.body().getString("activeClient");
          String username1 = event.body().getString("username");
          if (activeClientId == null || username1 == null) {
            sendStatusCode(req, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return;
          }
          String path = req.path();
          String module = "Collide";

          if (path.endsWith("demo/")) {
            path = path.replace("/demo", "");
            module = "Demo";
          }
          String responseText = getHostPage(path, module, sessionId, username1, activeClientId);
          response.setStatusCode(HttpStatus.SC_OK);
          byte[] page = responseText.getBytes(Charset.forName("UTF-8"));
          response.putHeader("Content-Length", Integer.toString(page.length));
          response.putHeader("Content-Type", "text/html");
          response.end(Buffer.buffer(page));
        } else {
          sendRedirect(req, "/static/login.html");
        }
      }
    );
  }

  /**
   * Generate the header for the host page that includes the client bootstrap information as well as
   * relevant script includes.
   */
  private String getHostPage(String path, String module, String userId, String username, String activeClientId) {
    StringBuilder sb = new StringBuilder();
    sb.append("<!doctype html>\n");
    sb.append("<html>\n");
    sb.append("  <head>\n");
    sb.append("<title>CollIDE - Collaborative Development</title>\n");
    if (path.endsWith("/"))
      path = path.substring(0, path.length()-1);
//    if (module.indexOf('/') == -1)
//      module = module + '/' + module;
    sb.append("<script src=\"" + path +"/static/sockjs.js\"></script>\n");
    sb.append("<script src=\"" + path+"/static/vertxbus.js\"></script>\n");
    sb.append("<script src=\"" +path+"/static/" + module + "." +
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
    sb.append("<script>\n")
    .append("window['__session'] = {\n")
        .append(JsonFieldConstants.SESSION_USER_ID).append(": \"").append(userId).append("\",\n")
        .append(JsonFieldConstants.SESSION_ACTIVE_ID).append(": \"").append(activeClientId)
        .append("\",\n").append(JsonFieldConstants.SESSION_USERNAME).append(": \"")
        .append(username).append("\"\n};\n")

        .append("window['collide'] = {\n")
        .append("name: 'Guest', module: 'collide.demo.Child', open: '/demo/src/main/java" +
        		"/collide/demo/child/ChildModule.java' };")
        .append("</script>");
  }

  private void sendRedirect(HttpServerRequest req, String url) {
    if (req.path().startsWith("/collide")) {
      url = "/collide" + url;
      url = url.replace("login.html", "login_collide.html");
      System.out.println("Got it"+req.path());
    }
    System.out.println(req.path());
    req.response().putHeader("Location", url);
    sendStatusCode(req, HttpStatus.SC_MOVED_TEMPORARILY);
  }

  private void sendStatusCode(HttpServerRequest req, int statusCode) {
    req.response().setStatusCode(statusCode);
    req.response().end();
  }
}
