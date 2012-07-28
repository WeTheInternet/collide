package com.google.gwt.dev.codeserver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.collide.server.maven.MavenResources;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.codeserver.CodeServer;

public class CodeSvr {

  
  private static Process process;

  /**
   * This method starts the codeserver if it is not already started.
   * 
   * It then either starts or recompiles the modules.
   * 
   * @param modules - The gwt modules to recompile
   */
  public static void startOrRefresh(TreeLogger logger, MavenResources config, List<String> modules) {
    AppSpace app;
    try {
      app = AppSpace.create(config.getWorkDir());
    } catch (IOException e1) {
      throw new Error("Unable to initialize gwt recompiler ",e1);
    }
    List<File> sourcePath = new ArrayList<File>();
    for (String module : modules){
      Recompiler compiler = new Recompiler(app, module.split("/")[0], sourcePath , logger);
    }
//    
//    Runtime r = Runtime.getRuntime();
//    try {
//      process = r.exec("");
//    } catch (IOException e) {
//      e.printStackTrace();
//      throw new RuntimeException(e);
//    }
  }

  
  
}
