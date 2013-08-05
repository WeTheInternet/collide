package com.google.gwt.dev.codeserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import xapi.inject.impl.LazyPojo;

import com.google.collide.plugin.shared.CompiledDirectory;
import com.google.collide.plugin.shared.IsCompiler;
import com.google.gwt.dev.cfg.ResourceLoader;

public class RecompileController implements IsCompiler {

  private Logger log = Logger.getLogger(getClass().getSimpleName());

  private LazyPojo<CompiledDirectory> compileDir =
      new LazyPojo<CompiledDirectory>(){
    @Override
    protected CompiledDirectory initialValue() {
      final CompileDir dir = initialize();

      // Try to look up the permutation map from war directory
      File warp = dir.getWarDir();

      File war = new File(warp,getModuleName()+File.separator+"compilation-mappings.txt");
      Map<String, String> permutations = new HashMap<>();
      if (war.exists()) {
        try (
          BufferedReader read = new BufferedReader(new FileReader(war));
        ){
        String line, strongName=null;
        while ((line = read.readLine())!=null) {
          if (line.startsWith("user.agent")) {
            if (strongName != null) {
              if (line.contains("safari")) {
                permutations.put("safari", strongName);
              }
              else if (line.contains("gecko1_8")) {
                permutations.put("gecko1_8", strongName);
              }
              else if (line.contains("gecko")) {
                permutations.put("gecko", strongName);
              }
              else if (line.contains("ie6")) {
                permutations.put("ie6", strongName);
              }
              else if (line.contains("ie8")) {
                permutations.put("ie8", strongName);
              }
              else if (line.contains("ie9")) {
                permutations.put("ie9", strongName);
              }
              else if (line.contains("opera")) {
                permutations.put("opera", strongName);
              }
            }
          } else {
            int ind = line.indexOf(".cache.js");
            if (ind > -1) {
              strongName = line.substring(0, ind);
            }
          }
        }
        }catch (Exception e) {
          e.printStackTrace();
        }
      }

      return new CompiledDirectory()
        .setDeployDir(dir.getDeployDir().getAbsolutePath())
        .setExtraDir(dir.getExtraDir().getAbsolutePath())
        .setGenDir(dir.getGenDir().getAbsolutePath())
        .setLogFile(dir.getLogFile().getAbsolutePath())
        .setWarDir(dir.getWarDir().getAbsolutePath())
        .setWorkDir(dir.getWorkDir().getAbsolutePath())
        .setSourceMapDir(dir.findSymbolMapDir(getModuleName()).getAbsolutePath())
        .setUri(getModuleName())
        .setUserAgentMap(permutations)
      ;
    };
    @Override
    public void reset() {
      //TODO: delete any files, if they exist
      super.reset();
    };
  };
  private final Recompiler recompiler;

  public RecompileController(Recompiler compiler) {
    this.recompiler = compiler;
  }

  public CompiledDirectory recompile(){
    if (compileDir.isSet()) {
      // TODO maybe skip destroying the existing compile if clients are still connected.
      destroy(compileDir.get());
    }
    compileDir.reset();
    return compileDir.get();
  }

  /**
   * Kill previous compile in a separate thread, to avoid wasting wall-time.

   * @param dir - The draft compile to destroy.
   */
  private void destroy(final CompiledDirectory dir) {
    final File deployDir = new File(dir.getDeployDir());
    final File extraDir = new File(dir.getExtraDir());
    final File genDir = new File(dir.getGenDir());
    final File logDir = new File(dir.getLogFile());
    final File mapDir = new File(dir.getSourceMapDir());
    final File warDir = new File(dir.getWarDir());
    final File workDir = new File(dir.getWorkDir());
    Thread cleanup = new Thread(new Runnable() {
      @Override
      public void run() {
        
        try {
          // wait thirty seconds before taking out the old compile
          Thread.sleep(30000);
          destroy(deployDir);
          Thread.sleep(1000);
          destroy(extraDir);
          Thread.sleep(1000);
          destroy(genDir);
          Thread.sleep(1000);
          destroy(mapDir);
          Thread.sleep(1000);
          destroy(warDir);
          Thread.sleep(1000);
          destroy(workDir);
          Thread.sleep(1000);
        } catch (InterruptedException e) {Thread.currentThread().interrupt();}
      }

      private void destroy(File file) {
        try {
          FileUtils.deleteDirectory(file);
        } catch (IOException e) {
          System.err.println("Error destroying "+file+" ; ");
          System.err.println("Check if directory is empty? "+file.exists());
        }
      }
    });
    cleanup.start();
  }

  public String getModuleName(){
    return recompiler.getModuleName();
  }
  public ResourceLoader getResourceLoader(){
    return recompiler.getResourceLoader();
  }

  protected CompileDir initialize(){
    Map<String, String> defaultProps = new HashMap<String, String>();
    defaultProps.put("user.agent", "safari,gecko1_8");
    defaultProps.put("locale", "en");
    defaultProps.put("compiler.useSourceMaps", "true");
      try{
        CompileDir dir = recompiler.compile(defaultProps);
        return dir;
      }catch (Exception e) {
        e.printStackTrace();
        log.log(Level.SEVERE, "Unable to compile module.", e);
        throw new RuntimeException(e);
      }
    }

  public void cleanup() {

  }

}
