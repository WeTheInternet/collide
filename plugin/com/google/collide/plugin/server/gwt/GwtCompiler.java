package com.google.collide.plugin.server.gwt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;

import org.vertx.java.core.eventbus.EventBus;

import xapi.util.X_Debug;

import com.google.collide.dto.CodeModule;
import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.codeserver.GwtCompilerThread;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

public class GwtCompiler {

  private Object compiler;
  private JsonArray<String> src = JsonCollections.createArray();
  private JsonArray<String> deps = JsonCollections.createArray();
  private final String module;
  private CrossThreadVertxChannel io;
  private UrlAndSystemClassLoader cl;
  private final PrintWriterTreeLogger log;
  private Method compileMethod;
  
  
  public GwtCompiler(String module) {
    this.module = module;
    log = new PrintWriterTreeLogger();
  }

  public boolean isRunning() {
    if (compiler == null) {
      return false;
    }
    try {
      return (Boolean)compiler.getClass().getMethod("isRuning").invoke(compiler);
    } catch (Exception e) {
      throw X_Debug.rethrow(e);
    }
  }

  public boolean isStarted() {
    if (compiler == null) {
      return false;
    }
    try {
      return (Boolean)compiler.getClass().getMethod("isStarted").invoke(compiler);
    } catch (Exception e) {
      throw X_Debug.rethrow(e);
    }
  }

  public void kill() {
    if (compiler != null) {
      try {
        compiler.getClass().getMethod("kill").invoke(compiler);
      } catch (Exception e) {
        throw X_Debug.rethrow(e);
      }
      compiler = null;
    }
  }

  public boolean isMatchingClasspath(CodeModule code) {
    return matches(src, code.getSrc()) && matches(deps, code.getSrc());
  }

  private boolean matches(JsonArray<String> one, JsonArray<String> two) {
    return JsonCollections.equals(one, two);
  }

  public void scheduleRecompile() {
    if (compiler != null) {
      try {
        compiler.getClass().getMethod("doRecompile").invoke(compiler);
      } catch (Exception e) {
        throw X_Debug.rethrow(e);
      }
      
    }
  }

  public void compile(String request) {
    assert compiler != null : "You must initailize the internal compiler before calling .compile() on "+getClass().getName();
    io.setOutput(request);
    try {
//      compiler.compile(request);
      compileMethod.invoke(compiler, request);
    } catch (Throwable e) {
      throw X_Debug.rethrow(e);
    }
  }

  public void initialize(GwtCompileImpl compileRequest, URL[] cp, EventBus eb, String address) {
    if (cl != null) {
      if (!Arrays.equals(cp, cl.getURLs())) {
        cl = null;
      }
    }
    if (cl == null) {
      cl = new UrlAndSystemClassLoader(cp, log);
    } else {
      cl.setAllowSystem(true);
    }
    if (io == null) {
      io = new CrossThreadVertxChannel(cl, eb, address) {
        @Override
        public void destroy() throws Exception {
          io = null;
          compiler = null;
          super.destroy();
        }
      };
    }
    try {
      Class<?> recompilerClass = cl.loadClass(GwtCompilerThread.class.getName());
      Class<?> stringClass = cl.loadClass(String.class.getName());
      Class<?> classLoaderClass = cl.loadClass(ClassLoader.class.getName());
      Class<?> objectClass = cl.loadClass(Object.class.getName());
      Constructor<?> ctor = recompilerClass.getConstructor(stringClass);
      compiler = ctor.newInstance(module);
      Method method = recompilerClass.getMethod("setContextClassLoader", classLoaderClass);
      method.invoke(compiler, cl);

      method = recompilerClass.getMethod("setDaemon", boolean.class);
      method.invoke(compiler, true);
      
      method = recompilerClass.getMethod("setChannel", classLoaderClass, objectClass);
      method.invoke(compiler, getClass().getClassLoader(), io);
      io.setChannel(null);
      compileMethod = recompilerClass.getMethod("compile", String.class);
      cl.setAllowSystem(false);
    } catch (Exception e) {
      log.log(Type.ERROR, "Unable to start the GWT compiler", e);
    }
  }

  public CrossThreadVertxChannel getIO() {
    assert io != null : "You must call .initialize() before calling getIO() in "+getClass().getName();
    return io;
  }

  public void log(String item) {
    log.log(Type.TRACE, item);
  }

}
