package com.google.gwt.dev.codeserver;

import com.google.collide.dto.GwtRecompile;
import xapi.log.X_Log;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.javac.UnitCache;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class SuperDevUtil {

  private static final ConcurrentHashMap<String, RecompileController> compilers
    = new ConcurrentHashMap<String, RecompileController>();

  public static RecompileController getOrMakeController(TreeLogger logger, GwtRecompile request, int port) {
    String module = request.getModule();
    RecompileController ret = compilers.get(module);
    if (ret != null)  {
      ret.cleanup();
      return ret;
    }
    AppSpace app;
    try {
      File tmp = File.createTempFile("recompile", "log").getParentFile();
      tmp.deleteOnExit();
      // We've overridden AppSpace so we can use more deterministic names for our compile folders,
      // but if the user does not order the jars correctly, our overridden method will be missing.
      try {
        // So, to be safe, we'll try with reflection first, and, on failure, use the existing method.
        ClassLoader cl = SuperDevUtil.class.getClassLoader();
        Class<?> cls = cl.loadClass(AppSpace.class.getName());
        Method method = cls.getDeclaredMethod("create", File.class, String.class);
        method.setAccessible(true);
        app = (AppSpace) method.invoke(null, tmp , "Gwtc"+module);
      } catch (Exception e) {
        e.printStackTrace();
        app = AppSpace.create(tmp);
      }

    } catch (IOException e1) {
      throw new Error("Unable to initialize gwt recompiler ",e1);
    }
    List<File> sourcePath = new ArrayList<File>();
    for (String src : request.getSources().asIterable()){
      //TODO: sanitize this somehow?
      if (".".equals(src))src = new File("").getAbsolutePath();
      if (src.startsWith("file:"))src = src.substring(5);
      File dir = new File(src);
      if (!dir.exists()){
        X_Log.error(SuperDevUtil.class,"Gwt source directory "+dir+" does not exist");
      }else
        X_Log.trace(SuperDevUtil.class, "Adding to source: "+dir);
      sourcePath.add(dir);
    }
    for (String src : request.getDependencies().asIterable()){
      if (".".equals(src))src = new File("").getAbsolutePath();
      if (src.startsWith("file:"))src = src.substring(5);
      File dir = new File(src);
      if (!dir.exists()){
        X_Log.error(SuperDevUtil.class,"Gwt dependency directory "+dir+" does not exist");
      }else
        X_Log.trace(SuperDevUtil.class, "Adding to dependencies: "+dir);
      sourcePath.add(dir);
    }
    final OutboxDir outbox = null;
    final LauncherDir launcher = null;
    final Options opts = null;

    final UnitCache cache =  null;
    final MinimalRebuildCacheManager rebinds = null;
    Recompiler compiler = new Recompiler(outbox, launcher, module.split("/")[0], opts,
        cache, rebinds
//        sourcePath, "127.0.0.1:"+port,logger
    );
      try{
        RecompileController recompiler = new RecompileController(compiler);
        compilers.put(module, recompiler);
        return recompiler;
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
  }



}
