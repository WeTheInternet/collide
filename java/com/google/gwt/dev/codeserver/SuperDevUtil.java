package com.google.gwt.dev.codeserver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.collide.dto.GwtCompile;
import com.google.collide.server.maven.MavenResources;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

public class SuperDevUtil {

  
  /**
   * This method starts the codeserver if it is not already started.
   * 
   * It then either starts or recompiles the modules.
   * 
   * @param modules - The gwt modules to recompile
   */
  public static RecompileController startOrRefresh(TreeLogger logger, MavenResources config, List<String> modules) {
    AppSpace app;
    try {
      app = AppSpace.create(config.getWorkDir());
    } catch (IOException e1) {
      throw new Error("Unable to initialize gwt recompiler ",e1);
    }
    List<File> sourcePath = new ArrayList<File>();
    for (String module : modules){
      Recompiler compiler = new Recompiler(app, module.split("/")[0], sourcePath , logger);

      Map<String, String> defaultProps = new HashMap<String, String>();
      defaultProps.put("user.agent", "safari");
      defaultProps.put("locale", "en");
      defaultProps.put("logLevel", "DEBUG");
      defaultProps.put("compiler.useSourceMaps", "true");
      try {
        CompileDir result = compiler.compile(defaultProps);
      } catch (UnableToCompleteException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private static final ConcurrentHashMap<String, RecompileController> compilers 
    = new ConcurrentHashMap<String, RecompileController>();
  
  public static RecompileController getOrMakeController(TreeLogger logger, GwtCompile request) {
    String module = request.getModule();
    RecompileController ret = compilers.get(module);
    if (ret != null)  
      return ret;
    AppSpace app;
    try {
      File tmp = File.createTempFile("recompile", "log");
      tmp.deleteOnExit();
      app = AppSpace.create(tmp.getParentFile());
    } catch (IOException e1) {
      throw new Error("Unable to initialize gwt recompiler ",e1);
    }
    List<File> sourcePath = new ArrayList<File>();
    for (String src : request.getSrc().asIterable()){
      //TODO: sanitize this somehow?
      if (".".equals(src))src = new File("").getAbsolutePath();
      File dir = new File(src);
      if (!dir.exists()){
        System.err.println("Error! Gwt source directory "+dir+" does not exist");
      }else
        System.out.println("Adding to source: "+dir);
      sourcePath.add(dir);
    }
      Recompiler compiler = new Recompiler(app, module.split("/")[0], sourcePath , logger);
      try{
        RecompileController recompiler = new RecompileController(compiler);
        compilers.put(module, recompiler);
        return recompiler;
      } catch (Exception e) {
        e.printStackTrace();
        throw new Error(e);
      }
  }

  
  
}
