package com.google.collide.plugin.server.gwt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonObject;

import xapi.collect.api.InitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.collect.impl.InitMapString;
import xapi.inject.impl.LazyPojo;
import xapi.log.X_Log;
import xapi.util.api.ConvertsValue;

import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.server.DtoServerImpls.CompileResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.plugin.server.AbstractPluginServer;
import com.google.collide.plugin.server.IsCompileThread;
import com.google.collide.server.shared.util.Dto;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.dev.codeserver.GwtCompilerThread;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

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

  private final InitMap<String, IsCompileThread<GwtCompile>> compilers = new InitMapString<IsCompileThread<GwtCompile>>(new ConvertsValue<String, IsCompileThread<GwtCompile>>() {
    @Override
    public IsCompileThread<GwtCompile> convert(String module) {
      return null;
    }
  });
  
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

  class GwtKillHandle implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> event) {
      
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
      X_Log.trace("Dependencies", compileRequest.getDeps());
      X_Log.trace("Source", compileRequest.getSrc());
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
