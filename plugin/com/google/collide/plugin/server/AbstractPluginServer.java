package com.google.collide.plugin.server;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import xapi.log.X_Log;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.api.ReceivesValue;

import com.google.collide.dto.CodeModule;
import com.google.collide.dto.GwtRecompile;
import com.google.collide.dto.server.DtoServerImpls.LogMessageImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.plugin.server.gwt.CrossThreadVertxChannel;
import com.google.gwt.core.ext.TreeLogger.Type;

public abstract class AbstractPluginServer <C extends AbstractCompileThread<?>> extends BusModBase implements ServerPlugin {


  protected abstract Class<C> compilerClass();
  
  protected String libRoot;

  protected String webRoot;

  protected static final String AUTH_COOKIE_NAME = "_COLLIDE_SESSIONID";

  @Override
  public abstract String getAddressBase();

  @Override
  public abstract Map<String,Handler<Message<JsonObject>>> getHandlers();


  @Override
  public void initialize(Vertx vertx) {
    this.eb = vertx.eventBus();
    String pluginBase = getAddressBase();
    for (Map.Entry<String, Handler<Message<JsonObject>>> handle : getHandlers().entrySet()){
      vertx.eventBus().registerHandler(pluginBase+"."+handle.getKey(), handle.getValue());
    }
  }

  public List<URL> getCompilerClasspath(final CodeModule request, final ReceivesValue<String> logger) {
    List<URL> list = new ArrayList<URL>(){
      private static final long serialVersionUID = 7809897000236224683L;
      @Override
      public boolean add(URL e) {
        if (e==null)return false;
        logger.set(
            LogMessageImpl.make()
            .setLogLevel(Type.TRACE)
            .setMessage("Adding "+e.toExternalForm()+" to classpath")
            .setModule(request.getModule())
        .toJson());
        return super.add(e);
      }
    };

    File webDir = new File(webRoot);
    File libDir = new File(libRoot);

    Set<String> dedup = new LinkedHashSet<>();//we want duplicate removal and deterministic ordering.

    //add super-sources first (todo: implement)

    //add source folders
    for (String cp : request.getSources().asIterable()){
      if (!cp.endsWith(".jar")){
        URL url = toUrl(webDir,cp);
        if (url != null && dedup.add(url.toExternalForm())){
          X_Log.debug("Adding src folder",cp, url);
          list.add(url);
        }
      }
    }
    //required to run vertx threads
    
    list.add(toUrl(libDir,"vertx/lib/vertx-core-1.3.1.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/vertx-platform-1.3.1.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/vertx-lang-java-1.3.1.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/jackson-core-asl-1.9.4.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/jackson-mapper-asl-1.9.4.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/netty-3.5.9.Final.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/hazelcast-2.4.1.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"collide-server.jar"));//required to run vertx threads
    
    //now, add all the jars listed as source
    for (String cp : request.getSources().asIterable()){
      
      if (cp.endsWith(".jar")) {
        URL url = toUrl(webDir,cp);
        if (url != null && dedup.add(url.toExternalForm())){
          X_Log.debug("Adding src jar",cp, url);
          list.add(url);//needed for collide compile
        }
      }
    }


    String xapiVersion = System.getProperty("xapi.version", "0.3");//Hardcode X_Namespace.XAPI_VERSION for now
    if (!X_String.isEmpty(xapiVersion)) {
      list.add(toUrl(libDir,"xapi-gwt-"+xapiVersion+".jar"));//needs to be before gwt-dev.
    }
    
    list.add(toUrl(libDir,"gwt-user.jar"));//required by compiler
    list.add(toUrl(libDir,"gwt-dev.jar"));//required by compiler
    list.add(toUrl(libDir,"gwt-codeserver.jar"));//required by compiler

    JsonArray<String> deps = request.getDependencies();
    if (deps != null)
    for (String cp : deps.asIterable()){
      URL url = toUrl(libDir,cp);
      if (url != null && dedup.add(url.toExternalForm())) {
        if (cp.endsWith(".jar")){
          X_Log.debug("Adding dependency jar",cp, url);
          list.add(url);//needed for collide compile
        }else{
          X_Log.debug("Adding dependency folder",cp, url);
          list.add(url);//needed for collide compile
        }
      }
    }
    
    //clear our deps and put our entire resolved classpath back
    deps.clear();
    for (URL url : list){
      deps.add(url.toExternalForm().replace("file:", ""));
    }
    
    return list ;
  }
  protected List<URL> getServerClasspath(final CodeModule request, final CrossThreadVertxChannel io) {
    List<URL> list = new ArrayList<URL>(){
      private static final long serialVersionUID = 7809897000236224683L;
      @Override
      public boolean add(URL e) {
        if (e==null)return false;
        io.send(
            LogMessageImpl.make()
            .setLogLevel(Type.TRACE)
            .setMessage("Adding "+e.toExternalForm()+" to classpath")
            .setModule(request.getModule())
        .toJson());
        return super.add(e);
      }
    };

    File webDir = new File(webRoot);
    File libDir = new File(libRoot);

    Set<String> dedup = new LinkedHashSet<>();//we want duplicate removal and deterministic ordering.

    //add super-sources first (todo: implement)

    //add source folders
    for (String cp : request.getSources().asIterable()){
      if (!cp.endsWith(".jar")){
        URL url = toUrl(webDir,cp);
        if (url != null && dedup.add(url.toExternalForm())){
          X_Log.debug("Adding src folder",cp, url);
          list.add(url);
        }
      }
    }

    // next, before we add the rest of the compiler jars, throw in the vertx libs
    // These jars will be removed from gwt-compiler classpath

    list.add(toUrl(libDir,"vertx/lib/vertx-core-1.3.1.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/vertx-platform-1.3.1.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/vertx-lang-java-1.3.1.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/jackson-core-asl-1.9.4.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/jackson-mapper-asl-1.9.4.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/netty-3.5.9.Final.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"vertx/lib/hazelcast-2.4.1.jar"));//required to run vertx threads
    list.add(toUrl(libDir,"collide-server.jar"));//required to run vertx threads

    //now, add all the jars listed as source
    for (String cp : request.getSources().asIterable()){
      
      if (cp.endsWith(".jar")) {
        URL url = toUrl(webDir,cp);
        if (url != null && dedup.add(url.toExternalForm())){
          X_Log.debug("Adding src jar",cp, url);
          list.add(url);//needed for collide compile
        }
      }
    }


    String xapiVersion = System.getProperty("xapi.version", "0.3");//Hardcode X_Namespace.XAPI_VERSION for now
    if (!X_String.isEmpty(xapiVersion)) {
      list.add(toUrl(libDir,"xapi-gwt-"+xapiVersion+".jar"));//needs to be before gwt-dev.
    }
    
    list.add(toUrl(libDir,"gwt-user.jar"));//required by compiler
    list.add(toUrl(libDir,"gwt-dev.jar"));//required by compiler
    list.add(toUrl(libDir,"gwt-codeserver.jar"));//required by compiler

    JsonArray<String> deps = request.getDependencies();
    if (deps != null)
    for (String cp : deps.asIterable()){
      URL url = toUrl(libDir,cp);
      if (url != null && dedup.add(url.toExternalForm())) {
        if (cp.endsWith(".jar")){
          X_Log.debug("Adding dependency jar",cp, url);
          list.add(url);//needed for collide compile
        }else{
          X_Log.debug("Adding dependency folder",cp, url);
          list.add(url);//needed for collide compile
        }
      }
    }

    //clear our deps and put our entire resolved classpath back
    deps.clear();
    for (URL url : list){
      deps.add(url.toExternalForm());
    }
    
    return list ;
  }


  @Override
  public void start() {
    super.start();
    libRoot = getMandatoryStringConfig("staticFiles");
    int ind = libRoot.indexOf("static");
    if (ind > 0)
      libRoot = libRoot.substring(0, ind);
    libRoot = libRoot + "lib"+File.separator;


    webRoot = getMandatoryStringConfig("webRoot");

    initialize(vertx);
  }


  protected URL toUrl(File cwd, String jar) {
    //TODO: allow certain whitelisted absolute uris
    //TODO: allow virtual filesystem uris, like ~/, /bin/, /lib/, /war/

    String path = cwd.getAbsolutePath();
    File file = new File(path,jar);
    if (file.exists()){
      logger.trace("Classpath file exists: "+file);
    }else{
      logger.warn( "Classpath file does not exist! "+file);
      return null;
    }
    path = "file:"+path+File.separator+jar;
    URI uri = URI.create(path);
    try{
      return uri.toURL();
    }catch (Exception e) {
      e.printStackTrace();
      throw X_Debug.rethrow(e);
    }
  }

  public EventBus getEventBus() {
    return eb;
  }


}
