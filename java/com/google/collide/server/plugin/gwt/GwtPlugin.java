package com.google.collide.server.plugin.gwt;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonObject;

import wetheinter.net.pojo.LazySingletonProvider;

import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.GwtStatus.CompileStatus;
import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtStatusImpl;
import com.google.collide.server.plugin.ServerPlugin;
import com.google.collide.server.shared.util.Dto;
import com.google.collide.server.shared.util.ReflectionChannel;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.codeserver.CompilerRunnerImpl;

public class GwtPlugin extends BusModBase implements ServerPlugin {

  private final Logger log = Logger.getLogger(getClass().getSimpleName());
  
  String libRoot;

  public GwtPlugin() {
  }
  
  public static interface ModuleCompiler{
    void compile(GwtCompile request) throws CompilerBusyException;
  }
  
  @Override
  public void initialize(Vertx vertx) {
    this.eb = vertx.eventBus();
    String pluginBase = getAddressBase();
    log.log(Level.FINER, "Installing plugin "+getClass()+" to vertx address base "+pluginBase+".*");
    for (Map.Entry<String, Handler<Message<JsonObject>>> handle : getHandlers().entrySet()){
      vertx.eventBus().registerHandler(pluginBase+"."+handle.getKey(), handle.getValue());
      log.log(Level.FINER, "Installing plugin handler "+handle.getValue()+" to vertx address base "+pluginBase+"."+handle.getKey());
    }
    
  }
  
  @Override
  public void start() {
    super.start();
    libRoot = getMandatoryStringConfig("staticFiles");
    int ind = libRoot.indexOf("static");
    if (ind > 0)
      libRoot = libRoot.substring(0, ind);
    libRoot = libRoot + "lib"+File.separator;
    initialize(vertx);
  }
  

  protected List<URL> getCodeServerClasspath(GwtCompile request) throws MalformedURLException {
    List<URL> list = new ArrayList<URL>();

    //for now, just an ugly hardcoded list...
    File cwd = new File(libRoot);
    list.add(toUrl(cwd,"vertx/lib/jars/vert.x-core.jar"));
    list.add(toUrl(cwd,"vertx/lib/jars/vert.x-platform.jar"));
    list.add(toUrl(cwd,"vertx/lib/jars/jackson-core.jar"));
    list.add(toUrl(cwd,"vertx/lib/jars/jackson-mapper.jar"));
    
    list.add(toUrl(cwd,"gwt-user.jar"));
    list.add(toUrl(cwd,"elemental.jar"));
    list.add(toUrl(cwd,"gwt-dev.jar"));
    list.add(toUrl(cwd,"gwt-codeserver.jar"));
    list.add(toUrl(cwd,"waveinabox-import-0.3.jar"));
    list.add(toUrl(cwd,"collide-server.jar"));
    list.add(toUrl(cwd,"xapi-super-0.2.jar"));

    list.add(toUrl(cwd,"guava-gwt-12.0.jar"));
    list.add(toUrl(cwd,"client-src.jar"));
    list.add(toUrl(cwd,"client-common-src.jar"));
    list.add(toUrl(cwd,"client-scheduler-src.jar"));
    list.add(toUrl(cwd,"common-src.jar"));
    list.add(toUrl(cwd,"concurrencycontrol-src.jar"));
    list.add(toUrl(cwd,"model-src.jar"));
    list.add(toUrl(cwd,"media-src.jar"));
    list.add(toUrl(cwd,"waveinabox-import-0.3.jar"));
    list.add(toUrl(cwd,"gson-2.2.1.jar"));
    list.add(toUrl(cwd,"guava-12.0.jar"));
    list.add(toUrl(cwd,"jsr305.jar"));
    
    System.out.println(list);
    return list ;
  }

  private URL toUrl(File cwd, String jar) throws MalformedURLException {
    File file = new File(cwd,jar);
    System.out.println(file);
    String path = cwd.getAbsolutePath();
    path = "file:"+path+File.separator+jar;
    System.out.println(path);
    URI uri = URI.create(path );
//    uri = uri.resolve("");
    return uri.toURL();
  }

  @Override
  public String getAddressBase() {
    return "gwt";
  }

  class NoParentClassLoader extends URLClassLoader{
    boolean allowSystem = true;
    public NoParentClassLoader(URL[] urls){
      super(urls,null);
    }
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      if (allowSystem)
        try{
          return ClassLoader.getSystemClassLoader().loadClass(name);
        }catch (Exception e) {
          log.log(Level.FINE, "Could not load "+name+" from system classloader");
        }
      return super.loadClass(name);
    }
  }
  public class GwtCompileHandler implements Handler<Message<JsonObject>> {
    public class CompilerCleanup implements Runnable{
      @Override
      public void run() {
        io = null;
        if (lastCompile != null)
          allModules.get().remove(lastCompile.getModule());
      }
    }
    NoParentClassLoader cl;
    CrossThreadVertxChannel io;
    Object compiler;
    GwtCompile lastCompile;
    
    private final LazySingletonProvider<Method> compileLauncher = new LazySingletonProvider<Method>(){
      protected Method initialValue() {

        try{
          //prepare our compiler's classpath
          URL[] cp = getCodeServerClasspath(lastCompile).toArray(new URL[0]);
          cl = new NoParentClassLoader(cp);
          final Class<?> runCls = cl.loadClass(CompilerRunnerImpl.class.getName());
          
          compiler = runCls.newInstance();
          Method method = runCls.getMethod("setChannel", cl.loadClass(ClassLoader.class.getName()),Object.class);
          Object otherChannel = method.invoke(compiler, getClass().getClassLoader(), io);
          io.setChannel(otherChannel);
          method = runCls.getMethod("setOnDestroy", cl.loadClass(Object.class.getName()));
          method.invoke(compiler, new CompilerCleanup());
          
          method = compiler.getClass().getMethod("setContextClassLoader", cl.loadClass(ClassLoader.class.getName()));
          method.invoke(compiler, cl);
          method = compiler.getClass().getMethod("setDaemon", boolean.class);
          method.invoke(compiler, true);
          method = compiler.getClass().getMethod("compile", String.class);
          cl.allowSystem = false;
          return method;
        }catch (Exception e) {
          e.printStackTrace();
          if (vertx instanceof VertxInternal)
            ((VertxInternal)vertx).reportException(e);
          throw new RuntimeException(e);
        }
      };
    };
    
    @Override
    public void handle(final Message<JsonObject> message) {
      //TODO(james): before handling, broadcast on internal channel for existing recompiler

      String jsonString = Dto.get(message);
      GwtCompileImpl compileRequest = GwtCompileImpl.fromJsonString(jsonString);
      //TODO compare this compile request with the previous one;
      //if the classpath has changed, we should kill our thread and start again.
      lastCompile = compileRequest;
      //create a reflection channel to help us send serialized json between threads
      Method method;
      synchronized(this){//only create one please!
        if (null==io){
          io = new CrossThreadVertxChannel(cl, jsonString,message, eb,getAddressBase()+".log");
        }
        else{
          io.setOutput(jsonString);
          io.setMessage(message);
        }
        method = compileLauncher.get();
      }
      
      try {
        try {
          method.invoke(compiler, jsonString);
        } catch (InvocationTargetException e) {
          Throwable t = e;
          while (t instanceof InvocationTargetException&&t.getCause() != null)
            t = t.getCause();
          throw t;
        }
      } catch (Throwable e) {
        if (e.getClass().getName().contains(CompilerBusyException.class.getSimpleName())){
          GwtStatusImpl status = GwtStatusImpl.make();
          status.setCompilerStatus(CompileStatus.BLOCKING);
          status.setModule(compileRequest.getModule());
          message.reply(Dto.wrap(status));
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

  private final LazySingletonProvider<Map<String, Handler<Message<JsonObject>>>> allModules =
      new LazySingletonProvider<Map<String, Handler<Message<JsonObject>>>>() {
        protected java.util.Map<String, Handler<Message<JsonObject>>> initialValue() {
          Map<String, Handler<Message<JsonObject>>> map =
              new HashMap<String, Handler<Message<JsonObject>>>();

          map.put("compile", new GwtCompileHandler());
          
          return map;
        };
      };

  @Override
  public Map<String, Handler<Message<JsonObject>>> getHandlers() {
    return ImmutableMap.copyOf(allModules.get());
  }

}
