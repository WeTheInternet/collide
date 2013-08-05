package com.google.collide.plugin.server;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonObject;

import xapi.inject.impl.LazyPojo;
import xapi.log.X_Log;
import xapi.util.X_Debug;
import xapi.util.X_String;

import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.server.DtoServerImpls.CompileResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.dto.server.DtoServerImpls.LogMessageImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.plugin.server.gwt.CompilerBusyException;
import com.google.collide.plugin.server.gwt.CrossThreadVertxChannel;
import com.google.collide.plugin.server.gwt.UrlAndSystemClassLoader;
import com.google.collide.server.shared.util.Dto;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.codeserver.AbstractCompileThread;
import com.google.gwt.dev.codeserver.GwtCompilerThread;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

public abstract class AbstractPluginServer <Compiler extends AbstractCompileThread<?>> extends BusModBase implements ServerPlugin {


  protected abstract Class<Compiler> compilerClass();
  
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

  public class GwtCompileHandler implements Handler<Message<JsonObject>> {
    public class CompilerCleanup implements Runnable{
      @Override
      public void run() {
        logger.info("Cleaning up runtime thread "+getClass());
        io = null;
        cl = null;
//        if (lastCompile != null)
//          allModules.get().remove(lastCompile.getModule());
        compileLauncher.reset();
      }
    }
    UrlAndSystemClassLoader cl;
    URL[] cp;
    CrossThreadVertxChannel io;
    Object compiler;
    GwtCompile lastCompile;

    private final LazyPojo<Method> compileLauncher = new LazyPojo<Method>(){

      @Override
      protected Method initialValue() {
        try{
          //prepare our compiler's classpath
          PrintWriterTreeLogger log = new PrintWriterTreeLogger();

          // We have to prime this classloader without touching it directly,
          // otherwise it will start loading from our classloader, and very bad things will happen.
          cl = new UrlAndSystemClassLoader(cp, log);
          // So, instead we have to use reflection to prepare our foreign thread.
          final Class<?> runCls = cl.loadClass(GwtCompilerThread.class.getName());
          compiler = runCls.newInstance();

          // Tell our foreign thread
          Method method = runCls.getMethod("setChannel", cl.loadClass(ClassLoader.class.getName()),Object.class);
          Object otherChannel = method.invoke(compiler, getClass().getClassLoader(), io);
          io.setChannel(otherChannel);
          method = compiler.getClass().getMethod("setDaemon", boolean.class);
          method.invoke(compiler, true);

          method = runCls.getMethod("setOnDestroy", cl.loadClass(Object.class.getName()));
          method.invoke(compiler, new CompilerCleanup());
          //set 'foreign' classloader :)
          method = compiler.getClass().getMethod("setContextClassLoader", cl.loadClass(ClassLoader.class.getName()));
          method.invoke(compiler, cl);


          method = compiler.getClass().getMethod("compile", String.class);
          cl.setAllowSystem(false);//turn off system classloader for compile
          return method;
        }catch (Exception e) {
          //send this exception through io to client...
          e.printStackTrace();
          if (vertx instanceof VertxInternal)
            ((VertxInternal)vertx).reportException(e);
          throw new RuntimeException(e);
        }
      };
    };
    boolean isrepeat = false;
    @Override
    public void handle(final Message<JsonObject> message) {
      //TODO(james): before handling, broadcast on internal channel for existing recompiler

      String jsonString = Dto.get(message);
      GwtCompileImpl compileRequest = GwtCompileImpl.fromJsonString(jsonString);
      X_Log.info(compileRequest.getDeps());
      X_Log.info(compileRequest.getSrc());
      //TODO compare this compile request with the previous one;
      //if the classpath has changed, we should kill our thread and start again.
      lastCompile = compileRequest;
      //create a reflection channel to help us send serialized json between threads
      Method method;
      boolean rerun = compileLauncher.isSet();
      //TODO: check if classpath has changed, and kill server if different.
      synchronized(this){//only create one please!
        if (null==io){
          io = new CrossThreadVertxChannel(cl, jsonString, eb,getAddressBase()+".log"){
            @Override
            public void destroy() throws Exception {
              compileLauncher.reset();
              io = null;
              super.destroy();
            }
          };
        }
        cp = getServerClasspath(lastCompile, io).toArray(new URL[0]);
        io.setOutput(lastCompile.toString());
        method = compileLauncher.get();
      }

      try {
        try {
          CompileResponseImpl reply = CompileResponseImpl.make()
              .setModule(compileRequest.getModule())
              .setCompilerStatus(CompilerState.RUNNING)
           ;
          if (!rerun){
            //let the client know this is the first compile, and may be slow...
            reply.setCompilerStatus(CompilerState.UNLOADED);
          }
          method.invoke(compiler, jsonString);
          message.reply(Dto.wrap(reply));
        } catch (InvocationTargetException e) {
          Throwable t = e;
          while (t instanceof InvocationTargetException&&t.getCause() != null)
            t = t.getCause();
          throw t;
        }
      } catch (Throwable e) {
        e.printStackTrace();
        if (e.getClass().getName().contains(RuntimeException.class.getSimpleName()) && e.getCause() != null)
          e = e.getCause();
        if (e.getClass().getName().contains(CompilerBusyException.class.getSimpleName())){
          CompileResponseImpl status = CompileResponseImpl.make();
          status.setCompilerStatus(CompilerState.BLOCKING);
          status.setModule(compileRequest.getModule());
          message.reply(Dto.wrap(status));
        }else if (!isrepeat&&e.getClass().getName().contains(IllegalMonitorStateException.class.getSimpleName())){
          io = null;
          compileLauncher.reset();
          try{
            isrepeat = true;
            handle(message);
          }finally{
            isrepeat = false;
          }
        }else{
          io = null;
          compileLauncher.reset();
          e.printStackTrace();
          if (vertx instanceof VertxInternal)
            ((VertxInternal)vertx).reportException(e);
          throw new RuntimeException(e);
        }
      }
    }
  }


  protected List<URL> getServerClasspath(final GwtCompile request, final CrossThreadVertxChannel io) {
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
    for (String cp : request.getSrc().asIterable()){
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
    list.add(toUrl(libDir,"collide-server.jar"));//needed to run vertx threads

    //now, add all the jars listed as source
    for (String cp : request.getSrc().asIterable()){
      
      if (cp.endsWith(".jar")) {
        URL url = toUrl(webDir,cp);
        if (url != null && dedup.add(url.toExternalForm())){
          X_Log.debug("Adding src jar",cp, url);
          list.add(url);//needed for collide compile
        }
      }
    }


    String xapiVersion = System.getProperty("xapi.version", "0.3");
    if (!X_String.isEmpty(xapiVersion))
    list.add(toUrl(libDir,"xapi-gwt-"+xapiVersion+".jar"));//needs to be before gwt-dev.
    //inherit guava first, to avoid old repackaged jars
//    list.add(toUrl(libDir,"guava-gwt-12.0.jar"));//needed for collide compile
//    list.add(toUrl(libDir,"guava-12.0.jar"));//needed for collide compile
    
    list.add(toUrl(libDir,"gwt-user.jar"));//required by compiler
    list.add(toUrl(libDir,"gwt-dev.jar"));//required by compiler
    list.add(toUrl(libDir,"gwt-codeserver.jar"));//required by compiler

    JsonArray<String> deps = request.getDeps();
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


}
