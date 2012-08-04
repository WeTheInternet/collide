package com.google.gwt.dev.codeserver;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import wetheinter.net.pojo.LazySingletonProvider;

public class RecompileController {

  private Logger log = Logger.getLogger(getClass().getSimpleName());
  
  private LazySingletonProvider<CompileDir> compileDir =
      new LazySingletonProvider<CompileDir>(){
    protected CompileDir initialValue() {
      return initialize();
    };
  };
  private Recompiler recompiler;
  
  public RecompileController(Recompiler compiler) {
    this.recompiler = compiler;
  }
  
  CompileDir recompile(){
    compileDir.reset();  
    return compileDir.get();
  }

  protected CompileDir initialize(){
    Map<String, String> defaultProps = new HashMap<String, String>();
    defaultProps.put("user.agent", "safari");//TODO: rip from module.xml
    defaultProps.put("locale", "en");
    defaultProps.put("compiler.useSourceMaps", "false");//set back to true when we serve sourcemaps from CompileDir
      try{
        CompileDir dir = recompiler.compile(defaultProps);
        return dir;
      }catch (Exception e) {
        e.printStackTrace();
        log.log(Level.SEVERE, "Unable to compile module.", e);
        throw new Error(e);
      }
    }
  
}
